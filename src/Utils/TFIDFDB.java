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

import Model.News;//�����ŵķ�װ
//import Test.test1;//ֻ�ڲ���ʱʹ��һ��

/**
 * ���ߣ����ڼ���TFIDF
 * 
 * @author miniBuck
 *
 */
public class TFIDFDB {
	/**
	 * �ִʣ�����һ�����б�
	 * 
	 * @param analyzeStr
	 *            �����ַ���
	 * @param analyzer
	 *            �ִ�����Lucene��analyzer��ʽ��
	 * @return ���б�
	 */
	public static ArrayList<String> getAnalyseResult(String analyzeStr, Analyzer analyzer) {
		ArrayList<String> response = new ArrayList<String>();
		TokenStream tokenstream = null;
		try {
			// ����������fieldName��TokenStream���˴�����ʱû�����ĵ��й���������˵��
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
	 * �ִ�
	 * 
	 * @param str�����ַ���
	 *            
	 * @return �ִ��б�
	 */
	public static ArrayList<String> cutWords(String str) {
		String text = str;
		// ʹ�õ�Jcseg5 �汾
		Analyzer analyzer = new JcsegAnalyzer5X(JcsegTaskConfig.COMPLEX_MODE);
		//����ͣ�ôʣ���Ϊ�����õİ汾Ĭ���ǲ�����ͣ�ôʱ��
		JcsegAnalyzer5X jcseg = (JcsegAnalyzer5X) analyzer;
		JcsegTaskConfig config = jcseg.getTaskConfig();
		config.setClearStopwords(true);
		ArrayList<String> words = getAnalyseResult(text, analyzer);
		return words;
	}

/**
 * ���зֵĴ�ͳ�ƴ�Ƶ��Ƶ����
 * @param cutwords
 * @return �Թ�ϣ����ʽ�洢
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
	 * ���зֵĴ�ͳ�ƴ�Ƶ��Ƶ�ʣ�
	 * @param cutwords
	 * @return �Թ�ϣ����ʽ�洢
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
 * ���������б�����ÿƪ����id��Ӧ�Ĵ�Ƶͳ��
 * @param list
 * @return ����id����Ƶͳ�ƹ�ϣ��
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
 * ���������б����������б����������ŵ�tfֵ
 * @param list �����б�
 * @return<����id��<�ʣ�tfֵ>>
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
	 * ��tf�б�����idf
	 * 
	 * @param all_tf ���дʵ�tfֵ
	 * @return ÿ���ʵ�idfֵ
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
	 * @param all_tf ����������ÿ���ʵ�tf��
	 * @param idfs ���дʵ�idfֵ
	 * @return ÿƪ������ÿ���ʵ�tfidfֵ<����id,<�ʣ�tfidֵ>>
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
	 * @param tf_idf ÿƪ������ÿ���ʵ�tfidfֵ
	 * @return ÿƪ������tfidfֵǰʮ�Ĵ����б�<����id,�����б�>
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
				l.add(entry); // ��map�е�Ԫ�ط���list��
			}
			l.sort(new Comparator<Map.Entry<String, Float>>() {

				@Override
				public int compare(Entry<String, Float> arg0, Entry<String, Float> arg1) {
					// TODO Auto-generated method stub
					return (int) (arg1.getValue() - arg0.getValue());
				}
				// ���򣨴Ӵ�С�����У�����Ϊ��return o1.getValue()-o2.getValue��;
			});
			ArrayList<String> r = new ArrayList<>();
			for (int i = 0; i < 10 && i < l.size(); i++) {// ȡǰ10top
				Map.Entry<String, Float> entry = l.get(i);
				r.add(entry.getKey());
			}
			result.put(id, r);
		}
		return result;
	}

/**
 * ���������б����ظ������б��TFIDFǰ10�Ĺؼ����б���װһ�£���Ϊtfidf������ʹ�õ����
 * @param newslist �����б�
 * @return ÿƪ������tfidfֵǰʮ�Ĵ����б�<����id,�����б�>
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
