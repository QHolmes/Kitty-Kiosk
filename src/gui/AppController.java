/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui;

import dataStructures.actions.ActionInfo;
import core.Core;
import core.ActionObject;
import dataStructures.entity.EntityInfo;
import helperClasses.AwesomeIcons;
import helperClasses.PinThread;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import static javafx.scene.layout.Region.USE_PREF_SIZE;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * FXML Controller class
 *
 * @author Quinten Holmes
 */
public class AppController implements Initializable {

    //Navigation
        @FXML private Button menuButton;
        @FXML private Button histButton;
        @FXML private VBox menuBox;
        @FXML private AnchorPane testPane;
        @FXML private AnchorPane historyPane;
        @FXML private HBox menuBarPane; 
        @FXML private ScrollPane menuPane;
        @FXML private AnchorPane cover;
        private ToggleButton  settingsButton;
    
    //Settings menu
        @FXML private VBox settings;
        @FXML private TextField emailTF;
        @FXML private TextField clientTF;
        @FXML private TextField secretTF;
        @FXML private PasswordField passwordPF;
        @FXML private Label loginLabel;
        @FXML private CheckBox activateCB;
        @FXML private CheckBox pinCB;
        @FXML private CheckBox pullHistoryCB;
        @FXML private CheckBox replaceHistoryCB;
        @FXML private CheckBox showCheckCB;
        @FXML private TreeView actionList;
        @FXML private TreeView associatedTree;
        @FXML private Pane menuSpacerPane;
        @FXML private Pane mainSpacerPane;
        @FXML private TitledPane accountSettingsTab;
        @FXML private TitledPane entityActionsTab;
        @FXML private TitledPane applicationSettingsTab;
        @FXML private HBox checkboxCover;
        @FXML private Slider maxWidthSlider;
        @FXML private Button fullScreen;
        @FXML private ToggleGroup maxAgeToggle;
        @FXML private ToggleButton maxAgeHour;
        @FXML private ToggleButton maxAgeMin;
        @FXML private ToggleButton maxAgeSec;
        @FXML private Button maxAgeReset;
        @FXML private Button menuScaleButton;
        @FXML private Slider maxAgeSlider;
        @FXML private Slider menuScaleSlider;
        @FXML private PasswordField setPinField;
        @FXML private Button setPinButton;
        @FXML private Button removePinButton;
    //Information
        @FXML private TitledPane infoTab;
        @FXML private Label APICallsLabel;
    //Pin Pane
        @FXML private VBox pinPane;
        @FXML private Button pinEnter;
        @FXML private Button pinExit;
        @FXML private Button pinErase;
        @FXML private PasswordField pinField;
        
    //Operations
        private Map<String, ActionObject> actions = new HashMap(); 
        private TreeItem<String> selectedAction;
        private ActionObject selectedActionObject;
        private ToggleButton seletedMenuButton;
        private Stage primaryStage;
        private Scene scene;
        private final AppController controller = this;
        final private Core core = new Core();
        private Thread loginThread;
        private Thread activateActionThread;
        private PinThread pinThread;
    
    public AppController(){
        core.initialize(controller);
    }
    
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        core.getLogger().info("Initializing AppController...");
        
        prepareMenu();
        preparePin();
        prepareVisualSettings();
        
        addSettingsButton();
        
        infoTab.setExpanded(false);
        APICallsLabel.textProperty().bind(core.getAPICallsObservable());
        //Setup Settings menu
        maxAgeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue.intValue() != newValue.intValue())
                maxAgeSlide(newValue.intValue());
        });
        
        maxAgeSlider.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if(!newValue)
                core.saveGUISettings();
        });
        
        maxAgeSlider.setValue(core.getMaxDataAge());
        
        //Set the toggled button
        switch(core.getMaxDataModidier()){
            case(1):
                maxAgeSec.setSelected(true);
                break;
            case(60):
                maxAgeMin.setSelected(true);
                break;
            case(3600):
                maxAgeHour.setSelected(true);
                break;
            default:
                maxAgeResetClick(null);          
        }     
        
        
        //Note: the starting value for menu scale slider is set in afterInit();
        menuScaleSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue.intValue() != newValue.intValue()){
                core.getScale().setLeftMenuButtonHeight((double) newValue  * 0.24);
                core.getScale().setLeftMenuWidth((double) newValue);
            }
        });
        
        menuScaleSlider.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if(!newValue)
                core.saveGUISettings();
        });
        
        menuScaleButton.setOnAction( e -> {
            menuScaleSlider.setValue(250);
            core.saveGUISettings();
        });

        
        //Setup all data as needed
        validSetup();
        clearMainPane();
        settingsButtonClick(null);
        
        //Set listener for style needs
        actionList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> entitySelect((TreeItem<String>) newValue));
                
        core.getLogger().info("AppController initialization complete.");
    }  
    
    /**
     * Creates all the UI elements needed to customize the UI
     */
    private void prepareVisualSettings(){
       maxWidthSlider.setValue(new Double(core.getMainPaneWidth()));
       testPane.setMaxWidth(core.getMainPaneWidth());
       
       maxWidthSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            new Thread(() -> {testPane.setMaxWidth(newValue.intValue());}).start();
            core.setMainPaneWidth(newValue.intValue());
        }); 
    }
    
    /**
     * Creates the  the animations to open/ close the left side menu.
     */
    private void prepareMenu() {
        
        testPane.getStyleClass().add("mainPane");
        
        //Create opening and closing animations
        TranslateTransition openNav=new TranslateTransition(new Duration(350), menuPane);
        openNav.setToX(0);
        TranslateTransition closeNav=new TranslateTransition(new Duration(200), menuPane);

        //Resize the menu bar and the spacing atop the menu to match
        menuBarPane.prefHeightProperty().bind(core.getScale().getLeftMenuIconHeight());
        menuSpacerPane.prefHeightProperty().bind(core.getScale().getLeftMenuIconHeight());
        mainSpacerPane.prefHeightProperty().bind(core.getScale().getLeftMenuIconHeight());

        //Bind the hight and width of the menuButton 
        menuButton.prefHeightProperty().bind(core.getScale().getLeftMenuIconHeight());
        menuButton.prefWidthProperty().bind(core.getScale().getLeftMenuIconHeight());

        //Set the text and action of the menu button
        menuButton.setText(AwesomeIcons.REORDER);
        menuButton.setOnAction((ActionEvent evt)->{
            if(menuPane.getTranslateX()!=0){
                openNav.play();
            }else{
                closeNav.setToX(-(menuPane.getWidth() + 5));
                closeNav.play();
            }
        });

        //Bind the hight and width of the histButton 
        histButton.prefHeightProperty().bind(core.getScale().getLeftMenuIconHeight());
        histButton.prefWidthProperty().bind(core.getScale().getLeftMenuIconHeight());

        //Set the text and action of the history button
        histButton.setText(AwesomeIcons.ARCHIVE);
        histButton.setVisible(false);
        histButton.setOnAction(e -> historyButton(e));
        menuPane.addEventHandler(MouseEvent.MOUSE_EXITED, evt ->  {
            closeNav.setToX(-(menuPane.getWidth() + 5));
            closeNav.play();
        });

        //change font size of both icons when icon height changes
        menuButton.prefHeightProperty().addListener((observable, oldValue, newValue) -> {
            int fontSize = (int) Math.round(core.getScale().getLeftMenuIconHeight().getValue() * .75);
            menuButton.setStyle("-fx-font-size: "+fontSize+"px;");
            histButton.setStyle("-fx-font-size: "+fontSize+"px;");
        });
        
        //If the mouse leaves and then reenters the menu area before it is closed, reopen.   
        menuPane.addEventHandler(MouseEvent.MOUSE_ENTERED, evt -> openNav.play());
        
        //When the size is changed open the menu so the user can see how large the menu now is
        menuPane.prefWidthProperty().bind(core.getScale().getLeftMenuWidth());
        menuPane.prefWidthProperty().addListener((observable, oldValue, newValue) -> {
            openNav.play();
        });
    }
    
    public void setCover(boolean b){
        cover.setVisible(b);
    }
    
    private void clearMainPane(){
        Platform.runLater(() -> { 
            testPane.getChildren().clear();
            pinPane.setVisible(false);
            testPane.getChildren().add(pinPane);
        });
    }
    
    /**
     * Places the given pane in the center area. The menu will also be moved to 
     * the closed position if need be.
     * @param main 
     */
    private boolean setMainPane(Pane main){
        if(historyPane.isVisible())
               historyPane.setVisible(false);
        
        boolean success = false;
        
        
        try{
            //Clear old pane and set new one
            AnchorPane.setLeftAnchor(main, 0.0);
            AnchorPane.setRightAnchor(main, 0.0);
            AnchorPane.setTopAnchor(main, 0.0);
            AnchorPane.setBottomAnchor(main, 0.0);
            testPane.getChildren().clear();
            testPane.getChildren().add(main);
            pinPane.setVisible(false);
            testPane.getChildren().add(pinPane);
            
            success = true;
        }catch(NullPointerException e){
            
        }
        
        //Close menu
        TranslateTransition closeNav=new TranslateTransition(new Duration(200), menuPane);
                closeNav.setToX(-(menuPane.getWidth() + 5));
                closeNav.play();
                
        return success;
    }
    
    private void maxAgeSlide(int value) {
        core.setMaxDataAge(value);
    }
    
    @FXML
    private void maxAgeResetClick(ActionEvent e) {
        core.setMaxDataModidier(60);
        core.setMaxDataAge(3);
        maxAgeMin.setSelected(true);
        maxAgeSlider.setValue(3);
    }
    
    @FXML private void pinButtonInput(ActionEvent e) {
        pinField.appendText(((Button) e.getSource()).getText());
    }
    
    @FXML void pinFieldClear(ActionEvent e){
        pinField.clear();
        pinField.requestFocus();
    }
    
    @FXML void pinSubmit(ActionEvent e){
        if(pinThread != null){
            synchronized(pinThread){
               pinThread.notify();   
            }
        }    
    }
    
    @FXML void pinExit(ActionEvent e){
        pinField.clear();
        if(pinThread != null)
            pinThread.interrupt();
        
        showPin(false);
    }
    
    @FXML
    private void maxAgeHourClick(ActionEvent e) {
        core.setMaxDataModidier(3600);
    }
    
    @FXML
    private void maxAgeMinClick(ActionEvent e) {
        core.setMaxDataModidier(60);
    }
    
    @FXML
    private void maxAgeSecClick(ActionEvent e) {
        core.setMaxDataModidier(1);
    }
    
    @FXML
    private void fullScreen(ActionEvent e) {
        if(!primaryStage.isFullScreen()){
           primaryStage.setFullScreen(true);
        }else{
           primaryStage.setFullScreen(false); 
        }
    }
    
    @FXML
    private void resetMainPaneWidth(ActionEvent e) {
        maxWidthSlider.setValue(1350.0);
        new Thread(() -> {testPane.setMaxWidth(1350);}).start();
        core.setMainPaneWidth(1350);
    }
    
    
    @FXML void activateActionBox(ActionEvent e){
        try{
        if(activateCB.isSelected()){  
            String entityID = core.getEntityByName(selectedAction.getParent().getValue()).id;
            String actionID = core.getActionID(entityID, selectedAction.getValue());
            
           
           //Enable other boxes
           pinCB.setDisable(false);
           pinCB.setSelected(false);
           pullHistoryCB.setDisable(false);
           pullHistoryCB.setSelected(false);
           
           Task<Void> task = new Task<Void>() {
                 @Override
                 protected Void call() throws Exception {
                    createActionObject(actionID, entityID).join();
                    try{
                    //check if the same object is selected
                    if(associatedTree.getRoot() == null){
                        Platform.runLater(() -> { 
                            String acName = selectedAction.getValue();
                            ActionObject ob = actions.get(acName);
                            loadAssociatedEntities(ob);
                        });
                    }
                    
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    return null;
                }//End call
            };//End Task  

             activateActionThread = new Thread(task);
                 activateActionThread.setName("Creating ActionObject");
                 activateActionThread.setDaemon(true);
                 activateActionThread.start();
           
           
           checkboxLoading(true);
           
        }else{
           actions.remove(selectedAction.getValue());
           removeButton(selectedAction.getValue());
           
           core.removeAction(selectedAction.getValue());
           
           //diable other boxes
           pinCB.setDisable(true);
           pinCB.setSelected(false);
           pullHistoryCB.setDisable(true);
           pullHistoryCB.setSelected(false);
           replaceHistoryCB.setDisable(true);
           replaceHistoryCB.setSelected(false);
           showCheckCB.setDisable(true); 
           showCheckCB.setSelected(false);
           checkboxLoading(false);
           associatedTree.setRoot(null);
        }
        }catch (Exception f){
            core.getLogger().log(Level.SEVERE, "Error with activation box Exception-{0}", f.getMessage());
        }
    }
    
    @FXML
    private void pullHistoryBox(ActionEvent e) {
        ActionObject obj = actions.get(selectedAction.getValue());
        obj.setPullHistory(pullHistoryCB.isSelected());
        if(pullHistoryCB.isSelected()){
            if(obj.isReturnable())
                showCheckCB.setDisable(false); 
            else
               showCheckCB.setDisable(true); 
            
            replaceHistoryCB.setDisable(false);
        }else{
           showCheckCB.setDisable(true); 
           replaceHistoryCB.setDisable(true);
        }
        
        checkboxLoading(obj.isLoading());
    }
    
    @FXML
    private void checkoutTableBox(ActionEvent e) {
        ActionObject obj = actions.get(selectedAction.getValue());
        obj.setShowCheckOut(showCheckCB.isSelected());
        checkboxLoading(obj.isLoading());
    }
    
    @FXML
    private void pinCBBox(ActionEvent e) {
        ActionObject obj = actions.get(selectedAction.getValue());
        obj.setRequirePin(pinCB.isSelected());
    }
    
    @FXML
    private void historyButton(ActionEvent e) {
        
        if(selectedActionObject != null && selectedActionObject.getPullHistory().getValue())
            selectedActionObject.toggleHistory();
        
    }
    
    @FXML
    private void historyTableBox(ActionEvent e) {
        ActionObject obj = actions.get(selectedAction.getValue());
        obj.setShowHistory(replaceHistoryCB.isSelected());
        checkboxLoading(obj.isLoading());
    }
    
    /**
     * Checks if the current token is valid and then enables or disable items
     * as needed.
     * @return True if token is valid, else false.
     */
    public boolean validSetup(){
        core.getLogger().info("Checking token access...");
        boolean isValid = core.isValid();
        if(isValid){
            accountSettingsTab.setExpanded(false);
            entityActionsTab.setExpanded(true);
            entityActionsTab.setDisable(false);
            emailTF.setText(core.getEmail());
            clientTF.setText(core.getClientId());
            
            pullActions();
            loadActionObjects();
            core.getLogger().info("Token valid.");
        }else{
           accountSettingsTab.setExpanded(true);
           entityActionsTab.setExpanded(false); 
           infoTab.setExpanded(false);
           entityActionsTab.setDisable(true);
           core.getLogger().info("Token invalid, credentials needed.");
           clearMenu();
        }
        
        applicationSettingsTab.setExpanded(false);
        
        return isValid;
    }
    
    @FXML
    private void logOut(ActionEvent e) {
        if(core.isValid()){
            
            emailTF.setText("");
            passwordPF.setText("");
            secretTF.setText("");
            clientTF.setText("");

            core.LogOut();
            
            loginLabel.setTextFill(Color.web("#1fc424"));
            loginLabel.setText(null);
            loginLabel.setText("Logged out"); 
            
            validSetup();
            actionList.setRoot(null);
        }
    }
    
    @FXML
    private void login(ActionEvent e) {
	boolean filled = true;
        
        //Checks if all fields are filled, if not tells the user they need to be 
        //filled
        StringBuilder build = new StringBuilder();
        core.getLogger().info("Login attempt.");
        if(emailTF.getText().isEmpty()){
            componentAlert(emailTF, "Username cannot be empty");
            build.append("Username, ");
            filled = false;
        }else{
            componentAlert(emailTF, null);
        }
        if(passwordPF.getText().isEmpty()){
            componentAlert(passwordPF, "Password cannot be empty");
            build.append("Password, ");
            filled = false;
        }else{
            componentAlert(passwordPF, null);
        }
        if(secretTF.getText().isEmpty()){
            componentAlert(secretTF, "Secret ID cannot be empty");
            build.append("Secret ID, ");
            filled = false;
        }else{
            componentAlert(secretTF, null);
        }
        if(clientTF.getText().isEmpty()){
            componentAlert(clientTF, "Client ID cannot be empty");
            build.append("Client ID, ");
            filled = false;
        }else{
            componentAlert(clientTF, null);
        }
        
        //If the fields are not filled, create error message
        if(!filled){
            //Clears the last comma
            int lastIndex = build.lastIndexOf(",");
            int firstIndex = build.indexOf(",");
            build.deleteCharAt(lastIndex);
            
            //If there are more than one items in the list replace the last comma with a 
            //"and"
            if(firstIndex != lastIndex){
               lastIndex = build.lastIndexOf(",");
               
               //If there are only two items in the list the comma is not corret
               if(firstIndex != lastIndex)
                    build.replace(lastIndex, lastIndex + 1, ", and");
               else
                    build.replace(lastIndex, lastIndex + 1, " and");  
            }
            
            
            build.append("cannot be empty.");
            
            core.getLogger().log(Level.WARNING, "Login info missing: {0}; login not attempted", build.toString());
        }
        
        String responce = build.toString();
        //If all fields are filled check if valid
        if(filled){
            setCover(true);
            Task<Void> task = new Task<Void>() {
                 @Override
                 protected Void call() throws Exception {
                    core.getLogger().info("Attempting login, sending to core to process.");
                    String responce = core.login(emailTF.getText(), 
                             passwordPF.getText(),
                             secretTF.getText(),
                             clientTF.getText());

                    if(core.isValid()){
                        loginLabel.setTextFill(Color.web("#1fc424"));
                        core.getLogger().info("Login attempt successful.");
                        core.getEntitieMap();
                    }
                    else
                        loginLabel.setTextFill(Color.web("#c10d2b"));

                    new Thread(() -> {
                    Platform.runLater(() -> { 
                        loginLabel.setText(null);
                        loginLabel.setText(responce);
                        });   
                    }).start();
                     
                    
                    validSetup();
                    setCover(false);
                    return null;
                }//End call
            };//End Task  

             loginThread = new Thread(task);
                 loginThread.setName("Sorting buttons");
                 loginThread.setDaemon(true);
                 loginThread.start();
        }else{
            loginLabel.setTextFill(Color.web("#c10d2b"));
            
            loginLabel.setText(null);
            loginLabel.setText(responce); 

            validSetup();
        }
    }
    
    /**
     * Will set a red box around the button with the given 
     * String as the tool tip. If the tooltip is null or empty the 
     * alert will be cleared.
     * @param c
     * @param tooltip 
     */
    private void componentAlert(Control c, String tooltip){
        //Gets the tool tip, if there is no tip set it creates a new one
        Platform.runLater(() -> { 
            Tooltip tip = c.getTooltip();
            if(tip == null){
                tip = new Tooltip();
                tip.hide();
                c.setTooltip(tip);
            }

            if(tooltip == null || tooltip.isEmpty()){
                c.getStyleClass().removeAll("alertField");
                tip.hide();
            }else{
                c.getStyleClass().add("alertField");
                tip.setText(tooltip);
                /*
                Window stage = c.getScene().getWindow();

                Bounds bounds = c.localToScreen(c.getBoundsInLocal());
                double posX = bounds.getMinX();
                double posY = bounds.getMaxY();

                tip.show(c, posX, posY);
                */
            }
        });
    }
    
    /**
     * Creates the settings button and adds it to the menu.
     */
    private void addSettingsButton(){
        Label icon = new Label(AwesomeIcons.COG);
            icon.getStyleClass().add("icons");
            icon.setMinHeight(USE_PREF_SIZE);
            icon.setMinWidth(USE_PREF_SIZE);
            icon.setMaxHeight(USE_PREF_SIZE);
            icon.setMaxWidth(USE_PREF_SIZE);
            
        icon.prefHeightProperty().bind(core.getScale().getLeftMenuButtonHeight());
        icon.prefWidthProperty().bind(core.getScale().getLeftMenuButtonHeight());
            
        settingsButton = new ToggleButton("Settings", icon);
        settingsButton.setId("Settings");
        settingsButton.getStyleClass().add("menuButton");
            settingsButton.prefHeightProperty().bind(core.getScale().getLeftMenuButtonHeight());
            settingsButton.prefWidthProperty().bind(core.getScale().getLeftMenuWidth());
            settingsButton.setMinHeight(USE_PREF_SIZE);
            settingsButton.setMinWidth(USE_PREF_SIZE);
            settingsButton.setMaxHeight(USE_PREF_SIZE);
            settingsButton.setMaxWidth(USE_PREF_SIZE);
            settingsButton.setOnAction( e ->settingsButtonClick(e));
            settingsButton.setSelected(true);
        seletedMenuButton = settingsButton;    
        menuBox.getChildren().add(settingsButton);
        
        settingsButton.prefWidthProperty().addListener((observable, oldValue, newValue) -> {
            int fontSize = (int) Math.round(24 * (core.getScale().getLeftMenuWidth().getValue() / 250));
            
            if(fontSize < 4)
                fontSize = 24;
            
            boolean shrink = true;

            Text testLabel = new Text("Settings");
            testLabel.getStyleClass().add("menuButton");
            testLabel.setStyle("-fx-font-size: " + fontSize +"px;");

            Scene testScene;
            do{
               testScene = new Scene(new Group(testLabel));
               testLabel.applyCss();
               double width = testLabel.getLayoutBounds().getWidth();
               if(width > core.getScale().getLeftMenuWidth().getValue()){
                   fontSize -= 2;
                   testLabel.setStyle("-fx-font-size: " + fontSize +"px;");
               }else{
                   shrink = false;
               }
            }while(shrink);
            
            int iconSize = (int) Math.round(fontSize * 1.5);
            
            icon.setStyle("-fx-font-size: " + iconSize +"px;");
            settingsButton.setStyle("-fx-font-size: " + fontSize +"px;");
            
        });
    }
    
    /**
     * handles when the settings button is clicked. Because this is the only
     * button that does not have an ActionObject it needs its own action.
     * @param event Required, not used. Can be null.
     */
    private void settingsButtonClick(ActionEvent event){
        if(core.isRequireCode()){
            getPin(null, null);
        }else{
            showSettings();
        }
    }
    
    private void showSettings(){
        seletedMenuButton.setSelected(false);
        seletedMenuButton = settingsButton;
        seletedMenuButton.setSelected(true);
        selectedActionObject = null;
        histButton.visibleProperty().bind(new SimpleBooleanProperty(false));
        
        Platform.runLater(() -> { 
            setMainPane(settings);
        });
    }
    
    /**
     * Handles when a button is clicked. If the corresponding Action Object requires a pin
     * it will hand that off, else it will call displayActionObject to show
     * it's pane.
     * @param event Source of event, should be a ToggleButton
     */
    private void menuButtonClicked(ActionEvent event){
        //Check & resolve pin
        ActionObject ao = (ActionObject) ((ToggleButton) event.getSource()).getUserData();
        ToggleButton toggle = (ToggleButton) event.getSource();
        
        core.getLogger().log(Level.FINE, "Button clicked: Action Object [0]", ao.getActionName());        
        
        if(core.isRequireCode() && ao.getRequirePin().getValue()){
            getPin(ao, toggle);
        }
        else
            displayActionObject(ao, toggle);
    }
    
    /**
     * Switches the selectedMenuButton and selectedActionObject to the current one,
     * than has their pane displayed. If the pane fails to display the button will
     * be removed and the settings screen will be displayed.
     * @param ao
     * @param toggle 
     */
    private void displayActionObject(ActionObject ao, ToggleButton toggle) {
        //Swap the selected button
        seletedMenuButton.setSelected(false);
        seletedMenuButton = toggle;
        seletedMenuButton.setSelected(true);
        
        //Set the selected action
        selectedActionObject = ao;
        
        Platform.runLater(() -> { 
            //Display action pane. If displaying fails, remove button and action.
            if(setMainPane(selectedActionObject.getPane())){
                selectedActionObject.focus();

                //Shows or hides the history toggle button
                histButton.visibleProperty().bind(selectedActionObject.getPullHistory());
                core.getLogger().log(Level.INFO, "Showing pane for Action Object [{0}]", ao.getActionName());
            }else{
                core.getLogger().log(Level.WARNING, "Error showing Action Object [{0}], removing button.", ao.getActionName());
                removeButton(ao.getActionName());
                clearMainPane();
            }
        });
    }
    
    /**
     * Updates the tree view in the settings menu with all the entities and
     * their actions. Dose nothing if there is no valid token.
     */
    private void pullActions(){
        
        if(!core.isValid())
            return;
        
        Map<String, EntityInfo> entityMap = core.getEntitieMap();
        
        if(entityMap == null || entityMap.isEmpty())
            entityMap = core.getEntitieMap();
            
         if(entityMap != null && !entityMap.isEmpty()){
            core.getLogger().info("Getting action names for all entities.");
            //Create list for Entity combo box
                TreeItem<String> rootItem = new TreeItem<> ("ROOT");
                ColorAdjust blackout = new ColorAdjust();
                blackout.setBrightness(-1.0);

                ObservableList<String> options =  FXCollections.observableArrayList();
                entityMap.forEach((s, e) -> {
                    try{
                    //Get the entity icon and set it to black
                    Node image = new ImageView(new Image(e.icon, 30, 30, false, false));
                    image.setEffect(blackout);
                    
                    //Create item with the entity name and icon
                    TreeItem<String> item = new TreeItem<> (e.name, image);                    
                    
                    //Add all actions for the entity
                    
                    JSONArray actionArray = core.getEntityActions(e.id);
                    JSONObject json;
                    core.getLogger().log(Level.INFO, "Getting actions for {0} - {1} actions to get", new Object[]{e.name, actionArray.length()});
                    for (int j = 0; j < actionArray.length(); j++) {
                        json = actionArray.getJSONObject(j);
                        image = new ImageView(new Image(json.getString("icon"), 20, 20, true, true));
                        image.setEffect(blackout);
                        TreeItem<String> action = new TreeItem<> (json.getString("name"), image);
                        item.getChildren().add(action);                        
                    }
                    
                    rootItem.getChildren().add(item);
                    }catch (Exception eee){
                        eee.printStackTrace();
                    }
                });
                
                new Thread(() -> {
                    Platform.runLater(() -> { 
                        actionList.setRoot(rootItem);
                        actionList.setShowRoot(false);
                    });   
                }).start();
                
                
         }
    }

    /**
     * When a item is selected on the tree view it will bring up the settings
     * if the selected item is a action
     * @param selected 
     */
    private void entitySelect(TreeItem<String> selected) {
        
        associatedTree.setRoot(null);
        
        //This should prevent any null selections and entity roots from triggering
        if(selected == null || selected.getParent().getValue().equals("ROOT")){
           checkboxLoading(false);
           activateCB.setDisable(true);
           activateCB.setSelected(false);
           pinCB.setDisable(true);
           pinCB.setSelected(false);
           pullHistoryCB.setDisable(true);
           pullHistoryCB.setSelected(false);
           replaceHistoryCB.setDisable(true);
           replaceHistoryCB.setSelected(false);
           showCheckCB.setDisable(true); 
           showCheckCB.setSelected(false);
           this.selectedAction = null;
           return;
        }
        
        this.selectedAction = selected;        
        ActionObject object = actions.get(selected.getValue());
        
        //if null is returned the ActionObject has not been created, elsewise we
        //need to check what was returned
        if(object == null){
           checkboxLoading(false);
           activateCB.setDisable(false);
           activateCB.setSelected(false);
           pinCB.setDisable(true);
           pinCB.setSelected(false);
           pullHistoryCB.setDisable(true);
           pullHistoryCB.setSelected(false);
           replaceHistoryCB.setDisable(true);
           replaceHistoryCB.setSelected(false);
           showCheckCB.setDisable(true); 
           showCheckCB.setSelected(false);
        }else{
            checkboxLoading(object.isLoading());
            
            //Show it is active
            activateCB.setDisable(false);
            activateCB.setSelected(true);
            
            //If active, these will always be enabled
            pinCB.setDisable(false);
            pullHistoryCB.setDisable(false);
            
            //Get pin
            pinCB.setDisable(!core.isRequireCode());
            pinCB.setSelected(object.getRequirePin().getValue());
            
            //Check if we are pulling history, if not the table replacements are
            //disabled.
            if(object.getPullHistory().getValue()){
                pullHistoryCB.setSelected(true);
                
                //Setup checkout table box
                if(object.isReturnable()){
                    showCheckCB.setDisable(!object.isReturnable()); 
                    showCheckCB.setSelected(object.getShowCheckOut().getValue());
                }
                
                //Set up history table box
                replaceHistoryCB.setDisable(false);
                replaceHistoryCB.setSelected(object.getShowHistory().getValue());
                
            }else{
               pullHistoryCB.setSelected(false); 
               //Setup checkout table box
               showCheckCB.setDisable(true); 
               showCheckCB.setSelected(false);
               
               //Set up history table box
               replaceHistoryCB.setDisable(true);
               replaceHistoryCB.setSelected(false);
            }
            
            loadAssociatedEntities(object);
        }
    }
    
    /**
     * Loads the treeView of associated entities + fields of the given Action Object.
     * @param object Selected ActionObject
     */
    private void loadAssociatedEntities(ActionObject object){
            if(object == null)
                return;
        
        //Create tree for associated entities
            TreeItem<String> rootItem = new TreeItem<> ("ROOT");
            ActionInfo act = object.getActInfo();
            
        //Loop through all entities
            object.getActInfo().associatedEntities.forEach((k,s) ->{
                try{
                EntityInfo info = core.getEntityByID(s);
                if(info != null){
                    //Loop through all fields of the current entity
                    TreeItem<String> entitieItem = new TreeItem<> (info.name);
                    Set<String> mappedFields = act.getEntityMappedFields(info.id);
                    info.fields.values().forEach((f) -> {
                        
                        CheckBox check = new CheckBox();
                        if(f.isEntityDefault){
                            check.setSelected(true);
                            check.setDisable(true);
                        }else{
                            check.setSelected(mappedFields.contains(f.key));
                            check.setOnAction((ActionEvent e)->{
                                if(check.isSelected()){
                                    boolean res = act.addMappedField(f.key, k, s, f.name, f.sourceEntityID != null);
                                    check.setSelected(res);
                                }else{
                                    act.removeMappedField(s, f.key);
                                }
                                
                                core.saveActionsActive();
                                core.getLogger().log(Level.INFO, "[{0}] was selected as an additional column for [{1}]", new Object[] {f.name, f.sourceEntityID});
                            });
                        }
                        entitieItem.getChildren().add(new TreeItem<> (f.name, check));
                    });

                    rootItem.getChildren().add(entitieItem);
                }
                }catch(java.lang.NoSuchFieldError e){
                    e.printStackTrace();
                }
            });
            
            associatedTree.setRoot(rootItem);  
            associatedTree.setShowRoot(false);
    }
    
    /**
     * Upon start creates all action objects that are listed in the core. Does 
     * nothing if the core is not valid.
     */
    private void loadActionObjects(){
        if(!core.isValid())
            return;
        
        actions = core.getSelectedActions(); 
        
        if(actions != null && !actions.isEmpty()){
             core.getLogger().info("Reloading ActionObjects.");
            //wait for each action to be created first
            ArrayList<String> toRemove = new ArrayList<>();

            synchronized(actions){
            actions.forEach((k, v) -> {  
                try{
                    v.pullActionInfo(core);

                    if(v.checkValid()){
                        ToggleButton  button = v.getButton();
                        button.setOnAction((ActionEvent e)->menuButtonClicked(e));
                        addButton(button);
                    }else{
                        toRemove.add(k);
                    }
                }catch(Exception e){
                  core.getLogger().log(Level.SEVERE, "Error loading ActionObject {0}, Exception: {1}", new Object[]{k, e.getMessage()});
                  toRemove.add(k);
                  e.printStackTrace();
                }

             });
            }

            toRemove.forEach((s) -> {
                actions.remove(s);
                core.getLogger().log(Level.WARNING, "Action \"[0]\" not valid. Removing from list.", s);
             });

            core.getLogger().info("All ActionObjects loaded.");
        }
        
    }
    
    /**
     * Interrupts all the threads in preparation of shutting down.
     */
    public void shutDown(){
        core.shutDown();
    }
    
    /**
     * This will create an action object for use of the given action ID and entity
     * ID. The creation is done in a task as the creation will involve many calls.
     * @param actionID
     * @param entityID 
     */
    private Thread createActionObject(String actionID, String entityID){
        if(!core.isValid())
            return null;
        
        Task<Void> task = new Task<Void>() {
             @Override
             protected Void call() throws Exception {
                core.getLogger().log(Level.INFO, "Creating ActionObject for Entity-{0} Action {1}...", new Object[]{entityID, actionID});
                try{  
                    ActionObject action = new ActionObject(actionID, entityID, core);
                    action.pullActionInfo(core);
                    core.addAction(action);
                    
                    ToggleButton button = action.getButton();
                    button.setOnAction((ActionEvent e)->menuButtonClicked(e));
                    addButton(button);

                    checkboxLoading(false);
                }catch(Exception e){
                   core.getLogger().log(Level.SEVERE, 
                           "Error creating ActionObject for Entity-{0} Action {1}, Exception: {2} \n {3}",
                           new Object[]{entityID, actionID, e.getMessage(), e.getStackTrace()});
                   e.printStackTrace();
                   return null;
                }
                core.getLogger().log(Level.INFO, "Success creating ActionObject for Entity-{0} Action {1}.", 
                        new Object[]{entityID, actionID});
                return null;
            }//End call
        };//End Task  

             Thread th = new Thread(task);
                 th.setName("createActionObject-" + actionID);
                 th.setDaemon(true);
                 th.start();
                 
        return th;
    }
    
    /**
     * Adds the given button to the menu in alphabetical order. The settings menu
     * will be kept at the bottom of the list.
     * @param button 
     */
    private Thread addButton(ToggleButton button){
       Thread th = new Thread(() -> {
            Platform.runLater(() -> { 
            synchronized(menuBox){
                String name = button.getText();
                ObservableList<Node> list = menuBox.getChildren();
                int place = 0;

                //Finds new index location
                for(int i = 0; i < list.size(); i++){
                    if(((ToggleButton) list.get(i)).getText().compareToIgnoreCase(name) > 0){
                        place = i;
                        break;
                    }
                }
                
                //Removes if there are any extra buttons of the same name
                for(int i = 0; i < list.size(); i++){
                    if(((ToggleButton) list.get(i)).getText().equals(button.getText())){
                        list.remove(i);
                    }
                }
                //Add the button at the found place
                menuBox.getChildren().add(place, button);
                
                //remove and readd the settings button at the end of the list
                try{
                    menuBox.getChildren().remove(settingsButton);
                    menuBox.getChildren().add(settingsButton);
                }catch(NullPointerException e){
                    
                }
            }
            });   
        });
       th.start(); 
       
       return th;
    }
    
    private void removeButton(String text){
        ObservableList<Node> list = menuBox.getChildren();
        for(Node n : list){
            if(((ToggleButton) n).getText().equals(text)){
                new Thread(() -> {
                    Platform.runLater(() -> { 
                        menuBox.getChildren().remove(n);
                        actions.remove( ((ActionObject) n.getUserData()).getActionName());
                    });   
                }).start();
            }
        }
    }
    
    private void checkboxLoading(boolean b){
        new Thread(() -> {
            Platform.runLater(() -> { 
                checkboxCover.setVisible(b);
                if(!b)
                    return;
                
                 ActionObject object = actions.get(selectedAction.getValue());
            
                if(object != null){
                    //Show it is active
                    activateCB.setDisable(false);
                    activateCB.setSelected(true);

                    //If active, these will always be enabled
                    pinCB.setDisable(false);
                    pullHistoryCB.setDisable(false);

                    //Get pin
                    pinCB.setSelected(object.getRequirePin().getValue());

                    //Check if we are pulling history, if not the table replacements are
                    //disabled.
                    if(object.getPullHistory().getValue()){
                        pullHistoryCB.setSelected(true);

                        //Setup checkout table box
                        if(object.isReturnable()){
                            showCheckCB.setDisable(!object.isReturnable()); 
                            showCheckCB.setSelected(object.getShowCheckOut().getValue());
                        }

                        //Set up history table box
                        replaceHistoryCB.setDisable(false);
                        replaceHistoryCB.setSelected(object.getShowHistory().getValue());
                    }else{
                       pullHistoryCB.setSelected(false); 
                       //Setup checkout table box
                       showCheckCB.setDisable(true); 
                       showCheckCB.setSelected(false);

                       //Set up history table box
                       replaceHistoryCB.setDisable(true);
                       replaceHistoryCB.setSelected(false);
                    }
                }
            });   
        }).start();
    }

    /**
     * Called when an ActionObject is done loading and the settings can be 
     * accessed again.
     * @param object 
     */
    public void doneWaiting(ActionObject object) {
        //FOR TESTING TODO: Remove
        core.cleanHTTPMaps();
        
        if(selectedAction == null)
            return;
        
        ActionObject obj = actions.get(selectedAction.getValue());
       if(obj != null && obj.equals(object)) 
           checkboxLoading(false);
    }

    public void setStage(Stage stage) {
        primaryStage = stage;
        core.setStage(primaryStage);
    }
    
    
    
    /**
     * Registers any event filters that are needed and anything else that must
     * occur after the scene is shown. This needs to be called after 
     * initalization and thus should be called by main.
     * 
     * Current Filters:
     * F11 -> full screen
     * F1  -> history screen
     * Esc -> ActionObject focus
     * 
     */
    public void afterInit(){
        core.getScale().refresh();
        menuScaleSlider.setValue(core.getScale().getLeftMenuWidth().getValue());
        
        scene = primaryStage.getScene();
        scene.addEventFilter(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            if(event.getCode() == KeyCode.F11)
              fullScreen(null);
            
        });
        
        scene.addEventFilter(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            if(event.getCode() == KeyCode.ESCAPE){
                if(selectedActionObject != null)
                    selectedActionObject.focus();
                
                showPin(false);
            }
        });
        
        scene.addEventFilter(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            if(event.getCode() == KeyCode.F1){
                if(selectedActionObject != null)
                    selectedActionObject.toggleHistory();
            }
        });
        
        new Thread(() -> {
            Platform.runLater(() -> {
                if(primaryStage != null)
                    primaryStage.setFullScreen(false);
                else
                    System.out.println("Oh no it be null son");
            });   
        }).start();
        
        
    }
    
    /**
     * Removes all menu buttons except the settings button and then goes to the 
     * settings page.
     */
    private void clearMenu(){
        menuBox.getChildren().clear();
        menuBox.getChildren().add(settingsButton);
        settingsButtonClick(null);
    }

    /**
     * Sets up the controls needed to set and enter a pin. Restricts both 
     * PasswordFields to be numbers only, changes the buttons to have the correct
     * icons, and disables the "remove" pin button if no pin is set.
     */
    private void preparePin() {
        //Settings menu
        removePinButton.setDisable(!core.isRequireCode());

        setPinField.textProperty().addListener(
            //Restrict input to only numbers, max length 9
            (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
               String s = newValue;
                s = s.replaceAll("[^.\\d]", "");
                
                if(s.length() > 9)
                    s = s.substring(0, 8);

                setPinField.setText(s);                                 
        });
        
        //Pin Pane
        pinEnter.setText(AwesomeIcons.OK_SIGN);
        pinExit.setText(AwesomeIcons.REMOVE_SIGN);
        pinErase.setText(AwesomeIcons.ERASER);
        
        pinField.textProperty().addListener(
            //Restrict input to only numbers, max length 9
            (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
               String s = newValue;
                s = s.replaceAll("[^.\\d]", "");

                if(s.length() > 9)
                    s = s.substring(0, 8);
                
                pinField.setText(s);                                 
            });
    }
    
    /**
     * Sets the visibility of the pin pane.
     * @param b If true the pane will display, else it will be hidden.
     */
    private void showPin(boolean b){
        
        new Thread(() -> {
        Platform.runLater(() -> { 
            pinPane.setVisible(b);
        
            if(b){
                pinField.requestFocus();
            }
        });
        }).start();
        
    }

    private void getPin(ActionObject ao, ToggleButton toggle) {
        componentAlert(pinField, null);
        if(pinThread != null && pinThread.isAlive())
                pinThread.interrupt();
        
        pinThread = new PinThread(core, controller);
        pinThread.updateObjects(ao, toggle);
        pinThread.setDaemon(true);
        pinThread.start();
        
        showPin(true);
    }
    
    /**
     * Takes the test from setPinField and submits it for pin approval. If successful
     * the settings page is reloaded so the user must enter their pin, else wise
     * alerts the user.
     * @param e 
     */
    @FXML
    private void setPinButton(ActionEvent e) {
        componentAlert(setPinField, null);
        
        String input = setPinField.getText();
        
        if(input.length() < 4){
            componentAlert(setPinField, "Pin must be at least 4 digits.");
            setPinField.clear();
            return;
        }
        
        if(!input.equals(input.replaceAll("[^.\\d]", ""))){
            componentAlert(setPinField, "Pin can only be digits.");
            setPinField.clear();
            return;
        }
        
        if(core.setCode(Integer.parseInt(input))){
            removePinButton.setDisable(false);
        }else{
            componentAlert(setPinField, "Error changing pin.");
        }
        
        setPinField.clear();
    }
    
    @FXML
    private void removePinButton(ActionEvent e) {
        componentAlert(setPinField, null);
        
       if(setPinField.getText().isEmpty()){
           componentAlert(emailTF, "Error changing pin.");
           setPinField.clear();
           return;
       }
       
       String input = setPinField.getText();
       
       if(!input.equals(input.replaceAll("[^.\\d]", ""))){
            componentAlert(setPinField, "Pin can only be digits.");
            setPinField.clear();
            return;
        }
       
       if(core.checkCode(Integer.parseInt(input))){
           core.setCode(0);
           removePinButton.setDisable(true);
           setPinField.clear();
       }else{
          componentAlert(setPinField, "Pin is inccorect, please enter current pin to remove."); 
       }
       
    }

    public void checkPin(ActionObject ao, ToggleButton toggle) {
        if(ao != null && toggle != null){
            if(pinField.getText().isEmpty() || !core.checkCode(Integer.parseInt(pinField.getText()))){
                core.getLogger().log(Level.WARNING, "Incorrect code entered for Action Object [0]", ao.getActionName());
                componentAlert(pinField, "Code is incorrect.");
                pinField.clear();
            }else{
                core.getLogger().log(Level.INFO, "Correct code entered, showing Action Object [0]", ao.getActionName());

                pinField.clear();
                showPin(false);
                displayActionObject(ao, toggle);
                pinThread.interrupt();
            }
        }else {
            if(pinField.getText().isEmpty() || !core.checkCode(Integer.parseInt(pinField.getText()))){
                core.getLogger().log(Level.WARNING, "Incorrect code entered for Settings.");   
                componentAlert(pinField, "Code is incorrect.");
                pinField.clear();
            }else{
                core.getLogger().log(Level.INFO, "Correct code entered, showing Settings.");

                pinField.clear();
                showPin(false);
                showSettings();
                pinThread.interrupt();
            }
        }
    }
}
