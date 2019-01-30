/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructures.actions;

import core.Core;
import http.Entities;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Reads the given JSONObject and extracts row data
 * @author Quinten Holmes
 */
public class ActionColumnInfo implements Serializable{
    private static final long serialVersionUID = 1;
    
    public int order;
    public final String id;
    public String name;
    public String key;
    public String type;
    public String fieldDataType; //HTTP return type to expect
    public String delimiter;
    public String sourceEntityID;
    public String description;
    public String filteredBySameValueForFieldWithID;
    public String longName;
    public String JSONString;
    public String defaultValue;
    public String[] listOptions;
    public String[] filteredListID;
    public String[] showOnFilterEntityObjectIDs;
    
    public boolean isOpen;
    public boolean isRequired;
    public boolean isEditable;
    public boolean isReturnField;
    public boolean isRequiredReturn;
    public boolean isEditableReturn;
    public boolean isFiltered;
    public boolean showOnAllFilterEntityObjects;
    public boolean mobileScanOnly;
    public boolean allowScanning;
    public boolean display; //True this column should be shown on the table, else false
    
    public String referenceFieldKey; //The key of the action assocated with
    public String associatedFieldKey; //The key on the object entitiy field that this field is linked through
    public boolean referenceJSON; //True if the reference field is a JSON object
    public boolean isReference; //True this column is not part of the action, else false
    
    public String displayField;
    public String displayEntity;
    
    /**
     * Constructor or adding native action fields
     * @param row JSONObject from the Action Object
     * @throws JSONException 
     */
    public ActionColumnInfo(JSONObject row) throws JSONException{
        //Get String Objects
        id = row.getString("id");
        updateRow(row);
        display = true;
    }
    
    /**
     * When added an additional column from an associated entity, this constructor is used.
     * @param associatedFieldKey Key of entity this field is from
     * @param referenceFieldKey The key of field to be added from the referenced entity
     * @param name
     * @param order
     * @param JSON True if the field referenced is returned as a JSON object
     */
    public ActionColumnInfo(String referenceFieldKey, String associatedFieldKey, String sourceEntityID, String name, int order, boolean JSON){
        id = null;
        this.associatedFieldKey = associatedFieldKey;
        this.referenceFieldKey = referenceFieldKey;
        this.sourceEntityID = sourceEntityID;
        this.order = order;
        this.referenceJSON = JSON;
        this.name = name;
        display = true;
        isReference = true;
        isReturnField = false;
    }
    
    /**
     * 
     * @param row Updates the information of the given row.
     * @throws JSONException 
     */
    public final void updateRow(JSONObject row) throws JSONException{
        //Reference fields are treated differently
        if(isReference)
            return;
        
        //Get String info
            order = row.getInt("order");
            name = row.getString("name");
            key = row.getString("key");
            type = row.getString("type");
            JSONString = row.toString();
        
        //The following could be null, checking first
            Object field = row.get("delimiter");
            if(field != null)
                delimiter = field.toString();

            field = row.get("source_entity_id");
            if(field != null)
                sourceEntityID = field.toString();

            field = row.get("description");
            if(field != null)
                description = field.toString();

            field = row.get("filtered_by_same_value_for_field_with_id");
            if(field != null)
                filteredBySameValueForFieldWithID = field.toString();
        
        //Get array objects
            JSONArray array;

            array = row.getJSONArray("list_options");
            if(array.length() > 0){
                listOptions = new String[array.length()];
                for(int i = 0; i < array.length(); i++)
                    listOptions[i] = array.getString(i);
            }

            array = row.getJSONArray("filtered_list_ids");
            if(array.length() > 0){
                filteredListID = new String[array.length()];
                for(int i = 0; i < array.length(); i++)
                    filteredListID[i] = array.getString(i);
            }

            array = row.getJSONArray("show_on_filter_entity_object_ids");
            if(array.length() > 0){
                showOnFilterEntityObjectIDs = new String[array.length()];
                for(int i = 0; i < array.length(); i++)
                    showOnFilterEntityObjectIDs[i] = array.getString(i);
            }
        
        //Get boolean Objects
            isOpen = row.getBoolean("is_open_field");
            isRequired = row.getBoolean("is_required");
            isEditable = row.getBoolean("is_editable");
            isReturnField = row.getBoolean("is_return_field");
            isRequiredReturn = row.getBoolean("is_required_return");
            isEditableReturn = row.getBoolean("is_editable_return");
            isFiltered = row.getBoolean("is_filtered");
            showOnAllFilterEntityObjects = row.getBoolean("show_on_all_filter_entity_objects");
        
        //Get data type
            switch(row.getString("type")){
                case ("MultipleListField"):
                case ("MultipleEntityListField"):
                    fieldDataType = "Array";
                    break;
                case ("EntityListField"):
                    fieldDataType = "JSON";
                    break;
                case ("YesNoField"):
                    field = row.getJSONObject("extra_details").get("default_value");
                    if(field != null)
                        defaultValue = (String) field;
                    
                    fieldDataType = "YesNoField";   
                    break;
                case ("DisplayValueField"):
                    fieldDataType = "DisplayValueField";
                    
                        displayField = row.getJSONObject("extra_details")
                                .getJSONObject("view_field")
                                .getString("key");
                        displayEntity = row.getJSONObject("extra_details")
                                .getJSONObject("view_field")
                                .getString("entity_id");
                    break;
                default:
                    fieldDataType = "String";
                    break;
            }
        
        //Get extra detail section
            JSONObject json = row.getJSONObject("extra_details");

            try{longName = json.getString("long_name");}catch(JSONException e){}

            //Other functions might be added later
            try{
            switch(type){
                case ("TextField"):
                    mobileScanOnly = row.getJSONObject("extra_details").getBoolean("mobile_scan_only");
                    allowScanning = row.getJSONObject("extra_details").getBoolean("allow_scanning");
                    break;
                case ("StatusField"):
                case ("ListField"):
                    field = row.getJSONObject("extra_details").get("default_value");
                    if(field != null)
                        defaultValue = (String) field;
                    break;
            }
            }catch(JSONException e){}
    }
    
    @Override
    public String toString(){
        return name;
    }
    
    
    public String getColumnData(JSONObject actionObject, Core core){
        String field = null;
        StringBuilder builder = new StringBuilder();
        
        try{
            if(isReference){
                String refID = actionObject.getJSONObject(associatedFieldKey).getString("id");
                JSONObject refJSON = new JSONObject(Entities.getEntityObject(core, refID, true).getKey());

                if(referenceJSON){
                    refJSON = new JSONObject(refJSON.getJSONObject(referenceFieldKey));
                    field = refJSON.getString("display_name");
                }else{
                    if(refJSON.isNull(referenceFieldKey))
                        field = "";
                    else
                        field = refJSON.getString(referenceFieldKey);
                }
            }else{
                switch(fieldDataType){
                    case("Array"):
                        if(actionObject.isNull(key)){
                            field = "";
                            break;
                        }

                        JSONArray fieldArray = actionObject.getJSONArray(key);
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
                        if(actionObject.isNull(key))
                            field = "";
                        else
                            field = actionObject.getString(key);
                        break;
                    case("JSON"):
                        if(actionObject.isNull(key) || actionObject.getJSONObject(key).isNull("display_name"))
                            field = "";
                        else
                            field = actionObject.getJSONObject(key).getString("display_name");
                        break;
                    case("YesNoField"):
                        if(actionObject.isNull(key))
                            field = "";
                        else{
                            if(actionObject.getBoolean(key))
                                field = "Yes";
                            else
                                field = "No";
                        }

                        break;
                    case ("DisplayValueField"):
                        JSONObject refJSON = new JSONObject(Entities.getEntityObject(core, displayEntity, true).getKey());
                        
                        try{
                            if(refJSON.isNull(displayField))
                                field =  "";
                            else
                                field = refJSON.getString(displayField);
                        }catch(JSONException e){
                            if(refJSON.isNull(displayField))
                                field =  "";
                            else
                                field = refJSON.getJSONObject(displayField).getString("display_name");
                        }
                        
                        break;
                    default:
                        field = "";
                }  
            }
        }catch(IOException | JSONException e){
            field = "";
            String logName = name;
            if(logName == null || logName.isEmpty())
                logName = "Unknown";
            core.getLogger().log(Level.SEVERE, "Error getting data for column [{0}] {1}", 
                  new Object[]{logName, e.toString()});
        }
        
        return field;
    }
    
}
