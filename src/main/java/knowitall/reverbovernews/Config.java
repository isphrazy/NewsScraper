package knowitall.reverbovernews;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Scanner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Config {
	
	private final String ENCODE = "UTF-8";
	private final String JSON_FOLDER_NAME = "folder-name";
	private final String JSON_DATE_FORMAT = "date-format";
	
	private String rootDir;
	private JsonObject config;
	private DateFormat dateFormat;
	
	public void loadConfig(String configFile) throws FileNotFoundException{
		
		String fileContent = readFile(configFile);
		
		config = (JsonObject)(new JsonParser()).parse(fileContent);
		rootDir = config.get(JSON_FOLDER_NAME).getAsString();
		dateFormat = new SimpleDateFormat(config.get(JSON_DATE_FORMAT).getAsString());
	}
	
	public DateFormat getDateFormat(){
		return dateFormat;
	}
	
	public JsonObject getConfig(){
		return config;
	}
	
	public String getRootDir(){
		return rootDir;
	}

	private String readFile(String configFile) throws FileNotFoundException {
		Scanner sc = new Scanner(new File(configFile), ENCODE);
		StringBuilder sb = new StringBuilder();
		while(sc.hasNextLine())
			sb.append(sc.nextLine());
		
		return sb.toString();
	}
}
