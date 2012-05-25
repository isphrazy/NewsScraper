package knowitall.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

public class MergeRawData {
    
    private static final String ROOT_DIR = "yahoo_data/";
    
    public static void main(String[] args){
        
        File backFile = new File(ROOT_DIR + "2012-05-16b/raw_data/");
        for(String fileName : backFile.list()){
//            File sourceFile = new File(backFile + fileName);
            String mergedFileName = ROOT_DIR + "2012-05-16/raw_data/" + fileName;
//            File mergedFile = new File(mergedFileName);
            FileWriter fstream;
            try {
                fstream = new FileWriter(mergedFileName,true);
                BufferedWriter out = new BufferedWriter(fstream);
                String srcContent = FileLoader.loadFile(ROOT_DIR + "2012-05-16b/raw_data/" + fileName);
                out.write(srcContent);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
