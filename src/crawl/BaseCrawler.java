package crawl;

import java.util.concurrent.CountDownLatch;

public abstract class BaseCrawler {

	protected abstract void crawl(int id);
	
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
