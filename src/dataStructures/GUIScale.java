/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dataStructures;

import core.Core;
import helperClasses.Save;
import java.io.Serializable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 *
 * @author Quinten Holmes
 */
public class GUIScale implements Serializable{
    
    private static final long serialVersionUID = 1;
    
    private double leftMenuWidthDouble = 250;
    private transient DoubleProperty leftMenuWidth;
    private double leftMenuButtonHeightDouble = 60;
    private transient DoubleProperty leftMenuButtonHeight;
    private transient DoubleProperty leftMenuIconHeight;
    private transient Core core;
    
    /**
     * This needs to be called before any observable are got. Each observable is 
     * created and its saved value is used to set it.
     * @param core
     */
    public void initialize(Core core){
        this.core = core;
        leftMenuWidth = new SimpleDoubleProperty(leftMenuWidthDouble);
        leftMenuButtonHeight = new SimpleDoubleProperty(leftMenuButtonHeightDouble);
        leftMenuIconHeight = new SimpleDoubleProperty(leftMenuButtonHeightDouble * 0.65);
    }
    
    /**
     * Saves the given double and changes the corresponding properties value
     * to match. Default value = 250
     * @param leftMenuWidthDouble 
     */
    public void setLeftMenuWidth(double leftMenuWidthDouble) {
        this.leftMenuWidthDouble = leftMenuWidthDouble;
        leftMenuWidth.set(leftMenuWidthDouble);
    }

    /**
     * Returns a DoubleProperty for the width of the left menu. This property will
     * be updated when setLeftMenuWidth is called.
     * @return 
     */
    public DoubleProperty getLeftMenuWidth() {
        if(leftMenuWidth == null)
            initialize(core);
        
        return leftMenuWidth;
    }

    /**
     * Saves the given double and changes the corresponding properties value
     * to match. Default value = 60
     * @param leftMenuButtonHeightDouble 
     */
    public void setLeftMenuButtonHeight(double leftMenuButtonHeightDouble) {
        this.leftMenuButtonHeightDouble = leftMenuButtonHeightDouble;
        leftMenuButtonHeight.set(leftMenuButtonHeightDouble);
        
        //Update the icon size as well
        leftMenuIconHeight.set(leftMenuButtonHeightDouble * .65);
    }

    /**
     * Gets the height for the icon. This is based on the button height but is 
     * x0.65 the size. 
     * @return 
     */
    public DoubleProperty getLeftMenuIconHeight() {
        return leftMenuIconHeight;
    }
    
    

    /**
     * Returns a DoubleProperty for the hight of the left menu buttons. This 
     * property will be updated when setLeftMenuButtonHeight is called.
     * @return 
     */
    public DoubleProperty getLeftMenuButtonHeight() {
        if(leftMenuButtonHeight == null)
            initialize(core);
        
        return leftMenuButtonHeight;
    }

    /**
     * Changes all properties to a different number and then back to the saved
     * value to trigger listeners.
     */
    public void refresh() {
        leftMenuWidth.set(leftMenuWidthDouble + 1);
        leftMenuButtonHeight.set(leftMenuButtonHeightDouble + 1);
        leftMenuIconHeight.set(leftMenuButtonHeightDouble);
        
        leftMenuWidth.set(leftMenuWidthDouble);
        leftMenuButtonHeight.set(leftMenuButtonHeightDouble);
        leftMenuIconHeight.set(leftMenuButtonHeightDouble * .65);
    }
    
    public synchronized void save(Core core){
        Save.saveObject(this, "GUIScale", core);
    }
    
    
}
