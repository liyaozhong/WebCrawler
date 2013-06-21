package crawl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import model.Movie_Info;
import util.BasicUtil;
import util.ConstantUtil;
import util.LogUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import witer.DBWriter;
import witer.ImageWriter;

public class YyetsCrawler extends BaseCrawler{
		
	public static void main(String[] args){
		YyetsCrawler yc = new YyetsCrawler();
		yc.begin();
	}
	
	public YyetsCrawler(){
		movie_src = "Yyets";
		CRAWLABLE_URLS.add("http://www.yyets.com/resourcelist?page=%d&channel=&area=&category=&format=&sort=");
	}
	
	protected void begin(){
		super.begin();
	}
	
	/**
	 * 获取当前最大页
	 * @return MAX_PAGE
	 */
	protected boolean getMaxPage(){
		
		for(int i = 0; i < CRAWLABLE_URLS.size() ; i ++){
			int retry_counter = 0;
			Document doc = null;
			String url = String.format(CRAWLABLE_URLS.get(i), 1);
			try {
				doc = Jsoup.connect(url)
						.userAgent(ConstantUtil.AGENT).timeout(ConstantUtil.TIME_OUT * 2).post();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				LogUtil.getInstance().write(e.getMessage() + "\nIOException : " + url + "\n");
				e.printStackTrace();
			}
			//doc为空，retry2次
			while(doc == null){
				if(++retry_counter < 3){
					LogUtil.getInstance().write(this.getClass().getName() + " : getMaxPage Method getContent return null . retrying time : " + retry_counter);
					try {
						doc = Jsoup.connect(url)
								.userAgent(ConstantUtil.AGENT).timeout(ConstantUtil.TIME_OUT * 2).post();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						LogUtil.getInstance().write(e.getMessage() + "\nIOException : " + url + "\n");
						e.printStackTrace();
					}
				}else{
					break;
				}
			}
			if(doc == null){
				LogUtil.getInstance().write(this.getClass().getName() + " : getMaxPage Method getContent return null \n" + url);
				CRAWLABLE_MAX_PAGE.add(i, 0);
				continue;
			}
			//find last page
			try {
				List<Node> max_page_nodes = doc.getElementsByClass("pages").first().childNode(0).childNodes();
				Node max_page_node = max_page_nodes.get(max_page_nodes.size() - 1).childNode(0);
				String max_page_str = max_page_node.toString();
				max_page_str = max_page_str.substring(max_page_str.lastIndexOf(".") + 1);
				int last_page = Integer.parseInt(max_page_str);
				CRAWLABLE_MAX_PAGE.add(i, last_page);
				LogUtil.getInstance().write("CRAWLABLE_MAX_PAGE : index=" + i + " value=" + last_page);
				System.out.println("last page found: " + last_page);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				LogUtil.getInstance().write(e.getMessage() + "\nException");
				e.printStackTrace();
				CRAWLABLE_MAX_PAGE.add(i, 0);
			}
		}
		return true;
	}
	
	/**
	 * 获取电影信息
	 * @param id : 当前线程ID
	 * @param sUrl : 网页地址
	 * @return 当前页获取电影数量
	 */
	protected int crawlMovies(int id, String sUrl){
		Document doc = null;
		try {
			doc = Jsoup.connect(sUrl)
					.userAgent(ConstantUtil.AGENT).timeout(ConstantUtil.TIME_OUT).post();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(doc == null){
			LogUtil.getInstance().write(this.getClass().getName() + " : crawlMovies Method getContent return null \n" + sUrl);
			return 0;
		}
		
		//finding movies
		try {
			List<Node> movie_nodes = doc.getElementsByClass("AreaR2").first().childNodes()
										.get(5).childNode(1).childNode(1).childNodes();
			doc = null;
			ArrayList<Movie_Info> movies = new ArrayList<Movie_Info>();
			for(int i = 1 ; i < movie_nodes.size() - 1 ; i ++){
				Node movie_mode = movie_nodes.get(i);
				Node e1 = movie_mode.childNode(1).childNode(0);
				String href = e1.attr("href");
				String src = e1.childNode(0).attr("src");
				src = src.replace("m_", "");
				
				Movie_Info info = new Movie_Info();
				info.setHaiBaoPath(BasicUtil.formatString(src));

				//get movie
				try {
					doc = Jsoup.connect(href)
							.userAgent(ConstantUtil.AGENT).timeout(ConstantUtil.TIME_OUT).get();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(doc == null){
					LogUtil.getInstance().write(this.getClass().getName() + " : crawlMovies Method getContent return null \n" + href);
					continue;
				}
				Elements head = doc.head().select("meta");
				for(Element node : head){
					String att = node.attr("name");
					if(att != null && att.equals("keywords")){
						att = node.attr("content");
						int sg = att.indexOf(",");
						if(sg != -1){
							info.setMovieName(att.substring(0, sg));
							int sg2 = att.indexOf(",", sg + 1);
							if(sg2 != -1){
								String other_name = att.substring(sg + 1, sg2);
								boolean split = false;
								if(other_name.contains("/")){
									StringTokenizer st1 = new StringTokenizer(other_name, "/");
									while(st1.hasMoreElements()){
										info.addName(st1.nextToken().trim());
									}
									split = true;
								}
								if(other_name.contains("&")){
									StringTokenizer st2 = new StringTokenizer(other_name, "&");
									while(st2.hasMoreElements()){
										info.addName(st2.nextToken().trim());
									}
									split = true;
								}
								if(!split){
									info.addName(other_name);
								}
							}
						}
					}
				}
//				if(info.getMovieName() != null){
//					ImageWriter.getInstance().addMovieList(info);
//				}
				if(doc.getElementById("tabs") == null){
					continue;
				}
				List<Node> down_load_links_nodes = doc.getElementById("tabs").childNode(5).childNodes();
				for(int ii = 3; ii < down_load_links_nodes.size() - 2 ; ii = ii + 2){
					Node down_load_links_node = down_load_links_nodes.get(ii);
					List<Node> down_load_links_nodes2 = down_load_links_node.childNodes();
					for(int j = 0; j < down_load_links_nodes2.size(); j ++){
						Node n = down_load_links_nodes2.get(j);
						if(n.childNodeSize() == 0){
							continue;
						}
						String down_load_name = n.childNode(0).childNode(0).attr("title");
						ArrayList<String> down_load_links = new ArrayList<String>();
						List<Node> down_load_link_nodes = n.childNode(1).childNodes();
						for(int jj = 0 ; jj < down_load_link_nodes.size(); jj ++){
							Node down_load_link_node = down_load_link_nodes.get(jj);
							String type = down_load_link_node.attr("type");
							if(type.equalsIgnoreCase("ed2k")){
								String ed2k = down_load_link_node.attr("href");
								if(ed2k != null && ed2k.length() != 0){
									down_load_links.add(BasicUtil.formatString(ed2k));
								}
							}else if(type.equalsIgnoreCase("magnet")){
								String magnet = down_load_link_node.attr("href");
								if(magnet != null && magnet.length() != 0){
									down_load_links.add(BasicUtil.formatString(magnet));
								}
							}else if(type.equalsIgnoreCase("pan")){
								String pan_xf = down_load_link_node.attr("xf");
								String pan_thunder = down_load_link_node.attr("thunder");
								if(pan_xf != null && pan_xf.length() != 0){
									down_load_links.add(BasicUtil.formatString(pan_xf));
								}
								if(pan_thunder != null&& pan_thunder.length() != 0){
									down_load_links.add(BasicUtil.formatString(pan_thunder));
								}
							}else if(type.equalsIgnoreCase("")){
								String thunderhref = down_load_link_node.attr("thunderhref");
								String fg = down_load_link_node.attr("fg");
								if(thunderhref != null && thunderhref.length() != 0){
									down_load_links.add(BasicUtil.formatString(thunderhref));
								}
								if(fg != null && fg.length() != 0){
									down_load_links.add(BasicUtil.formatString(fg));
								}
							}

						}
//						info.addName(BasicUtil.formatString(down_load_name));
						//RegexUtil.getMovieName(BasicUtil.formatString(down_load_name));
						info.addDownLoadLinks(down_load_links, down_load_name);
					}
					
				}
				movies.add(info);
			}
			DBWriter.getInstance().addMovieList(movies);
			return movies.size();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LogUtil.getInstance().write(e.getMessage() + "\nException");
			e.printStackTrace();
			return 0;
		}
	}
	
}
