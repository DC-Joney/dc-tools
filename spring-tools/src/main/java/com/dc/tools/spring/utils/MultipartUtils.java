package com.dc.tools.spring.utils;

import com.dc.tools.common.IdGenerator;
import com.dc.tools.common.RandomIdGenerator;
import com.dc.tools.common.utils.CloseTasks;
import com.dc.tools.common.utils.DirectBufferUtils;
import com.dc.tools.common.utils.ShareTempDir;
import com.dc.tools.io.buffer.AsyncInputStream;
import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

/**
 * Operation multiplier file
 */
@UtilityClass
public class MultipartUtils {

    /**
     * 临时文件存储的目录
     */
    private final ShareTempDir shareTempDir = new ShareTempDir(false, "upload_book");

    /**
     * 雪花算法实现用于生成文件唯一标识
     */
    private final IdGenerator sequence = new RandomIdGenerator();

    /**
     * 根据上传的文件创建一个新的临时文件
     *
     * @param multipartFile 上传的文件
     * @param close         针对大文件上传时，是否需要关闭上传的文件，因为上传的文件可能存在于内存中，这回导致jvm的内存暴涨
     * @return 返回复制后的文件
     */
    public Path newTempFile(MultipartFile multipartFile, boolean close) throws IOException {
        try (CloseTasks closeTasks = new CloseTasks()) {
            //获取原来的文件名称
            String originalFilename = multipartFile.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);

            String baseName = FilenameUtils.getBaseName(originalFilename).replace(" ", "@")
                    .replace("?", "");


            //生成新的临时文件名称
            String tempFileName = baseName + "_" + sequence.nextId() + "." + extension;
            //获取临时文件path
            Path tempPath = shareTempDir.getTempDirectory().resolve(tempFileName);
            FileChannel tempChannel = FileChannel.open(tempPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            closeTasks.addTask(tempChannel::close, "Close multipart temp channel, fileName is : {}", originalFilename);

            //获取上传的文件的流数据
            InputStream inputStream = multipartFile.getInputStream();
            //如果inputStream 为 FileInputStream类型表示当前数据不在内存中，那么直接通过FileChannel.transferTo进行复制
            //否则优先使用已经在内存中的数据
            if (inputStream instanceof FileInputStream) {
                FileInputStream stream = (FileInputStream) inputStream;
                FileChannel uploadChannel = stream.getChannel();
                closeTasks.addTask(uploadChannel::close, "Close multipart upload channel, fileName is: {}", originalFilename);
                //将上传文件的临时数据复制到新的文件
                uploadChannel.transferTo(0, uploadChannel.size(), tempChannel);
                return tempPath;
            }

            //通过mmap + write的方式写入
            MappedByteBuffer byteBuffer = tempChannel.map(FileChannel.MapMode.READ_WRITE, 0, multipartFile.getSize());
            //将这部分数据预加载到内存中，否则会产生大量页中断
            byteBuffer.load();
            //将数据写入到内存
            byteBuffer.put(multipartFile.getBytes());
            //将数据强制写入到文件
            //TODO: 是否需要优化
            closeTasks.addTask(byteBuffer::force, "Force multipart data to file, fileName is: {}", originalFilename);
            //释放这块内存, 避免引起内存泄露
            closeTasks.addTask(() -> DirectBufferUtils.release(byteBuffer), "Release multipart buffer, fileName is: {}", originalFilename);
            return tempPath;
        } finally {
            //将上传的临时文件删除，并且将缓存的内存清空
            if (close && multipartFile instanceof CommonsMultipartFile) {
                CommonsMultipartFile file = (CommonsMultipartFile) multipartFile;
                file.getFileItem().delete();
            }
        }
    }

    /**
     * 根据上传的文件创建一个新的临时文件
     *
     * @param multipartFile 上传的文件
     * @return 返回复制后的文件
     */
    public Path newTempFile(MultipartFile multipartFile) throws IOException {
        return newTempFile(multipartFile, false);
    }

    /**
     * 下载文件
     *
     * @param fileName    文件名称
     * @param inputStream 输入流
     * @param response    response
     */
    public CompletableFuture<Void> downloadFile(String fileName, AsyncInputStream inputStream, HttpServletResponse response) {
        CompletableFuture<Void> writeFuture = new CompletableFuture<>();
        try {
            // 设置response的Header
            response.setCharacterEncoding("UTF-8");
            //Content-Disposition的作用：告知浏览器以何种方式显示响应返回的文件，用浏览器打开还是以附件的形式下载到本地保存
            //attachment表示以附件方式下载   inline表示在线打开   "Content-Disposition: inline; filename=文件名.mp3"
            // filename表示文件的默认名称，因为网络传输只支持URL编码的相关支付，因此需要将文件名URL编码后进行传输,前端收到后需要反编码才能获取到真正的名称
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            response.setContentType("application/octet-stream");
            CompletableFuture<ByteBuf> bufferFuture = inputStream.getBuffer();
            // 告知浏览器文件的大小
            bufferFuture.thenAccept(byteBuf -> response.addHeader("Content-Length", "" + byteBuf.readableBytes()));
            bufferFuture.thenAcceptAsync(byteBuf -> {
                try {
                    byteBuf.readBytes(response.getOutputStream(), byteBuf.readableBytes());
                    response.getOutputStream().flush();
                    writeFuture.complete(null);
                } catch (IOException e) {
                    writeFuture.completeExceptionally(e);
                }
            }, inputStream.getExecutor());

        } catch (IOException ex) {
            writeFuture.completeExceptionally(ex);
        }

        return writeFuture;
    }
}
