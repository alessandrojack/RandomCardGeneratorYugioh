package org.example.randomcardgenerator;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Launcher extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("/org/example/randomcardgenerator/controller/card-overview-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Yu-Gi-Oh! Card Manager");
        stage.setScene(scene);
        stage.show();
    }
}