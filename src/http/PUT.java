/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package http;

import core.Core;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.logging.Level;
import javafx.util.Pair;

/**
 *
 * @author dholmes
 */
public class PUT {
    
    public static Pair<String, Integer> sendPUTAssetPanda(String targetURL, String data, Core core) throws IOException{
        HttpURLConnection con = null;
        int responseCode = 0;
        int APICalls = core.incAPICalls();
        int gate = 0;

        core.getLogger().log(Level.FINER, "Making PUT call to Asset Panda url = [{0}]  CallNumber [{1}]", new Object[] {targetURL, APICalls});
                  
        while(gate < 3){
            try {
              //Create connection
              URL url = new URL(targetURL);
              con = (HttpURLConnection) url.openConnection();
              con.setRequestMethod("PUT");         
              
              con.setRequestProperty("Content-Type",  "application/json");

              con.setRequestProperty("Content-Length", 
                  Integer.toString(data.getBytes().length));
              con.setRequestProperty("Content-Language", "en-US");  
              con.setRequestProperty ("Authorization", "Bearer " + core.getToken());
              con.setReadTimeout(10000);

              con.setUseCaches(false);
              con.setDoOutput(true);

              //Send request
              DataOutputStream wr = new DataOutputStream (
                  con.getOutputStream());
              wr.writeBytes(data);
              wr.close();

              //Get Response 
              responseCode = con.getResponseCode();
              core.getLogger().log(Level.FINER, "PUT call [{0}] returned, code [{1}].", new Object[] {APICalls, responseCode});

              InputStream is = con.getInputStream();
              BufferedReader rd = new BufferedReader(new InputStreamReader(is));
              StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
              String line;
              while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
              }
              rd.close();

              core.getLogger().log(Level.FINEST, "PUT call [{0}] code [{1}] responce [{2}].", new Object[] {APICalls, responseCode, response.toString()});
              return new Pair<>(response.toString(), responseCode);

            }catch (SocketTimeoutException | SocketException e){
                if(gate < 3){
                    gate++;
                    int timeout = ((int) Math.round(Math.pow(2, gate)) * 100);
                    core.getLogger().log(Level.WARNING, "Error making call [{0}]. Backing off [{1}].", new Object[] {APICalls, timeout});
                    try {
                        Thread.sleep(timeout);
                    } catch (InterruptedException ex) {}
                }else{
                    core.getLogger().log(Level.SEVERE, "Error making call [{0}]. Giving up.", APICalls);
                    throw new IOException("Request" + APICalls +" could not be sent");
                }

            }catch (IOException e) {
              StringBuilder response = new StringBuilder();
              try {
                  InputStream is = con.getErrorStream();
                  BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                   // or StringBuffer if Java version 5+
                  String line;
                  while ((line = rd.readLine()) != null) {
                          response.append(line);
                          response.append('\r');
                  }
                  rd.close();
                  responseCode = con.getResponseCode();
              } catch(IOException e1) {
                  if (con != null) {
                      con.disconnect();
                  }

                  throw e1;
              }

                  return new Pair<>(response.toString(), responseCode);
            } 
          }

        return null;
    }
}
