package crawl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import model.Movie_Info;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import util.LogUtil;
import witer.DBWriter;
import witer.ImageWriter;

public class M1905Crawler extends BaseCrawler{
	private final static int TIME_OUT = 5000;
	private final static String AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)";
	private final static String M1905_MOVIE_URL = "http://www.m1905.com/mdb/film/list/year-%d/o0d0p%d.html";
	private final static String ID_NEW_PAGE = "new_page";
	private final static String ATT_SCR = "src";
	private final static String ATT_ALT = "alt";
	private final static String ATT_HREF = "href";
	private final static String NO_PIC = "nopic.gif";
	private final static int[] YEAR = {2013,2012,2011,2010,2009,2008,2007,2006,2005,2004,2003,2001,2000};
	private static int[] LAST_PAGE = {0,0,0,0,0,0,0,0,0,0,0,0,0};
	
	public static void main(String[] args){
		M1905Crawler mc = new M1905Crawler();
		mc.begin();
	}
	
	private final String movie_src = "M1905";
	private final static int THREAD_NUM = 10;
	private void begin(){
		//文件目录初始化
		File f = new File("image/" + movie_src);
		f.mkdir();
		//获取max page
		for(int i = 0 ; i< YEAR.length; i ++){
			if(!getMaxPage(i)){
				System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++get max page failed! at year" + YEAR[i] + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
				continue;
			}
		}
		ImageWriter.getInstance().setMovieSrc(movie_src);
		ImageWriter.getInstance().start();
		DBWriter.getInstance().setDBName(movie_src);
		DBWriter.getInstance().start();
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
		DBWriter.getInstance().halt();
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
				String sUrl = String.format(M1905_MOVIE_URL, YEAR[i], page_counter);
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
		try {
			Elements inqList_childs = doc.getElementById(ID_NEW_PAGE).previousElementSibling().children();
			doc = null;
			ArrayList<Movie_Info> movie_list = new ArrayList<Movie_Info>();
			for(int i= 0; i < inqList_childs.size(); i ++){
				Element movie_elements =  inqList_childs.get(i).children()
						.first();
				String href = movie_elements.attr(ATT_HREF);
				Element movie_element =  movie_elements.children().first();
				String src = movie_element.attr(ATT_SCR);
				String alt = movie_element.attr(ATT_ALT);
				if(alt != null && src != null){
					src = src.substring(0, src.lastIndexOf("/") + 1) + 
							src.substring(src.lastIndexOf("_") + 1, src.length());
					Movie_Info movie = new Movie_Info(alt, src);
					//含有nopic.gif表示当前电影没有海报
					if(!src.endsWith(NO_PIC)){
						ImageWriter.getInstance().addMovieList(movie);
					}
					
					//获取影片译名
					try {
						doc = Jsoup.connect(href)
								.userAgent(AGENT).timeout(TIME_OUT).post();
						Node other_name_node = doc.getElementsByClass("laMovName").first().child(1).child(0).childNode(0);
						if(other_name_node.childNodeSize() != 0){
							String other_name = other_name_node.childNode(0).toString();
							if(other_name.length() != 0){
								movie.addName(other_name);
							}
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					movie_list.add(movie);
				}
			}
			DBWriter.getInstance().addMovieList(movie_list);
			return inqList_childs.size();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LogUtil.getInstance().write("[error] crawling movies at URL : " + sUrl);
			return 0;
		}
	}
	
	/**
	 * 获取当前最大页
	 * @return MAX_PAGE
	 */
	private boolean getMaxPage(int year){
		Document doc = null;
		String url = String.format(M1905_MOVIE_URL, YEAR[year], 1);
		try {
			doc = Jsoup.connect(url)
					.userAgent(AGENT).timeout(TIME_OUT * 2).post();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(doc == null){
			return false;
		}
		//find last page
		try {
			Elements totall_num_elements = doc.getElementsByClass("termsBox");
			Node totall_num_node = totall_num_elements.first().childNode(5).childNode(0);
			String totall_num_str = totall_num_node.toString();
			String num = totall_num_str.substring(2, totall_num_str.length() - 3);
			LAST_PAGE[year] = Integer.parseInt(num) / 30 + 1;
			System.out.println("last_page = " + LAST_PAGE[year] + " at year " + YEAR[year]);
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LogUtil.getInstance().write("[error] getting max page at URL : " + url);
			return false;
		}
	}
}

