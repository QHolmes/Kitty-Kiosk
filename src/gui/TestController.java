/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import core.Core;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckComboBox;
import org.json.JSONStringer;
import org.json.JSONWriter;

/**
 * FXML Controller class
 *
 * @author Quinten Holmes
 */
public class TestController implements Initializable {

    private CheckComboBox<String> cmb;
    @FXML TextField field;
    @FXML ComboBox combo;
    @FXML VBox box;
    
    private Core core;
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        String[] names = {"Joe", "Frank", "Bob"};
        cmb = new CheckComboBox<String>();
                    ((CheckComboBox) cmb).getItems().addAll((Object[]) names);
        
        box.getChildren().add(cmb);

    } 
    
    public void setCore(Core core){
        this.core = core;
    }
    
    @FXML
     private void handleButtonAction(ActionEvent event) {
        System.out.println("Field |-" + field.getText() + "-|");
        System.out.println("Field |-" + combo.getSelectionModel().getSelectedIndex() + "-|");
        System.out.println("Field |-" + cmb.getCheckModel().getCheckedItems() + "-|");
           
        JSONWriter writer = new JSONStringer().object();
        
        writer.key("field_1").value("Hello World");
        writer.endObject();
        System.out.println(writer.toString());
        
        core.getGPS();
        
          
    }
    
}
