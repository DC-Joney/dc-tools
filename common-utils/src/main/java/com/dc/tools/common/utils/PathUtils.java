package com.dc.tools.common.utils;

import com.dc.tools.common.SequenceUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.List;

/**
 * 获取文件路径
 *
 * @author zhangyang
 * @apiNote JAVA NIO 对于文件系统的定义:
 *
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
 * @date 2020-10-15
 */
@Slf4j
@UtilityClass
public class PathUtils {

    public static final String PREFIX = "/BOOT-INF/classes";

    /**
     * 获取路径地址
     *
     * @param resourceLocation 文件相对路径
     */
    public Path getPathUnchecked(String resourceLocation) {

        try {

            ClassLoader loader = ClassUtils.getDefaultClassLoader();
            if (loader == null)
                loader = ClassLoader.getSystemClassLoader();

            URL fileURL = loader.getResource("");
            Assert.notNull(fileURL, "Cannot find system resource path");
            if (!ResourceUtils.isJarURL(fileURL)) {
                if (resourceLocation.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
                    return Paths.get(ResourceUtils.getURL(resourceLocation).toURI());
                }

                if (resourceLocation.startsWith("/"))
                    resourceLocation = resourceLocation.substring(1);

                URL resourceURI = loader.getResource(resourceLocation);
                Assert.notNull(resourceURI, "Cannot find resource path");
                return Paths.get(resourceURI.toURI());
            }
            FileSystem fileSystem = registerFileSystem(fileURL.toURI());
            return fileSystem.getPath(PREFIX, resourceLocation);

        } catch (Exception ex) {
            log.error("Get resource error, cause is: ", ex);
            throw new InvalidPathException(resourceLocation, "获取文件出错,请联系管理员");
        }

    }


    public String generateName(Path path) {
        String extension = FilenameUtils.getExtension(path.toString());
        return SequenceUtil.nextIdString() + "." + extension;
    }


    /**
     * 注册基于Jar包的FileSystem
     *
     * @param jarFileUri jar文件
     */
    private FileSystem registerFileSystem(URI jarFileUri) throws Exception {

        List<FileSystemProvider> providers = FileSystemProvider.installedProviders();
        FileSystemProvider systemProvider = null;

        for (FileSystemProvider provider : providers) {
            if (provider.getScheme().equals(jarFileUri.getScheme())) {
                systemProvider = provider;
                break;
            }
        }

        if (systemProvider == null)
            throw new ProviderNotFoundException("Cannot support provider {}");

        FileSystem fileSystem;

        try {
            fileSystem = systemProvider.getFileSystem(jarFileUri);

        } catch (FileSystemNotFoundException ex) {
            fileSystem = systemProvider.newFileSystem(jarFileUri, Collections.singletonMap("create", "true"));
        }

        return fileSystem;
    }


    /**
     * 获取输入流
     *
     * @param resourceLocation 文件相对路径
     */
    public InputStream getInputStream(String resourceLocation) {
        return PathUtils.class.getResourceAsStream(resourceLocation);
    }


}
