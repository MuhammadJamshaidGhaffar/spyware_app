package com.example.spyware_server_fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HelloController implements Initializable {

    @FXML
    private Pane main_panel;
    @FXML
    public Button start_server_btn;
    @FXML
    public Circle live_indicator;
    @FXML
    private Button console_btn;

    @FXML
    private Button gui_btn;

    //    @FXML
//    public ImageView live_indicator;
//    public Image live_image;
//    public Image offline_image;
    //------- Start Server Panel ----------
    public Pane startServerPanel = null;
    public Scene startServerScene = null;
    public StartServerController start_server_controller = null;

    // --------- CMD PANEL ---------
    public Pane cmdPanel = null;
    CmdController cmd_controller = null;
    // --------- GUI PANEL ---------
    public Pane guiPanel = null;
    GuiController gui_controller = null;
    //-----------------------------
    public SpywareServer server = null;
    //------------------
    Stage stage = null;
    Scene mainScene = null;

    public HelloController(Stage stage){

        // setting the stage. This stage is repsonsible for closing current controller
        this.stage = stage;

//        live_image = GuiController.getIconImage(GuiController.resourcesIconsBasePath+"green_circle.png");
//        offline_image = GuiController.getIconImage(GuiController.resourcesIconsBasePath+"red_circle.png");

        try {
            loadStartServerPanel();
            loadCmdPanel();
            loadGUIPanel();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
        try {
            System.out.println("Cakked");
            switchToStartServerPanel();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }


    }

    @Override
    public void initialize(URL location, ResourceBundle resources){

        System.out.println("initliaze method of Hello COntroller called");
//        try {
//            switchToStartServerPanel();
//        }
//        catch (IOException e){
//            System.out.println(e.getMessage());
//        }
    }
    @FXML
    void on_server_panel_btn_click(ActionEvent event){
        if(server == null){
            try {
                switchToStartServerPanel();
            }
            catch (IOException e){
                System.out.println(e.getMessage());
            }
        }
        else{

            try {
                cmd_controller.appendlnOutputCmdText("Closing server...");
                server.closeServer();
                server = null;
//                start_server_btn.setText("Start Server");
            }
            catch (Exception e){
                System.out.println(e.getMessage());
                System.out.println("Failed to close Server");
            }
            stage.setScene(startServerScene);
        }
    }
//    @FXML
//    void on_disconnect_click(ActionEvent event) {
//        try {
//            server.clientSocket.close();
//        }
//        catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
//
//        try {
//            server.serverSocket.close();
//        }
//        catch (Exception e){
//            System.out.println(e.getMessage());
//        }
//        server =  null;
//
//    }
    @FXML
    void on_console_click(ActionEvent event) {
        try {
            switchToCmdPanel();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    @FXML
    void on_gui_btn_click(ActionEvent event) {
        try {
            switchToGUIPanel();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    public void setScene(Scene mainScene){
        this.mainScene = mainScene;
    }

    void loadStartServerPanel() throws IOException{
        if(startServerPanel == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("startServerPanel.fxml"));
            start_server_controller = new StartServerController(this , stage);
            loader.setController(start_server_controller);
            startServerPanel = loader.load();
            startServerScene = new Scene(startServerPanel);
        }
    }

    void switchToStartServerPanel() throws IOException{
        loadStartServerPanel();

        System.out.println("Setting scene");
        stage.setScene(startServerScene);
//        main_panel.getChildren().setAll(startServerPanel);

    }
    void loadCmdPanel() throws IOException{
        if(cmdPanel == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("cmdPanel.fxml"));
            cmd_controller = new CmdController(this);
            loader.setController(cmd_controller);
            cmdPanel = loader.load();
        }
    }
    void switchToCmdPanel() throws IOException{
        loadCmdPanel();
        main_panel.getChildren().setAll(cmdPanel);
        console_btn.setStyle("-fx-background-color:#00e431;");
        gui_btn.setStyle("-fx-background-color:#03BD2C;");

    }
    void loadGUIPanel() throws IOException{
        if(guiPanel == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("guiPanel.fxml"));
            gui_controller = new GuiController(this);
            loader.setController(gui_controller);
            guiPanel = loader.load();
        }
    }
    void switchToGUIPanel() throws IOException{
        loadGUIPanel();
        main_panel.getChildren().setAll(guiPanel);
        gui_btn.setStyle("-fx-background-color:#00e431;");
        console_btn.setStyle("-fx-background-color:#03BD2C;");

    }
}

