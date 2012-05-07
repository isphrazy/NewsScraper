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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;

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
    
    public ExtractedDataFormater(Calendar calendar, String configFileLocation){
        
        startDate = null;
        endDate = null;
        extractedDataSuffix = null;
        extractedDataDir = null;
        data = new ArrayList<FormattedNewsData>();
        
        loadConfig(configFileLocation);
    }

    public void format(String[] dir, String[] timeInterval,
            double confidenceThreshold, String category) {
        
        if(dir != null){
            if(dir[0] == null || dir[1] == null)
                throw new IllegalArgumentException("Illegal arguments: dir");
            else{
                srcDir = dir[0];
                targetDir = dir[1];
                if(!targetDir.endsWith("/"))
                    targetDir += "/";
                
                if(!srcDir.endsWith("/"))
                    srcDir += "/";
                
            }
        }
        
        if(timeInterval != null){
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
        }
        
        this.confidenceThreshold = confidenceThreshold;
        this.category = category;
        
        startFormatting();
    }
    
    private void startFormatting() {
        try {
            File srcFolder = new File(srcDir);
            String[] files = srcFolder.list();
            for(String file : files){
                String time = getFileTime(file);
                Date thisDate = dateFormat.parse(time);
                if(startDate == null || (!thisDate.after(endDate) && !thisDate.before(startDate))){
                    formatFile(srcDir, file);
                }
            }
        } catch (ParseException excp) {
            excp.printStackTrace();
        }
        
        sortData();
        
        outputData();
    }

    private void outputData() {
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetDir + "top_500.txt"),ENCODE));
            StringBuilder sb = new StringBuilder();
            int length = Math.min(MAX_OUTPUT_DATA, data.size());
            for(int i = 0; i < length; i++){
                sb.append(data.get(i).toString());
            }
            out.write(sb.toString());
            out.close();
        } catch (UnsupportedEncodingException excp) {
            // TODO Auto-generated catch block
            excp.printStackTrace();
        } catch (FileNotFoundException excp) {
            // TODO Auto-generated catch block
            excp.printStackTrace();
        } catch (IOException excp) {
            // TODO Auto-generated catch block
            excp.printStackTrace();
        }
    }

    private void sortData() {
        Collections.sort(data, new FormattedNewsDataComparator());
    }

    private String getFileTime(String fileName) {
        return fileName.substring(0, dateFormatStr.length());
    }

    /*
     * 
     */
    private void formatFile(String dir, String fileName) {
        File file = new File(dir + fileName);
        String fileContent = getContent(file);
        try {
            JSONObject jFile = new JSONObject(fileContent);
            String[] keys = JSONObject.getNames(jFile);
            for(String key : keys){
                JSONObject value = (JSONObject) jFile.get(key);
                JSONArray extractions = value.getJSONArray("extractions");
                for(int i = 0; i < extractions.length(); i++){
                    JSONObject extraction = (JSONObject) extractions.get(i);
//                    if(extraction.get("confidence"))
                    FormattedNewsData currentNewsData = new FormattedNewsData();
                    currentNewsData.id = Long.parseLong(key);
                    currentNewsData.title = value.getString("title");
                    currentNewsData.url = value.getString("url");
                    currentNewsData.date = value.getString("date");
                    currentNewsData.category = value.getString("category");
                    currentNewsData.arg1 = extraction.getString("arg1");
                    currentNewsData.arg2 = extraction.getString("arg2");
                    currentNewsData.relation = extraction.getString("relation");
                    currentNewsData.sentence = extraction.getString("sent");
                    currentNewsData.confidence = extraction.getLong("confidence");
                    data.add(currentNewsData);
                }
                
                
            }
        } catch (JSONException excp) {
            // TODO Auto-generated catch block
            excp.printStackTrace();
        }
//        Gson gson = new Gson();
//        HashMap<Long, ExtractedNewsData> map = gson.fromJson(fileContent, new TypeToken<HashMap<Long, ExtractedNewsData>>(){}.getType());
//        Iterator<Entry<Long, ExtractedNewsData>> it = map.entrySet().iterator();
//        while(it.hasNext()){
//            Map.Entry<Long, ExtractedNewsData> pairs = (Map.Entry<Long, ExtractedNewsData>)it.next();
//            ExtractedNewsData currentData = (ExtractedNewsData) pairs.getValue();
//            if(currentData.)
//        }
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
            double result = o1.confidence - o2.confidence;
            if(result < 0) return -1;
            if(result > 0) return 1;
            return 0;
        }
    }
}
