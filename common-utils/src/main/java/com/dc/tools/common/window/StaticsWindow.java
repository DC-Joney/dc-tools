package com.dc.tools.common.window;

import cn.hutool.core.lang.Assert;
import com.dc.tools.common.utils.SystemClock;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * 用于统计的滑动窗口实现，只统计数值
 *
 * @author zy
 */
public class StaticsWindow {

    /**
     * 开始时间
     */
    private final long startTime;

    /**
     * 每个窗口的间隔时间
     */
    private final int windowInterval;

    /**
     * 用于存储窗口数据
     */
    private final AtomicLongArray array;

    /**
     * mask 用于计算对应的窗口index
     */
    private final int mask;

    /**
     * 高21 bit用于存储count数量，低 41bit存储时间，最后1bit用于作为更新标识
     */
    private static final int BASE_SHIFT = 42;

    private static final long MAGIC = ~(-1L << BASE_SHIFT);

    /**
     * window窗口的总大小，必须为power2
     */
    private final int windowTime;

    private static final int MAX_MAGIC = ~(-1 << 16);

    /**
     * @param windowInterval 窗口的时间间隔
     * @param windowSize     窗口的大小
     */
    public StaticsWindow(int windowInterval, int windowSize) {
        Assert.isTrue(windowInterval <= MAX_MAGIC, "windowInterval must be <= {}", MAX_MAGIC);
        windowSize = power2(windowSize);
        Assert.isTrue(windowSize <= MAX_MAGIC, "windowSize must be <= {}", MAX_MAGIC);
        //为了避免第一个窗口的初始值为0与实际计算冲突，所有默认开始时间为创建时间 - windowInterval
        //保证第一个窗口的开始时间一定 >= windowInterval
        this.startTime = SystemClock.now() - windowInterval;
        this.windowInterval = windowInterval;
        this.array = new AtomicLongArray(windowSize);
        this.mask = array.length() - 1;
        this.windowTime = windowInterval * array.length();
    }


    StaticsWindow(int windowInterval, int windowSize, long startTime, long[] windowValues) {
        this.windowInterval = windowInterval;
        this.array = new AtomicLongArray(windowSize);
        for (int i = 0; i < windowValues.length; i++) {
            array.set(i, windowValues[i]);
        }

        this.mask = windowSize - 1;
        this.startTime = startTime;
        this.windowTime = windowInterval * array.length();
    }

    /**
     * 获取当前窗口的数值
     *
     * @param nowTime 当前时间
     */
    public long currentWindow(long nowTime) {
        //计算间隔时间
        long deltaTime = nowTime - startTime;
        //计算窗口index
        int index = (int) (deltaTime / windowInterval & mask);
        //计算窗口的开始时间
        long startTime = deltaTime - deltaTime % windowInterval;
        for (; ; ) {
            //获取对应的窗口
            long window = array.get(index);
            //如果window == 0表示还未初始化
            if (window == 0) {
                //通过cas进行初始化
                if (array.compareAndSet(index, 0, startTime << 1)) {
                    break;
                } else {
                    Thread.yield();
                }
            }
            //如果窗口的开始时间 == 实际计算出的开始时间则认为是同一个窗口
            else if ((window & MAGIC) >> 1 == startTime) {
                break;
            }
            //这里 < startTime 表示为窗口过期
            else if ((window & MAGIC) >> 1 < startTime) {
                //如果低1bit == 0表示当前正在更新，避免后面执行的cas浪费资源
                if ((window & 1) == 0 && array.compareAndSet(index, window, window | 1)) {
                    //将窗口的时间更新为最新时间
                    array.set(index, startTime << 1);
                    break;
                } else {
                    Thread.yield();
                }
            }
        }


        return array.get(index);

    }

    /**
     * 获取对应的窗口index
     *
     * @param nowTime 当前时间
     */
    public int getIndex(long nowTime) {
        long deltaTime = nowTime - startTime;
        return (int) (deltaTime / windowInterval & mask);
    }

    /**
     * 获取当前窗口的数量
     *
     * @param nowTime 当前时间
     */
    public int getCount(long nowTime) {
        return (int) (currentWindow(nowTime) >>> BASE_SHIFT);
    }

    /**
     * 获取总的数量统计
     */
    public int sumCount() {
        long count = 0;
        long nowTime = SystemClock.now();
        for (int i = 0; i < array.length(); i++) {
            long window = array.get(i);
            if (!isDep(window, nowTime)) {
                count += (window >>> BASE_SHIFT);
            }
        }

        return (int) count;

    }

    /**
     * 判断窗口是否过期
     *
     * @param window  具体的窗口值
     * @param nowTime 当前时间
     */
    public boolean isDep(long window, long nowTime) {
        long windowStartTime = (window & MAGIC) >> 1;
        long deltaTime = nowTime - windowInterval - startTime;
        return deltaTime - windowStartTime > windowTime;
    }


    /**
     * 添加count
     *
     * @param nowTime 当前时间
     * @return 返回当前窗口统计的count值
     */
    public int addCount(long nowTime, int count) {
        int index = getIndex(nowTime);
        for (; ; ) {
            //获取当前窗口数值
            long window = currentWindow(nowTime);
            //保留最后1bit，避免并发溢出
            if (window < 0) {
                break;
            }
            //更新count值
            if (array.compareAndSet(index, window, window + (count * (MAGIC + 1)))) {
                break;
            } else {
                Thread.yield();
            }

        }


        return (int) (currentWindow(nowTime) >>> BASE_SHIFT);
    }


    static int power2(int h) {
        return 1 << -(Integer.numberOfLeadingZeros(h - 1));
    }


    /**
     * 这里直接计算byte即可，因为规定了窗口size为2的指数
     *
     * @param arraySize 窗口数组大小
     */
    static byte[] getBits(int arraySize) {
        int byteArraySize = arraySize >>> 3;
        return new byte[byteArraySize];
    }

    /**
     * 用于序列化到本地磁盘
     * <p>
     * 正常如果是16个窗口磁盘存储为1kb左右，存储30W的滑动窗口大约为350M左右，这里还包括存储了过期窗口<br/>
     * 存储并不针对短期的滑动窗口，而是针对长期而言的，比如对于比较重要的场景可能不可以丢失窗口的数据<br/>
     * 正常情况下对于目标数据的统计是可以丢失的，因为并没那么重要，所以不需要存储到磁盘
     */
    public ByteBuffer dump() {
        long nowTime = System.currentTimeMillis();
        //创建bit数组
        byte[] bits = getBits(array.length());
        //shift偏移量
        int bitShift = 3;
        //bit mask
        int mask = ~(-1 << bitShift);
        //用于存放有效的window 索引
        List<Short> arrayIndexes = new ArrayList<>();
        // 因为每个窗口都需要64bit进行存储，可以采用bitmap的方式将无效的窗口直接抛弃掉
        // 并且无效将窗口设置为0，可以避免存储无效数据，通过计算bit index的方式将对应的窗口数据恢复到目标index
        for (int index = 0; index < array.length(); index++) {
            long window = array.get(index);
            //窗口的开始时间
            long windowStartTime = (window & MAGIC) >> 1;
            //计算新的间隔时间
            long deltaTime = nowTime - windowInterval - startTime;
            //如果窗口依然有效则将窗口数值添加到array,否则直接不要,因为需要重新赋值时间,没必要一直使用旧时间
            if (deltaTime - windowStartTime <= windowTime) {
//                dataBuffer.putLong(window);
                arrayIndexes.add((short) index);
                //计算bit数组index
                int bitArrayIndex = index >>> bitShift;
                //计算bitIndex
                int bitIndex = bitArrayIndex & mask;
                //将对应的bit位设置为1
                bits[bitArrayIndex] |= 1 << bitIndex;
            }

        }

        //计算需要存储的大小
        // 64bit 时间
        // 64 * window = 所有window占用的总字节数
        // 32bit window的信息
        // 16 bit bits 数组的大小
        int storeSize = Long.SIZE * (arrayIndexes.size() + 1) + Integer.SIZE + bits.length * Byte.SIZE + Short.SIZE;
        ByteBuffer dataBuffer = ByteBuffer.allocateDirect(storeSize);
        //存放开始时间
        dataBuffer.putLong(startTime);
        //添加滑动窗口信息
        dataBuffer.putInt(windowInterval << 16 | array.length());
        //byte数组最大为8192，实际根本不肯能用这么多
        dataBuffer.putShort((short) bits.length);
        //存放索引数组
        dataBuffer.put(bits);
        //将所有的window添加到buffer中
        for (Short arrayIndex : arrayIndexes) {
            dataBuffer.putLong(array.get(arrayIndex));
        }

        dataBuffer.flip();
        return dataBuffer;
    }

    /**
     * 加载磁盘数据创建新的滑动窗口
     *
     * @param dataBuffer 磁盘的buffer
     */
    public static StaticsWindow restore(ByteBuffer dataBuffer) {
        //获取当前时间
        long nowTime = SystemClock.now();
        //获取开始时间
        long startTime = dataBuffer.getLong();
        //获取窗口的间隔时间与窗口大小
        int w = dataBuffer.getInt();
        int windowInterval = w >> 16;
        int windowSize = w & MAX_MAGIC;

        short byteArraySize = dataBuffer.getShort();
        byte[] bitsArray = new byte[byteArraySize];
        //获取bit array数组
        dataBuffer.get(bitsArray);


        int bitShift = 3;
        int mask = ~(-1 << bitShift);

        //存储窗口的所有数据
        long[] windowValues = new long[windowSize];
        //整个窗口的时间
        int windowTime = windowInterval * windowSize;
        for (int i = 0; i < windowSize; i++) {
            int arrayIndex = i >>> bitShift;
            int bitIndex = i & mask;
            //如果对应的bit位置为0，表示当前窗口是无效的，则继续循环
            if ((bitsArray[arrayIndex] & 1 << bitIndex) == 0) {
                continue;
            }

            long window = dataBuffer.getLong();
            //窗口的开始时间
            long windowStartTime = (window & MAGIC) >> 1;
            //计算新的间隔时间
            long deltaTime = nowTime - windowInterval - startTime;
            //如果窗口依然有效则将窗口数值添加到array,否则直接不要,因为需要重新赋值时间,没必要一直使用旧时间
            if (deltaTime - windowStartTime <= windowTime) {
                windowValues[i] = window;
            }
        }

        return new StaticsWindow(windowInterval, windowSize, nowTime, windowValues);
    }


    public static void main(String[] args) throws InterruptedException {


        StaticsWindow cacheLeapWindow = new StaticsWindow(1000, 4);
        ExecutorService executorService = Executors.newCachedThreadPool();

        for (int i = 0; i < 2; i++) {
            executorService.execute(() -> {
                for (int h = 0; h < 5; h++) {
                    cacheLeapWindow.addCount(SystemClock.now(), 1);
                    try {
                        TimeUnit.MILLISECONDS.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        TimeUnit.MILLISECONDS.sleep(7200);
        System.out.println(cacheLeapWindow.sumCount());


    }

}
