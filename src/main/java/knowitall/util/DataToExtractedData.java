package knowitall.util;

import java.io.File;
import java.util.Calendar;

import knowitall.reverbovernews.ReverbNewsExtractor;
import knowitall.reverbovernews.YahooRssScraper;

public class DataToExtractedData {
    public static void main(String[] args){
        File root = new File("yahoo_data/data/");
        String[] folders = root.list();
        
        for(String folderName : folders){
            ReverbNewsExtractor rne = new ReverbNewsExtractor(Calendar.getInstance(), "YahooConfigFile");
        }
    }
}
