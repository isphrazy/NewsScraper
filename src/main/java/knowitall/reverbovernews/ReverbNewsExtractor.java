package knowitall.reverbovernews;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import edu.washington.cs.knowitall.argumentidentifier.ConfidenceMetric;
import edu.washington.cs.knowitall.commonlib.Range;
import edu.washington.cs.knowitall.extractor.R2A2;
import edu.washington.cs.knowitall.extractor.ReVerbExtractor;
import edu.washington.cs.knowitall.extractor.ReVerbRelationExtractor;
import edu.washington.cs.knowitall.extractor.conf.ConfidenceFunction;
import edu.washington.cs.knowitall.extractor.conf.ConfidenceFunctionException;
import edu.washington.cs.knowitall.extractor.conf.ReVerbConfFunction;
import edu.washington.cs.knowitall.nlp.ChunkedSentence;
import edu.washington.cs.knowitall.nlp.ChunkedSentenceReader;
import edu.washington.cs.knowitall.nlp.OpenNlpSentenceChunker;
import edu.washington.cs.knowitall.nlp.extraction.ChunkedBinaryExtraction;
import edu.washington.cs.knowitall.normalization.BinaryExtractionNormalizer;
import edu.washington.cs.knowitall.normalization.NormalizedBinaryExtraction;
import edu.washington.cs.knowitall.util.DefaultObjects;

public class ReverbNewsExtractor {
	
//	private final String CONFIG_FILE_NAME = "YahooRssConfig";
	private final String TARGET_DIR = "reverb_extracted";
	private final String ENCODE = "UTF-8";
	
	private String configFileName;
	private String dateString;
	private String rootDir;
	private Calendar calendar;
	private ErrorMessagePrinter emp;
//	private ConfidenceFunction confFunc;
	private ReVerbExtractor reverb;
	private ReVerbConfFunction confFunc;
	private Map<Long, ExtractedNewsData> data;
	
	public ReverbNewsExtractor(Calendar calendar, String configFileName){
		this.calendar = calendar;
		this.configFileName = configFileName;
        reverb = new ReVerbExtractor();
        confFunc = new ReVerbConfFunction();
        data = new HashMap<Long, ExtractedNewsData>();
	}
	public void exp(){
		System.out.println("in exp");
//		confFunc = new ConfidenceMetric();
//		BinaryExtractionNormalizer normalizer;
		try {
	        String sentStr = "Michael McGinn is the mayor of Seattle! ";
//	        sentStr += sentStr;
//	        
	        BufferedReader br = new BufferedReader(new StringReader(sentStr + sentStr));
	        
//	        StringReader sr = new StringReader(sentStr + sentStr);
	        
	        OpenNlpSentenceChunker onsc = new OpenNlpSentenceChunker();
	        onsc.chunkSentence(sentStr + sentStr);
	        
	        ChunkedSentenceReader reader = DefaultObjects.getDefaultSentenceReader(br);
	        
	        reverb = new ReVerbExtractor();
	        confFunc = new ReVerbConfFunction();
	        for (ChunkedSentence sent : reader.getSentences()) {
	        	System.out.println(sent.toString());
		        for (ChunkedBinaryExtraction extr : reverb.extract(sent)) {
		            double conf = confFunc.getConf(extr);
		            System.out.println("Arg1=" + extr.getArgument1());
		            System.out.println("Rel=" + extr.getRelation());
		            System.out.println("Arg2=" + extr.getArgument2());
		            System.out.println("Conf=" + conf);
		        }
	        }
	    
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void extract(String srcDir, String targetDir){
		
		
		loadConfig(configFileName);
		String location = null;
		if(srcDir == null){//extract from default location
			location = rootDir + dateString + "/data/";
			System.out.println("location: " + location);
			File dataFolder = new File(location);
			String[] dataFiles = dataFolder.list();
			for(String fileName : dataFiles){
//				String fileContent = ;
				extractData(loadData(location + fileName));
			}
		
		}else{
			
		}
		outputData(targetDir == null ? rootDir + dateString + "/extracted_data/" : targetDir);
		
		System.out.println("finish!");
	}

	private void extractData(String newsData) {
		
		System.out.println("start extracting data");
		Gson gson = new Gson();
		HashMap<Long, ExtractedNewsData> map = gson.fromJson(newsData, new TypeToken<HashMap<Long, ExtractedNewsData>>(){}.getType());
		Iterator<Entry<Long, ExtractedNewsData>> it = map.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<Long, ExtractedNewsData> pairs = (Map.Entry<Long, ExtractedNewsData>)it.next();
			ExtractedNewsData currentData = (ExtractedNewsData) pairs.getValue();
			currentData.extractions = new HashMap<String, ChunkedBinaryExtraction>();
			reverbExtract(currentData, currentData.imgAlt);
			reverbExtract(currentData, currentData.imgTitle);
			reverbExtract(currentData, currentData.content);
		}
		//add this map to the wholeData
		data.putAll(map);
	}

	private void outputData(String targetDir) {
		System.out.println("start outputing data");
		
		File targetFolder = new File(targetDir);
		if(!targetDir.endsWith("/")) targetDir += "/";
		targetFolder.mkdirs();
		if (!targetFolder.exists()) emp.printLineMsg("" + this, "can't create folder");
		
		String jsonDataDir = targetDir + dateString + "_ExtractedData.revnews";
		String readableDataDir = targetDir + dateString + "_readable.txt";
		System.out.println("storing in " + jsonDataDir);
		File jsonDataFile = new File(jsonDataDir);
//		File readableFile = new File(readableDataDir);
		
		try {
			jsonDataFile.createNewFile();
			if(!jsonDataFile.exists()) emp.printLineMsg("" + this, "can't create output file data");
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jsonDataFile),ENCODE));
			Iterator<Entry<Long, ExtractedNewsData>> it = data.entrySet().iterator();
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			String seperator = ", ";
			boolean empty = true;
			while(it.hasNext()){
				empty = false;
				Map.Entry<Long, ExtractedNewsData> pair = (Map.Entry<Long, ExtractedNewsData>)it.next();
				sb.append("\"" + pair.getKey() + "\": " + pair.getValue().toJsonString() + seperator);
			}
			if(!empty) sb.delete(sb.length() - seperator.length(), sb.length());
			sb.append("}");
			out.write(sb.toString());
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	private void reverbExtract(ExtractedNewsData currentData, String str) {
		if(str != null && str.length() > 1){
			try {
		        BufferedReader br = new BufferedReader(new StringReader(str));
		        ChunkedSentenceReader reader = DefaultObjects.getDefaultSentenceReader(br);
		        
		        for (ChunkedSentence sent : reader.getSentences()) {
//		        	System.out.println(sent.toString());
//		        	System.out.println("str: " + str);
			        for (ChunkedBinaryExtraction extr : reverb.extract(sent)) {
			        	currentData.extractions.put(sent.toString(), extr);
//			            double conf = confFunc.getConf(extr);
//			            System.out.println("Arg1=" + extr.getArgument1());
//			            System.out.println("Rel=" + extr.getRelation());
//			            System.out.println("Arg2=" + extr.getArgument2());
//			            System.out.println("Conf=" + conf);
			        }
		        }
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	private String loadData(String location) {
		System.out.println("loading data from: " + location);
		StringBuilder sb = new StringBuilder();
		try {
			
			Scanner sc = new Scanner(new File(location), ENCODE);
			while(sc.hasNextLine()) sb.append(sc.nextLine());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	private void loadConfig(String fileName) {
		Config config = new Config();
		try {
			config.loadConfig(fileName);
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}
		emp = ErrorMessagePrinter.getInstance(config.getRootDir(), calendar);
		rootDir = config.getRootDir();
		dateString = config.getDateFormat().format(calendar.getTime());
	}
	
	
}
