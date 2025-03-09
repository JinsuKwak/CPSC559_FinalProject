package p2pclient;

import p2pclient.config.P2PClientConfig;
import p2pclient.config.P2PSettings;
import p2pclient.net.P2PServer;
import p2pclient.service.*;
import p2pclient.cli.MainCLI;
import p2pclient.utils.PortManager;

import java.io.File;
import java.util.Scanner;

public class MainApp {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        P2PSettings settings = P2PSettings.loadSettings();
        if (settings.isMissingValues()) {
            System.out.println("Some settings are missing. Please enter them below:");
            settings.promptForMissingValues(scanner);
            settings.saveSettings();
        }
        P2PClientConfig config = new P2PClientConfig(settings);
        PortManager portManager = config.getPortManager();
        System.out.println(config);

        PeerService peerService = new PeerService(config);
        FileService fileService = new FileService(config, peerService);

        P2PServer p2pServer = new P2PServer(portManager.getServerSocket(), config.getMaxThreads(), config);
        new Thread(p2pServer::start).start();

        MainCLI cli = new MainCLI(peerService, fileService, scanner);
        cli.startCLI();

        scanner.close();
        p2pServer.stop();
        portManager.close();
    }

}