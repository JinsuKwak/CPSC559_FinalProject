package p2pclient.net;

import p2pclient.config.P2PClientConfig;
import p2pclient.dto.ReqChunkDto;
import p2pclient.dto.ReqFileDto;
import p2pclient.dto.ResPeerDto;
import p2pclient.utils.HashUtils;

import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

public class P2PUploader extends Thread {
    private final P2PClientConfig config;
    private final ReqFileDto fileMetaData;
    private final File file;
    private final List<ResPeerDto> peers;
    private final ExecutorService threadPool;
    private final int chunkSize;
    private final List<ReqChunkDto> uploadedChunks = Collections.synchronizedList(new ArrayList<>());

    public P2PUploader(P2PClientConfig config, ReqFileDto fileMetaData, File file, List<ResPeerDto> peers) {
        this.config = config;
        this.fileMetaData = fileMetaData;
        this.file = file;
        this.peers = new ArrayList<>(peers);
        this.threadPool = Executors.newFixedThreadPool(config.getMaxThreads());
        this.chunkSize = config.getChunkSize();
    }

    @Override
    public void run() {
        List<Future<Boolean>> futures = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            int chunkIndex = 0;
            byte[] buffer = new byte[chunkSize];

            while (true) {
                int bytesRead = raf.read(buffer);
                if (bytesRead == -1) break;

                byte[] chunkData = Arrays.copyOf(buffer, bytesRead);
                String chunkHash = HashUtils.computeHash(chunkData);

                int startIndex = new Random().nextInt(peers.size());
                int finalChunkIndex = chunkIndex;
                // Submit the task and add it to futures list
                futures.add(threadPool.submit(() -> uploadChunkToPeers(finalChunkIndex, chunkHash, chunkData, startIndex)));
                chunkIndex++;
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Error while reading file: " + e.getMessage());
        }

        // Wait for all futures to finish
        for (Future<Boolean> future : futures) {
            try {
                // This will block until the future is completed (task is finished)
                future.get();
            } catch (Exception e) {
                e.printStackTrace(); // Handle future execution exceptions here
            }
        }

        // All tasks are done, now shutdown the thread pool
        threadPool.shutdown();
        System.out.println("All chunks uploaded successfully!");

        fileMetaData.setFile_chunks(uploadedChunks);
    }

    private boolean uploadChunkToPeers(int chunkIndex, String chunkHash, byte[] chunkData, int startIndex) {
        int peerCount = peers.size();
        for (int i = 0; i < peerCount; i++) {
            ResPeerDto peer = peers.get((startIndex + i) % peerCount);

            if (uploadChunk(peer.peer_ip, peer.peer_port, chunkHash, chunkData)) {
                synchronized (uploadedChunks) {
                    uploadedChunks.add(new ReqChunkDto((long) chunkIndex, chunkHash, chunkData.length));
                }
                return true;
            }
        }
        return false;
    }

    private boolean uploadChunk(String peerIp, int peerPort, String chunkHash, byte[] chunkData) {
        try (Socket socket = new Socket(peerIp, peerPort);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeBytes("upload    ");
            dos.writeUTF(chunkHash);
            dos.write(chunkData);
            dos.flush();

            String response = dis.readUTF();
            return "OK".equals(response);

        } catch (IOException e) {
            System.err.println("Failed to upload chunk to " + peerIp + ":" + peerPort);
            return false;
        }
    }
}