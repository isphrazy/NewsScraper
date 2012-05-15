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
    
    /**
     * print this object's fields separated by \t
     */
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(killTab(arg1) + "\t");
        sb.append(killTab(relation) + "\t");
        sb.append(killTab(arg2) + "\t");
        sb.append(confidence + "\t");
        sb.append(killTab(sentence) + "\t");
        sb.append(killTab(title) + "\t");
        sb.append(date + "\t");
        sb.append(category + "\t");
        sb.append(url + "\t");
        sb.append(id + "\n");
        return sb.toString();
    }
    
    /*
     * get rid of \t in a string
     */
    private String killTab(String s){
        return s.replace("\t", " ");
    }
    

}
