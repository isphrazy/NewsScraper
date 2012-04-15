package edu.washington.cs.NewsScraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class YahooRssScraper {
	
	private final String CONFIG_FILE_NAME = "YahooRssConfig";
	private final String JSON_BASE_URL = "rss-url";
	private final String JSON_CATEGORY_LIST = "category";
	private final String JSON_RSS_LIST = "rss-list";
	private final String JSON_RSS_PAGE_IDENTIFIER = "rss-page-identifier";
	private final String JSON_FOLDER_NAME = "folder-name";
	private final String JSON_SENTENCE_MINIMUM_LENGTH_REQUIREMENT = "sentence-minimum-length";
	private final String ID_COUNT_FILE_NAME = "idCount";
	
	private JsonObject configJO;
	private Calendar calendar;
	private String dateString;
	private ErrorMessagePrinter emp;
	private String baseURL;
	private String[] categoryList;
	private List<RssCategory> rssCategoryList;
	private String folderDirection;
	private String todayFolderLocation;
	private String rawDataDir;
	private Map<String, NewsData> dataMap;
	private int sentenceMinimumLengthRequirement;
	
	public YahooRssScraper(Calendar calendar){
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		this.calendar = calendar;
		dateString = dateFormat.format(calendar.getTime());
		
		rssCategoryList = new ArrayList<RssCategory>();
	}
	
	/**
	 * 
	 * @param processData if this is true, the data fetched online will be processed and stored
	 * to database, otherwise only the fetched data will be saved
	 */
	public void scrape(boolean processData){
		
		loadConfig();

//		fetchData();
		
//		if(processData)
			processHtml(todayFolderLocation + "raw_data/");
			
			parseData();
		
	}

	private void parseData() {
		int prevCount;
		int currentCount;
		File idCountFile = new File(ID_COUNT_FILE_NAME);
		try {
			Scanner sc = new Scanner(idCountFile);
			sc.nextInt();
			prevCount = sc.nextInt();
			currentCount = prevCount + 1;
		} catch (FileNotFoundException e) {
			emp.printLineMsg("" + this, "can't find idCount");
			currentCount = -1;
			prevCount = -1;
		}
		
		Map<Integer, NewsData> resultData = new HashMap<Integer, NewsData>();
		
		for(String title : dataMap.keySet()){
			resultData.put(currentCount++, dataMap.get(title));
		}
		
        try {
        	String dataLocation = todayFolderLocation + "data/";
        	File f = new File(dataLocation);
        	f.mkdir();
        	
        	String rssData = dataLocation + "yahoo_rss.data";
        	File dataFile = new File(rssData);
        	dataFile.createNewFile();
        	
        	FileWriter fstream = new FileWriter(rssData);
        	BufferedWriter out = new BufferedWriter(fstream);
			out.write(new Gson().toJson(resultData));
			out.close();
			
			FileWriter idCountStream = new FileWriter(ID_COUNT_FILE_NAME);
			BufferedWriter idOut = new BufferedWriter(idCountStream);
			idOut.write(prevCount + " " + currentCount);
			idOut.close();
		} catch (IOException e) {
			emp.printLineMsg("" + this, "write database to local file failed");
			e.printStackTrace();
		}
        
        
	}

	/*
	 * parse the files in given directory into map
	 * dir is the html files directory
	 */
	private void processHtml(String dir) {
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
							if(desc.text().length() > sentenceMinimumLengthRequirement){
								data.content = desc.text();
								dataMap.put(title, data);
							}
						}else{
							
							//length check
							if(para.text().length() > sentenceMinimumLengthRequirement){
								String content = para.text();
								//get rid of the "..." at the end of the content
								if(content.endsWith("..."))
									content = content.substring(0, content.length() - 3);
								int pubSep = content.indexOf('-');
								if(pubSep > 2 && pubSep < 30 && content.substring(pubSep - 2, pubSep + 2).equals(") - ")){
									content = content.substring(pubSep + 2);
								}
								data.content = content;
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
				}else{
					System.out.println(pubdate);
				}
			}
			System.out.println("process " + fileName + " successfully");
		}
	}


	/*
	 * fetch data online from yahoo rss.
	 */
	private void fetchData() {
		rawDataDir = todayFolderLocation + "raw_data/";
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
	}

	/*
	 * create directory for today's data
	 */
	private void makeTodayDirectory(String dataFolderLoction) {
		File folder = new File(dataFolderLoction);
		if(!folder.exists())
			folder.mkdir();
		emp = ErrorMessagePrinter.getInstance(dataFolderLoction);
		
		//if today's folder not exist, create one
		todayFolderLocation = dataFolderLoction + dateString + "/";
		File todayFolder = new File(todayFolderLocation);
		if(!todayFolder.mkdir()){
			emp.printLineMsg("" + this, "can't crate today's directory");
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
