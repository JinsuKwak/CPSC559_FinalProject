package p2pclient.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.net.*;

public class PortManager {
    private ServerSocket serverSocket;
    private int listeningPort;
    private String localIPAddress;

    public PortManager(int preferredPort) {
        this.listeningPort = findAvailablePort(preferredPort);
        this.localIPAddress = findLocalIPAddress(true);
    }

    private int findAvailablePort(int preferredPort) {
        try {
            this.serverSocket = new ServerSocket(preferredPort);
            this.listeningPort = preferredPort;
            return this.listeningPort;
        } catch (IOException e) {
            System.err.println("Port " + preferredPort + " is in use. Selecting a random available port...");
        }

        try {
            this.serverSocket = new ServerSocket(0);
            this.listeningPort = serverSocket.getLocalPort();
            System.out.println("Using port " + this.listeningPort);
            return this.listeningPort;
        } catch (IOException e) {
            throw new RuntimeException("Failed to find an available port", e);
        }
    }

    private String findLocalIPAddress(boolean test) {
        if (test) {
            return  "127.0.0.1";
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface netInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Failed to get local IP address: " + e.getMessage());
        }
        return "127.0.0.1";
    }


    public String getLocalIPAddress() {
        return localIPAddress;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void close() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server socket closed on port " + listeningPort);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}