package com.dc.tools.io.file;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.dc.tools.common.utils.CloseTasks;
import com.dc.tools.common.utils.DirectBufferUtils;
import com.dc.tools.io.buffer.unit.DataSize;
import com.dc.tools.timer.AbstractTask;
import com.dc.tools.timer.Timer;
import com.dc.tools.timer.TimerUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文本文件写入，非线程安全，如果需要支持线程安全请在外部加锁实现
 * <p>
 * 对于文件写入的场景，java目前支持 sendfile、mmap+write、filechannel + buffer三种场景
 * <ul>
 *     <li>1、sendfile是上述三种场景中性能最高且最快速的方式，目前nio支持的方式只可以通过{@link FileChannel#transferTo(long, long, WritableByteChannel)}</li>
 *     <li>2、mmap+write 主要针对小数据量的场景，比如每次写入的数据大概几十个字节或者几百个字节(不超过4KB的场景)，
 *     因为mmap的内存可以支持{@code mlock}以及{@code madvice}操作，如果是业务开发严禁使用{@code  mlock}操作因为会导致该内存无法被应用程序使用</li>
 *     <li>3、fileChannel+buffer 主要针对数据量比较大的场景，尤其是在写入的数据是4KB的倍数时效率极高</li>
 *     <li>4、mmap 或者 fileChannel 底层都是借助{@code page cache}完成数据的写入读取的，底层在写入数据时只是会写到操作系统的缓冲区(page cache)中，再由操作系统在合适的时机将缓冲区的数据同步到磁盘中</li>
 *     <li>5、如果文件写入的数据并不是需要实时或者半实时刷入磁盘的，尽量少调用{@link MappedByteBuffer#force()}或者{@link FileChannel#force(boolean)} 方法，
 *     因为会造成程序卡顿，当我们调用force方法时，系统底层会调用{@code fsync}、{@code fdatasync}或者{@code msync} 来将page cache中的数据刷入到磁盘</li>
 *     <li>6、一个文件的描述符被close成功，并不会触发操作系统的刷盘</li>
 *     <li>7、当调用{@link FileChannel#force(boolean)} }方法时也会将mmap映射的page cache一起刷入到磁盘</li>
 *     <li>8、对于{@link java.nio.DirectByteBuffer} 以及 {@link MappedByteBuffer} 类型的堆外内存，当程序中不再持有上述两者的强引用时，jvm会在垃圾回收时调用cleaner方法对内存进行回收 {@link   Reference#processPendingReferences()}</li>
 *     <li>9、mapped buffer 内存映射的最大大小为 (1 << 31) - 1，因为在调用map方法时会对size进行判断 {@link sun.nio.ch.FileChannelImpl#map(FileChannel.MapMode, long, long)}</li>
 *     <li>10、sendfile支持文件到文件的传输，以及文件到网络的传输（windows除外）{@link sun.nio.ch.FileChannelImpl#transferToDirectly(long, int, WritableByteChannel)} </li>
 *     <li>11、Sendfile and mmap implementation： <a href = "https://hg.openjdk.org/jdk8u/jdk8u/jdk/file/7fcf35286d52/src/solaris/native/sun/nio/ch/FileChannelImpl.c">jvm code</a></li>
 *     <li>12、当我们写入的数据需要拆分上下游时，为了避免中间文件的转换可以采用{@link Pipe}, 因为底层同样支持send file操作</li>
 *     <li>13、当读取的channel不是必须为堆内内存时（这里不考虑read channel为fileChannel的场景），避免使用{@link FileChannel#transferFrom(ReadableByteChannel, long, long)}，因为会通过堆内的内存进行复制，可以采用direct buffer + read更方便一些</li>
 * </ul>
 * </p>
 * <p>
 *   mapped buffer ：<a href="https://mp.weixin.qq.com/s?__biz=Mzg2MzU3Mjc3Ng==&mid=2247489304&idx=1&sn=18e905f573906ecff411ebdcf9c1a9c5&chksm=ce77d15ff9005849b022e4288e793e5036bda42916591a4a3d28f76554188c25cfbd35d951f9&token=1717370099&lang=zh_CN#rd">write efficiency</a>
 * </p>
 *
 * @author zy
 * @apiNote 单文件不建议多线程写入，对于磁盘文件写入的场景应尽量顺序写入
 * @see ReferenceQueue
 * @see Reference
 */
public class CommonFileWriter implements FileWriter {

    private static final Logger log = LoggerFactory.getLogger(CommonFileWriter.class);

    /**
     * base unit
     */
    private static final int BASE_SHIFT = 20;

    /**
     * mapped buffer chunk size
     */
    private static final int MAX_CHUNK_SIZE = 32;

    /**
     * base memory unit
     */
    private static final int PAGE_SHIFT = 10;

    /**
     * 每个内存映射模块支持的最大大小为 32M
     * TODO: 是否需要进行收缩，当调用MappedByteBuffer#force时会发生阻塞
     */
    private static final int MAX_CHUNK_FILE_SIZE = MAX_CHUNK_SIZE << BASE_SHIFT;

    /**
     * 刷磁盘的单元
     */
    private static final int FORCE_FILE_UNIT = 4 << PAGE_SHIFT;

    /**
     * reserve size for header or tail data, if size is already 2^, change to (1 << -(Integer.SIZE - PAGE_SHIFT + 1))
     */
    private static final int RESERVE_SIZE = 2 << PAGE_SHIFT;

    /**
     * all mapped buffer references,
     */
    private static final Set<MappedByteBufferReference> references = new ConcurrentHashSet<>();

    /**
     * gc mapped buffer notifications
     */
    private static final ReferenceQueue<MappedByteBuffer> bufferQueue = new ReferenceQueue<>();

    /**
     * 扫描是否存在内存泄露
     */
    private static final Timer SCAN_TIMER;

    static {
        //检测可能存在的内存泄露行为，并且进行上报
        //TODO: 检测的意义不大，可以删除掉，因为当CommonFileWriter的强引用消失后，jvm会总回收相应的内存
        //TODO: 如果在CommonFileWriter内部是持有池化资源的内存则需要进行内存泄露检测
        SCAN_TIMER = TimerUtils.createTimer("check-buffer");
        SCAN_TIMER.addTask(new CheckBufferLeak(), 1, TimeUnit.MINUTES);
    }

    /**
     * 文件channel
     */
    private final FileChannel fileChannel;

    /**
     * 文件路径
     */
    private final Path filePath;

    /**
     * 当前写入文件的buffer
     */
    private volatile MappedBuffer writeBuffer;

    /**
     * 全文件 写入文件的指针位置
     */
    private final AtomicLong writePos = new AtomicLong();

    /**
     * 内存映射的单元
     */
    private final int mapUnitSize;

    /**
     * map buffer 写入的位置
     */
    private final AtomicInteger bufferPos = new AtomicInteger();

    /**
     * 需要被分割的标准文件大小，如果是不需要被分割的文件设置为0，如果是切割的文件则显示实际切割的文件大小
     */
    private final DataSize dataSize;

    /**
     * 需要被关闭的任务
     */
    private final CloseTasks closeTasks;


    /**
     * @param filePath 文件路径
     * @param dataSize 底层写入数据时mmap的大小
     */
    public CommonFileWriter(Path filePath, DataSize dataSize) throws Exception {
        this(filePath, Collections.emptyList(), dataSize);
    }


    /**
     * @param filePath    文件路径
     * @param fileHeaders 文件headers
     * @param dataSize    文件大小
     */
    public CommonFileWriter(Path filePath, List<String> fileHeaders, DataSize dataSize) throws Exception {
        this.filePath = filePath;
        this.closeTasks = new CloseTasks("WriteFile@{}", filePath.getFileName().toString());
        this.fileChannel = FileChannel.open(filePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);
        //关闭fileChannel
        this.closeTasks.addTask(fileChannel::close, "{} file  will be closed", filePath);
        this.dataSize = dataSize;
        //计算映射单元的大小, 预留2KB的内存用于写入文件头
        this.mapUnitSize = (dataSize.isZero() ? MAX_CHUNK_FILE_SIZE :
                (int) (dataSize.toBytes() >>> BASE_SHIFT > MAX_CHUNK_SIZE ? MAX_CHUNK_FILE_SIZE : dataSize.toBytes())) + RESERVE_SIZE;
        //每个文件预留出2KB左右，用于避免buffer overflow 异常
        //从文件头开始进行mmap映射
        reset(0, mapUnitSize);
        //添加headers到文件中
        addHeaders(fileHeaders);

        //在关闭任务时，将数据刷入到磁盘中
        this.closeTasks.addFirst(() -> fileChannel.force(false), "{} file  will be force", filePath);
    }


    public void reset(long startPos, int mapSize) throws IOException {
        if (this.writeBuffer != null) {
            writeBuffer.close();
        }

        //之前的buffer会被回收，因为没有指针指向
        this.writeBuffer = new MappedBuffer(startPos, mapSize);
        //重置bufferPos的指针位置
        this.bufferPos.set(0);
    }


    /**
     * 写入文件头
     *
     * @param fileHeaders fileHeaders
     */
    private void addHeaders(List<String> fileHeaders) throws Exception {
        //写文件头
        ByteBuf buffer = PooledByteBufAllocator.DEFAULT.directBuffer(1024);
        try {
            fileHeaders.forEach(line -> buffer.writeCharSequence(line, StandardCharsets.UTF_8));
            writeLine(buffer.nioBuffer());
        } finally {
            ReferenceCountUtil.safeRelease(buffer);
        }
    }


    @Override
    public synchronized void writeLine(ByteBuffer byteBuffer) throws Exception {
        //当前buffer的指针
        long currentPos = bufferPos.get();
        //写入的位置
        long writePos = this.writePos.get();
        //如果本次写入的大小已经无法在当前的mmap中完成，则生成下个mmap文件
        if (currentPos + byteBuffer.remaining() >= mapUnitSize) {
            //重置writeBuffer
            reset(writePos, mapUnitSize);
        }

        //计算全文件写入位置
        this.writePos.addAndGet(byteBuffer.remaining());
        //计算当前buffer写入位置
        currentPos = this.bufferPos.getAndAdd(byteBuffer.remaining());
        ByteBuffer buffer = writeBuffer.slice();
        buffer.position((int) currentPos);
        buffer.put(byteBuffer);
    }

    @Override
    public void close() throws IOException {
        //截断文件至需要的位置
        this.closeTasks.addFirst(() -> fileChannel.truncate(writePos.get()), "{} file  will be truncated", filePath);
        //拿到最后一个writeBuffer 进行关闭
        this.closeTasks.addFirst(() -> writeBuffer.close(), "{} mapped buffer will be clean", filePath);
        this.closeTasks.close();
        //help gc
        this.writeBuffer = null;
    }

    @Override
    public Path filePath() {
        return filePath;
    }


    class MappedBuffer implements Closeable {

        /**
         * 具体使用的buffer对象
         */
        private MappedByteBuffer buffer;

        /**
         * mapped buffer reference
         */
        private final MappedByteBufferReference reference;


        /**
         * @param startPos 内存映射的起始位置
         * @param mapSize  内存映射的大小
         */
        public MappedBuffer(long startPos, int mapSize) throws IOException {
            this.buffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, startPos, mapSize);
            this.reference = new MappedByteBufferReference(startPos, mapSize, filePath, buffer, bufferQueue, references);
            //预加载部分内存，避免出现多次缺页中断，madvice并不一定会预加载内存
            //TODO: 是否需要考虑采用warmFile的方式对内存进行预热
            this.buffer.load();
        }

        public ByteBuffer slice() {
            return buffer.slice();
        }

        @Override
        public void close() throws IOException {
            //TODO: 是否在切换buffer时采用force的方法强制刷盘
            //将之前buffer中的数据进行刷盘，最后采用FileChannel#force同一刷入磁盘, force方法调用会增加耗时
//            buffer.force();
            DirectBufferUtils.safeRelease(buffer);
            //移除reference的强引用，当reference强应用没有被正常移除时可检测到内存泄漏
            //必须将close放到buffer=null之前，否则会误测出内存泄漏问题
            reference.close();
            //help gc
            this.buffer = null;
        }
    }


    /**
     * 通过reference来检测是否存在内存泄露，当jvm中有指向reference的强引用时，referenceQueue中会捕捉到需要被回收的
     * reference对象，并且通过{@link CommonFileWriter#references}即可检测到是否存在不正常关闭行为以及内存泄露问题
     * <p>
     * TODO: 是否将内存泄漏的检测移动到StreamDateFile内部，因为MappedByteBuffer在强引用被释放时会自动回收
     */
    private static class MappedByteBufferReference extends WeakReference<MappedByteBuffer> {

        /**
         * All references for mapped buffer, for check buffer is leaked
         */
        private final Set<MappedByteBufferReference> references;

        /**
         * 开始映射的位置
         */
        private final long startPos;

        /**
         * 映射的具体长度
         */
        private final int mapSize;

        /**
         * 文件路径
         */
        private final Path filePath;

        /**
         * @param startPos   map start offset position
         * @param mapSize    mapped size
         * @param filePath   The file path corresponding to mapped buffer
         * @param byteBuffer mapped buffer
         * @param queue      buffer reference queue
         * @param references check buffer reference is normal recycle
         */
        public MappedByteBufferReference(long startPos, int mapSize,
                                         Path filePath,
                                         MappedByteBuffer byteBuffer,
                                         ReferenceQueue<MappedByteBuffer> queue,
                                         Set<MappedByteBufferReference> references) {
            super(byteBuffer, queue);
            this.startPos = startPos;
            this.mapSize = mapSize;
            this.references = references;
            this.filePath = filePath;
            this.references.add(this);
        }

        /**
         * 清空reference 并且判断内存是否正常被回收
         */
        boolean dispose() {
            clear();
            return references.remove(this);
        }


        public void close() {
            clear();
            references.remove(this);
        }

    }

    /**
     * 用于检测是否存在内存泄漏
     */
    static class CheckBufferLeak extends AbstractTask {

        static final CheckBufferLeak INSTANCE = new CheckBufferLeak();

        public CheckBufferLeak() {
            super("check buffer leak");
        }


        @Override
        protected void runTask() {
            //检测内存是否存在泄露
            checkBuffer();
        }

        @Override
        protected void afterExecute(Exception e) {
            SCAN_TIMER.addTask(this, 1, TimeUnit.MINUTES);
        }

        /**
         * 查看是否存在内存泄露
         */
        void checkBuffer() {
            //检测是否存在内存泄露
            for (; ; ) {
                MappedByteBufferReference ref = (MappedByteBufferReference) bufferQueue.poll();
                if (ref == null) {
                    break;
                }

                if (!ref.dispose()) {
                    continue;
                }

                log.error("The {} mapped buffer is leaked, start position is {}, mapped size is {}", ref.filePath, ref.startPos, ref.mapSize);
            }
        }
    }


}
