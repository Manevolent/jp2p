package com.manevolent.jp2p;

import com.manevolent.jp2p.client.NetworkClient;
import com.manevolent.jp2p.client.ssl.SSL;
import com.manevolent.jp2p.extensible.socket.client.NativeSocketClient;
import com.manevolent.jp2p.extensible.socket.server.NativeTcpServer;
import com.manevolent.jp2p.server.NetworkServer;

import javax.net.ssl.TrustManager;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;

public class SSLServerTest {

    public static void main(String[] args) throws Exception {
        KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        keygen.initialize(1024);
        final KeyPair pair = keygen.generateKeyPair();

        final X509Certificate certificate = SSL.generateCertificate("cn=\"localhost\"", pair, 365, "SHA1withRSA");

        final InetSocketAddress serverAddr =
                new InetSocketAddress(InetAddress.getByName("localhost"), 11223);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket();
                    serverSocket.bind(serverAddr);
                    NetworkServer networkServer = new NativeTcpServer(serverSocket);
                    NetworkClient client = SSL.wrapServer(
                            networkServer.accept(),
                            new TrustManager[0],
                            certificate,
                            pair.getPrivate(),
                            false
                    );
                    System.err.println("Server accepted " + client.getRemoteEndpoint());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String line;
                    while (client.isConnected()) {
                        if ((line = reader.readLine()) != null)
                            System.err.println("Client said: \"" + line + "\"");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();


        Socket clientSocket = new Socket();
        clientSocket.connect(serverAddr);
        NetworkClient dataLayer = new NativeSocketClient(clientSocket, true);

        System.err.println("Client connected to " + dataLayer.getRemoteEndpoint() + "...");

        NetworkClient securityLayer = SSL.wrapClient(dataLayer, certificate);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(securityLayer.getOutputStream()));

        while (securityLayer.isConnected()) {
            writer.write("Test.");
            writer.newLine();
            writer.flush();
        }
    }

}
