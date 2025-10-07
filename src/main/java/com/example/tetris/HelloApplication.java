package com.example.tetris;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

/**
 * The Hello application class.
 */
public class HelloApplication extends Application {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        URL css = getClass().getResource("/Styles/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();

    }
}