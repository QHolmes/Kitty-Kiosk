/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructures;

import core.Core;
import helperClasses.Save;
import java.io.Serializable;

/**
 *
 * @author Quinten Holmes
 */
public class coreInformation implements Serializable{
    private static final long serialVersionUID = 1;
    
    private int code = 0;
    private int APICalls = 0;
    private int checkoutAction = 0;
    private int mainPaneWidth = 1350;
    private int maxDataAge = 3;
    private int maxDataAgeModifier = 60;
    
    private final String lockAPICount = "";
    

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getAPICalls() {
        return APICalls;
    }

    public int incAPICalls() {
        synchronized(lockAPICount){
            APICalls++;
        }
        
        return APICalls;
    }

    public int getCheckoutAction() {
        return checkoutAction;
    }

    public void setCheckoutAction(int checkoutAction) {
        this.checkoutAction = checkoutAction;
    }

    public int getMainPaneWidth() {
        return mainPaneWidth;
    }

    public void setMainPaneWidth(int mainPaneWidth) {
        this.mainPaneWidth = mainPaneWidth;
    }

    public int getMaxDataAge() {
        return maxDataAge;
    }

    public void setMaxDataAge(int maxDataAge) {
        this.maxDataAge = maxDataAge;
    }

    public int getMaxDataAgeModifier() {
        return maxDataAgeModifier;
    }

    public void setMaxDataAgeModifier(int maxDataAgeModifier) {
        this.maxDataAgeModifier = maxDataAgeModifier;
    }
    
    public synchronized void save(Core core){
        Save.saveObject(this, "coreInformation", core);
    }
    
    
}
