/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helperClasses;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.scene.control.Button;

//Not sure if in use, check

/**
 *
 * @author Quinten Holmes
 */
public class SortList<T> {
    private Comparator<T> compare;
    public List sort(List<T> list, Comparator<T> compare){
        this.compare = compare;
        if(list.size() > 1){
             int mid1 = list.size() / 2;
            //Split the first list in 2
            List<T> left = new ArrayList<>(mid1 + 1);
            List<T> right = new ArrayList<>(mid1 + 1);
            
            
            for (int i = 0; i < mid1; i++) {
                left.add(list.get(i));
            }
            
            for (int i = mid1; i < list.size(); i++){
                right.add(list.get(i));
            } 
            
            List<T> done = mergeSort(left, right);
            for(T n : done)
                System.out.println("B-" +((Button) n).getText());
            return done;
        }else
            return list;
    }
    
    private List mergeSort(List<T> l1, List<T> l2){
        
        ArrayList<T> sorted = new ArrayList<>(l1.size() + l2.size());
        if(l1.size() > 1 && l2.size() > 1){
            int mid1 = l1.size() / 2;
            //Split the first list in 2
            List<T> l1Left = new ArrayList<>(mid1 + 1);
            List<T> l1Right = new ArrayList<>(mid1 + 1);
            
            for (int i = 0; i < mid1; i++) {
                l1Left.add(l1.get(i));
            }
            
            for (int i = mid1; i < l1.size(); i++){
                l1Right.add(l1.get(i));
            }
            
            //Split the second list in 2
            mid1 = l2.size() / 2;
            List<T> l2Left = new ArrayList<>(mid1 + 1);
            List<T> l2Right = new ArrayList<>(mid1 + 1);
            
            for (int i = 0; i < mid1; i++) {
                l2Left.add(l2.get(i));
            }
            
            for (int i = mid1; i < l2.size(); i++){
                l2Right.add(l2.get(i));
            }
            
            //Sort and return
            l1 = mergeSort(l1Left, l1Right);
            l2 = mergeSort(l2Left, l2Right);
        }
        
        int p1 = 0;
        int p2 = 0;
        
        for (int i = 0; i < l1.size() + l2.size() && p1 < l1.size() && p2 < l2.size(); i++) {
            String n1 = ((Button) l1.get(p1)).getText();
            String n2 = ((Button) l2.get(p2)).getText();
            if(compare.compare(l1.get(p1), l2.get(p2)) < 0){
                sorted.add(l1.get(p1));
                p1++;
            }else{
               sorted.add(l2.get(p2));
               p2++;
            }
        }
        
        if(p1 < l1.size())
            for (int i = p1; p1 < l1.size(); p1++) 
                sorted.add(l1.get(p1));
        
        if(p2 < l2.size())
            for (int i = p2; p2 < l2.size(); p2++) 
                sorted.add(l2.get(p2));
        
        return sorted;
    }
}
