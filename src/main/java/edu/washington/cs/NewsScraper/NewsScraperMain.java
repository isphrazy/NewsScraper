package edu.washington.cs.NewsScraper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;


/**
 * This class is used as scrape new from internet
 * @author Pingyang He
 *
 */
public class NewsScraperMain {
	
	private static final String PROCESS_ARGUMENT = "-s";
	private static final String DIRECTORY_ARGUMENT = "-d";
	
	private static Calendar calendar;
	
    public static void main( String[] args ) throws IOException{
    	
    	getDate();
    	
    	String dir = null;
    	
    	if(args.length > 0){
    		int argsLength = args.length;
    		if(argsLength == 1 && args[0].equals(PROCESS_ARGUMENT))
    			fetchYahooRSS(false, dir);
    		else if(argsLength == 2 && args[0].equals(DIRECTORY_ARGUMENT))
    			fetchYahooRSS(true, args[1]);
    	}else{
    		fetchYahooRSS(true, null);
    	}
    	
    }
    
    /*
     * fetch rss from yahoo, store(and/or process it) and store in local file
     * if want to process the rss, pass in true as first argument
     * the second argument is the directory where the html is stored, if it's null, 
     * use today's directory
     */
	private static void fetchYahooRSS(boolean proc, String dir) throws IOException {
		
		YahooRssScraper yrs = new YahooRssScraper(calendar);
		yrs.scrape(proc, dir);
		
	}
    
    /*
     * get today's date
     */
    private static void getDate() {
		DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy");
		calendar = Calendar.getInstance();
	}


	/*
	 * create a hashcode for the given string
	 */
	private static long createId(String title){
		long hash = 7;
		int prime = 31;
		for(int i = 0; i < title.length(); i++){
			hash = hash * prime + title.charAt(i);
		}
		return hash;
	}

   
}
