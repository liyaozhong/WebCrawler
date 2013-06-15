package crawl;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.Movie_Info;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import witer.ImageWriter;

public class MtimeCrawler extends BaseCrawler{
	private final static int TIME_OUT = 5000;
	private final static String AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)";
	private final static String Mtime_MOVIE_URL = "http://movie.mtime.com/movie/search/section/#pageIndex=%d&year=%d";
	private final static int[] YEAR = new int[126];
	private static int[] LAST_PAGE = new int[126];
	
	public static void main(String[] args){
		MtimeCrawler mc = new MtimeCrawler();
		mc.begin();
	}
	
	private final String movie_src = "Mtime";
	private final static int THREAD_NUM = 10;
	private void begin(){
		//文件目录初始化
		File f = new File("image/" + movie_src);
		f.mkdir();
		//初始化YEAR数组,从1888 - 2013
		for(int i = 0 ; i < YEAR.length; i ++){
			YEAR[i] = 2013 - i;
		}
		//获取max page
		for(int i = 0 ; i< YEAR.length; i ++){
			if(!getMaxPage(i)){
				System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++get max page failed! at year" + YEAR[i] + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				continue;
			}
		}
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
		ImageWriter.getInstance().halt();
		exe.shutdown();
	}
	
	/**
	 * 指定线程按规则爬取页面
	 * @param id : 当前线程ID
	 */
	protected void crawl(int id){
		int total = 0;
		for(int i = 0 ; i< YEAR.length; i ++){
			int page_counter = 0 + id;
			for(;page_counter <= LAST_PAGE[i]; page_counter += THREAD_NUM){
				System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": crawling page: " + page_counter + " year: " + YEAR[i] + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				String sUrl = String.format(Mtime_MOVIE_URL, page_counter, YEAR[i]);
				int counter = crawlMovies(id, sUrl);
				System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": " + counter + " movies crawled at page: " + page_counter + " year : " + YEAR[i] + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				total += counter;
			}
		}
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": " + total + " movies crawled++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}
	
	/**
	 * 获取电影信息
	 * @param id : 当前线程ID
	 * @param sUrl : 网页地址
	 * @return 当前页获取电影数量
	 */
	private int crawlMovies(int id, String sUrl){
		Document doc = null;
		try {
			doc = Jsoup.connect(sUrl)
					.userAgent(AGENT).timeout(TIME_OUT).post();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(doc == null){
			return 0;
		}
		
		//finding movies
		
		return 0;
	}
	
	/**
	 * 获取当前最大页
	 * @return MAX_PAGE
	 */
	private boolean getMaxPage(int year){
		Document doc = null;
		String url = String.format(Mtime_MOVIE_URL, 1, YEAR[year]);
		try {
			doc = Jsoup.connect(url)
					.userAgent(AGENT).timeout(TIME_OUT * 2).get();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(doc == null){
			return false;
		}
		//find last page
		Element e = doc.getElementById("searchResultRegion");
		
		System.out.println();
		return true;
	}
}
