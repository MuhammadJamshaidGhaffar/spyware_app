import javax.sound.sampled.*;
import java.io.DataOutputStream;
import java.net.Socket;

public class Microphone {
    public volatile boolean running = true;
    Thread thread ;
    private String host;
    private int port;
    public TargetDataLine microphone;
    public Socket socket;
    public Microphone(String host , int port){
        this.host = host;
        this.port = port;
    }
    public  void startStreaming() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                stopStreaming();
                try {
                    Thread.sleep(1000);
                }catch (Exception e){
                    System.out.println(e.getMessage());
                }
                try {
                    // Set up audio format
                    AudioFormat format = new AudioFormat(44100, 16, 2, true, true);

                    // Get the default microphone
                    microphone = AudioSystem.getTargetDataLine(format);

                    // Set up the data line info
                    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                    if (!AudioSystem.isLineSupported(info)) {
                        System.out.println("Microphone not supported");
                        return;
                    }

                    // Open the microphone and start capturing audio
                    microphone.open(format);
                    microphone.start();

                    // Connect to the server socket
                    boolean isConnected = false;
                    running = false;
                    for(int i=0;i<10&& !isConnected;i++) {
                        try {
                            socket = new Socket(host, port);
                            isConnected = true;
                            running = true;
                        }
                        catch (Exception e){
                            System.out.println(e.getMessage());
                            try {
                                Thread.sleep(1000);
                            } catch (Exception err) {
                                System.out.println(err.getMessage());
                            }
                        }
                    }
                    if(!isConnected){
                        System.out.println("Failed to open audio stream");
                        return;
                    }
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    System.out.println("Streaming microphone audio...");

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while (running) {
                        bytesRead = microphone.read(buffer, 0, buffer.length);
                        outputStream.write(buffer, 0, bytesRead);
                        outputStream.flush();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    System.out.println("[Microphone closing all sockets and audio streams]");
                    if (microphone != null) {
                        microphone.stop();
                        microphone.close();
                    }
                    try {
                        socket.close();
                    }
                    catch (Exception e){
                        System.out.println("[Microphone] Failed to close socket");
                        System.out.println(e.getMessage());
                    }
                }
            }
        });
        thread.start();

    }
    public void stopStreaming(){
        running = false;
        try{
            if (microphone != null) {
                System.out.println("closing microphone");
                microphone.stop();
                microphone.close();
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
