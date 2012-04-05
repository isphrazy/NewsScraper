package edu.washington.cs.NewsScraper;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class NewsScraperMain 
{
    public static void main( String[] args ) throws IOException
    {
//        System.out.println( "Hello World!" );
    	String url = "http://www.cnn.com/2012/04/04/showbiz/whitney-houston-toxicology/index.html";
        Document doc = Jsoup.connect(url).get();
        System.out.println(doc.title());
    }
   
}
