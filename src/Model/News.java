package Model;
import java.sql.Timestamp;
/**
 * 
 * @author miniBuck
 *新闻对象的设计，有下列属性：按照下面定义的顺序
 *新闻id、题目、正文、链接地址、尖标题、副标题、原题目作者、来源、来源地址、种类、（我也不知道叫什么域）、发表时间、保存时间
 */
public class News {
	private int id;
	private String title;
	private String content;
	private String url;
	private String jtitle;
	private String subtitle;
	private String original_title;
	private String author;
	private String source;
	private String source_url;
	private String category;
	private String rawhtml;
	private Timestamp posted_at;
	private Timestamp saved_at;
	
	
	public News(int id,String title,String content,String url,String jtitle,String subtitle,String original_title,String author,String source,String source_url,String category,String rawhtml,Timestamp posted_at,Timestamp saved_at)
	{
		this.id = id;
		this.title = title;
		this.content = content;
		this.url = url;
		this.jtitle = jtitle;
		this.subtitle = subtitle;
		this.original_title = original_title;
		this.author = author;
		this.source = source;
		this.source_url = source_url;
		this.category = category;
		this.rawhtml = rawhtml;
		this.posted_at = posted_at;
		this.saved_at = saved_at;
	}
	
	public void putid(int id){
		this.id = id;
	}
	public int getid(){
		return id;
	}
	public void puttitle(String title){
		this.title = title;
	}
	public String gettitle(){
		return title;
	}
	
	public void putcontent(String content){
		this.content = content;
	}
	public String getcontent(){
		return content;
	}
	
	public void puturl(String url){
		this.url = url;
	}
	public String geturl(){
		return url;
	}
	
	public void putjtitle(String title){
		this.jtitle = title;
	}
	public String getjtitle(){
		return jtitle;
	}
	
	public void putsubtitle(String subtitle){
		this.subtitle = subtitle;
	}
	public String getsubtitle(){
		return subtitle;
	}
	
	public void putoriginal_title(String original_title){
		this.original_title = original_title;
	}
	public String getoriginal_title(){
		return original_title;
	}
	
	public void putauthor(String author){
		this.author = author;
	}
	public String getauthor(){
		return author;
	}
	
	public void putsource(String source){
		this.source = source;
	}
	public String getsource(){
		return source;
	}
	
	public void putsource_url(String source_url){
		this.source_url = source_url;
	}
	public String getsource_url(){
		return source_url;
	}
	
	public void putcategory(String category){
		this.category = category;
	}
	public String getcategory(){
		return category;
	}
	
	public void putrawhtml(String rawhtml){
		this.rawhtml = rawhtml;
	}
	public String getrawhtml(){
		return rawhtml;
	}
	
	public void putposted_at(Timestamp posted_at){
		this.posted_at = posted_at;
	}
	public Timestamp getposted_at(){
		return posted_at;
	}
	
	public void putsaved_at(Timestamp saved_at){
		this.saved_at = saved_at;
	}
	public Timestamp getsaved_at(){
		return saved_at;
	}	
}
