package crawl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import model.Movie_Info;

import util.BasicUtil;
import witer.DBWriter;
import witer.ImageWriter;


public class DyttCrawler extends BaseCrawler{
	
	private static final String ROOT_URL = "http://www.dytt8.net/";
	private static final String NEW_MOVIE_URL = "html/gndy/dyzz/list_23_%d.html";
	private int MAX_PAGE = 1;
	private final static int TIME_OUT = 5000;
	private final static String AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)";
	private final static int THREAD_NUM = 10;
	private final String movie_src = "Dytt";
	
	public static void main(String[] args) {
		DyttCrawler dc = new DyttCrawler();
		dc.begin();
	}
	
	public DyttCrawler(){
		
	}
		
	private void begin(){
		//文件目录初始化
		File f = new File("image/" + movie_src);
		f.mkdir();
		//获取max page
		if(!getMaxPage()){
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++get max page failed! halting++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			return;
		}
		DBWriter.getInstance().start();
		ImageWriter.getInstance().setMovieSrc(movie_src);
		ImageWriter.getInstance().start();
		ExecutorService exe = Executors.newFixedThreadPool(THREAD_NUM);
		CountDownLatch cdl = new CountDownLatch(THREAD_NUM);
		for(int thread_id = 1; thread_id <= THREAD_NUM; thread_id ++){
			exe.execute(new MoiveCrawler(cdl, thread_id));
		}
		try {
			cdl.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		DBWriter.getInstance().halt();
		ImageWriter.getInstance().halt();
		exe.shutdown();
	}
	
	/**
	 * 指定线程按规则爬取页面
	 * @param id : 当前线程ID
	 */
	protected void crawl(int id){
		int total = 0;
		int page_counter = 0 + id;
		for(;page_counter <= MAX_PAGE; page_counter += THREAD_NUM){
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": crawling page: " + page_counter + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			String sUrl = ROOT_URL + String.format(NEW_MOVIE_URL, page_counter);
			String s = getContent(sUrl);
			int counter = crawlMovies(id, s);
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": " + counter + " movies crawled++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			total += counter;
		}
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": " + total + " movies crawled++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}
	
	/**
	 * 获取指定网页内容
	 * @param surl ：网址
	 * @return 网页源代码
	 */
	private String getContent(String surl){
		StringBuffer sb = new StringBuffer();
		try {
			URL url = new URL(surl);
			URLConnection urlconnection = url.openConnection();
			urlconnection.addRequestProperty("User-Agent", AGENT);
			urlconnection.setConnectTimeout(TIME_OUT);
			InputStream is = url.openStream();
			BufferedReader bReader = new BufferedReader(new InputStreamReader(is, "GB2312"));
			String rLine = null;
			while((rLine=bReader.readLine())!=null){
				sb.append(rLine);
				sb.append("/r/n");
			}
		}catch (IOException e) {
		}
		return sb.toString();
	}
	
	/**
	 * 获取当前最大页
	 * @return MAX_PAGE
	 */
	private boolean getMaxPage(){
		String sUrl = ROOT_URL + String.format(NEW_MOVIE_URL, 1);
		String s = getContent(sUrl);
		String regex = "<a href=(\"|\')list_23_[0-9]{1,}.html(\"|\')>末页</a>";
		Pattern pt = Pattern.compile(regex);
		Matcher mt = pt.matcher(s);
		while(mt.find()){
			String str = mt.group();
			str = str.substring(17, str.length() - 13);
			try {
				MAX_PAGE = Integer.parseInt(str);
				System.out.println("last page found: " + MAX_PAGE);
				return true;
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	private final static String MOVIE_URL_PATTERN = "<a href=\"/html/gndy/dyzz/[0-9]{1,}/[0-9]{1,}.html\"";
	private final static String HAIBAO_PATTERN = "<img[^<]*src=\"http.*?(jpg|gif|JPG|GIF)\"";
	private final static String PIANMING_PATTERN = "(◎译　　名|◎片　　名|◎中 文 名|◎英 文 名|◎译 　　名|◎片 　　名)[^<]*<br />";
	private final static String XIAZAIMING_PATTERN = "(rmvb|avi|mp4|mkv|RMVB|AVI|PM4|MKV)\">[^<]*(rmvb|avi|mp4|mkv|RMVB|AVI|PM4|MKV)";
	private final static String[] MOVIE_PATTERNS = {HAIBAO_PATTERN, PIANMING_PATTERN, XIAZAIMING_PATTERN};

	/**
	 * 获取电影信息
	 * @param id : 当前线程ID
	 * @param s : 网页源代码
	 * @return 当前页获取电影数量
	 */
	private int crawlMovies(int id, String s){
		int movie_counter = 0;
		ArrayList<Movie_Info> movie_list = new ArrayList<Movie_Info>();
		Pattern pt = Pattern.compile(MOVIE_URL_PATTERN);
		Matcher mt = pt.matcher(s);
		while(mt.find()){
			String str = mt.group();
			str =ROOT_URL + str.substring(10, str.length() - 1);
//			System.out.println("thread + " + id + ": crawling movie at :" + str);
			Movie_Info movie_info = new Movie_Info();

//			Document doc = null;
//			try {
//				doc = Jsoup.connect(str)
//						.userAgent(AGENT).timeout(TIME_OUT).get();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			if(doc == null){
//				return 0;
//			}
//			List<Node> e = doc.getElementById("Zoom").childNode(2).childNodes();
//			Node n = e.get(1);

			String content = getContent(str);
			for(int i = 0; i < MOVIE_PATTERNS.length; i ++){
				parsePattern(movie_info, content, i);
			}
			movie_counter ++;
			ImageWriter.getInstance().addMovieList(movie_info.clone());
			movie_info.setHaiBaoPath("image/" + movie_src + "/" + movie_info.getMovieName() + ".jpg");
			movie_list.add(movie_info);
		}
		DBWriter.getInstance().addMovieList(movie_list);
		return movie_counter;
	}

	/**
	 * 按照正则表达式解析电影信息
	 * @param movie_info : 电影类
	 * @param s : 网页源代码
	 * @param n : 当前正则表达式. 0 : HAIBAO_PATTERN; 1 : PIANMING_PATTERN; 2 : XIAZAIMING_PATTERN.
	 */
	private void parsePattern(Movie_Info movie_info, String s, int n){
		Pattern pt = Pattern.compile(MOVIE_PATTERNS[n]);
		Matcher mt = pt.matcher(s);
		while (mt.find()) {
			//先去除一些特殊的符号
			String str = mt.group();
			str = BasicUtil.formatString(str);
			//用正则表达式进行匹配,通过字符串处理找到相应内容
			switch(n){
			case 0:
				str = str.substring(str.indexOf("src=\"") + 5, str.length() - 1);
				if(!movie_info.hasHaiBaoPath()){
					movie_info.setHaiBaoPath(str.trim());
				}
				break;
			case 1:
				str = str.substring(6, str.length() - 6);
				StringTokenizer st = new StringTokenizer(str, "/");
				if(st.countTokens() == 0){
					movie_info.setMovieName(str.trim());
					movie_info.addName(str.trim());
				}
				while(st.hasMoreElements()){
					String tmp = st.nextToken();
					if(!movie_info.hasName()){
						movie_info.setMovieName(tmp.trim());
					}
					movie_info.addName(tmp.trim());
				}
				break;
			case 2:
				str = str.substring(str.indexOf("\">") + 2);
				movie_info.addDownLoadLinks(str.trim());
				break;
			default:break;
			}
		}
	}
	
}
