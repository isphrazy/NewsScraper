package knowitall.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RandomExtractedData {

    private static JSONObject result;
    
    public static void main(String[] args) {
        result = new JSONObject();
        get();
        put();
    }
    

    private static void put() {
        File outputFile = new File("./n-nary_keypoints.txt");
        try {
            outputFile.createNewFile();
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile),"UTF-8"));
            out.write(result.toString());
            out.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private static void get() {
        File srcFolder = new File("./yahoo_data/extracted_data/");
        String[] srcFiles = srcFolder.list();
        for(int i = 0; i < srcFiles.length; i++){
            File srcFile = new File(srcFolder, srcFiles[i]);
            String fileContent = FileLoader.loadFile(srcFile.toString());
            try {
                JSONObject jFileContent = new JSONObject(fileContent);
                JSONArray ja = jFileContent.names();
                for(int j = 0; j < ja.length(); j++){
//                    if(new Random().nextDouble() < 0.01){
                        String key = ja.getString(j);
                        JSONArray jExtractions = jFileContent.getJSONObject(key).getJSONArray("extractions");
                        boolean keep = false;
                        for(int k = 0; k < jExtractions.length(); k++){
                            JSONObject jExtraction = jExtractions.getJSONObject(k);
                            if(jExtraction.getDouble("confidence") > 0.9){
                                keep = true;
                                break;
                            }
                        }
                        if(keep && new Random().nextDouble() < 0.001)
                            result.put(key, jFileContent.getJSONObject(key));
//                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
