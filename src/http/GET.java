package http;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


import core.Core;
import javafx.util.Pair;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;

/**
 *
 * @author Quinten Holmes
 */
public class GET {
	
	public static Pair<String, Integer> getAssetPanda(String targetURL, Core core) throws SocketTimeoutException, java.io.IOException, java.net.UnknownHostException{
		String reply;
		HttpURLConnection con;
		int responseCode;
                int gate = 0;
		int APICalls = core.incAPICalls();
                core.getLogger().log(Level.FINER, "Making GET call to Asset Panda url = [{0}]  CallNumber [{1}]", new Object[] {targetURL, APICalls});
                
                while(gate < 4)
		try{
                    URL obj = new URL(targetURL);
                    con = (HttpURLConnection) obj.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty ("Authorization", "Bearer " + core.getToken());
                    con.setReadTimeout(60000);
                    responseCode = con.getResponseCode();

                    if(responseCode == HttpURLConnection.HTTP_OK) {  
                        //Success
                        StringBuilder response;
                        try(BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                                String inputLine;
                                response = new StringBuilder();
                                
                                while ((inputLine = in.readLine()) != null) 
                                    response.append(inputLine);
                            }

                        // Log result
                        reply = response.toString();
                        core.getLogger().log(Level.FINER, "GET call [{0}] returned successful, code [{1}].", new Object[] {APICalls, responseCode});
                        core.getLogger().log(Level.FINEST, "GET call [{0}] code [{1}] responce [{2}].", new Object[] {APICalls, responseCode, reply});
                    }else {
                        StringBuffer response;
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
                            String inputLine;
                            response = new StringBuffer();
                            
                            while ((inputLine = in.readLine()) != null)
                                response.append(inputLine);
                        }				

                        reply = response.toString();
                        core.getLogger().log(Level.FINER, "GET call [{0}] returned un-successful, code [{1}].", new Object[] {APICalls, responseCode});
                        core.getLogger().log(Level.FINEST, "GET call [{0}] code [{1}] responce [{2}].", new Object[] {APICalls, responseCode, reply});

                        
                        core.getLogger().fine(targetURL);
                        //Bad Gateway or Too many requests
                        if(responseCode == 502 || responseCode == 429){
                            gate++;
                            timeout(gate, APICalls, core);
                        }else
                            core.getLogger().log(Level.WARNING, "Server returned HTTP response code: {0}: {1}", 
                                    new Object[]{responseCode, reply});
                    }			
                        return new Pair<>(reply, responseCode);
                    
                }catch(SocketTimeoutException | SocketException e){
                    if(gate < 3){
                        gate++;
                        timeout(gate, APICalls, core);
                    }else{
                        core.getLogger().log(Level.SEVERE, "Error making call [{0}]. Giving up.", APICalls);
                        throw e;
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
                
            return null;
	}
        
        public static Pair<String, Integer> getPublic(String targetURL, Core core) throws IOException, java.net.ConnectException{
                String reply;
		HttpURLConnection con;
		int responseCode;
                int gate = 0;
		int APICalls = core.incAPICalls();
                core.getLogger().log(Level.FINER, "Making public GET call url = [{0}]  CallNumber [{1}]", new Object[] {targetURL, APICalls});
                
                while(gate < 3)
		try{
                    URL obj = new URL(targetURL);
                    con = (HttpURLConnection) obj.openConnection();
                    con.setRequestMethod("GET");
                    con.setReadTimeout(3000);
                    responseCode = con.getResponseCode();

                    if(responseCode == HttpURLConnection.HTTP_OK) {  
                        //Success
                        StringBuilder response;
                        try(BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                                String inputLine;
                                response = new StringBuilder();
                                
                                while ((inputLine = in.readLine()) != null) 
                                    response.append(inputLine);
                            }

                        // Log result
                        reply = response.toString();
                        core.getLogger().log(Level.FINER, "GET call [{0}] returned successful, code [{1}].", new Object[] {APICalls, responseCode});
                        core.getLogger().log(Level.FINEST, "GET call [{0}] code {1} responce [{2}].", new Object[] {APICalls, responseCode, reply});
                    }else {
                        StringBuffer response;
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
                            String inputLine;
                            response = new StringBuffer();
                            
                            while ((inputLine = in.readLine()) != null)
                                response.append(inputLine);
                        }				

                        reply = response.toString();
                        core.getLogger().log(Level.FINER, "GET call [{0}] returned un-successful, code [{1}].", new Object[] {APICalls, responseCode});
                        core.getLogger().log(Level.FINEST, "GET call [{0}] code [{1}] responce [{2}].", new Object[] {APICalls, responseCode, reply});
                    }

                    if(responseCode != 200){
                        throw new IOException("Server returned HTTP response code: " + responseCode + ": Responce " + reply);
                    }else			
                        return new Pair<>(reply, responseCode);
                    
                }catch(SocketTimeoutException | SocketException e){
                    if(gate < 3){
                        gate++;
                        int timeout = ((int) Math.round(Math.pow(2, gate)) * 100);
                        core.getLogger().log(Level.WARNING, "Error making call [{0}]. Backing off [{1}].", new Object[] {APICalls, timeout});
                        try {
                            Thread.sleep(timeout);
                        } catch (InterruptedException ex) {}
                    }else{
                        core.getLogger().log(Level.SEVERE, "Error making call [{0}]. Giving up.", APICalls);
                        throw new IOException("Request could not be sent");
                    }
                }
                
            return null;

        }
        
        private static void timeout(int factor, int callNumber, Core core){
            int timeout = ((int) Math.round(Math.pow(2, factor)) * 100);
            core.getLogger().log(Level.WARNING, "Error making call [{0}]. Backing off [{1}].", new Object[] {callNumber, timeout});
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException ex) {}
        }
}
