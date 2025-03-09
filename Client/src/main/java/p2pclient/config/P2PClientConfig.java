package p2pclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import p2pclient.utils.PortManager;

import java.io.File;
import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

/**
 * P2PClientConfig - Server Address, HttpClient, ObjectMapper, listening Port, ...
 */
public class P2PClientConfig {

    private final String baseUrl;
    private final PortManager portManager;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final int CHUNK_SIZE = 1024 * 1024; // 1 MB
    private final int NUMBER_OF_COPIES = 2;
    private final int MAX_THREADS = 5;
    private final Map<String, String> directories = new HashMap<>();

    public P2PClientConfig(P2PSettings settings) {
        this.baseUrl = settings.getBaseUrl();
        this.portManager = new PortManager(settings.getListeningPort());
        this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        this.mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        directories.put("complete", "downloads/complete");
        directories.put("incomplete", "downloads/incomplete");
        directories.put("uploads", "uploads");

        ensureDirectoriesExist();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public ObjectMapper getObjectMapper() {
        return mapper;
    }

    public int getListeningPort() {
        return portManager.getListeningPort();
    }

    public String getLocalIPAddress() {
        return portManager.getLocalIPAddress();
    }

    public PortManager getPortManager() {
        return portManager;
    }

    public int getChunkSize() {
        return CHUNK_SIZE;
    }

    public int getNumberOfCopies() {
        return NUMBER_OF_COPIES;
    }

    public int getMaxThreads() {
        return MAX_THREADS;
    }

    public Map<String, String> getDirectories() {
        return directories;
    }

    public String getDirectoryPath(String key) {
        return directories.getOrDefault(key, null);
    }

    private void ensureDirectoriesExist() {
        for (Map.Entry<String, String> entry : directories.entrySet()) {
            File dir = new File(entry.getValue());
            if (!dir.exists()) {
                if (dir.mkdirs()) {
                    System.out.println("Created directory: " + dir.getAbsolutePath());
                } else {
                    System.err.println("Failed to create directory: " + dir.getAbsolutePath());
                }
            }
            directories.put(entry.getKey(), dir.getAbsolutePath());
        }
    }

    @Override
    public String toString() {
        return """
           ===========================
           P2P Client Configuration
           ---------------------------
           Base URL         : %s
           Local IP         : %s
           Listening Port   : %d
           Chunk Size       : %d bytes
           Number of Copies : %d
           Max Threads      : %d
           ===========================
           """.formatted(baseUrl, getLocalIPAddress(), getListeningPort(), CHUNK_SIZE, NUMBER_OF_COPIES, MAX_THREADS);
    }
}
