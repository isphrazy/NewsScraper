package knowitall.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import knowitall.reverbovernews.Config;
import knowitall.reverbovernews.ExtractedNewsData;

public class StatisticsGenerator {
    
    private static List<String> categoryList;
    private static Map<String, DayOfWeekData> dayOfWeekMap;
    private static String[] dayOfWeek;
    private static Set<String> duplicateChecker;
    
    public static void main(String[] args){
		
        initVar();
        
        loadConfig();
        
	    generateStat();
		
	}


    private static void initVar() {
        categoryList = new ArrayList<String>();
        dayOfWeekMap = new HashMap<String, DayOfWeekData>();
        dayOfWeek = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        duplicateChecker = new HashSet<String>();
    }


    private static void generateStat() {
		
	    String dataFolder = "yahoo_data/extracted_data/";
	    String outputStaticFolder = "yahoo_data/statics/";
	    
	    File dataFolderFile = new File(dataFolder);
	    
	    for(String fileName : dataFolderFile.list()){
	         
	        processFile(dataFolder + fileName);
	        
	    }
	    
	    outputStat();
	}
    
    private static void outputStat() {
        
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("yahoo_data/statics/statics.txt")),"UTF-8"));
            out.write(statisticsToSting());
            out.close();
        } catch (IOException excp) {
            excp.printStackTrace();
        }
    }
    
    private static String statisticsToSting(){
        StringBuilder sb = new StringBuilder();
        //print category:
        for(int i = 0; i < categoryList.size(); i++){
            sb.append(categoryList.get(i) + "\t");
        }
        sb.append("\n");
        for(String d : dayOfWeekMap.keySet()){
            DayOfWeekData data = dayOfWeekMap.get(d);
            sb.append(d + "\t" + data.totalCount + "\t" + data.extractionCount);
            for(int i = 0; i < categoryList.size(); i++){
                sb.append("\t" + data.categoryCount.get(categoryList.get(i)));
            }
            sb.append("\n");
        }
        return sb.toString();
    }


    private static void processFile(String fileDir) {
        String content = FileLoader.loadFile(fileDir);
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date date = null; 
        try {
            JSONObject jContent = new JSONObject(content);
            JSONArray ja = jContent.names();
            for(int i = 0; i < ja.length(); i++){
                JSONObject news = (JSONObject) jContent.get(ja.getString(i));
                if (!duplicateChecker.contains(news.getString("title").trim())){
                    duplicateChecker.add(news.getString("title").trim());
                    
                    date = (Date)formatter.parse(news.getString("date")); 
                    Calendar cal = new GregorianCalendar();
                    cal.setTime(date);
                    int weekday = cal.get(Calendar.DAY_OF_WEEK);
                    
                    DayOfWeekData thatDay = dayOfWeekMap.get(dayOfWeek[weekday - 1]);
                    thatDay.totalCount ++;
                    
                    JSONArray extractions = news.getJSONArray("extractions");
                    thatDay.extractionCount += extractions.length();
                    
                    String category = news.getString("category");
                    thatDay.categoryCount.put(category, thatDay.categoryCount.get(category) + 1);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }



    private static void loadConfig() {
        Config config = new Config();
        try {
            config.loadConfig("YahooRssConfig");
            categoryList = config.getCategory();
            for(int i = 0; i < dayOfWeek.length; i++){
                dayOfWeekMap.put(dayOfWeek[i], new DayOfWeekData());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private static class DayOfWeekData{
        int totalCount;
        int extractionCount;
        Map<String, Integer> categoryCount;
        
        public DayOfWeekData(){
            totalCount = 0;
            categoryCount = new HashMap<String, Integer>();
            for(int i = 0; i < categoryList.size(); i++){
                categoryCount.put(categoryList.get(i), 0);
            }
        }
        
    }
}
