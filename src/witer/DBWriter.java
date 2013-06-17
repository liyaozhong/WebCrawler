package witer;
import java.sql.*;
import java.util.ArrayList;

import crawl.BaseCrawler;

import util.LogUtil;

import model.Movie_Info;

public class DBWriter {
	private static final String driver = "com.mysql.jdbc.Driver";
	private static final String url = "jdbc:mysql://127.0.0.1:3306/";
	private static String db_name = BaseCrawler.class.getName();
	private static final String root = "root";
	private static final String password = "liyaozhong";
	
	private static void convertForMySQL(ArrayList<Movie_Info> movie_list){
		for(int i = 0; i < movie_list.size(); i ++){
			Movie_Info info = movie_list.get(i);
			movie_list.set(i, info.convertForMySQL());
		}
	}
	private static ArrayList<String> creatMovie_InfoSQL(ArrayList<Movie_Info> movie_list){
		ArrayList<String> sql = new ArrayList<String>();
		for(int i = 0; i < movie_list.size(); i ++){
			Movie_Info info = movie_list.get(i);
			String name = info.getMovieName();
			String path = info.getHaiBaoPath();
			sql.add("replace into movieinfo set MOVIE_NAME='"+name+"', HAIBAO_PATH='"+path+"'");
		}
		return sql;
	}
	private static ArrayList<String> creatMovie_NameSQL(ArrayList<Movie_Info> movie_list){
		ArrayList<String> sql = new ArrayList<String>();
		for(int i = 0; i < movie_list.size(); i ++){
			Movie_Info info = movie_list.get(i);
			ArrayList<String> names = info.getNames();
			for(int j = 0; j < names.size(); j ++){
				sql.add("insert into movienames (MOVIE_NAME, MOVIE_OTHER_NAME) values('"+info.getMovieName()+"', '"+names.get(j)+"')");
			}
		}
		return sql;
	}
	private static ArrayList<String> creatMovie_DONWLOADLINKSQL(ArrayList<Movie_Info> movie_list){
		ArrayList<String> sql = new ArrayList<String>();
		for(int i = 0; i < movie_list.size(); i ++){
			Movie_Info info = movie_list.get(i);
			ArrayList<String> downloadlinks = info.getDownLoadLinks();
			for(int j = 0; j < downloadlinks.size(); j ++){
				sql.add("insert into moviedownloadlinks (MOVIE_NAME, DOWN_LOAD_LINK) values('"+info.getMovieName()+"', '"+downloadlinks.get(j)+"')");
			}
		}
		return sql;
	}
	
	public void setDBName(String name){
		db_name = name;
	}
	
	private Connection conn = null;
	private static DBWriter wdb;
	private Runnable task;
	private DBWriter(){}
	public synchronized static DBWriter getInstance(){
		if(wdb == null){
			return new DBWriter();
		}
		return wdb;
	}
	
	private static boolean halt = false;
	private static ArrayList<Movie_Info> movie_list = new ArrayList<Movie_Info>();
	public void halt(){
		halt = true;
	}
	
	private final static int MAX_DBITEM_NUM = 20;
	public void addMovieList(ArrayList<Movie_Info> list){
		synchronized (DBWriter.movie_list) {
			while(DBWriter.movie_list.size() > MAX_DBITEM_NUM){
				try {
					DBWriter.movie_list.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			DBWriter.movie_list.addAll(list);
			if(DBWriter.movie_list.size() > MAX_DBITEM_NUM){
				DBWriter.movie_list.notify();
			}
		}
	}
	
	public void start(){
		task = new Runnable() {
			
			@Override
			public void run() {
				while(true){
					synchronized (movie_list) {
						if(movie_list.size() == 0 && halt){
							LogUtil.getInstance().write("DBWriter halting");
							System.out.println("------------------------------------------------DBWriter halting------------------------------------------------");
							break;
						}
						if(movie_list.size() == 0){
							try {
								movie_list.wait();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						if(movie_list.size() != 0){
							LogUtil.getInstance().write("begin DBWriter");
							System.out.println("------------------------------------------------begin DBWriter------------------------------------------------");
							write(movie_list);
							LogUtil.getInstance().write("end DBWriter");
							System.out.println("------------------------------------------------end DBWriter------------------------------------------------");
							movie_list.clear();
							movie_list.notify();
						}
					}
				}
			}
		};
		Thread thread = new Thread(task);
		thread.start();
	}
		
	private void write(ArrayList<Movie_Info> movie_list){
		Statement stmt = null;
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			conn = DriverManager.getConnection(url + db_name, root, password);
			if(!conn.isClosed()){
				conn.setAutoCommit(false);
			}
			if(!conn.isClosed()){
				convertForMySQL(movie_list);
				stmt = conn.createStatement();
				ArrayList<String> sql = creatMovie_InfoSQL(movie_list);
				if(sql.size() > 0){
					for(int i = 0; i < sql.size(); i ++){
						stmt.addBatch(sql.get(i));
					}
					stmt.executeBatch();
					conn.commit();
				}
				
				stmt = conn.createStatement();
				sql = creatMovie_NameSQL(movie_list);
				if(sql.size() > 0){
					for(int i = 0; i < sql.size(); i ++){
						stmt.addBatch(sql.get(i));
					}
					stmt.executeBatch();
					conn.commit();
				}
				
				stmt = conn.createStatement();
				sql = creatMovie_DONWLOADLINKSQL(movie_list);
				if(sql.size() > 0){
					for(int i = 0; i < sql.size(); i ++){
						stmt.addBatch(sql.get(i));
					}
					stmt.executeBatch();
					conn.commit();
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				conn.rollback();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		try {
			stmt.close();
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
