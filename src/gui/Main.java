package gui;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

        
import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Font;
import javafx.stage.Stage;



/**
 *
 * @author Quinten Holmes
 */
public class Main extends Application {
    
    private AppController controller;
    private FXMLLoader fxmlLoader;
    private Parent root;
   @Override
   public void init() throws Exception {
       //Load resources
       Font.loadFont(getClass().getResource("/fonts/BRBELRT0.TTF").toExternalForm(), 12);
       Font.loadFont(getClass().getResource("/fonts/fa-solid-900.ttf").toExternalForm(), 12);
        
      controller = new AppController();
        
      fxmlLoader = new FXMLLoader(getClass().getResource("/gui/app.fxml"));
      fxmlLoader.setController(controller);
      
       root = (Parent) fxmlLoader.load();
   }
    
    @Override
    public void start(Stage stage) throws Exception {
        controller.setStage(stage);
        
        Scene scene = new Scene(root);        
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> controller.shutDown());
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint("Press F11 to toggles full-screen mode");
        
        //show main program
        stage.show();
        controller.afterInit();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        LauncherImpl.launchApplication(Main.class, AssetPandaPreloader.class, args);
    }
    
}
