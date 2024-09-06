package org.oldfr;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Javafx extends Application {
private static Scene scene;

@Override
    public void start(Stage stage) throws Exception{
        scene=new Scene(loadFXML("/newGen"),1330,912);
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