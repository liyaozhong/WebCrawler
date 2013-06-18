package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class BasicUtil {
	
	/**
	 * 去除特殊符号
	 * @param str
	 * @return
	 */
	public static String formatString(String str){
		if(str.contains("&nbsp;")){
			str = str.replaceAll("&nbsp;", "");
		}
		if(str.contains("&amp;")){
			str = str.replaceAll("&amp;", "&");
		}
		if(str.contains("&middot;")){
			str = str.replaceAll("&middot;", ".");
		}
		if(str.contains("&rsquo;")){
			str = str.replaceAll("&rsquo;", "’");
		}
		if(str.contains("&mdash;")){
			str = str.replaceAll("&mdash;", "—");
		}
		if(str.contains("&ndash;")){
			str = str.replaceAll("&ndash;", "–");
		}
		if(str.contains("&quot;")){
			str = str.replaceAll("&quot;", "\"");
		}
		if(str.contains("&deg;")){
			str = str.replaceAll("&quot;", "°");
		}
		return str;
	}
	
	private static char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	public static String getMD5(byte[] src){
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(src);
			byte[] tmp = md.digest();
			char[] result = new char[tmp.length * 2];
			int k = 0;
			for(int i = 0 ;i < tmp.length; i ++){
				byte b = tmp[i];
				result[k++] = hexDigits[b >>> 4 & 0xf];
				result[k++] = hexDigits[b & 0xf];
			}
			return new String(result);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
