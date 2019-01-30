/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructures.actions ;

import core.Core;
import javafx.util.Pair;
import http.Entities;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Quinten Holmes
 */
public class ActionInfo implements Serializable{
    private static final long serialVersionUID = 1;
   
    public String key;
    public String actionName;
    public String actionID;
    public String entityName;
    public String entityID;
    public String returnName;
    public String icon;
    public boolean gpsLocation;
    public boolean isReturnable;
    public boolean isAutoReturnable;
    public boolean changeReturnName;
    public boolean preformOnNotReturned;
    public String actionJSONString;
    public ArrayList<ActionColumnInfo> returnColumns;
    public ArrayList<String> returnOrder;
    public ArrayList<String> historyOrder;
    private ArrayList<ActionColumnInfo> actionOrderColumns;
    public HashMap<String, String> associatedEntities;
    public HashMap<String, Map<String, ActionColumnInfo>> mappedColumns; 
    private HashMap<String, ActionColumnInfo> allColumns;
    private HashMap<String, Set<String>> entityFieldsMapped;
    private transient Core core;
    
    public final String LOCK = "hello";
    private int associatedOrder = 100000;

    
    /**
     * Gets info for the given entity and action ID, and provides a central place to store the info. 
     * @param actionID
     * @param entityID
     * @param core
     * @throws IOException 
     */
    public ActionInfo(String actionID, String entityID, Core core) throws IOException{
        this.actionID = actionID;
        this.entityID = entityID;
        this.core = core;
        
        associatedEntities = new HashMap<>();
        mappedColumns = new HashMap<>();
        allColumns = new HashMap();
        entityFieldsMapped = new HashMap<>();
        returnColumns = new ArrayList<>();
        returnOrder = new ArrayList<>();
        historyOrder = new ArrayList<>();
        actionOrderColumns = new ArrayList<ActionColumnInfo>(){
            @Override
            public boolean add(ActionColumnInfo mt) {
                super.add(mt);
                Collections.sort(actionOrderColumns, new ColumnInfoComparator());
                return true;
            }
        }; 
        
        core.getLogger().log(Level.INFO, "Getting action info for action ID-[{0}] entity ID-[{1}].", new Object[]{actionID, entityID});
        Pair<String, Integer> getCall = Entities.getActions(core, entityID, true);
        
        //Find given action ID
        JSONArray array = new JSONArray(getCall.getKey());
        JSONObject action = null;

        for(int i = 0; i < array.length(); i++) {
                action = array.getJSONObject(i);
                if(action.getString("id").compareTo(actionID) == 0) 
                        break;
        }

        //double check we have the correct action
        if(action == null){
            throw new IOException("Responce null");
        }else if(action.getString("id").compareTo(actionID) != 0){
            throw new IOException("Responce does not equal actionID");
        }

        actionJSONString = action.toString();
        
        //Setup action information
            isReturnable = action.getBoolean("is_returnable");
            isAutoReturnable = action.getBoolean("is_auto_returnable");
            gpsLocation = action.getBoolean("track_gps");
            preformOnNotReturned = action.getBoolean("perform_on_not_returned");
            
            actionName = action.getString("name");
            returnName = action.getString("return_name");
            changeReturnName = action.getBoolean("change_return_name");
            icon = action.getString("icon");
            key = action.getString("key");
            
        updateColumns(core);
    }

    /**
     * Updates the order return column should appear in.
     * @param newColOrder Order of columns by action column name
     */
    public void newReturnOrder(ArrayList<String> newColOrder) {
        returnOrder.clear();
        returnOrder.addAll(newColOrder);
    }
    
    /**
     * Updates the order history columns should appear in.
     * @param newColOrder Order of columns by action column name
     */
    public void newHistoryOrder(ArrayList<String> newColOrder) {
        historyOrder.clear();
        historyOrder.addAll(newColOrder);
    }
    /**
     * Checks Asset Panda for columns that are part of this action. Duplicates are 
     * updated. 
     * @param core
     */
    public final void updateColumns(Core core){
        this.core = core;
        
        synchronized(LOCK){
            //Check neither map is null
            boolean first = false;
            
            if(actionOrderColumns.isEmpty())
                first = true;

            JSONObject action = new JSONObject(actionJSONString);
            JSONArray array = action.getJSONArray("fields");
            ActionColumnInfo row;
            associatedEntities.clear();

            //Updates or creates new, for each row
            for(int i = 0; i < array.length(); i++){
                action = array.getJSONObject(i);
                String id = action.getString("id");
                row = allColumns.get(id);


                //Update if the row exsists, else create new
                if(row != null){
                    row.updateRow(action);
                }else{
                    row = new ActionColumnInfo(action);
                    allColumns.put(row.id, row);

                    if(!row.isReturnField && isReturnable)
                        returnColumns.add(row);

                    actionOrderColumns.add(row);
                }

                //If the row has a source entity, add it to the map
                if(row.sourceEntityID != null && !row.sourceEntityID.equals("null"))
                    associatedEntities.put(row.key, row.sourceEntityID);
            }

            ArrayList<String> remove = new ArrayList<>();
            //Remove columns no longer associated 
            mappedColumns.forEach((s, e) -> {
                if(!associatedEntities.containsKey(s))
                    remove.add(s);
            });
            
            //remove.forEach(s -> mappedColumns.remove(s));
            
            if(first){
                
                actionOrderColumns.forEach( (v) -> {
                    historyOrder.add(v.name);
                    if(!v.isReturnField)
                        returnOrder.add(v.name);
                });
                
            }
            
            entityFieldsMapped.clear();
            
            //Ensure entityFieldsMapped is up to date
            mappedColumns.forEach((a, m) -> {
                m.forEach((r, col) -> {
                    if(!entityFieldsMapped.containsKey(col.sourceEntityID))
                        entityFieldsMapped.put(col.sourceEntityID, new HashSet<>());
                    
                    entityFieldsMapped.get(col.sourceEntityID).add(col.associatedFieldKey);
                    
                }) ;
            });
        }//End synchronized block
        
        core.getLogger().log(Level.INFO, "Action info updated for action [{0}].", new Object[]{actionName});    
    }
    
    /**
     * Tries to add the given entityID and filedKey combo to the mappedColumns list.
     * This will return false if the given entityID cannot be mapped, or if the
     * column is already mapped.
     * @param JSONField
     * @param referenceFieldKey The key of the entity field to add
     * @param sourceEntityID
     * @param name
     * @param associatedFieldKey The key of the action field the provides the link
     * @return true if the column could be added, else false.
     */
    public boolean addMappedField(String referenceFieldKey, String associatedFieldKey, String sourceEntityID, String name, boolean JSONField){
        synchronized(LOCK){
            //Check the enity can be associated
            if(!associatedEntities.containsKey(associatedFieldKey))
                return false;

            //Check if the associatedFieldKey HashMap exists, if not create
            if(!mappedColumns.containsKey(associatedFieldKey))
                mappedColumns.put(associatedFieldKey, new HashMap<>());

            //Check if the field is already mapped, if so return true
            if(mappedColumns.get(associatedFieldKey).containsKey(referenceFieldKey))
                return true;

            //Create the column it self
            ActionColumnInfo actCol = new ActionColumnInfo(referenceFieldKey, associatedFieldKey, sourceEntityID, name, associatedOrder++, JSONField);
              
            //Check if the entityFieldsMapped contains the soureEntiyID HashSet; if not, create
            if(!entityFieldsMapped.containsKey(sourceEntityID))
                        entityFieldsMapped.put(sourceEntityID, new HashSet<>());
                    
            //Add the column to the hashSet
            entityFieldsMapped.get(sourceEntityID).add(referenceFieldKey);
            
            //Log
            core.getLogger().log(Level.INFO, "Mapped filed [{0}] added to action [{1}].", new Object[]{actCol.name, actionName});
            
            //Add the column to the other Maps as needed
            mappedColumns.get(associatedFieldKey).put(referenceFieldKey, actCol);
            actionOrderColumns.add(actCol);
            
            historyOrder.add(actCol.name);
                if(!actCol.isReturnField)
                    returnOrder.add(actCol.name);
            return true;  
        }
    }

    /**
     * Removes the given column from the mappedColumns list
     * @param entityID
     * @param fieldKey
     * @return true if an item was removed, else returns false
     */
    public boolean removeMappedField(String entityID, String fieldKey) {
        synchronized(LOCK){
            if(!mappedColumns.containsKey(entityID))
                return false;

            if(!mappedColumns.get(entityID).containsKey(fieldKey))
                return false;

            ActionColumnInfo colInfo = mappedColumns.get(entityID).remove(fieldKey);
            if(colInfo != null){
                actionOrderColumns.remove(colInfo);
                returnColumns.remove(colInfo);
                returnOrder.remove(colInfo.name);
                historyOrder.remove(colInfo.name);
                actionOrderColumns.remove(colInfo);
                allColumns.remove(colInfo.id);
                
                if(colInfo.sourceEntityID != null)
                    entityFieldsMapped.remove(colInfo.sourceEntityID);
                
                if(colInfo.associatedFieldKey != null)
                    mappedColumns.remove(colInfo.associatedFieldKey); 
            }
            
            core.getLogger().log(Level.INFO, "Mapped filed [{0}] added to action [{1}].", new Object[]{colInfo.name, actionName});
            
            return true;
        }
    }
    
    /**
     * Gets the list of fields mapped from a given entity ID
     * @param entityID Entity to get mapped fields from
     * @return set of all mapped fields
     */
    public Set<String> getEntityMappedFields(String entityID){
        Set<String> set = new HashSet<>();
        
        if(entityFieldsMapped.containsKey(entityID)){
            synchronized(LOCK){
                set.addAll(entityFieldsMapped.get(entityID));
            }
        }
        
        return set;
    }
    
    /**
     * Extracts column info from the given JSONObject. This will include the
     * actionObjectEntity ID in the first row, the JSON string in the second.
     * @param json JSON Object to get row info from
     * @param entityObjectID The entity object ID of the action's object
     * @param forReturn
     * @param core Core for error report, and get new info
     * @return ArrayList of data given in actionOrderColumn order
     */
    public ArrayList<String> getRowInfo(JSONObject json, String entityObjectID, boolean forReturn, Core core){
        this.core = core;
        ArrayList<String> row = new ArrayList<>(actionOrderColumns.size() + 2);
        StringBuilder builder = new StringBuilder();
        row.add(actionID); //action ID
        row.add(entityObjectID); //Entity Object ID
        row.add(json.getString("id")); //ActionObject ID
        row.add(json.toString()); //full json
        forReturn = false;
        
        for(int j = 0; j < actionOrderColumns.size(); j++){
            if(actionOrderColumns.get(j).key == null && !actionOrderColumns.get(j).isReference)
                row.add("");
            else
                row.add(actionOrderColumns.get(j).getColumnData(json, core));
        }
        
        return row;
    }

    public ArrayList<ActionColumnInfo> getActionOrderColumns() {
        ArrayList<ActionColumnInfo> rtArray = new ArrayList<>(actionOrderColumns.size());
        rtArray.addAll(actionOrderColumns);
        return rtArray;
    }
    
    class ColumnInfoComparator implements Comparator<ActionColumnInfo> {
        @Override
        public int compare(ActionColumnInfo a, ActionColumnInfo b) {
            return a.order < b.order ? -1 : a.order == b.order ? 0 : 1;
        }
    }
    
}
