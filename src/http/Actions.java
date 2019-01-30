/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package http;

import core.Core;
import javafx.util.Pair;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Quinten Holmes
 */
public class Actions {
    
    public static Pair<String, Integer> sendActionObject(Core core, String entityObjectID, String actionID, String urlParameters) throws IOException{
        String url = String.format("https://login.assetpanda.com:443/v2/entity_objects/%s/action_objects/%s", entityObjectID, actionID);
        return POST.postAssetPanda(url, urlParameters, core);
    }
    
    public static Pair<String, Integer> sendActionObjectReturn(Core core, String entityObjectID, String actionObjectID, String data) throws IOException{
        String url = String.format("https://login.assetpanda.com:443/v2/entity_objects/%s/action_objects/%s/return", entityObjectID, actionObjectID);
        return POST.postAssetPanda(url, data, core);
    }
    
    public static Pair<String, Integer> editActionObject(Core core, String entityObjectID, String actionObjectID, String data) throws IOException{
        String url = String.format("https://api.assetpanda.com:443/v2/entity_objects/%s/action_objects/%s", entityObjectID, actionObjectID);
        return PUT.sendPUTAssetPanda(url, data, core);
    }

    public static Pair<String, Integer> fetchActionObject(Core core, String entityObjectID, String actionObjectID) throws IOException{
        String url = String.format("https://api.assetpanda.com:443/v2/entity_objects/%s/action_objects/%s", entityObjectID, actionObjectID);
        return GET.getAssetPanda(url, core);
    }
   
    public static Pair<Boolean, String> checkActionReturn(Core core, String entityObjectID, String actionID) throws IOException {
        boolean isOut = false;
        String ActionObjectID = null;
            String url = String.format("https://login.assetpanda.com:443/v2/entity_objects/%s/action_objects?action_id=%s", entityObjectID, actionID);
            Pair<String, Integer> reply = GET.getAssetPanda(url, core);
            JSONArray array = new JSONArray(reply.getKey());
            
            if(array.length() <= 0){
                return new Pair(false, null);
            }else{
                JSONObject json = array.getJSONObject(0);
                if(!json.getBoolean("returned")){
                    ActionObjectID = json.getString("id");
                    isOut = true;
                }
            }
        
        return new Pair(isOut, ActionObjectID);
    }
    
}
