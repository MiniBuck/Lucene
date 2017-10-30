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
	 * �����ݿ��ȡid ��Χ�����ݣ�����news�б�
	 * 
	 * @param from id��Χ[from to]
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
	 * ���������ݽ����˴洢���� ͨ�����Ŷ����б�д��������ָ��Ŀ¼
	 * 
	 * @param list
	 *            ���Ŷ����б�
	 * @param indexPath
	 *            ����·��
	 */
	public void WriteIndexByNewsList(IndexWriter writer, List<News> list,
			HashMap<Integer, ArrayList<String>> keyword_By_tfidf) {
		try {
			// д����
			for (News n : list) {
				Document doc = new Document();
				//ʹ��tfidf���ߣ���ȡ�����б���ÿ�������еĹؼ��ʣ�tfidfǰʮ�Ĵʣ�
				ArrayList<String> l = keyword_By_tfidf.get(n.getid());
				StringBuilder sb = new StringBuilder();
				for (String str : l) {
					sb.append(str + " ");
				}			
				//�ؼ�����Ϊ��ͨ�������Lucene
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
 * ��� �����ݿ���idΪ[from��to]���ļ�д������
 * @param from
 * @param to
 * @param indexpath ��Ҫд��������ļ�Ŀ¼
 */
	public void indexUsingDBById(int from, int to, String indexpath) {
		long Starttime = System.nanoTime();
		try {
			System.out.println("Indexing to directory \'" + indexpath + "\'...");
			// �����ֵ�Ŀ¼�����ļ�ϵͳ
			FSDirectory directory = FSDirectory.open(Paths.get(indexpath, new String[0]));
			//ʹ�õ�Jcseg�ִʣ�����ͣ�ôʱ��������Դ��ĸ���ģʽ�����Ǽ��˼��ֹ���������
			Analyzer analyzer = new JcsegAnalyzer5X(JcsegTaskConfig.COMPLEX_MODE);
			JcsegAnalyzer5X jcseg = (JcsegAnalyzer5X) analyzer;
			JcsegTaskConfig config = jcseg.getTaskConfig();
			config.setClearStopwords(true);

			// �޸������޸�����
			IndexWriterConfig writerconfig = new IndexWriterConfig(analyzer);
			// Ĭ�����׷��ģʽ
			writerconfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			// ����writer
			IndexWriter writer = new IndexWriter(directory, writerconfig);
			// д����

			List<News> l = connectDBAndRead(from, to);//�����ݿ��ȡ����
			HashMap<Integer, ArrayList<String>> keyword_By_tfidf = TFIDFDB.getkeyword(l);
			//��ȡ�ؼ���
			WriteIndexByNewsList(writer, l, keyword_By_tfidf);
			//�ؼ�����ͬ����һ������
			// �ر�writer
			writer.close();
			System.out.println("�˴β�����ʱ�� " + (System.nanoTime() - Starttime));
		} catch (IOException e) {
			System.out.println("catch a" + e.getClass() + "\n with a message" + e.getMessage());
		}
	}

	/**
	 * ��ȡ�ƶ��ִ��� �ķִʽ��
	 * 
	 * @param analyzeStr
	 *            Ҫ�ֵ��ַ���
	 * @param analyzer
	 *            �ִ���
	 * 
	 * @return
	 */
	public List<String> getAnalyseResult(String analyzeStr, Analyzer analyzer) {
		List<String> response = new ArrayList<String>();
		TokenStream tokenstream = null;
		try {
			// ����������fieldName��TokenStream
			tokenstream = analyzer.tokenStream("keyword", new StringReader(analyzeStr));
			// �ʻ㵥Ԫ��Ӧ���ı�
			CharTermAttribute attr = tokenstream.addAttribute(CharTermAttribute.class);
			// ��������ʹ��incrementToken ��ʼ����֮ǰ���ô˷���
			// ��������ֵδ�ɾ�״̬����״̬��ʵ�ֱ���ʵ�����ַ������Ա����ǿ��Ա����ã��������Ǳ�������һ��
			tokenstream.reset();
			// Consumer����IndexWriter��ʹ�ô˷����������͵���һ��token
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
 * һ���򵥵���������Ҳ����������
 * @param filepathars ������Ŀ¼
 * @param field ��Ҫ��ѯ����
 * @param content ��ѯ�����ݣ�����ʹ����򵥵�term��ѯ��lucene����������ѯ
 * @throws IOException
 */
	public void search(String filepathars, String field, String content) throws IOException {
		Path path = FileSystems.getDefault().getPath(filepathars);
		// ��������Ŀ¼
		Directory directory = FSDirectory.open(path);
		// ���������鿴��
		IndexReader indexReader = DirectoryReader.open(directory);
		// ��������������
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		// ������������
		Term term = new Term(field, content);
		// �����ѯ
		Query query = new TermQuery(term);
		// ����ǰʮ���ĵ�
		TopDocs topdocs = indexSearcher.search(query, 10);
		// ��ӡ������
		System.out.println("��������+" + topdocs.totalHits);
		// ȡ���ĵ�
		ScoreDoc[] scoreDocs = topdocs.scoreDocs;
		// ����ȡ������
		for (ScoreDoc scoreDoc : scoreDocs) {
			Document doc = indexSearcher.doc(scoreDoc.doc);
			System.out.println("id" + doc.get("id"));
			System.out.println("content" + doc.get("content"));
			System.out.println("keyword" + doc.get("keyword"));
		}

	}

	//��Ϊ�Լ�д��tfidfЧ�ʹ��ͣ�����ÿ�β�ѯ50ƪ���ų�ȡ�ؼ��֣���һ������ʹ��word2vec��ȡ
	public static void main(String[] args) {
		test1 t = new test1();
		// ��ڣ�����ΪID��Χ[1,5517],����λ��
		//for (int i = 1; i <= 5500; i = i + 50) {
		//	t.indexUsingDBById(i, i + 49, "C:\\Users\\hp\\Desktop\\index");
		//}

		try {
			t.search("C:\\Users\\hp\\Desktop\\index", "content", "��Ҫì��");
			System.out.println("**************************************************");
			t.search("C:\\Users\\hp\\Desktop\\index", "keyword", "��Ҫì��");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
