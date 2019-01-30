/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helperClasses;

import core.ActionObject;
import core.Core;
import gui.AppController;
import javafx.scene.control.ToggleButton;

/**
 *
 * @author dholmes
 */
public class PinThread extends Thread{
    private final Core core;
    private final AppController app;
    private ActionObject ao;
    private ToggleButton toggle;
    
    public PinThread(Core core, AppController app){
        this.core = core;
        this.app = app;
    }
    
    public void updateObjects(ActionObject ao, ToggleButton toggle){
        this.ao = ao;
        this.toggle = toggle;
    }
    
    @Override
    public void run(){
    try{
      synchronized(this) {
        while(!Thread.interrupted()){
            wait();
            app.checkPin(ao, toggle);
        }
      }
    } catch (InterruptedException e) {}
  }
}
