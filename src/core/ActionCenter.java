package core;

import dataStructures.actions.ActionColumnInfo;
import dataStructures.actions.ActionInfo;
import dataStructures.ComboBoxAutoComplete;
import dataStructures.actions.ActionObjectEntite;
import helperClasses.CheckoutStatus;
import javafx.util.Pair;
import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import http.Actions;

import http.Entities;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import org.controlsfx.control.CheckComboBox;
import org.json.JSONStringer;
import org.json.JSONWriter;

/**
 *
 * @author Quinten Holmes
 */
public class ActionCenter{
    private final Core core;
    private ArrayList<objStrut> elements;
    private GridPane actionGrid;
    private ActionObject actionObj;
    private ActionInfo actInfo;



    @SuppressWarnings("unchecked")
    public ActionCenter(Core core, ActionInfo actInfo, ActionObject actionObj) throws IOException {
            this.core = core;
            this.actInfo = actInfo;
            this.actionObj = actionObj;

        core.getLogger().log(Level.INFO, "Action Center creation started for Action Name-[{0}]", actInfo.actionName);

        //Get list of actions
        //Form url, check if the core already has the request, if not send HTTP request
        core.getLogger().log(Level.INFO, "Action Center for [{0}] creating rows...", actInfo.actionName);

        JSONObject action = new JSONObject(actInfo.actionJSONString);
        JSONArray array = action.getJSONArray("fields");

        //Set-up vars for creating layout
        elements = new ArrayList<>(array.length());

        //Create grid
        actionGrid = new GridPane();
          actionGrid.setVgap(10);
          actionGrid.setHgap(20);
          actionGrid.setPadding(new Insets(10, 10, 10, 10));

        try{
        ArrayList<ActionColumnInfo> actionOrder = actInfo.getActionOrderColumns();
        for(int i = 0; i < actionOrder.size() && !Thread.currentThread().isInterrupted(); i++) {
            //skip reference fields
            if(actionOrder.get(i).isReference)
                continue;

            //Get action struct                                           
            elements.add(getRow(actionOrder.get(i)));
        }  
        }catch (Exception e){
            e.printStackTrace();
        }

        core.getLogger().log(Level.INFO, "All Action Center rows created successfully for action-[{0}]",
                actInfo.actionName);

    }

    /**
     * All entity depended action fields will be updated before returning the grid
     * @param entityObjectID Of the object to build dependency for
     * @param rtAction true if this should show fields for return, else will only
     * show non-return fields.
     */
    private void dependencyBuild(String entityObjectID, boolean rtAction){

        JSONObject json;
        JSONArray array;
        Pair<String, Integer> pair = null;
        for(int i = 0; i < elements.size(); i++){
            if(elements.get(i).isDependent() && elements.get(i).isReturnField() == rtAction){
                json = elements.get(i).getJson();
                switch(elements.get(i).getType()) {     
                    case ("DisplayValueField"): 
                        try {
                            //Get field key
                            String key = json.getJSONObject("extra_details")
                                    .getJSONObject("entity_field")
                                    .getString("key");

                            //Get field type
                            pair = Entities.getEntiteInfo(core, actInfo.entityID);  
                            array = new JSONObject(pair.getKey()).getJSONArray("fields");

                            for(int j = 0; j < array.length(); j++){
                                json = array.getJSONObject(j);
                                if(json.getString("key").equals(key))
                                    break;
                            }

                            //Get entityObject info
                            String type = json.getString("type");                                
                            pair = Entities.getEntityObject(core, entityObjectID, false);

                            json = new JSONObject(pair.getKey());

                            //If null leave empty else get value
                            String info;
                            if(json.get(key) != null){
                                if(type.equals("EntityListField"))
                                    info = json.getJSONObject(key).getString("display_name");
                                else
                                    info = json.getString(key);
                            }else{
                                 info = "[Blank]";
                            }

                            //Update text field
                            ((Label) elements.get(i).getControl()).setText(info);
                        } catch (IOException ex) {
                            //TODO log
                            core.getLogger().log(Level.SEVERE, "Error creationg ActionCenter for actInfo.actionName-[{0}] IOExceprion-{1}",
                                new Object[]{actInfo.actionName, ex.getMessage()});
                            ((Label) elements.get(i).getControl()).setText("[Error fetching information]");
                        } catch (JSONException e){

                            if(pair != null){
                                core.getLogger().log(Level.SEVERE, "Error creationg ActionCenter for actInfo.actionName-[{0}] JSONException-{1}: {2} ",
                                    new Object[]{actInfo.actionName, e.getMessage(), pair.getKey()});
                            }
                            else{
                                core.getLogger().log(Level.SEVERE, "Error creationg ActionCenter for actInfo.actionName-[{0}] JSONException-{1}",
                                    new Object[]{actInfo.actionName, e.getMessage()});
                            }

                            ((Label) elements.get(i).getControl()).setText("[Blank]");
                        }                    
                        break;
                    default:                                      
            }//End switch
            }//End if dependednt
        }//End forloop
    }

    /**
     * Returns the grid pane with all controls for the return. The ActionObjectEntitie
     * is required for any editing related grids, but can be null in all other cases.
     * @param entityObjectID The ID of the object that the grid is for
     * @param actionEntity  Required for editing
     * @param status The type of grid that should be returned
     * @return 
     */
    public GridPane getActionGrid(String entityObjectID, ActionObjectEntite actionEntity, CheckoutStatus status){
        
        actionGrid = new GridPane();
              actionGrid.setVgap(10);
              actionGrid.setHgap(20);
              actionGrid.setPadding(new Insets(10, 10, 10, 10));

        if(elements.isEmpty())
            return actionGrid;

        switch(status){
            case NEW:
                newAction();
                dependencyBuild(entityObjectID, false);
                break;
            case RETURN:
                returnAction();
                dependencyBuild(entityObjectID, true);
                break;
            case EDIT:
                editAction(actionEntity);
                dependencyBuild(entityObjectID, false);
                break;
            case EDITRETURNED:
                editAction(actionEntity);
                dependencyBuild(entityObjectID, false);
                dependencyBuild(entityObjectID, true);
                break;
            default:

        }
            
        return actionGrid;
    }

    /**
     * Gives the return name if one is set, if it does not change it will be
     * the same as the name. If the action is not returnable this will be blank.
     * @return 
     */
    public String getReturnName(){
        return actInfo.returnName;
    }

    /**
     * Returns the name of the action.
     * @return 
     */
    public String getActionName(){
        return actInfo.actionName;
    }

    /**
     * Returns the action's ID.
     * @return 
     */
    public String getID(){
        return actInfo.actionID;
    }

    /**
     * Returns the ID of the entity this action belongs to.
     * @return 
     */
    public String getEntityID(){
        return actInfo.entityID;
    }

    /**
     * Returns true if this action has returnable fields, else returns false.
     * @return 
     */

    public boolean isReturnable(){
        return actInfo.isReturnable;
    }

    /**
     * Builds the objStrut for the given actionField
     * @param json
     * @return
     * @throws IOException 
     */
    @SuppressWarnings("unchecked")
    private objStrut getRow(ActionColumnInfo colInfo) throws IOException {
            //create new row and add json file and requirement boolean
            //other vars                

            objStrut strut;
            Control control;

            ObservableList<String> options;

            //create label, a * is added at the end if it is required for user benifit
                String labelName;  
                if(colInfo.isRequired)
                        labelName = String.format("%s*", colInfo.name);
                else
                        labelName = colInfo.name;

                Label label = new Label(labelName);	
                label.getStyleClass().add("actionLabel");

            core.getLogger().log(Level.FINE, "Creating ActionCenter Row ActionName-[{0}] RowName-[{1}] RowType-[{2}]",
                    new Object[]{actInfo.actionName, colInfo.name, colInfo.type});

            //Get the type of the action and get the correct component
            switch(colInfo.type) {

            case ("NumericField"): 
                    control = new TextField();
                    control.getStyleClass().add("actionControls");

                    int maxDec = 4;
                    String delm = ",";
                    ((TextField) control).textProperty().addListener(
                        (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                           String s = newValue;
                            s = s.replaceAll("[^.\\d]", "");  

                            int lastIndex = s.lastIndexOf('.');
                            int firstIndex = s.indexOf('.');

                            boolean hasDec = false;
                            boolean decAtEnd = false;

                            if(lastIndex == firstIndex && lastIndex >= 0){
                                hasDec = true;
                            }else if(lastIndex != firstIndex){
                                hasDec = true;
                                s = oldValue;
                            }

                            /*if(hasDec)
                                if((s.length() - 1) - firstIndex >= maxDec && 0 <= maxDec)
                                    s = s.substring(0, s.length() - firstIndex);
                                */
                            if(s.charAt(s.length() - 1) == '.')
                                decAtEnd = true;

                            if(!s.isEmpty()){
                                NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
                                nf.setMaximumFractionDigits(maxDec);
                                double number = Double.parseDouble(s);
                                s = nf.format(number);
                                s = s.replaceAll(",", delm);
                            }

                            if(decAtEnd)
                                s += ".";

                            ((TextField) control).setText(s);                                 
                        });

                    strut = new objStrut(colInfo, label, control, false); 
                    break;
            case ("PhoneField"):
                    control = new TextField();
                    control.getStyleClass().add("actionControls");

                    //Set it so only numbers can be entered and a max of 15 digits
                    ((TextField) control).textProperty().addListener(
                            (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                        if (!newValue.matches("\\d*")) {
                            ((TextField) control).setText(newValue.replaceAll("[^\\d]", ""));
                        }

                        if (((TextField) control).getText().length() > 15) {
                            String s = ((TextField) control).getText().substring(0, 15);
                            ((TextField) control).setText(s);
                        }
                    });
                    strut = new objStrut(colInfo, label, control, false); 
                    break;
            case ("EntityListField"):
            case ("MultipleEntityListField"):
                    //Moved this to a different function to keep this cleaner
                    strut = entityListField(colInfo, label);	
                    break;			
            case ("UrlField"):
            case ("EmailField"):
            case ("TextField"):
            case ("AddressField"):
                    control = new TextField();
                        ((TextField)control).setPrefWidth(200);
                    control.getStyleClass().add("actionControls");

                    strut = new objStrut(colInfo, label, control, false);
                    break;

            case ("MemoField"):
                    control = new TextField();
                        ((TextField)control).setPrefWidth(200);
                    control.getStyleClass().add("actionControls");

                    strut = new objStrut(colInfo, label, control, false);
                    break;

            case ("DateField"):
                    DatePicker datepick = new DatePicker(LocalDate.now());
                    control = datepick;
                    control.getStyleClass().add("actionCalander");

                    strut = new objStrut(colInfo, label, control, false);
                    break;
            case ("DateStampField"):
                    control = core.getDateStamp();
                    strut = new objStrut(colInfo, label, control, false);
                    break;
            case ("DateTimeStampField"):
                    control = core.getDateTimeStamp(); 
                    strut = new objStrut(colInfo, label, control, false);
                    break;             
            case ("StatusField"):
                    control = new ComboBox();
                    control.getStyleClass().add("actionControls");
                    options =  FXCollections.observableArrayList(core.getStatusNames(actInfo.entityID));
                    ((ComboBox) control).setItems(options);
                    ((ComboBox) control).setValue(colInfo.defaultValue);

                    strut = new objStrut(colInfo, label, control, false, core.getStatusNames(actInfo.entityID), core.getStatusID(actInfo.entityID));
                    break;
            case ("YesNoField"):
            case ("ListField"):
                    control = new ComboBox();
                    control.getStyleClass().add("actionControls");

                    //Get options and add to ComboBox
                    options =  FXCollections.observableArrayList();
                    for(int i = 0; i < colInfo.listOptions.length; i++){
                        options.add(colInfo.listOptions[i]);
                    }	

                    ((ComboBox) control).setItems(options);

                    //Set default
                    ((ComboBox) control).setValue(colInfo.defaultValue);

                    strut = new objStrut(colInfo, label, control, false, colInfo.listOptions);
                    break;
            case ("DateCalculationField"):
                    control = new Label("");
                    strut = new objStrut(colInfo, label, control, false);
                    break;
            case ("DisplayValueField"): 
                    control = new Label("");
                    control.getStyleClass().add("actionText");
                    strut = new objStrut(colInfo, label, control, true);
                    break;
            case ("MultipleListField"): //TODO add this: Has default
            case ("CurrencyField"):
            case ("NumericAutoIncrementField"):       
            case ("SignatureField"): //TODO add this
            case ("TimeField"): //TODO add this
            case ("ApUserField"):
            case ("UserStampField"): //TODO add this
            case ("TimeZoneField"):


            default:
                    control = new Label("Type [" + colInfo.type + "] not supported"); 
                    strut = new objStrut(colInfo, label, control, false);
            }

            return strut;
    }

    /**
     * Takes a JSONArray of entity ids and get the name for them
     * 
     * @param array
     * @return
     */
    private String[] arrayToName(JSONArray array){
            String[] names = new String[array.length()];

            for(int i =0; i < array.length(); i++){
                    names[i] = array.getJSONObject(i).getString("name");
            }

            return names;				
    }

    /**
     * Takes a JSONArray of entity ids turns it into a String[]
     * 
     * @param array
     * @return
     */
    private String[] arrayToID(JSONArray array){
            String[] IDs = new String[array.length()];

            for(int i =0; i < array.length(); i++){
                    IDs[i] =  array.getJSONObject(i).getString("id");
            }

            return IDs;				
    }

    private void newAction(){
        int field = 0;
        for(objStrut ob: elements ){
            if(ob.isOpen()){
                int index = field;
                Platform.runLater(() -> {
                    actionGrid.add(ob.getLabel(),0,index);
                    actionGrid.add(ob.getControl(),1,index);
                });
                field++;
            }
        }
    }
    
    private void returnAction(){
        int field = 0;
        for(objStrut ob: elements )
            if(ob.isReturnField()){
                int index = field;
                Platform.runLater(() -> {
                    actionGrid.add(ob.getLabel(),0,index);
                    actionGrid.add(ob.getControl(),1,index);
                });
                field++;
            }
    }
    
    private void editAction(ActionObjectEntite action){
        int field = 0;
        Control c;
        for(objStrut o: elements){
            if((!action.isReturned() && o.isReturnField()) || o.isDependent())
                continue;
            
            
            switch(o.getType()) {  
            case ("UrlField"):
            case ("EmailField"): 
            case ("PhoneField"):
            case ("TextField"):
            case ("MemoField"):
            case ("NumericField"): 
                Platform.runLater(() -> ((TextField) o.getControl()).setText(action.getKeyField(o.getKey())));
                c = o.getControl();
                break;
            case ("DateField"):
                Date date1;
                try {
                    date1 = new SimpleDateFormat("yyyy-mm-dd").parse(action.getKeyField(o.getKey()));
                } catch (ParseException ex) {
                    date1 = new Date();
                    Logger.getLogger(ActionCenter.class.getName()).log(Level.SEVERE, null, ex);
                }
                Date date2 = date1;
                Platform.runLater(() -> ((DatePicker)o.getControl()).setValue(date2.toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate()));
                
                c = o.getControl();
                break;//This will be filled in for the user
            case ("DateStampField"):
            case ("DateTimeStampField"):
                Label lb = new Label(action.getKeyField(o.getKey()));
                lb.getStyleClass().add("actionText");
                c = lb;
                 break;
            case ("EntityListField"):
            case ("ListField"):
            case ("StatusField"):
            case ("MultipleEntityListField"):
            case ("MultipleListField"):
                Platform.runLater(() -> ((ComboBox) o.getControl()).setValue(action.getKeyField(o.getKey())));
                
                c = o.getControl();
                break;
            case ("DateCalculationField"):
            default:
                c = new Label("Type [" + o.getType() + "] not supported"); 
            }
            
            int index = field;
            Control col = c;
            Platform.runLater(() -> {
                
                actionGrid.add(o.getLabel(),0,index);
                actionGrid.add(col,1,index);
                
                //Disable field if needed
                if(action.isReturned()){
                    if(!o.isEditableReturn())
                        o.getControl().setDisable(true);
                }else{
                    if(!o.isEditable())
                        o.getControl().setDisable(true);
                }
            });
            field++;
            
            
        }
    }

    /**
     * Gets all entities that could be selected and adds them to a comboBox.
     * Handles both normal and "MultipleEntityListField" based on String type.
     * @param json
     * @param label
     * @param type
     * @param isRequired
     * @param rtField
     * @return
     * @throws JSONException
     * @throws IOException 
     */
    @SuppressWarnings("unchecked")
    private objStrut entityListField(ActionColumnInfo colInfo, Label label) throws JSONException, IOException{

            JSONArray array = new JSONObject(colInfo.JSONString).getJSONArray("filtered_list_ids");


            String[] names;
            String[] ids;

            //Checks if only a list of entities appear 
            if(array.length() <= 0){ 

                    //get names and ids of all assets in the given entity
                    Pair<String[], String[]> info = core.getEntityByID(colInfo.sourceEntityID)
                            .getDisplay(core, true);
                    names = info.getValue();
                    ids = info.getKey();

                    if(names != null){			

                            //Check if assets need to be removed
                            if(colInfo.isFiltered){
                                    //Need to figure out what I do with this

                            }

                    }else //End if list != null
                            throw new IOException("Error pulling asset information from id: " + colInfo.sourceEntityID);

            }else{ 
                    //If there is only a selection of entities to choose from...			

                    //get name and ID lists
                    names = arrayToName(array);
                    ids = arrayToID(array);

                    //create the component		

            }//End if show_on_all_filter_entity_objects

            Control cmb;
            if(colInfo.type.equalsIgnoreCase("MultipleEntityListField")){
                cmb = new CheckComboBox<>();
                ((CheckComboBox) cmb).getItems().addAll((Object[]) names);
            }else{
               cmb = new ComboBox<>();
                    cmb.setTooltip(new Tooltip());
                    ((ComboBox) cmb).getItems().addAll((Object[]) names);
                ComboBoxAutoComplete temp = new ComboBoxAutoComplete<>((ComboBox) cmb);
            }

            cmb.getStyleClass().add("actionControls");

            return new objStrut(colInfo, label, cmb, false, names, ids);		
    }
    
    public Pair<String,Integer> updateAction(String entityObjectID, String actionID, ActionObjectEntite action){
         boolean success = true;
        Pair<String,Integer> pair;
        boolean rtAction;

        //Check if all required field are filled and Url + Email fields are valid
        core.getLogger().log(Level.INFO, "Checking filled fields to edit action ActionName-{0}", actInfo.actionName);
        for(objStrut o: elements){
            //Skip any fields that are not shown
            if((!action.isReturned() && o.isReturnField()) || o.isDependent())
                continue;
            
            //Only need to check fields that are being returned
            if(!(o.isEditable() || o.isEditableReturn()))
                continue;
            
            //Only need to check required fields and validate some types
            if(o.isRequired()
                    || o.getType().equals("EmailField") 
                    || o.getType().equals("UrlField")){

                switch(o.getType()) {      
                    case("UrlField"):
                        if(!(((TextField) o.getControl()).getText().isEmpty() && o.isRequired())){
                            if(!((TextField) o.getControl()).getText().isEmpty()){
                                try {
                                    new URL(((TextField) o.getControl()).getText()).toURI();
                                    o.getControl().setStyle("-fx-border-color: white");
                                }catch (MalformedURLException | URISyntaxException e) {
                                    success = false;
                                    o.getControl().setStyle("-fx-border-color: red");
                                }
                            }else{
                                o.getControl().setStyle("-fx-border-color: white");
                            }
                        }else{
                            success = false;
                            o.getControl().setStyle("-fx-border-color: red");
                        }
                        break;
                    case("EmailField"):                         
                        if(!(((TextField) o.getControl()).getText().isEmpty() && o.isRequired())){
                            if(((TextField) o.getControl()).getText().isEmpty()
                                || isValidEmail(((TextField) o.getControl()).getText())){
                                o.getControl().setStyle("-fx-border-color: white");
                            }else{
                                success = false;
                                o.getControl().setStyle("-fx-border-color: red");
                            }
                        }else{
                            success = false;
                            o.getControl().setStyle("-fx-border-color: red");
                        }

                        break;
                    case ("PhoneField"):
                    case ("TextField"):
                    case ("MemoField"):
                    case ("NumericField"): 
                        if(((TextField) o.getControl()).getText().isEmpty()){
                            success = false;
                            o.getControl().setStyle("-fx-border-color: red");
                        }else{
                            o.getControl().setStyle("-fx-border-color: white");
                        }
                         break;
                    case ("DateCalculationField"):
                    case ("DateField"):
                    case ("DateStampField"):
                    case ("DateTimeStampField"):
                         break;//This will be filled in for the user
                    case ("EntityListField"):
                    case ("ListField"):
                    case ("StatusField"):
                        String box;
                        if(((ComboBox) o.getControl()).getValue() == null)
                           box = null;
                        else
                            box = ((ComboBox) o.getControl()).getValue().toString();

                        System.out.printf("%s: %s %n", o.getLabel().getText(), box);
                        if(box == null || box.isEmpty()){
                            success = false;
                            o.getControl().setStyle("-fx-border-color: red");

                        }else{
                            o.getControl().setStyle("-fx-border-color: white");
                        }
                        break;
                    case ("MultipleEntityListField"):
                    case ("MultipleListField"):
                        if(((CheckComboBox) o.getControl()).getCheckModel().isEmpty()){
                            success = false;
                            o.getControl().setStyle("-fx-border-color: red");
                        }else{
                            o.getControl().setStyle("-fx-border-color: white");
                        }
                        break;
                    default:

                    }
            }
        }//End check of required fields

        if(success){
            JSONWriter writer = new JSONStringer().object();
            String key;
            String value;
            String[] names;
            ObservableList<String> options;
            ObservableList<Integer> indices;

            core.getLogger().log(Level.INFO, "Edit action has fields filled "
                    + "correctly, sending to Asset Panda ActionName-[{0}] EntityObjectID-[{1}]", new Object[] {actInfo.actionName, entityObjectID});

            writer.key("action_fields").object();
            for(objStrut o: elements){
                //Skip any fields that are not shown
                if((!action.isReturned() && o.isReturnField()) || o.isDependent())
                    continue;

                //Only need to check fields that are being returned
                if(!(o.isEditable() || o.isEditableReturn()))
                    continue;
                
                key = o.getJson().getString("key");
                switch(o.getType()) {
                    case("UrlField"):
                    case("EmailField"):
                    case ("TextField"):
                    case ("MemoField"):
                    case ("PhoneField"):
                    case ("NumericField"): 
                        value = ((TextField) o.getControl()).getText();
                        writer.key(key).value(value);
                        break;
                    case ("DateCalculationField"):
                        break;//Do nothing
                    case ("DateField"):
                       LocalDate date = ((DatePicker) o.getControl()).getValue();                    
                       value = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                       writer.key(key).value(value);
                        break;
                    case ("DateStampField"):
                        value =  new SimpleDateFormat("YYYY-MM-dd").format(new Date());
                        writer.key(key).value(value);
                        break;
                    case ("DateTimeStampField"):
                        value = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date());
                        writer.key(key).value(value);
                         break;
                    case ("StatusField"):
                        if(((String) ((ComboBox) o.getControl()).getValue()).equalsIgnoreCase("yes"))
                            value = "true";
                        else
                            value = "false";

                        writer.key(key).value(value);
                        break;
                    case ("EntityListField"):
                        //Get selected index then fetches that index from the IDs

                            value = (String) ((ComboBox) o.getControl()).getValue();
                            if(value != null){
                                names = o.getNames();
                                for(int j = 0; j < names.length; j++){
                                    if(value.equals(names[j])){
                                        value = o.getIDs()[j];
                                        break;
                                    }
                                }

                            }else
                                value = null;
                        writer.key(key).value(value);
                        break;
                    case ("ListField"):
                        value = ((ComboBox) o.getControl()).getValue().toString();
                        writer.key(key).value(value);
                        break;

                    case ("YesNoField"):
                        value = ((ComboBox) o.getControl()).getValue().toString();
                        if(value.equalsIgnoreCase("yes"))
                            value = "true";
                        else if (value.equalsIgnoreCase("no"))
                            value = "false";
                        else
                            value = "false";

                        writer.key(key).value(value);
                        break;
                    case ("MultipleEntityListField"):
                        options = ((CheckComboBox) o.getControl()).getCheckModel().getCheckedItems();
                        writer.object().key(key).array();
                        for (String string : options)
                        {
                                writer.value(string);
                        }
                        writer.endArray();
                        break;
                    case ("MultipleListField"):
                        String[] ids = o.getIDs();
                        indices = ((CheckComboBox) o.getControl()).getCheckModel().getCheckedIndices();
                        writer.object().key(key).array();
                        for (int j : indices)
                        {
                                writer.value(ids[j]);
                        }
                        writer.endArray();
                        break;
                    default:
                       value = null;
                       writer.key(key).value(value);
                              
                }//End switch
            }//End forloop

            if(actInfo.gpsLocation){
               Pair<Double, Double> gps = core.getGPS(); 
               writer.key("gps_coordinates").array();
               writer.value(gps.getKey());
               writer.value(gps.getValue());
               writer.endArray();
            }

            writer.endObject(); //end action_fields
            writer.endObject(); //close JSON writer

            String data = writer.toString();


            try{
                    pair = Actions.editActionObject(core, entityObjectID, action.actionObjectID, data);
            }catch(IOException e){
                core.getLogger().log(Level.SEVERE, "Error editing action; IOException-", e.getMessage());
                return new Pair("Error submitting action; IOException-" + e.getMessage(), 0);
            }



        }else{  //End if success   
            core.getLogger().log(Level.INFO, "Edited action does not have all fields "
                    + "filled correctly ActionName-{0}", actInfo.actionName);
            return null;
        }           

        //Add event to tables if need be
        if(pair != null && pair.getValue() == 200){
            core.getLogger().log(Level.INFO, "Action has been edited sucessfully "
                    + "ActionName-[{0}] EntityObjectID-[{1}]", new Object[] {actInfo.actionName, entityObjectID});
            addActionEvent(pair, entityObjectID);
        }else{

        }

        return pair;
    }
	
    public Pair<String,Integer> submitAction(String entityObjectID, String actionID){
        boolean success = true;
        Pair<String,Integer> pair;

        Pair<Boolean, String> checkoutPair;
        String actionObjectID;
        boolean rtAction;

        try {
            checkoutPair = Actions.checkActionReturn(core, entityObjectID, actionID);
            actionObjectID = checkoutPair.getValue();
            rtAction = checkoutPair.getKey();

        } catch (IOException ex) {
            core.getLogger().log(Level.WARNING, "Error checking action return; IOException-", ex.getMessage());
            return new Pair("Error checking action return", 0);
        }

        //Check if all required field are filled and Url + Email fields are valid
        core.getLogger().log(Level.INFO, "Checking filled fields for submitted action ActionName-{0}", actInfo.actionName);
        for(objStrut o :elements){
            if((o.isOpen() == !rtAction) || (o.isReturnField() == rtAction)){
                if(o.isRequired()
                        || o.getType().equals("EmailField") 
                        || o.getType().equals("UrlField")){

                    switch(o.getType()) {                               

                        case("UrlField"):
                            if(!(((TextField) o.getControl()).getText().isEmpty() && o.isRequired())){
                                if(!((TextField) o.getControl()).getText().isEmpty()){
                                    try {
                                        new URL(((TextField) o.getControl()).getText()).toURI();
                                        o.getControl().setStyle("-fx-border-color: white");
                                    }catch (MalformedURLException | URISyntaxException e) {
                                        success = false;
                                        o.getControl().setStyle("-fx-border-color: red");
                                    }
                                }else{
                                    o.getControl().setStyle("-fx-border-color: white");
                                }
                            }else{
                                success = false;
                                o.getControl().setStyle("-fx-border-color: red");
                            }
                            break;
                        case("EmailField"):                         
                            if(!(((TextField) o.getControl()).getText().isEmpty() && o.isRequired())){
                                if(((TextField) o.getControl()).getText().isEmpty()
                                    || isValidEmail(((TextField) o.getControl()).getText())){
                                    o.getControl().setStyle("-fx-border-color: white");
                                }else{
                                    success = false;
                                    o.getControl().setStyle("-fx-border-color: red");
                                }
                            }else{
                                success = false;
                                o.getControl().setStyle("-fx-border-color: red");
                            }

                            break;
                        case ("PhoneField"):
                        case ("TextField"):
                        case ("MemoField"):
                        case ("NumericField"): 
                            if(((TextField) o.getControl()).getText().isEmpty()){
                                success = false;
                                o.getControl().setStyle("-fx-border-color: red");
                            }else{
                                o.getControl().setStyle("-fx-border-color: white");
                            }
                             break;
                        case ("DateCalculationField"):
                        case ("DateField"):
                        case ("DateStampField"):
                        case ("DateTimeStampField"):
                             break;//This will be filled in for the user
                        case ("EntityListField"):
                        case ("ListField"):
                        case ("StatusField"):
                            String box;
                            if(((ComboBox) o.getControl()).getValue() == null)
                               box = null;
                            else
                                box = ((ComboBox) o.getControl()).getValue().toString();

                            System.out.printf("%s: %s %n", o.getLabel().getText(), box);
                            if(box == null || box.isEmpty()){
                                success = false;
                                o.getControl().setStyle("-fx-border-color: red");

                            }else{
                                o.getControl().setStyle("-fx-border-color: white");
                            }
                            break;
                        case ("MultipleEntityListField"):
                        case ("MultipleListField"):
                            if(((CheckComboBox) o.getControl()).getCheckModel().isEmpty()){
                                success = false;
                                o.getControl().setStyle("-fx-border-color: red");
                            }else{
                                o.getControl().setStyle("-fx-border-color: white");
                            }
                            break;
                        default:

                        }
                }
            }
        }//End check of required fields

        if(success){
            JSONWriter writer = new JSONStringer().object();
            String key;
            String value;
            String[] names;
            ObservableList<String> options;
            ObservableList<Integer> indices;

            core.getLogger().log(Level.INFO, "Submitted action has fields filled "
                    + "correctly, sending to Asset Panda ActionName-[{0}] EntityObjectID-[{1}]", new Object[] {actInfo.actionName, entityObjectID});

            writer.key("action_fields").object();
            for(objStrut o :elements){
                if((o.isOpen() == !rtAction) || (o.isReturnField() == rtAction)){

                key = o.getJson().getString("key");
                switch(o.getType()) {
                    case("UrlField"):
                    case("EmailField"):
                    case ("TextField"):
                    case ("MemoField"):
                    case ("PhoneField"):
                    case ("NumericField"): 
                        value = ((TextField) o.getControl()).getText();
                        writer.key(key).value(value);
                        break;
                    case ("DateCalculationField"):
                        break;//Do nothing
                    case ("DateField"):
                       LocalDate date = ((DatePicker) o.getControl()).getValue();                    
                       value = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                       writer.key(key).value(value);
                        break;
                    case ("DateStampField"):
                        value =  new SimpleDateFormat("YYYY-MM-dd").format(new Date());
                        writer.key(key).value(value);
                        break;
                    case ("DateTimeStampField"):
                        value = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date());
                        writer.key(key).value(value);
                         break;
                    case ("StatusField"):
                        if(((String) ((ComboBox) o.getControl()).getValue()).equalsIgnoreCase("yes"))
                            value = "true";
                        else
                            value = "false";

                        writer.key(key).value(value);
                        break;
                    case ("EntityListField"):
                        //Get selected index then fetches that index from the IDs

                            value = (String) ((ComboBox) o.getControl()).getValue();
                            if(value != null){
                                names = o.getNames();
                                for(int j = 0; j < names.length; j++){
                                    if(value.equals(names[j])){
                                        value = o.getIDs()[j];
                                        break;
                                    }
                                }

                            }else
                                value = null;
                        writer.key(key).value(value);
                        break;
                    case ("ListField"):
                        value = ((ComboBox) o.getControl()).getValue().toString();
                        writer.key(key).value(value);
                        break;

                    case ("YesNoField"):
                        value = ((ComboBox) o.getControl()).getValue().toString();
                        if(value.equalsIgnoreCase("yes"))
                            value = "true";
                        else if (value.equalsIgnoreCase("no"))
                            value = "false";
                        else
                            value = "false";

                        writer.key(key).value(value);
                        break;
                    case ("MultipleEntityListField"):
                        options = ((CheckComboBox) o.getControl()).getCheckModel().getCheckedItems();
                        writer.object().key(key).array();
                        for (String string : options)
                        {
                                writer.value(string);
                        }
                        writer.endArray();
                        break;
                    case ("MultipleListField"):
                        String[] ids = o.getIDs();
                        indices = ((CheckComboBox) o.getControl()).getCheckModel().getCheckedIndices();
                        writer.object().key(key).array();
                        for (int j : indices)
                        {
                                writer.value(ids[j]);
                        }
                        writer.endArray();
                        break;
                    default:
                       value = null;
                       writer.key(key).value(value);
                }               
                }//End switch
            }//End forloop

            if(actInfo.gpsLocation){
               Pair<Double, Double> gps = core.getGPS(); 
               writer.key("gps_coordinates").array();
               writer.value(gps.getKey());
               writer.value(gps.getValue());
               writer.endArray();
            }

            writer.endObject(); //end action_fields
            writer.endObject(); //close JSON writer

            String data = writer.toString();


            try{
                if(rtAction && actInfo.isReturnable)
                    pair = Actions.sendActionObjectReturn(core, entityObjectID, actionObjectID, data);
                else
                    pair = Actions.sendActionObject(core, entityObjectID, actInfo.actionID, data); 

            }catch(IOException e){
                core.getLogger().log(Level.SEVERE, "Error submitting action; IOException-", e.getMessage());
                return new Pair("Error submitting action; IOException-" + e.getMessage(), 0);
            }



        }else{  //End if success   
            core.getLogger().log(Level.INFO, "Submitted action does not have all fields "
                    + "filled correctly ActionName-{0}", actInfo.actionName);
            return null;
        }           

        //Add event to tables if need be
        if(pair != null && pair.getValue() == 200){
            core.getLogger().log(Level.INFO, "Submitted action has returned sucessfully "
                    + "ActionName-[{0}] EntityObjectID-[{1}]", new Object[] {actInfo.actionName, entityObjectID});
            addActionEvent(pair, entityObjectID);
        }else{

        }

        return pair;

    }

    /**
     * A data structure created just to keep everything neat.
     */

    private class objStrut{
        private boolean entityDependent;
        private Label label;
        private JSONObject json;
        private String[] names = null;
        private String[] ids = null;
        private Control control;
        public ActionColumnInfo colInfo;

        public objStrut(ActionColumnInfo colInfo, Label label, Control control, boolean entityDependent, String[] names, String[] ids){
            this(colInfo, label, control, entityDependent, names);
            this.ids = ids;                
        }

        public objStrut(ActionColumnInfo colInfo, Label label, Control control, boolean entityDependent, String[] names){
            this(colInfo, label, control, entityDependent);
            this.names = names;       
        }

        public objStrut(ActionColumnInfo colInfo, Label label, Control control, boolean entityDependent){
            this.label = label;
            this.control = control;
            this.json = new JSONObject(colInfo.JSONString);
            this.colInfo = colInfo;
            this.entityDependent = entityDependent;
        }

        public String getType(){
            return colInfo.type;
        }

        public Label getLabel(){
            return label;
        }

        public JSONObject getJson(){
            return json;
        }

        public boolean isRequired(){
            return colInfo.isRequired;
        }

        public boolean isDependent(){
            return entityDependent;
        }

        public boolean isReturnField(){
            return colInfo.isReturnField;
        }
        
        public boolean isOpen(){
            return colInfo.isOpen;
        }

        public boolean isEditable(){
            return colInfo.isEditable;
        }

        public boolean isEditableReturn(){
            return colInfo.isEditableReturn;
        }

        public String[] getNames(){
            return names;
        }

        public String[] getIDs(){
            return ids;
        }

        public Control getControl(){
           return control;
        }

        public String getDefaultInput() {
            return colInfo.defaultValue;
        }
        
        public String getKey(){
            return colInfo.key;
        }

    }

     public static boolean isValidEmail(String email){
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\."+
                            "[a-zA-Z0-9_+&*-]+)*@" +
                            "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
                            "A-Z]{2,7}$";

        Pattern pat = Pattern.compile(emailRegex);
        if (email == null)
            return false;
        return pat.matcher(email).matches();   
    }
     
  private void addActionEvent(Pair<String,Integer> pair, String entityObjectID){
      try{
        JSONObject json = new JSONObject(pair.getKey()); 
        core.getLogger().log(Level.FINE, "Adding actionEvent to table ActionName-{0}", actInfo.actionName);
        ActionObjectEntite event = new ActionObjectEntite(actInfo, entityObjectID, json, core);
        
        //Tell the action object to update
        actionObj.updateActionHistory(event);
        
      }catch(JSONException e){
          e.printStackTrace();
      }
        
  }   
  
  public void clearFields(){
      for(int i = 0; i < elements.size(); i++){
        switch(elements.get(i).getType()) {                               

            case ("DateStampField"):
            case ("DateTimeStampField"):
                break;
            case ("UrlField"):
            case ("EmailField"): 
            case ("PhoneField"):
            case ("TextField"):
            case ("MemoField"):
            case ("NumericField"): 
                ((TextField) elements.get(i).getControl()).setText("");
                 break;
            case ("DateField"):
                ((DatePicker)elements.get(i).getControl()).setValue(LocalDate.now());
                 break;//This will be filled in for the user
            case ("EntityListField"):
            case ("ListField"):
            case ("StatusField"):
            case ("MultipleEntityListField"):
            case ("MultipleListField"):
                ((ComboBox) elements.get(i).getControl()).setValue(elements.get(i).getDefaultInput());
                break;
            case ("DateCalculationField"):
            default:

            }
        
        elements.get(i).getControl().setVisible(true);
        elements.get(i).getControl().setDisable(false);
    }//End for
  }
    
} 
