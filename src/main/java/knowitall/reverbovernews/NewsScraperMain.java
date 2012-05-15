package knowitall.reverbovernews;

import java.io.IOException;
import java.util.Calendar;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
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
    
    private static final String SCRAPE_DATA_ONLY = "s";
    private static final String SCRAPE_DATA_AND_PROCESS_DATA = "sp";
    private static final String PROCESS_RSS_WITH_GIVEN_DIR = "p";
    private static final String USE_REVERB = "r";
    private static final String USE_REVERB_WITH_DIR = "rd";
    private static final String FORMAT_OPT = "fmt";
    private static final String FORMAT_DIR = "fd";
    private static final String FORMAT_TODAY = "ftoday";
    private static final String FORMAT_TIME_FILTER = "ft";
    private static final String FORMAT_CONFIDENCE_THRESHOLD = "fct";
    private static final String FORMAT_CATEGORY_FILTER = "fc";
    private static final String HELP = "h";
    
    private static Calendar calendar;
    private static Options options;
    
    public static void main( String[] args ) throws IOException{
        initiateVars();
    
        CommandLine cmd = getCommands(args, options);

        validateOptionNumbers(cmd);
        
        fetchNews(cmd);

        extractNews(cmd);

        formatData(cmd);
       
        help(cmd);

    }
    
    /*
     * print the help option
     */
    private static void help(CommandLine cmd) {
        if(cmd.hasOption(HELP)){
            printUsage();
        }
    }

    /*
     * initiate variables for this object
     */
    private static void initiateVars() {
        calendar = Calendar.getInstance();
        options = new Options();
    }

    /*
     * format the stored data
     */
    private static void formatData(CommandLine cmd) {
        if(cmd.hasOption(FORMAT_OPT)){
            String[] dir = null;
            String[] timeInterval = null;
            double confidenceThreshold = -1;
            boolean formatToday = false;
            String category = null;
            
            if(cmd.hasOption(FORMAT_DIR)){
                dir = cmd.getOptionValues(FORMAT_DIR);
                if(dir.length != 2 || dir[0] == null || dir[1] == null){
                    printUsage();
                }
            }
            
            if(cmd.hasOption(FORMAT_TODAY)){
                if(cmd.hasOption(FORMAT_TIME_FILTER)) printUsage();
                formatToday = true;
                
            }else if(cmd.hasOption(FORMAT_TIME_FILTER)){
                timeInterval = cmd.getOptionValues(FORMAT_TIME_FILTER);
                if(timeInterval.length != 2 || 
                    timeInterval[0] == null || timeInterval[1] == null){
                    printUsage();
                }
            }
            
            if(cmd.hasOption(FORMAT_CONFIDENCE_THRESHOLD)){
                confidenceThreshold = Double.parseDouble(cmd.getOptionValue(FORMAT_CONFIDENCE_THRESHOLD, "-1"));
            }
            
            if(cmd.hasOption(FORMAT_CATEGORY_FILTER)){
                category = cmd.getOptionValue(FORMAT_CATEGORY_FILTER);
                if(category == null) printUsage();
            }
            
            ExtractedDataFormater formater = new ExtractedDataFormater(calendar, YAHOO_CONFIG_FILE);
            formater.format(dir, timeInterval, confidenceThreshold, category, formatToday);
            
        }
    }

    /*
     * extract the news
     */
    private static void extractNews(CommandLine cmd) {
        if(cmd.hasOption(USE_REVERB)){
            reverbExtract(null);
        }else if(cmd.hasOption(USE_REVERB_WITH_DIR)){
            String[] dirs = cmd.getOptionValues(USE_REVERB_WITH_DIR);
            reverbExtract(dirs);
        }
    }

    /*
     * fetch the news online
     */
    private static void fetchNews(CommandLine cmd) {
        try {
            if (cmd.hasOption(SCRAPE_DATA_ONLY)){//only fetch data from internet, not process the raw data
                fetchYahooRSS(true, false, null);
            }else if(cmd.hasOption(SCRAPE_DATA_AND_PROCESS_DATA)){
                fetchYahooRSS(true, true, null);
            }else if(cmd.hasOption(PROCESS_RSS_WITH_GIVEN_DIR)){
                fetchYahooRSS(false, true, cmd.getOptionValues(PROCESS_RSS_WITH_GIVEN_DIR));
            }
        } catch (IOException excp) {
            excp.printStackTrace();
        }
    }

    /*
     * validate the number of options passed in args
     */
    private static void validateOptionNumbers(CommandLine cmd) {
        if (cmd.getOptions().length == 0)
            printUsage();        
    }

    /*
     * extract fetched data.
     * if the direction is null, extract the file in default folder(today's folder)
     * else fetch the data from the directory specified by the first element, 
     * and store the result in the directory specified by the second element.
     */
    private static void reverbExtract(String[] dir) {
        
        ReverbNewsExtractor rne = new ReverbNewsExtractor(calendar, YAHOO_CONFIG_FILE);
        if(dir == null)
            rne.extract(null, null);
        else if(dir.length == 2 && dir[0] != null && dir[1] != null)
            rne.extract(dir[0], dir[1]);
        else
            printUsage();
    }
    

    /*
     * fetch rss from yahoo, store(and/or process it) and store in local file
     * if want to process the rss, pass in true as first argument
     * the second argument is the directory where the html is stored, if it's null, 
     * use today's directory
     */
    private static void fetchYahooRSS(boolean fetchData, boolean proc, String[] dirs) throws IOException {
        YahooRssScraper yrs = new YahooRssScraper(calendar);
        if(fetchData && !proc && dirs == null){//fetch data only
            yrs.scrape(true, false, null, null);
        }else if(fetchData && proc && dirs == null){//fetch data and then process it
            yrs.scrape(true, true, null, null);
        }else if(!fetchData && proc && dirs.length == 2 
                && dirs[0] != null && dirs[1] != null){//process data in the given dir
            
            yrs.scrape(false, true, dirs[0], dirs[1]);
        }else{
            printUsage();
        }
    }

    /*
     * setup the comman line options and parse passed in string array
     */
    private static CommandLine getCommands(String[] args, Options options) {
        
        Option fetchDataOnlyOp = new Option(SCRAPE_DATA_ONLY, false, 
                "fetch the rss(without processing it)");

        Option fecthDataAndProcessData = new Option(SCRAPE_DATA_AND_PROCESS_DATA, false, 
                "fetch rss, then process it");
        
        Option processWithDirOp = new Option(PROCESS_RSS_WITH_GIVEN_DIR, false, 
                "process rss only, the first arg is the source directory, the second dir is the target direcotory to save data");
        processWithDirOp.setArgs(2);
        
        Option useReverbOp = new Option(USE_REVERB, false, 
                "use reverb to extract today's file");
        
        Option useReverbWithDirOp = new Option(USE_REVERB_WITH_DIR, false,
                "use reverb to extract files in the first arg and save it into second arg");
        useReverbWithDirOp.setArgs(2);
        
        Option formaterOp = new Option(FORMAT_OPT, false,
                "format the reverb news database into human readable file.");
        
        Option formatTodayOp = new Option(FORMAT_TODAY, false,
                "format the today's file; this can not be used with " + FORMAT_TIME_FILTER);
//        formaterOp.setArgs(2);
        
        Option formatConfidenceThreshhold = new Option(FORMAT_CONFIDENCE_THRESHOLD, false, 
                "this option can not be used without " + FORMAT_OPT + " is being used. " +
                "This specify a the minimum confidence requirement, if not specified then a" +
                "a certain number of extraction will be selected");
        formatConfidenceThreshhold.setArgs(1);
        
        Option formatCategoryFilter = new Option(FORMAT_CATEGORY_FILTER, false, 
                "this option can not be used without " + FORMAT_OPT + " is being used. " +
                "specify the category nane; if not specified, all categories will be spcified");
        formatCategoryFilter.setArgs(1);
        
        Option formatTimeFilter = new Option(FORMAT_TIME_FILTER, false, 
                "this option can not be used without " + FORMAT_OPT + " is being used. " +
                "specify the time interval, the files that falls into this interval " +
                "will be formated(for eg: 2012-05-01 2012-05-04); if not specified, then the default one will be formated");
        formatTimeFilter.setArgs(2);
        
        Option formatDir = new Option(FORMAT_DIR, false, 
                "this option can not be used without " + FORMAT_OPT + " is being used. " +
                "specify the directory of source files and target file, " +
                "if the it's not secified, then use the default one");
        formatDir.setArgs(2);
        
        Option helpOp = new Option(HELP, false,
                "print program usage");
        
        options.addOption(fetchDataOnlyOp);
        options.addOption(fecthDataAndProcessData);
        options.addOption(processWithDirOp);
        options.addOption(useReverbOp);
        options.addOption(useReverbWithDirOp);
        options.addOption(formaterOp);
        options.addOption(formatTodayOp);
        options.addOption(formatConfidenceThreshhold);
        options.addOption(formatCategoryFilter);
        options.addOption(formatTimeFilter);
        options.addOption(formatDir);
        options.addOption(helpOp);
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            printUsage();
        }
        return cmd;
    }
    
    /*
     * print the option usage and exit
     */
    private static void printUsage(){
        HelpFormatter f = new HelpFormatter();
        f.printHelp("options:", options);
        System.exit(1);
    }
    
}
