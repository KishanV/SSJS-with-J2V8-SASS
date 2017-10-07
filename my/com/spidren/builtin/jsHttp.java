package com.spidren.builtin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException; 
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException; 

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object; 
import com.spidren.v8Worker;

public class jsHttp implements JavaCallback {
	V8 v8;
 
	public jsHttp(V8 v8) {
		this.v8 = v8; 
	}

	@Override
	public Object invoke(V8Object receiver, V8Array parameters) { 
		V8Object va = new V8Object(v8);
		FileHttp qr = new FileHttp(v8);
		va.registerJavaMethod(qr, "Get", "Get", new Class<?>[] { String.class}, false);
		return va;
	}

	class FileHttp {
		 
	 public String Get(String urlToRead) throws Exception {
	      StringBuilder result = new StringBuilder();
	      URL url = new URL(urlToRead);
	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	      conn.setRequestMethod("GET");
	      BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	      String line;
	      while ((line = rd.readLine()) != null) {
	         result.append(line);
	      }
	      rd.close();
	      return result.toString();
	   }
		 
		public FileHttp( V8 v8) {
			 
		} 
	}
}
