package com.dc.tools.io.download;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HttpFile {

    public static final HttpFileClient CLIENT = new HttpFileClient();

    String fileUrl;

    public HttpFile(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Path download() throws Exception {
        return CLIENT.downloadFile(fileUrl)
                .toFuture()
                .get();
    }

    public Path download(long time, TimeUnit timeUnit) throws Exception {
        return CLIENT.downloadFile(fileUrl)
                .toFuture()
                .get(time, timeUnit);
    }


    public CompletableFuture<Path> downloadFuture() throws Exception {
        return CLIENT.downloadFile(fileUrl)
                .toFuture();
    }


    public FileChannel readFileChannel() throws Exception {
        Path path = CLIENT.downloadFile(fileUrl)
                .toFuture()
                .get(10, TimeUnit.SECONDS);
        return FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ);
    }


    public ReadableByteChannel readChannel() throws Exception {
        Path path = CLIENT.downloadFile(fileUrl)
                .toFuture()
                .get(10, TimeUnit.SECONDS);
        return Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.READ);
    }


    public void transferTo(WritableByteChannel writableByteChannel) throws Exception {
        FileChannel fileChannel = readFileChannel();
        fileChannel.transferTo(0, fileChannel.size(), writableByteChannel);
    }

}
