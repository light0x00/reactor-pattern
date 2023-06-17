package io.github.light0x00.reactor;

import io.github.light0x00.reactor.lib.EventContext;
import io.github.light0x00.reactor.lib.EventHandler;
import io.github.light0x00.reactor.lib.ReactorBoot;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

/**
 * @author light
 * @since 2022/3/13
 */
@Slf4j
@SuppressWarnings("Duplicates")
public class App {

    public static class ServerApp {
        public static void main(String[] args) throws IOException {
            ReactorBoot.runServer(9001, 2, new AckEventHandler());
        }
    }

    public static class ClientApp {
        public static void main(String[] args) throws IOException {
            ReactorBoot.runClient(9001, new ClientEventHandler());
        }
    }

    public static class ClientEventHandler extends LoggingEventHandler {
        @Override
        public void onEstablished(EventContext event) throws IOException {
            log.info("An connection established");
            ByteBuffer buffer = ByteBuffer.wrap("多年以后，马孔多已经遍布新顶木屋，在那些最古道的街道上，却依然可见巴旦杏树蒙尘的残枝断干，然而已无人知晓出自谁人手之。".getBytes(StandardCharsets.UTF_8));
            event.write(buffer);
        }
    }

    public static class LoggingEventHandler extends EventHandler {
        @Override
        public void onEstablished(EventContext event) throws IOException {
            log.info("An connection established");
        }

        @Override
        public void onRead(EventContext event) throws IOException {
            log.info("Read event");
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            while (event.read(buffer) > 0) {
                buffer.flip();
                System.out.print(StandardCharsets.UTF_8.decode(buffer)); //可能发生最后一个字符被截断导致乱码的情况
                buffer.clear();
            }
            log.info("Read finish");
        }
    }

    public static class AckEventHandler extends EventHandler {
        @Override
        public void onRead(EventContext event) throws IOException {

            log.info("Read event");
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int n;
            while ((n = event.read(buffer)) > 0) {
                buffer.flip();
                System.out.println(StandardCharsets.UTF_8.decode(buffer)); //由于没有实现编码解码,这里可能发生最后一个字符被截断导致乱码的情况
                buffer.clear();
            }
            log.info("Read finish");

            if (n != -1) {
                event.write(ByteBuffer.wrap("ACK".getBytes(StandardCharsets.UTF_8)));
            }

        }
    }
}
