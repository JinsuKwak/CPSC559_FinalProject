package p2pclient.service;

import p2pclient.config.P2PClientConfig;
import p2pclient.dto.*;
import p2pclient.net.P2PDownloader;
import p2pclient.net.P2PUploader;
import p2pclient.utils.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.net.http.*;
import java.net.URI;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import p2pclient.utils.HashUtils;

public class FileService {
    private final P2PClientConfig config;
    private final PeerService peerService;

    public FileService(P2PClientConfig config, PeerService peerService) {
        this.config = config;
        this.peerService = peerService;
    }

    /**
     * get all available file list from DB (GET /files)
     */
    public List<ResFileDto> getAllFiles() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + "/files"))
                .GET()
                .build();

        HttpResponse<String> response = config.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Failed to get all files: " + response.body());
            return Collections.emptyList();
        }

        List<ResFileDto> files = config.getObjectMapper().readValue(
                response.body(), new TypeReference<List<ResFileDto>>() {}
        );
        return files;
    }

    public boolean uploadFile(File file, String password) throws IOException, InterruptedException, NoSuchAlgorithmException {
        List<ResPeerDto> peers = peerService.getPeers();
        String myIP = config.getLocalIPAddress();
        int myPort = config.getListeningPort();
        boolean onlyMe = peers.size() == 1 && peers.getFirst().peer_ip.equals(myIP) && peers.getFirst().peer_port == myPort;

        //
        // comment here to
        //
        if (peers.isEmpty() || onlyMe) {
            System.out.println("No active peers found. Please upload file later.");
            return false;
        }

        // remove self from peer list
        peers.removeIf(peer -> peer.peer_ip.equals(myIP) && peer.peer_port == myPort);

        //
        // here for test with one machine
        //

        String fileName = file.getName();
        String fileHash = HashUtils.computeHash(file);
        Long fileSize = file.length();
        boolean fileEnc = Objects.nonNull(password) && !password.isBlank();

        // encrypt file here // leave as it is for now
        // if(fileEnc){
        //      file = encryptFile(file, password)
        // {

        ReqFileDto fileMetaData = new ReqFileDto(fileName, fileHash, fileSize, fileEnc);

        P2PUploader uploader = new P2PUploader(config, fileMetaData, file, peers);
        uploader.start();
        // maybe loading bar here?
        uploader.join();
        Map<String, Object> result = uploadFileMetadata(fileMetaData);
        return true;
    }


    /**
     * Upload File Metadata (POST /files/upload_file)
     * returns : file_id, file_name, file_hash, success_message
     */
    public Map<String, Object> uploadFileMetadata(ReqFileDto fileDto) throws IOException, InterruptedException {
        String body = config.getObjectMapper().writeValueAsString(fileDto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + "/files/upload_file"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = config.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("Upload failed: " + response.body());
        }

        System.out.println("DEBUG: Response Body: " + JsonUtils.jsonFormatter(response.body()));
        return config.getObjectMapper().readValue(response.body(), new TypeReference<>() {});
    }

    /**
     * Download File Metadata (GET /files/download_file/{file_id})
     */
    public ResFileDto downloadFileMetadata(int fileId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getBaseUrl() + "/files/download_file/" + fileId))
                .GET()
                .build();

        HttpResponse<String> response = config.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("Download failed: " + response.body());
            return null;
        }

        System.out.println("DEBUG: Response Body: " + JsonUtils.jsonFormatter(response.body()));
        return config.getObjectMapper().readValue(response.body(), ResFileDto.class);
    }


}