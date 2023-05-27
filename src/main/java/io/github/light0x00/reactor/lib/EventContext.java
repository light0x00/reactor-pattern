package io.github.light0x00.reactor.lib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author light
 * @since 2022/3/12
 */
@Slf4j
public class EventContext {

    private SelectionKey selectionKey;
    /**
     * 待写入 socket 缓冲的数据，将在下一次 writable 事件发生时写入。
     * <p>
     * 目前存在的优化点：应限制可写入缓冲的最大数据量，超出后只能等待 socket 发送缓冲有空余空间（即 writable 事件发生）
     */
    @Getter
    private final Queue<ByteBuffer> buffersToWrite = new ConcurrentLinkedDeque<>();

    private volatile boolean closeByPeer;

    public EventContext(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    public int read(ByteBuffer buffer) throws IOException {
        int ret = ((ReadableByteChannel) selectionKey.channel()).read(buffer);
        if (ret == -1) {  //不能直接关闭 要等数据写完
            log.info("Received FIN ");
            closeByPeer = true;
            //收到 FIN, 表示对方不会继续发送数据了, 那么应该取消读事件
            //不然 selector.select() 会一直重复返回 readable event
            selectionKey.interestOps(selectionKey.interestOps() ^ SelectionKey.OP_READ);

            //race condition
            write(ByteBuffer.wrap("OHAYO".getBytes(StandardCharsets.UTF_8))); //TEST
            if (closeIfNoPendingWrite()) {
                log.info("Received FIN, Send FIN");
            } else {
                log.info("Received FIN, wait writable event to tidy the pending output buffer. {}", (selectionKey.interestOps() & SelectionKey.OP_WRITE));
            }
        }
        return ret;
    }

    public void write(ByteBuffer data) {
        if (writeIfNotClosed(data)) return;
        throw new IllegalStateException("Socket Closed");
    }

    private boolean closeIfNoPendingWrite() throws IOException {
        if (buffersToWrite.isEmpty()) {
            synchronized (buffersToWrite) {
                if (buffersToWrite.isEmpty()) {
                    selectionKey.channel().close();
                    selectionKey.cancel();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean writeIfNotClosed(ByteBuffer data) {
        // Whether the socket can continue to write depends on whether a FIN sent by this side.
        // The returning value of chanel.isOpen() denotes that.
        if (selectionKey.channel().isOpen()) {
            //race condition
            synchronized (buffersToWrite) {
                if (selectionKey.channel().isOpen()) {
                    buffersToWrite.add(data);
                    selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
//                    selectionKey.interestOps( SelectionKey.OP_WRITE);
                    log.info("write!");
                    return true;
                }
            }
        }
        return false;
    }

    public void flush() throws IOException {
        ByteBuffer buf;

        while ((buf = buffersToWrite.peek()) != null) {
            /* Some types of channels, depending upon their state, may write only some of the bytes or possibly none at all.
            A socket channel in non-blocking mode, for example, cannot write any more bytes than are free in the socket's output buffer.*/
            ((WritableByteChannel) selectionKey.channel()).write(buf);
            if (buf.remaining() == 0) {
                buffersToWrite.poll();
            } else {
                //如果还有剩余，意味 socket 发送缓冲着已经满了，只能等待下一次 Writable 事件
                log.debug("suspend writing socket buffer");
                return;
            }
        }
        if (closeByPeer) {
            if (closeIfNoPendingWrite()) {
                log.info("pending buffer has been tidy, Send FIN");
            } else {
                // concurrent write after above iterating to queue
                // if entering this branch , denote that thread executes a operation to enqueue new buffer , upon above iterating
                // so just wait for the next writable event.
            }
        } else {
            synchronized (buffersToWrite) {
                if (buffersToWrite.isEmpty()) {
//                    selectionKey.interestOps( SelectionKey.OP_READ);
                    selectionKey.interestOps(selectionKey.interestOps() ^ SelectionKey.OP_WRITE);
                    log.info("pending buffer has been tidy, remove interest to writeable.");
                } else {
                    // concurrent write after above iterating to queue
                }
            }
        }
    }

    public boolean isOpen() {
        return selectionKey.channel().isOpen();
    }

    public void close() throws IOException {
        selectionKey.channel().close();
    }
}
