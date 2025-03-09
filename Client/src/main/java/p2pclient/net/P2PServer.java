package p2pclient.net;

import p2pclient.config.P2PClientConfig;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * P2PServer - Handles incoming peer connections (ping, download, upload).
 */
public class P2PServer {
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final P2PClientConfig config;

    public P2PServer(ServerSocket serverSocket, int maxThreads, P2PClientConfig config) {
        this.serverSocket = serverSocket;
        this.threadPool = Executors.newFixedThreadPool(maxThreads);
        this.config = config;
    }

    /**
     * Starts the P2P server to handle peer connections.
     */
    public void start() {
        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new PeerHandler(clientSocket, config.getDirectories()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to start P2P server on port ", e);
        }
    }

    /**
     * Stops the server.
     */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("P2P Server stopped.");
            }
            threadPool.shutdown();
        } catch (IOException e) {
            System.err.println("Error stopping P2PServer: " + e.getMessage());
        }
    }

    /**
     * Handles peer connections (ping, download, upload).
     */
        private record PeerHandler(Socket clientSocket, Map<String, String> directories) implements Runnable {

        @Override
        public void run() {
            try (BufferedInputStream inputStream = new BufferedInputStream(clientSocket.getInputStream());
                 BufferedOutputStream outputStream = new BufferedOutputStream(clientSocket.getOutputStream())) {

                // first 10 byte for message type
                byte[] messageBuffer = new byte[10];
                int bytesRead = inputStream.read(messageBuffer);
                if (bytesRead != 10) {
                    return;
                }

                String messageType = new String(messageBuffer).trim();

                if ("ping".equalsIgnoreCase(messageType)) {
                    outputStream.write("pong      ".getBytes());
                    outputStream.flush();
                    System.out.println("Responded to ping request.");
                    return;
                }

                if ("download".equalsIgnoreCase(messageType)) {
                    handleDownload(inputStream, outputStream);
                    return;
                }

                if ("upload".equalsIgnoreCase(messageType)) {
                    handleUpload(inputStream);
                    return;
                }

            } catch (IOException e) {
                System.err.println("Error handling peer connection: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void handleDownload(InputStream inputStream, OutputStream outputStream) throws IOException {
            byte[] chunkHashBuffer = new byte[64]; // SHA-256 (64 char)
            int bytesRead = inputStream.read(chunkHashBuffer);
            if (bytesRead <= 0) {
                outputStream.write("error     ".getBytes());
                outputStream.flush();
                return;
            }

            String chunkHash = new String(chunkHashBuffer).trim();
            sendChunk(chunkHash, outputStream);
        }

        private void handleUpload(InputStream inputStream) throws IOException {
            byte[] chunkHashBuffer = new byte[64]; // SHA-256 (64 char)
            int bytesRead = inputStream.read(chunkHashBuffer);
            if (bytesRead <= 0) {
                return;
            }

            String chunkHash = new String(chunkHashBuffer).trim();
            receiveChunk(chunkHash, inputStream);
        }

        private void sendChunk(String chunkHash, OutputStream outputStream) throws IOException {
            // find file name == chunkHash & send to peer
            File chunkFile = new File(directories.get("uploads") + "/" + chunkHash);
            if (!chunkFile.exists()) {
                outputStream.write("not_found".getBytes());
                outputStream.flush();
                return;
            }

            try (FileInputStream fileInputStream = new FileInputStream(chunkFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            outputStream.flush();
        }

        private void receiveChunk(String chunkHash, InputStream inputStream) throws IOException {
            // receive chunk from peer and save as file of its hash
            // FIX instant return if directories.get("uploads") + "/" + chunkHash exists
            File chunkFile = new File(directories.get("uploads") + "/" + chunkHash);
            try (FileOutputStream fileOutputStream = new FileOutputStream(chunkFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}