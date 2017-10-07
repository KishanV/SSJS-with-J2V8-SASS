package fi.iki.elonen2;

import java.io.IOException;

public class ssjsWatcher {
		public static void main(String[] args) {
			
			try {
				DebugWebSocketServer ws = new DebugWebSocketServer(9090, false); 
				ws.start(); 
			} catch (IOException e) { 
				e.printStackTrace();
			}
		}
}
