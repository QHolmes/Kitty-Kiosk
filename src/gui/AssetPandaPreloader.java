/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import javafx.application.Preloader;
import javafx.application.Preloader.ProgressNotification;
import javafx.application.Preloader.StateChangeNotification;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Simple Preloader Using the ProgressBar Control
 *
 * @author Quinten Holmes
 */
public class AssetPandaPreloader extends Preloader {
    
    ProgressBar bar;
    Stage stage;
    
    private Scene createPreloaderScene() {
        bar = new ProgressBar();
        
        //Create label and style
        Label label = new Label("Asset Panda: Kitty Kiosk");
        label.setStyle("-fx-font-size: 40px;" + "-fx-alignment: center;"
                    + "-fx-font-family: Belgium;");
        
        // load the image
         Image image = new Image("images/assetPandaLogoWithText.png");
 
         // simple displays ImageView the image as is
         ImageView iV = new ImageView();
         iV.setImage(image);
         
        VBox p = new VBox(iV, label);
        p.spacingProperty().set(10);
        p.setStyle("-fx-alignment: center;" +
                   "-fx-background-color: linear-gradient(from 70% 30% to 100% 100%, #ebeaea, #a4a4a4)");
        
        return new Scene(p, 500, 250);        
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        stage.setScene(createPreloaderScene());
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setAlwaysOnTop(true); 
        stage.show();
    }
    
    @Override
    public void handleStateChangeNotification(StateChangeNotification scn) {
        if (scn.getType() == StateChangeNotification.Type.BEFORE_START) {
            stage.hide();
        }
    }
    
    @Override
    public void handleProgressNotification(ProgressNotification pn) {
        bar.setProgress(pn.getProgress());
    }    
    
}
