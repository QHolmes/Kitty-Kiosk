/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helperClasses;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;

/**
 *
 * @author dholmes
 */
public class TextFit {
    
    public static int getTextFontSize(String text, int starting, double maxSize){
        Scene testScene;
        int fontSize = starting;
        boolean shrink = true;

        Text testLabel = new Text(text);
        testLabel.setStyle("-fx-font-size: "+fontSize+"px;" + "-fx-alignment: center;"
            + "-fx-font-family: Belgium;");

        do{
           testScene = new Scene(new Group(testLabel));
           testLabel.applyCss();
           double width = testLabel.getLayoutBounds().getWidth();
           if(width > maxSize){
               fontSize -= 2;
               testLabel.setStyle("-fx-font-size: " + fontSize +"px;" + "-fx-alignment: center;"
            + "-fx-font-family: Belgium;");
           }else{
               shrink = false;
           }
        }while(shrink);

        return fontSize;
    }
    
}
