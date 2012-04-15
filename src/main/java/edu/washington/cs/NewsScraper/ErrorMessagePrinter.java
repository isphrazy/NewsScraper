package edu.washington.cs.NewsScraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * This class will print the given string to error.log file
 * This class is singleton, use getInstance method to use the object
 * 
 * @author Pingyang He
 *
 */
public class ErrorMessagePrinter {
	
	private static ErrorMessagePrinter instance;
	private static FileWriter errorLog;
	private static String folderName;
	private static Calendar calendar;
	private static String dateString;
	
	//singleton
	private ErrorMessagePrinter(){}
	
	/**
	 * returns the instance of this class
	 * @param location where the error log file will be stored
	 * @param cal gives the current time
	 * @return the instance
	 */
	public static ErrorMessagePrinter getInstance(String location, Calendar cal){
		if(instance == null){
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			calendar = cal;
			dateString = dateFormat.format(calendar.getTime());
			
			instance = new ErrorMessagePrinter();
			folderName = location + "error.log";
		}
		return instance;
		
	}
	
	/**
	 * this method will print the given strong to error log
	 * @param methodName where is this method being called
	 * @param msg the message will be print to file
	 */
	public void printLineMsg(String methodName, String msg){
		try {
			File errorFile = new File(folderName);
			
			//if error file not exist, create one
			if(!errorFile.exists()) errorFile.createNewFile();
			
			errorLog = new FileWriter(folderName, true);
			BufferedWriter out = new BufferedWriter(errorLog);
	        out.write(dateString + ": " + methodName + ": " + msg + "\n");
	        out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
