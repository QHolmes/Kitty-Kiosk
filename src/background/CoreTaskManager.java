/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package background;

import core.ActionObject;
import core.Core;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author Quinten Holmes
 */
public class CoreTaskManager extends Thread{
    
    private final Core core;
    private final int refeshRate;
    private final Map<String, ActionObject> actionsActive;
    private Calendar today;
    
    /**
     * Will call cleanMap on the given core every refeshRate seconds. It will remove any object
     * older than objectMapAge seconds old.
     * @param core
     * @param refeshRate
     * @param actionsActive 
     */
    public CoreTaskManager(Core core, int refeshRate, Map actionsActive){
        this.core = core;
        this.refeshRate = refeshRate;
        this.actionsActive = actionsActive;
        today = Calendar.getInstance();
    }
    
    @Override
    public void run(){
       try {
           while(!Thread.currentThread().isInterrupted()){
               core.getLogger().log(Level.FINE, "Data Cleaner Checking...");
               
               core.cleanHTTPMaps();
               core.updateEntityInfo();
               actionsActive.forEach((k, v) -> {  
                    try{
                       if(v.needsUpdate()) 
                           v.update();
                    }catch(Exception e){
                      core.getLogger().log(Level.SEVERE, "Error checking for update requirement "
                              + "ActionObject {0}, Exception: {1}", new Object[]{k, e.getMessage()});
                    }
                 });
                Calendar yesterday = Calendar.getInstance();
                yesterday.add(Calendar.DAY_OF_YEAR, -1);
                
               if(today.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)
                   && today.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)){
                   
                   core.getLogger().log(Level.INFO, "Turning log over");
                   core.setupLogger();
                   today = Calendar.getInstance(); 
               }
               
               core.saveHTTPMap();
               
               Thread.sleep(refeshRate * 1000);
            }
       } catch (InterruptedException ex) {
           core.getLogger().log(Level.INFO, "Cleaner stopped.");
       }
    }
    
}
