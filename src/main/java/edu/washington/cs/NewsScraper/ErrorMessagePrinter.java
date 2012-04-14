package edu.washington.cs.NewsScraper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ErrorMessagePrinter {
	
//	private static String date;
	private static ErrorMessagePrinter instance;
	
	private static FileWriter errorLog;
	private static String folderName;
	
	private ErrorMessagePrinter(){}
	
	public static ErrorMessagePrinter getInstance(String location){
		if(instance == null){
//			date = todayDate;
			instance = new ErrorMessagePrinter();
			folderName = location + "error.log";
		}
		return instance;
		
	}
	
	public void printLineMsg(String msg){
		printLineMsg("Unknow", msg);
	}
	
	public void printLineMsg(String methodName, String msg){
		try {
			File errorFile = new File(folderName);
			
			//if error file not exist, create one
			if(!errorFile.exists()) errorFile.createNewFile();
			
			errorLog = new FileWriter(folderName, true);
			System.out.println(folderName);
			BufferedWriter out = new BufferedWriter(errorLog);
	        out.write(methodName + ": " + msg + "\n");
	        out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
