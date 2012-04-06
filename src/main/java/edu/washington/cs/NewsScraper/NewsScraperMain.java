package edu.washington.cs.NewsScraper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;


public class NewsScraperMain 
{
    public static void main( String[] args ) throws IOException
    {
//    	fetchYahooRSS();
//    	readLocalFiles();
//    	String url = "http://www.cnn.com/2012/04/04/showbiz/whitney-houston-toxicology/index.html?eref=rss_topstories&utm_source=feedburner&utm_medium=feed&utm_campaign=Feed%3A+rss%2Fcnn_topstories+%28RSS%3A+Top+Stories%29";
//        Document doc = Jsoup.connect(url).get();
//        Elements content = doc.select("p.cnn_storypgraph2");
//        System.out.println(content.text());
    	count();
    	
    }
    
    @SuppressWarnings("null")
	private static void count() throws IOException{
    	
    	Map<String, String> titleToContent = new HashMap<String, String>();
    	File f = new File("exp");
    	String[] files = f.list();
    	for(int i = 0; i < files.length; i++){
    		
    		processHTML("exp/" + files[i], titleToContent);
    	}
    	

		Gson gson = new Gson();
		FileWriter fstream = new FileWriter("business.out");
		BufferedWriter out = new BufferedWriter(fstream);
        out.write(gson.toJson(titleToContent));
//        System.out.println();
        out.close();
    }
    
    private static void processHTML(String string, Map<String, String> titleToContent) throws IOException {
    	String input = readFile(string);
    	Document wholeHtml = Jsoup.parse(input);
    	
    	Elements items = wholeHtml.getElementsByTag("item");
		for(Element item : items){
			Element titleEle = item.getElementsByTag("title").first();
			String title = titleEle.text().trim();
			if(!titleToContent.containsKey(title)){
				Element desc = item.getElementsByTag("description").first();
				desc = Jsoup.parse(StringEscapeUtils.unescapeHtml4(desc.toString()));
				Element para = desc.getElementsByTag("p").first();
				String alt = null;
				if(para == null){
					alt = desc.text();
					titleToContent.put(title, alt);
				}else{
					try{
					alt = para.getElementsByTag("a").first().getElementsByTag("img").first().attr("alt");
					}catch (NullPointerException e){
						e.printStackTrace();
						alt="";
						
					}
					titleToContent.put(title, alt + "///" + para.text());
				}
			}
		}
	}

	private static String readFile(String name) throws IOException{
		System.out.println(name);
    	FileReader input = new FileReader(name);
		BufferedReader bufRead = new BufferedReader(input);
		StringBuilder contentBuilder = new StringBuilder();
		String line;
		while ((line = bufRead.readLine()) != null){
			contentBuilder.append(line);
		}
		return contentBuilder.toString();
    }

	private static void readLocalFiles() throws IOException {
		String dirName = "4-5-0145";
		File f = new File(dirName);
		String[] fileNames = f.list();
		for(int i = 0; i < 1; i++){
//			Scanner s = new Scanner(new File(dirName + "/" + fileNames[i]));
			FileReader input = new FileReader(dirName + "/" + fileNames[i]);
			BufferedReader bufRead = new BufferedReader(input);
			StringBuilder contentBuilder = new StringBuilder();
			String line;
			while ((line = bufRead.readLine()) != null){
				contentBuilder.append(line);
			}
			analysisYahooRSS(contentBuilder.toString());
//			System.out.println(contentBuilder.toString());
		}
	}
	
	private static void analysisYahooRSS(String input){
		Document wholeHtml = Jsoup.parse(input);
		Elements items = wholeHtml.getElementsByTag("item");
		for(Element item : items){
			Element titleEle = item.getElementsByTag("title").first();
			String title = titleEle.text().trim();
			System.out.println(title + ": " + createId(title));
		}
	}
	
	private static long createId(String title){
		long hash = 7;
		int prime = 31;
		for(int i = 0; i < title.length(); i++){
			hash = hash * prime + title.charAt(i);
		}
		return hash;
	}

	private static void fetchYahooRSS() throws IOException {
		String baseURL = "http://news.yahoo.com/rss/";
		String rssList[] = new String[]{"us", "business", "education", "stock-markets", 
										"entertainment", "sports", "tech", "science", 
										"health", "politics", "internet", "gaming", 
										"movies", "tv", "economy", "crime-trials"};
//		String rssList[] = new String[]{"us"};
		int length = rssList.length;
		
		DateFormat dateFormat = new SimpleDateFormat("MM-dd_HH");
		Date date = new Date();
		String hour = dateFormat.format(date);
		//make a folder with current time
		File newFolder = new File(hour);
		newFolder.mkdir();
		
		for(int i = 0; i < length; i++){
	        Document doc = Jsoup.connect(baseURL + rssList[i]).get();
	        
	        
	        FileWriter fstream = new FileWriter(hour + "/" + rssList[i] + "_" + ".html");
//	        FileWriter fstream = new FileWriter("1.html");
	        BufferedWriter out = new BufferedWriter(fstream);
	        out.write(doc.toString());
	        System.out.println(rssList[i] + " done!");
//	        System.out.println();
	        out.close();
		}
	}
   
}
