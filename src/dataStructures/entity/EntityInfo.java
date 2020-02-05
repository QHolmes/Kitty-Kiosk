/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructures.entity;

import core.Core;
import background.BuildEntityData;
import javafx.util.Pair;
import http.Entities;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;
import java.util.logging.Level;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Quinten Holmes
 */
public class EntityInfo implements Serializable{
    private static final long serialVersionUID = 2;
    
    public final String id;
    public String name; 
    public String key;
    public String icon;
    public String JSONString;
    public String defaultField;
    public String secondaryField;
    
    public String[] listField;
    public String[] allowedAttachments;
    
    public boolean trackGPS;
    public boolean requireCommentOnObjectDelete;
    public boolean showLinked;
    public boolean showAssociated;
    public boolean filterFieldsByStatus;
    
    public Map<String, EntityFieldInfo> fields;
    public List<String> fieldData;
    public Calendar lastDataUpdate = null;
    public int maxEntityDataAge = 3600;
    
    private final StampedLock dataLock = new StampedLock();
    
    public EntityInfo(String entityID, Core core) throws IOException{
        this.id = entityID;
        fields = new HashMap<>();
        fieldData = new ArrayList<>();
        update(core);
    }
    
    public EntityFieldInfo getEntityField(String fieldID){
        return fields.get(fieldID);
    }
    
    /**
     * Updates the information of the entity, then updates the info of the attached
     * entity fields.
     * @param core
     * @throws IOException 
     */
    public final void update(Core core) throws IOException{
        Pair<String, Integer> pair = Entities.getEntiteInfo(core, id);
        
        if(pair.getValue() != 200)
            throw new IOException();
        
        JSONString = pair.getKey();
        JSONObject json = new JSONObject(JSONString);
        
        //Get booleans
         trackGPS = json.getBoolean("track_gps");
         requireCommentOnObjectDelete = json.getBoolean("require_comment_on_object_delete");
         showLinked = json.getBoolean("show_linked");
         showAssociated = json.getBoolean("show_associated");
         filterFieldsByStatus = json.getBoolean("filter_fields_by_status");
         
         //Get Strings
         name = json.getString("name");
         key = json.getString("key");
         icon = json.getString("icon");
         
         //Get String array
         JSONArray array = json.getJSONArray("list_fields");
         listField = new String[array.length()];
         
         for(int i = 0; i < array.length(); i++)
             listField[i] = array.getString(i);
         
         array = json.getJSONArray("allowed_attachments");
         allowedAttachments = new String[array.length()];
         
         for(int i = 0; i < array.length(); i++)
             allowedAttachments[i] = array.getString(i);
         
         array = json.getJSONArray("fields");     
         EntityFieldInfo field;
         
         for(int i = 0; i < array.length(); i++){
             json = array.getJSONObject(i);
             field = fields.get(json.getString("id"));
             if(field != null)
                 field.updateField(json);
             else{
                 field = new EntityFieldInfo(json, this);
                 fields.put(field.id, field);
             }
             
             //check if the field is default or secondary, if so mark info
             if(field.isEntityDefault)
                 defaultField = field.key;
             else if(field.isSecondaryDefault)
                 secondaryField = field.key;
         }
        
    }
    
    /**
     * Returns the JSON of each entityObject in this entity
     * @param core
     * @return 
     */
    public List<String> getFieldData(Core core){
        updateDataField(core, false);
        
        //If data is old, update first
        long stamp = dataLock.readLock();
        try{
            return fieldData;
        }finally{
            dataLock.unlockRead(stamp);
        }
    }
    
    /**
     * Checks if the age of the data is past max age.
     * @return true if the data is old.
     */
    public boolean isDataOld(){
        if(lastDataUpdate == null)
            return false;
        
        Calendar maxAge = Calendar.getInstance();
        maxAge.add(Calendar.SECOND, -maxEntityDataAge);
        return lastDataUpdate.before(maxAge);
    }
    
    /**
     * Returns a Pair with the string array of ids (p) and display names (q). 
     * If secondary is true, the display name will have the secondary field appended
     * to the end of the display name in arrows. Reforms the arrays each time.
     * @param core
     * @param secondary True if the secondary should be shown with the display name
     * @return Pair<String[], String[]> of ids and displayNames respectively
     */
    public Pair<String[], String[]> getDisplay(Core core, boolean secondary){
        updateDataField(core, false);
        
        long stamp = dataLock.readLock();
        try{
           String[] data = new String[fieldData.size()];
           String[] ids = new String[fieldData.size()];
           
           JSONObject json;
           if(secondary){
                for (int i = 0; i < fieldData.size(); i++) {
                    json = new JSONObject(fieldData.get(i));
                    ids[i] = json.getString("id");
                    data[i] = json.getString("display_with_secondary");
                }
           }else{
               for (int i = 0; i < fieldData.size(); i++) {
                    json = new JSONObject(fieldData.get(i));
                    ids[i] = json.getString("id");
                    data[i] = json.getString("display_name");
                }
           }
           
           return new Pair(ids, data);
        }finally{
            dataLock.unlockRead(stamp);
        }
        
    }
    
    /**
     * Checks if the data is old, if so (or is forced) it updates the data. The
     * JSON for each entity is saved in the fieldData. Fields will need to parse
     * the JSON as needed.
     * @param core 
     * @param force Force an update, even if the data isn't old.
     */
    private void updateDataField(Core core, boolean force){
        
        long stamp = dataLock.writeLock();
        try {
            if(!isDataOld() && !force && !fieldData.isEmpty())
                return;
            
            fieldData = new BuildEntityData().getEntityData(core, id);
            lastDataUpdate = Calendar.getInstance();
            core.saveEntityInfo();
        } catch (IOException ex) {
            core.getLogger().log(Level.WARNING, "IOException loading entity data for [{0}]: {1}", 
                    new Object[] {name, ex.toString()});
        }finally{
            dataLock.unlockWrite(stamp);
        }
    }
    
    
    
    
}
