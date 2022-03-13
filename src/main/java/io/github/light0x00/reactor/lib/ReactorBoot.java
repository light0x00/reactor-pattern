package io.github.light0x00.reactor.lib;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author light
 * @since 2022/3/12
 */
@Slf4j
public class ReactorBoot {

    public static ServerReactor runServer(int port, int level, EventHandler handler) throws IOException {
        List<IOReactor> subReactors = new ArrayList<>(level);
        for (int i = 0; i < level; i++) {
            IOReactor subReactor = new IOReactor(handler);
            subReactors.add(subReactor);
            Thread t = new Thread(subReactor, "EventLoop-IO" + i);
            t.setDaemon(false);
            t.start();
        }

        ServerSocketChannel ssc = createServerChannel(port);
        ServerReactor mainReactor = new ServerReactor(ssc, subReactors);
        Thread t = new Thread(mainReactor, "EventLoop-Main");
        t.setDaemon(false);
        t.start();
        return mainReactor;
    }

    public static ClientReactor runClient(int port, EventHandler eventHandler) throws IOException {
        SocketChannel clientChannel = createClientChannel(port);
        ClientReactor clientReactor = new ClientReactor(clientChannel, eventHandler);
        Thread t = new Thread(clientReactor, "EventLoop-Client");
        t.setDaemon(false);
        t.start();
        return clientReactor;
    }


    private static ServerSocketChannel createServerChannel(int port) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.bind(new InetSocketAddress(port));
        return ssc;
    }

    private static SocketChannel createClientChannel(int port) throws IOException {
        SocketChannel clientChannel = SocketChannel.open();
        clientChannel.configureBlocking(false);
        clientChannel.connect(new InetSocketAddress(port));
        return clientChannel;
    }
}
