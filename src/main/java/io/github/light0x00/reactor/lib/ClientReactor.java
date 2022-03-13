package io.github.light0x00.reactor.lib;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * @author light
 * @since 2022/3/13
 */
@Slf4j
public class ClientReactor extends Reactor {

    private EventHandler eventHandler;
    private SocketChannel clientChannel;


    public ClientReactor(SocketChannel clientChannel, EventHandler eventHandler) throws IOException {
        SelectionKey sk = clientChannel.register(selector, SelectionKey.OP_CONNECT);
        sk.attach(new EventContext(sk));
        this.eventHandler = eventHandler;
        this.clientChannel = clientChannel;
    }

    @Override
    protected void dispatch(SelectionKey sk) throws IOException {
        EventContext eventContext = (EventContext) sk.attachment();
        if (sk.isConnectable()) {
            /*This method may be invoked at any time.
            If a read or write operation upon this channel is invoked while an invocation of this method is in progress
            then that operation will first block until this invocation is complete. */
            clientChannel.finishConnect();
            sk.interestOps(SelectionKey.OP_READ);
            eventHandler.onEstablished(eventContext);
        } else if (sk.isWritable()) {
            eventHandler.onWrite(eventContext);
        } else if (sk.isReadable()) {
            eventHandler.onRead(eventContext);
        }
    }
}
