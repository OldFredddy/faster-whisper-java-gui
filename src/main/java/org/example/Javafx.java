package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.IOException;

public class Javafx extends Application {
private static Scene scene;

@Override
    public void start(Stage stage) throws Exception{
        scene=new Scene(loadFXML("/newGen"),1329,828);
        stage.setScene(scene);
        stage.show();
        stage.setOnCloseRequest(c -> System.exit(0));

}

    static void setRoot(String fxml) throws IOException{
        scene.setRoot(loadFXML(fxml));
    }
   private static Parent loadFXML(String fxml) throws IOException{
        FXMLLoader fxmlLoader = new FXMLLoader(Javafx.class.getResource(fxml+".fxml"));
        return fxmlLoader.load();
   }
    public static void main(String[] args) {
        launch();
    }

}