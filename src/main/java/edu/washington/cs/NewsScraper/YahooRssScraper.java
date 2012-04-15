package edu.washington.cs.NewsScraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import com.google.gson.JsonParser;

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
	private final String JSON_FOLDER_NAME = "folder-name";
	private final String JSON_SENTENCE_MINIMUM_LENGTH_REQUIREMENT = "sentence-minimum-length";
	private final String ID_COUNT_FILE_NAME = "idCount";
	private final String OUTPUT_DATABASE_NAME = "yahoo_rss.data";
	
	private JsonObject configJO;
	private Calendar calendar;
	private String dateString;
	private ErrorMessagePrinter emp;
	private String baseURL;
	private List<RssCategory> rssCategoryList;
	private String todayFolderLocation;
	private String rawDataDir;
	private Map<String, NewsData> dataMap;
	private int sentenceMinimumLengthRequirement;
	private Set<String> duplicateChecker;
	
	/**
	 * constructor
	 * @param calendar indicates the date of today
	 */
	public YahooRssScraper(Calendar calendar){
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		this.calendar = calendar;
		dateString = dateFormat.format(calendar.getTime());
		
		rssCategoryList = new ArrayList<RssCategory>();
		duplicateChecker = new HashSet<String>();
	}
	
	/**
	 * 
	 * @param processData if this is true, the data fetched online will be processed and stored
	 * to database, otherwise only the fetched data will be saved
	 * @param dir tells where the html will be scraped is stored; if it's null, use today's directory
	 */
	public void scrape(boolean processData, String dir){
		
		loadConfig();

		if(dir == null)
			fetchData();
		
		if(processData){
			if(dir == null)
				processHtml(rawDataDir);
			else
				processHtml(dir);
			
			outputDataBase();
		}
		
		
	}

	
	/*
	 * output the map data to database in json format
	 */
	private void outputDataBase() {
		System.out.println("start output data");
		
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
		
		//put id number as key
		Map<Long, NewsData> resultData = new HashMap<Long, NewsData>();
		for(String title : dataMap.keySet()){
			resultData.put(currentCount++, dataMap.get(title));
		}
		
		//output json data
        try {
        	String dataLocation = todayFolderLocation + "data/";
        	File f = new File(dataLocation);
        	f.mkdir();
        	
        	String rssData = dataLocation + dateString + "_" + OUTPUT_DATABASE_NAME;
        	File dataFile = new File(rssData);
        	dataFile.createNewFile();
        	
        	FileWriter fstream = new FileWriter(rssData);
        	BufferedWriter out = new BufferedWriter(fstream);
			out.write(new Gson().toJson(resultData));
			out.close();
			System.out.println("database write successfully");
			
		} catch (IOException e) {
			emp.printLineMsg("" + this, "write database to local file failed");
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
        
	}

	/*
	 * parse the files in given directory into map
	 * dir is the html files directory
	 */
	private void processHtml(String dir) {
		
		//make sure dir ends with '/'
		if(!dir.endsWith("/")) dir = dir + "/";
		
		System.out.println("start processing html");
		dataMap = new HashMap<String, NewsData>();
		File rawDataFile = new File(dir);
		String[] files = rawDataFile.list();
		for(String fileName : files){
			int seperatorPos = fileName.indexOf('_');
			String categoryName = fileName.substring(0, seperatorPos);
			String rssName = fileName.substring(seperatorPos + 1, fileName.indexOf('.'));
			
			//read rss file from local disk
			String fileContent = getFileContent(dir + fileName);
			
	    	Document wholeHtml = Jsoup.parse(fileContent);
	    	
			//each item contains a news
	    	Elements items = wholeHtml.getElementsByTag("item");
			for(Element item : items){
				
				String pubdate = item.getElementsByTag("pubdate").first().text();
				if(pubdate.substring(0, 10).equals(dateString)){
					
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
						if(para == null){//description has no child tag "p"
							
							//length check
							String descText = desc.text().trim();
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
								if(duplicateChecker.contains(paraText))	continue;
								duplicateChecker.add(paraText);
								//get rid of the "..." at the end of the content
								if(paraText.endsWith("..."))
									paraText = paraText.substring(0, paraText.length() - 3);
								int pubSep = paraText.indexOf("(Reuters) - ");
								if(pubSep > 0) paraText = paraText.substring(pubSep + 11);
								data.content = paraText;
							}
							
							try{
								//process image info
								Element img = para.getElementsByTag("a").first().getElementsByTag("img").first();
								
								if(img.attr("alt").length() > sentenceMinimumLengthRequirement)
									data.imgAlt = img.attr("alt");
								if(img.attr("title").length() > sentenceMinimumLengthRequirement)
									data.imgTitle = img.attr("title");
							}catch (NullPointerException e){
								System.out.println(categoryName + ": " + rssName + ": " + title + " has no image");
							}
							dataMap.put(title, data);
						}
					}
				}
			}
			System.out.println("process " + fileName + " successfully");
		}
		System.out.println("end processing html");
	}


	/*
	 * fetch data online from yahoo rss.
	 */
	private void fetchData() {
		
		File rawDir = new File(rawDataDir);
		rawDir.mkdir();
		for(int i = 0; i < rssCategoryList.size(); i++){
			RssCategory rCat = rssCategoryList.get(i);
//			for(String rssName : rCat.rssList.keySet()){
			for(int j = 0; j < rCat.rssList.length; j++){
				String rssName = rCat.rssList[j];
				try {
					Document doc = Jsoup.connect(baseURL + rssName).get();
//					rCat.rssList.put(rssName, doc);
					
					//write fetched xml to local data
					FileWriter fstream = new FileWriter(rawDataDir + rCat.categoryName + "_" + rssName + ".html", true);
					BufferedWriter out = new BufferedWriter(fstream);
			        out.write(doc.toString());
			        out.close();
			        
			        System.out.println(rCat.categoryName + ": " + rssName + " fetched successfully");
			        
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
		
		String configFile = getFileContent(CONFIG_FILE_NAME);
		configJO = (JsonObject)(new JsonParser()).parse(configFile);
		
		//if data folder not exist, create one
		makeTodayDirectory(configJO.get(JSON_FOLDER_NAME).getAsString());
		
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
		rawDataDir = todayFolderLocation + "raw_data/";
	}

	/*
	 * create directory for today's data
	 */
	private void makeTodayDirectory(String dataFolderLoction) {
		File folder = new File(dataFolderLoction);
		if(!folder.exists())
			folder.mkdir();
		emp = ErrorMessagePrinter.getInstance(dataFolderLoction, calendar);
		
		//if today's folder not exist, create one
		todayFolderLocation = dataFolderLoction + dateString + "/";
		File todayFolder = new File(todayFolderLocation);
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
	private String getFileContent(String fileName) {
		StringBuilder sb = new StringBuilder();
		try {
			Scanner configScanner = new Scanner(new File(fileName));
			while(configScanner.hasNextLine()){
				sb.append(configScanner.nextLine());
			}
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
//		public Map<String, Document> rssList = new TreeMap<String, Document>();
		public String[] rssList;
		
		public RssCategory(String categoryName){
			this.categoryName = categoryName;
		}
	}
	
	/*
	 * The schema of the data that will be stored in database
	 */
	private class NewsData{
		public String title;
		public String date;
		public String imgAlt;
		public String imgTitle;
		public String content;
		public String category;
		public String subCategory;
		
		public NewsData(String category, String subCategory, String title, String date){
			imgAlt = "";
			content = "";
			imgTitle = "";
			this.category = category;
			this.subCategory = subCategory;
			this.title = title;
			this.date = date;
		}
		
	}
	
	
	
	
}
