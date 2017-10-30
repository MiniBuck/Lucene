package Utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.lionsoul.jcseg.analyzer.JcsegAnalyzer5X;
import org.lionsoul.jcseg.core.JcsegTaskConfig;

import Model.News;//对新闻的封装
//import Test.test1;//只在测试时使用一下

/**
 * 工具：用于计算TFIDF
 * 
 * @author miniBuck
 *
 */
public class TFIDFDB {
	/**
	 * 分词，返回一个词列表
	 * 
	 * @param analyzeStr
	 *            待分字符串
	 * @param analyzer
	 *            分词器（Lucene中analyzer形式）
	 * @return 词列表
	 */
	public static ArrayList<String> getAnalyseResult(String analyzeStr, Analyzer analyzer) {
		ArrayList<String> response = new ArrayList<String>();
		TokenStream tokenstream = null;
		try {
			// 返回适用于fieldName的TokenStream，此处我暂时没看懂文档中关于这个域的说明
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
	 * 分词
	 * 
	 * @param str待分字符串
	 *            
	 * @return 分词列表
	 */
	public static ArrayList<String> cutWords(String str) {
		String text = str;
		// 使用的Jcseg5 版本
		Analyzer analyzer = new JcsegAnalyzer5X(JcsegTaskConfig.COMPLEX_MODE);
		//加载停用词，因为我是用的版本默认是不启用停用词表的
		JcsegAnalyzer5X jcseg = (JcsegAnalyzer5X) analyzer;
		JcsegTaskConfig config = jcseg.getTaskConfig();
		config.setClearStopwords(true);
		ArrayList<String> words = getAnalyseResult(text, analyzer);
		return words;
	}

/**
 * 对切分的词统计词频（频数）
 * @param cutwords
 * @return 以哈希表形式存储
 */
	public static HashMap<String, Integer> normalTF(ArrayList<String> cutwords) {
		HashMap<String, Integer> resTF = new HashMap<String, Integer>();
		for (String word : cutwords) {
			if (resTF.get(word) == null) {
				resTF.put(word, 1);			
			} else {
				resTF.put(word, resTF.get(word) + 1);				
			}
		}
		return resTF;
	}

	/**
	 * 对切分的词统计词频（频率）
	 * @param cutwords
	 * @return 以哈希表形式存储
	 */
	public static HashMap<String, Float> tf(ArrayList<String> cutwords) {
		HashMap<String, Float> resTF = new HashMap<String, Float>();
		int wordLen = cutwords.size();
		HashMap<String, Integer> intTF = TFIDFDB.normalTF(cutwords);
		Iterator<?> iter = intTF.entrySet().iterator(); 														// from TF
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			resTF.put(entry.getKey().toString(), Float.parseFloat(entry.getValue().toString()) / wordLen);
		}
		return resTF;
	}

/**
 * 根据新闻列表，返回每篇新闻id对应的词频统计
 * @param list
 * @return 新闻id：词频统计哈希表
 */
	public static HashMap<Integer, HashMap<String, Integer>> normalTFAllFiles(List<News> list) {
		HashMap<Integer, HashMap<String, Integer>> allNormalTF = new HashMap<Integer, HashMap<String, Integer>>();
		List<News> filelist = list;
		for (News file : filelist) {
			HashMap<String, Integer> dict = new HashMap<String, Integer>();
			ArrayList<String> cutwords = TFIDFDB.cutWords(file.getcontent()); 
			dict = TFIDFDB.normalTF(cutwords);
			allNormalTF.put(file.getid(), dict);
		}
		return allNormalTF;
	}

/**
 * 根据新闻列表，返回新闻列表里所有新闻的tf值
 * @param list 新闻列表
 * @return<新闻id，<词，tf值>>
 */
	public static HashMap<Integer, HashMap<String, Float>> tfAllFiles(List<News> list) {
		HashMap<Integer, HashMap<String, Float>> allTF = new HashMap<Integer, HashMap<String, Float>>();
		List<News> filelist = list;
		for (News file : filelist) {
			HashMap<String, Float> dict = new HashMap<String, Float>();
			ArrayList<String> cutwords = TFIDFDB.cutWords(file.getcontent()); 
			dict = TFIDFDB.tf(cutwords);
			allTF.put(file.getid(), dict);
		}
		return allTF;
	}

	/**
	 * 对tf列表运算idf
	 * 
	 * @param all_tf 所有词的tf值
	 * @return 每个词的idf值
	 */
	public static HashMap<String, Float> idf(HashMap<Integer, HashMap<String, Float>> all_tf) {
		HashMap<String, Float> resIdf = new HashMap<String, Float>();
		HashMap<String, Integer> dict = new HashMap<String, Integer>();
		// int docNum = FileList.size();
		int docNum = all_tf.size();
		Set<Integer> idset = all_tf.keySet();
		Iterator<Integer> it = idset.iterator();
		while (it.hasNext()) {
			int id = it.next();
			HashMap<String, Float> temp = all_tf.get(id);
			Iterator iter = temp.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String word = entry.getKey().toString();
				if (dict.get(word) == null) {
					dict.put(word, 1);
				} else {
					dict.put(word, dict.get(word) + 1);
				}
			}
		}
		Iterator iter_dict = dict.entrySet().iterator();
		while (iter_dict.hasNext()) {
			Map.Entry entry = (Map.Entry) iter_dict.next();
			float value = (float) Math.log(docNum / Float.parseFloat(entry.getValue().toString()));
			resIdf.put(entry.getKey().toString(), value);
		}
		return resIdf;
	}

	/**
	 * 
	 * @param all_tf 所有新闻中每个词的tf词
	 * @param idfs 所有词的idf值
	 * @return 每篇新闻中每个词的tfidf值<新闻id,<词，tfid值>>
	 */
	public static HashMap<Integer, HashMap<String, Float>> tf_idf(HashMap<Integer, HashMap<String, Float>> all_tf,
			HashMap<String, Float> idfs) {
		HashMap<Integer, HashMap<String, Float>> resTfIdf = new HashMap<Integer, HashMap<String, Float>>();

		Set<Integer> idset = all_tf.keySet();
		Iterator<Integer> it = idset.iterator();
		while (it.hasNext()) {
			int id = it.next();
			HashMap<String, Float> tfidf = new HashMap<String, Float>();
			HashMap<String, Float> temp = all_tf.get(id);
			Iterator iter = temp.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				String word = entry.getKey().toString();
				Float value = (float) Float.parseFloat(entry.getValue().toString()) * idfs.get(word);
				tfidf.put(word, value);
			}
			resTfIdf.put(id, tfidf);
		}

		return resTfIdf;
	}

	/**
	 * 
	 * @param tf_idf 每篇新闻中每个词的tfidf值
	 * @return 每篇新闻里tfidf值前十的词与列表<新闻id,词语列表>
	 */
	public static HashMap<Integer, ArrayList<String>> topNkeywoed(HashMap<Integer, HashMap<String, Float>> tf_idf) {
		HashMap<Integer, ArrayList<String>> result = new HashMap<Integer, ArrayList<String>>();
		Set<Integer> idset = tf_idf.keySet();
		Iterator<Integer> it = idset.iterator();
		while (it.hasNext()) {
			int id = it.next();
			HashMap<String, Float> temp = tf_idf.get(id);
			Set<String> keywordSet = temp.keySet();
			Iterator<String> iterator = keywordSet.iterator();
			List<Map.Entry<String, Float>> l = new ArrayList<>();
			for (Map.Entry<String, Float> entry : temp.entrySet()) {
				l.add(entry); // 将map中的元素放入list中
			}
			l.sort(new Comparator<Map.Entry<String, Float>>() {

				@Override
				public int compare(Entry<String, Float> arg0, Entry<String, Float> arg1) {
					// TODO Auto-generated method stub
					return (int) (arg1.getValue() - arg0.getValue());
				}
				// 逆序（从大到小）排列，正序为“return o1.getValue()-o2.getValue”;
			});
			ArrayList<String> r = new ArrayList<>();
			for (int i = 0; i < 10 && i < l.size(); i++) {// 取前10top
				Map.Entry<String, Float> entry = l.get(i);
				r.add(entry.getKey());
			}
			result.put(id, r);
		}
		return result;
	}

/**
 * 根据新闻列表，返回改新闻列表的TFIDF前10的关键字列表，封装一下，作为tfidf工具类使用的入口
 * @param newslist 新闻列表
 * @return 每篇新闻里tfidf值前十的词与列表<新闻id,词语列表>
 */
	public static HashMap<Integer, ArrayList<String>> getkeyword(List<News> newslist) {
		HashMap<Integer, HashMap<String, Float>> all_tf = tfAllFiles(newslist);
		HashMap<String, Float> idfs = idf(all_tf);
		HashMap<Integer, HashMap<String, Float>> tfidf = tf_idf(all_tf, idfs);// !!!
		HashMap<Integer, ArrayList<String>> result = topNkeywoed(tfidf);// !!
		return result;
	}
	/*
	 * public static void main(String[] args) { test1 t = new test1();
	 * List<News> newslist = t.connectDBAndRead(1, 2);
	 * //System.out.println(newslist);
	 * 
	 * HashMap<Integer, HashMap<String, Float>> all_tf = tfAllFiles(newslist);
	 * //System.out.println(all_tf);
	 * 
	 * HashMap<String, Float> idfs = idf(all_tf); //System.out.println(idfs);
	 * 
	 * System.out.println(); HashMap<Integer, HashMap<String, Float>> tfidf =
	 * tf_idf(all_tf, idfs); //System.out.println(tfidf);
	 * 
	 * HashMap<Integer, ArrayList<String>> result = topNkeywoed(tfidf);
	 * //System.out.println(result);
	 * 
	 * }
	 */
}
