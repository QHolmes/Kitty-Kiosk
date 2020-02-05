package helperClasses;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import core.Core;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.logging.Level;
import static core.Config.DATA_DIR;

/**
 * @author Quinten Holmes
 */
public class Save {
    //Using this so we don't have a queue to keep saving the same file
    private static HashSet<String> saving = new HashSet();
    
	public static void saveObject(Serializable obj, String objName, Core core){
        String location = DATA_DIR.resolve(objName + ".ser").toString();
            
            if(saving.contains(location))
                return;
            
            FileOutputStream  writer = null;
            ObjectOutputStream oos = null;
		try {   
                    saving.add(location);
                    writer = new FileOutputStream(new File(location));
                    
                    oos = new ObjectOutputStream(writer);
                    oos.writeObject(obj);
                    oos.close();
                    writer.close();

                    core.getLogger().log(Level.FINER, "Saving core - {0}", location);
		}catch (FileNotFoundException e){
                    //If the folder does not exist, create it and try again
                if(DATA_DIR.toFile().mkdirs()){
                     core.getLogger().log(Level.FINER, "Created save folder - {0}", DATA_DIR);
                        saveObject(obj, objName, core);
                    }
                        
                    core.getLogger().log(Level.WARNING, "Could not save core. {0}", e.getMessage());
                    
                }catch (IOException e) {
                     core.getLogger().log(Level.WARNING, "Could not save core, IOException: {0}", e.getMessage());
                     e.printStackTrace();
		}finally{
                    try{
                        if(oos != null)
                            oos.close();
                        if(writer != null)
                           writer.close();
                    }catch(IOException close){
                        close.printStackTrace();
                    }
                    saving.remove(location);
                }
	}
	
	public static Serializable loadObject(String objName, Core cor){
            Serializable obj = null;
            String location = DATA_DIR.resolve(objName + ".ser").toString();
            
            int i = 0;
            while (obj == null && i < 5){
                try {
                    Thread.sleep(100 * i);
                } catch (InterruptedException ex) {}
                
                obj = loadSerizlizableFile(location, objName, cor);
                i++;
            }
            
            return obj;
	}
        
        private static Serializable loadSerizlizableFile(String location, String objName, Core core){
            Serializable obj = null;
            
            
            ObjectInputStream ois = null;
            FileInputStream reader = null;
            try{
                reader = new FileInputStream(new File(location));
                ois = new ObjectInputStream(reader);
                obj = (Serializable) ois.readObject();
                core.getLogger().log(Level.INFO, "Found {0}: {1}", new Object[] {objName, location});
            }catch(InvalidClassException inClass){
                core.getLogger().log(Level.INFO, "Older verson of {0} found, not loading.", objName);
            }catch( IOException | ClassNotFoundException e){
                //File probably is locked
            }finally{
                try {
                    if(ois != null)
                        ois.close();
                    if(reader != null)
                        reader.close();
                } catch (IOException ex) {
                }
            }
            
            return obj;
        }
}
