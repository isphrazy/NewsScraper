package knowitall.reverbovernews;

public class FormattedNewsData {
    
    public Long id;
    public String url;
    public String title;
    public String sentence;
    public String date;
    public String arg1;
    public String relation;
    public String arg2;
    public String category;
    public double confidence;
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(killTab(title) + "\t");
        sb.append(date + "\t");
        sb.append(killTab(arg1) + ", " + killTab(relation) + ", " + killTab(arg2) + ", " + confidence + "\t");
        sb.append(killTab(sentence) + "\t");
        sb.append(category + "\t");
        sb.append(url + "\t");
        sb.append(id + "\n");
        return sb.toString();
    }
    
    private String killTab(String s){
        return s.replace("\t", " ");
    }
    

}
