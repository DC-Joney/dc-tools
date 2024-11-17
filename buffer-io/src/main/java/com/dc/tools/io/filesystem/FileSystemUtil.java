package com.dc.tools.io.filesystem;

import cn.hutool.core.util.StrUtil;
import com.dc.tools.common.utils.StringUtils;
import com.google.common.collect.Maps;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.List;

/**
 * 用于创建zip对应的{@link FileSystem}
 *
 * @author zy
 */
@UtilityClass
public class FileSystemUtil {

    private static final String BASE_SCHEMA_PREFIX = "jar:file:";
    private static final String BASE_PATH_SEPARATOR = "/";

    public static final String ZIP_EXTENSION = "zip";

    static {
        //加载所有的FileSystemProvider，其中主要是加载对于操作zip文件的FileSystemProvider
        FileSystemProvider.installedProviders();
    }

    public static void assertZipFileExists(Path zipFile) throws IOException {
        String extension = FilenameUtils.getExtension(zipFile.toString());
        if (!extension.equals(ZIP_EXTENSION)) {
            throw new IllegalArgumentException("Cannot resolve file extension: " + extension);
        }

        if (Files.notExists(zipFile))
            throw new FileNotFoundException("Cannot find zip file for name: " + zipFile);
    }


    /**
     * 获取Zip文件对应的文件系统
     *
     * @param zipFile zipFile文件名称
     */
    public static FileSystem newZipFileSystem(Path zipFile, Charset charset) throws IOException {
        String zipPath = getZipPath(zipFile.toAbsolutePath().toString());
        //判断ZipFileSystemProvider是否在文件中
        return registerFileSystem(URI.create(zipPath), charset);
    }

    /**
     * 注册基于Jar包的FileSystem
     *
     * @param jarFileUri jar文件
     */
    public static FileSystem registerFileSystem(URI jarFileUri, Charset charset) throws IOException {

        List<FileSystemProvider> providers = FileSystemProvider.installedProviders();
        FileSystemProvider systemProvider = null;

        for (FileSystemProvider provider : providers) {
            if (provider.getScheme().equals(jarFileUri.getScheme())) {
                systemProvider = provider;
                break;
            }
        }

        if (systemProvider == null)
            throw new ProviderNotFoundException(StrUtil.format("Cannot support provider for {}", jarFileUri.getScheme()));

        FileSystem fileSystem;

        try {
            fileSystem = systemProvider.getFileSystem(jarFileUri);
        } catch (FileSystemNotFoundException ex) {

            //see: http://www.docjar.com/html/api/com/sun/nio/zipfs/ZipFileSystem.java.html, line:79
            HashMap<String, Object> environment = Maps.newHashMap();
            environment.put("create", "true");

            // 如果不使用临时文件，那么在读取数据时会使用一个ByteArrayOutputStream来缓存数据，导致堆内存占用过多
            //see: http://www.docjar.com/html/api/com/sun/nio/zipfs/ZipFileSystem.java.html, line: 1358,1362
            //see: https://stackoverflow.com/questions/23858706/zipping-a-huge-folder-by-using-a-zipfilesystem-results-in-outofmemoryerror
            environment.put("useTempFile", Boolean.TRUE);

            //设置读取压缩文件的字符集
            //see com.sun.nio.zipfs.ZipFileSystem.ZipFileSystem
            environment.put("encoding", charset.displayName());

            fileSystem = systemProvider.newFileSystem(jarFileUri, environment);
        }

        return fileSystem;
    }


    /**
     * 获取zip文件的路径
     *
     * @param zipFile zip文件
     */
    public static String getZipPath(String zipFile) {
        if (zipFile.startsWith(BASE_PATH_SEPARATOR)) {
            return BASE_SCHEMA_PREFIX + zipFile;
        }

        String zipFilePath = StringUtils.replace(zipFile, "\\", "/");
        return BASE_SCHEMA_PREFIX + BASE_PATH_SEPARATOR + zipFilePath;
    }

}
