package edu.washington.cs.NewsScraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
	
	private String date;
	private ErrorMessagePrinter emp;
	private String baseURL;
	private String[] categoryList;
	private List<RssCategory> rssCategoryList;
	private String folderDirection;
	private String todayFolderLocation;
	
	public YahooRssScraper(String date){
		this.date = date;
		
		rssCategoryList = new ArrayList<RssCategory>();
	}
	
	public void scrape(){
		
		loadConfig();

		fetchData();
		
		processData();
		
	}

	private void processData() {
		
	}

	private void fetchData() {
		String rawDataDir = todayFolderLocation + "raw_data/";
		File rawDir = new File(rawDataDir);
		rawDir.mkdir();
		for(int i = 0; i < rssCategoryList.size(); i++){
			RssCategory rCat = rssCategoryList.get(i);
			for(int j = 0; j < rCat.rssList.length; j++){
				String rssName = rCat.rssList[j];
				try {
					Document doc = Jsoup.connect(baseURL + rssName).get();
					FileWriter fstream = new FileWriter(rawDataDir + rCat.categoryName + "_" + rssName + ".data", true);
					BufferedWriter out = new BufferedWriter(fstream);
			        out.write(doc.toString());
			        out.close();
			        
				} catch (IOException e) {
					emp.printLineMsg("" + this, "can not download: " + rCat.categoryName + "_" + rssName);
					e.printStackTrace();
				}
			}
		}
		System.out.println("done");
	}

	/*
	 * read the yahoo configuration file and load it into
	 */
	private void loadConfig() {
		
		String configFile = getFileContent(CONFIG_FILE_NAME);
		
			
		JsonParser jp = new JsonParser();
		JsonObject configJO = (JsonObject)jp.parse(configFile);
		
		
		//if data folder not exist, create one
		makeDirectory(configJO.get(JSON_FOLDER_NAME).getAsString());
		
		
		
		//get base url
		baseURL = configJO.get(JSON_BASE_URL).getAsString();
		System.out.println("URL used: " + baseURL);
		
		//get the category list
		JsonArray categoryJA = configJO.get(JSON_CATEGORY_LIST).getAsJsonArray();
		for(int i = 0; i < categoryJA.size(); i++){
			rssCategoryList.add(new RssCategory(categoryJA.get(i).getAsString()));
		}
		
		
		Gson gson = new Gson();
		JsonObject rssList = (JsonObject) configJO.get(JSON_RSS_LIST);
		for(int i = 0; i < rssCategoryList.size(); i++){
			RssCategory rc = rssCategoryList.get(i);
			String categoryName = rc.categoryName;
			rc.rssList = gson.fromJson(rssList.get(categoryName), String[].class);
		}
	}

	private void makeDirectory(String dataFolderLoction) {
		File folder = new File(dataFolderLoction);
		if(!folder.exists())
			folder.mkdir();
		emp = ErrorMessagePrinter.getInstance(dataFolderLoction);
		
		//if today's folder not exist, create one
		todayFolderLocation = dataFolderLoction + date + "/";
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
			emp.printLineMsg("" + this, "can not load yahoo configuration file");
			e.printStackTrace();
			return null;
		}
		return sb.toString();
	}
	
	/*
	 * contains the category name and rss list
	 * eg: 
	 */
	private class RssCategory{
		public String categoryName;
		public String[] rssList;
		
		public RssCategory(String categoryName){
			this.categoryName = categoryName;
		}
	}
	
	
}
