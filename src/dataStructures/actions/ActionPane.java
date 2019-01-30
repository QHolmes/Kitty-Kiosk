/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructures.actions;

import core.ActionCenter;
import core.ActionObject;
import core.Core;
import helperClasses.ResponseCodes;
import helperClasses.CheckoutStatus;
import http.Actions;
import java.io.IOException;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Pair;
import org.json.JSONObject;

/**
 *
 * @author dholmes
 */
public class ActionPane {
    private ActionCenter center;
    private final ActionObject action;
    private final Core core;
    private final ActionInfo actInfo;
    
    private final Object waiting = new Object();
    
    private String entityObjectID;
    
    private Button submitButton;
    private Button editButton;
    private SimpleStringProperty assetCodeString;
    private ScrollPane actionFieldScrollPane;
    private HBox actionArea;
    
    private Thread submitThread;
    private Thread joinThread;
    private Thread editThread;
    private ResponseCodes code;
    private Pair<String, Integer> actionResponce;
    private ActionObjectEntite actionObjectEntity;
    private boolean isEdit = false;
    private HBox loadingActionScreen;
    
    private GridPane box;

    public ActionPane(ActionCenter center, ActionObject action, Core core) {
        this.center = center;
        this.action = action;
        this.core = core;
        this.actInfo = action.getActInfo();
        
        buildPane();
    }

    public void setCenter(ActionCenter center) {
        this.center = center;
    }
    
    private void buildPane(){
        //---------------------------------
        //----------Action Pane------------
        //---------------------------------
            assetCodeString = new SimpleStringProperty("NULL");
            
        //Add action name
            Label actName = new Label(actInfo.actionName);
            actName.getStyleClass().add("actionTitle");

            AnchorPane.setLeftAnchor(actName, 14.0);
            AnchorPane.setRightAnchor(actName, 14.0);
            AnchorPane.setTopAnchor(actName, 24.0);
        
        //add action subtitle
            Label assetCodeLb = new Label();
            assetCodeLb.textProperty().bind(assetCodeString);
            assetCodeLb.getStyleClass().add("actionSubtitle");

            AnchorPane.setLeftAnchor(assetCodeLb, 14.0);
            AnchorPane.setRightAnchor(assetCodeLb, 14.0);
            AnchorPane.setTopAnchor(assetCodeLb, 90.0);

        //Add action field Scroll Pane
            actionFieldScrollPane = new ScrollPane();
            actionFieldScrollPane.getStyleClass().add("actionPane");

            AnchorPane.setLeftAnchor(actionFieldScrollPane, 35.0);
            AnchorPane.setRightAnchor(actionFieldScrollPane, 35.0);
            AnchorPane.setTopAnchor(actionFieldScrollPane, 144.0);
            AnchorPane.setBottomAnchor(actionFieldScrollPane, 64.0);
            
        //Add Cancel button
            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().add("checkButton");
            cancelButton.setOnAction(e-> {
                code = ResponseCodes.CANCELED;
                closeActionPane();
            });

            AnchorPane.setLeftAnchor(cancelButton, 14.0);
            AnchorPane.setBottomAnchor(cancelButton, 14.0);
        
        //Add submit and edit button
            submitButton = new Button("Submit");
            submitButton.getStyleClass().add("checkButton");
            submitButton.setOnAction(e-> {
                    submit();
            });
            
            editButton = new Button("Edit");
            editButton.getStyleClass().add("checkButton");
            editButton.setOnAction(e-> {
                editButton();
                
            });

            GridPane grid = new GridPane();
              grid.setHgap(10);
              //grid.setPadding(new Insets(10, 10, 10, 10));
              grid.add(editButton, 0, 0);
              grid.add(submitButton, 1, 0);
              
            AnchorPane.setRightAnchor(grid, 14.0);
            AnchorPane.setBottomAnchor(grid, 14.0);

            
         //loading cover
         
            ProgressIndicator assetLoading = new ProgressIndicator();
            assetLoading.setProgress(-1);
            assetLoading.setPrefWidth(200);
         
            loadingActionScreen = new HBox(assetLoading);
                loadingActionScreen.setFillHeight(true);
                loadingActionScreen.setStyle("-fx-background-color: rgba(0, 0, 0, .4); -fx-alignment: center;");
                loadingActionScreen.setMaxHeight(700);
                loadingActionScreen.setMinHeight(700);
                loadingActionScreen.setVisible(false);
            

            AnchorPane actionAnchor = new AnchorPane(grid, cancelButton, actionFieldScrollPane, actName, assetCodeLb, loadingActionScreen);
                actionAnchor.setStyle("-fx-background-color: #f4f4f4");
                actionAnchor.setMaxWidth(900);
                actionAnchor.setMinWidth(600);
                actionAnchor.setMaxHeight(700);
                actionAnchor.setMinHeight(700);
                
            loadingActionScreen.prefWidthProperty().bind(actionAnchor.widthProperty());
            
            actionArea = new HBox(actionAnchor); 
            actionArea.setStyle("-fx-alignment: center;");
                    // + "-fx-background-color: rgba(0, 0, 0, .4);");
            
            AnchorPane.setLeftAnchor(actionArea, 0.0);
            AnchorPane.setRightAnchor(actionArea, 0.0);
            AnchorPane.setTopAnchor(actionArea, 0.0);
            AnchorPane.setBottomAnchor(actionArea, 0.0);
            actionArea.setVisible(false);
    }
    
    public HBox getActionArea(){
        return actionArea;
    }
    
    /**
     * Closes the action pane and notifies anyone who has joined.
     */
    public void closeActionPane(){
        if(!actionArea.isVisible())
            return;
        
        Platform.runLater(() -> {
            actionArea.setVisible(false);
            submitButton.setDisable(false);
            center.clearFields();
            
            synchronized(waiting){
                waiting.notifyAll();
            }
            if(submitThread != null && submitThread.isAlive())
                submitThread.interrupt();
            
            if(editThread != null && editThread.isAlive())
                editThread.interrupt();
        });
    }
    
    /**
     * Open the action pane for the given entity object ID. This method
     * will determine if the object needs to be returned or not and show the correct
     * grid from the action center. This method will do nothing if the action pane
     * is already visible.
     * @param subText text under the action name, normally is the scanned info
     * @param entityObjectID the entity object ID of the object that the action should be applied too.
     */
    public void openActionPane(String subText, String entityObjectID, ActionObjectEntite actionEntity){
        actionObjectEntity = actionEntity;
        if(actionArea.isVisible())
            closeActionPane();
        
        this.entityObjectID = entityObjectID;
        
        paneLoading(false);
        Platform.runLater(() -> {
            center.clearFields();
            assetCodeString.set(subText);
            editButton.setText("Edit");
            submitButton.setText("Submit");
        });
        
        isEdit = false;
        
        //Check if the object can be edited/ returned
        if(actInfo.isReturnable && actionObjectEntity == null)
            try{
                actionObjectEntity = isSubmitedAction(entityObjectID);
            }catch (IOException ex){
                ex.printStackTrace();
            }
        
        //Select the correct grid, and set it in the actionField
        GridPane box;
        if(actionObjectEntity != null){
            if(actionObjectEntity.isReturned())
                box = center.getActionGrid(entityObjectID, actionObjectEntity, CheckoutStatus.EDITRETURNED);
            else
                box = center.getActionGrid(entityObjectID, null, CheckoutStatus.RETURN);
        } else
            box = center.getActionGrid(entityObjectID, null, CheckoutStatus.NEW);

        Platform.runLater(() -> {
            actionFieldScrollPane.setContent(box);
            actionArea.setVisible(true);
            
            if(actionObjectEntity == null)
                editButton.setDisable(true);
            else if(actionObjectEntity.isReturned()){
                editButton.setDisable(true);
                editButton.setText("Back");
                submitButton.setText("Update");
                isEdit = true;
            }else 
                editButton.setDisable(false);
        });
    }
    
    /**
     * Will return a thread that will stop once the action pane is closed. Will 
     * return null if the action pane isn't open.
     * @return Thread to join, that will stop after actionPane is closed
     */
    public Thread joinActionPane(){
        if(joinThread == null || !joinThread.isAlive()){
            Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                synchronized(waiting){
                    try {
                        waiting.wait();
                    } catch (InterruptedException ex) {
                        closeActionPane();
                    }
                }
                return null;
            }};

            joinThread = new Thread(task);
                joinThread.setDaemon(true);
                joinThread.setName("waiting for action-" + actInfo);
                joinThread.start();
        }

        return joinThread;
    }
    
    /**
     * Submits the actions with the given field. Once returned, code will
     * be updated with the response code of the action.
     */
    public void submit(){
        if(submitThread != null && submitThread.isAlive())
            return;
        
        submitThread = new Thread() {
            @Override
            public void run(){
                
                try{
                    paneLoading(true);

                Thread th = new Thread(){
                    @Override
                    public void run(){
                        if(!isEdit)
                            actionResponce = center.submitAction(entityObjectID, actInfo.actionID);
                        else if(actionObjectEntity != null)
                            actionResponce = center.updateAction(entityObjectID, actInfo.actionID, actionObjectEntity);

                        else{
                            code = ResponseCodes.UNKNOWN;
                            closeActionPane();
                        }
                    }
                };

                th.setDaemon(true);
                try{
                    th.start();
                    th.join();
                }catch(InterruptedException inter){
                    return;
                }


                if(Thread.currentThread().isInterrupted())
                    return;

                //Check the responce code for any errors
                if(actionResponce != null){
                    switch(actionResponce.getValue()){
                        case(200):
                            code = ResponseCodes.SUCCESS;
                            break;
                        case(406):
                            //406: Object is not in action filter
                            code = ResponseCodes.NOTINFILTER;
                            break;
                        case(401):
                            //401: Token is incorrect
                            code = ResponseCodes.INVAILDTOKEN; //if(!controller.validSetup()) return null;
                            break;
                        case(422):
                           //422: Record could not be found
                           //Check if the action still exists, if not terminate.
                           code = ResponseCodes.RECORDNOTFOUND; //if(!action.checkValid())                                       
                           break;
                        case(0):
                       default:
                           code = ResponseCodes.UNKNOWN;
                    } 
                    
                    closeActionPane(); 
                }else
                
                paneLoading(false);
                    
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }};
        
            submitThread.setDaemon(true);
            submitThread.setName("Submitting-" + actInfo);
            submitThread.setPriority(Thread.MAX_PRIORITY);
            submitThread.start();
    }
    
    /**
     * Checks if the given entityObjectID needs to be returned for this ActionObject.
     * @param entityObjectID of the entity to check
     * @return if the entity needs to be returned
     */
    public ActionObjectEntite isSubmitedAction(String entityObjectID) throws IOException{
        if(center.isReturnable()){
            try {
                Pair<Boolean, String> pair = Actions.checkActionReturn(core, entityObjectID, actInfo.actionID);
                
                if(pair.getKey()){
                    Pair<String, Integer> pair2 = Actions.fetchActionObject(core, entityObjectID, pair.getValue());
                    if(pair2.getValue() == 200)
                        return new ActionObjectEntite(actInfo, entityObjectID, new JSONObject(pair2.getKey()), core);
                }
            } catch (IOException ex) {
                core.getLogger().log(Level.WARNING, "Could not determin if {0} needs to be returned: Action Name {1}", new Object[]{entityObjectID, actInfo.actionName});
                throw ex;
            }
        }
        
        return null;
        
    }

    /**
     * Returns the Response code that was returned from the action. After use, code
     * resets to null.
     * @return response from action object
     */
    public ResponseCodes getCode() {
        ResponseCodes rtCode = code;
        code = null;
        return rtCode;
    }

    /**
     * Returns the pair that was returned from the action. After use, pair
     * resets to null.
     * @return information returned from the GET call
     */
    public Pair<String, Integer> getActionResponce() {
        Pair<String, Integer> rtPair = actionResponce;
        actionResponce = null;
        return rtPair;
    }

    private void editButton() {
        if(editThread != null && editThread.isAlive())
            return;
        
        editThread = new Thread() {
             
            @Override
            public void run(){
                if(!isEdit){
                    if(actionObjectEntity == null)
                        return;

                    paneLoading(true);
                    Thread th = new Thread(){
                        public void run(){
                            if(actionObjectEntity.isReturned())
                                box = center.getActionGrid(entityObjectID, 
                                        actionObjectEntity, CheckoutStatus.EDITRETURNED);
                            else
                                box = center.getActionGrid(entityObjectID, 
                                        actionObjectEntity,CheckoutStatus.EDIT);

                            isEdit = true;
                        }
                    };
                    
                    th.setDaemon(true);
                    try {
                        th.start();
                        th.join();
                    } catch (InterruptedException ex) {
                        return;
                    }

                    paneLoading(false);
                    Platform.runLater(() -> {
                        actionFieldScrollPane.setContent(box);
                        editButton.setText("Back");
                        submitButton.setText("Update");
                    });
                }else{
                    paneLoading(true);

                    GridPane box = center.getActionGrid(entityObjectID, null, CheckoutStatus.RETURN);

                    isEdit = false;
                    
                    paneLoading(false);
                    Platform.runLater(() -> {
                        editButton.setText("Edit");
                        submitButton.setText("Submit");
                    });
                }
            }
        };
        
        editThread.setDaemon(true);
        editThread.setName("Switching edit mode -" + actInfo);
        editThread.setPriority(Thread.MAX_PRIORITY);
        editThread.start();
        
    }
    
    private void paneLoading(boolean b){
        Platform.runLater(() -> {
            submitButton.setDisable(b);
            loadingActionScreen.setVisible(b);
        });
    }
    
    
}
