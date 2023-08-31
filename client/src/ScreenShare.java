import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ScreenShare {
    private int port;
    private String host = "localhost";
    Thread thread = null;
    Socket socket = null;
    private volatile boolean running = true;
    public ScreenShare(String host ,int port){
        this.host = host;
        this.port = port;
    }
    public ScreenShare(String host){
        this(host ,8085);
    }
    public void start(){
        running = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Robot robot = new Robot();
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    Rectangle screenRect = new Rectangle(screenSize);

                    boolean isConnected = false;
                    while (running && !isConnected)
                        try {
                            socket = new Socket(host, port);
                            isConnected = true;
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            System.out.println("Failed to connect to server\nwaiting...");
                            Thread.sleep(100);
                        }


                    ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());

                    while (running) {
                        BufferedImage screenshot = robot.createScreenCapture(screenRect);

                        // Convert BufferedImage to byte array
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(screenshot, "png", baos);
                        byte[] imageBytes = baos.toByteArray();

                        // Send the image bytes over the socket connection
                        outputStream.writeObject(imageBytes);
                        outputStream.flush();

                        // Delay between each frame
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    System.out.println("Closing client socket");
                    try {
                        socket.close();
                    } catch (Exception e) {
                        System.out.println("failed to close screenshare socket");
                        running = false;
                    }
                }
            }
            }
        );
        thread.start();

    }
    public void stop(){
        running = false;
//        try {
//            thread.interrupt();
//        }catch (Exception e){
//            System.out.println("Failed to interrupt thread");
//        }

    }
}
