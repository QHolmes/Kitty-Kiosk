/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructures.actions;

import core.ActionObject;
import core.Core;
import java.util.logging.Level;
import javafx.util.Pair;

/**
 *
 * @author dholmes
 */
public class ActionTransaction extends Thread {
    private final ActionObject actionObject;
    private final ActionPane actionPane;
    private final  Core core;
    private String entityObjectID;
    private final String actionID;
    private final String entityID;
    private final String actionName;
    private final String textField;
    private Thread thread;
    private Pair<String, Integer> actionResponce = null;
    private static ActionTransaction transaction;
    private boolean success = false;
    private boolean interupted = false;
    private ActionObjectEntite actionObjectEntity;

    public ActionTransaction(ActionObject actionObject, String actionID, String entityID, String actionName, String textField, ActionPane actionPane, Core core) {
        this.actionObject = actionObject;
        this.actionID = actionID;
        this.actionName = actionName;
        this.entityID = entityID;
        this.textField = textField;
        this.actionPane = actionPane;
        this.core = core;
        
        //ensure only one transaction is runing at a time
        if(transaction != null && !transaction.isAlive())
            transaction.interrupt();
        
        transaction = this;
        entityObjectID = null;
        actionObjectEntity = null;
    }
    
    /**
     * Constructor used if you already have the entity ID.
     * @param actionObject
     * @param actionID
     * @param entityID
     * @param actionName
     * @param textField
     * @param actionPane
     * @param core 
     */
    public ActionTransaction(ActionObject actionObject, String actionID, String entityID, String actionName, String textField, ActionObjectEntite actionObjectEntity, ActionPane actionPane, Core core) {
        this(actionObject, actionID, entityID, actionName, textField, actionPane, core);
        this.entityObjectID = actionObjectEntity.entityObjectID;
        this.actionObjectEntity = actionObjectEntity;
    }
    
    @Override
    public void run(){
        core.getLogger().log(Level.INFO, "Action transaction begain for action ID-[{0}] action Name-[{1}]; given scan [{2}]", 
                    new Object[]{actionID, actionName, textField});
        
        actionObject.updateLoading("Searching for Asset \"" + textField + "\"", "Attempt 1/3");
        actionObject.loadingAsset(true);
        
        for(int i = 1; i <= 3 && entityObjectID == null; i++){
            try{
                entityObjectID = getObjectID();

                //Check for interution
                if(checkInterupt()){
                    close();
                    return;
                }
                
                //If this is hit, it must have found something
                break;
            }catch(InterruptedException ex){
                if(checkInterupt()){
                    close();
                    return;
                }
                
                if( i < 3)
                    actionObject.updateLoading("Searching for Asset \"" + textField + "\"", "Attempt " + (i+1) + "/3");
                else
                    actionObject.updateLoading("Searching for Asset \"" + textField + "\"", "Timeout :(");
            }
        }
        
        //Check for interution
        if(checkInterupt()){
            close();
            return;
        }
        
        //if null, it could not connect
        if(entityObjectID == null){
            showMessage("Search timeout, please try again.");
            core.getLogger().log(Level.INFO, "Action transaction timeout searching for action ID-[{0}] action Name-[{1}] with the given scan [{2}]", 
                    new Object[]{actionID, actionName, textField});
            close();
            return;
        }
        
        //Check for interution
            if(checkInterupt()) return;
        
        //if no entity is found, return
        if(entityObjectID.isEmpty()){
            if(checkInterupt()){
                close();
                return;
            }
            showMessage("No item found for the given barcode.");
                core.getLogger().log(Level.INFO, "No object found for action ID-[{0}] action Name-[{1}] with the given scan [{2}]", 
                        new Object[]{actionID, actionName, textField});
                close();
                return;
        }
        
        //Check for interution
            if(checkInterupt()){
                close();
                return;
            }
        
        core.getLogger().log(Level.INFO, "Object found for action ID-[{0}] action Name-[{1}] with the given scan [{2}]",
                        new Object[]{actionID, actionName, textField, entityObjectID});
        
        actionObject.updateLoading("Object found: \"" + textField + "\"", "Loading object information...");
        
        //Open the action window
        try{
            actionPane.openActionPane(textField, entityObjectID, actionObjectEntity);
        }catch(Exception ex){
            close();
            ex.printStackTrace();
            core.getLogger().log(Level.INFO, "Error opening Actionpane for action ID-[{0}] action Name-[{1}]: Exception - ",
                        new Object[]{actionID, actionName, ex.getMessage()});
            return;
        }
        
        //Wait for the action pane to be closed
        thread = actionPane.joinActionPane();
        
        //Check for interution, if so close the ActionPane
            if(checkInterupt()){
                close();
                return;
            }
            
        if(thread != null){
            try {
                thread.join();
                
                //Get info
                actionResponce = actionPane.getActionResponce();
                
                //Check if interupt
                if(checkInterupt()){
                    close();
                    return;
                }
                
                if(actionResponce != null){
                    switch(actionPane.getCode()){
                        case SUCCESS:
                            success = true;
                            break;
                        case CANCELED:
                            break;
                        case NOTINFILTER:
                            showMessage("This device is not in the action's filter.");
                            break;
                        case INVAILDTOKEN:
                            showMessage("Token is invalid, please log out and log back in.");
                            break;
                        case RECORDNOTFOUND:   
                            showMessage("No item found for the given barcode."); //Should not reach
                           break;
                       default:
                           showMessage("An unknown error occured");
                    }  
                }
            } catch (InterruptedException ex) {
                //If interupted, close the action
                core.getLogger().log(Level.INFO, "Action checkout canceled: action ID-{0} action Name-{1}",
                        new Object[]{actionID, actionName, textField, entityObjectID});
                actionPane.closeActionPane();
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
        
        close();
        
    }
    
    /**
     * Tries to get the entityObjectID by searching for the string given in the text field.
     * If multiple entities or no entity is found, it will return an empty string. 
     * @return EntityObjectID, if found, else it returns an empty sting. 
     * This should never return null
     */
    private String getObjectID() throws InterruptedException{
        //Check Asset Panda for the given text, it will have 3 seconds to respond.
        entityObjectID = null;
        thread = new Thread(() -> {
            entityObjectID = core.getEntityObjectID(entityID, textField);
        });

       thread.start();
       thread.join(30000); 
       
       return entityObjectID;
    } 
    
    private void showMessage(String message){
        actionObject.popUp(message);
    }
    
    private void close(){
        if(thread != null && thread.isAlive())
            thread.interrupt();
        
        actionObject.loadingAsset(false);
        
        if(success)
            actionObject.checkMark();
        
        actionObject.clearCheckoutField();
    }
    
    private boolean checkInterupt(){
        if(Thread.currentThread().isInterrupted()){
            interupted = true;
             core.getLogger().log(Level.INFO, "Action checkout canceled: action ID-{0} action Name-{1}",
                        new Object[]{actionID, actionName, textField, entityObjectID});
            return true;
        }
        
        return interupted;
    }
    
}
