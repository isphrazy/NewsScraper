package edu.washington.cs.NewsScraper;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class NewsScraperMain 
{
    public static void main( String[] args ) throws IOException
    {
    	String url = "http://www.cnn.com/2012/04/04/showbiz/whitney-houston-toxicology/index.html?eref=rss_topstories&utm_source=feedburner&utm_medium=feed&utm_campaign=Feed%3A+rss%2Fcnn_topstories+%28RSS%3A+Top+Stories%29";
        Document doc = Jsoup.connect(url).get();
        Elements content = doc.select("p.cnn_storypgraph2");
        System.out.println(content.text());
    }
   
}
