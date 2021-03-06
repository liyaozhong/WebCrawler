package util;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtil {
	
	/**
	 * 中文名和英文名共同显示,通过S01E01或S01E01E02或S01E01_02或S01或EP01或1X01或E01或Season1或SEASON1方式表示第几集
	 */
	private static final String regex0 = "([^\\. \\[\\]]*[^a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]+[^\\. ]*)(\\.| )(.*)(Season\\d\\d?|season\\d\\d?|SEASON\\d\\d?|((S|s)\\d\\d?((E|e)\\d\\d?){1,2})|(S|s)\\d\\d?(E|e)\\d\\d?_\\d\\d?|(S|s)\\d\\d?|(E|e)(P|p)\\d\\d?|(E|e)\\d\\d?|\\d\\d?x\\d\\d?)(\\.|-| |\\[).*?(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex0_name = "([^\\. \\[\\]]*[^a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]+[^\\. ]*)(\\.| )";
	
	/**
	 * 只显示英文名,通过Season1方式表示第几集
	 */
	private static final String regex1 = "^([a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]*)(\\.|-| |\\()(Season\\d\\d?|season\\d\\d?|SEASON\\d\\d?)(\\.|-| |\\)).*(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex1_name = "^([a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]*)(\\.|-| |\\()(Season\\d\\d?|season\\d\\d?|SEASON\\d\\d?)";
	
	/**
	 * 只显示英文名,通过S01E01或S01E01E02或S01E01_02或S01或EP01或1X01或E01方式表示第几集
	 * 特例：aaf-ub.s01e06r，增加(R|r)?
	 * 特例：The Guardian.S01E17-22，增加-?
	 */
	private static final String regex2 = "^([a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]*)(\\.|-| |\\()(((S|s)\\d\\d?((E|e)\\d\\d?){1,2})|(S|s)\\d\\d?(E|e)\\d\\d?_\\d\\d?|(S|s)\\d\\d?|(E|e)(P|p)\\d\\d?|(E|e)\\d\\d?|\\d\\d?x\\d\\d?)(R|r)?-?(\\.|-| |\\)).*(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex2_name = "^([a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]*)(\\.|-| |\\()(((S|s)\\d\\d?((E|e)\\d\\d?){1,2})|(S|s)\\d\\d?(E|e)\\d\\d?_\\d\\d?|(S|s)\\d\\d?|(E|e)(P|p)\\d\\d?|(E|e)\\d\\d?|\\d\\d?x\\d\\d?)(R|r)?-?";
		
	/**
	 * 电影 特殊类型：[垃圾文字]英文名字.720p/年代
	 */
	private static final String regex3 = "^(\\[|【)?[^\\[\\]【】]+(\\]|】)( |\\.|-)*[a-zA-Z\\d\\.\\ ]+\\.\\d{3,4}p?\\..*?(mp4|avi|rmvb|mkv|mpg)";
	private static final String regex3_name = "^(\\[|【)?[^\\[\\]【】]+(\\]|】)( |\\.|-)*[a-zA-Z\\d\\.\\ ]+\\.\\d{3,4}p?";
	
	/**
	 * 电影 特殊类型：[垃圾文字]中文名字(或英文名字，不包含'.')DVD/HD/BD
	 */
	private static final String regex4 = "^(\\[|【)?[^\\[\\]【】]+(\\]|】)( |\\.|-)*[^\\[\\]\\.]+(DVD|HD|BD|hd|bd).*?(mp4|avi|rmvb|mkv|mpg)";
	private static final String regex4_name = "^(\\[|【)?[^\\[\\]【】]+(\\]|】)( |\\.|-)*[^\\[\\]\\.]+(DVD|HD|BD|hd|bd)";
	
	/**
	 * 电影 特殊类型：[垃圾文字]中文名字(截取第一个'.'之前)
	 */
	private static final String regex5 = "^(\\[|【)?[^\\[\\]【】]+(\\]|】)( |\\.|-)*[^\\[\\]\\.]+(\\.)?.*?(mp4|avi|rmvb|mkv|mpg)";
	private static final String regex5_name = "^(\\[|【)?[^\\[\\]【】]+(\\]|】)( |\\.|-)*[^\\[\\]\\.]+";
	
	
	/**
	 * 通过中括号区分字段的,包含S01E01等信息
	 */
	private static final String regex6 = "(\\[([^\\[\\]]+)\\])(\\[(Season\\d\\d?|season\\d\\d?|SEASON\\d\\d?|((S|s)\\d\\d?((E|e)\\d\\d?){1,2})|(S|s)\\d\\d?(E|e)\\d\\d?_\\d\\d?|(S|s)\\d\\d?|(E|e)(P|p)\\d\\d?|(E|e)\\d\\d?|\\d\\d?x\\d\\d?).*\\]).*\\.(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex6_name = "(\\[([^\\[\\]]+)\\])";
	
	/**
	 * 电影类，显示中文名字和英文名字,由[]连接(要求不止一个[],[]中不含'.')
	 */
	private static final String regex7 = "^\\[([^\\.\\[\\]]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\.\\[\\]]*)\\] *\\[(.*?)(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex7_name = "^\\[([^\\.\\[\\]]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\.\\[\\]]*)\\] *\\[";
	
	/**
	 * 电影 特殊类型：先英文后中文 +　HR-HDTV
	 * Adulthood.成年之殇.双语字幕.HR-HDTV.AC3.960X518.x264-人人影视制作.avi
	 */
	private static final String regex8 = "^[a-zA-Z0-9\\. ]+(\\.| )([^a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]+\\d*)+(\\.| ).*?(HR-HDTV).*?(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex8_name = "^[a-zA-Z0-9\\. ]+(\\.| )([^a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]+\\d*)+(\\.| )";
		
	/**
	 * 电影类，显示中文名字和英文名字,由空格或_连接
	 */
	private static final String regex9 = "^([^\\._ \\[\\]]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\._ \\[\\]]*)( |_)(.*?)(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex9_name = "^([^\\._ \\[\\]]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\._ \\[\\]]*)( |_)";
	
	/**
	 * 电影类，显示中文名字和英文名字,由.连接
	 */
	private static final String regex10 = "^([^\\.\\[\\]]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\.\\[\\]]*)\\.(.*?)(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex10_name = "^([^\\.\\[\\]]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\.\\[\\]]*)\\.";
	
	/**
	 * 匹配到第一个表示年份字段之前的部分
	 * 包括：用空格和.进行分割
	 */
	private static final String regex11 = "^.*?(\\.| )\\d{4}(\\.| ).*?(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex11_name = "^.*?(\\.| )\\d{4}(\\.| )";
	
	/**
	 * 匹配到第一个全数字字段之前的部分
	 * 包括：用空格和.进行分割
	 */
	private static final String regex12 = "^.*?(\\.| )(\\d)+(\\.| ).*(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex12_name = "^.*?(\\.| )(\\d)+(\\.| )";
	
	/**
	 * 匹配到第一个00p或00P之前的部分
	 * 包括：用空格和.进行分割
	 */
	private static final String regex13 = "^.*?(\\.| )(\\d)+(P|p)(\\.| |-).*(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex13_name = "^.*?(\\.| )(\\d)+(P|p)(\\.| |-)";
	
	
	private static final String[] regex = {regex0, regex1,regex2, regex3, regex4, regex5, regex6, regex7, regex8, regex9, regex10,
										   regex11, regex12, regex13};
	private static final String[] regex_name = {regex0_name, regex1_name, regex2_name, regex3_name, regex4_name, regex5_name, 
												regex6_name, regex7_name, regex8_name, regex9_name, regex10_name, regex11_name,
												regex12_name, regex13_name};
	
	
	public static String getMovieName(String down_load_name){
		for(int i = 0 ; i < regex.length; i ++){
			Pattern pt = Pattern.compile(regex[i]);
			Matcher mt = pt.matcher(down_load_name);
			//System.err.print(i);
			if(mt.find()){
				String str = mt.group();
				Pattern pt2 = Pattern.compile(regex_name[i]);
				Matcher mt2 = pt2.matcher(str);
				if(mt2.find()){
					String str2 = mt2.group();
					switch (i){
					case 3:
						int begin3 = str2.indexOf("]") == -1? str2.indexOf("】") : str2.indexOf("]");
						str2 = str2.substring(begin3 + 1, str2.lastIndexOf("."));
						str2 = str2.replaceAll("\\.", " ");
						str2 = str2.trim();
						break;
					case 4:
						int begin4 = str2.indexOf("]") == -1? str2.indexOf("】") : str2.indexOf("]");
						if(str2.endsWith("DVD")){
							str2 = str2.substring(begin4 + 1, str2.length() - 3);
						}else if(str2.endsWith("HD") || str2.endsWith("BD") || str2.endsWith("hd") || str2.endsWith("bd")){
							str2 = str2.substring(begin4 + 1, str2.length() - 2);
						}
						str2 = str2.trim();
						str2 = str2.replaceAll("\\.", "");
						break;
					case 5:
						int begin5 = str2.indexOf("]") == -1? str2.indexOf("】") : str2.indexOf("]");
						str2 = str2.substring(begin5 + 1, str2.length());
						str2 = str2.trim();
						str2 = str2.replaceAll("\\.", "");
						//str2 = str2.replaceAll("-", "");
						break;
					case 0 : 
						str2 = str2.substring(0, str2.length() - 1);
						if(str2.contains("]")){
							int begin0 = str2.indexOf("]");
							if(begin0 == str2.length() - 1 || str2.contains("[")){
								str2 = str2.substring(0, begin0);
							}else{
								str2 = str2.substring(begin0 + 1, str2.length());
							}
						}
						break;
					case 8: case 9: case 10:
						str2 = str2.substring(0, str2.length() - 1);
						break;
					case 7:
						str2 = str2.substring(1, str2.indexOf("]") - 1);
						break;
					case 1: case 2:
						if(str2.lastIndexOf(".") == -1){
							if(str2.lastIndexOf(" ") == -1){
								break;
							}
							str2 = str2.substring(0, str2.lastIndexOf(" "));
							break;
						}
						str2 = str2.substring(0, str2.lastIndexOf("."));
						str2 = str2.replaceAll("\\.", " ");
						break;
					case 6:
						str2 = str2.substring(1, str2.length() - 1);
						break;
					case 11 : case 12: case 13:
						str2 = str2.substring(0, str2.length() - 2);
						if(str2.lastIndexOf(".") == -1){
							if(str2.lastIndexOf(" ") == -1){
								break;
							}
							str2 = str2.substring(0, str2.lastIndexOf(" "));
							break;
						}
						str2 = str2.substring(0, str2.lastIndexOf("."));
						str2 = str2.replaceAll("\\.", " ");
						break;
					default :
						break;
					}
					return str2;
				}
				else{
					return null;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * 去除括号
	 */
	private static final String fromat_regex0_match = "^[^\\(\\)（）]*";
	/**
	 * 匹配前置第几集
	 */
	private static final String fromat_regex1_match = "^(第.*?集)(:|：)";
	/**
	 * 匹配结尾
	 */
	private static final String format_regex2_match = "-?(第.*?集|卷.|国语|(c|C)(d|D)\\d|\\d+分钟加长版|TC抢先版|R5中字|R5中英双字|加长版|导演剪辑版|电影版|中英双字|特别加长版|(s|S)\\d\\d?| vol)$";
	/**
	 * 匹配前置中括号,中括号内容无用
	 */
	private static final String format_regex3_match = "^(\\[|\\【)(预告片)(\\]|\\】)";
	/**
	 * 匹配前置中括号,中括号内容为电影名
	 */
	private static final String format_regex4_match = "^(\\[|\\【)[^\\]\\】]+(\\]|\\】)";
	/**
	 * 匹配英文名.年代.中文名(截取英文部分)
	 */
	private static final String format_regex5_match = "^[a-zA-Z0-9\\.]+\\.\\d{4}\\.";
	
	private static final String[] fomat_regex_match = {fromat_regex0_match, fromat_regex1_match, format_regex2_match, format_regex3_match, format_regex4_match,
														format_regex5_match};

	public static String formatMovieName(String movie_name){
		for(int i = 0 ; i < fomat_regex_match.length; i ++){
			Pattern pt = Pattern.compile(fomat_regex_match[i]);
			Matcher mt = pt.matcher(movie_name);
			if(mt.find()){
				switch (i){
				case 0:
					movie_name = mt.group();
					break;
				case 1:
					movie_name = movie_name.substring(mt.group().length());
					break;
				case 2:
					movie_name = movie_name.substring(0, movie_name.length() - mt.group().length());
					break;
				case 3:
					movie_name = movie_name.substring(mt.group().length());
					break;
				case 4:
					movie_name = mt.group();
					movie_name = movie_name.substring(1, movie_name.length() - 1);
					break;
				case 5:
					movie_name = mt.group();
					movie_name = movie_name.substring(0, movie_name.length() - 6);
					break;
				}
			}
		}
		
		movie_name = movie_name.replace("_", " ");
		movie_name = movie_name.replace("-", " ");
		movie_name = movie_name.replace(".", " ");
		return movie_name.trim();
	}
	
	public static void main(String args[]){
		
		Statement stmt = null;
		Connection conn = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/Yyets", "root", "liyaozhong");
			stmt = conn.createStatement();
			String sql = "select * from moviedownloadlinks order by 'MOVIE_NAME' ASC";
			ResultSet result = stmt.executeQuery(sql);
			BufferedReader stdin =new BufferedReader(new InputStreamReader(System.in));
			int counter = 0;
			while(result.next()){
				String downloadname = result.getString(3);
				String movie_name = getMovieName(downloadname);
				System.out.println(movie_name + "		" + downloadname);
				if(movie_name != null)
					System.out.println(formatMovieName("======" + movie_name));
				if(counter < 10){
					counter ++;
				}else{
					System.out.println("====================================================");
					counter = 0;
					try {
						stdin.read();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			try {
				if(stmt != null){
					stmt.close();
				}
				if(conn != null){
					conn.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
}
