package core;

import background.MyLogger;
import background.CoreTaskManager;
import dataStructures.entity.EntityInfo;
import dataStructures.GUIScale;
import dataStructures.coreInformation;
import dataStructures.loginInformation;
import gui.AppController;
import helperClasses.Save;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.*;

import http.Entities;
import http.GET;
import http.POST;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.util.Pair;

/**
 *
 * @author Quinten Holmes
 */
public class Core{        
    
    //Static vars
	public final String VERSION = "Asset Panda: Kitty Kiosk v0.7 Beta";
        public final int MAXCALLWAIT = 10000; //Max time to wait for a call to return in milliseconds
        private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    //Saved Vars
        private final loginInformation loginInfo;
        private final coreInformation coreInfo;
        private final GUIScale scale;
        private final HashMap<String, ActionObject> actionsActive;
        /**
         * |Call|Time call started|ArrayList<Thread> of waiting Objects|
         */
        private final HashMap<String, ArrayList<Object>> HTTPmap;
        private final HashMap<String, EntityInfo> entityInfoMap;
	
        
    //Transitent objects
        final public transient Object INPROGLOCK = new Object();
        private transient Pair<Double,Double> gps;
        private transient Map<String, ObservableList> observList = new HashMap();
        private transient StringProperty currentTime;
        private transient StringProperty currentDate;
        private transient StringProperty APICallsObservable;
        private transient CoreTaskManager cleaner;
        private transient Thread timeThread;
        private transient Thread dateThread;
        private transient Stage primaryStage;
        private transient AppController controller;
        
    @SuppressWarnings("Passing_suspicious_parameter_in_the_constructor")
    public Core(){
        //Load classes
        Serializable obj;

        //Load loginInfo
            obj = Save.loadObject("loginInformation", this);

            if(obj == null)
                loginInfo = new loginInformation();
            else
                loginInfo = (loginInformation) obj;

        //Load coreInfo
            obj = Save.loadObject("coreInformation", this);

            if(obj == null)
                coreInfo = new coreInformation();
            else
                coreInfo = (coreInformation) obj;

        //Load GUIScale info
            obj = Save.loadObject("GUIScale", this);

            if(obj == null)
                scale = new GUIScale();
            else
                scale = (GUIScale) obj;

        //Load actionsActive
            obj = Save.loadObject("actionsActive", this);

            if(obj == null)
                actionsActive = new HashMap();
            else
                actionsActive = (HashMap) obj;

        //Load HTTPmap
            obj = Save.loadObject("HTTPmap", this);

            if(obj == null)
                HTTPmap = new HashMap();
            else
                HTTPmap = (HashMap) obj;

        //Load entityInfoMap
            obj = Save.loadObject("entityInfoMap", this);

            if(obj == null)
                entityInfoMap = new HashMap();
            else
                entityInfoMap = (HashMap) obj;

    }
       

    /**
     * Returns the max width of the main pane.
     * @return 
     */
    public int getMainPaneWidth(){
        return coreInfo.getMainPaneWidth();
    }

    /**
     * Saves the max width of the main pane.
     * @param mainPaneWidth 
     */
    public void setMainPaneWidth(int mainPaneWidth){
        coreInfo.setMainPaneWidth(mainPaneWidth);
        coreInfo.save(this);
    }

    public boolean isRequireCode() {
        return loginInfo.isRequireCode();
    }
     
     /**
     * Will compare the given code with the current code and return if it is correct.
     * @param code pin to check against the correct pin.
     * @return True if the code is correct, else false. Will return true if no
     * code is set or if a pin is not required.
     */
    public boolean checkCode(int code){
        if(coreInfo.getCode() == code || code == 0 || !loginInfo.isRequireCode()){
            LOGGER.log(Level.INFO, "Pin entere");
            return true;
        }
            return false;
    }
     
     /**
      * Checks the given code and will set the code if it returns true. The given
      * code must be between 4-9 digits
      * @param code The 4-9 digit pin requested
      * @return True is the code is accepted and changed, else false.
      */

    public boolean setCode(int code) {
        if( code >= 1000 & code < 1000000000){
            coreInfo.setCode(code);
            loginInfo.setRequireCode(true);
            LOGGER.log(Level.INFO, "Pin updated.");
            return true;
        } else if (code <= 0 ){
            coreInfo.setCode(0);
            loginInfo.setRequireCode(false);
            LOGGER.log(Level.INFO, "Pin removed.");
        }
            return false;
    }
        
        public void initialize(AppController controller){
            if(cleaner != null)
                cleaner.interrupt();
            
            this.controller = controller;
            
            cleaner = new CoreTaskManager(this, 120, actionsActive);
            cleaner.start();
            
            setupLogger();
            scale.initialize(this);
            
            if(entityInfoMap.isEmpty())
                setUpEntityMap();
                
            APICallsObservable = new SimpleStringProperty(String.format("%d", coreInfo.getAPICalls()));
        }
        
        /**
         * Calls MyLogger.setup(). To setup the naming convention of the logger.
         */
        public void setupLogger(){
             try {
                MyLogger.setup();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error setting up log, IOException ", ex.getMessage());
            }
        }
        
        /**
         * Returns the Logger used.
         * @return 
         */
        public Logger getLogger(){
            return LOGGER;
        }
                
	
         /**
         * Returns JSON array of all actions for the selected entity.
         * Actions can return null if invalid.
         * @param entityID
         * @return 
         */
	public JSONArray getEntityActions(String entityID) {
            if(!loginInfo.isValid())
                return null;
            
            JSONArray actions = null;
            
            try {
                Pair<String, Integer> reply =
                        Entities.getActions(this, entityID, true);
                actions = new JSONArray(reply.getKey());
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Failed to get entityActions for entity {0}", entityID);
                //TODO: handel this
            }
            
            return actions;
	}
        
        
        
        /**
         * Returns a Map of all selected actions. Selected actions will appear 
         * in the left hand menu.
         * @return 
         */
        public Map getSelectedActions(){
            return actionsActive;
        }
        
        public void LogOut(){
            LOGGER.log(Level.INFO, "Logging out...");
            loginInfo.setValid(false);
            loginInfo.setToken("");
            loginInfo.setClientID("");
            loginInfo.setEmail("");
            
            actionsActive.forEach((k, v) -> {  
                try{
                    v.shutDown();
                }catch(Exception e){
                  LOGGER.log(Level.SEVERE, "Error shutting down ActionObject {0} for logout, Exception: {1}", 
                          new Object[]{k, e.getMessage()});
                }
            });
            
            actionsActive.clear();
            
            resetHTTPMap();
            LOGGER.log(Level.INFO, "Logging out complete.");
        }
        
        /**
         * Adds an observable list to be recalled later via the given ID.
         * @param id
         * @param list 
         */
        public void addObservableList(String id, ObservableList list){
            if(observList == null)
                observList = new HashMap(); //TODO remove once core field deleted 5/31
            if(observList.containsKey(id))
                observList.remove(id);
            observList.put(id, list);
        }
        
        /**
         * Returns the actionID of the given action name and entity ID. Will 
         * return null if the token is not valid or the action is not found.
         * @param entityID
         * @param actionName
         * @return 
         */
        public String getActionID(String entityID, String actionName){
            if(!loginInfo.isValid())
                return null;
            
            String actionID = null;
            JSONArray array = getEntityActions(entityID);
            JSONObject json;
            
            for (int j = 0; j < array.length(); j++) {
                json = array.getJSONObject(j);
                if(json.getString("name").equals(actionName)){
                   actionID =  json.getString("id");
                   break;
                }                     
            }
            
            return actionID;
        }
        
        /**
         * Returns the Observable list of the corresponding ID
         * @param id
         * @return 
         */
        public ObservableList getObservableList(String id){
            return observList.get(id);
        }
	
        /**
         * Sets the maxDataAge modifier. 1 = seconds 60 = Minutes 3600 = hours
         * @param maxDataAgeModifier
         */      
        public void setMaxDataModidier(int maxDataAgeModifier){
            coreInfo.setMaxDataAgeModifier(maxDataAgeModifier);
            coreInfo.save(this);
        }
        
        /**
         * Returns the maxDataAge modifier. 1 = seconds 60 = Minutes 3600 = hours
         * @return 
         */  
        public int getMaxDataModidier(){
            return coreInfo.getMaxDataAgeModifier();
        }
        
        /**
         * Returns the maxDataAge in seconds, this will not apply the modifier
         * @return 
         */
        public int getMaxDataAge(){
            return coreInfo.getMaxDataAge();
        }
        
        /**
         * Sets the max age of data in seconds.
         * @param maxDataAge 
         */
        public void setMaxDataAge(int maxDataAge){
            coreInfo.setMaxDataAge(maxDataAge);
            coreInfo.save(this);
        }
        
	/**
	 * Will check if there is already a list under that name, if so it will
         * replace it else it will add a new entry
	 * @param key
         * @param object
	 */
	
	public void addHTTPMap(String key, Object object){
            ArrayList<Object> array = new ArrayList<>(2);
            array.add(new Date());
            array.add(object);
            
            synchronized(HTTPmap){
                HTTPmap.put(key, array);
            }
	}
        
        /**
         * Saves the data in the HTTP map. This should not be called to often as 
         * data in the HTTPMap is not critical.
         */
        public void saveHTTPMap(){
            //Save core update
            Save.saveObject(HTTPmap, "HTTPmap", this);
        }
	
	/**
	 * Returns the object for the given key. If the key is not found it will return null
	 * @param key
	 * @return
	 */
	
	public Object getHTTPMap(String key){	
            ArrayList<Object> list =  HTTPmap.get(key);
            if(list != null)
		return list.get(1);
            else
                return null;
	}
	
	/**
	 * Returns null if list is not found
	 * @param key
	 * @return
	 */
	public Date getHTTPLastUpdate(String key){
                ArrayList<Object> array = HTTPmap.get(key);
		Date lastUpdate = null;
		
		if(array != null)
			lastUpdate = (Date) array.get(0);		
		
		return lastUpdate;
	}
	
	/**
	 * Removes old objects from HTTP maps. 
         * HTTPMap has objects removed that were added more then maxDateAge seconds ago.
         * HTTPInprog notifies and removes objects older then MAXCALLWAIT
	 */
	public void cleanHTTPMaps(){
            //Create a data that is the max age for creation
            if(coreInfo.getMaxDataAge() <= 0)
                return;
            
            Calendar calendar = Calendar.getInstance();
            
            switch (coreInfo.getMaxDataAgeModifier()) {
                case (3600):
                    calendar.add(Calendar.HOUR, -(coreInfo.getMaxDataAge()));
                    break;
                case (60):
                    calendar.add(Calendar.MINUTE, -(coreInfo.getMaxDataAge()));
                    break;
                default:
                    calendar.add(Calendar.SECOND, -(coreInfo.getMaxDataAge()));
                    break;
            }
            
            final Date date = calendar.getTime();
            
            LOGGER.log(Level.FINE, "Removing objects in Map added before {0}.", date);
            
            ArrayList<String> toRemoveMap = new ArrayList<>();
            synchronized(HTTPmap){
                HTTPmap.forEach((k, v) -> {
                    if(((Date) v.get(0)).before(date)){
                        LOGGER.log(Level.FINER, "Removed " + k + " from map; Creation date {0}", (Date) v.get(0));
                        toRemoveMap.add(k);
                    }
                });  

                LOGGER.log(Level.FINE, "Removed {0} objected added before {1}", new Object[] {toRemoveMap.size(), date});

                toRemoveMap.forEach((k) -> {
                    HTTPmap.remove(k);
                });

                calendar = Calendar.getInstance();
                calendar.add(Calendar.MILLISECOND, -(MAXCALLWAIT));
                final Date expire = calendar.getTime();

                //Save update
                Save.saveObject(HTTPmap, "HTTPmap", this);
            }
            
            //Clean 
            jlibs.core.lang.RuntimeUtil.gc(5);
	}
        
        /**
         * Clears all data from both HTTP maps.
         */
        public void resetHTTPMap(){
            HTTPmap.clear();
            
             //Save update
            Save.saveObject(HTTPmap, "HTTPmap", this);
        }

        /**
         * Returns the total number of API calls made.
         * @return total number of API calls made.
         */
        public int getAPICalls() {
            return coreInfo.getAPICalls();
        }
        
        /**
         * Increments the total number of API calls made and returns the new number.
         * @return the new incremented number of API calls.
         */
        public int incAPICalls(){
            if(APICallsObservable == null)
               APICallsObservable = new SimpleStringProperty(String.format("%d", coreInfo.getAPICalls()));

            Platform.runLater(() -> {
                APICallsObservable.set(String.format("%d", coreInfo.incAPICalls()));
            });

            return coreInfo.getAPICalls();
        }
        
        public StringProperty getAPICallsObservable(){
            return APICallsObservable;
        }
        
        
	
        /**
         * Returns the client ID used to sign in with. Will return null if not 
         * signed in.
         * @return 
         */
	public String getClientId() {
		return loginInfo.getClientID();
	}
	
        /**
         * Sets the given pin as the security pin if the given pin is a positive 
         * number with at least 4 digits.
         * @param pin 
         */
	public void setPin(int pin) {
            if(pin >= 1000)
		loginInfo.setPin(pin);
	}
	
        /**
         * Returns the security pin, this will be -1 if not set.
         * @return 
         */
	public int getPin() {
		return loginInfo.getPin();
	}
	
        /**
         * Returns the token, may return null if not logged in.
         * @return 
         */
	public String getToken() {
		return loginInfo.getToken();
	}

        /**
         * Returns the email of the logged in user, may return null if not
         * signed in
         * @return 
         */
	public String getEmail() {
		return loginInfo.getEmail();
	}
        /**
         * Takes the name of the action, finds it's ID, creates an actionObject
         * @param action
         */
	public void addAction(ActionObject action) {            
            if(!loginInfo.isValid())
                return;
            //Remove any duplicates
            removeAction(action.getActionName());
            
            //Add to map
            actionsActive.put(action.getActionName(), action);
            Save.saveObject(actionsActive, "actionsActive", this);
	}
        
        /**
         * Removes the actionName from the active action list.
         * @param actionName 
         */
        @SuppressWarnings("empty-statement")
        public void removeAction(String actionName){
            while(actionsActive.remove(actionName) != null){
                LOGGER.log(Level.INFO, "[{0}] action deactivated.", actionName);
            };
            Save.saveObject(actionsActive, "actionsActive", this);
        }
        
        /**
         * Returns the name of the currently selected entity
         * @param entityName
         * @return 
         */
        public EntityInfo getEntityByName(String entityName) {
            Set<Entry<String, EntityInfo>> list = entityInfoMap.entrySet();
            Iterator<Entry<String,EntityInfo>> inter = list.iterator();
            Entry<String,EntityInfo> e;

            while(inter.hasNext()){
                e = inter.next();
                if(entityName.compareToIgnoreCase(e.getValue().name) == 0)
                    return e.getValue();
            }
          
            return null;
	}
        
        /**
         * Returns the Entity Info of the given entity ID.
         * @param entityID Id of the entity to return
         * @return EntityInfo the the given entity
         */
        public EntityInfo getEntityByID(String entityID) {
            return entityInfoMap.get(entityID);
	}
        
	
        /**
         * Returns true if the core has a valid token
         * @return 
         */
	public boolean isValid() {
		return loginInfo.isValid();
	}
	
        /**
         * entities can return null if not set or invalid.
         * @return 
         */
	public Map<String, EntityInfo> getEntitieMap() {
           return entityInfoMap;
	}
        
        /**
         * Fetches entities from Asset Panda
         */
        private void setUpEntityMap(){
            if(!loginInfo.isValid())
                return;
            
            JSONArray array;
            try {
                Pair<String, Integer> pair = Entities.getEntites(this);
                if(pair.getValue() == 200){
                    array = new JSONArray(pair.getKey());
                }else{
                    LOGGER.severe("Getting entites failed: Probably incorrect token."); 
                    //TODO Handel this
                    return;
                }
            
                //Create all EntityInfos
                EntityInfo info;
                JSONObject json;
                entityInfoMap.clear();
                for(int i = 0; i < array.length(); i++){
                    json = array.getJSONObject(i);
                    info = entityInfoMap.get(json.getString("id"));
                    if(info == null){
                        info = new EntityInfo(json.getString("id"), this);
                        entityInfoMap.put(info.id, info);
                    }else{
                        info.update(this);
                    }
                }
            
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Getting entites failed: IOException: {0}", ex.getMessage());
                //TODO handle this
            }
            
        }
	
        /**
         * Sets the given action ID to the checkout action
         * @param checkoutAction 
         */
	public void setCheckoutAction(int checkoutAction) {
		coreInfo.setCheckoutAction(checkoutAction);
             //Save core update
                coreInfo.save(this);
	}
	
	public int getCheckoutAction() {
		return coreInfo.getCheckoutAction();
	}
        
        /**
         * Returns a string array of all status names of the given entity
         * @param entityID
         * @return 
         */
        public String[] getStatusNames(String entityID){
            String[] names = (String[]) getHTTPMap("statuses?entity_id=" + entityID + "N");
            
            if(names == null){
                getStatus(entityID);            
                names = (String[]) getHTTPMap("statuses?entity_id=" + entityID + "N");
            }
           
            return names;
        }
        
        /**
         * Returns an array of strings of the status ID of the given entity
         * @param entityID
         * @return 
         */
        public String[] getStatusID(String entityID){
            String[] ids = (String[]) getHTTPMap("statuses?entity_id=" + entityID + "I");
            
            if(ids == null){
                getStatus(entityID);            
                ids = (String[]) getHTTPMap("statuses?entity_id=" + entityID + "I");
            }
           
            return ids;
        }

        public AppController getController() {
            return controller;
        }
        
        /**
         * Returns the GPS location based on IP address.
         * Pair is returned Lat, Long
         * @return 
         */
        public Pair<Double, Double> getGPS(){
            //http://ip-api.com/json/
            if(gps == null){
                Pair<String, Integer> reply;
                try {
                    reply = GET.getPublic("http://ip-api.com/json/", this);                    
                    JSONObject json = new JSONObject(reply.getKey());  
                    gps = new Pair (json.getDouble("lat"), json.getDouble("lon"));
                } catch (IOException ex) {
                    //TODO handel errors better
                    gps = new Pair(50,50);
                    LOGGER.log(Level.SEVERE, "Failed to get GPS location, IOException: {0}", ex.getMessage());
                    LOGGER.log(Level.INFO, "Using generic GPS location 50 50");
                }
            }
            return gps;
        }
        
        private void getStatus(String entityID){
            try {
                //Get array of statuses
                Pair<String, Integer> reply = Entities.getActions(this, entityID, true);
                JSONArray array = new JSONArray(reply.getKey());
                JSONObject json;
                
                //Create arrays for names and IDs
                String[] names = new String[array.length()];
                String[] ids = new String[array.length()];
                
                //Add each name and ID to arrays
                for(int i = 0; i < array.length(); i++){
                    json = array.getJSONObject(i);
                    names[i] = json.getString("name");
                    ids[i] = json.getString("id");
                }
                
                //Add them to core list
                addHTTPMap("statuses?entity_id=" + entityID + "N", names);
                addHTTPMap("statuses?entity_id=" + entityID + "I", ids);
            } catch (IOException ex) {
                //TODO handle errors
                LOGGER.log(Level.SEVERE, "Could not retrieve statuses for entityID[{0}], IOException: {1}", new Object[]{entityID, ex.getMessage()});
            }
        }
        
        /**
         * Returns a label with the current date or time. Will update with current
         * time.
         * @return 
         */
	public Label getDateTimeStamp(){
            Label dateTimeStamp = new Label();
            
            //If the current time has not been set up, create it and it's task
            if(currentTime == null){
                currentTime = new SimpleStringProperty("0");
                // Background Task
                    Task task = new Task() {
                        @Override
                        protected Integer call() throws Exception {
                            boolean interupted = false;
                            int i = 0;
                            while(!interupted){
                                i++;
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ie) {
                                    break;
                                }
                                
                                if(Thread.currentThread().isInterrupted()){
                                    break;
                                }

                                // Update the GUI on the JavaFX Application Thread
                                Platform.runLater(() -> {
                                    DateFormat timeFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
                                    currentTime.setValue(timeFormat.format(new Date()));
                                });

                            }//end while
                            return null;
                        }//End Call
                     };//End task                    

                    timeThread = new Thread(task);
                    timeThread.setDaemon(true);
                    timeThread.setName("Current DateTimeStamp Updater");
                    timeThread.start(); 
            }//end if currentTime == null

            dateTimeStamp.textProperty().bind(currentTime);
            dateTimeStamp.getStyleClass().add("actionText");
		
            return dateTimeStamp;
	}
	
        /**
         * Returns a label with the current date MM/dd/yyyy format, will auto
         * update if the date changes
         * @return 
         */
	public Label getDateStamp(){
		
             Label dateStamp = new Label();
             
             if(currentDate == null){
                currentDate = new SimpleStringProperty("0");

                   Task task = new Task() {
                       @Override
                       protected Integer call() throws Exception {

                           boolean interupted = false;
                            int i = 0;
                            while(!interupted){
                                i++;
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ie) {
                                    break;
                                }
                                
                                if(Thread.currentThread().isInterrupted()){
                                    break;
                                }

                               // Update the GUI on the JavaFX Application Thread
                               Platform.runLater(() -> {
                                   DateFormat timeFormat = new SimpleDateFormat("MM/dd/yyyy");
                                   currentDate.setValue(timeFormat.format(new Date()));
                               });

                           }//end while
                           return null;
                       }//End Call
                    };//End task                    

                   dateThread = new Thread(task);
                   dateThread.setDaemon(true);
                   dateThread.setName("Current DateStamp Updater");
                   dateThread.start(); 
             }//end if currentDate == null

            dateStamp.textProperty().bind(currentDate);  
            dateStamp.getStyleClass().add("actionText");

            return dateStamp;
            //return getDateTimeStamp();
	}
        
        /**
         * Gets the given entity object ID returned from the search. If there are 
         * more than one or no objects returned from search will return an empty string.
         * 
         * @param entityID
         * @param barcode
         * @return 
         */
        public String getEntityObjectID(String entityID, String barcode){
            //Check if the ID is already known
            
            //If not, try to get the id
            String objectID; 
            Pair<String, Integer> reply = new Pair<>("null",-1);
            try {
                //Make the call
                reply = Entities.getEntityObjectID(this, barcode, entityID, true);
                JSONObject json = new JSONObject(reply.getKey());

                //Get the number of objects returned
                int number = json.getJSONObject("totals").getInt("objects");                    
                if(number != 1)
                    return "";

                //Get the id and save it for later use
                JSONArray array = json.getJSONArray("objects");
                json = array.getJSONObject(0);
                objectID = json.getString("id");

            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Could not search for barcode[{0}] in entityID[{1}, IOException: {2}", new Object[]{barcode, entityID, ex.getMessage()});
                objectID = null;
            }catch (JSONException e) {
                LOGGER.log(Level.SEVERE, "Could not search for barcode[{0}] in entityID[{1}, JSONException: {2} from: {3}", new Object[]{barcode, entityID, e.getMessage(), reply.getKey()});
                objectID = null;
            }
            
            
            return objectID;
        }
        
        /**
         * Sends the post request to Asset panda. If valid it will update all info.
         * 
         * @param userName
         * @param password
         * @param secretID
         * @param clientID
         * @return String with details on login attempt
         */
        public String login(String userName, String password, String secretID, String clientID){
            
            //Send data for post call
            LOGGER.info("Logging in...");
            String tokenString = String.format("client_id=%s&client_secret=%s&email=%s&password=%s&device=Checkout App&app_version=%s", 
						clientID, secretID, userName,password, VERSION);
            
            	
	    Pair<JSONObject, Integer> reply;
            try {
                reply = POST.executePost("https://login.assetpanda.com/v2/session/token", tokenString, this);
            } catch (IOException ex) {
                Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
                return "Login failed IOException";
            }
            
            if(reply.getValue() == 200){
               loginInfo.setValid(true);
               
               //Save Info for use later
               loginInfo.setClientID(clientID);
               loginInfo.setEmail(userName);
               loginInfo.setToken(reply.getKey().getString("access_token"));
            }                
            else
                loginInfo.setValid(false);
            
            //Save core update
            loginInfo.save(this);
            
            if(loginInfo.isValid()){
                LOGGER.log(Level.INFO, "Login successful.");
                return "Success";
            }else{
                
                if(reply.getValue() >= 500){   
                    LOGGER.log(Level.SEVERE, "Logging failed. Code: {0}.", reply.getValue());
                    return"Assat Panda server error - This could be caused by inccorect client ID and/ or client secret.";
                }
                
                JSONObject error = (JSONObject) reply.getKey();
                String base = error.getJSONArray("errors").getJSONObject(0).getJSONArray("base").getString(0);
                
                LOGGER.log(Level.SEVERE, "Logging failed. Code: {0} Base:{1}.", 
                        new Object[]{reply.getValue(), base});
                
                return base;
            }
        }

    public void shutDown() {
        LOGGER.log(Level.INFO, "Core shutdown initiated.");
        
        if(cleaner != null)
            cleaner.interrupt();
        if(dateThread != null)
            dateThread.interrupt();
        if(timeThread != null)
            timeThread.interrupt();
        
        actionsActive.forEach((k, v) -> {  
            try{
               if(v != null){
                   v.shutDown();
                   
               }
            }catch(Exception e){
              LOGGER.log(Level.SEVERE, "Error interrupting thread "
                      + "ActionObject {0}, Exception: {1}", new Object[]{k, e.getMessage()});
            }
        });
        
        LOGGER.log(Level.INFO, "Core shutdown complete.");
        System.exit(1);
    }

    /**
     * Returns a GUIScale, that holds all the sizes for elements.
     * @return 
     */
    public GUIScale getScale() {
        return scale;
    }
    
    /**
     * Returns one of the 5 cats in an image view.
     * Cat 1: 30%
     * Cat 2: 30%
     * Cat 3:  5%
     * Cat 4: 15%
     * Cat 5: 20%
     * @return 
     */
    
    public Image getCat(){
        
        //Choose a random number 0-99
         Random rand = new Random(); 
         int randInt = rand.nextInt(99);
         String catFileName;
         
         //Find which cat is to be selected
         if(randInt < 30){
            catFileName = "cat1.gif";
         } else if (randInt <60){
            catFileName = "cat2.gif"; 
         } else if (randInt < 80){
            catFileName = "cat5.gif"; 
         } else if (randInt < 95){
            catFileName = "cat4.gif";
         } else {
            catFileName = "cat3.gif";
         }
        
         // Send the image         
        return new Image("images/cats/" + catFileName);
    }
    
    /**
     * Saves the active action list.
     */
    public void saveActionsActive(){
        Save.saveObject(actionsActive, "actionsActive", this);
    }
    
    /**
     * Saves the gui settings
     */
    public void saveGUISettings(){
        Save.saveObject(scale, "GUIScale", this);
    }
    
    /**
     * Saves all entity info fields
     */
    public void saveEntityInfo(){
        Save.saveObject(entityInfoMap, "entityInfoMap", this);
    }
    
    /**
     * Updates all entityInfos
     */
    public void updateEntityInfo(){
                   
        if(entityInfoMap.isEmpty()){
            setUpEntityMap();
            return;
        }
        
        entityInfoMap.forEach( (k,v) -> {
            try {
                v.update(this);
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, "IOException updating Entity [{0}]: {1}",
                        new Object[] {v.name, ex.getMessage()});
            }catch (NullPointerException ex){
                getLogger().log(Level.WARNING, "Error trying to update NULL entity.");
            }
        });
        saveEntityInfo();
    }

    public void setStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
    
    public Stage getStage(){
        return primaryStage;
    }
}
