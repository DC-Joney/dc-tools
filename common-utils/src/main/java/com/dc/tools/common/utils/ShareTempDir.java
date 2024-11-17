package com.dc.tools.common.utils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 同步生成临时目录
 *
 * @author zhangyang
 * @date 2020-08-17
 */
@SuppressWarnings("Duplicates")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShareTempDir extends AbstractQueuedSynchronizer {

    @Setter(AccessLevel.PRIVATE)
    volatile Path tempDirectory;

    final FileSync shareSync;

    private static final Path tempPath = Paths.get(SystemUtils.JAVA_IO_TMPDIR);

    @Getter
    final boolean useTempApi;

    final String filePrefix;

    /**
     * @param useTempApi true:
     *                   <p>
     *                   //java {@link Files#createTempDirectory(Path, String, FileAttribute[])} api
     *                   在第一次创建临时目录时过程过长，但是在第二次第三次进行创建时时间会非常短（第一次 1.4s ,第二次 1ms
     *                   <p>
     *                   false:
     *                   //java {@link Files#createDirectories(Path, FileAttribute[])}
     *                   每一次创建的时间长短是差不多一致的 （第一次 18ms 第二次 19ms 第三次 18ms）
     */
    public ShareTempDir(boolean useTempApi, String filePrefix) {
        this.shareSync = new ShareSync(this);
        this.useTempApi = useTempApi;
        this.filePrefix = filePrefix;
    }


    /**
     * 获取生成的临时目录
     */
    public Path getTempDirectory() {
        shareSync.fileLock();
        try {

            if (!this.isExists()) {
                Path tempDirectory = useTempApi ? Files.createTempDirectory(Paths.get(SystemUtils.JAVA_IO_TMPDIR), String.format("%s.", filePrefix))
                        : Files.createDirectories(tempPath.resolve(filePrefix));

                setTempDirectory(tempDirectory);
            }
        } catch (IOException ex) {
            throw new RuntimeException("创建临时目录出错，原因为: ", ex);
        } finally {
            shareSync.unlock();
        }

        return tempDirectory;
    }


    public Path newChild(Path childPath) {
        return getTempDirectory().resolve(childPath);
    }


    /**
     * linux 每隔一段时间会自动清除tmp目录下的临时文件。所以需要判断该文件是否存在
     */
    private boolean isExists() {
        return tempDirectory != null && Files.exists(tempDirectory);
    }


    public interface FileSync {

        /**
         * 加锁
         */
        void fileLock();

        /**
         * 解锁
         */
        void unlock();
    }


    /**
     * 基于共享锁的 加锁 解锁
     */
    private static class ShareSync extends AbstractQueuedSynchronizer implements FileSync {

        /**
         * 线程加锁状态
         */
        private static final int LOCK_STATE = -1;

        /**
         * 线程解锁状态
         */
        private static final int UNLOCK_STATE = 1;

        private ShareTempDir tempDir;

        public ShareSync(ShareTempDir tempDir) {
            setState(UNLOCK_STATE);
            this.tempDir = tempDir;
        }

        @Override
        protected int tryAcquireShared(int arg) {

            //如果缓存的值已经不为null，则直接返回
            if (tempDir.isExists()) {
                return UNLOCK_STATE;
            }

            if (getState() == LOCK_STATE) {
                return LOCK_STATE;
            }

            for (; ; ) {
                int state = getState();
                if (state == LOCK_STATE || getExclusiveOwnerThread() != null)
                    break;

                if (compareAndSetState(UNLOCK_STATE, LOCK_STATE)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    break;
                }
            }

            if (getExclusiveOwnerThread() == Thread.currentThread()) {
                return UNLOCK_STATE;
            }

            return LOCK_STATE;
        }


        @Override
        protected boolean tryReleaseShared(int arg) {
            //防止释放锁的时候 线程再次加锁
            if (getExclusiveOwnerThread() == Thread.currentThread()) {
                setExclusiveOwnerThread(null);
            }

            int state = getState();

            if (state == LOCK_STATE)
                setState(UNLOCK_STATE);


            /*
                这里返回true的原因是，如果在共享锁释放传播的过程中出现异常那么就会导致后边的线程一直阻塞
                那么就要保证下一个线程在unlock时可以帮助释放CLH队列中等待的线程

                TODO: 这种情况会发生吗？解决方法:
                return state == LOCK_STATE && compareAndSetState(state, UNLOCK_STATE);

                不过AQS在释放时，同时也会判断head==tail，当CLH队列不存在阻塞节点时，返回true，那么就不会再走后续逻辑
             */
            return true;

        }

        public void fileLock() {
            acquireShared(1);
        }

        public void unlock() {
            releaseShared(1);
        }
    }


    /**
     * 基于独占锁的 加锁 解锁
     */
    private static class ReentrantSync extends AbstractQueuedSynchronizer implements FileSync {

        public ReentrantSync(ShareTempDir tempDir) {

        }

        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        public void fileLock() {
            acquire(1);
        }

        public void unlock() {
            release(1);
        }

    }
}



