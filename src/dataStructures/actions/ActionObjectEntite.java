/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructures.actions;

import core.Core;
import http.Actions;
import http.Entities;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;
import javafx.concurrent.Task;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author dholmes
 */
public class ActionObjectEntite {
    public final String actionObjectID;
    private final ActionInfo action;
    private ArrayList<String> info;
    private HashMap<String, Integer> keyField;
    public final String entityObjectID;
    public String entityObjectPrimary;
    private boolean returned;
    private boolean removed = false;

    public ActionObjectEntite(ActionInfo action, String entityObjectID, JSONObject actionObject, Core core) {
        this.action = action;
        this.entityObjectID = entityObjectID;
        actionObjectID = actionObject.getString("id");
        returned = actionObject.getBoolean("returned");
        
        getEntityObjectID(core);
        
        entityObjectPrimary = "";
        fillData(actionObject, core);
        
    }
    
    private void getEntityObjectID(Core core){
        Task task = new Task() {
            @Override
            protected Integer call() throws Exception {
                try{
                    Pair<String,Integer> reply = Entities.fetchEntityObject(core, entityObjectID);
                    JSONObject json = new JSONObject(reply.getKey());
                    entityObjectPrimary = json.getString("display_with_secondary");
                }catch(IOException e){
                    entityObjectPrimary = "";
                }
                
                return null;
            }//End Call
         };//End task                    

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start(); 
    }
    
    /**
     * Returns the data in actionColumnOrder (as defined by the given actionInfo)
     * @param index 0 to n-1 for column order. 
     * @return Data in the field, can be null.
     */
    public String getField(int index){
        return info.get(index);
    }
    
    /**
     * Returns the data of the corresponding key
     * @param key to find value of
     * @return Data in the field, can be null.
     */
    public String getKeyField(String key){
        return info.get(keyField.get(key));
    }
    
    /**
     * Returns the number of elements in the actionObject field
     * @return 
     */
    public int getFieldSize(){
        return info.size();
    }
    
    /**
     * Returns true if this Action Object Entity received a 422 error (Object
     * not found)
     * @return true if the object no longer exists
     */
    public boolean isRemoved(){
        return removed;
    }
    
    /**
     * Returns the actionID of the actionObject's action.
     * @return 
     */
    public String getActionID(){
        return action.actionID;
    }
    
    /**
     * Returns the entity ID of the entity associated with the actionObject's 
     * action.
     * @return 
     */
    public String getEntityID(){
        return action.entityID;
    }
    
    /**
     * Returns true if this actionObject has been returned/ does not need to be 
     * returned.
     * @return 
     */
    public boolean isReturned(){
        return returned;
    }
    
    /**
     * Forces actionObject to update itself
     * @param core
     * @return trues the response code of the update
     * @throws java.io.IOException
     */
    public int update(Core core) throws IOException{
        if(removed)
            return 422;
        
        int gate = 0;
        while(gate < 3){
            try{
                Pair<String,Integer> reply = Actions.fetchActionObject(core, entityObjectID, actionObjectID);
                
                if(reply.getValue() == 200){
                    JSONObject json = new JSONObject(reply.getKey());
                    fillData(json, core);
                    gate = 10;
                    return 200;
                }
                
                if(reply.getValue() == 422)
                    removed = true;
                
                if(reply.getValue() < 500)
                    return reply.getValue();
                
                break;
            }catch(JSONException e){
                core.getLogger().log(Level.WARNING, "JSONException - Failed to get return info for ActionObject-[{0}] ActionName-[{1}].", new Object[] {actionObjectID, action.actionName});
            }catch(NullPointerException ex){
                core.getLogger().log(Level.WARNING, "NullPointerException - Failed to get return info for ActionObject-[{0}] ActionName-[{1}].", new Object[] {actionObjectID, action.actionName});
            }
            gate++;
        }
        
        return 0;
    }
    
    /**
     * Used to see if any of the fields contains the given string. Case insensitive. 
     * This is normally called for history searches.
     * @param subText string to check if any fields contain
     * @return True if at least one field contains the subText
     */
    public boolean contains(String subText){
        String subTextLower = subText.toLowerCase();
        
        for(String s: info){
            if(s.toLowerCase().contains(subTextLower))
                return true;
        }
        
        return false;
        //return info.stream().anyMatch((s) -> (s.toLowerCase().contains(s)));
    }
    
    /**
     * Parses the given JSON file to get the field info. At the end the info
     * array will match the data in the given JSON along with added columns.
     * @param actionObject
     * @param core 
     */
    private void fillData(JSONObject actionObject, Core core){
        ArrayList<ActionColumnInfo> fieldOrder = action.getActionOrderColumns();
        info = new ArrayList<>(fieldOrder.size());
        keyField = new HashMap();
        ActionColumnInfo column;
        StringBuilder builder;
        String field;
        
        //get vars
        returned = actionObject.getBoolean("returned");
        
        //get field values
        for(int i = 0; i < fieldOrder.size(); i++){
            column = fieldOrder.get(i);
            
            try{
                if(column.isReference){
                    String refID = actionObject.getJSONObject(column.associatedFieldKey).getString("id");
                    JSONObject refJSON = new JSONObject(Entities.getEntityObject(core, refID, true).getKey());

                    if(column.referenceJSON){
                        refJSON = new JSONObject(refJSON.getJSONObject(column.referenceFieldKey));
                        field = refJSON.getString("display_name");
                    }else{
                        if(refJSON.isNull(column.referenceFieldKey))
                            field = "";
                        else
                            field = refJSON.getString(column.referenceFieldKey);
                    }
                }else{
                    switch(column.fieldDataType){
                        case("Array"):
                            if(actionObject.isNull(column.key)){
                                field = "";
                                break;
                            }

                            JSONArray fieldArray = actionObject.getJSONArray(column.key);
                            builder = new StringBuilder();
                            
                            for(int k = 0; k < fieldArray.length() - 1; k++){
                                if(fieldArray.isNull(k))
                                            builder.append(", ");
                                else
                                    builder.append(fieldArray.getString(k)).append(", ");
                            }
                            builder.append(fieldArray.getString(fieldArray.length() - 1));
                            field  = builder.toString();
                            break;
                        case("String"):
                            if(actionObject.isNull(column.key))
                                field = "";
                            else
                                field = actionObject.getString(column.key);
                            break;
                        case("JSON"):
                            if(actionObject.isNull(column.key) || actionObject.getJSONObject(column.key).isNull("display_name"))
                                field = "";
                            else
                                field = actionObject.getJSONObject(column.key).getString("display_name");
                            break;
                        case("YesNoField"):
                            if(actionObject.isNull(column.key))
                                field = "";
                            else{
                                if(actionObject.getBoolean(column.key))
                                    field = "Yes";
                                else
                                    field = "No";
                            }
                            break;
                        case ("DisplayValueField"):
                            if(actionObject.isNull(column.key))
                                field = "";
                            else{           

                                JSONObject refJSON = new JSONObject(Entities.getEntityObject(core, entityObjectID, true).getKey());

                                try{
                                    if(refJSON.isNull(column.displayField))
                                        field =  "";
                                    else
                                        field = refJSON.getString(column.displayField);
                                }catch(JSONException e){
                                    if(refJSON.isNull(column.displayField))
                                        field =  "";
                                    else
                                        field = refJSON.getJSONObject(column.displayField).getString("display_name");
                                }
                            }
                        break;
                        default:
                            field = "";
                    }  
                }
            }catch(IOException | JSONException e){
                field = "";
                String logName = column.name;
                if(logName == null || logName.isEmpty())
                    logName = "Unknown";
                core.getLogger().log(Level.SEVERE, "Error getting data for column [{0}] {1}", 
                      new Object[]{logName, e.toString()});
            }
            
          info.add(i, field);
          keyField.put(column.key, i);
        }//end for
    
    }
    
    @Override
    public boolean equals(Object o){
        if( o instanceof ActionObjectEntite)
            return actionObjectID.compareTo(((ActionObjectEntite) o).actionObjectID) == 0;
        else
            return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + Objects.hashCode(this.actionObjectID);
        return hash;
    }
    
    
}
