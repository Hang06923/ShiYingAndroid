package com.hang.shiying.download;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class FileDownloaderTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ServerSocket server;
    private Thread serverThread;
    private byte[] payload;

    @Before
    public void setUp() throws Exception {
        payload = new byte[128 * 1024];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i % 251);
        }
        server = new ServerSocket();
        server.bind(new InetSocketAddress("127.0.0.1", 0));
        serverThread = new Thread(() -> {
            try (Socket socket = server.accept()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.US_ASCII));
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    // Drain the request headers before sending the fixed response.
                }
                String headers = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: video/mp4\r\n"
                        + "Content-Length: " + payload.length + "\r\n"
                        + "Connection: close\r\n\r\n";
                socket.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));
                socket.getOutputStream().write(payload);
                socket.getOutputStream().flush();
            } catch (Exception ignored) {
                // The test assertion reports a failed connection with a missing file.
            }
        });
        serverThread.start();
    }

    @After
    public void tearDown() {
        try {
            server.close();
        } catch (Exception ignored) {
            // The socket may already be closed by the test process.
        }
    }

    @Test
    public void writesResponseAndReportsProgress() throws Exception {
        File output = new File(temporaryFolder.getRoot(), "video.mp4");
        AtomicInteger lastProgress = new AtomicInteger(-1);

        File result = new FileDownloader().download(
                "http://127.0.0.1:" + server.getLocalPort() + "/video.mp4",
                output,
                new AtomicBoolean(false),
                lastProgress::set,
                "https://www.douyin.com/");

        assertEquals(output, result);
        assertArrayEquals(payload, Files.readAllBytes(result.toPath()));
        assertTrue(lastProgress.get() >= 0);
    }
}
