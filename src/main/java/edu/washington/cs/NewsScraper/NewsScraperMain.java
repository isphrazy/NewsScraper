package edu.washington.cs.NewsScraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;


public class NewsScraperMain 
{
    public static void main( String[] args ) throws IOException
    {
//    	fetchYahooRSS();
    	readLocalFiles();
//    	String url = "http://www.cnn.com/2012/04/04/showbiz/whitney-houston-toxicology/index.html?eref=rss_topstories&utm_source=feedburner&utm_medium=feed&utm_campaign=Feed%3A+rss%2Fcnn_topstories+%28RSS%3A+Top+Stories%29";
//        Document doc = Jsoup.connect(url).get();
//        Elements content = doc.select("p.cnn_storypgraph2");
//        System.out.println(content.text());
    	
    	
    }

	private static void readLocalFiles() {
		File f = new File("4-5-0145");
		String[] fileNames = f.list();
		for(int i = 0; i < fileNames.length; i++){
			
		}
	}

	private static void fetchYahooRSS() throws IOException {
		String baseURL = "http://news.yahoo.com/rss/";
		String rssList[] = new String[]{"us", "business", "education", "stock-markets", 
										"entertainment", "sports", "tech", "science", 
										"health", "politics", "internet", "gaming", 
										"movies", "tv", "economy", "crime-trials"};
//		String rssList[] = new String[]{"us"};
		int length = rssList.length;
		for(int i = 0; i < length; i++){
	        Document doc = Jsoup.connect(baseURL + rssList[i]).get();
	        
	        DateFormat dateFormat = new SimpleDateFormat("HH_mm_ss");
	        Date date = new Date();
	        
	        FileWriter fstream = new FileWriter(rssList[i] + "_" + dateFormat.format(date) + ".html");
//	        FileWriter fstream = new FileWriter("1.html");
	        BufferedWriter out = new BufferedWriter(fstream);
	        out.write(doc.toString());
	        System.out.println(rssList[i] + " done!");
//	        System.out.println();
	        out.close();
		}
	}
   
}
