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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import core.Config;

/**
 *
 * @author Quinten Holmes
 */
public class BuildEntityData {
    
    public List<String> getEntityData(Core core, String entityID) throws IOException{
        ArrayList<String> entityData = null;

        Pair<String, Integer> tempPair = Entities.getEntiteObjects(core, 0, entityID);
        Pair<JSONObject, Integer> reply = new Pair<>(new JSONObject(tempPair.getKey()), tempPair.getValue());

        if(reply.getValue() == 200) {
            ExecutorService executor = Executors.newFixedThreadPool(30);
            
            JSONObject json = reply.getKey();
            JSONArray array = json.getJSONArray("objects");
            int number = json.getJSONObject("totals").getInt("group_totals");
            int offset = 0;
            entityData = new ArrayList<>(number); 
            
            int threadNumber = number / Config.RESULTS_LIMIT;
            
            if(number % Config.RESULTS_LIMIT > 0)
                threadNumber++;
            
            Thread th;
            for (int i = 0; i < threadNumber; i++) {
                executor.execute(new EntityDataThread(entityID, offset, entityData, core));
                offset += Config.RESULTS_LIMIT;
            }
            
            
            executor.shutdown();
            try {
                executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException ex) {}
            
        }else {
            core.getLogger().log(Level.SEVERE, "Error getting entity data Code-[{0}] Message[{1}]", 
                   new Object[] {reply.getValue(), reply.getKey().toString()});
        }

        return entityData;		
    }
    
    private class EntityDataThread extends Thread{
        
        private final String entityID;
        private final int offset;
        private final ArrayList<String> entityData;
        private final Core core;

        public EntityDataThread(String entityID, int offset, ArrayList<String> entityData, Core core) {
            this.entityID = entityID;
            this.offset = offset;
            this.entityData = entityData;
            this.core = core;
        }
        
        @Override
        public void run(){
            Pair<String, Integer> tempPair;
            try {
                //Make the GET call to Asset Panda
                tempPair =Entities.getEntiteObjects(core, offset, entityID);
            
                JSONArray array;
                JSONObject json = new JSONObject(tempPair.getKey());

                if(tempPair.getValue() == 200) {
                    //If sucessful, add all objects to the given arrayList
                    array = json.getJSONArray("objects");
                    for(int i = 0; i < array.length(); i++) {
                        json = array.getJSONObject(i);				
                        entityData.add(json.toString());				
                    }
                }
                else {
                    //Error, code !=200
                    String base = json.getJSONArray("errors").getJSONObject(0).getJSONArray("base").getString(0);
                    core.getLogger().log(Level.WARNING, "Error getting entity data, HTTP Code-[{0}] Message-[{1}]",
                            new Object[] {tempPair.getValue(), base});
                    return;
                }
            } catch (IOException ex) {
                core.getLogger().log(Level.WARNING, "IOException while getting entity data: {0}", ex.toString());
            }
        
        }
        
    }
    
}
