package com.spidren;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import fi.iki.elonen.NanoWebSocketServer.WebSocket;

public class v8Socket implements Runnable{

    	public File file;
    	public String data;
		public String json;
		public String type;
		public String path;
		public WebSocket socket;
		public int sentCount;
		public String name;
		public boolean isJson = false;
		 
		
		@Override
		public void run() { 
			sendData(sentCount,isJson,data);
			 
		} 
		
		private void sendData(int sentCount,boolean isJson,String data) {
			ByteArrayOutputStream Bout = new ByteArrayOutputStream();
			DataOutputStream Dout = new DataOutputStream(Bout);
			try {
				Dout.writeInt(sentCount);
				Dout.writeUTF((isJson ? "json" : "string"));
				Dout.writeUTF(data);
				socket.send(Bout.toByteArray()); 
			} catch (IOException e) { 
				e.printStackTrace();
			}
		}
    	
    }