package knowitall.reverbovernews;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


/**
 * This class is used as scrape new from internet
 * @author Pingyang He
 *
 */
public class NewsScraperMain {
	
	private static final String YAHOO_CONFIG_FILE = "YahooRssConfig";
	
	private static final String FETCH_DATA_ONLY = "s";
	private static final String PROCESS_RSS_WITH_GIVEN_DIR = "d";
	private static final String USE_REVERB = "r";
	private static final String USE_REVERB_WITH_DIR = "rd";
	
	private static Calendar calendar;
	
    public static void main( String[] args ) throws IOException{
    	
    	calendar = Calendar.getInstance();
    	
    	CommandLine cmd = getCommands(args);
//    	System.out.println(cmd.h.getOptionValues(FETCH_DATA_ONLY));
    	
    	
//    	if(args.length > 0){
//    		int argsLength = args.length;
//    		if(argsLength == 1 && args[0].equals(FETCH_DATA_ONLY))
//    			fetchYahooRSS(false, null, null);
//    		else if(argsLength == 3 && args[0].equals(PROCESS_RSS_WITH_GIVEN_DIR))
//    			fetchYahooRSS(true, args[1], args[2]);
//    		else{
//    			System.out.println("invalid input argument number");
//    		}
//    	}else{
//    		fetchYahooRSS(true, null, null);
//    	}
    	
    	reverbExtract(cmd.getOptionValues(USE_REVERB_WITH_DIR));
    	
    }
    
    private static void reverbExtract(String[] optionValues) {
    	ReverbNewsExtractor rne = new ReverbNewsExtractor(calendar, YAHOO_CONFIG_FILE);
//    	rne.exp();
    	rne.extract(null, null);
	}

	private static CommandLine getCommands(String[] args) {
    	
    	Option fetchDataOnlyOp = new Option(FETCH_DATA_ONLY, false, 
    			"only fetch rss, not processing it");
    	
    	Option processWithDirOp = new Option(PROCESS_RSS_WITH_GIVEN_DIR, false, 
    			"process rss in first arg and save it to second arg");
    	
    	Option useReverbOp = new Option(USE_REVERB, false, 
    			"use reverb to extract today's file");
    	
    	Option useReverbWithDirOp = new Option(USE_REVERB_WITH_DIR, false,
    			"use reverb to extract files in the first arg and save it into second arg");
    	
        Options options = new Options();
        options.addOption(fetchDataOnlyOp);
        options.addOption(processWithDirOp);
        options.addOption(useReverbOp);
        options.addOption(useReverbWithDirOp);
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
        return cmd;
	}

	/*
     * fetch rss from yahoo, store(and/or process it) and store in local file
     * if want to process the rss, pass in true as first argument
     * the second argument is the directory where the html is stored, if it's null, 
     * use today's directory
     */
	private static void fetchYahooRSS(boolean proc, String sourceDir, String targetDir) throws IOException {
		
		YahooRssScraper yrs = new YahooRssScraper(calendar);
		yrs.scrape(proc, sourceDir, targetDir);
		
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
