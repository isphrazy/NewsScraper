package knowitall.reverbovernews;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ExtractedDataFormater {
    
    private final int MAX_OUTPUT_DATA = 500;
    private final String ENCODE = "UTF-8";

    private Calendar calendar;
    private ErrorMessagePrinter emp;
    private String rootDir;
    private String dateFormatStr;
    private DateFormat dateFormat;
//    private String dateString;
    private String extractedDataSuffix;
    private String extractedDataDir;
    private String srcDir;
    private String targetDir;
    private Date startDate;
    private Date endDate;
    private String category;
    private double confidenceThreshold;
    private List<FormattedNewsData> data;
    private boolean allTime;
    private Set<String> duplicateChecker;
    private int totalCount = 0;
    private double totalConf = 0;
    /**
     * 
     * @param calendar
     * @param configFileLocation
     */
    public ExtractedDataFormater(Calendar calendar, String configFileLocation){
        
        startDate = null;
        endDate = null;
        extractedDataSuffix = null;
        extractedDataDir = null;
        allTime = false;
        data = new ArrayList<FormattedNewsData>();
        this.calendar = calendar;
        duplicateChecker = new HashSet<String>();
        loadConfig(configFileLocation);
    }

    
    /**
     * 
     * @param dir source and target directory; if it's null, then the default one will
     * be used.
     * @param timeInterval specify the interval of the time, the data fall into 
     * this date will be processed
     * @param confidenceThreshold specify the minimum confidence level; If not
     * specified(-1), all the data will be considered.
     * @param category specify the certain category will be specified. 
     * If not specified, then all the category will be considered
     */
    public void format(String[] dir, String[] timeInterval,
            double confidenceThreshold, String category, boolean formatToday) {
        
        if(dir != null){
            if(dir[0] == null || dir[1] == null)
                throw new IllegalArgumentException("Illegal arguments: dir");
            else{
                srcDir = dir[0];
                targetDir = dir[1];
                rootDir = "";
                if(!targetDir.endsWith("/"))
                    targetDir += "/";
                
                if(!srcDir.endsWith("/"))
                    srcDir += "/";
                
            }
        }
        
        if(formatToday){
            if(timeInterval != null) throw new IllegalArgumentException("either formtToday is true" +
            		" or timeInterval is null");
//            startDate = endDate = new Date()
        }else if(timeInterval != null){
            if(timeInterval[0] == null || timeInterval[1] == null)
                throw new IllegalArgumentException("Illegal arguments: timeInterval");
            else{
                try {
                    startDate = dateFormat.parse(timeInterval[0]);
                    endDate = dateFormat.parse(timeInterval[1]);
                } catch (ParseException excp) {
                    System.out.println("can't parse passed in date");
                    excp.printStackTrace();
                }
            }
        }else{
            allTime = true;
        }
        
        this.confidenceThreshold = confidenceThreshold;
        this.category = category;
//        for(this.confidenceThreshold = 0.95; this.confidenceThreshold > 0; this.confidenceThreshold -= 0.05){
        startFormatting();
//            totalCount = 0;
//            totalConf = 0;
//            duplicateChecker.clear();
//        }
    }
    
    /*
     * start formatting the data
     */
    private void startFormatting() {
        try {
            File srcFolder = new File(srcDir);
            if(!srcDir.endsWith("/")) srcDir += "/";
            String[] files = srcFolder.list();
            for(String file : files){
                //get the time of the file(parse the time in the file's name)
                String time = getFileTime(file);
                
                if(allTime)
                    formatFile(srcDir, file);
                else{
                    Date thisDate = dateFormat.parse(time);
                    if(!thisDate.after(endDate) && !thisDate.before(startDate))
                        formatFile(srcDir, file);
                }
                
            }
        } catch (ParseException excp) {
            excp.printStackTrace();
        }
        
//        sortData();
//        data = data.subList(0, MAX_OUTPUT_DATA);
        shuffleData();
//        System.out.println(this.confidenceThreshold + "\t" + totalCount + "\t" + totalConf / totalCount);
//        System.out.println("average conf: " + totalConf / totalCount);
        
//        
        outputData();
    }



    /*
     * output the data to disk
     */
    private void outputData() {
        try {
            String outputFolderDir = rootDir + targetDir;
            File outputLoc = new File(outputFolderDir);
            outputLoc.mkdirs();
            
            String outputFileName = outputFolderDir + "top_500.txt";
            
            File outputFile = new File(outputFileName);
            if(!outputFile.exists())
                outputFile.createNewFile();
            
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile),ENCODE));
            StringBuilder sb = new StringBuilder();
//            int length = Math.min(MAX_OUTPUT_DATA, data.size());
            int length = data.size();
            for(int i = 0; i < length; i++){
                sb.append(data.get(i).toString());
            }
            out.write(sb.toString());
            out.close();
        } catch (UnsupportedEncodingException excp) {
            excp.printStackTrace();
        } catch (FileNotFoundException excp) {
            excp.printStackTrace();
        } catch (IOException excp) {
            excp.printStackTrace();
        }
    }

    /*
     * sort the stored data
     */
    private void sortData() {
        Collections.sort(data, new FormattedNewsDataComparator());
    }
    
    /*
     * shuffle the stored data
     */
    private void shuffleData() {
//        Collections.shuffle(data);
        Collections.shuffle(data, new Random());
    }

    /*
     * get the time in the file name
     */
    private String getFileTime(String fileName) {
        return fileName.substring(0, dateFormatStr.length());
    }

    /*
     * convert data from 
     */
    private void formatFile(String dir, String fileName) {
        File file = new File(dir + fileName);
        String fileContent = getContent(file);
        try {
            JSONObject jFile = new JSONObject(fileContent);
            String[] keys = JSONObject.getNames(jFile);
            for(String key : keys){//each news
                JSONObject value = (JSONObject) jFile.get(key);
                JSONArray extractions = value.getJSONArray("extractions");
                String title = value.getString("title").trim();
                if(duplicateChecker.contains(title))
                    continue;
                duplicateChecker.add(title);
                long id = Long.parseLong(key);
                //each extraction for the news
                for(int i = 0; i < extractions.length(); i++){
                    JSONObject extraction = (JSONObject) extractions.get(i);
//                    if(extraction.get("confidence"))
                    FormattedNewsData currentNewsData = new FormattedNewsData();
                    currentNewsData.confidence = extraction.getDouble("confidence");
                    if(confidenceThreshold == -1 || currentNewsData.confidence > confidenceThreshold){
                        currentNewsData.id = id;
                        currentNewsData.title = title;
                        currentNewsData.url = value.getString("url");
                        currentNewsData.date = value.getString("date");
                        currentNewsData.category = value.getString("category");
                        currentNewsData.arg1 = extraction.getString("arg1");
                        currentNewsData.arg2 = extraction.getString("arg2");
                        currentNewsData.relation = extraction.getString("relation");
                        currentNewsData.sentence = extraction.getString("sent");
                        data.add(currentNewsData);
                        totalCount++;
                        totalConf += currentNewsData.confidence;
                    }
                }
                
                
            }
        } catch (JSONException excp) {
            excp.printStackTrace();
        }
    }

    /*
     * return the content of given file
     */
    private String getContent(File file) {
        StringBuilder sb = new StringBuilder();
        try {
            Scanner sc = new Scanner(file);
            while(sc.hasNextLine())
                sb.append(sc.nextLine());
            
        } catch (FileNotFoundException excp) {
            emp.printLineMsg("failed to load file: ", file.toString());
            excp.printStackTrace();
        }
        return sb.toString();
    }

    /*
     * load configuration file from given location and name
     */
    private void loadConfig(String location) {
        Config config = new Config();
        try {
            config.loadConfig(location);
        } catch (FileNotFoundException e) {
            
            e.printStackTrace();
        }
        emp = ErrorMessagePrinter.getInstance(config.getRootDir(), calendar);
        rootDir = config.getRootDir();
//        dateString = config.getDateFormat().format(calendar.getTime());
        dateFormat = config.getDateFormat();
        extractedDataSuffix = config.getExtractedDataSuffix();
        srcDir = rootDir + config.getExtractedDataDir();
        targetDir = config.getFormattedExtractedDataDir();
        dateFormatStr = config.getDateFormatString();
    }
    
    private class FormattedNewsDataComparator implements Comparator<FormattedNewsData> {
        
        public int compare(FormattedNewsData o1, FormattedNewsData o2) {
            double result = o2.confidence - o1.confidence;
            if(result < 0) return -1;
            if(result > 0) return 1;
            return 0;
        }
    }
}
