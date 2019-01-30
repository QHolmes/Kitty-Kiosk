/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructures.entity;

import core.Core;
import background.BuildEntityHistory;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 *
 * @author Quinten Holmes
 */
public class EntityObject implements Serializable{
    private static final long serialVersionUID = 1;
    
    private final String entityID;
    private String entityName;
    private String filterStatus;
    private EntityInfo entityInfo;
    
    
    private transient Core core;
    private transient Logger LOGGER;
    private transient TextField searchField;
    private transient AnchorPane entityPane;
    private transient ImageView entityView;
    private transient AnchorPane entityScreen;
    private transient Label screenLabel;
    private transient TableView entityTable;
    private transient AnchorPane entityTablePane;
    private transient Thread updateThread;
    private transient Date lastUpdate;
    private transient BuildEntityHistory history;
    
    
    
    public EntityObject(String entityID, EntityInfo entityInfo){
        this.entityID = entityID;
        this.entityInfo = entityInfo;
    }
    
    public void initialize(Core core, EntityInfo entityInfo) throws IOException{
        this.core = core;
        LOGGER = core.getLogger();
        setUpPanes();
    }
    
    private void setUpPanes(){
        double textMult = 1;
        
        //---------------------------------
        //----------History Pane-----------
        //---------------------------------
        
        searchField = new TextField();
        searchField.setTooltip(new Tooltip());
        searchField.setPrefHeight(50);

        searchField.setMaxWidth(800);
        searchField.setMinWidth(200);
        searchField.setOnKeyPressed(e-> searchHistory(e));
        searchField.getStyleClass().add("checkTextField");

        Label label = new Label("Enter a string to search all fields.");
        label.setStyle("-fx-font-size: " + (12.0 * textMult) + "px;");

        VBox chFieldBox = new VBox(searchField, label);
        chFieldBox.setStyle("-fx-alignment: bottom_center;");
        chFieldBox.setMaxWidth(800);
        chFieldBox.setMinWidth(200);
        chFieldBox.setMaxHeight(83);
        chFieldBox.setMinHeight(83);

        label = new Label("History");
        label.setStyle("-fx-font-size: " + (40.0 * textMult) + "px;" 
                + "-fx-font-family: Belgium;");
        Button button = new Button("Reset");
        button.setStyle("-fx-font-size: " + (30.0 * textMult) 
                + "px;" + "-fx-font-family: Belgium;");
        button.getStyleClass().add("checkButton");
        button.setPrefHeight(40);
        button.setPrefWidth(150);
        button.setOnAction(e-> resetSearch(e));
        
        HBox chHBox = new HBox(label, chFieldBox, button);
        chHBox.setMaxWidth(1200);
        chHBox.setMinWidth(400);
        chHBox.setMaxHeight(200);
        chHBox.setMinHeight(100);
        
        chHBox.setSpacing(20);
        chHBox.setPadding(new Insets(15, 12, 15, 12));
        chHBox.setStyle("-fx-alignment: center;");
        
        HBox.setHgrow(chFieldBox, Priority.ALWAYS);
        
        
        AnchorPane.setLeftAnchor(chHBox, 0.0);
        AnchorPane.setRightAnchor(chHBox, 0.0);
        AnchorPane.setTopAnchor(chHBox, 70.0);
        
        entityTablePane = new AnchorPane();
        
        AnchorPane.setLeftAnchor(entityTablePane, 20.0);
        AnchorPane.setRightAnchor(entityTablePane, 20.0);
        AnchorPane.setTopAnchor(entityTablePane, 190.0);
        AnchorPane.setBottomAnchor(entityTablePane, 20.0);
       
        //Create screen for checkout
             entityView = new ImageView(core.getCat());
                entityView.setPreserveRatio(true);
                entityView.fitHeightProperty().set(100);
            
            screenLabel = new Label("Loading Action History");
            screenLabel.setStyle("-fx-font-size: 32px;" + "-fx-alignment: center;"
            + "-fx-font-family: Belgium;");
            screenLabel.setMaxWidth(450);
            screenLabel.setMinWidth(450);
            screenLabel.setLayoutY(154);
            
            AnchorPane screenVis = new AnchorPane(entityView, screenLabel);
            screenVis.setMaxHeight(220);
            screenVis.setMinHeight(220);
            screenVis.setMaxWidth(450);
            screenVis.setMinWidth(450);

            screenVis.setStyle("-fx-background-color: linear-gradient(from 70% 30% to 100% 100%, #ebeaea, #a4a4a4)");

            HBox screenBox = new HBox(screenVis); 
            screenBox.setStyle("-fx-alignment: center;");
        
        entityScreen = new AnchorPane(screenBox);
        entityScreen.setStyle("-fx-background-color: rgba(0, 0, 0, .4);");
        entityScreen.setVisible(false);
        
        label = new Label(entityName);
        label.getStyleClass().add("titleText");
        AnchorPane.setLeftAnchor(label, 0.0);
        AnchorPane.setRightAnchor(label, 0.0);
        AnchorPane.setTopAnchor(label, 2.0);

        entityPane = new AnchorPane(chHBox, entityTablePane, label, entityScreen);
        entityPane.setStyle("-fx-background-color: lightblue;");
        entityPane.getStyleClass().add("mainPane");
        
        AnchorPane.setLeftAnchor(entityPane, 0.0);
        AnchorPane.setRightAnchor(entityPane, 0.0);
        AnchorPane.setTopAnchor(entityPane, 0.0);
        AnchorPane.setBottomAnchor(entityPane, 0.0);
        
        entityPane.setVisible(false);
        
    }
    
    /**
     * Filters the history table to the current string in the search box. The 
     * current filter will be used if characters are add but will be reset
     * back to the original if the back space key is detected. Esc will clear the 
     * filter and search box.
     * @param e 
     */
    private void searchHistory(KeyEvent e){ 
        
        if(entityTable != null && history != null && history.isDone()){
            KeyCode code = e.getCode();
            ObservableList<ArrayList<String>> filteredList;
            
            String txtUsr;
            String input = searchField.getText().toLowerCase();

              if(code == KeyCode.ESCAPE){
                  resetSearch(null);
                  return;
              }
              if(input.isEmpty()){
                  //TODO change this to have a local saved list
                  filteredList = FXCollections.observableArrayList(history.getEntityList());
                  txtUsr = "";
              }else if(code == KeyCode.BACK_SPACE){
                  //TODO change this to have a local saved list
                  filteredList = FXCollections.observableArrayList(history.getEntityList());                       
                  txtUsr = input.substring(0, input.length() - 1);
              }else{
                  filteredList = FXCollections.observableArrayList(entityTable.getItems());                      
                  txtUsr = input + e.getText();
              }

                boolean found;
             if(!txtUsr.isEmpty())
                for(int i = filteredList.size() - 1; i >= 0 ; i--){
                    found = false;
                    for(int j = 0; j < filteredList.get(i).size() && !found; j++){
                        if(filteredList.get(i).get(j).toLowerCase().contains(txtUsr)){
                            found = true;
                            break;
                        }                                                   
                    }

                    if(!found)
                       filteredList.remove(i);
                }


            Platform.runLater(() -> { 
                entityTable.getItems().clear();
                entityTable.getItems().addAll(filteredList);
            });  
            
        }else{
            searchField.setText("");
        }
    }
    
    /**
     * Resets the search box and returns the full list to the history table.
     * @param event 
     */
    private void resetSearch(ActionEvent event){
        //TODO change this to have a local saved list
        if(history != null && history.isDone() && entityTable != null){
            entityTable.setItems(history.getEntityList());
            searchField.setText("");
        }
    }
    
    private void loadTables(){
        if(history == null || !history.isDone())
            return;
        
        //Set table on history pane
        new Thread(() -> {
                Platform.runLater(() -> { 
                    entityTable = history.getEntityTable();
                    AnchorPane.setLeftAnchor(entityTable, 0.0);
                        AnchorPane.setRightAnchor(entityTable, 0.0);
                        AnchorPane.setTopAnchor(entityTable, 0.0);
                        AnchorPane.setBottomAnchor(entityTable, 0.0);
                        
                    entityTablePane.getChildren().clear();
                    entityTablePane.getChildren().add(entityTable);
                });   
            }).start(); 
    }
   
    public Thread update(){
        if(entityTablePane == null)
            return null;
        
        if(updateThread != null && updateThread.isAlive())
            updateThread.interrupt();
        
        //Reload the action center
        EntityObject obj = this;
        
         Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                  try{
                    history = new BuildEntityHistory();
                    history.start();
                    loadTables();
                    lastUpdate = new Date();
                    
                  }catch(Exception inter){
                      LOGGER.log(Level.SEVERE, "Exception while updating EntityID-{0} EntityName-{1}: Message-{2}.",
                              new Object[]{entityID, entityName, inter.toString()});
                  }
                
                  return null;
              }   
            };
         
         updateThread = new Thread(task);
            updateThread.setDaemon(true);
            updateThread.setName("updateEntityCenter-"+entityName);
            updateThread.start();
        
        return updateThread;
    }

}
