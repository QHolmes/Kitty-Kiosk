/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package background;

import core.Core;
import dataStructures.actions.ActionObjectEntite;
import javafx.util.Pair;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import javafx.collections.ObservableList;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class will receive an obervable list, get the action entity ID from the first
 * column and check if it was returned. If so it will remove the action from the table.
 * @author Quinten Holmes
 */
public class ActionEntityReturnChecker extends Thread{
    
    private final ObservableList<ActionObjectEntite> list;
    private final Core core;
    private final String actionName;
    
    public ActionEntityReturnChecker(Core core, ObservableList list, String actionName){
        this.list = list;
        this.core = core;
        this.actionName = actionName;
    }
    
    @Override
    public void run(){
        core.getLogger().log(Level.INFO, "Checking for return table updates for ActionName-[{0}].", actionName);
        
        ArrayList<Integer> removeList = new ArrayList<>();
        Pair<String,Integer> reply;
        JSONObject json;
        int gate = 0;
        String objectID = "";
        for(int i =0; i < list.size(); i++){
            objectID = "";
            try{
                ActionObjectEntite temp = list.get(i);
                objectID = temp.actionObjectID;
                temp.update(core);
                
                if(temp.isReturned())
                    removeList.add(i);
                else if(temp.isRemoved()){
                    removeList.add(i);
                    core.getLogger().log(Level.INFO, "Action Object Entity [{0}] no longer exists, removing from tables --  ActionName-[{1}].",
                            new Object[] {temp.actionObjectID, actionName});
                }

            }catch(JSONException e){
                core.getLogger().log(Level.WARNING, "Failed to get return info for ActionObject-[{0}] ActionName-[{1}].", new Object[] {objectID, actionName});
            }catch(NullPointerException ex){
                //if null value, retry upto 3 times
                gate++;
                if(gate <= 3){
                    i--;
                    continue;
                }
            }catch (IOException io){
                core.getLogger().log(Level.WARNING, "IOException: Failed to get return info "
                        + "for ActionObject-[{0}] ActionName-[{1}] -- {2}.", new Object[] {objectID, actionName, io.toString()});
            }
            gate = 0;
        }
            
        ActionObjectEntite removed;
        int index;

        for(int i = removeList.size() - 1; i >= 0; i--){
            index = removeList.get(i);
            removed = list.remove(index);
            core.getLogger().log(Level.INFO, "ActionObject-[{0}] already returned for Action-[{1}], removing from return table.", new Object[]{removed.actionObjectID, actionName});
        }
    }
}
