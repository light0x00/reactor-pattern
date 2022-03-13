package io.github.light0x00.reactor.lib;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Objects;

/**
 * @author light
 * @since 2022/3/12
 */
@Slf4j
public class ServerReactor extends Reactor {
    ServerSocketChannel ssc;
    List<IOReactor> reactors;

    public ServerReactor(ServerSocketChannel socket, List<IOReactor> subReactors) throws IOException {
        SelectionKey key = socket.register(selector, SelectionKey.OP_ACCEPT);
        key.attach(new EventContext(key));
        this.ssc = socket;
        this.reactors = subReactors;
    }

    private IOReactor determineReactor(SocketChannel sc) {
        int hash = Objects.hashCode(sc);
        int index = hash & (reactors.size() - 1);
        return reactors.get(index);
    }

    @Override
    protected void dispatch(SelectionKey sk) throws IOException {
        if (sk.isAcceptable()) {
            SocketChannel sc = ssc.accept();
            sc.configureBlocking(false);
            IOReactor subReactor = determineReactor(sc);
            subReactor.tasks.add(() -> {
                try {
                    subReactor.register(sc);
                } catch (IOException e) {
                    sk.cancel();
                    log.error("", e);
                }
            });
            subReactor.selector.wakeup();
        }
    }

}
