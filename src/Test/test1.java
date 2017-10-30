package Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.lionsoul.jcseg.analyzer.JcsegAnalyzer5X;
import org.lionsoul.jcseg.core.JcsegTaskConfig;

import Model.News;
import Utils.TFIDFDB;

public class test1 {
	private static String url = "jdbc:mysql://localhost:3306/***";
	private static String user = "***";
	private static String password = "***";
	private static String driver = "com.mysql.jdbc.Driver";
	private static final String tablename = "***";

	/**
	 * 从数据库读取id 范围的数据，返回news列表
	 * 
	 * @param from id范围[from to]
	 *            
	 * @param to
	 * @return
	 */
	public List<News> connectDBAndRead(int from, int to) {
		Connection connection = null;
		Statement statement = null;
		ResultSet resultset = null;
		ArrayList<News> list = new ArrayList<News>();
		try {
			String selectSql = "SELECT * FROM " + tablename + " WHERE id >= " + from + " and id <= " + to;
			Class.forName(driver);
			connection = DriverManager.getConnection(url, user, password);
			statement = connection.createStatement();
			resultset = statement.executeQuery(selectSql);

			while (resultset.next()) {
				int id = resultset.getInt("id");
				String title = resultset.getString("title");
				String content = resultset.getString("content");
				String url = resultset.getString("url");
				String jtitle = resultset.getString("jtitle");
				String subtitle = resultset.getString("subtitle");
				String original_title = resultset.getString("original_title");
				String author = resultset.getString("author");
				String source = resultset.getString("source");
				String source_url = resultset.getString("source_url");
				String category = resultset.getString("category");
				String rawhtml = resultset.getString("raw_html");
				Timestamp posted_at = resultset.getTimestamp("posted_at");
				Timestamp saved_at = resultset.getTimestamp("saved_at");
				News n = new News(id, title, content, url, jtitle, subtitle, original_title, author, source, source_url,
						category, rawhtml, posted_at, saved_at);
				list.add(n);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list;
	}

	/**
	 * 对新闻内容进行了存储索引 通过新闻对象列表，写入索引到指定目录
	 * 
	 * @param list
	 *            新闻对象列表
	 * @param indexPath
	 *            索引路径
	 */
	public void WriteIndexByNewsList(IndexWriter writer, List<News> list,
			HashMap<Integer, ArrayList<String>> keyword_By_tfidf) {
		try {
			// 写索引
			for (News n : list) {
				Document doc = new Document();
				//使用tfidf工具，获取新闻列表中每条新闻中的关键词（tfidf前十的词）
				ArrayList<String> l = keyword_By_tfidf.get(n.getid());
				StringBuilder sb = new StringBuilder();
				for (String str : l) {
					sb.append(str + " ");
				}			
				//关键词作为普通的域存入Lucene
				TextField keyword = new TextField("keyword", sb.toString(), Store.YES);

				StoredField id = new StoredField("id", n.getid());
				TextField title = new TextField("title", n.gettitle(), Store.YES);
				TextField content = new TextField("content", n.getcontent(), Store.YES);
				StringField url = new StringField("url", n.geturl(), Store.YES);
				TextField jtitle = new TextField("jtitle", n.getjtitle(), Store.YES);
				TextField subtitle = new TextField("subtitle", n.getsubtitle(), Store.YES);
				TextField original_title = new TextField("original_title", n.getoriginal_title(), Store.YES);
				StringField author = new StringField("author", n.getauthor(), Store.YES);
				StringField source = new StringField("source", n.getsource(), Store.YES);
				StringField source_url = new StringField("source_url", n.getsource_url(), Store.YES);
				StringField category = new StringField("category", n.getcategory(), Store.YES);
				StoredField rawhtml = new StoredField("rawhtml", n.getrawhtml());
				LongPoint posted_at = new LongPoint("posted_at", n.getsaved_at().getTime());
				StoredField posted_ats = new StoredField("posted_at", n.getsaved_at().getTime());
				LongPoint saved_at = new LongPoint("saved_at", n.getsaved_at().getTime());
				StoredField saved_ats = new StoredField("saved_at", n.getsaved_at().getTime());
				doc.add(keyword);

				doc.add(id);
				doc.add(title);
				doc.add(content);
				doc.add(url);
				doc.add(jtitle);
				doc.add(subtitle);
				doc.add(original_title);
				doc.add(author);
				doc.add(source);
				doc.add(source_url);
				doc.add(category);
				doc.add(rawhtml);
				doc.add(rawhtml);
				doc.add(posted_at);
				doc.add(posted_ats);
				doc.add(saved_at);
				doc.add(saved_ats);
				writer.addDocument(doc);
			}
			writer.commit();
		} catch (IOException e) {
			System.out.println("catch a" + e.getClass() + "\n with a message" + e.getMessage());
		}
	}

/**
 * 入口 将数据库中id为[from，to]的文件写入索引
 * @param from
 * @param to
 * @param indexpath 将要写入的索引文件目录
 */
	public void indexUsingDBById(int from, int to, String indexpath) {
		long Starttime = System.nanoTime();
		try {
			System.out.println("Indexing to directory \'" + indexpath + "\'...");
			// 创建字典目录存于文件系统
			FSDirectory directory = FSDirectory.open(Paths.get(indexpath, new String[0]));
			//使用的Jcseg分词，开启停用词表，启用它自带的复杂模式（就是加了几种过滤条件）
			Analyzer analyzer = new JcsegAnalyzer5X(JcsegTaskConfig.COMPLEX_MODE);
			JcsegAnalyzer5X jcseg = (JcsegAnalyzer5X) analyzer;
			JcsegTaskConfig config = jcseg.getTaskConfig();
			config.setClearStopwords(true);

			// 修改索引修改配置
			IndexWriterConfig writerconfig = new IndexWriterConfig(analyzer);
			// 默认设成追加模式
			writerconfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			// 索引writer
			IndexWriter writer = new IndexWriter(directory, writerconfig);
			// 写索引

			List<News> l = connectDBAndRead(from, to);//从数据库读取新闻
			HashMap<Integer, ArrayList<String>> keyword_By_tfidf = TFIDFDB.getkeyword(l);
			//获取关键字
			WriteIndexByNewsList(writer, l, keyword_By_tfidf);
			//关键字连同新闻一起建索引
			// 关闭writer
			writer.close();
			System.out.println("此次操作用时： " + (System.nanoTime() - Starttime));
		} catch (IOException e) {
			System.out.println("catch a" + e.getClass() + "\n with a message" + e.getMessage());
		}
	}

	/**
	 * 获取制定分词器 的分词结果
	 * 
	 * @param analyzeStr
	 *            要分的字符串
	 * @param analyzer
	 *            分词器
	 * 
	 * @return
	 */
	public List<String> getAnalyseResult(String analyzeStr, Analyzer analyzer) {
		List<String> response = new ArrayList<String>();
		TokenStream tokenstream = null;
		try {
			// 返回适用于fieldName的TokenStream
			tokenstream = analyzer.tokenStream("keyword", new StringReader(analyzeStr));
			// 词汇单元对应的文本
			CharTermAttribute attr = tokenstream.addAttribute(CharTermAttribute.class);
			// 消费者在使用incrementToken 开始消费之前调用此方法
			// 将次流充值未干净状态。有状态的实现必须实现这种方法，以便他们可以被重用，就像他们被被构建一样
			tokenstream.reset();
			// Consumer（即IndexWriter）使用此方法将流推送到下一个token
			while (tokenstream.incrementToken()) {
				response.add(attr.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (tokenstream != null) {
				try {
					tokenstream.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return response;
	}

/**
 * 一个简单的搜索，我也放在这里了
 * @param filepathars 索引的目录
 * @param field 想要查询的域
 * @param content 查询的内容，这里使用最简单的term查询，lucene还有其他查询
 * @throws IOException
 */
	public void search(String filepathars, String field, String content) throws IOException {
		Path path = FileSystems.getDefault().getPath(filepathars);
		// 定义索引目录
		Directory directory = FSDirectory.open(path);
		// 定义索引查看器
		IndexReader indexReader = DirectoryReader.open(directory);
		// 定义索引搜索器
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		// 定义搜索词条
		Term term = new Term(field, content);
		// 定义查询
		Query query = new TermQuery(term);
		// 命中前十条文档
		TopDocs topdocs = indexSearcher.search(query, 10);
		// 打印命中数
		System.out.println("命中数：+" + topdocs.totalHits);
		// 取出文档
		ScoreDoc[] scoreDocs = topdocs.scoreDocs;
		// 遍历取出数据
		for (ScoreDoc scoreDoc : scoreDocs) {
			Document doc = indexSearcher.doc(scoreDoc.doc);
			System.out.println("id" + doc.get("id"));
			System.out.println("content" + doc.get("content"));
			System.out.println("keyword" + doc.get("keyword"));
		}

	}

	//因为自己写的tfidf效率过低，所以每次查询50篇新闻抽取关键字，下一步考虑使用word2vec抽取
	public static void main(String[] args) {
		test1 t = new test1();
		// 入口，参数为ID范围[1,5517],索引位置
		//for (int i = 1; i <= 5500; i = i + 50) {
		//	t.indexUsingDBById(i, i + 49, "C:\\Users\\hp\\Desktop\\index");
		//}

		try {
			t.search("C:\\Users\\hp\\Desktop\\index", "content", "主要矛盾");
			System.out.println("**************************************************");
			t.search("C:\\Users\\hp\\Desktop\\index", "keyword", "主要矛盾");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
