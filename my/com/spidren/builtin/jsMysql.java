package com.spidren.builtin;

import java.sql.Connection;

import com.eclipsesource.v8.JavaCallback; 
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.spidren.config.MysqlConfig;

public class jsMysql  implements JavaCallback {
	V8 v8;
	public static Connection conn = null;
	public jsMysql(V8 v8) {
		new MysqlConfig();
		this.v8 = v8;
	}

	public Object invoke(final V8Object receiver, final V8Array parameters) {
		 String QueryStr = "";
		 if (parameters.length() > 0) {
		      Object arg1 = parameters.get(0);
		      QueryStr = arg1.toString();
		      if (arg1 instanceof Releasable) {
		        ((Releasable) arg1).release();
		      }
		 }

		 V8Object va = new V8Object(v8);
		 jsQuery qr = new jsQuery(QueryStr,conn,v8);
		 va.registerJavaMethod(qr, "setString", "setString",new Class<?>[] {String.class,String.class}, false);
		 va.registerJavaMethod(qr, "getQuery", "getQuery",new Class<?>[] {}, false); 
		 va.registerJavaMethod(qr, "execute", "execute",new Class<?>[] {}, false); 
		 va.registerJavaMethod(qr, "executeWithFields", "executeWithFields",new Class<?>[] {}, false); 
		 va.registerJavaMethod(qr, "update", "update",new Class<?>[] {}, false); 
		 va.registerJavaMethod(qr, "insert", "insert",new Class<?>[] {}, false); 
		 return va;
	 }
}
