package io.github.light0x00.reactor.lib;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * @author light
 * @since 2022/3/12
 */
public class IOReactor extends Reactor {

    private EventHandler eventHandler;

    public IOReactor(EventHandler handler) throws IOException {
        this.eventHandler = handler;
    }

    public void register(SelectableChannel channel) throws IOException {
        SelectionKey sk = channel.register(selector, SelectionKey.OP_READ);
        EventContext eventContext = new EventContext(sk);
        sk.attach(eventContext);
        eventHandler.onEstablished(eventContext);
    }

    @Override
    protected void dispatch(SelectionKey sk) throws IOException {
        EventContext eventContext = (EventContext) sk.attachment();
        if (sk.isReadable()) {
            try {
                eventHandler.onRead(eventContext);
            } catch (IOException e) {
                sk.channel().close();
            }
        } else if (sk.isWritable()) {
            eventHandler.onWrite(eventContext);
        }
    }
}
