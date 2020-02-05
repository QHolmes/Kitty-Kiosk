/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructures.entity;

import core.Core;
import javafx.util.Pair;
import java.io.Serializable;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Quinten Holmes
 */
public final class EntityFieldInfo implements Serializable{
    private static final long serialVersionUID = 1;

    public final EntityInfo entity;
    
    public boolean isDeleted;
    public boolean isRequired;
    public boolean isEntityDefault;
    public boolean isSecondaryDefault;
    public boolean includeInReplication;
    public boolean isFiltered;
    public boolean showOnAllFilterEntityObjects;
    public boolean showOnAllStatues;
    public boolean isParentField;
    public boolean disableMe;
    
    public int order;
    
    public final String id;
    public String name;
    public String key;
    public String type;
    public String color;
    
    public String[] listOptions;
    public String[] filteredListIds;
    public String[] showOnFilterEntityObjects;
    public String[] showOnAllStatuses;
    public String[] colorCoding;
    
    //Could be null
    public boolean mobileScanOnly;
    public boolean allowScanning;
    
    //could be null
    public String keyboardType;
    public String delimiter;
    public String description;
    public String sourceEntityID;
    public String parentID;
    public String parentName;
    public String additionalFieldID;
    
    //could be null
    public String[] additionalFieldKeys;
    
    public EntityFieldInfo(JSONObject json, EntityInfo entity){
        this.entity = entity;
        id = json.getString("id");
        updateField(json);
    }
    
    void updateField(JSONObject json) {
        //booleans
        isDeleted = json.getBoolean("is_deleted");
        isRequired = json.getBoolean("is_required");
        isEntityDefault = json.getBoolean("is_entity_default");
        isSecondaryDefault = json.getBoolean("is_secondary_default");
        includeInReplication = json.getBoolean("include_in_replication");
        isFiltered = json.getBoolean("is_filtered");
        showOnAllFilterEntityObjects = json.getBoolean("show_on_all_filter_entity_objects");
        showOnAllStatues = json.getBoolean("show_on_all_statuses");
        isParentField = json.getBoolean("is_parent_field");
        disableMe = json.getBoolean("disable_me");

        
        //Integers
        order = json.getInt("order");

        
        //Strings
        name = json.getString("name");
        key = json.getString("key");
        type = json.getString("type");
        color = json.getString("color");
        
        
        //could be null
        Object field;
        if(!json.isNull("keyboard_type"))
            keyboardType = json.getString("keyboard_type");
        
        if(!json.isNull("delimiter"))
            delimiter = json.getString("delimiter");
        
        if(!json.isNull("description"))
            description = json.getString("description");
        
        if(!json.isNull("source_entity_id"))
            sourceEntityID = json.getString("source_entity_id");

        
        
        //Array lists
        JSONArray array;
        
        array = json.getJSONArray("list_options");
        listOptions = new String[array.length()];
        for(int i = 0; i < array.length(); i++)
            listOptions[i] = array.getString(i);
        
        array = json.getJSONArray("list_options");
        filteredListIds = new String[array.length()];
        for(int i = 0; i < array.length(); i++)
            filteredListIds[i] = array.getString(i);
        
        array = json.getJSONArray("list_options");
        showOnFilterEntityObjects = new String[array.length()];
        for(int i = 0; i < array.length(); i++)
            showOnFilterEntityObjects[i] = array.getString(i);
        
        array = json.getJSONArray("list_options");
        showOnAllStatuses = new String[array.length()];
        for(int i = 0; i < array.length(); i++)
            showOnAllStatuses[i] = array.getString(i);
        
        array = json.getJSONArray("list_options");
        colorCoding = new String[array.length()];
        for(int i = 0; i < array.length(); i++)
            colorCoding[i] = array.getString(i);
        
        
        
        //additional_group_fields
        if(sourceEntityID != null){
            JSONObject additionalFields = json.getJSONObject("additional_group_fields");
            if(additionalFields.has("entity_id")){
                additionalFieldID = additionalFields.getString("entity_id");
                array = additionalFields.getJSONArray("keys");
                additionalFieldKeys = new String[array.length()];
                for(int i = 0; i < array.length(); i++)
                    additionalFieldKeys[i] = array.getString(i);
            }
            
        }
        
        //extra_details
        try{
            JSONObject extraDetails = json.getJSONObject("extra_details");
            
            switch(type){
                case ("TextField"):
                    mobileScanOnly = extraDetails.getBoolean("mobile_scan_only");
                    allowScanning = extraDetails.getBoolean("allow_scanning");
                    break;
            }
        }catch(JSONException e){}
        
        //parent_field
        if(!isParentField){
            JSONObject parentField = json.getJSONObject("parent_field");
            if(parentField.has("id")){
                parentID = parentField.getString("id");
                parentName = parentField.getString("name");
            }
        }        
    }
    
    /**
     * Gets the value, for all Entity Objects, in this field. 
     * @param core
     * @return String[] of all values, could return null if there was an error.
     */
    public Pair<String[], String[]> getFieldData(Core core){
        List<String> entitieData = entity.getFieldData(core);
        if(entitieData == null)
            return null;
        
        String[] ids = new String[entitieData.size()];
        String[] data = new String[entitieData.size()];
        String field;
        StringBuilder builder;
        JSONObject json;
        
        for(int i = 0; i < entitieData.size(); i++){
            json = new JSONObject(entitieData.get(i));
            ids[i] = json.getString("id");
            switch(type){
                case ("MultipleListField"):
                case ("MultipleEntityListField"):
                    if(json.isNull(key)){
                        field = "";
                        break;
                    }

                    JSONArray fieldArray = json.getJSONArray(key);
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
                case ("EntityListField"):
                    if(json.isNull(key) || json.getJSONObject(key).isNull("display_name"))
                        field = "";
                    else
                        field = json.getJSONObject(key).getString("display_name");
                    break;
                case ("YesNoField"):
                    if(json.isNull(key))
                        field = "";
                    else{
                        if(json.getBoolean(key))
                            field = "Yes";
                        else
                            field = "No";
                    }
                    break;
                default:
                    if(json.isNull(key))
                        field = "";
                    else
                        field = json.getString(key);
                    break;
            }  
            
            data[i] = field;
        }
        
        return new Pair(ids, data);
    }
    
}
