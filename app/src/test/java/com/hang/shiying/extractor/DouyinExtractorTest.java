package com.hang.shiying.extractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public final class DouyinExtractorTest {
    private ServerSocket server;
    private Thread serverThread;

    @After
    public void tearDown() throws Exception {
        if (server != null) {
            server.close();
        }
        if (serverThread != null) {
            serverThread.join(2_000);
        }
    }

    @Test
    public void resolvesIdAfterRedirectResponseIsRead() throws Exception {
        server = new ServerSocket();
        server.bind(new InetSocketAddress("127.0.0.1", 0));
        int port = server.getLocalPort();
        String finalLocation = "http://127.0.0.1:" + port + "/share/video/7701831672870941843/";

        serverThread = new Thread(() -> {
            try {
                Socket first = server.accept();
                readRequest(first);
                respond(first,
                        "HTTP/1.1 302 Found\r\n"
                                + "Location: " + finalLocation + "\r\n"
                                + "Content-Length: 0\r\n"
                                + "Connection: close\r\n\r\n");
                Socket second = server.accept();
                readRequest(second);
                respond(second,
                        "HTTP/1.1 200 OK\r\n"
                                + "Content-Type: text/html; charset=utf-8\r\n"
                                + "Content-Length: 2\r\n"
                                + "Connection: close\r\n\r\n"
                                + "ok");
            } catch (Exception ignored) {
                // A connection failure surfaces as the assertion below.
            }
        });
        serverThread.start();

        String resolved = DouyinExtractor.resolveId(
                "http://127.0.0.1:" + port + "/short");

        assertEquals("7701831672870941843", resolved);
    }

    @Test
    public void retriesShareRequestUntilCookieUnlocksContent() throws Exception {
        server = new ServerSocket();
        server.bind(new InetSocketAddress("127.0.0.1", 0));
        int port = server.getLocalPort();
        AtomicReference<String> secondRequest = new AtomicReference<>("");

        serverThread = new Thread(() -> {
            try {
                Socket first = server.accept();
                readRequest(first);
                respond(first,
                        "HTTP/1.1 200 OK\r\n"
                                + "Content-Type: text/html; charset=utf-8\r\n"
                                + "Content-Length: 20\r\n"
                                + "Connection: close\r\n\r\n"
                                + "<html>initial</html>");
                Socket second = server.accept();
                readRequest(second);
                respond(second,
                        "HTTP/1.1 200 OK\r\n"
                                + "Set-Cookie: ttwid=abc123; Path=/; HttpOnly\r\n"
                                + "Content-Type: text/html; charset=utf-8\r\n"
                                + "Content-Length: 20\r\n"
                                + "Connection: close\r\n\r\n"
                                + "<html>initial</html>");
                Socket third = server.accept();
                secondRequest.set(readRequest(third));
                respond(third,
                        "HTTP/1.1 200 OK\r\n"
                                + "Content-Type: text/html; charset=utf-8\r\n"
                                + "Content-Length: 25\r\n"
                                + "Connection: close\r\n\r\n"
                                + "<html>videoInfoRes</html>");
            } catch (Exception ignored) {
                // A connection failure surfaces as the assertions below.
            }
        });
        serverThread.start();

        String html = DouyinExtractor.fetchSharePage(
                "http://127.0.0.1:" + port + "/share/video/7701831672870941843/");

        assertTrue(html.contains("videoInfoRes"));
        assertTrue(secondRequest.get().contains("Cookie: ttwid=abc123"));
    }

    private static String readRequest(Socket socket) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                socket.getInputStream(),
                StandardCharsets.US_ASCII));
        StringBuilder request = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            request.append(line).append('\n');
        }
        return request.toString();
    }

    private static void respond(Socket socket, String response) throws Exception {
        socket.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().flush();
        socket.close();
    }
}
