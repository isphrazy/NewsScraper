package knowitall.reverbovernews;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * this class fetch the rss data from yahoo, and instore the metadata to the database
 * @author Pingyang He
 *
 */
public class YahooRssScraper {
    
    private final String CONFIG_FILE_NAME = "YahooRssConfig";
    private final String JSON_BASE_URL = "rss-url";
    private final String JSON_CATEGORY_LIST = "category";
    private final String JSON_RSS_LIST = "rss-list";
    private final String JSON_SENTENCE_MINIMUM_LENGTH_REQUIREMENT = "sentence-minimum-length";
    private final String ID_COUNT_FILE_NAME = "idCount";
    private final String OUTPUT_DATABASE_NAME = "yahoo_rss.data";
    private final String TAG_LINK = "<link />";
    private final String TAG_SOURCE = "</source>";
    private final String REUTERS_KEYWORD = "(Reuters) - ";
    private final String HEALTHYDAY_KEYWORD = "(HealthDay News) -- ";
    private final String LINK_GARBAGE_TAIL = "\n";
    private final String GARBAGE_TAIL = "...";
    private final String USELESS_CONTENT_INDICATOR = "[...]";
    private final String[] ENDING_PUNCTUATION = {".", "?", "!", ".\"", "?\"", "!\""};
    private final String ENCODE = "UTF-8";
    private final String FOLDER_PATH_SEPERATOR = "/";
    
    private JsonObject configJO;
    private Calendar calendar;
    private String dateString;
    private ErrorMessagePrinter emp;
    private String baseURL;
    private List<RssCategory> rssCategoryList;
    private String outputLocation;
    private String rawDataDir;
    private Map<String, NewsData> dataMap;
//    private List<SimpleNewsData> simpleDataMap;
    private int sentenceMinimumLengthRequirement;
    private Set<String> duplicateChecker;
    private boolean ignoreDate;
    private DateFormat dateFormat;
    
    /**
     * constructor
     * @param calendar indicates the date of today
     */
    public YahooRssScraper(Calendar calendar){
        
        this.calendar = calendar;
        
        rssCategoryList = new ArrayList<RssCategory>();
        duplicateChecker = new HashSet<String>();
        ignoreDate = false;
        dataMap = new HashMap<String, NewsData>();
//        simpleDataMap = new ArrayList<SimpleNewsData>();
    }
    
    /**
     * 
     * @param fetchData indicates whether use wants to fetch data online
     * @param processData if this is true, the data fetched online will be processed and stored
     * to database, otherwise only the fetched data will be saved
     * @param sourceDir tells where the html that will be scraped is stored; if it's null, use today's directory
     * @targetDir where the processed data will be stored
     */
    public void scrape(boolean fetchData, boolean processData, String sourceDir, String targetDir){
        
        if(sourceDir != null && targetDir != null)
            ignoreDate = true;
        
        loadConfig();

        if(fetchData)
            fetchData();
        
        if(processData){
            if(sourceDir == null && targetDir == null)
                processHtml(rawDataDir);
            else if(sourceDir != null && targetDir != null){
                outputLocation = targetDir.trim();
                if(!outputLocation.endsWith(FOLDER_PATH_SEPERATOR))outputLocation += FOLDER_PATH_SEPERATOR;
                File locationFile = new File(outputLocation);
                locationFile.mkdirs();
                if(!locationFile.exists())
                    emp.printLineMsg("" + this, "failed to create target folder");
                
                processHtml(sourceDir);
            }else{
                throw new IllegalArgumentException();
            }
            outputDataBase();
        }
    }

    
    /*
     * output the map data to database in json format
     */
    private void outputDataBase() {
        System.out.println("start to output news data");
        
        //load id number from last time
        long prevCount;
        long currentCount;
        File idCountFile = new File(ID_COUNT_FILE_NAME);
        try {
            Scanner sc = new Scanner(idCountFile);
            sc.nextInt();
            prevCount = sc.nextLong();
            currentCount = prevCount + 1;
        } catch (FileNotFoundException e) {
            emp.printLineMsg("" + this, "can't find idCount");
            currentCount = -1;
            prevCount = -1;
        }
        
        try {

            String dataLocation = outputLocation + "data/";
            File f = new File(dataLocation);
            f.mkdirs();
            
            String rssData = dataLocation + dateString + "_" + OUTPUT_DATABASE_NAME;
            File dataFile = new File(rssData);
            dataFile.createNewFile();
            
            //not using JSON since converting json to string doesn't support unicode
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            String seperator = ", ";
            for(String title : dataMap.keySet()){
                sb.append("\"" + currentCount++ + "\": " + dataMap.get(title).toJsonString() + seperator);
            }
            //fense post problem
            if(!dataMap.isEmpty())
                sb.delete(sb.length() - seperator.length(), sb.length());
            
            sb.append("}");
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(rssData)),ENCODE));
            if(sb.length() < 100)
                emp.printLineMsg("" + this, "output data is too short");
            out.write(sb.toString());
            out.close();
            
        } catch (Exception e) {
            emp.printLineMsg("" + this, "fail to output data");
            e.printStackTrace();
        }
        
        
        //write the new id count to file
        FileWriter idCountStream;
        try {
            idCountStream = new FileWriter(ID_COUNT_FILE_NAME);
            BufferedWriter idOut = new BufferedWriter(idCountStream);
            idOut.write(prevCount + " " + currentCount);
            idOut.close();
        } catch (IOException e) {
            emp.printLineMsg("" + this, "can't increase id count");
            e.printStackTrace();
        }
        System.out.println("finished outputing news data");
        
    }

    /*
     * parse the files in given directory into map
     * dir is the html files directory
     */
    private void processHtml(String dir) {
        
        //make sure dir ends with '/'
        if(!dir.endsWith("/")) dir = dir + "/";
        
        System.out.println("start processing html");

        File rawDataFile = new File(dir);
        String[] files = rawDataFile.list();
        
        if(ignoreDate && files.length > 0){
            String fileDate = getFileDate(files[0]);
            if(fileDate != null)
                dateString = fileDate;
        }
        
        for(String fileName : files){
            System.out.print("process " + fileName);
            int timeSeperatorPos = fileName.indexOf('_');
            int catSeperatorPos = fileName.indexOf('_', timeSeperatorPos + 1);
            String categoryName = fileName.substring(timeSeperatorPos + 1, catSeperatorPos);
            String rssName = fileName.substring(catSeperatorPos + 1, fileName.indexOf('.'));
            
            //read rss file from local disk
            String fileContent = getFileContent(dir + fileName, ENCODE);
//            System.out.println("fileContent: " + fileContent);
            
            Document wholeHtml = Jsoup.parse(fileContent);
            
            //each item contains a news
            Elements items = wholeHtml.getElementsByTag("item");
            for(Element item : items){
                try{
                    String pubdate = item.getElementsByTag("pubdate").first().text();
                    if(checkDateMatch(pubdate) || ignoreDate){
                        
                        //get news' title
                        Element titleEle = item.getElementsByTag("title").first();
                        String title = titleEle.text().trim();
                        
                        //make sure no duplicate news
                        if(!dataMap.containsKey(title)){
                            
                            //make sure it's today's news
                            Element desc = item.getElementsByTag("description").first();
                            desc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(desc.toString()));
                            
                            Element para = desc.getElementsByTag("p").first();
//                        
                            NewsData data = new NewsData(categoryName, rssName, title, dateString);
                            getURL(item, data);
                            
                            getSource(item, data);
                            
                            getImageUrl(item, data);
                            
                            if(para == null){//description has no child tag "p"
                                
                                //length check
                                String descText = desc.text().trim();
                                descText = fixContent(descText);
                                if(descText == null) continue;
                                if(descText.length() > sentenceMinimumLengthRequirement 
                                        && !duplicateChecker.contains(descText)){
                                    duplicateChecker.add(descText);
                                    data.content = descText;
                                    dataMap.put(title, data);
                                }
                            }else{
                                
                                //length check
                                String paraText = para.text().trim();
                                if(paraText.length() > sentenceMinimumLengthRequirement){
                                    paraText = fixContent(paraText);
                                    if(paraText == null) continue;
                                    if(duplicateChecker.contains(paraText))    continue;
                                    duplicateChecker.add(paraText);
                                    data.content = paraText;
                                }
                                
                                try{
                                    //process image info
                                    Element img = para.getElementsByTag("a").first().getElementsByTag("img").first();
                                    if(data.imgUrl.length() < 1)
                                        data.imgUrl = img.attr("src");
                                    String imgAlt = img.attr("alt").trim();
                                    if(imgAlt.length() > sentenceMinimumLengthRequirement
                                            && !duplicateChecker.contains(imgAlt)){
                                        data.imgAlt = imgAlt;
                                        duplicateChecker.add(imgAlt);
                                    }
                                    
                                    String imgTitle = img.attr("title");
                                    if(imgTitle.length() > sentenceMinimumLengthRequirement
                                            && !duplicateChecker.contains(imgTitle)){
                                        data.imgTitle = img.attr("title");
                                        duplicateChecker.add(imgTitle);
                                    }
                                }catch (NullPointerException e){
                                    System.out.println(categoryName + ": " + rssName + ": " + title + " ----- has no image");
                                }
                                dataMap.put(title, data);
//                                simpleDataMap.add(sData);
                            }
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    emp.printLineMsg("" + this + ": " + categoryName + " " + rssName, e.getMessage());
                }
            }
            System.out.println(" successfully");
        }
        System.out.println("end processing html");
    }

    /*
     * check if the given date is the same as dateString
     */
    private boolean checkDateMatch(String pubdate) {
        if(pubdate.substring(0, 10).equals(dateString)){
            return true;
        }
        DateFormat formatter = new SimpleDateFormat("");
        try {
            Date d = dateFormat.parse(dateString);
            int dayPos = pubdate.indexOf(' ');
            int monthPos = pubdate.indexOf(' ', dayPos + 1);
            int yearPos = pubdate.indexOf(' ', monthPos + 1);
            int endPos = pubdate.indexOf(' ', yearPos + 1);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            DateFormat monthFormat = new SimpleDateFormat("MMM", Locale.ENGLISH);
            Date dm = monthFormat.parse(pubdate.substring(monthPos + 1, yearPos));
            if(c.get(c.DAY_OF_MONTH) == Integer.parseInt(pubdate.substring(dayPos + 1, monthPos))
                    &&c.get(c.MONTH) == dm.getMonth()
                    &&c.get(c.YEAR) == Integer.parseInt(pubdate.substring(yearPos + 1, endPos))){
                return true;
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }
    

    private String getFileDate(String fileName) {
        try{
            return fileName.substring(0, 10);
        }catch (Exception e){
            return null;
        }
    }

    private void getImageUrl(Element item, NewsData data) {
        Elements allElements = item.getAllElements();
        for(Element element : allElements){
            if(element.tagName().equals("media:text")){
                Element content = Jsoup.parse(StringEscapeUtils.unescapeHtml4(element.toString()));
                Elements allContent = content.getAllElements();
                for(Element c : allContent){
                    if(c.tagName().equals("img")){
                        String imageUrl = element.attr("src");
                        if(imageUrl != null && imageUrl.length() > 0 ){
                            System.out.println(imageUrl);
                            data.imgUrl = imageUrl;
                            break;
                        }
                    }                    
                }
            }
        }
    }

    /*
     * gets the source of given ite, then store it in data
     */
    private void getSource(Element item, NewsData data) {
        try{
            String source = item.getElementsByTag("source").first().text();
            
            if(source.length() < 1){
                String itemText = item.html();
                int sourceTagPos = itemText.indexOf(TAG_SOURCE);
                int sourceEndPos = itemText.indexOf('<', sourceTagPos + 1);
                if(sourceTagPos >= 0 && sourceEndPos >= 0)
                    source = removeNewLineTail(itemText.substring(sourceTagPos + TAG_SOURCE.length(), sourceEndPos));
            }
            
            data.source = source;
        }catch (Exception e){
            return;
        }
        
    }

    /*
     * gets the url of the given item, and stores it in data
     */
    private void getURL(Element item, NewsData data) {
        try{
            String url = item.getElementsByTag("link").first().text().trim();
            if(url.length() < 1){
                String itemText = item.html();
                int linkTagPos = itemText.indexOf(TAG_LINK);
                int linkEndPos = itemText.indexOf('<', linkTagPos + 1);
                if(linkTagPos >= 0 && linkEndPos >= 0)
                    url = removeNewLineTail(itemText.substring(linkTagPos + TAG_LINK.length(), linkEndPos));
            }
            data.url = url.trim();
        }catch (Exception e){
            return;
        }
    }
    
    /*
     * remove the new line character at the end of the given string
     */
    private String removeNewLineTail(String str){
        if(str.endsWith(LINK_GARBAGE_TAIL)) 
            return str.substring(0, str.length() - LINK_GARBAGE_TAIL.length()).trim();
        return str;
    }

    /*
     * get rid of useless information in a paragraph, if the whole paragraph is 
     * useless, return null 
     */
    private String fixContent(String paraText) {
        
        if(paraText.endsWith(USELESS_CONTENT_INDICATOR)) return null;    

        //get rid of the leading publisher info
        int pubSep = paraText.indexOf(REUTERS_KEYWORD);
        if(pubSep >= 0) paraText = paraText.substring(pubSep + REUTERS_KEYWORD.length());
        
        int HealthyDayPos = paraText.indexOf(HEALTHYDAY_KEYWORD);
        if(HealthyDayPos >= 0) paraText = paraText.substring(HealthyDayPos + HEALTHYDAY_KEYWORD.length());
        
        if(paraText.endsWith(GARBAGE_TAIL)){
            //get rid of the "..." at the end of the content
            paraText = paraText.substring(0, paraText.length() - 3).trim();
            for(int i = 0; i < ENDING_PUNCTUATION.length; i++){
                if(paraText.endsWith(ENDING_PUNCTUATION[i])) return paraText;
            }
        }else
            return paraText;
        
        return null;
    }

    /*
     * fetch data online from yahoo rss.
     */
    private void fetchData() {
        System.out.println("start fectching data");
        File rawDir = new File(rawDataDir);
        rawDir.mkdirs();
        for(int i = 0; i < rssCategoryList.size(); i++){
            RssCategory rCat = rssCategoryList.get(i);
            for(int j = 0; j < rCat.rssList.length; j++){
                String rssName = rCat.rssList[j];
                try {
                    Document doc = Jsoup.connect(baseURL + rssName).get();
                    
                    //write fetched xml to local data
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(rawDataDir + dateString + "_" + rCat.categoryName + "_" + rssName + ".html"), true),ENCODE));
                    out.write(doc.toString());
                    out.close();
                    
                    System.out.println("fetch " + rCat.categoryName + ": " + rssName + " successfully");
                    
                } catch (IOException e) {
                    emp.printLineMsg("" + this, "can not download: " + rCat.categoryName + "_" + rssName);
                    e.printStackTrace();
                }
            }
        }
        System.out.println("done fetching");
    }

    
    /*
     * read the yahoo configuration file and load it into
     */
    private void loadConfig() {
        System.out.println("loading configuration file");
        
        Config config = new Config();
        
        try {
            config.loadConfig(CONFIG_FILE_NAME);
        } catch (FileNotFoundException e) {
            emp.printLineMsg("" + this, "failed to load from configuration file");
            e.printStackTrace();
        }
        configJO = config.getConfig();
//        String configFile = getFileContent(CONFIG_FILE_NAME, "ascii");
//        configJO = (JsonObject)(new JsonParser()).parse(configFile);

        //load date format
//        DateFormat dateFormat = new SimpleDateFormat(configJO.get(JSON_DATE_FORMAT).getAsString());
        dateFormat = config.getDateFormat();
        dateString = dateFormat.format(calendar.getTime());
        emp = ErrorMessagePrinter.getInstance(config.getRootDir(), calendar);
        
        //if data folder not exist, create one
        if(!ignoreDate)
            makeTodayDirectory(config.getRootDir());
        
        //get base url
        baseURL = configJO.get(JSON_BASE_URL).getAsString();
        System.out.println("URL used: " + baseURL);
        
        //get the category list
        JsonArray categoryJA = configJO.get(JSON_CATEGORY_LIST).getAsJsonArray();
        for(int i = 0; i < categoryJA.size(); i++){
            rssCategoryList.add(new RssCategory(categoryJA.get(i).getAsString()));
        }
        
        //load rsslist
        JsonObject rssList = (JsonObject) configJO.get(JSON_RSS_LIST);
        for(int i = 0; i < rssCategoryList.size(); i++){
            RssCategory rc = rssCategoryList.get(i);
            String categoryName = rc.categoryName;
            rc.rssList = new Gson().fromJson(rssList.get(categoryName), String[].class);
        }
        sentenceMinimumLengthRequirement = configJO.get(JSON_SENTENCE_MINIMUM_LENGTH_REQUIREMENT).getAsInt();
        rawDataDir = outputLocation + "raw_data/";
        
        
    }

    /*
     * create directory for today's data
     */
    private void makeTodayDirectory(String dataFolderLoction) {
        
        File folder = new File(dataFolderLoction);
        if(!folder.exists())
            folder.mkdir();
                
        //if today's folder not exist, create one
        outputLocation = dataFolderLoction + dateString + "/";
        File todayFolder = new File(outputLocation);
        todayFolder.mkdir();
        
        //if the folder is not created
        if(!todayFolder.exists()){
            
            emp.printLineMsg("" + this, "can't crate today's directory");
            System.exit(1);
        }
        
        
    }

    /*
     * load file to a string then return it 
     */
    private String getFileContent(String fileName, String encode) {
        StringBuilder sb = new StringBuilder();
        try {

            Scanner configScanner = new Scanner(new File(fileName), encode);
            while(configScanner.hasNextLine()){
                sb.append(configScanner.nextLine());
            }
            
            configScanner.close();
            
        } catch (FileNotFoundException e) {
            emp.printLineMsg("" + this, "can not load file: " + fileName);
            e.printStackTrace();
            return null;
        }
        return sb.toString();
    }
    
    /*
     * contains the category name and rss list
     * eg: categoryName:ENTERTAINMENT, rssList:
     */
    private class RssCategory{
        public String categoryName;
        public String[] rssList;
        
        public RssCategory(String categoryName){
            this.categoryName = categoryName;
        }
    }
    
}
