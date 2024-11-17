package com.dc.tools.io.filesystem;

import com.dc.tools.common.annotaion.JustForTest;
import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * This class creates zip archives. Instead of directly using {@link java.util.zip.ZipOutputStream},
 * this implementation uses the jar {@link FileSystem} available since Java 1.7.<p>
 * The advantage of using a {@code FileSystem} is that it can easily be processed in parallel.<p>
 * This class can create zip archives with parallel execution by combining parallel {@link Stream} processing
 * with the jar {@code FileSystem}.<p>
 * This class has a {@link #main(String[])} method which emulates a minimal command-line zip utility, i.e.
 * it can be used to create standard zip archives.
 *
 * @author Lennart Börjeson
 * @author zy
 * @apiNote change from original code
 */
@Beta
@Slf4j
public class ParallelZip implements Closeable {

    /**
     * 被压缩文件的Zip FileSystem
     */
    private final FileSystem zipArchive;


    /**
     * 是否是并行压缩
     */
    private final boolean parallel;


    /**
     * base目录，在特定情况下我们希望压缩的文件路径只是我们所需要的，举个例子:
     * <p>
     * 假设我们压缩的目录为 /mnt/data/images、/mnt/data/pages,但是我们在压缩目录内只想包含/images以及images目录下的所有文件
     * 我们就需要将baseDirectory设置为 /mnt/data, 那么压缩出来的目录就为 images, pages
     * </p>
     */
    private final String baseDirectory;

    /**
     * Creates and initialises a Zip archive.
     *
     * @param zipPath need to zip path name
     * @param options {@link Options}
     * @throws IOException        Thrown on any underlying IO errors
     * @throws URISyntaxException Thrown on file name syntax errors.
     * @deprecated baseDirectory 该属性已经失效
     */
    public ParallelZip(Path zipPath, String baseDirectory, Charset charset, boolean delete, Options options) throws IOException, URISyntaxException {
        this.parallel = options == Options.PARALLEL;

        //TODO: 已废弃
        this.baseDirectory = baseDirectory.endsWith("/") ? baseDirectory.substring(0, baseDirectory.length() - 1) : baseDirectory;

        //将zip文件删除
        if (delete) {
            Files.deleteIfExists(zipPath);
        }

        this.zipArchive = FileSystemUtil.newZipFileSystem(zipPath, charset);

    }

    /**
     * Creates and initialises a Zip archive.
     *
     * @param archiveName name (file path) of the archive
     * @param options     {@link Options}
     * @throws IOException        Thrown on any underlying IO errors
     * @throws URISyntaxException Thrown on file name syntax errors.
     */
    public ParallelZip(String archiveName, Charset charset, boolean delete, Options options) throws IOException, URISyntaxException {
        this(Paths.get(archiveName), "", charset, delete, options);
    }

    /**
     * Creates and initialises a Zip archive.
     *
     * @param archiveName name (file path) of the archive
     * @param options     {@link Options}
     * @throws IOException        Thrown on any underlying IO errors
     * @throws URISyntaxException Thrown on file name syntax errors.
     */
    public ParallelZip(String archiveName, Charset charset, Options options) throws IOException, URISyntaxException {
        this(Paths.get(archiveName), "", charset, true, options);
    }

    /**
     * Creates and initialises a Zip archive.
     *
     * @param archiveName name (file path) of the archive
     * @throws IOException        Thrown on any underlying IO errors
     * @throws URISyntaxException Thrown on file name syntax errors.
     */
    public ParallelZip(String archiveName, Charset charset) throws IOException, URISyntaxException {
        this(archiveName, charset, Options.RECURSIVE);
    }

    /**
     * Adds one file to the archive.
     *
     * @param f Path of file to add, not null
     */
    public void zipOneFile(final ZipArchivePath archivePath) {
        try {


            Path srcPath = archivePath.srcPath;
            Path compressionPath = archivePath.compressionPath;


//            //TODO: 这里需要进行优化，是否应该屏蔽掉baseDirectory，而是在传入路径时就进行拼接
//            //这里需要对路径做特殊处理
//            String parentPath = f.getParent().toString();
//            //将base路径设置为空
//            String zipParent = parentPath.replace(baseDirectory, "");
            Path parent = compressionPath.getParent();
            //在zip文件中创建对应的目录
            if (parent.getNameCount() > 0)
                Files.createDirectories(zipArchive.getPath(parent.toString()));

            //构造zip文件中的路径
            final Path zipEntryPath = zipArchive.getPath(compressionPath.toString());
            //删除对应的zip文件
            String message = " adding: %s";
            if (Files.exists(zipEntryPath)) {
                Files.deleteIfExists(zipEntryPath);
                message = " updating: %s";
            }

            StringBuilder logBuffer = new StringBuilder();

//            HashSet<StandardOpenOption> openOptions = Sets.newHashSet(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);

//            try (CloseTasks closeTasks = new CloseTasks()) {
//                logBuffer.append(String.format(message, f));
//                FileChannel destChannel = zipArchive.provider().newFileChannel(zipEntryPath, openOptions);
//                closeTasks.addTask(destChannel::close, "Close destination channel");
//                FileChannel srcChannel = f.getFileSystem().provider().newFileChannel(f, Collections.singleton(StandardOpenOption.READ));
//                ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) srcChannel.size());
//                closeTasks.addTask(()-> DirectBufferUtils.release(byteBuffer), "");
//                srcChannel.read(byteBuffer, 0);
//                byteBuffer.flip();
//                destChannel.write(byteBuffer, 0);
//                closeTasks.addTask(srcChannel::close, "Close src channel");
////                srcChannel.transferTo(0, srcChannel.size(), destChannel);
////                srcChannel.map(FileChannel.MapMode.READ_ONLY,0,srcChannel.size());
//            } catch (Exception e) {
//                log.error("Error adding %s, cause is: {}", f, e);
//                return;
//            }

            //对文件进行复制
            try (OutputStream out = Files.newOutputStream(zipEntryPath)) {
                logBuffer.append(String.format(message, srcPath));
                Files.copy(srcPath, out);
                out.flush();
            } catch (Exception e) {
                log.error("Error adding %s, cause is: {}", srcPath, e);
                return;
            }

            final long size = (long) Files.getAttribute(zipEntryPath, "zip:size");
            final long compressedSize = (long) Files.getAttribute(zipEntryPath, "zip:compressedSize");
            final double compression = (size - compressedSize) * 100.0 / size;
            final int method = (int) Files.getAttribute(zipEntryPath, "zip:method");
            final String methodName = method == 0 ? "stored" : method < 8 ? "compressed" : "deflated";
            logBuffer.append(String.format(" (%4$s %3$.0f%%)", size, compressedSize, compression, methodName));
            System.out.println(logBuffer);
        } catch (Exception e1) {
            throw new ParallelZipException(String.format(" Error accessing zip archive for %s:", archivePath.srcPath), e1);
        }
    }

    @Override
    public void close() throws IOException {
        zipArchive.close();
    }


    /**
     * 需要被压缩的文件以及文件夹包装
     * <p></p>
     * <p>
     * 假设我们压缩的目录为 /mnt/data/images, 但是我们在压缩目录内只想包含/images以及images目录下的所有文件，我们就需要将
     * <ul>
     *     <li>src设置为 /mnt/data/images </li>
     *     <li>parentPath设置为 /images</li>
     * </ul>
     * <p>
     * 那么我们压缩出来的目录就为 /images/**
     * </p>
     */
    public static class ZipArchivePath {

        /**
         * 需要被压缩的文件路径
         */
        private final Path srcPath;

        /**
         * 被压缩的文件对应的压缩包中的文件路径
         */
        private final Path parentPath;

        /**
         * 根据文件计算出src文件在压缩文件中对应的文件名称
         */
        Path compressionPath;


        public ZipArchivePath(Path srcPath, Path parentPath) {
            this.srcPath = srcPath;
            this.parentPath = parentPath;
        }

        ZipArchivePath(Path srcPath, Path parentPath, Path compressionPath) {
            this.srcPath = srcPath;
            this.parentPath = parentPath;
            this.compressionPath = compressionPath;
        }

        ZipArchivePath resolveCompressionFile() {
            this.compressionPath = parentPath.resolve(srcPath);
            return new ZipArchivePath(srcPath, parentPath, compressionPath);
        }

        ZipArchivePath resolveChildFile(Path childFile) {
            String childPath = childFile.toString();
            String parentPath = srcPath.toString();
            String searchPath = childPath.replace(parentPath, "");
            searchPath = searchPath.startsWith("/") ? searchPath.substring(1) : searchPath;
            Path compressionPath = this.parentPath.resolve(searchPath);
            return new ZipArchivePath(childFile, this.parentPath, compressionPath);
        }


        ZipArchivePath pathNormalize() {
            return new ZipArchivePath(srcPath.normalize(), parentPath, compressionPath.normalize());
        }
    }


    public void addPaths(List<ZipArchivePath> paths) {
        final List<ZipArchivePath> expandedPaths =
                paths.stream()            // Process file name list
                        .flatMap(this::filesWalk)        // Find file, or, if recursive, files
                        .map(ZipArchivePath::pathNormalize)            // Ensure no contrived paths
                        .collect(toList());                // Collect into List! NB! Necessary!

        // Do NOT remove the collection into a List!
        // Doing so can defeat the desired parallelism.
        // By first resolving all directory traversals,
        // we ensure all files will be processed in parallel in the next step.
        // (This is because the directory traversal parallelises
        // badly, whereas the contents of a list does eminently so.)

        // If parallel processing requested, use parallel stream,
        // else use normal stream.
        final Stream<ZipArchivePath> streamOfPaths =
                parallel ? expandedPaths.parallelStream() : expandedPaths.stream();

        streamOfPaths.forEach(this::zipOneFile);

    }

    /**
     * If the given {@link File} argument represents a real file (i.e.
     * {@link File#isFile()} returns {@code true}), converts the given file
     * argument to a {@link Stream} of a single {@link Path} (of the given file
     * argument).
     * <p>
     * Else, if {@link Options#RECURSIVE} was specified in the constructor
     * {@link ParallelZip#ParallelZip(String, Charset)}, assumes the file represents a directory and then uses
     * {@link Files#walk(Path, FileVisitOption...)} to return a
     * {@code Stream} of all real files contained within this directory tree.
     * <p>
     * Returns an empty stream if any errors are encountered.
     *
     * @param f File, representing a file or directory.
     * @return Stream of all Paths resolved
     */
    private Stream<ZipArchivePath> filesWalk(ZipArchivePath archivePath) {

        // If argument is a file, return directly as single-item stream
        if (Files.isRegularFile(archivePath.srcPath)) {
            return Stream.of(archivePath.resolveCompressionFile());
        }

        // Check if argument is a directory and RECURSIVE option specified
        if (Files.isDirectory(archivePath.srcPath))
            try {
                // Traverse directory and return all files found
                return Files.walk(archivePath.srcPath, FileVisitOption.FOLLOW_LINKS)
                        .filter(Files::isRegularFile)
                        .map(archivePath::resolveChildFile); // Only return real files
            } catch (IOException e) {
                throw new ParallelZipException(String.format("Error traversing directory %s", archivePath.srcPath), e);
            }

        // Argument is neither file nor directory: Return empty stream
        return Stream.empty();
    }

    /**
     * Represents Zip processing options. (Internal to this application; not needed by the jar FileSystem.)
     *
     * @author Lennart Börjeson
     */
    public enum Options {
        /**
         * Requests that all file additions should be executed in parallel.
         */
        PARALLEL,
        /**
         * Requests that any directory specified as input should be
         * recursively traversed and all files found added individually to the
         * Zip archive. Paths will be preserved.
         */
        RECURSIVE;

    }

    //压缩普通文件
    @JustForTest
    public static void compressUniversal() {

        ArrayList<ZipArchivePath> paths = Lists.newArrayList(
                new ZipArchivePath(Paths.get("/Users/zy/启蒙智慧创作绘本（双语）4册/归档/商界-2019-01英文"), Paths.get("/商界-2019-01英文")),
                new ZipArchivePath(Paths.get("/Users/zy/启蒙智慧创作绘本（双语）4册/归档/商界-2019-01中文"), Paths.get("/商界-2019-01中文"))
        );

        try (ParallelZip parallelZip = new ParallelZip("/Users/zy/启蒙智慧创作绘本（双语）4册/归档5.zip", ZipOnlyReadFile.GBK_CHARSET)) {
            parallelZip.addPaths(paths);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {

        compressUniversal();

        try (ZipOnlyReadFile onlyReadFile = new ZipOnlyReadFile("/Users/zy/启蒙智慧创作绘本（双语）4册/归档.zip")) {
            Path gbk = onlyReadFile.toCharsetZip(ZipOnlyReadFile.GBK_CHARSET);
            System.out.println(gbk);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }


}
