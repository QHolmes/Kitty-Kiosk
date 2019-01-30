/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helperClasses;

import java.util.Comparator;
import javafx.scene.Node;
import javafx.scene.control.Button;
//Not sure if in use, check


/**
 *
 * @author Quinten Holmes
 */
public class ButtonComparator implements Comparator<Node> {

    @Override
    public int compare(Node t, Node t1) {
        return ((Button)t).getText().compareToIgnoreCase(((Button)t1).getText());
    }
    
}
