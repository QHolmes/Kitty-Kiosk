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
public class loginInformation implements Serializable{
    private static final long serialVersionUID = 1;
    
    private String token = "";
    private String clientID = "";
    private String email = "";
    private int pin = -1;
    private boolean requireCode = false;
    private boolean valid = false;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getPin() {
        return pin;
    }

    public void setPin(int pin) {
        this.pin = pin;
    }

    public boolean isRequireCode() {
        return requireCode;
    }

    public void setRequireCode(boolean requireCode) {
        this.requireCode = requireCode;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public synchronized void save(Core core){
        Save.saveObject(this, "loginInformation", core);
    }    
    
    
}
