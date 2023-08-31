package com.example.spyware_server_fx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("main.fxml"));


        HelloController helloController = new HelloController(stage);
        fxmlLoader.setController(helloController);
        Parent root = fxmlLoader.load();
        helloController.setScene(new Scene(root));
//        Pane main_panel = (Pane) root.lookup("#main_panel");
//        try {
//            // Create a new panel to replace the current one
//            Pane newPanel = FXMLLoader.load(getClass().getResource("connect_panel.fxml"));
//
//            // Replace the current panel with the new one
//            main_panel.getChildren().setAll(newPanel);
//        }
//        catch (IOException e){
//            System.out.println(e.getMessage());
//        }

//        Scene scene = new Scene(root);
        stage.setTitle("Spyware");
        stage.setScene(helloController.startServerScene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}