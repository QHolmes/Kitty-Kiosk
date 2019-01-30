/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package helperClasses;

import java.util.ArrayList;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import dataStructures.actions.ActionObjectEntite;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.text.Text;

/**
 *
 * @author Quinten Holmes
 */
public class AutoTableView{
    private ArrayList<Integer> sizes;
    private final TableView<ActionObjectEntite> table;
    
    public AutoTableView(TableView<ActionObjectEntite> table){
        this.table = table;
        
        //Set the number of columns to the number of elemts in sizes,
        //each row will represent the length of the longest string in the
        //corisponding index.
        sizes = new ArrayList();
        
        if(table.getColumns() == null || table.getColumns().isEmpty())
            return;
        
        //Sets the header of the column as the starting longest value
        table.getColumns().forEach((c) -> {
            if(c.isVisible())
                sizes.add((int) Math.ceil(c.getText().length() * 1.3));
            else
                sizes.add(0);
        });
        
        //Set what happens when a new row is added
        
        table.getItems().addListener((ListChangeListener)c -> {
            c.next();
            if(c.wasAdded())
                c.getAddedSubList().forEach((row) -> {
                    rowAdded((ActionObjectEntite) row, true);
            });
        });
        
        
        table.widthProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            reSize();
        });
        
        
        //If there are items added to the table find thier length and resize the
        //table. Else we are done
        if(table.getItems() == null || table.getItems().isEmpty())
            return;
        
        table.getItems().forEach(item -> {
            try{
            rowAdded(item, false);
            }catch(Exception e){
                e.printStackTrace();
            }
        });
        
        reSize();
        
        
    }
    
    private void rowAdded(ActionObjectEntite row, boolean allowResize){
        boolean resize = false;
        
        if(row == null)
            return;
        
        try{
            for(int i = 0; i < row.getFieldSize(); i++){
                if(row.getField(i).length() > sizes.get(i)){
                    sizes.set(i, row.getField(i).length());
                    resize = true;
                }
            }
                
        }catch (ArrayIndexOutOfBoundsException ex){
            
        }
        
        if(resize && allowResize)
            reSize();
    }
    
    private void reSize(){
        double width = table.getWidth() - 7;
        width -= (sizes.size() * 2);
        double totalLength = 0;
       
        //Get the width in pixels of on character 
        Text testLabel = new Text("A");
        testLabel.setStyle("-fx-font-size: 12px;");
        Scene testScene = new Scene(new Group(testLabel));
        testLabel.applyCss();
        
        //Calculate the min width based on number of characters times the charWidth 
        double charWidth = testLabel.getLayoutBounds().getWidth();
        double[] minWidths = new double[sizes.size()];
        double totalWidth = 0;
        
        //Ensure every column has atleast the minimum amount of space
        for (int i = 0; i < sizes.size(); i++) {
            minWidths[i] = (sizes.get(i) * charWidth) + 5;
            totalWidth += minWidths[i];
        }
        
        for(int i = 0; i < sizes.size(); i++){
                table.getColumns().get(i).setPrefWidth(minWidths[i]);
        }
        
        //If there is space left over give out based on ratio
        if(totalWidth < width){
            double extra = width - totalWidth;
            for(int i = 0; i < sizes.size(); i++){
                totalLength += sizes.get(i);
            }  

            for(int i = 0; i < sizes.size(); i++){
                table.getColumns().get(i).setPrefWidth(((sizes.get(i)/totalLength) * extra) + minWidths[i]);
            }
        }
        
    }
    
    private static Method columnToFitMethod;

    static {
        try {
            columnToFitMethod = TableViewSkin.class.getDeclaredMethod("resizeColumnToFitContent", TableColumn.class, int.class);
            columnToFitMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static void autoFitTable(TableView tableView) {
        tableView.getItems().addListener(new ListChangeListener<Object>() {
            @Override
            public void onChanged(Change<?> c) {
                for (Object column : tableView.getColumns()) {
                    try {
                        columnToFitMethod.invoke(tableView.getSkin(), column, -1);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
