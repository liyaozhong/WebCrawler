package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtil {
	
	/**
	 * 电影 特殊类型：[垃圾文字]中文名字
	 */
	private static final String regex0 = "^(\\[|【)[^\\[\\]【】]+(\\]|】)( |\\.|-)?[^\\[\\]\\.]+(\\.)?(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex0_name = "^(\\[|【)[^\\[\\]【】]+(\\]|】)( |\\.|-)?[^\\[\\]\\.]+";
	
	/**
	 * 中文名和英文名共同显示,通过S01E01或S01E01E02或S01或EP01或1X01或E01或Season1或SEASON1方式表示第几集
	 */
	private static final String regex1 = "([^\\. ]*[^a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]+[^\\. ]*)(\\.| )(.*)(Season\\d\\d?|season\\d\\d?|SEASON\\d\\d?|((S|s)\\d\\d?((E|e)\\d\\d?){1,2})|(S|s)\\d\\d?|(E|e)(P|p)\\d\\d?|(E|e)\\d\\d?|\\d\\d?x\\d\\d?)(\\.|-| ).*(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex1_name = "([^\\. ]*[^a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]+[^\\. ]*)(\\.| )";
	
	/**
	 * 只显示英文名,通过Season1方式表示第几集
	 */
	private static final String regex2 = "^([a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]*)(\\.|-| |\\()(Season\\d\\d?|season\\d\\d?|SEASON\\d\\d?)(\\.|-| |\\)).*(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex2_name = "^([a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]*)(\\.|-| |\\()(Season\\d\\d?|season\\d\\d?|SEASON\\d\\d?)";
	
	/**
	 * 只显示英文名,通过S01E01或S01E01E02或S01或EP01或1X01或E01方式表示第几集
	 * 特例：aaf-ub.s01e06r，增加(R|r)?
	 * 特例：The Guardian.S01E17-22，增加-?
	 */
	private static final String regex3 = "^([a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]*)(\\.|-| |\\()(((S|s)\\d\\d?((E|e)\\d\\d?){1,2})|(S|s)\\d\\d?|(E|e)(P|p)\\d\\d?|(E|e)\\d\\d?|\\d\\d?x\\d\\d?)(R|r)?-?(\\.|-| |\\)).*(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex3_name = "^([a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]*)(\\.|-| |\\()(((S|s)\\d\\d?((E|e)\\d\\d?){1,2})|(S|s)\\d\\d?|(E|e)(P|p)\\d\\d?|(E|e)\\d\\d?|\\d\\d?x\\d\\d?)(R|r)?-?";
		
	/**
	 * 通过中括号区分字段的
	 */
	private static final String regex4 = "(\\[([^\\[\\]]+)\\])(\\[((S|s)\\d\\d?(E|e)\\d\\d?).*\\]).*\\.(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex4_name = "(\\[([^\\[\\]]+)\\])";
	
	/**
	 * 电影类，显示中文名字和英文名字,由[]连接(要求不止一个[])
	 */
	private static final String regex5 = "^\\[([^\\.]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\. ]*)\\] *\\[(.*)(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex5_name = "^\\[([^\\.]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\. ]*)\\] *\\";
	
	/**
	 * 电影 特殊类型：先英文后中文 +　HR-HDTV
	 * Adulthood.成年之殇.双语字幕.HR-HDTV.AC3.960X518.x264-人人影视制作.avi
	 */
	private static final String regex6 = "^[a-zA-Z0-9\\. ]+(\\.| )([^a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]+\\d*)+(\\.| ).*?(HR-HDTV).*?(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex6_name = "^[a-zA-Z0-9\\. ]+(\\.| )([^a-zA-Z0-9\\._ --'～~\\[\\]\\(\\)]+\\d*)+(\\.| )";
		
	/**
	 * 电影类，显示中文名字和英文名字,由空格或_连接
	 */
	private static final String regex7 = "^([^\\._ ]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\._ ]*)( |_)(.*)(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex7_name = "^([^\\._ ]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\._ ]*)( |_)";
	
	/**
	 * 电影类，显示中文名字和英文名字,由.连接
	 */
	private static final String regex8 = "^([^\\.]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\.]*)\\.(.*)(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex8_name = "^([^\\.]*[^a-zA-Z0-9\\._ --'～~\\[\\]]+[^\\.]*)\\.";
	
	/**
	 * 匹配到第一个表示年份字段之前的部分
	 * 包括：用空格和.进行分割
	 */
	private static final String regex9 = "^.*?(\\.| )\\d{4}(\\.| ).*(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex9_name = "^.*?(\\.| )\\d{4}(\\.| )";
	
	/**
	 * 匹配到第一个全数字字段之前的部分
	 * 包括：用空格和.进行分割
	 */
	private static final String regex10 = "^.*?(\\.| )(\\d)+(\\.| ).*(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex10_name = "^.*?(\\.| )(\\d)+(\\.| )";
	
	/**
	 * 匹配到第一个00p或00P之前的部分
	 * 包括：用空格和.进行分割
	 */
	private static final String regex11 = "^.*?(\\.| )(\\d)+(P|p)(\\.| |-).*(mp4|avi|rmvb|mkv|mpg)?";
	private static final String regex11_name = "^.*?(\\.| )(\\d)+(P|p)(\\.| |-)";
	
	
	private static final String[] regex = {regex0, regex1,regex2, regex3, regex4, regex5, regex6, regex7, regex8, regex9, regex10,
										   regex11};
	private static final String[] regex_name = {regex0_name, regex1_name, regex2_name, regex3_name, regex4_name, regex5_name, 
												regex6_name, regex7_name, regex8_name, regex9_name, regex10_name, regex11_name};
	
	
	public static void getMovieName(String down_load_name){
		boolean match = false;
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
					case 0:
						int begin = str2.indexOf("]") == -1? str2.indexOf("】") : str2.indexOf("]");
						str2 = str2.substring(begin + 1, str2.length());
						str2 = str2.trim();
						str2 = str2.replaceAll("\\.", "");
						str2 = str2.replaceAll("-", "");
						break;
					case 1 : case 6: case 7: case 8:
						str2 = str2.substring(0, str2.length() - 1);
						break;
					case 5:
						str2 = str2.substring(1, str2.indexOf("]") - 1);
						break;
					case 2: case 3:
						if(str2.lastIndexOf(".") == -1){
							if(str2.lastIndexOf(" ") == -1){
								match = true;
								break;
							}
							str2 = str2.substring(0, str2.lastIndexOf(" "));
							break;
						}
						str2 = str2.substring(0, str2.lastIndexOf("."));
						str2 = str2.replaceAll("\\.", " ");
						break;
					case 4:
						str2 = str2.substring(1, str2.length() - 1);
						break;
					case 9 : case 10: case 11:
						str2 = str2.substring(0, str2.length() - 2);
						if(str2.lastIndexOf(".") == -1){
							if(str2.lastIndexOf(" ") == -1){
								match = true;
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
					System.out.print("match regex " + i + " : " + str2);
					System.out.println("__________" + down_load_name);
				}
				else{
					System.out.println("no name__________" + down_load_name);
				}
				match = true;
				break;
			}
		}
		if(!match){
			System.out.println("		" + down_load_name);
		}
	}
	
	
	/**
	 * 问题测试：BBC 地平线;二战启示录;冒牌天神;冰河世纪三部曲 & 番外篇 合辑;发展受阻;同志亦凡人;地下拳击场;权力的游戏;特殊案件专案组;神秘博士2005版;绿箭;虎胆龙威5;行星边际2
	 */
	public static void main(String args[]){
		File f = new File("regextest");
		if(f.isDirectory()){
			File[] list = f.listFiles();
			
			for(int i = 213 ;i < list.length; i ++){
				BufferedReader stdin =new BufferedReader(new InputStreamReader(System.in));
				try {
					stdin.read();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				File tmp = list[i];
				System.out.println("-----------------------current file : " + tmp.getName() + "  id : " + i + "-----------------------");
				try {
					FileReader reader = new FileReader(tmp);
					BufferedReader buffer = new BufferedReader(reader);
					String line = null;
					buffer.readLine();
					while((line = buffer.readLine()) != null){
						if(line.length() != 0){
							getMovieName(line);
						}
					}
					buffer.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("-----------------------done-----------------------");
			}
		}
	}
}
