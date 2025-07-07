// Main.java
package application;

import javafx.application.Application;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.HostServices;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Sample.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);

    
         String cssPath = "/application/css/styles.css";

        try {
      
            String cssUrl = getClass().getResource(cssPath).toExternalForm();
            scene.getStylesheets().add(cssUrl);
            System.out.println("CSS başarıyla yüklendi: " + cssUrl);
        } catch (NullPointerException e) {
            System.err.println("HATA: CSS dosyası bulunamadı! Yol: " + cssPath);
         
            e.printStackTrace();
        }


        stage.setTitle("Sinema Bilet Otomasyonu");
        stage.setScene(scene);
        stage.getProperties().put("hostServices", getHostServices());

        stage.show();
    }
 

    public static void main(String[] args) {
        launch(args);
    }
}