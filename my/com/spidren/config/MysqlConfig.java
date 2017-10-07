package com.spidren.config;

import java.io.File; 
public class MysqlConfig {
	public static String host = "";
	public static String mysql_user = "root";
	public static String mysql_pass = "";
	public static String CD = "";
	public static String DB  = "app";
	public static int port = 2004;
	 
	public MysqlConfig(){
		if("Windows 10".equals(System.getProperty("os.name"))){
			host = "192.168.0.103";
			mysql_user = "root";
			mysql_pass = "";
			port = 8080;
		} else {
			mysql_user = "root";
			mysql_pass = "123";
			port = 8080
			;
		}
	}
}