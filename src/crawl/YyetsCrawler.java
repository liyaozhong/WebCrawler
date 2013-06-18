package crawl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import model.Movie_Info;
import util.BasicUtil;
import util.LogUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

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
		File f = new File("regextest");
		f.mkdir();
		super.begin();
	}
	
	/**
	 * 获取当前最大页
	 * @return MAX_PAGE
	 */
	protected boolean getMaxPage(){
		
		for(int i = 0; i < CRAWLABLE_URLS.size() ; i ++){
			Document doc = null;
			String url = String.format(CRAWLABLE_URLS.get(i), 1);
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
				List<Node> max_page_nodes = doc.getElementsByClass("pages").first().childNode(0).childNodes();
				Node max_page_node = max_page_nodes.get(max_page_nodes.size() - 1).childNode(0);
				String max_page_str = max_page_node.toString();
				max_page_str = max_page_str.substring(max_page_str.lastIndexOf(".") + 1);
				int last_page = Integer.parseInt(max_page_str);
				CRAWLABLE_MAX_PAGE.add(i, last_page);
				System.out.println("last page found: " + last_page);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LogUtil.getInstance().write(this.getClass().getName() + "	[error] getting max page at URL : " + url);
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
				Node e2 = movie_mode.childNode(3).childNode(1).childNode(1).childNode(1).childNode(1).childNode(0);
				String movie_name_node = e2.toString();
				String movie_name = movie_name_node.substring(movie_name_node.indexOf("《") + 1, movie_name_node.lastIndexOf("》"));
				
				Movie_Info info = new Movie_Info();
				info.setHaiBaoPath(BasicUtil.formatString(src));
				movie_name =  BasicUtil.formatString(movie_name);
				StringTokenizer st = new StringTokenizer(movie_name, "/");
				if(st.countTokens() == 0){
					info.setMovieName(movie_name.trim());
				}
				while(st.hasMoreElements()){
					String tmp = st.nextToken();
					if(!info.hasName()){
						info.setMovieName(tmp.trim());
					}else{
						info.addName(tmp.trim());
					}
				}
				if(info.getMovieName() != null){
					ImageWriter.getInstance().addMovieList(info);
				}
				//get movie
				try {
					doc = Jsoup.connect(href)
							.userAgent(AGENT).timeout(TIME_OUT).get();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(doc == null){
					continue;
				}
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
						info.addDownLoadLinks(down_load_links);
					}
					
				}
				movies.add(info);
				
				/** regex testing**/
				FileWriter write = new FileWriter(new File("regextest/" + info.getMovieName() + ".txt"));
				for(int o = 0; o < info.getNames().size(); o ++){
					write.write(info.getNames().get(o) + "\r\n");
				}
				write.flush();
				write.close();
			}
			DBWriter.getInstance().addMovieList(movies);
			return movies.size();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LogUtil.getInstance().write(this.getClass().getName() + "	[error] crawling movies at URL : " + sUrl);
			return 0;
		}
	}
	
}
