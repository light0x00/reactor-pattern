package io.github.light0x00.reactor.lib;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author light
 * @since 2022/3/12
 */
@Slf4j
public abstract class EventHandler {

    public void onEstablished(EventContext event) throws IOException {
    }

    public abstract void onRead(EventContext event) throws IOException;

    final void onWrite(EventContext event) throws IOException {
        event.flush();
    }

}
