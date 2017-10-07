package com.spidren;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eclipsesource.v8.*;
import com.spidren.builtin.*;
import org.apache.commons.lang3.StringEscapeUtils;
import com.spidren.config.MysqlConfig;

import fi.iki.elonen.NanoHTTPD.CookieHandler;
import fi.iki.elonen.NanoHTTPD.HTTPSession;
import fi.iki.elonen.NanoWebSocketServer;

public class v8Worker {
    V8 v8;
    V8Object v8Obj;
    public v8Worker() {
        v8 = V8.createV8Runtime();
        v8Obj = new V8Object(v8);
        v8Obj.add("www", "./www/");
        v8.add("me", v8Obj);
        // v8.executeScript("var data = [];function write(str){data[data.length]=str;}");
        v8.registerJavaMethod(new jsMysql(v8), "mysqlQuery");
        v8.registerJavaMethod(new jsMongoDb(v8), "mongoDb");
        //v8.registerJavaMethod(new jsTest(v8), "jsTest");
        v8.registerJavaMethod(new jsFile(v8), "file");
        v8.registerJavaMethod(new jsHttp(v8), "http");
        v8.registerJavaMethod(this, "print", "print",
                new Class<?>[]{String.class});
        v8.registerJavaMethod(this, "getHost", "getHost", new Class<?>[]{});
        v8.registerJavaMethod(this, "setJson", "setJson", new Class<?>[]{});
        v8.registerJavaMethod(this, "getHeaders", "getHeaders",
                new Class<?>[]{}, false);
        v8.registerJavaMethod(this, "getParms", "getParms", new Class<?>[]{},
                false);
        v8.registerJavaMethod(this, "getfiles", "getfiles", new Class<?>[]{},
                false);
        v8.registerJavaMethod(this, "getUri", "getUri", new Class<?>[]{},
                false);

        v8.registerJavaMethod(this, "getSession", "getSession",
                new Class<?>[]{String.class}, false);
        v8.registerJavaMethod(this, "setSession", "setSession", new Class<?>[]{
                String.class, String.class}, false);
        v8.registerJavaMethod(this, "sendRport", "sendRport", new Class<?>[]{Integer.class,String.class});
        try {
            v8.executeScript(ReadBigStringIn(new BufferedReader(new FileReader(
                    new File("./configer.js")))));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String sendRport(Integer no,String str) {
        ByteArrayOutputStream Bout = new ByteArrayOutputStream();
        DataOutputStream Dout = new DataOutputStream(Bout);
        try {
            Dout.writeInt(no);
            Dout.writeUTF("json");
            Dout.writeUTF(str);
            NanoWebSocketServer.sendToAll(Bout.toByteArray());
        } catch (IOException e) {
            // e.printStackTrace();
        }
        return null;
    }

    static long counter = 0;
    static Map<String, ssjsSession> mapA = new HashMap();
    public ssjsSession ssjssession;

    public Object getHost() {
        String port = ":" + MysqlConfig.port + "";
        if (!port.equals(":80")) {
            return MysqlConfig.host + port;
        } else {
            return "kapdaking.com";
        }
    }


    public String getUri() {
        return session.getUri();
    }

    public Object getSession(String key) {
        String data = ssjssession.data.get(key);
        return data;
    }

    public void setSession(String key, String data) {
        ssjssession.data.put(key, data);
    }

    public void create_session() {
        CookieHandler ok = session.getCookies();
        String ID = ok.read("SSJSID");
        if (ID == null) {
            ID = counter++ + "_" + "";
            ok.set("SSJSID", ID + "; path=/; expires=;", 2);
        }
        ssjssession = mapA.get(ID);
        if (ssjssession == null) {
            mapA.put(ID, new ssjsSession());
        }
    }

    public void onRequest() {
        ssjssession = null;
        create_session();
    }

    HTTPSession session;

    public Object getHeaders() {
        V8Object v8Obj = new V8Object(v8);
        Map<String, String> header = session.getHeaders();
        Iterator<String> e = header.keySet().iterator();
        while (e.hasNext()) {
            String value = e.next();
            v8Obj.add(value, header.get(value));
        }
        return v8Obj;
    }

    public Object getParms() {
        V8Object v8Obj = new V8Object(v8);
        Map<String, String> Parms = session.getParms();
        Iterator<String> e = Parms.keySet().iterator();
        while (e.hasNext()) {
            String value = e.next();
            v8Obj.add(value, Parms.get(value));
        }
        return v8Obj;
    }

    public Object getfiles() {
        V8Object v8Obj = new V8Object(v8);
        Map<String, String> files = session.files;
        Iterator<String> e = files.keySet().iterator();
        while (e.hasNext()) {
            String value = e.next();
            v8Obj.add(value, files.get(value));
        }
        return v8Obj;
    }

    public void print(String str) {
        System.out.println(str);
        sendData(str);
    }

    public boolean isJson = false;

    public void setJson() {
        isJson = true;
    }

    StringBuilder sb;
    public static File jsfile;

    public String jsOutput(v8Run v8run) {
        session = v8run.session;
        onRequest();
        String fnStr = "";
        String args = "";
        if (v8run.session.ssjscall == null) {
            args = "()";
        } else if (v8run.session.ssjscall.equals("fn")) {
            v8run.session.ssjsJson = v8run.session.getParms().get("json");
            v8run.session.getParms().remove("json");
            v8run.mime = "text/html";
            fnStr = ":" + v8run.session.ssjsName;
            args = ""
                    + "."
                    + v8run.session.ssjsName
                    + "("
                    + v8run.session.ssjsJson.substring(1,
                    v8run.session.ssjsJson.length() - 1) + ")";
        } else if (v8run.session.ssjscall.equals("json")) {
            v8run.session.ssjsJson = v8run.session.getParms().get("json");
            v8run.session.getParms().remove("json");
            v8run.mime = "text/html";
            fnStr = ":" + v8run.session.ssjsName;
            v8.add("prmJsonStr", v8run.session.ssjsJson);
            args = "" + "." + v8run.session.ssjsName + "(jsonPrm());";
        }

        File file = v8run.file;
        isJson = false;
        sb = new StringBuilder();
        // page = new StringBuilder();
        jsfile = file;
        FileReader fr = null;
        String js = null;
        String fp = StringEscapeUtils.escapeJava(file.getAbsolutePath() + ":"
                + file.lastModified() + ":" + file.length());
        boolean v8Ex = false;
        Object objStr = null;
        try {
           objStr = v8.executeScript("this['" + fp + fnStr + "'] == undefined");
           v8.executeScript("data = [];this['" + fp + fnStr + "']" + args + ";");
        } catch (V8ScriptException e) {
            if(objStr.toString().equals("true")){
                v8Ex = true;
            }else{
                sendData(e.toString());
                sendError(e);
                System.out.println(e.getMessage());
                System.out.println(e);
            }
        }

        if (v8Ex) {
            try {
                fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                js = ReadBigStringIn(br);
                // String reg = "<!--[\\s\\S]*?-->|<\\?js[\\s\\S]*?\\?>";
                // String reg = "<\\?js[\\s\\S]*?\\?>"; <\?js[\s\S]*?\?>
                String reg = "\\/\\/<\\?js[\\s\\S]*?\\?>|<\\?js[\\s\\S]*?\\?>|<script type=\"ssjs\">[\\s\\S]*?</script>|<script type=\'ssjs\'>[\\s\\S]*?</script>|<script>//ssjs[\\s\\S]*?</script>";
                String[] Data = js.split(reg);
                StringBuilder jsb = new StringBuilder();
                StringBuilder exjs = new StringBuilder();
                Vector<String> exFN = new Vector<String>();
                Vector<String> exName = new Vector<String>();
                Vector<String> exVAR = new Vector<String>();
                Vector<Boolean> exBool = new Vector<Boolean>();
                int exFnCount = 0;
                int i = 0;

                Pattern part = Pattern.compile("^\\:[A-z,]*\\([A-z,]*\\)\\;");
                Pattern pattern = Pattern.compile(reg);
                Matcher matcher = pattern.matcher(js);

                jsb.append("this['" + fp + "'] = function(){");
                for (i = 0; i < Data.length; i++) {

                    System.out.println(StringEscapeUtils.escapeJava(Data[i]));
                    jsb.append("   write(\""
                            + StringEscapeUtils.escapeJava(Data[i]) + "\");");

                    if (matcher.find()) {
                        String jsTag = matcher.group(0);
                        if (jsTag.startsWith("<script type=\"ssjs\">")
                                || jsTag.startsWith("<script type='ssjs'>")) {
                            jsTag = jsTag.substring(20, jsTag.length() - 9);
                        } else if (jsTag.startsWith("<script>//ssjs")) {
                            jsTag = jsTag.substring("<script>//ssjs".length(),
                                    jsTag.length() - 9);

                        } else if (jsTag.startsWith("//<?js")) {
                            jsTag = jsTag.substring("//<?js".length(),
                                    jsTag.length() - 2);
                        } else {
                            jsTag = jsTag.substring(4, jsTag.length() - 2);
                        }

                        Matcher mh = part.matcher(jsTag);
                        int j = 0;
                        if (mh.find()) {
                            String fn = mh.group(0);
                            j = fn.length();
                            // System.out.println("\n"+mh.group(0) +
                            // " \n"+jsTag);
                            String body = jsTag.substring(1, j - 1);
                            String name = body.substring(0, body.indexOf("("));
                            body = body.substring(body.indexOf("("),
                                    body.length());
                            // exjs.append("this['"+fp +":"+ name +"']" +
                            // " = function" + body + " { " + jsTag.substring(j)
                            // + "}\n");
                            exFN.add("\n this." + name + "" + " = function"
                                    + body + " { " + jsTag.substring(j)
                                    + "\n} ");
                            exName.add(name);
                            exVAR.add("");
                            exBool.add(true);
                            exFnCount++;
                            jsb.append(getLinesN(jsTag));
                        } else {
                            exName.add("");
                            exFN.add("");
                            exBool.add(false);
                            exVAR.add(jsTag);
                            jsb.append(jsTag);
                        }

                    }
                }

                int exStr = 0;
                StringBuilder allStr = new StringBuilder();
                for (int a = 0; a < exFnCount; a++) {
                    StringBuilder jsStr = new StringBuilder();
                    String name = "";
                    boolean done = false;
                    for (int b = 0; b < exBool.size(); b++) {
                        if (exBool.get(b).equals(true) & done == false) {
                            exBool.set(b, false);
                            name = exName.get(b);
                            done = true;
                            jsStr.append(exFN.get(b));
                        } else {
                            jsStr.append(exVAR.get(b));
                        }
                    }
                    // jsStr.append("print(this."+name+"+'');");
                    jsStr.append(" \n } return new js(); }());\n\n");
                    allStr.append("this['" + fp + ":" + name + "']"
                            + " =  (function() { function js() {"
                            + jsStr.toString());
                }
                // System.out.println(allStr.toString());

                jsb.append(" \n return 0 };");
                js = jsb.toString() + "\n" + allStr.toString();
                //System.out.println(jsb.toString());
                // System.out.println(StringEscapeUtils.unescapeJava(js));
            } catch (FileNotFoundException e) {
                sendData(e.toString());
                System.out.println(e.toString());
                js = "";
            }

            try {
                v8.executeScript(js, file.getAbsolutePath(), 0);
                v8.executeStringScript("js.getAndReset()");
                v8.executeScript("this['" + fp + fnStr + "']" + args + ";");
            } catch (V8ScriptException e) {
                sendData(e.toString());
                sendError(e);
                System.out.println(e.toString());
            }
        }

        String data = v8.executeStringScript("js.getAndReset()");
        // System.out.println(data);
        return data;

    }

    public static String ReadBigStringIn(BufferedReader buffIn) {
        StringBuilder everything = new StringBuilder();
        String line;
        try {
            while ((line = buffIn.readLine()) != null) {
                everything.append(line + '\n');
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
        return everything.toString();
    }

    public String socketOutput(v8Socket v8socket) {
        // onRequest();
        isJson = false;
        sb = new StringBuilder();
        // page = new StringBuilder();
        File file = v8socket.file;
        jsfile = file;
        FileReader fr = null;
        String js = null;

        String fnStr = "";
        String args = "";
        if (v8socket.type.equals("fn")) {
            fnStr = ":" + v8socket.name;
            args = "." + v8socket.name + "("
                    + v8socket.json.substring(1, v8socket.json.length() - 1)
                    + ")";
        } else if (v8socket.type.equals("json")) {
            fnStr = ":" + v8socket.name;
            v8.add("prmJsonStr", v8socket.json);
            args = "." + v8socket.name + "(jsonPrm())";
        } else {
            args = "()";
        }

        String fp = StringEscapeUtils.escapeJava(file.getAbsolutePath() + ":"
                + file.lastModified() + ":" + file.length());
        boolean v8Ex = false;
        try {
            // System.out.println(v8Obj.executeIntegerFunction(fp, null));
            // v8Obj.executeIntegerFunction(fp, null);
            v8.executeScript("data = [];this['" + fp + fnStr + "']" + args
                    + ";");
        } catch (Exception e) {
            v8Ex = true;
            // System.out.println(e);
        }

		/*
         * String fp =
		 * StringEscapeUtils.escapeJava(file.getAbsolutePath()+":"+file
		 * .lastModified()+":"+file.length()); boolean v8Ex = false; try {
		 * System.out.println("this['"+fp+fnStr+"']"+args+";");
		 * v8.executeScript("data = [];this['"+fp+fnStr+"']"+args+";"); } catch
		 * (Exception e) { v8Ex = true; }
		 */

        if (v8Ex) {
            try {
                fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                js = ReadBigStringIn(br);
                // String reg = "<!--[\\s\\S]*?-->|<\\?js[\\s\\S]*?\\?>";
                // String reg = "<\\?js[\\s\\S]*?\\?>"; <\?js[\s\S]*?\?>
                String reg = "<\\?js[\\s\\S]*?\\?>|<script type=\"ssjs\">[\\s\\S]*?</script>|<script type=\'ssjs\'>[\\s\\S]*?</script>|<script>//ssjs[\\s\\S]*?</script>";
                String[] Data = js.split(reg);
                StringBuilder jsb = new StringBuilder();
                StringBuilder exjs = new StringBuilder();
                Vector<String> exFN = new Vector<String>();
                Vector<String> exName = new Vector<String>();
                Vector<String> exVAR = new Vector<String>();
                Vector<Boolean> exBool = new Vector<Boolean>();
                int exFnCount = 0;
                int i = 0;

                Pattern part = Pattern.compile("^\\:[A-z,]*\\([A-z,]*\\)\\;");
                Pattern pattern = Pattern.compile(reg);
                Matcher matcher = pattern.matcher(js);

                jsb.append("this['" + fp + "'] = function(){");
                for (i = 0; i < Data.length; i++) {
                    jsb.append("\n write(\""
                            + StringEscapeUtils.escapeJava(Data[i]) + "\");");
                    if (matcher.find()) {
                        String jsTag = matcher.group(0);
                        if (jsTag.startsWith("<script type=\"ssjs\">")
                                || jsTag.startsWith("<script type='ssjs'>")) {
                            jsTag = jsTag.substring(20, jsTag.length() - 9);
                        } else if (jsTag.startsWith("<script>//ssjs")) {
                            jsTag = jsTag.substring("<script>//ssjs".length(),
                                    jsTag.length() - 9);
                        } else {
                            jsTag = jsTag.substring(4, jsTag.length() - 2);
                        }

                        Matcher mh = part.matcher(jsTag);
                        int j = 0;
                        if (mh.find()) {
                            String fn = mh.group(0);
                            j = fn.length();
                            // System.out.println("\n"+mh.group(0) +
                            // " \n"+jsTag);
                            String body = jsTag.substring(1, j - 1);
                            String name = body.substring(0, body.indexOf("("));
                            body = body.substring(body.indexOf("("),
                                    body.length());
                            // exjs.append("this['"+fp +":"+ name +"']" +
                            // " = function" + body + " { " + jsTag.substring(j)
                            // + "}\n");
                            exFN.add("this." + name + "" + " = function" + body
                                    + " { " + jsTag.substring(j) + "}\n");
                            exName.add(name);
                            exVAR.add("");
                            exBool.add(true);
                            exFnCount++;
                        } else {
                            exName.add("");
                            exFN.add("");
                            exBool.add(false);
                            exVAR.add(jsTag);
                            jsb.append(jsTag);
                        }

                    }
                }

                int exStr = 0;
                StringBuilder allStr = new StringBuilder();
                for (int a = 0; a < exFnCount; a++) {
                    StringBuilder jsStr = new StringBuilder();
                    String name = "";
                    boolean done = false;
                    for (int b = 0; b < exBool.size(); b++) {
                        if (exBool.get(b).equals(true) & done == false) {
                            exBool.set(b, false);
                            name = exName.get(b);
                            done = true;
                            jsStr.append(exFN.get(b));
                        } else {
                            jsStr.append(exVAR.get(b));
                        }
                    }
                    // jsStr.append("print(this."+name+"+'');");
                    jsStr.append(" \n } return new js(); }());\n\n");
                    allStr.append("this['" + fp + ":" + name + "']"
                            + " =  (function() { function js() {"
                            + jsStr.toString());
                }
                // System.out.println(allStr.toString());

                jsb.append(" \n return 0 };");
                js = jsb.toString() + "\n" + allStr.toString();
                System.out.println(js);
                // System.out.println(StringEscapeUtils.unescapeJava(js));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                js = "";
            }

            try {
                // v8Obj.add(fp, "v8Obj");
                v8.executeScript(js, file.getAbsolutePath(), 2);
                v8.executeScript("this['" + fp + fnStr + "']" + args + ";");
                // v8Obj.executeIntegerFunction(fp, null);
                // System.out.println(js);
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        // String data = sb.toString();
        String data = v8.executeStringScript("js.getAndReset()");
        // System.out.println(data);
        return data;
    }

    private void sendData(String data) {
        //System.out.println("sendData : ");
        //System.out.println(data);
        ByteArrayOutputStream Bout = new ByteArrayOutputStream();
        DataOutputStream Dout = new DataOutputStream(Bout);
        try {
            Dout.writeInt(100);
            Dout.writeUTF("string");
            Dout.writeUTF(data);
            NanoWebSocketServer.sendToAll(Bout.toByteArray());
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    private void sendError(V8ScriptException data) {
        //System.out.println("sendData : ");
        //System.out.println(data);
        ByteArrayOutputStream Bout = new ByteArrayOutputStream();
        DataOutputStream Dout = new DataOutputStream(Bout);
        String str = "{" +
                "\"msg\":\""+StringEscapeUtils.escapeJava(data.getJSMessage())+"\","+
                "\"url\":\""+StringEscapeUtils.escapeJava(data.getFileName().replaceAll("\\\\", "/"))+"\","+
                "\"linenumber\":\""+StringEscapeUtils.escapeJava(data.getLineNumber()+"")+"\","+
                "\"linechar\":\""+StringEscapeUtils.escapeJava(data.getStartColumn()+"")+"\","+
                "\"error\":\""+StringEscapeUtils.escapeJava(data.getJSStackTrace())+"\""+
                "}";

        System.out.println(str);
        try {
            Dout.writeInt(98);
            Dout.writeUTF("json");
            Dout.writeUTF(str);
            NanoWebSocketServer.sendToAll(Bout.toByteArray());
        } catch (IOException e) {
            // e.printStackTrace();
        }
    }

    private static String getLinesN(String str) {
        String[] lines = str.split("\r\n|\r|\n");
        StringBuilder sb = new StringBuilder();
        sb.append(" write(\"");
        for (int i = 0; i < lines.length - 1; i++) {
            sb.append("\\n");
        }
        sb.append("\");");
        return sb.toString();
    }
}
