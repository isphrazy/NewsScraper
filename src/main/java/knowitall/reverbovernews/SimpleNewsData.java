package knowitall.reverbovernews;

import java.lang.reflect.Field;

public class SimpleNewsData {
    public String title;
    public String url;
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        String seperator = ", ";
        sb.append("{");
        Field[] fields = this.getClass().getFields();
        for(Field field : fields){
            String fieldName = field.getName();
            sb.append("\""+ fieldName + "\"");
            sb.append(":");
            try {
                sb.append("\"" + field.get(this).toString().replace("\"", "\\\"") + "\"");
            } catch (IllegalArgumentException excp) {
                excp.printStackTrace();
            } catch (IllegalAccessException excp) {
                excp.printStackTrace();
            }
            sb.append(seperator);
        }
        
        return sb.toString();
    }

}
