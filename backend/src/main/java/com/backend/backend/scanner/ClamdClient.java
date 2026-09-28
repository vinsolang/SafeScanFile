package com.backend.backend.scanner;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.backend.backend.exception.ScannerUnavailableException;

/**
 * Minimal clamd client using the INSTREAM command.
 *
 * The file content is streamed over TCP, so the ClamAV container needs
 * no access to our filesystem. Frame format (see clamd docs):
 *   "zINSTREAM\0", then repeated [4-byte big-endian length][data],
 *   terminated by a zero-length chunk. Reply: "stream: OK",
 *   "stream: <signature> FOUND" or "... ERROR", NUL-terminated.
 *
 * Intentionally free of Spring so it can be tested on its own.
 */
public final class ClamdClient {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int CHUNK_SIZE = 8 * 1024;
    private static final int MAX_REPLY_BYTES = 4 * 1024;

    private final String host;
    private final int port;
    private final int readTimeoutMs;

    public ClamdClient(String host, int port, int readTimeoutMs) {
        this.host = host;
        this.port = port;
        this.readTimeoutMs = readTimeoutMs;
    }

    /** Streams the file to clamd and returns the parsed verdict. */
    public ScanResult scan(Path file) {
        try (Socket socket = open()) {
            OutputStream out = new BufferedOutputStream(socket.getOutputStream());
            IOException writeFailure = null;
            try {
                out.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
                streamFile(file, out);
                out.write(new byte[] {0, 0, 0, 0});
                out.flush();
            } catch (IOException e) {
                // clamd closes early when it rejects the stream (e.g. size limit)
                // and still sends a reason. Remember the failure, try to read it.
                writeFailure = e;
            }

            String reply = readReply(socket.getInputStream());
            if (reply.isEmpty() && writeFailure != null) {
                throw writeFailure;
            }
            return parseReply(reply);
        } catch (IOException e) {
            throw new ScannerUnavailableException(
                    "Cannot talk to ClamAV at " + host + ":" + port + ": " + e.getMessage(), e);
        }
    }

    /** True if clamd answers PING with PONG. */
    public boolean ping() {
        try (Socket socket = open()) {
            OutputStream out = socket.getOutputStream();
            out.write("zPING\0".getBytes(StandardCharsets.US_ASCII));
            out.flush();
            return "PONG".equals(readReply(socket.getInputStream()));
        } catch (IOException e) {
            return false;
        }
    }

    static ScanResult parseReply(String reply) {
        String r = reply.trim();
        if (r.equals("stream: OK")) {
            return ScanResult.clean();
        }
        if (r.startsWith("stream: ") && r.endsWith(" FOUND")) {
            String name = r.substring("stream: ".length(), r.length() - " FOUND".length()).trim();
            return ScanResult.threat(name.isEmpty() ? "Unknown" : name);
        }
        return ScanResult.error(r.isEmpty() ? "Empty reply from ClamAV" : r);
    }

    private Socket open() throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(readTimeoutMs);
            return socket;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    private static void streamFile(Path file, OutputStream out) throws IOException {
        byte[] buf = new byte[CHUNK_SIZE];
        try (InputStream in = Files.newInputStream(file)) {
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(ByteBuffer.allocate(4).putInt(n).array());
                out.write(buf, 0, n);
            }
        }
    }

    private static String readReply(InputStream in) throws IOException {
        ByteArrayOutputStream reply = new ByteArrayOutputStream();
        try {
            int b;
            while ((b = in.read()) != -1 && b != 0 && reply.size() < MAX_REPLY_BYTES) {
                reply.write(b);
            }
        } catch (IOException e) {
            if (reply.size() == 0) {
                throw e;
            }
        }
        return reply.toString(StandardCharsets.US_ASCII);
    }

}
