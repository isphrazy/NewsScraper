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
	
	private static final String PROCESS_ARGUMENT = "-p";
	
	private static Calendar calendar;
	
    public static void main( String[] args ) throws IOException{
    	
    	getDate();
    	
    	fetchYahooRSS(args[0].equals(PROCESS_ARGUMENT));
    	
    }
    
    /*
     * fetch rss from yahoo, store(and/or process it) and store in local file
     * if want to process the rss, pass in true as argument
     */
	private static void fetchYahooRSS(boolean proc) throws IOException {
		
		YahooRssScraper yrs = new YahooRssScraper(calendar);
		yrs.scrape(proc);
		
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
