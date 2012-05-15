package knowitall.reverbovernews;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.washington.cs.knowitall.extractor.R2A2;
import edu.washington.cs.knowitall.extractor.ReVerbExtractor;
import edu.washington.cs.knowitall.extractor.ReVerbRelationExtractor;
import edu.washington.cs.knowitall.nlp.OpenNlpSentenceChunker;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;
import edu.washington.cs.knowitall.util.DefaultObjects;

public class ReverbNewsExtractor {
    
    //private final String TARGET_DIR = "reverb_extracted";
    private final String ENCODE = "UTF-8";
    
    private String configFileName;
    private String dateString;
    private String rootDir;
    private String extractedDataSuffix;
    private String extractedDataDir;
    private Calendar calendar;
    private ErrorMessagePrinter emp;
    private ReVerbExtractor reverb;
//    private ReVerbRelationExtractor reverb;
    private Map<Long, ExtractedNewsData> data;
    private OpenNlpSentenceChunker chuncker;
    //this will be true when user are not extracting data for today's data
    private boolean ignoreDate;
    
    /**
     * constructor
     * @param calendar gives the time of the caller
     * @param configFileName tells the location of configuration file
     */
    public ReverbNewsExtractor(Calendar calendar, String configFileName){
        System.out.println("preparing for extracting news data");
        this.calendar = calendar;
        this.configFileName = configFileName;
        ignoreDate = false;
        reverb = new ReVerbExtractor();
//        reverb = new R2A2();
        data = new HashMap<Long, ExtractedNewsData>();
        try {
            chuncker = new OpenNlpSentenceChunker();
        } catch (IOException e) {
            emp.printLineMsg("" + this, "can't initialize sentence chunker");
            e.printStackTrace();
        }
    }
    
    /**
     * extract data
     * @param srcDir specify the location of source data, if null, then use today's location
     * @param targetDir specify where the result will be stored, if null, then put data in today's location
     */
    public void extract(String srcDir, String targetDir){
        
        loadConfig(configFileName);
        String location = null;
        if(srcDir == null && targetDir == null){//extract from default location
            location = rootDir + dateString + "/data/";
        }else if(srcDir != null && targetDir != null){
            location = srcDir;
        }else{
            throw new IllegalArgumentException("the arguments put into extract is wrong");
        }
        
        if(!location.endsWith("/")) location += "/";
        
        System.out.println("location: " + location);
        File dataFolder = new File(location);
        String[] dataFiles = dataFolder.list();
        for(String fileName : dataFiles){
            extractData(loadData(location + fileName));
            
            //start outputting data
            if(targetDir == null){
            	outputData(rootDir + "/" + extractedDataDir + "/");
            }else{
            	ignoreDate = true;
            	dateString = fileName.substring(0, 10);
            	if(!targetDir.endsWith("/")) targetDir += "/";
            	outputData(targetDir);
            }
        }
        
        
        System.out.println("extraction finished");
    }

    
    /*
     * parse the given newsData json string and extract the info in the string
     */
    private void extractData(String newsData) {
        System.out.println("start extracting data");
        Gson gson = new Gson();
        HashMap<Long, ExtractedNewsData> map = gson.fromJson(newsData, new TypeToken<HashMap<Long, ExtractedNewsData>>(){}.getType());
        Iterator<Entry<Long, ExtractedNewsData>> it = map.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Long, ExtractedNewsData> pairs = (Map.Entry<Long, ExtractedNewsData>)it.next();
            ExtractedNewsData currentData = (ExtractedNewsData) pairs.getValue();
            currentData.extractions = new HashMap<String, ChunkedBinaryExtraction>();
            reverbExtract(currentData, currentData.imgAlt);
            reverbExtract(currentData, currentData.imgTitle);
            reverbExtract(currentData, currentData.content);
            reverbExtract(currentData, currentData.title);
        }
        //add this map to the wholeData
        data.putAll(map);
    }

    /*
     * 
     */
    private void outputData(String targetDir) {
        System.out.println("start outputing data");
        
        File targetFolder = new File(targetDir);
        if(!targetDir.endsWith("/")) targetDir += "/";
        targetFolder.mkdirs();
        if (!targetFolder.exists()) emp.printLineMsg("" + this, "can't create folder");
        String jsonDataDir = targetDir + dateString + "_ExtractedData." + extractedDataSuffix;
//        String readableDataDir = targetDir + dateString + "_readable.txt";
        System.out.println("storing in " + jsonDataDir);
        File jsonDataFile = new File(jsonDataDir);
//        File readableFile = new File(readableDataDir);
        
        try {
            jsonDataFile.createNewFile();
            if(!jsonDataFile.exists()) emp.printLineMsg("" + this, "can't create output file data");
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jsonDataFile),ENCODE));
            Iterator<Entry<Long, ExtractedNewsData>> it = data.entrySet().iterator();
            
            //create json output string manually
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            String seperator = ", ";
            boolean empty = true;
            while(it.hasNext()){
                empty = false;
                Map.Entry<Long, ExtractedNewsData> pair = (Map.Entry<Long, ExtractedNewsData>)it.next();
                sb.append("\"" + pair.getKey() + "\": " + pair.getValue().toJsonString() + seperator);
            }
            if(!empty) sb.delete(sb.length() - seperator.length(), sb.length());
            sb.append("}");
            out.write(sb.toString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    /*
     * extract the given string, and store the extracted information into the given ExtractedNewsData
     */
    private void reverbExtract(ExtractedNewsData currentData, String str) {
        if(str != null && str.length() > 1){
            try {
//                BufferedReader br = new BufferedReader(new StringReader(str));
                String[] sentences = DefaultObjects.getDefaultSentenceDetector().sentDetect(str);
                
                for (String sent : sentences) {
                    for (ChunkedBinaryExtraction extr : reverb.extract(chuncker.chunkSentence(sent))) {
                        currentData.extractions.put(sent.toString(), extr);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /*
     * load file with given location, and return files content
     */
    private String loadData(String location) {
        System.out.println("loading data from: " + location);
        StringBuilder sb = new StringBuilder();
        Scanner sc = null;
        try {
            
            sc = new Scanner(new File(location), ENCODE);
            while(sc.hasNextLine()) sb.append(sc.nextLine());
        } catch (FileNotFoundException e) {
            emp.printLineMsg("" + this, "can't load data from loadData");
            e.printStackTrace();
        } finally{
            sc.close();
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
        dateString = config.getDateFormat().format(calendar.getTime());
        extractedDataSuffix = config.getExtractedDataSuffix();
        extractedDataDir = config.getExtractedDataDir();
    }
    
    
}
