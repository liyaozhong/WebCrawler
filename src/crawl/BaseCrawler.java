package crawl;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import util.ConstantUtil;
import util.LogUtil;
import witer.DBWriter;
import witer.ImageWriter;

public abstract class BaseCrawler {

	protected static ArrayList<String> CRAWLABLE_URLS = new ArrayList<String>();
	protected static ArrayList<Integer> CRAWLABLE_MAX_PAGE = new ArrayList<Integer>();
	protected static String movie_src;
	protected static int current_category = 0;
	protected static int current_page = 1;
	
	/**
	 * 指定线程按规则爬取页面
	 * @param id : 当前线程ID
	 */
	protected final void crawl(int id){
		int total = 0;		
		while(true){
			String sUrl = getNextPage();
			if(sUrl == null){
				break;
			}
			LogUtil.getInstance().write("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": crawling : " + sUrl + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": crawling : " + sUrl + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			int counter = crawlMovies(id, sUrl);
			total += counter;
			LogUtil.getInstance().write("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": " + counter + " movies crawled++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": " + counter + " movies crawled++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		}
		LogUtil.getInstance().write("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": " + total + " movies crawled++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
		System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++thread " + id + ": " + total + " movies crawled++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	}
	
	protected abstract int crawlMovies(int id, String sUrl);
	
	protected abstract boolean getMaxPage();
	
	protected void begin(){
		//文件目录初始化
		File f = new File(ConstantUtil.IMAGE_ROOT_DIR + movie_src);
		f.mkdir();
		//获取max page
		if(!getMaxPage()){
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++get max page failed! halting++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
			return;
		}
		DBWriter.getInstance().start();
		ImageWriter.getInstance().setMovieSrc(movie_src);
		ImageWriter.getInstance().start();
		ExecutorService exe = Executors.newFixedThreadPool(ConstantUtil.THREAD_NUM);
		CountDownLatch cdl = new CountDownLatch(ConstantUtil.THREAD_NUM);
		for(int thread_id = 1; thread_id <= ConstantUtil.THREAD_NUM; thread_id ++){
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
	
	protected synchronized final String getNextPage(){
		if(current_category == CRAWLABLE_MAX_PAGE.size()){
			return null;
		}
		if(current_page <= CRAWLABLE_MAX_PAGE.get(current_category)){
			return String.format(CRAWLABLE_URLS.get(current_category), current_page ++);
		}else{
			current_category ++;
			if(current_category == CRAWLABLE_MAX_PAGE.size()){
				return null;
			}
			current_page = 1;
			return String.format(CRAWLABLE_URLS.get(current_category), current_page ++);
		}
	}
	
	class MoiveCrawler implements Runnable{
		
		private CountDownLatch cdl;
		private int id;
		public MoiveCrawler(CountDownLatch cdl, int id){
			this.cdl = cdl;
			this.id = id;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			crawl(id);
			cdl.countDown();
		}
		
	}
}
