package knowitall.util;

import java.io.File;
import java.util.Calendar;

import knowitall.reverbovernews.YahooRssScraper;

public class RawDataToData {
    public static void main(String[] args){
        File root = new File("yahoo_data");
        String[] folders = root.list();
        
        for(String folderName : folders){
            YahooRssScraper yrs = new YahooRssScraper(Calendar.getInstance());
            yrs.scrape(false, true, "yahoo_data/" + folderName + "/raw_data/", "yahoo_data/" + folderName);
        }
    }
}
