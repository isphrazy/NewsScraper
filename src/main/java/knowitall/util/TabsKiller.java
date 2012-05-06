package knowitall.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class TabsKiller {
    
    private static String DIRECTORY_OPTION = "d";
    
    public static void main(String[] args){
        Options options = new Options();
        Option dir_op = new Option(DIRECTORY_OPTION, false, "specify the files directory");
        
        options.addOption(dir_op);
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException excp) {
            printUsage(options);
            excp.printStackTrace();
        }
        
        if(cmd.getOptions().length < 0) printUsage(options);
        String dir = cmd.getOptionValue(DIRECTORY_OPTION);
        if(!dir.endsWith("/")) dir += "/";
        File folder = new File(dir);
        String[] files = folder.list();
        for(String file : files){
            File f = new File(dir + file);
            processFile(f);
        }
    }

    private static void processFile(File f) {
        try {
            Scanner scanner = new Scanner(f);
            StringBuilder sb = new StringBuilder();
            while(scanner.hasNext()){
                sb.append(scanner.nextLine().replace("\t", "    "));
            }
            scanner.close();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),"UTF-8"));
            out.write(sb.toString());
            out.close();
        } catch (FileNotFoundException excp) {
            excp.printStackTrace();
        } catch (UnsupportedEncodingException excp) {
            excp.printStackTrace();
        } catch (IOException excp) {
            excp.printStackTrace();
        }
    }

    private static void printUsage(Options options) {
        HelpFormatter f = new HelpFormatter();
        f.printHelp("options:", options);
        System.exit(1);
    }
}
