package p2pclient.cli;

import p2pclient.config.P2PClientConfig;
import p2pclient.service.*;
import p2pclient.dto.*;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.List;

public class MainCLI {
    private final PeerService peerService;
    private final FileService fileService;
    private final Scanner scanner;
    private Path currentDirectory;

    public MainCLI(PeerService peerService, FileService fileService, Scanner scanner) {
        this.peerService = peerService;
        this.fileService = fileService;
        this.scanner = scanner;
        this.currentDirectory = Paths.get(System.getProperty("user.dir"));
    }

    public void startCLI() {
        System.out.println("Welcome to P2P CLI. Type 'help' for commands.");

        boolean running = true;
        while (running) {
            System.out.print(currentDirectory + " > ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] tokens = line.split("\\s+");
            String cmd = tokens[0].toLowerCase();

            try {
                switch (cmd) {
                    case "help":
                        printHelp();
                        break;
                    case "addpeer":
                        handleAddPeer(tokens);
                        break;
                    case "removepeer":
                        handleRemovePeer(tokens);
                        break;
                    case "listpeers":
                        handleListPeers();
                        break;
                    case "listfiles":
                        handleListFiles();
                        break;
                    case "upload":
                        handleUploadFile(tokens);
                        break;
                    case "download":
                        handleDownloadFile(tokens);
                        break;
                    case "ls":
                        handleLS();
                        break;
                    case "cd":
                        handleCD(tokens);
                        break;
                    case "reset":
                        handleReset();
                    case "exit":
                    case "quit":
                        running = false;
                        break;
                    default:
                        System.out.println("Unknown command: " + cmd);
                }
            } catch (Exception e) {
                System.err.println("Error processing command: " + cmd);
                e.printStackTrace();
            }
        }

        System.out.println("Goodbye!");
    }

    // =======================
    //  Command Handlers
    // =======================

    private void handleAddPeer(String[] tokens) throws IOException, InterruptedException {
        if (tokens.length < 3) {
            System.out.println("Usage: addPeer <ip> <port>");
            return;
        }
        String ip = tokens[1];
        int port = Integer.parseInt(tokens[2]);
        peerService.addPeer(ip, port);
        System.out.println("Peer added: " + ip + ":" + port);
    }

    private void handleRemovePeer(String[] tokens) throws IOException, InterruptedException {
        if (tokens.length < 3) {
            System.out.println("Usage: removePeer <ip> <port>");
            return;
        }
        String ip = tokens[1];
        int port = Integer.parseInt(tokens[2]);
        peerService.removePeer(ip, port);
        System.out.println("Peer removed: " + ip + ":" + port);
    }

    private void handleListPeers() throws IOException, InterruptedException {
        List<ResPeerDto> peers = peerService.getPeers();
        if (peers.isEmpty()) {
            System.out.println("No active peers found.");
        } else {
            System.out.println("Active Peers:");
            for (ResPeerDto peer : peers) {
                System.out.println(" - " + peer.peer_ip + ":" + peer.peer_port);
            }
        }
    }

    private void handleListFiles() throws IOException, InterruptedException {
        List<ResFileDto> allFiles = fileService.getAllFiles();
        if (allFiles.isEmpty()) {
            System.out.println("No files found on the server.");
        } else {
            System.out.println("Available Files:");
            for (ResFileDto file : allFiles) {
                System.out.printf(" - ID: %d | Name: %s | Hash: %s | Size: %d bytes | Enc: %b%n",
                        file.file_id, file.file_name, file.file_hash,
                        file.file_size, file.file_enc);
            }
        }
    }

    private void handleUploadFile(String[] tokens) throws IOException, InterruptedException, NoSuchAlgorithmException {
        if (tokens.length < 2) {
            System.out.println("Usage: uploadFile <name>");
            return;
        }

        String fileName = tokens[1];
        File file = new File(System.getProperty("user.dir"), fileName);

        if (!file.exists() || !file.isFile()) {
            System.out.println("Error: File '" + fileName + "' not found in current directory.");
            return;
        }

        long fileSize = file.length();
        System.out.printf("Uploading File: %s (Size: %d bytes)%n", fileName, fileSize);

        Scanner scanner = new Scanner(System.in);
        boolean encrypt = false;
        String password = null;

        while (true) {
            System.out.print("Do you want to encrypt the file? (yes/no): ");
            String response = scanner.nextLine().trim().toLowerCase();

            if (response.equals("yes")) {
                encrypt = true;
                break;
            } else if (response.equals("no")) {
                break;
            } else {
                System.out.println("Invalid input. Please enter 'yes' or 'no'.");
            }
        }

        if (encrypt) {
            while (true) {
                System.out.print("Enter encryption password (max 8 chars, letters & numbers only): ");
                password = scanner.nextLine().trim();

                if (password.length() > 8 || password.isEmpty()) {
                    System.out.println("Error: Password must be 8 characters or fewer.");
                    continue;
                }
                if (!password.matches("^[a-zA-Z0-9]+$")) {
                    System.out.println("Error: Password must contain only letters and numbers.");
                    continue;
                }
                break; // Valid password entered
            }
        }

        boolean success = fileService.uploadFile(file, password);

    }

    private void handleDownloadFile(String[] tokens) throws IOException, InterruptedException {
        if (tokens.length < 2) {
            System.out.println("Usage: downloadFile <fileId>");
            return;
        }
        int fileId = Integer.parseInt(tokens[1]);
        ResFileDto downloaded = fileService.downloadFileMetadata(fileId);

//        if (downloaded != null) {
//            System.out.println("Downloaded File: " + downloaded.getFileName()
//                    + " / #chunks=" + (downloaded.getFileChunks() != null ? downloaded.getFileChunks().size() : 0));
//            fileService.parallelDownloadChunks(downloaded);
//        }
    }

    private void handleLS() {
        try {
            List<String> files = Files.list(currentDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());

            if (files.isEmpty()) {
                System.out.println("No files found in " + currentDirectory);
            } else {
                System.out.println("Files in " + currentDirectory + ":");
                files.forEach(System.out::println);
            }
        } catch (Exception e) {
            System.err.println("Error listing files: " + e.getMessage());
        }
    }

    private void handleCD(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("Usage: cd <directory>");
            return;
        }
        Path newPath = currentDirectory.resolve(tokens[1]).normalize();

        if (!Files.exists(newPath)) {
            System.out.println("Error: Directory does not exist.");
            return;
        }
        if (!Files.isDirectory(newPath)) {
            System.out.println("Error: Not a directory.");
            return;
        }

        currentDirectory = newPath;
        System.out.println("Changed directory to: " + currentDirectory);
    }

    private void handleReset() {
        System.out.print("Are you sure you want to reset settings? (yes/no): ");
        String confirmation = scanner.nextLine().trim().toLowerCase();

        if (!confirmation.equals("yes")) {
            System.out.println("Reset canceled.");
            return;
        }

        File settingsFile = new File("p2pSettings.json");
        if (settingsFile.exists()) {
            if (settingsFile.delete()) {
                System.out.println("Settings file deleted. Please restart the program to reconfigure.");
            } else {
                System.err.println("Error: Could not delete settings file.");
            }
        } else {
            System.out.println("No settings file found. Nothing to reset.");
        }
    }

    // =======================
    // Print Help
    // =======================
    private void printHelp() {
        System.out.println("\nAvailable Commands:");
        System.out.println("  ls                       - List files in the current directory");
        System.out.println("  cd <directory>           - Change directory");
        System.out.println("  addPeer <ip> <port>      - Debug Only: Add a peer");
        System.out.println("  removePeer <ip> <port>   - Debug Only: Remove a peer");
        System.out.println("  listPeers                - List all active peers");
        System.out.println("  listFiles                - List available files");
        System.out.println("  upload <fileName>        - Upload a file");
        System.out.println("  download <fileId>        - Download a file");
        System.out.println("  reset                    - reset all settings");
        System.out.println("  exit / quit              - Exit CLI\n");
    }
}