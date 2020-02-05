/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package background ;

import dataStructures.actions.ActionInfo;
import core.ActionObject;
import core.Core;
import dataStructures.actions.ActionColumnInfo;
import dataStructures.actions.ActionObjectEntite;
import javafx.util.Pair;
import helperClasses.AutoTableView;
import http.Entities;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import core.Config;

/**
 *
 * @author Quinten Holmes
 */
public class BuildActionHistory extends Thread{
    
    private final Core core;
    private final ActionInfo actInfo;
    private ArrayList<String> entities;
    private ArrayList<ActionObjectEntite> actionHistory;
    private ArrayList<ActionObjectEntite> unreturnedActions;
    private ObservableList<ActionObjectEntite> historyList;
    private ObservableList<ActionObjectEntite> returnList;
    
    private boolean interupt = false;
    private boolean done = false;
    private final ActionObject actionObject;
    private final TableView<ActionObjectEntite> historyTable;
    private final TableView<ActionObjectEntite> returnTable;
    private Thread tableReturn;
    
    public BuildActionHistory(ActionInfo actInfo, ActionObject actionObject, Core core){        
        this.core = core;
        this.actInfo = actInfo;
        this.actionObject = actionObject;
        
        historyTable = new TableView<>();
            Label label = new Label("No action history found");
            label.getStyleClass().add("actionSubtitle");
            historyTable.setPlaceholder(label);
        
        returnTable = new TableView<>();
            label = new Label("No unreturned actions");
            label.getStyleClass().add("actionSubtitle");
            returnTable.setPlaceholder(label);
    }
    
    @Override
    public void run(){
        synchronized(this){
            //Get the total number of assets and create 5 threads of getEntities (private class)
            fillEntities();
            if(interupt || Thread.currentThread().isInterrupted())
                return;
            
            
            //Get history of each enity with more than 0 action objects of our given ID            
            actionHistory = new ArrayList<>();
            unreturnedActions = new ArrayList<>();

            getActionHistory(); 
            
            //Check for interrupt
            if(interupt || Thread.currentThread().isInterrupted()){
                interupt = true;
                return;
            }
            
            //Add all data
            historyList = FXCollections.observableArrayList();
            historyList.addAll(actionHistory);
            
            if(actInfo.isReturnable){
                returnList = FXCollections.observableArrayList();            
                returnList.addAll(unreturnedActions);
            }
            
            done = true;
            notify();
        }
        
        
    }
    
    private void getActionHistory(){
        
        Thread th;
        ArrayList<Thread> historyArray = new ArrayList<>(entities.size());
        String threadName = actInfo.actionName + "-fullHistory-";
        ExecutorService executor = Executors.newFixedThreadPool(30);
        
        //Each entity needs it's own thread
        for(int i = 0; i < entities.size(); i++){
            th = new getHistory(actionHistory, unreturnedActions, actInfo, core, entities.get(i));
            th.setName(threadName + entities.get(i));
            th.setDaemon(true);
            th.setPriority(Thread.MIN_PRIORITY);
            historyArray.add(th);
            executor.execute(th);
        }

        //Wait for all threads to finish
        try {
            executor.shutdown();
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
            
            boolean lol = true;
            lol = false;
        } catch (InterruptedException ex) {
            //If this thread is interrupted, interrupt the others and set a the flag
            historyArray.forEach(thread -> { thread.interrupt();});
            
            interupt = true;
        }  catch (Exception ex){
            ex.printStackTrace();
        }
    }
    
    private void fillEntities(){
        try {
            //Get total number of assets
            Pair<String, Integer> reply = Entities.getEntiteObjectsActions(core, 0, actInfo.entityID);
            JSONObject json = new JSONObject(reply.getKey()).getJSONObject("totals");
            int total = json.getInt("group_totals");

            //Create threads and start them
            entities = new ArrayList<>(total);
            ArrayList<BuildEntityAction> getEntArray = new ArrayList<>(5);
            int startValue = 0;

            int numberOfThreads = total / 100;
            if(numberOfThreads <= 0)
                numberOfThreads = 1;
            
            int skip = Config.RESULTS_LIMIT * numberOfThreads;
            for(int i = 0; i < numberOfThreads; i++){
                getEntArray.add(new BuildEntityAction(entities, core, actInfo.entityID, actInfo.actionID, startValue, skip, total));
                startValue += Config.RESULTS_LIMIT;
                getEntArray.get(i).setName(this.getName() + "-entities " + i);
                getEntArray.get(i).setDaemon(true);
                getEntArray.get(i).setPriority(Thread.MIN_PRIORITY);
                getEntArray.get(i).start();
            }

            //Wait for all threads to be finished, if this thread is interrupted interupt all others
            try {
                for (Thread thread : getEntArray) {
                    thread.join();
                }
            } catch (InterruptedException ex) {
                getEntArray.forEach((thread) -> {
                    thread.interrupt();
                    interupt = true;
                });
            }
        }catch (IOException ex) {
            Logger.getLogger(BuildActionHistory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public ObservableList getHistoryList() {
        return historyList;
    }

    public ObservableList getReturnList() {
        return returnList;
    }
    
       
    
    public TableView getHistoryTable(){
        TableColumn<ActionObjectEntite, String> col;

        //Add all the columns to the table
        ArrayList<ActionColumnInfo> actionOrder = actInfo.getActionOrderColumns();
        ArrayList<TableColumn<ActionObjectEntite, String>> colOrdering = new ArrayList<>(); 
        for(int i = 0; i < actionOrder.size(); i++){
            if(!actionOrder.get(i).display){
                continue;
            }
            
            col = new TableColumn<>(actionOrder.get(i).name); 
            final int index = i;
            col.setCellValueFactory(data -> {
                try{
                    return new SimpleStringProperty(data.getValue().getField(index));
                }catch (NullPointerException ex){
                    return new SimpleStringProperty("");
                }
            });
            colOrdering.add(col);
        }
        //Sort Columns
        colOrdering.sort(new SortColumns(actInfo.historyOrder));
        colOrdering.forEach((c) -> {
            historyTable.getColumns().add(c);
        });
        
        historyTable.getItems().addAll(historyList);
        
        new AutoTableView(historyTable);
        
        //Return items when double clicked
        historyTable.setRowFactory(tv -> {
            TableRow<ActionObjectEntite> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    ActionObjectEntite rowData = row.getItem();
                    actionObject.openActionPaneTable(rowData);
                }
            });
            return row ;
        });
        
        //Save column order changes
        historyTable.getColumns().addListener((ListChangeListener.Change<? extends TableColumn<ActionObjectEntite, ?>> change) -> {
                ObservableList<TableColumn<ActionObjectEntite, ?>> newOrder = historyTable.getColumns();
                StringBuilder builder = new StringBuilder();
                ArrayList<String> newColOrder = new ArrayList<>();

                //Get new column order
                newOrder.forEach( c ->{
                        newColOrder.add(c.getText());
                        builder.append("| ").append(c.getText()).append(" ");
                });

                builder.append("|");

                actInfo.newReturnOrder(newColOrder);
                core.saveActionsActive();

                //Log
                core.getLogger().log(Level.INFO, "[{0}] column reorder.", actInfo.actionName);
                core.getLogger().log(Level.FINE, "[{0}] new order: {1}", 
                        new Object[]{actInfo.actionName, builder.toString()});
            });
        
        return historyTable;
    }
    
    class SortColumns implements Comparator<TableColumn<ActionObjectEntite,?>> { 
        
        List<String> order;
        public SortColumns(List<String> m){
            order = new ArrayList<>();
            if(m != null)
                order.addAll(m);
        }
        
        @Override
        public int compare(TableColumn<ActionObjectEntite,?>a, TableColumn<ActionObjectEntite,?> b){ 
            
            if(a.getText().compareToIgnoreCase(b.getText()) == 0)
                return 0;
            
            for(String s: order){
                if(a.getText().compareToIgnoreCase(s) == 0)
                    return -1;
                
                if(b.getText().compareToIgnoreCase(s) == 0)
                    return 1;      
            }
            return 0; 
        } 
    }
    
    public TableView getReturnTable(){
        if(!actInfo.isReturnable)
            return getHistoryTable();
             
            
        TableColumn<ActionObjectEntite, String> col;
        if(returnList == null)
            return returnTable;

        //Add all the columns to the table
        ArrayList<TableColumn<ActionObjectEntite, String>> colOrdering = new ArrayList<>(); 
        ArrayList<ActionColumnInfo> actionOrder = actInfo.getActionOrderColumns();
        for(int i =0; i < actionOrder.size(); i++){
            String name = actionOrder.get(i).name;
            col = new TableColumn<>(name); 
            final int index = i; //actionObject ID is the first row, entityObject ID is second
            col.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getField(index)));
            colOrdering.add(col);

            if(actionOrder.get(i).isReturnField)
                col.setVisible(false);

            if(!actionOrder.get(i).display)
                col.setVisible(false);
        }

        //Sort Columns
        colOrdering.sort(new SortColumns(actInfo.returnOrder));

        colOrdering.forEach((c) -> {
            returnTable.getColumns().add(c);
        });

        returnTable.setItems(returnList);
        new AutoTableView(returnTable);
        
        //Return items when double clicked
        returnTable.setRowFactory(tv -> {
            TableRow<ActionObjectEntite> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    ActionObjectEntite rowData = row.getItem();
                    actionObject.returnActionEntitie(rowData, returnTable);
                }
            });
            return row ;
        });

        //Save column order changes
        returnTable.getColumns().addListener((ListChangeListener.Change<? extends TableColumn<ActionObjectEntite, ?>> change) -> {
                ObservableList<TableColumn<ActionObjectEntite, ?>> newOrder = returnTable.getColumns();
                StringBuilder builder = new StringBuilder();
                ArrayList<String> newColOrder = new ArrayList<>();

                //Get new column order
                newOrder.forEach( c ->{
                        newColOrder.add(c.getText());
                        builder.append("| ").append(c.getText()).append(" ");
                });

                builder.append("|");

                actInfo.newReturnOrder(newColOrder);
                core.saveActionsActive();

                //Log
                core.getLogger().log(Level.INFO, "[{0}] column reorder.", actInfo.actionName);
                core.getLogger().log(Level.FINE, "[{0}] new order: {1}", 
                        new Object[]{actInfo.actionName, builder.toString()});
            });

        return returnTable;
    }

     
    public boolean isDone(){
        return done;
    }
        
    private class getHistory extends Thread{
    
        private final ActionInfo actInfo;
        private final ArrayList<ActionObjectEntite> history;
        private final ArrayList<ActionObjectEntite> unReturned;
        private final String actionID;
        private final String entityID;
        private final Core core;
        
        /**
         * Gets all the actions of the given entityID
         * from the given starting point.
         * @param entities ArrayList of all entity object IDs
         * @param history  ArrayList to add all actions to
         * @param fieldKey The names of all keys wanted to be pulled
         * @param type The type of object to be found the given key
         * @param core
         * @param actionID the ID of the action to look for
         * @param startValue the starting index in entities
         */
        public getHistory(ArrayList<ActionObjectEntite> history, ArrayList<ActionObjectEntite> unReturned, ActionInfo actInfo, Core core, String entityID){
            this.actInfo = actInfo;
            this.actionID = actInfo.actionID;
            this.entityID = entityID;
            this.core = core;
            this.history = history;
            this.unReturned = unReturned;
        }
        
        @Override
        public void run(){
            Pair<String,Integer> reply;
            JSONArray array;
            int offset = 0;
            int gate = 0;
            
            boolean done = false;
            try {
                //Get all returned actions. Actions are returned, max, 30 at a time
                while(!done){
                    //loop to try call 3 times
                    while(gate < 3){
                        try{
                            reply = Entities.getActionObjects(core, entityID, actionID, offset, true, false);
                            array = new JSONArray(reply.getKey());
                            //Check if the array is empty
                            if(array.length() <= 0){
                                done = true;
                                break;
                            }

                            if(array.length() < 30)
                                done = true;

                            ActionObjectEntite row;
                            for(int i = 0; i < array.length(); i++){
                                JSONObject json = array.getJSONObject(i);
                                row = new ActionObjectEntite(actInfo, entityID, json, core);
                                history.add(row);
                            }  
                            offset += 30;
                            
                            gate = 0;
                        }catch (IOException ex) {
                            gate++;
                            core.getLogger().log(Level.WARNING, "IOException [{0}/3] getting action history for [{1}]: {2}",
                            new Object[] {gate, actInfo.actionName, ex.getMessage()});
                        }
                    }
                    
                }
                done = false;
                offset = 0;
                
                //Get all unreturned actions. Actions are returned, max, 30 at a time
                while(!done){
                    
                    gate = 0;
                    //loop to try call 3 times
                    while(gate < 3){
                        try{
                            reply = Entities.getActionObjects(core, entityID, actionID, offset, false, false);
                            array = new JSONArray(reply.getKey());
                            //Check if the array is empty
                            if(array.length() <= 0){
                                done = true;
                                break;
                            }

                            if(array.length() < 30)
                                done = true;

                            ActionObjectEntite row;
                            for(int i = 0; i < array.length(); i++){
                                JSONObject json = array.getJSONObject(i);
                                row = new ActionObjectEntite(actInfo, entityID, json, core);
                                history.add(row);
                                unReturned.add(row);
                            }
                            offset += 30;
                        }catch (IOException ex) {
                            gate++;
                            core.getLogger().log(Level.WARNING, "IOException [{0}/3] getting action history for [{1}]: {2}",
                                    new Object[] {gate, actInfo.actionName, ex.getMessage()});
                        }
                    }
                }
            }catch (JSONException ex){
                ex.printStackTrace();
            }
        }
     
    }//End get history
    
 }//End class


