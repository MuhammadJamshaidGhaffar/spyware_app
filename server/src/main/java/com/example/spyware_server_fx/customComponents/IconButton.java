package com.example.spyware_server_fx;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;

public class IconButton extends ImageView {

    private final Image defaultImage;
    private final Image hoverImage;
    private final Image pressedImage;

    public IconButton(Image defaultImage, Image hoverImage, Image pressedImage) {
        this.defaultImage = defaultImage;
        this.hoverImage = hoverImage;
        this.pressedImage = pressedImage;

        // Set the default image
        setImage(defaultImage);

        // Add event listeners
        setOnMouseEntered(this::onMouseEntered);
        setOnMouseExited(this::onMouseExited);
        setOnMousePressed(this::onMousePressed);
        setOnMouseReleased(this::onMouseReleased);
    }

    private void onMouseEntered(MouseEvent event) {
        setImage(hoverImage);
    }

    private void onMouseExited(MouseEvent event) {
        setImage(defaultImage);
    }

    private void onMousePressed(MouseEvent event) {
        setImage(pressedImage);
    }

    private void onMouseReleased(MouseEvent event) {
        setImage(defaultImage);
    }
}
