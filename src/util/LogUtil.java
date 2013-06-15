package util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class LogUtil {
	private final static String FILE_ROOT = "log";
	private static LogUtil log = null;
	java.util.Date utildate = null;
	SimpleDateFormat sdf = null;
	
	private LogUtil(){
		File f = new File(FILE_ROOT);
		f.mkdir();
	}
	
	public synchronized static LogUtil getInstance(){
		if(log == null){
			log = new LogUtil();
		}
		return log;
	}
	
	public synchronized void write(String log){
		utildate = new java.util.Date();
		File f = getCurLogFile();
		sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = sdf.format(utildate);
		try {
			FileWriter write = new FileWriter(f, true);
			write.append(time + "		" + log + "\n");
			write.flush();
			write.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private synchronized File getCurLogFile(){
		sdf = new SimpleDateFormat("yyyy-MM-dd HH"); 
		String time = sdf.format(utildate);
		sdf = new SimpleDateFormat("mm");
		int minute = Integer.parseInt(sdf.format(utildate));
		minute = minute - minute % 5;
		return new File(FILE_ROOT + "/" + time + " " + minute + ".txt");
	}
}
