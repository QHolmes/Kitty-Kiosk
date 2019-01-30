/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package background;

import core.Core;
import javafx.util.Pair;
import http.Entities;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Quinten Holmes
 */
class BuildEntityAction extends Thread{
    
        private final ArrayList<String> entities;
        private final String entityID;
        private final String actionID;
        private int startValue;
        private final int skipValue;
        private final int total;
        private final Core core;
        
        public BuildEntityAction(ArrayList<String>entities, Core core, String entityID, String actionID, int startValue, int skipValue, int total){
            this.entities = entities;
            this.entityID= entityID;
            this.actionID = actionID;
            this.startValue = startValue;
            this.skipValue = skipValue;
            this.total = total;
            this.core = core;
        }
        
        @Override
        public void run(){
            
            Pair<String, Integer> reply;
            JSONObject json;
            JSONObject entityAction;
            JSONArray array;
            JSONArray entityActionTotals;
            
            synchronized(this){
                try {
                    while(startValue < total && !Thread.currentThread().isInterrupted()){
                        reply = Entities.getEntiteObjectsActions(core, startValue, entityID);
                        json = new JSONObject(reply.getKey());                    
                        array = json.getJSONArray("objects");

                        for(int i = 0; i < array.length() && !Thread.currentThread().isInterrupted(); i++){

                           json = array.getJSONObject(i);
                           entityActionTotals = json.getJSONArray("action_object_totals");

                           for(int j = 0; j < entityActionTotals.length(); j++){
                               entityAction = entityActionTotals.getJSONObject(j).getJSONObject("entity_action");
                               if(actionID.equals(entityAction.getString("id"))){
                                   entities.add(json.getString("id"));
                                   break;
                               }
                           }
                        }        

                        startValue += skipValue;
                    }
                } catch (IOException ex) {
                    core.getLogger().log(Level.WARNING, "IOException getting Entity Action history entityID-[{0}] message-{1}", new Object[]{entityID, ex.toString()});
                }

                notify();
            }//End synchronized
    }
}
