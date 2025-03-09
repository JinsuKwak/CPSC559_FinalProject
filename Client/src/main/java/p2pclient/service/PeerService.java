package p2pclient.service;

import com.fasterxml.jackson.core.type.TypeReference;
import p2pclient.config.P2PClientConfig;
import p2pclient.dto.ReqPeerDto;
import p2pclient.dto.ResPeerDto;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import java.net.http.*;
import java.util.*;

public class PeerService {
    private final P2PClientConfig config;

    public PeerService(P2PClientConfig config) {
        this.config = config;
        try {
            registerSelfWithProxy(PeerAction.ADD);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to register self with proxy", e);
        }
    }

    public enum PeerAction {
        ADD, REMOVE
    }

    /**
     * Add Peer on Proxy (POST /peers/add)
     */
    public void addPeer(String ip, int port) throws IOException, InterruptedException {
        ReqPeerDto dto = new ReqPeerDto(ip, port);
        String body = config.getObjectMapper().writeValueAsString(dto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + "/peers/add"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = config.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("DEBUG:  addPeer Response: " + response.body());

    }

    /**
     * Remove Peer on Proxy (POST /peers/remove)
     */
    public void removePeer(String ip, int port) throws IOException, InterruptedException {
        ReqPeerDto dto = new ReqPeerDto(ip, port);
        String body = config.getObjectMapper().writeValueAsString(dto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + "/peers/remove"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = config.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("DEBUG:  removePeer Response: " + response.body());
    }


    public void registerSelfWithProxy(PeerAction action) throws IOException, InterruptedException {
        String myAddress = config.getLocalIPAddress();
        int myListeningPort = config.getListeningPort();

        if (myAddress == null) {
            System.err.println("Failed to retrieve local IP address.");
            return;
        }

        System.out.println("Registering self to proxy: " + myAddress + ":" + myListeningPort + " (Action: " + action + ")");

        switch (action) {
            case ADD:
                addPeer(myAddress, myListeningPort);
                return;
            case REMOVE:
                removePeer(myAddress, myListeningPort);
                return;
            default:
                System.err.println("Invalid action: " + action);
        }
    }


    /**
     * Get Active Peers (GET /peers)
     */
    public List<ResPeerDto> getPeers() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + "/peers"))
                .GET()
                .build();

        HttpResponse<String> response = config.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Failed to get peers: " + response.body());
            return Collections.emptyList();
        }
        return config.getObjectMapper().readValue(response.body(), new TypeReference<List<ResPeerDto>>() {});
    }
}