package knowitall.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * help load file
 * @author Pingyang He
 *
 */
public class FileLoader {
    
    //load the given file and return the content of the file
    //as string
    public static String loadFile(String dir){
        StringBuilder sb = new StringBuilder();
        try {
            Scanner sc = new Scanner(new File(dir));
            while(sc.hasNextLine()){
                sb.append(sc.nextLine());
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return sb.toString();
    }
}
