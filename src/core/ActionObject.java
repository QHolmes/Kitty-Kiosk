/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core;

import background.ActionEntityReturnChecker;
import http.Actions;
import http.Entities;
import background.BuildActionHistory;
import dataStructures.actions.ActionPane;
import dataStructures.actions.ActionTransaction;
import dataStructures.actions.ActionInfo;
import dataStructures.GUIScale;
import dataStructures.actions.ActionObjectEntite;
import javafx.util.Pair;
import helperClasses.AwesomeIcons;
import helperClasses.TextFit;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import static javafx.scene.layout.Region.USE_PREF_SIZE;
import javafx.scene.layout.VBox;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 *
 * @author Quinten Holmes
 */
public class ActionObject implements Serializable{
    private static final long serialVersionUID = 1;
    
    private boolean requirePin = false;
    private boolean pullHistory = false;
    private boolean showHistory = false;
    private boolean showCheckedOut = false;
    private final String actionID;
    private final String entityID;
    private int code = 0;
    private final ActionInfo actInfo;
    
    private transient SimpleBooleanProperty requirePinProp;
    private transient SimpleBooleanProperty pullHistoryProp;
    private transient SimpleBooleanProperty showHistoryProp;
    private transient SimpleBooleanProperty showCheckedOutProp;
    private transient Core core;
    private transient TextField checkoutField;
    private transient TextField historyField;
    private transient TableView historyTable;
    private transient TableView checkedOutTable;
    private transient AnchorPane checkedOutTablePane;
    private transient AnchorPane checkoutPane;
    private transient AnchorPane historyPane;
    private transient AnchorPane loadingScreen;
    private transient AnchorPane historyScreen;
    private transient AnchorPane historyTablePane;
    private transient AnchorPane mainPane;
    private transient HBox actionArea;
    private transient HBox notificationPane;
    private transient HBox checkHBox;
    private transient StackPane notificationStack;
    private transient ScrollPane actionFieldScrollPane;
    private transient Thread  historyThread;
    private transient Thread updateThread;
    private transient Thread hideCover;
    private transient Thread loadActionCenter;
    private transient ProgressIndicator tableIndicator;
    private transient boolean waitingActionInfo = false;
    private transient boolean coverScreen = false;
    private transient boolean waitingHistory = false;
    private transient ActionCenter center;
    private transient BuildActionHistory history;
    private transient ActionObject actionObject;
    private transient Date lastUpdate;
    private transient Label screenLabel;
    private transient Label screenSubLabel;
    private transient SimpleStringProperty screenLabelText;
    private transient SimpleStringProperty screenLabelSubText;
    private transient SimpleStringProperty screenLabelStyle;
    private transient SimpleStringProperty screenSubLabelStyle;
    private transient String entityObjectID;
    private transient ImageView checkoutView;
    private transient ImageView historyView;
    private transient ActionPane actionPane;
    private transient FadeTransition checkFade;
    private transient Button checkButton;
    private transient ActionTransaction transaction;
    private transient Thread actionReturn;
    
    
    public ActionObject(String actionID, String entityID, Core core) throws IOException{
        this.actionID = actionID;
        this.entityID = entityID;
        this.core = core;
        actInfo = new ActionInfo(actionID, entityID, core);
    }
    
    /**
     * Sets if a pin should be requested before showing the pane. The check should
     * be done by the GUI
     * @param b 
     */
    public void setRequirePin(boolean b){
        requirePin = b;
        requirePinProp.set(b);

        //Changes need to be saved in the core
        core.saveActionsActive();
        
    }
    
    /**
     * Returns true if the GUI should require a pin before displaying.
     * @return 
     */
    public SimpleBooleanProperty getRequirePin(){
        return requirePinProp;
    }
    
    /**
     * Sets if the action history should pulled from Asset Panda. This will 
     * automatically set showCheckout and showHistory to false and stop any current
     * history gathering. 
     * @param b 
     */
    public void setPullHistory(boolean b){
        
        pullHistory = b;
        pullHistoryProp.set(b);
        loadTables();
        if(pullHistory){
            //Default option is to show CheckedOut table if the action is returnable
            if(actInfo.isReturnable)
                showCheckedOut = true;
            
            pullActionInfo(core);
        }else{
            if(history != null){
                if(historyThread != null)
                    historyThread.interrupt();
                history.interrupt();
                history = null;
                waitingHistory = false;
                setWaiting();
            }
            
            
            setShowCheckOut(false);
            setShowHistory(false);
        }
        
        if(b && actInfo.isReturnable)
            showCheckedOutProp.set(true);
        
        if(!b){
          showCheckedOutProp.set(false); 
          showHistoryProp.set(false);
        }
        
        
        
        //Changes need to be saved in the core
        core.saveActionsActive();
    }
    
    /**
     * Returns true if action history data is being pulled from Asset Panda
     * @return 
     */
    public SimpleBooleanProperty getPullHistory(){
        return pullHistoryProp;
    }
    
    /**
     * Sets if the history table should be shown instead of the checkout table.
     * If set true the checkout table will be hidden (if currently shown)
     * @param b 
     */
    public void setShowHistory(boolean b){
        if(b)
            showCheckedOut = false;
        
        showHistory = b;
        showHistoryProp.set(b);
        
        loadTables();
        core.saveActionsActive();
    }
    
    /**
     * Returns true if the history table is being shown on the main pane.
     * @return 
     */
    public SimpleBooleanProperty getShowHistory(){
        return showHistoryProp;
    }
    
    /**
     * Sets if the checkout table is seen on the main screen. If true will hide 
     * the history table (if shown on the main pane). Can not be set true if the 
     * action has no return option.
     * @param b 
     */
    public void setShowCheckOut(boolean b){
        if(actInfo.isReturnable){
            if(b)
                showHistory = false;
            
            showCheckedOut = b;
            showCheckedOutProp.set(b);
            
            loadTables();
            
            //Changes need to be saved in the core
            core.saveActionsActive();
        }            
    }
    
    /**
     * Returns ture if the checkout table is being displayed on the main pane.
     * @return 
     */
    public SimpleBooleanProperty getShowCheckOut(){
        return showCheckedOutProp;
    }
    
    /**
     * Returns true if the action has a return option.
     * @return 
     */
    public boolean isReturnable(){
        return actInfo.isReturnable;
    }
    
    /**
     * Returns the name of this action.
     * @return 
     */
    public String getActionName(){
        return actInfo.actionName;
    }
    
    /**
     * Filters the history table to the current string in the search box. The 
     * current filter will be used if characters are add but will be reset
     * back to the original if the back space key is detected. Esc will clear the 
     * filter and search box.
     * @param e 
     */
    private void searchHistory(KeyEvent e){ 
        
        if(historyTable != null && history != null && history.isDone()){
            KeyCode keyCode = e.getCode();
            ObservableList<ActionObjectEntite> filteredList;
            
            String txtUsr;
            String input = historyField.getText().toLowerCase();

            //Check if it was an Esc
            if(keyCode == KeyCode.ESCAPE){
                resetHistorySearch(null);
                return;
            }
            
            //If the key is backspace, reload the results. Else build on the current
            if(keyCode == KeyCode.BACK_SPACE){
                filteredList = FXCollections.observableArrayList(history.getHistoryList());
                if(input.isEmpty())
                    txtUsr = input;
                else
                    txtUsr = input.substring(0, input.length() - 1);
            }else{
                filteredList = FXCollections.observableArrayList();
                filteredList.addAll(historyTable.getItems());
                txtUsr = input + e.getText();
            }

            //Check each entry for the substring, remove any that do not conatin
            if(!txtUsr.isEmpty())
                for(int i = filteredList.size() - 1; i >= 0 ; i--){
                    if(!filteredList.get(i).contains(txtUsr))
                       filteredList.remove(i);
            }

            //Update the table when ready
            Platform.runLater(() -> { 
                historyTable.getItems().clear();
                historyTable.getItems().addAll(filteredList);
            });  
            
        }else{
            historyField.setText("");
        }
    }

    public ActionInfo getActInfo() {
        return actInfo;
    }
    
    private void checkoutButtonAction(ActionEvent event){
        //Check if the field is empty, if it is reset formating and do nothing
        if(checkoutField.getText() == null || checkoutField.getText().isEmpty()){
            clearCheckoutField();
            return;
        }
        
        if(transaction != null && transaction.isAlive())
            transaction.interrupt();
        
        transaction = new ActionTransaction(this, actionID, entityID, actInfo.actionName, checkoutField.getText(),actionPane, core);
        transaction.start();
    }
    
    
    /**
     * Checks if the given entityObjectID needs to be returned for this ActionObject.
     * @param entityObjectID of the entity to check
     * @param displayMessage if a loading message needs to be displayed
     * @return if the entity needs to be returned
     */
    public boolean canBeReturned(String entityObjectID, boolean displayMessage){
        boolean canbeReturned = false;
        if(center.isReturnable()){
            if(displayMessage){
                updateLoading("Checking return options", "--");
                loadingAsset(true);
            }
            
            try {
                Pair<Boolean, String> pair = Actions.checkActionReturn(core, entityObjectID, actionID);
                canbeReturned = pair.getKey();
            } catch (IOException ex) {
                core.getLogger().log(Level.WARNING, "Could not determin if {0} needs to be returned: Action Name {1}", new Object[]{entityObjectID, actInfo.actionName});
            }
            
            if(displayMessage)
                loadingAsset(false);
        }
        
        return canbeReturned;
    }
    
    /**
     * Resets the search box and returns the full list to the history table.
     * @param event 
     */
    private void resetHistorySearch(ActionEvent event){
        //TODO change this to have a local saved list
        if(history != null && history.isDone() && historyTable != null){
            historyTable.setItems(history.getHistoryList());
            historyField.setText("");
        }
    }
    
    public void loadingAsset(boolean b) {
        if(loadingScreen.isVisible() == b)
            return;
        
        try{
        Platform.runLater(() -> {
            loadingScreen.setVisible(b);  
            
            if(!b){
                updateLoading("NULL", "--");
                checkoutField.requestFocus();
                setWaiting();
            }else{
                checkoutView.setImage(core.getCat());
            }
        });//End runnable
        
        }catch( Exception e){
           core.getLogger().log(Level.SEVERE, "Error loading asset, Exception-", e.getMessage());
        }
        
    }

    private void cover(boolean b) {
        if( coverScreen != b)
            core.getController().setCover(b);
        
        coverScreen = b;
    }
    
    private void setUpPanes(){
        double textMult = 1;
        Label label;
        Button button;
        
        //---------------------------------
        //---------Checkout Pane-----------
        //---------------------------------
        
            checkoutField = new TextField();
            checkoutField.setTooltip(new Tooltip());
            checkoutField.setPrefHeight(50);

            checkoutField.setMaxWidth(800);
            checkoutField.setMinWidth(200);
            checkoutField.setOnAction(e-> checkoutButtonAction(e));
            checkoutField.getStyleClass().add("checkTextField");

            label = new Label("Enter barcode to checkout or return.");
            label.setStyle("-fx-font-size: " + (12.0 * textMult) + "px;");

            VBox chFieldBox = new VBox(checkoutField, label);
            chFieldBox.setStyle("-fx-alignment: bottom_center;");
            chFieldBox.setMaxWidth(800);
            chFieldBox.setMinWidth(200);
            chFieldBox.setMaxHeight(83);
            chFieldBox.setMinHeight(83);

            label = new Label("Asset  ");
            label.setStyle("-fx-font-size: " + (40.0 * textMult) + "px;"
                    + "-fx-font-family: Belgium;");

            button = new Button("Enter");
            button.setStyle("-fx-font-size: " + (30.0 * textMult) + "px;" + "-fx-font-family: Belgium;");
            button.getStyleClass().add("checkButton");
            button.setOnAction(e-> checkoutButtonAction(e));
            button.setPrefHeight(40);
            button.setPrefWidth(150);

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

            HBox checkIndicator = new HBox(); //new HBox(tableIndicator);


            checkIndicator.setStyle("-fx-alignment: center;");

            AnchorPane.setLeftAnchor(checkIndicator, 0.0);
            AnchorPane.setRightAnchor(checkIndicator, 0.0);
            AnchorPane.setTopAnchor(checkIndicator, 0.0);
            AnchorPane.setBottomAnchor(checkIndicator, 0.0);

            HBox.setHgrow(checkIndicator, Priority.ALWAYS);

            checkedOutTablePane = new AnchorPane(checkIndicator);

            AnchorPane.setLeftAnchor(checkedOutTablePane, 20.0);
            AnchorPane.setRightAnchor(checkedOutTablePane, 20.0);
            AnchorPane.setTopAnchor(checkedOutTablePane, 190.0);
            AnchorPane.setBottomAnchor(checkedOutTablePane, 20.0);
        
        //---------------------------------
        //---------Loading screen----------
        //---------------------------------
            checkoutView = new ImageView(core.getCat());
                checkoutView.setPreserveRatio(true);
                checkoutView.fitHeightProperty().set(150);
                checkoutView.setLayoutX(174);
                checkoutView.setLayoutY(30);
            
            screenLabelText = new SimpleStringProperty();
            screenLabelSubText = new SimpleStringProperty();
            
            
            screenLabelStyle = new SimpleStringProperty();
            screenLabelStyle.set("-fx-font-size: 32px;" + "-fx-alignment: center;"
                    + "-fx-font-family: Belgium;");
            
            //Check the text fits the box
            screenLabelText.addListener((arg, oldVal, newVal) -> {
                int fontSize = 32;
                fontSize = TextFit.getTextFontSize(newVal, fontSize, 450.0);
                 screenLabelStyle.set("-fx-font-size: "+fontSize+"px;" + "-fx-alignment: center;"
                    + "-fx-font-family: Belgium;");
            });
            
            screenSubLabelStyle = new SimpleStringProperty();
            screenSubLabelStyle.set("-fx-font-size: 22px;" + "-fx-alignment: center;"
                    + "-fx-font-family: Belgium;");
            
            screenLabelSubText.addListener((arg, oldVal, newVal) -> {
                int fontSize = 22;
                fontSize = TextFit.getTextFontSize(newVal, fontSize, 450.0);
                 screenSubLabelStyle.set("-fx-font-size: "+fontSize+"px;" + "-fx-alignment: center;"
                    + "-fx-font-family: Belgium;");
            });
            
            screenLabel = new Label();
            
            screenLabel.textProperty().bind(screenLabelText);
            screenLabel.styleProperty().bind(screenLabelStyle);  
            screenLabelText.set("Loading Asset Information");
            
            screenLabel.setMaxWidth(450);
            screenLabel.setMinWidth(450);
            screenLabel.setLayoutY(154);
            
            screenSubLabel = new Label();
            
            screenSubLabel.textProperty().bind(screenLabelSubText);
            screenSubLabel.styleProperty().bind(screenSubLabelStyle);  
            screenLabelSubText.set("");
            
            screenSubLabel.setMaxWidth(450);
            screenSubLabel.setMinWidth(450);
            screenSubLabel.setLayoutY(190);
            
            
            AnchorPane screenVis = new AnchorPane(checkoutView, screenLabel, screenSubLabel);
            screenVis.setMaxHeight(220);
            screenVis.setMinHeight(220);
            screenVis.setMaxWidth(450);
            screenVis.setMinWidth(450);

            screenVis.setStyle("-fx-background-color: linear-gradient(from 70% 30% to 100% 100%, #ebeaea, #a4a4a4)");

            HBox screenBox = new HBox(screenVis); 
            screenBox.setStyle("-fx-alignment: center;");
        
            AnchorPane.setLeftAnchor(screenBox, 0.0);
            AnchorPane.setRightAnchor(screenBox, 0.0);
            AnchorPane.setTopAnchor(screenBox, 0.0);
            AnchorPane.setBottomAnchor(screenBox, 0.0);
         
         
            loadingScreen = new AnchorPane(screenBox);
            loadingScreen.setStyle("-fx-background-color: rgba(0, 0, 0, .4);");
            loadingScreen.setVisible(false);
            loadingScreen.getStyleClass().add("mainPane");
            
            AnchorPane.setLeftAnchor(loadingScreen, 0.0);
            AnchorPane.setRightAnchor(loadingScreen, 0.0);
            AnchorPane.setTopAnchor(loadingScreen, 0.0);
            AnchorPane.setBottomAnchor(loadingScreen, 0.0);
        
            label = new Label(actInfo.actionName);
            label.getStyleClass().add("titleText");
            AnchorPane.setLeftAnchor(label, 0.0);
            AnchorPane.setRightAnchor(label, 0.0);
            AnchorPane.setTopAnchor(label, 2.0);

        
        //---------------------------------
        //----------Notification-----------
        //---------------------------------

            //Test Button
            Button test = new Button("Test");
            test.setOnAction(e -> {  });
            
            AnchorPane.setLeftAnchor(test, 0.0);
            AnchorPane.setTopAnchor(test, 0.0);
            
            checkoutPane = new AnchorPane(chHBox, checkedOutTablePane, label);
            checkoutPane.setStyle("-fx-background-color: lightsteelblue;");
            checkoutPane.getStyleClass().add("mainPane");

            AnchorPane.setLeftAnchor(checkoutPane, 0.0);
            AnchorPane.setRightAnchor(checkoutPane, 0.0);
            AnchorPane.setTopAnchor(checkoutPane, 0.0);
            AnchorPane.setBottomAnchor(checkoutPane, 0.0);
        
        
        //---------------------------------
        //----------History Pane-----------
        //---------------------------------
        
            historyField = new TextField();
            historyField.setTooltip(new Tooltip());
            historyField.setPrefHeight(50);

            historyField.setMaxWidth(800);
            historyField.setMinWidth(200);
            historyField.setOnKeyPressed(e-> searchHistory(e));
            historyField.getStyleClass().add("checkTextField");

            label = new Label("Enter a string to search all fields.");
            label.setStyle("-fx-font-size: " + (12.0 * textMult) + "px;");

            chFieldBox = new VBox(historyField, label);
            chFieldBox.setStyle("-fx-alignment: bottom_center;");
            chFieldBox.setMaxWidth(800);
            chFieldBox.setMinWidth(200);
            chFieldBox.setMaxHeight(83);
            chFieldBox.setMinHeight(83);

            label = new Label("History");
            label.setStyle("-fx-font-size: " + (40.0 * textMult) + "px;" 
                    + "-fx-font-family: Belgium;");
            button = new Button("Reset");
            button.setStyle("-fx-font-size: " + (30.0 * textMult) 
                    + "px;" + "-fx-font-family: Belgium;");
            button.getStyleClass().add("checkButton");
            button.setPrefHeight(40);
            button.setPrefWidth(150);
            button.setOnAction(e-> resetHistorySearch(e));

            chHBox = new HBox(label, chFieldBox, button);
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
        
            //Loading will be under the table, so while the table is not visable this
            //Will be there.

            tableIndicator = new ProgressIndicator();
            tableIndicator.setProgress(-1);
            tableIndicator.setPrefWidth(200);

            checkIndicator = new HBox(tableIndicator); //new HBox(tableIndicator);
            checkIndicator.setStyle("-fx-alignment: center;");

            AnchorPane.setLeftAnchor(checkIndicator, 0.0);
            AnchorPane.setRightAnchor(checkIndicator, 0.0);
            AnchorPane.setTopAnchor(checkIndicator, 0.0);
            AnchorPane.setBottomAnchor(checkIndicator, 0.0);

            HBox.setHgrow(checkIndicator, Priority.ALWAYS);

            historyTablePane = new AnchorPane(checkIndicator);

            AnchorPane.setLeftAnchor(historyTablePane, 20.0);
            AnchorPane.setRightAnchor(historyTablePane, 20.0);
            AnchorPane.setTopAnchor(historyTablePane, 190.0);
            AnchorPane.setBottomAnchor(historyTablePane, 20.0);
        
        
        
        //Create screen for checkout
             historyView = new ImageView(core.getCat());
                historyView.setPreserveRatio(true);
                historyView.fitHeightProperty().set(100);
            
            screenLabel = new Label("Loading Action History");
            screenLabel.setStyle("-fx-font-size: 32px;" + "-fx-alignment: center;"
            + "-fx-font-family: Belgium;");
            screenLabel.setMaxWidth(450);
            screenLabel.setMinWidth(450);
            screenLabel.setLayoutY(154);
            
            screenVis = new AnchorPane(historyView, screenLabel);
            screenVis.setMaxHeight(220);
            screenVis.setMinHeight(220);
            screenVis.setMaxWidth(450);
            screenVis.setMinWidth(450);

            screenVis.setStyle("-fx-background-color: linear-gradient(from 70% 30% to 100% 100%, #ebeaea, #a4a4a4)");

            screenBox = new HBox(screenVis); 
            screenBox.setStyle("-fx-alignment: center;");
        
            historyScreen = new AnchorPane(screenBox);
            historyScreen.setStyle("-fx-background-color: rgba(0, 0, 0, .4);");
            historyScreen.setVisible(false);

            label = new Label(actInfo.actionName);
            label.getStyleClass().add("titleText");
            AnchorPane.setLeftAnchor(label, 0.0);
            AnchorPane.setRightAnchor(label, 0.0);
            AnchorPane.setTopAnchor(label, 2.0);

            historyPane = new AnchorPane(chHBox, historyTablePane, label, historyScreen);
            historyPane.setStyle("-fx-background-color: lightblue;");
            historyPane.getStyleClass().add("mainPane");

            AnchorPane.setLeftAnchor(historyPane, 0.0);
            AnchorPane.setRightAnchor(historyPane, 0.0);
            AnchorPane.setTopAnchor(historyPane, 0.0);
            AnchorPane.setBottomAnchor(historyPane, 0.0);
        
            historyPane.setVisible(false);
        //---------------------------------
        //---------Notifications-----------
        //---------------------------------
            notificationStack = new StackPane();
            notificationPane = new HBox(notificationStack);

            notificationStack.getChildren().addListener(new ListChangeListener() {
                @Override
                public void onChanged(ListChangeListener.Change c) {
                    Platform.runLater(() -> {
                        notificationPane.setVisible(!notificationStack.getChildren().isEmpty());
                    });
                }

            });
            
            AnchorPane.setLeftAnchor(notificationPane, 0.0);
            AnchorPane.setRightAnchor(notificationPane, 0.0);
            AnchorPane.setTopAnchor(notificationPane, 0.0);
            AnchorPane.setBottomAnchor(notificationPane, 0.0);
            notificationPane.setStyle("-fx-alignment: center;");
            notificationPane.setVisible(false);
        
        //---------------------------------
        //-----------Action Box------------
        //---------------------------------
        
            actionPane = new ActionPane(center, this, core);
            actionArea = actionPane.getActionArea();

            
            mainPane = new AnchorPane(checkoutPane, historyPane, loadingScreen, notificationPane, actionArea);
            
            
        //---------------------------------
        //---------Checkmark Box-----------
        //---------------------------------
            //Create exit button
            checkButton = new Button(AwesomeIcons.OK);
            checkButton.getStyleClass().add("bars");

            checkButton.setStyle("-fx-font-size: " + 300 + "px;" + "-fx-alignment: center;"
                        + "-fx-text-fill: green;");

            DropShadow dropShadow = new DropShadow();
            dropShadow.setRadius(10.0);
            dropShadow.setOffsetX(3.0);
            dropShadow.setOffsetY(3.0);
            dropShadow.setColor(Color.color(0.4, 0.5, 0.5));

            checkHBox = new HBox(checkButton);
            checkHBox.setEffect(dropShadow);

            AnchorPane.setLeftAnchor(checkHBox, 0.0);
            AnchorPane.setRightAnchor(checkHBox, 0.0);
            AnchorPane.setTopAnchor(checkHBox, 0.0);
            AnchorPane.setBottomAnchor(checkHBox, 0.0);
            checkHBox.setStyle("-fx-alignment: center;");
            checkButton.setOnAction(e -> notificationStack.getChildren().remove(checkHBox));
            
            checkFade = new FadeTransition(Duration.millis(2000), checkButton);
            checkFade.setFromValue(1.0);
            checkFade.setToValue(0.0);
            checkFade.setCycleCount(1);
            checkFade.setOnFinished((ActionEvent event) -> notificationStack.getChildren().remove(checkHBox));
            
        
    }
    
    /**
     * 
     * @param rowData
     */
    public void openActionPaneTable(ActionObjectEntite rowData){
        core.getLogger().log(Level.INFO, "Displaying action window: action ID-{0} action Name-{1}",
                        new Object[]{actionID, actInfo.actionName});
        this.entityObjectID = rowData.entityObjectID;
        
        if(!actionArea.isVisible())
            Platform.runLater(() -> {
                ActionTransaction trans = new ActionTransaction(this, actionID, entityID, actInfo.actionName, rowData.entityObjectPrimary, rowData, actionPane, core);
                trans.start();
            });//End runnable
    }
    
    /**
     * Will set the visibility of the history pane to the given value if 
     * pullHistory == true.
     * @param b visibility of history pane.
     */
    public void showHistory(boolean b){
        if(pullHistory){
            historyPane.setVisible(b);
            
            if(b){
                historyField.requestFocus();
                historyField.clear();
            }else{
                if(!loadingScreen.isVisible())
                    checkoutField.requestFocus();
                    
                checkoutField.clear();
            }
        }
        
       
    }
    
    /**
     * Will flip visibility of the history pane if pullHistory == true.
     */
    public void toggleHistory(){
        if(pullHistory){
            if(historyPane.isVisible())
                showHistory(false);
            else
                showHistory(true);
        }
            
    }
    
    /**
     * Returns the constructed pane for this action.
     * @return pane for the action
     */    
    public AnchorPane getPane(){
        return mainPane;
    }
    
    
    /**
     * 
     */
    private void closeActionPane(){
        if(actionArea.isVisible())
            Platform.runLater(() -> {
                actionArea.setVisible(false);
                checkoutField.setText(null);
                checkoutField.requestFocus();
            });//End runnable
    }
    
    /**
     * Gets action object info from Asset Panda. Calls setUpPanes to recreate 
     * the needed panes.
     * @param core
     * @param controller
     */
    public void pullActionInfo(Core core){
        //Set the current core (incase it was updated at some point)
        this.core = core;
        actInfo.updateColumns(core);
        setUpPanes();
        
        init();
        
        waitingActionInfo = true;
        setWaiting();
        
        core.getLogger().log(Level.INFO, "Pulling action info for action ID-{0} entity ID-{1}.", new Object[]{actionID, entityID});
        //Pull action history if needed, this would only be set if the action 
        //already existed but needed to be re-loaded after a restart.
        if(pullHistory && (history == null || history.isDone())){
            
            history = new BuildActionHistory(actInfo, this, core);
            history.setDaemon(true);
            
            
            Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                core.getLogger().log(Level.INFO, "Pulling history info for action ID-{0} entity ID-{1}.", new Object[]{actionID, entityID});
                waitingHistory = true;
                history.start();
                history.join();
                core.getLogger().log(Level.INFO, "History pulled for action ID-{0} entity ID-{1}.", new Object[]{actionID, entityID});
                loadTables();
                waitingHistory = false;
                setWaiting();                
                return null;
            }};
                      
            historyThread = new Thread(task);
                historyThread.setDaemon(true);
                historyThread.setName("WaitForHistory-"+actInfo.actionName);
                historyThread.start();
        }
        
        //Gets the action info and creates the ActionCenter.
        try {            
            ActionObject obj = this;
            
            core.getLogger().log(Level.INFO, "Creating action center for action ID-{0} entity ID-{1} ({2})", new Object[]{actionID, entityID, actInfo.actionName});
            //Create ActionCenter
            Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                
                  loadingAsset(true);
                  try{
                    center = new ActionCenter(core, actInfo, obj);
                    if(actionPane != null)
                        actionPane.setCenter(center);
                    
                    lastUpdate = new Date();
                    
                    core.getLogger().log(Level.INFO, "ActionCenter created for action ID [{0}] entity ID [{1}] action name [{2}]", new Object[]{actionID, entityID, actInfo.actionName});
                    
                    waitingActionInfo = false;
                    setWaiting();
                    loadingAsset(false);
                  }catch(IOException ex){
                      core.getLogger().log(Level.SEVERE, "Error creating ActionCenter for action ID [{0}] entity ID [{1}] action name [{2}], IOException: {3}", new Object[]{actionID, entityID, actInfo.actionName, ex.getMessage()});
                      updateLoading("Error: IOException. See log", "IOException");
                  }
                  
                  return null;
              }   
            };
                      
            loadActionCenter = new Thread(task);
                loadActionCenter.setDaemon(true);
                loadActionCenter.setName("loadActionCenter-"+actInfo.actionName);
                loadActionCenter.start();
          
        }catch (Exception ex) {
            core.getLogger().log(Level.SEVERE, "Error pulling action info for action ID-{0} "
                    + "entity ID-{1}, Exception: {2}", new Object[]{actionID, entityID, ex.getMessage()});
            //TODO handle error
        } 
        
    }
    
    /**
     * Returns a menu button for this action object that has the action name
     * and icon attached. This ActionObject will be set to the setUserData for
     * the controller to call when selected.
     * @return 
     */
    public ToggleButton  getButton(){
        ToggleButton  button;
        Image image = new Image(actInfo.icon);
        
        ImageView imageview = new ImageView(image);
        ColorAdjust blackout = new ColorAdjust();
        blackout.setBrightness(-1.0);
        
        imageview.setEffect(blackout);
        imageview.setCache(true);
        imageview.setCacheHint(CacheHint.QUALITY);
        
        imageview.fitHeightProperty().bind(core.getScale().getLeftMenuIconHeight());
        imageview.setPreserveRatio(true);
        
        GUIScale scale = core.getScale();
        
        Label icon = new Label();
        icon.setGraphic(imageview);
            icon.getStyleClass().add("icons");
            icon.prefHeightProperty().bind(scale.getLeftMenuButtonHeight());
            icon.prefWidthProperty().bind(scale.getLeftMenuButtonHeight());
            icon.setMinHeight(USE_PREF_SIZE);
            icon.setMinWidth(USE_PREF_SIZE);
            icon.setMaxHeight(USE_PREF_SIZE);
            icon.setMaxWidth(USE_PREF_SIZE);
        
        
        button = new ToggleButton(actInfo.actionName, icon);
        button.prefWidthProperty().addListener((observable, oldValue, newValue) -> {            
            Scene testScene;
            double menuWidth = core.getScale().getLeftMenuWidth().getValue();
            double menuHeight = core.getScale().getLeftMenuButtonHeight().getValue();
            double maxTextWidth = menuWidth - menuHeight - (menuWidth/15) - 20;
            
            int fontSize = (int) Math.round(24 * (menuWidth / 250));
            int startFontSize = fontSize;
            
            if(fontSize < 4)
                fontSize = 24;
            
            boolean shrink = true;
            button.wrapTextProperty().set(false);

            Text testLabel = new Text(actInfo.actionName);
            testLabel.getStyleClass().add("menuButton");
            testLabel.setStyle("-fx-font-size: " + fontSize +"px;");

            do{
               testScene = new Scene(new Group(testLabel));
               testLabel.applyCss();
               double width = testLabel.getLayoutBounds().getWidth();
               if(width > maxTextWidth){
                   if(fontSize == (startFontSize - 8) && !button.wrapTextProperty().getValue()){
                       button.wrapTextProperty().set(true);
                       testLabel.wrappingWidthProperty().set(maxTextWidth);
                       fontSize += 2;
                   }
                   fontSize -= 2;
                   testLabel.setStyle("-fx-font-size: " + fontSize +"px;");
               }else{
                   shrink = false;
               }
            }while(shrink);
            
            button.setStyle("-fx-font-size: " + fontSize +"px;");
        });
        
        button.getStyleClass().add("menuButton");
            button.setStyle("-fx-font-size: 24px;");
            button.prefHeightProperty().bind(scale.getLeftMenuButtonHeight());
            button.prefWidthProperty().bind(scale.getLeftMenuWidth());
            button.setMinHeight(USE_PREF_SIZE);
            button.setMinWidth(USE_PREF_SIZE);
            button.setMaxHeight(USE_PREF_SIZE);
            button.setMaxWidth(USE_PREF_SIZE);
        
        //Set button name
        button.setUserData(this);
        return button;
    }
    
    private void loadTables(){
        if(history == null || !history.isDone() || !pullHistory)
            return;
        
        //Set table on main pane
        if(showCheckedOut){
           new Thread(() -> {
                Platform.runLater(() -> { 
                    checkedOutTablePane.getChildren().remove(checkedOutTable);
                    checkedOutTable = history.getReturnTable();
                       AnchorPane.setLeftAnchor(checkedOutTable, 0.0);
                       AnchorPane.setRightAnchor(checkedOutTable, 0.0);
                       AnchorPane.setTopAnchor(checkedOutTable, 0.0);
                       AnchorPane.setBottomAnchor(checkedOutTable, 0.0);
                    checkedOutTablePane.getChildren().add(checkedOutTable);
                    core.getLogger().log(Level.INFO, "Showing updated table for action-[{0}] in entity-[{1}]", new Object[]{actInfo.actionName, actInfo.entityName});
                    TableView temp = checkedOutTable;
                });   
            }).start();           
        }else if(showHistory){
            new Thread(() -> {
                Platform.runLater(() -> { 
                    checkedOutTablePane.getChildren().remove(checkedOutTable);
                    checkedOutTable = history.getHistoryTable();
                       AnchorPane.setLeftAnchor(checkedOutTable, 0.0);
                       AnchorPane.setRightAnchor(checkedOutTable, 0.0);
                       AnchorPane.setTopAnchor(checkedOutTable, 0.0);
                       AnchorPane.setBottomAnchor(checkedOutTable, 0.0);
                    checkedOutTablePane.getChildren().add(checkedOutTable);
                });   
            }).start(); 
        }
        
        //Set table on history pane
        new Thread(() -> {
                Platform.runLater(() -> { 
                    historyTable = history.getHistoryTable();
                    AnchorPane.setLeftAnchor(historyTable, 0.0);
                        AnchorPane.setRightAnchor(historyTable, 0.0);
                        AnchorPane.setTopAnchor(historyTable, 0.0);
                        AnchorPane.setBottomAnchor(historyTable, 0.0);
                        
                    historyTablePane.getChildren().clear();
                    historyTablePane.getChildren().add(historyTable);
                });   
            }).start(); 
    }
    
    /**
     * Adds the given action to the the history table and adds it to the checkedOut 
     * table if the action is not a return. If there is already an entry under
     * the given actionObject ID it will be removed before it is re-added. If there
     * is a checkedOut table and the action is a return (ie not a checkOut) the
     * actionObject ID will be removed from the checkedOut table.
     * @param event
     */
    protected void updateActionHistory(ActionObjectEntite event){
        
        if(historyTable == null){
            return;
        }
        
        new Thread(() -> {
                Platform.runLater(() -> { 
                    ObservableList<ActionObjectEntite> historyList = historyTable.getItems();
                    
                    for(ActionObjectEntite a : historyList){
                        if(a.equals(event)){
                            historyList.remove(a);
                            break;
                        }
                    }

                    historyList.add(event);

                    if(checkedOutTable != null){
                        ObservableList<ActionObjectEntite> checkoutList = checkedOutTable.getItems();
                        
                        for(ActionObjectEntite a : checkoutList){
                                if(a.equals(event)){
                                    checkoutList.remove(a);
                                    break;
                                }
                            }

                        if(!event.isReturned())
                            checkoutList.add(event);
                    }
                });   
            }).start(); 
    }

    public boolean isLoading() {
        return (waitingActionInfo || waitingHistory || !(history == null || history.isDone()));
    }
    
    private void setWaiting(){
        if(!waitingActionInfo || !waitingHistory){
            core.getController().doneWaiting(this);
        }
    }
        
    /**
     * Updates the ActionCenter and tables by creating a new one and swapping when
     * possible.
     * @return 
     */
    public Thread update(){
        if(checkedOutTablePane == null)
            return null;
        
        if(updateThread != null && updateThread.isAlive())
            updateThread.interrupt();
        
        //Reload the action center
        ActionObject obj = this;
        
         Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                  ActionCenter ac;
                  
                  try{
                     core.getLogger().log(Level.INFO, "Updating ActionCenter for ActionName-{0}.", actInfo.actionName);
                     ac = new ActionCenter(core, actInfo, obj);
                     

                     core.getLogger().log(Level.FINE, "ActionCenter for ActionName-{0} created, waiting to submit update.",
                            actInfo.actionName);

                    //Wait until the current ActionCenter is not showing
                    while(coverScreen)
                        Thread.sleep(500);

                    core.getLogger().log(Level.INFO, "ActionCenter for ActionName-{0} updated.", actInfo.actionName);
                    center = ac;
                    if(actionPane != null)
                        actionPane.setCenter(center);
                    
                    history = new BuildActionHistory(actInfo, actionObject, core);
                    history.start();
                    history.join();
                    
                    loadTables();
                    
                    if(actInfo.isReturnable){
                        ActionEntityReturnChecker checker = new ActionEntityReturnChecker(core, history.getReturnList(), actInfo.actionName);
                        checker.start();
                        checker.join();
                    }
                    
                    lastUpdate = new Date();
                  }catch(InterruptedException inter){
                      core.getLogger().log(Level.SEVERE, "Updating actionID-{0} actionName-{1} interrupted.",
                              new Object[]{actionID, actInfo.actionName});
                  }catch(IOException ex){
                      core.getLogger().log(Level.SEVERE, "Error updating actionID-{0} actionName-{1} IOException: {2}.",
                              new Object[]{actionID, actInfo.actionName, ex.getMessage()});
                  }catch (Exception e){
                      e.printStackTrace();
                  }
                  return null;
              }   
            };
         
         updateThread = new Thread(task);
            updateThread.setDaemon(true);
            updateThread.setName("updateActionCenter-"+actInfo.actionName);
            updateThread.start();
            
        return updateThread;
    }
    
    /**
     * Returns true if it has been over maxAgeSec since the last update completed. 
     * If an update is currently in progress it will always return false.
     * @return 
     */
    
    public boolean needsUpdate(){
        final Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.SECOND, -(core.getMaxDataAge() * core.getMaxDataModidier()));
        final Date date = calendar.getTime(); 
        
        return lastUpdate.before(date) && !(updateThread != null && updateThread.isAlive());
    }

    /**
     * Calling this will stop all threads owned by this Action Objects. This should 
     * be called before exiting the program or deleting an ActionObject.
     */
    void shutDown() {
        core.getLogger().log(Level.INFO, "ActionObject {0} shuting down.", actInfo.actionName);
        
        if(historyThread != null)
            historyThread.interrupt();
        if(updateThread != null)
            updateThread.interrupt();
        if(hideCover != null)
            hideCover.interrupt();
        if(transaction != null)
            transaction.interrupt();
        if(loadActionCenter != null)
            loadActionCenter.interrupt();
    }
    
    /**
     * This will update the loading screen label to the given string
     * @param string Text to display for loading
     * @param subString Sub text to display
     */    
    public void updateLoading(String string, String subString){
        new Thread(() -> {
            Platform.runLater(() -> { 
                try{
                    screenLabelText.set(string);
                    screenLabelSubText.set(subString);
                }catch(Exception e){
                    e.printStackTrace();
                }
            });   
        }).start();
    }

    /**
     * Calling this will stop all current actions of the action object, reset 
     * formatting of the text field and set focus to the text field. This should 
     * be called whenever the pane becomes active or when the Esc key is pressed.
     */
    public void focus() {
        notificationPane.setVisible(false);
        //Close action pane if open
        closeActionPane();
        historyPane.setVisible(false);
        
        //Stop trasnaction if ongoing       
        if(transaction != null)
            transaction.interrupt();
        
        //Give focus to the text field if not currently loading
        if(!loadingScreen.isVisible()){
            checkoutField.requestFocus();
            checkoutField.setText("");  
            //reset formatting
            new Thread(() -> {
            Platform.runLater(() -> { 
                checkoutField.setStyle("-fx-border-color: white");
                checkoutField.getTooltip().hide();
                });   
            }).start();
        }
        
        if(!waitingActionInfo)
            loadingAsset(false);
        
        if(actionReturn != null && actionReturn.isAlive())
            actionReturn.interrupt();
    }

    private void init() {
        requirePinProp = new SimpleBooleanProperty(requirePin);
        pullHistoryProp = new SimpleBooleanProperty(pullHistory);
        showHistoryProp = new SimpleBooleanProperty(showHistory);
        showCheckedOutProp = new SimpleBooleanProperty(showCheckedOut);
        
        actionObject = this;
    }
    
    /**
     * Checks if the given action is still valid. 
     * @return True if the action is still valid, else false.
     */
    public boolean checkValid(){
        //TODO write a check
        boolean isValid = false;
        try{
            Pair<String, Integer> getCall = Entities.getActions(core, entityID, false);
            switch(getCall.getValue()){
                case(200):
                    isValid = true;
                    break;
                case(401):
                    core.getController().validSetup();
                    break;
                default:
            }
        }catch(IOException e){
                    
        }
        return isValid;
    }
    
    public void clearCheckoutField(){ 
      new Thread(() -> {
        Platform.runLater(() -> { 
            checkoutField.setStyle("-fx-border-color: white");
            checkoutField.clear();
            checkoutField.getTooltip().hide();
            });   
        }).start();
    }
    
    public void popUp(String text){
        Button button;
        AnchorPane screenVis;
        HBox popUp;
        
        //Create exit button
        button = new Button("X");
        button.getStyleClass().add("checkButton");
        
        button.setStyle("-fx-font-size: " + 12);
        
        AnchorPane.setRightAnchor(button, 0.0);
        AnchorPane.setTopAnchor(button, 0.0);
        
        
        int fontSize = 32;
        fontSize = TextFit.getTextFontSize(text, fontSize, 430.0);
        
        Label label2 = new Label(text);
        label2.setStyle("-fx-font-size: " +fontSize+ "px;" + "-fx-alignment: center;"
                    + "-fx-font-family: Belgium;");
        
        AnchorPane.setLeftAnchor(label2, 0.0);
        AnchorPane.setRightAnchor(label2, 0.0);
        AnchorPane.setTopAnchor(label2, 90.0);
        
        screenVis = new AnchorPane(label2, button);
        screenVis.setMaxHeight(220);
        screenVis.setMinHeight(220);
        screenVis.setMaxWidth(450);
        screenVis.setMinWidth(450);

        screenVis.setStyle("-fx-background-color: linear-gradient(from 70% 30% to 100% 100%, #ebeaea, #a4a4a4)");
        
        
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(10.0);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(Color.color(0.4, 0.5, 0.5));
        
        popUp = new HBox(screenVis);
        popUp.setEffect(dropShadow);
       
        AnchorPane.setLeftAnchor(popUp, 0.0);
        AnchorPane.setRightAnchor(popUp, 0.0);
        AnchorPane.setTopAnchor(popUp, 0.0);
        AnchorPane.setBottomAnchor(popUp, 0.0);
        popUp.setStyle("-fx-alignment: center;");
        button.setOnAction(e -> notificationStack.getChildren().remove(popUp));
        
        Platform.runLater(() -> { 
            notificationStack.getChildren().add(popUp);
        });   
    }
    
    /**
     * Displays a green check mark that will clear after a second.
     */
    public void checkMark(){
        
        
        Platform.runLater(() -> { 
            checkButton.setOpacity(1.0);
            notificationStack.getChildren().add(checkHBox);
            checkFade.playFromStart();
        });   
    }
    
    public void returnActionEntitie(ActionObjectEntite rowData, TableView<ActionObjectEntite> returnTable){
        if(actionReturn != null && actionReturn.isAlive())
                        return;
                    
        actionReturn = new Thread(){
            @Override
            public void run(){
                Platform.runLater(() -> {
                    actionObject.updateLoading(rowData.entityObjectPrimary, "Validating information");
                    actionObject.loadingAsset(true);
                });

                boolean open = actionObject.canBeReturned(rowData.entityObjectID, true);

                if(Thread.currentThread().isInterrupted()) 
                    return;

                if(open){
                    actionObject.openActionPaneTable(rowData);
                }else{
                    Platform.runLater(() -> {
                        actionObject.loadingAsset(false);
                        actionObject.popUp("Asset has already been returned");
                    });

                    returnTable.getItems().remove(rowData);
                }
            }
        };

        actionReturn.setDaemon(true);
        actionReturn.start();
    }
    
        
}
