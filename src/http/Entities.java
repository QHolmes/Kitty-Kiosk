package http;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import core.Core;
import javafx.util.Pair;
import static core.Config.RESULTS_LIMIT;

/**
 * @author Quinten Holmes
 */
public class Entities {
	
	
    public static Pair<String, Integer> getEntites(Core core) throws IOException {				
            return makeCall("entities", core, true);
    }

    public static Pair<String, Integer> getEntiteObjectsActions(Core core, int offset, String entityID) throws IOException {
        String url = String.format("entities/%s/objects?offset=%d&limit=%d&include=action_objects",
                entityID, offset, RESULTS_LIMIT);
            return makeCall(url, core, true);
    }
    
    public static Pair<String, Integer> getEntiteObjects(Core core, int offset, String entityID) throws IOException {
        String url = String.format("entities/%s/objects?offset=%d&limit=%d",
                entityID, offset, RESULTS_LIMIT);
            return makeCall(url, core, true);
    }

    public static Pair<String, Integer> getEntiteInfo(Core core, String ID) throws IOException {
            return makeCall("entities/"+ID, core, true);
    }	

    public static Pair<String, Integer> getActions(Core core, String ID, boolean getNew) throws IOException {
            return makeCall("actions?entity_id=" + ID, core, getNew);
    }

    public static Pair<String, Integer> getStatus(Core core, String entityID) throws IOException {   
            return makeCall("statuses?entity_id=" + entityID, core, true);
    } 

    public static Pair<String, Integer> getEntityActions(Core core, String entityObjectID, String actionID, boolean checkHistory) throws IOException{
            String url = String.format("entity_objects/%s/action_objects?action_id=%s", entityObjectID, actionID);
            return makeCall(url, core, checkHistory);
    }

    public static Pair<String, Integer> getEntityObjectID(Core core, String barcode, String entityID, boolean checkHistory) throws IOException{
        barcode = barcode.replaceAll(" ", "%20");
            String url = String.format("entities/%s/objects?barcode=%s", entityID, barcode);
            System.out.println(url);
            return makeCall(url, core, checkHistory);
    }

    public static Pair<String, Integer> getEntityObject(Core core, String entityObjectID, boolean checkHistory) throws IOException{
            String url = String.format("entity_objects/%s", entityObjectID);
            return makeCall(url, core, checkHistory);
    }

    public static Pair<String, Integer> getActionObjects(Core core, String entityObjectID, String actionID, int offset, boolean isReturned, boolean checkHistory) throws IOException{
            String url = String.format("entity_objects/%s/action_objects?offset=%d&action_id=%s&returned=%s", 
                    entityObjectID, offset, actionID, isReturned ? "true" : "false");
            return makeCall(url, core, checkHistory);
    }

    public static Pair<String, Integer> fetchEntityObject(Core core, String entityObjectID) throws IOException{
            String url = String.format("entity_objects/%s", entityObjectID);
            return makeCall(url, core, false);
    }  

    @SuppressWarnings("unchecked")
    public static String[] getListEntiteName(Core core, String ID) throws IOException{
            String[] entitesNames; 	

            String url = String.format("entities/%s/objects?offset=0&limit=%d", ID, RESULTS_LIMIT);
            entitesNames = (String[]) core.getHTTPMap(url + "N");


            if(entitesNames == null)
                    entitesNames = getListEntites(core, ID);

            return entitesNames;

    }

    //being replaced by getEntityData
    @SuppressWarnings("unchecked")
    public static String[] getListEntiteID(Core core, String ID) throws IOException{
            String[] entitesID; //new ArrayList<Pair<String, Integer>>()		

            String url = String.format("entities/%s/objects?offset=0&limit=%d", ID, RESULTS_LIMIT);
            entitesID = (String[]) core.getHTTPMap(url + "I");


            if(entitesID == null)
                    entitesID = getListEntites(core, ID);

            return entitesID;
    }
    //being replaced by getEntityData
    private static String[] getListEntites(Core core, String ID) throws IOException{			
                    String url = String.format("https://login.assetpanda.com:443/v2/entities/%s/objects?offset=0&limit=%d", ID, RESULTS_LIMIT);

                    String[] entitesNames = null;
                    String[]  entitesID;

                    Pair<String, Integer> tempPair = GET.getAssetPanda(url, core);
                    Pair<JSONObject, Integer> reply = new Pair<>(new JSONObject(tempPair.getKey()), tempPair.getValue());

                    if(reply.getValue() == 200) {
                            JSONObject json = reply.getKey();
                            JSONArray array = json.getJSONArray("objects");
                            int number = json.getJSONObject("totals").getInt("group_totals");
                            int Offset = 0;
                            entitesNames = new String[number];
                            entitesID = new String[number];


                            while( Offset < number) {
                                    for(int i = 0; i < array.length(); i++) {
                                            json = array.getJSONObject(i);				
                                            entitesNames[Offset] = json.getString("display_with_secondary");
                                            entitesID[Offset] = json.getString("id");
                                            Offset++;					
                                    }

                                    url = String.format("https://login.assetpanda.com:443/v2/entities/%s/objects?offset=%d&limit=%d", ID, Offset, RESULTS_LIMIT);

                                    tempPair = GET.getAssetPanda(url, core);
                                    reply = new Pair<>(new JSONObject(tempPair.getKey()), tempPair.getValue());
                                    if(reply.getValue() == 200) {
                                            array = reply.getKey().getJSONArray("objects");
                                    }
                                    else {
                                            core.getLogger().fine(""+reply.getKey());
                                            core.getLogger().fine(""+reply.getValue());
                                            core.getLogger().fine(""+Offset + " Number " + number);
                                            return null;
                                    }
                            }

                            url = String.format("entities/%s/objects?offset=0&limit=%d", ID, RESULTS_LIMIT);
                            core.addHTTPMap(url + "N", entitesNames);	
                            core.addHTTPMap(url + "I", entitesID);	
                    }else {
                            core.getLogger().fine(""+reply.getKey());
                            core.getLogger().fine(""+reply.getValue());
                    }

            return entitesNames;		
    }
    
    /**
     * Checks if the call is currently stored, if not it will make the call.
     * If the return is valid it will save the result in the Core's map.
     * @param url
     * @param core
     * @return
     * @throws IOException 
     */
    private static Pair<String, Integer> makeCall(String url, Core core, boolean checkHistory) throws IOException{
        //Check if someone else is already making this call, if so wait for return

        //Check if there is history of the call
        Pair<String, Integer> pair = (Pair<String, Integer>) core.getHTTPMap(url);

        //If there is no history make the call ourselves
        if(pair == null || !checkHistory){
            String call = "https://login.assetpanda.com:443/v2/" + url;
            pair  = GET.getAssetPanda(call, core);

            if(pair.getValue() < 500)
                core.addHTTPMap(url, pair);

        }

        return pair;
    }
	
}
