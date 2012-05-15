package knowitall.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

import knowitall.reverbovernews.Config;

public class StaticGenerator {
    
    private static List<String> categoryList;
    
    public static void main(String[] args){
		
        loadConfig();
        
	    generateStatics();
		
	}


    private static void generateStatics() {
		
	    String dataFolder = "yahoo_data/extracted_data/";
	    String outputStaticFolder = "yahoo_data/statics/";
	    
	    File dataFolderFile = new File(dataFolder);
	    
	    for(String fileName : dataFolderFile.list()){
	         
	        processFile(dataFolder + fileName);
	        
	    }
	}
    
    private static void processFile(String fileDir) {
        String content = FileLoader.loadFile(fileDir);
    }



    private static void loadConfig() {
        Config config = new Config();
        try {
            config.loadConfig("YahooRssConfig");
        } catch (FileNotFoundException e) {
            categoryList = config.getCategory();
            e.printStackTrace();
        }
    }
    
    
}
