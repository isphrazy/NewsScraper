package knowitall.util;

import java.io.File;

public class AddDateToRawData {
    public static void main(String[] args){
        File root = new File("yahoo_data");
        String[] folders = root.list();
        
        for(String folderName : folders){
            File folder = new File("yahoo_data/" + folderName + "/raw_data/");
            String[] files = folder.list();
            for(String fileName : files){
                if(!fileName.startsWith(folderName)){
                    File file = new File("yahoo_data/" + folderName + "/raw_data/" + fileName);
                    File newFile = new File("yahoo_data/" + folderName + "/raw_data/" + folderName + "_" + fileName);
                    file.renameTo(newFile);
                }
            }
        }
    }
}  
