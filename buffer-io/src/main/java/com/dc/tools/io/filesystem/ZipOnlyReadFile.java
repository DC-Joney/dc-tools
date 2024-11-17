package com.dc.tools.io.filesystem;

import com.dc.tools.common.SequenceUtil;
import com.dc.tools.common.utils.CloseTasks;
import com.dc.tools.common.utils.MethodInvoker;
import org.apache.commons.io.FilenameUtils;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dc.tools.io.filesystem.FileSystemUtil.newZipFileSystem;


/**
 * <p>
 * 用于只读的 ZipFileSystem，主要是对Zip内部文件进行读取
 * </p>
 *
 * @author zhangyang
 * @apiNote JAVA NIO 对于文件系统的定义:
 * <ul>
 * <li>1、在 NIO中 所有的{@link FileSystem} 都是通过{@link FileSystemProvider} 进行加载的，所有的{@code FileSystemProvider}
 * 都是通过{@link java.util.ServiceLoader }的，且都是单例的</li>
 * <li>2、在 windows环境下 {@code FileSystemProvider} 实现分别为 {@link sun.nio.fs.MacOSXFileSystemProvider}、{@link com.sun.nio.zipfs.ZipFileSystemProvider}
 * 与 {@link com.sun.nio.zipfs.JarFileSystemProvider}</li>
 * <li>3、FileSystem 都是由 {@code FileSystemProvider} 创建的，如果文件uri为 file 协议，则默认只有单例的FileSystem来代表当前的文件系统</li>
 * <li>4、由于Jar 包 或者是Zip 文件包是以多个存在的，所以在NIO中以 {@link com.sun.nio.zipfs.ZipFileSystem} 进行表示，
 * 每一个zip文件或者是一个jar包都是一个{@link com.sun.nio.zipfs.ZipFileSystem}，在{@link com.sun.nio.zipfs.ZipFileSystemProvider}
 * 内部使用{@link com.sun.nio.zipfs.ZipFileSystemProvider#filesystems}来存储 jar或者zip 到 ZipFileSystem的关系</li>
 * <li>5、当使用nio 来获取zip文件或者是jar文件的时候，首先要判断map中是否存在该jar 或者 zip对应的{@code FileSystem}，如果没有则进行创建即可</li>
 * <li>6、{@code Paths.get(String path)} api 就是从所有的Provider中找到对应协议的Provider然后找到对应的{@code FileSystem}再进行加载Path路径</li>
 * </ul>
 * <p>
 */
public class ZipOnlyReadFile implements Closeable {

    private static final String ROOT_PATH = "/";

    public static final String ZIP_EXTENSION = "zip";

    public static final Charset GBK_CHARSET = Charset.forName("GBK");

    private final Path zipFile;

    private final FileSystem fileSystem;

    private volatile boolean closed;

    /**
     * 当前zip文件的编码
     */
    private Charset charset;


    /**
     * 该构造器会自动判断当前zip文件的字符集编码
     *
     * @param zipFile zip文件路径
     */
    public ZipOnlyReadFile(String zipFile) throws IOException {
        this(Paths.get(zipFile));
    }


    public ZipOnlyReadFile(Path zipPath) throws IOException {
        assertFileExists(zipPath);
        this.zipFile = zipPath;
        this.fileSystem = autoCharsetFileSystem(zipFile);
    }

    public ZipOnlyReadFile(Path zipPath, Charset charset) throws IOException {
        this.zipFile = zipPath;
        assertFileExists(zipPath);
        this.fileSystem = newZipFileSystem(zipPath, charset);
    }

    private void assertFileExists(Path zipFile) throws IOException {
        String extension = FilenameUtils.getExtension(zipFile.toString());
        if (!extension.equals(ZIP_EXTENSION)) {
            throw new IllegalArgumentException("Cannot resolve file extension: " + extension);
        }

        if (Files.notExists(zipFile))
            throw new FileNotFoundException("Cannot find zip file for name: " + zipFile);
    }

    /**
     * 获取Zip文件对应的文件系统,会自动判断是UTF-8编码还是GBK编码
     *
     * @param zipFile zipFile文件名称
     */
    public FileSystem autoCharsetFileSystem(Path zipFile) throws IOException {
        Charset charset = StandardCharsets.UTF_8;
        FileSystem fileSystem = null;
        try {
            //这里优先使用UTF-8编码，在大多数情况下zip是支持UTF-8编码的
            fileSystem = newZipFileSystem(zipFile, charset);
            getChildPaths(fileSystem.getPath(ROOT_PATH)).collect(Collectors.toList());
            this.charset = StandardCharsets.UTF_8;
        } catch (Exception ex) {
            //如果抛这个异常表示当前的字符集编码不匹配则从新创建
            if (!ex.getMessage().contains("MALFORMED")) {
                throw ex;
            }

            try {
                //这里需要先删除对应的FileSystem,否则创建会报错
                fileSystem.close();
                //从新创建新的GBK字符编码的FileSystem
                fileSystem = newZipFileSystem(zipFile, GBK_CHARSET);
                //如果当前的编码也不支持GBK则直接抛出异常，不在进行尝试
                getChildPaths(fileSystem.getPath(ROOT_PATH)).collect(Collectors.toList());
                this.charset = GBK_CHARSET;
            } catch (IOException e) {
                fileSystem.close();
                throw e;
            }
        }

        return fileSystem;
    }

    private static void removeFileSystem(Path zipFile, FileSystem fileSystem) {
        try {

            MethodInvoker invoker = new MethodInvoker();
            invoker.setTargetObject(fileSystem.provider());
            invoker.setTargetMethod("removeFileSystem");
            invoker.setArguments(zipFile, fileSystem);
            invoker.setTargetClass(fileSystem.provider().getClass());
            invoker.prepare();
            invoker.invoke();
        } catch (Exception e) {
            throw new FileSystemRemoveException(e, "Cannot remove file system for {}", zipFile);
        }
    }


    /**
     * 返回一个用于读取filePath的只读{@linkplain FileChannel}
     *
     * @param filePath filePath 是针对于zip 文件的相对路径
     * @apiNote 这里会从zip文件中复制出一个新的文件，对新的文件进行读取操作，在特定场景下如果涉及到大量的io操作则可以使用这种方式实现，因为可以通过DirectByteBuffer或者是MappedByteBuffer来避免大量复制产生
     */
    public FileChannel newFileChannel(String filePath) throws IOException {
        Set<OpenOption> openOptions = Collections.singleton(StandardOpenOption.READ);
        return fileSystem.provider().newFileChannel(fileSystem.getPath(filePath), openOptions);
    }

    /**
     * 返回一个用于读取filePath的只读{@linkplain FileChannel}
     *
     * @param filePath filePath 是针对于zip 文件的相对路径
     * @apiNote 这里会从zip文件中复制出一个新的文件，对新的文件进行读取操作，在特定场景下如果涉及到大量的io操作则可以使用这种方式实现，因为可以通过DirectByteBuffer或者是MappedByteBuffer来避免大量复制产生
     */
    public FileChannel newFileChannel(Path filePath) throws IOException {
        Set<OpenOption> openOptions = Collections.singleton(StandardOpenOption.READ);
        return fileSystem.provider().newFileChannel(filePath, openOptions);
    }

    /**
     * <p>
     * 当前操作并不会从zip文件中复制新文件，底层依旧是通过{@linkplain java.nio.channels.Channels#newChannel(InputStream)} 的方式来读取数据
     * </p>
     *
     * <p>
     * <br/>
     * <h3>在非io频繁的情况下更建议使用{@linkplain ZipOnlyReadFile#newByteChannel(String)} 的方式来读取数据</h3>
     * </p>
     * <p>
     * 返回一个用于读取{@code filePath}的只读 {@link ByteChannel}
     *
     * @param filePath filePath 是针对于zip 文件的相对路径
     */
    public SeekableByteChannel newByteChannel(String filePath) throws IOException {
        Set<OpenOption> openOptions = Collections.singleton(StandardOpenOption.READ);
        Path path = fileSystem.getPath(filePath);
        return fileSystem.provider().newByteChannel(path, openOptions);
    }

    /**
     * <p>
     * 当前操作并不会从zip文件中复制新文件，底层依旧是通过{@linkplain java.nio.channels.Channels#newChannel(InputStream)} 的方式来读取数据
     * </p>
     *
     * <p>
     * <br/>
     * <h3>在非io频繁的情况下更建议使用{@linkplain ZipOnlyReadFile#newByteChannel(String)} 的方式来读取数据</h3>
     * </p>
     * <p>
     * 返回一个用于读取{@code filePath}的只读 {@link ByteChannel}
     *
     * @param filePath filePath 是针对于zip 文件的相对路径
     */
    public SeekableByteChannel newByteChannel(Path filePath) throws IOException {
        Set<OpenOption> openOptions = Collections.singleton(StandardOpenOption.READ);
        return fileSystem.provider().newByteChannel(filePath, openOptions);
    }

    /**
     * 返回基于 filePath 的Path对象
     *
     * @param filePath filePath 是针对于zip 文件的相对路径
     */
    public Path getPath(String filePath) {
        return fileSystem.getPath(filePath);
    }

    /**
     * 获取当前 parentPath 的所有子文件，不包括 directions
     *
     * @param parentPath zip 压缩包中的父目录文件
     */
    public Stream<Path> getChildFiles(Path parentPath) throws IOException {
        if (Files.isDirectory(parentPath)) {
            return Files.walk(parentPath, 1)
                    .filter(path -> !path.equals(parentPath))
                    .filter(Files::isRegularFile);
        }

        return Stream.empty();
    }


    /**
     * 获取当前 parentPath 的所有子目录以及文件
     *
     * @param parentPath zip 压缩包中的父目录文件
     */
    public Stream<Path> getChildPaths(Path parentPath) throws IOException {
        if (Files.isDirectory(parentPath)) {
            return Files.walk(parentPath, 1)
                    .filter(path -> !path.equals(parentPath))
                    .filter(path -> !path.toString().equals(ROOT_PATH));
        }

        return Stream.empty();
    }

    /**
     * 获取当前 parentPath 的所有子目录以及文件
     */
    public Stream<Path> getRootDirectories() throws IOException {
        return Files.walk(getPath(ROOT_PATH), 1)
                .filter(path -> !path.toString().equals(ROOT_PATH));
    }


    /**
     * 返回当前zip文件对应的FileSystem对象
     *
     * @apiNote 当不使用当前FileSystem对象时应该将其关闭，避免内部使用的内存一直不释放
     */
    public FileSystem getFileSystem() {
        if (this.closed) {
            return null;
        }

        return fileSystem;
    }


    /**
     * 返回当前 zip压缩包中目标文件的属性
     *
     * @param filePath 以 zip文件为相对路径的 filePath
     */
    public BasicFileAttributes getFileAttributes(Path filePath) throws IOException {
        return fileSystem.provider().readAttributes(filePath, BasicFileAttributes.class);
    }

    /**
     * 判断当前的path 是否是文件
     *
     * @param filePath 以 zip文件为相对路径的 filePath
     */
    public boolean isFile(Path filePath) throws IOException {
        return Files.isRegularFile(filePath);
    }

    /**
     * 获取当前的zip文件名称
     */
    public String getZipFileName() {
        return zipFile.toString();
    }

    /**
     * 将zip文件转为特定的字符集的zip压缩文件
     *
     * @param charset 字符集
     */
    public Path toCharsetZip(Charset charset) {
        return toCharsetZip(zipFile.getParent(), charset);
    }


    /**
     * 将zip文件转为特定的字符集的zip压缩文件
     *
     * @param charset    字符集
     * @param parentPath 转换后的zip文件存储的父级目录
     * @apiNote 目前仅支持UTF-8或者是GBK编码
     */
    public Path toCharsetZip(Path parentPath, Charset charset, boolean copyOld) {
        //创建新的zip文件
        Path newZipPath = parentPath.resolve(SequenceUtil.nextIdString() + "." + ZIP_EXTENSION);
        //创建压缩文件
        try (CloseTasks closeTasks = new CloseTasks()) {
            //如果charset是一致的则不做任何操作
            if (this.charset.displayName().equals(charset.displayName())) {
                //将当前的zip文件进行复制
                return copyOld ? Files.copy(zipFile, newZipPath) : zipFile;
            }

            ParallelZip parallelZip = new ParallelZip(newZipPath, ROOT_PATH, charset, true, ParallelZip.Options.PARALLEL);
            closeTasks.addTask(parallelZip::close, "Close parallel zip files: {}", zipFile);

            List<ParallelZip.ZipArchivePath> rootDirectories = getRootDirectories()
                    .map(path -> new ParallelZip.ZipArchivePath(path, path))
                    .collect(Collectors.toList());

            parallelZip.addPaths(rootDirectories);
        } catch (Exception e) {
            throw new ZipCopyException(e,
                    "Cannot copy zip file {} to new zip, source charset is {}, destination charset is {}, cause is:",
                    zipFile, this.charset, charset);
        }

        return newZipPath;
    }

    /**
     * 将zip文件转为特定的字符集的zip压缩文件
     *
     * @param charset    字符集
     * @param parentPath 转换后的zip文件存储的父级目录
     * @apiNote 目前仅支持UTF-8或者是GBK编码
     */
    public Path toCharsetZip(Path parentPath, Charset charset) {
        return toCharsetZip(parentPath, charset, false);
    }

    /**
     * 将zip文件转为特定的字符集的zip压缩文件
     *
     * @param charset 字符集
     * @param copyOld 如果压缩文件的字符集与当前传入的字符集相符时是返回原文件还是复制一个新的文件
     * @apiNote 目前仅支持UTF-8或者是GBK编码
     */
    public Path toCharsetZip(Charset charset, boolean copyOld) {
        return toCharsetZip(zipFile.getParent(), charset, copyOld);
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
        fileSystem.close();
    }


}
