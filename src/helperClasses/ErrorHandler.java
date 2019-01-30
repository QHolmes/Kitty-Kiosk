package helperClasses;

import core.Core;
import javafx.util.Pair;

/**
 *
 * @author Quinten Holmes
 */
public class ErrorHandler {

	public ErrorHandler(Pair<String, Integer> HTTP, Core core){
		
		int error = HTTP.getValue();
                switch(error){
                    case(401): //"Invalid access token"
                      //core.log("401 error reported " + HTTP.getKey());
                      System.out.println("Token: " + core.getToken());	
                      break;
                    case(111):
                        break;
                    case(422):
                        //bad login info
			//core.log("422 error reported " + HTTP.getKey());
                        break;
                    case(406):
                        //"Not Acceptable" internal format error probably
			//core.log("406 error reported " + HTTP.getKey());
                        break;
                    case(500):
                        //"Internal Server Error" The client_secret or client_id could be wrong
			//core.log("500 error reported " + HTTP.getKey());
                        break;
                    case(504):
                        //Gateway Timeout
			//core.log("504 error reported " + HTTP.getKey());
                        break;
                        
                    default:
                       
                }
		
	}
}
