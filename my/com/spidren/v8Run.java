package com.spidren;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.HTTPSession; 
import fi.iki.elonen.NanoHTTPD.Response;

public class v8Run implements Runnable{ 
    	public String data; 
		public HTTPSession session; 
		public String uri;
		public Map<String, String> header;
		public String mime;
		public File file; 
		public boolean isJson = false;
		
		@Override
    	public void run() {  
			InputStream is = null;
			byte[] by = null; 
			
			try {
				by = data.getBytes("UTF-8");  
				is = new ByteArrayInputStream(by);
			} catch (UnsupportedEncodingException e) { 
				e.printStackTrace();
			}
			
			Response response = new Response(Response.Status.OK, mime, is, by.length );
			response.addHeader("Accept-Ranges","bytes"); 
			response.addHeader("Content-Length", by.length +"");  
			response.addHeader("Cache-Control", "no-store"); 
			 
			
			session.sComplete(response);
			session.sClose(response); 
			
			if(session.tempFileManager != null){
				session.tempFileManager.clear();
			}
    	}
    }