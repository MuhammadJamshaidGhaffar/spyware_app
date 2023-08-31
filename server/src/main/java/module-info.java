module com.example.spyware_server_fx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.spyware_server_fx to javafx.fxml;
    exports com.example.spyware_server_fx;
}