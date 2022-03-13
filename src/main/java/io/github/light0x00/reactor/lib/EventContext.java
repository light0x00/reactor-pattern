package io.github.light0x00.reactor.lib;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
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
    private Queue<ByteBuffer> buffersToWrite = new ConcurrentLinkedDeque<>();

    public EventContext(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    public int read(ByteBuffer buffer) throws IOException {
        int ret = ((ReadableByteChannel) selectionKey.channel()).read(buffer);
        if (ret == -1) {
            selectionKey.cancel(); //取消监听
            throw new IOException();
        }
        return ret;
    }

    public void write(ByteBuffer data) {
        buffersToWrite.add(data);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
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
        selectionKey.interestOps(SelectionKey.OP_READ);
    }
}
