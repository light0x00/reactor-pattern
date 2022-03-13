package io.github.light0x00.reactor.lib;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * TODO 对象组合 Dispatcher 来分发事件
 *
 * @author light
 * @since 2022/3/12
 */
@Slf4j
@SuppressWarnings("Duplicates")
public abstract class Reactor implements Runnable {

    protected Selector selector = Selector.open();

    protected Queue<Runnable> tasks = new ConcurrentLinkedDeque<>();

    public Reactor() throws IOException {
    }

    public void run() {
        try {
            eventLoop();
        } catch (IOException e) {
            log.error("An error occur while event loop", e);
        }
    }

    private void eventLoop() throws IOException {
        while (!Thread.interrupted()) {
            Runnable c;
            while ((c = tasks.poll()) != null) {
                c.run();
            }
            selector.select();
            Set<SelectionKey> events = selector.selectedKeys();
            Iterator<SelectionKey> it = events.iterator();
            while (it.hasNext()) {
                SelectionKey event = it.next();
                dispatch(event);
            }
            events.clear();
        }
    }

    protected abstract void dispatch(SelectionKey sk) throws IOException;
}
