package p2pclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class P2PSettings {
    private static final String SETTINGS_FILE = "p2pSettings.json";

    private String serverAddress;
    private int serverPort;
    private int listeningPort;

    public static P2PSettings loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(file, P2PSettings.class);
            } catch (IOException e) {
                System.err.println("Error reading settings file: " + e.getMessage());
            }
        }
        return new P2PSettings();
    }

    @JsonIgnore
    public boolean isMissingValues() {
        return serverAddress == null || serverAddress.isBlank() || serverPort == 0 || listeningPort == 0;
    }

    public void promptForMissingValues(Scanner scanner) {
        if (serverAddress == null || serverAddress.isBlank()) {
            System.out.print("Enter server address (e.g., http://localhost): ");
            serverAddress = scanner.nextLine().trim();
        }
        if (serverPort == 0) {
            System.out.print("Enter server port (e.g., 33333): ");
            serverPort = Integer.parseInt(scanner.nextLine().trim());
        }
        if (listeningPort == 0) {
            System.out.print("Enter listening port for incoming file requests (e.g., 5000): ");
            listeningPort = Integer.parseInt(scanner.nextLine().trim());
        }
    }

    public void saveSettings() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File(SETTINGS_FILE), this);
            System.out.println("Settings saved to " + SETTINGS_FILE);
        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    public String getServerAddress() { return serverAddress; }
    public int getServerPort() { return serverPort; }
    public int getListeningPort() { return listeningPort; }
    @JsonIgnore
    public String getBaseUrl() { return serverAddress + ":" + serverPort; }
}