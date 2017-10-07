package fi.iki.elonen;

/*
 * #%L
 * NanoHttpd-Webserver
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTzIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import com.spidren.config.MysqlConfig;
import org.w3c.css.sac.CSSException;

import com.spidren.v8Run;
import com.spidren.v8Socket;
import com.spidren.v8Worker;
import com.vaadin.sass.internal.ScssStylesheet;

import fi.iki.elonen.NanoHTTPD.Response.IStatus;
import fi.iki.elonen.NanoWebSocketServer.WebSocketFrame.CloseCode;
import fi.iki.elonen2.DebugWebSocketServer;

public class ssjs extends NanoWebSocketServer {


    /**
     * Common mime type for dynamic content: binary
     */
    public static final String MIME_DEFAULT_BINARY = "application/octet-stream";

    /**
     * Default Index file names.
     */
    @SuppressWarnings("serial")
    public static final List<String> INDEX_FILE_NAMES = new ArrayList<String>() {

        {
            add("index.html");
            add("index.htm");
        }
    };


    /**
     * Hashtable mapping (String)FILENAME_EXTENSION -> (String)MIME_TYPE
     */
    @SuppressWarnings("serial")
    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>() {

        {
            put("css", "text/css");
            put("htm", "text/html");
            put("html", "text/html");
            put("xml", "text/xml");
            put("java", "text/x-java-source, text/java");
            put("md", "text/plain");
            put("txt", "text/plain");
            put("asc", "text/plain");
            put("gif", "image/gif");
            put("jpg", "image/jpeg");
            put("jpeg", "image/jpeg");
            put("png", "image/png");
            put("svg", "image/svg+xml");
            put("mp3", "audio/mpeg");
            put("m3u", "audio/mpeg-url");
            put("mp4", "video/mp4");
            put("ogv", "video/ogg");
            put("flv", "video/x-flv");
            put("mov", "video/quicktime");
            put("swf", "application/x-shockwave-flash");
            put("js", "application/javascript");
            put("pdf", "application/pdf");
            put("doc", "application/msword");
            put("ogg", "application/x-ogg");
            put("zip", "application/octet-stream");
            put("exe", "application/octet-stream");
            put("class", "application/octet-stream");
            put("m3u8", "application/vnd.apple.mpegurl");
            put("ts", " video/mp2t");
            put("ssjs", "ssjs");
            put("scss", "scss");
            put("sql", "sql");
            put("view", "text/plain");
        }
    };

    /**
     * The distribution licence
     */
    private static final String LICENCE = "Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen, 2010 by Konstantinos Togias\n" + "\n"
            + "Redistribution and use in source and binary forms, with or without\n" + "modification, are permitted provided that the following conditions\n" + "are met:\n"
            + "\n" + "Redistributions of source code must retain the above copyright notice,\n" + "this list of conditions and the following disclaimer. Redistributions in\n"
            + "binary form must reproduce the above copyright notice, this list of\n" + "conditions and the following disclaimer in the documentation and/or other\n"
            + "materials provided with the distribution. The name of the author may not\n" + "be used to endorse or promote products derived from this software without\n"
            + "specific prior written permission. \n" + " \n" + "THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR\n"
            + "IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES\n" + "OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.\n"
            + "IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,\n" + "INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT\n"
            + "NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,\n" + "DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY\n"
            + "THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n" + "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n"
            + "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";

    private static Map<String, WebServerPlugin> mimeTypeHandlers = new HashMap<String, WebServerPlugin>();

    public static String[] getLocalIP() {

        InetAddress[] localaddr;
        ArrayList<String> list = new ArrayList<String>();
        list.add("127.0.0.1");

        try {
            localaddr = Inet4Address.getAllByName(InetAddress.getLocalHost().getHostName());
            for (int i = 0; i < localaddr.length; i++) {
                // System.out.println( "---"+localaddr[i].getHostAddress());
                list.add(localaddr[i].getHostAddress());
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < list.size(); i++) {
            //System.out.println(list.get(i));
        }

        Enumeration<NetworkInterface> nets = null;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        for (NetworkInterface netint : Collections.list(nets)) {
            try {
                displayInterfaceInformation(list, netint);
            } catch (SocketException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return list.toArray(new String[0]);
    }

    static void displayInterfaceInformation(ArrayList<String> list, NetworkInterface netint) throws SocketException {
        // System.out.printf("Display name: %s\n", netint.getDisplayName());
        // System.out.printf("Name: %s\n", netint.getName());
        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            //System.out.println(inetAddress.toString().substring(1));
            list.add(inetAddress.toString().substring(1));
        }
        //  System.out.printf("\n");
    }

    /**
     * Starts as a standalone file server and waits for Enter.
     */
    public static void main(String[] args) {
        // Defaults

        try {
            DebugWebSocketServer ws = new DebugWebSocketServer(9090, false);
            ws.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        new MysqlConfig();
        int port = MysqlConfig.port;
        // bind to all interfaces by default
        List<File> rootDirs = new ArrayList<File>();
        boolean quiet = false;
        rootDirs.add(new File("./www").getAbsoluteFile());
        v8WOrker();
        String[] host = getLocalIP();
        for (int i = 0; i < host.length; i++) {
            //System.out.println(host[i]);
            ServerRunner.executeInstance(new ssjs(host[i], port, rootDirs, quiet), host[i] + "");
        }
        System.out.println("Server runing, \n" + port);
    }

    private static class MyWatchQueueReader implements Runnable {

        /**
         * the watchService that is passed in from above
         */
        private WatchService myWatcher;

        public MyWatchQueueReader(WatchService myWatcher) {
            this.myWatcher = myWatcher;
        }

        /**
         * In order to implement a file watcher, we loop forever
         * ensuring requesting to take the next item from the file
         * watchers queue.
         */
        @Override
        public void run() {
            try {
                // get the first event before looping
                WatchKey key = myWatcher.take();
                while (key != null) {
                    // we have a polled event, now we traverse it and 
                    // receive all the states from it
                    for (WatchEvent event : key.pollEvents()) {
                        System.out.printf("Received %s event for file: %s\n",
                                event.kind(), event.context());
                    }
                    key.reset();
                    key = myWatcher.take();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Stopping thread");
        }
    }


    protected static void registerPluginForMimeType(String[] indexFiles, String mimeType, WebServerPlugin plugin, Map<String, String> commandLineOptions) {
        if (mimeType == null || plugin == null) {
            return;
        }

        if (indexFiles != null) {
            for (String filename : indexFiles) {
                int dot = filename.lastIndexOf('.');
                if (dot >= 0) {
                    String extension = filename.substring(dot + 1).toLowerCase();
                    ssjs.MIME_TYPES.put(extension, mimeType);
                }
            }
            ssjs.INDEX_FILE_NAMES.addAll(Arrays.asList(indexFiles));
        }
        ssjs.mimeTypeHandlers.put(mimeType, plugin);
        plugin.initialize(commandLineOptions);
    }

    private final boolean quiet;

    protected List<File> rootDirs;


    public ssjs(String host, int port, List<File> wwwroots, boolean quiet) {
        super(host, port);
        this.quiet = quiet;
        this.rootDirs = new ArrayList<File>(wwwroots);
        init();
    }

    private boolean canServeUri(String uri, File homeDir) {
        boolean canServeUri;
        File f = new File(homeDir, uri);
        canServeUri = f.exists();
        /*if (!canServeUri) {
            String mimeTypeForFile = getMimeTypeForFile(uri);
            WebServerPlugin plugin = SimpleWebServer.mimeTypeHandlers.get(mimeTypeForFile);
            if (plugin != null) {
                canServeUri = plugin.canServeUri(uri, homeDir);
            }
        }*/
        return canServeUri;
    }

    /**
     * URL-encodes everything between "/"-characters. Encodes spaces as '%20'
     * instead of '+'.
     */
    private String encodeUri(String uri) {
        String newUri = "";
        StringTokenizer st = new StringTokenizer(uri, "/ ", true);
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("/")) {
                newUri += "/";
            } else if (tok.equals(" ")) {
                newUri += "%20";
            } else {
                try {
                    newUri += URLEncoder.encode(tok, "UTF-8");
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        }
        return newUri;
    }

    private String findIndexFileInDirectory(File directory) {
        for (String fileName : ssjs.INDEX_FILE_NAMES) {
            File indexFile = new File(directory, fileName);
            if (indexFile.isFile()) {
                return fileName;
            }
        }
        return null;
    }

    protected Response getForbiddenResponse(String s) {
        return newFixedLengthResponse(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: " + s);
    }

    protected Response getInternalErrorResponse(String s) {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERROR: " + s);
    }

    // Get MIME type from file name extension, if possible
    private String getMimeTypeForFile(String uri) {
        int dot = uri.lastIndexOf('.');
        String mime = null;
        if (dot >= 0) {
            mime = ssjs.MIME_TYPES.get(uri.substring(dot + 1).toLowerCase());
        }
        return mime == null ? ssjs.MIME_DEFAULT_BINARY : mime;
    }

    // Get MIME type from file name extension, if possible
    private String getExtantion(String uri) {
        int dot = uri.lastIndexOf('.');
        return uri.substring(dot + 1).toLowerCase() + "";
    }

    protected Response getNotFoundResponse() {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
    }

    /**
     * Used to initialize and customize the server.
     */
    public void init() {

    }

    protected String listDirectory(String uri, File f) {
        String heading = "Directory " + uri;
        StringBuilder msg =
                new StringBuilder("<html><head><title>" + heading + "</title><style><!--\n" + "span.dirname { font-weight: bold; }\n" + "span.filesize { font-size: 75%; }\n"
                        + "// -->\n" + "</style>" + "</head><body><h1>" + heading + "</h1>");

        String up = null;
        if (uri.length() > 1) {
            String u = uri.substring(0, uri.length() - 1);
            int slash = u.lastIndexOf('/');
            if (slash >= 0 && slash < u.length()) {
                up = uri.substring(0, slash + 1);
            }
        }

        List<String> files = Arrays.asList(f.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        }));

        Collections.sort(files);
        List<String> directories = Arrays.asList(f.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        }));

        Collections.sort(directories);
        if (up != null || directories.size() + files.size() > 0) {
            msg.append("<ul>");
            if (up != null || directories.size() > 0) {
                msg.append("<section class=\"directories\">");
                if (up != null) {
                    msg.append("<li><a rel=\"directory\" href=\"").append(up).append("\"><span class=\"dirname\">..</span></a></b></li>");
                }
                for (String directory : directories) {
                    String dir = directory + "/";
                    msg.append("<li><a rel=\"directory\" href=\"").append(encodeUri(uri + dir)).append("\"><span class=\"dirname\">").append(dir)
                            .append("</span></a></b></li>");
                }
                msg.append("</section>");
            }
            if (files.size() > 0) {
                msg.append("<section class=\"files\">");
                for (String file : files) {
                    msg.append("<li><a href=\"").append(encodeUri(uri + (file.endsWith(".ssjs") ? file.substring(0, file.length() - 5) : file))).append("\"><span class=\"filename\">").append((file.endsWith(".ssjs") ? file.substring(0, file.length() - 5) : file)).append("</span></a>");
                    File curFile = new File(f, file);
                    long len = curFile.length();
                    msg.append("&nbsp;<span class=\"filesize\">(");
                    if (len < 1024) {
                        msg.append(len).append(" bytes");
                    } else if (len < 1024 * 1024) {
                        msg.append(len / 1024).append(".").append(len % 1024 / 10 % 100).append(" KB");
                    } else {
                        msg.append(len / (1024 * 1024)).append(".").append(len % (1024 * 1024) / 10000 % 100).append(" MB");
                    }
                    msg.append(")</span></li>");
                }
                msg.append("</section>");
            }
            msg.append("</ul>");
        }
        msg.append("</body></html>");
        return msg.toString();
    }

    @Override
    public Response newFixedLengthResponse(IStatus status, String mimeType, String message) {
        Response response = super.newFixedLengthResponse(status, mimeType, message);
        response.addHeader("Accept-Ranges", "bytes");
        return response;
    }

    private Response respond(Map<String, String> headers, IHTTPSession session, String uri) {
        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0) {
            uri = uri.substring(0, uri.indexOf('?'));
        }

        // Prohibit getting out of current directory
        if (uri.contains("../")) {
            return getForbiddenResponse("Won't serve ../ for security reasons.");
        } else if (uri.equals("/")) {
            if (MysqlConfig.port == 80) {
                uri = "/index.html";
            }
        }

        boolean canServeUri = false;
        boolean canCss = false;
        boolean canJs = false;
        File homeDir = null;

        for (int i = 0; !canServeUri && i < this.rootDirs.size(); i++) {
            homeDir = this.rootDirs.get(i);
            canServeUri = canServeUri(uri, homeDir);
            canJs = canServeUri(uri + ".ssjs", homeDir);
            canCss = canServeUri(uri.substring(0, (uri.length() < 4 ? 0 : uri.length() - 4)) + ".scss", homeDir);
        }

        if (!canServeUri && !canJs && !canCss) {
            return getNotFoundResponse();
        }

        // Browsers get confused without '/' after the directory, send a
        // redirect.
        File f;
        boolean nowCssFile = false;
        String mimeTypeForFile = getMimeTypeForFile(uri);
        if (mimeTypeForFile.equalsIgnoreCase("ssjs") || mimeTypeForFile.equalsIgnoreCase("scss") || mimeTypeForFile.equalsIgnoreCase("sql")) {
            return getNotFoundResponse();
        } else if (canJs) {
            f = new File(homeDir, uri + ".ssjs");
        } else if (canCss) {
            f = new File(homeDir, uri);
            if (!f.exists()) {
                try {
                    nowCssFile = true;
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {
            f = new File(homeDir, uri);
        }


        if (f.isDirectory() && !uri.endsWith("/")) {
            uri += "/";
            Response res = newFixedLengthResponse(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
            res.addHeader("Location", uri);
            return res;
        }

        if (f.isDirectory()) {
            // First look for index files (index.html, index.htm, etc) and if
            // none found, list the directory if readable.
            String indexFile = findIndexFileInDirectory(f);
            if (indexFile == null) {
                if (f.canRead()) {
                    // No index file, list the directory if it is readable
                    return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, listDirectory(uri, f));
                } else {
                    return getForbiddenResponse("No directory listing.");
                }
            } else {
                mimeTypeForFile = ssjs.MIME_TYPES.get("html");
                f = new File(homeDir, uri + indexFile);
                // return respond(headers, session, uri + indexFile);
            }
        }


        WebServerPlugin plugin = ssjs.mimeTypeHandlers.get(mimeTypeForFile);
        Response response = null;
        if (plugin != null && plugin.canServeUri(uri, homeDir)) {
            response = plugin.serveFile(uri, headers, session, f, mimeTypeForFile);
            if (response != null && response instanceof InternalRewrite) {
                InternalRewrite rewrite = (InternalRewrite) response;
                return respond(rewrite.getHeaders(), session, rewrite.getUri());
            }
        } else {

        	/*System.out.println(uri);
            System.out.println("File:"+StringUtils.countMatches(f.getName(), "."));
        	System.out.println(getExtantion(f.getName()));
        	*/

            if (getExtantion(f.getName()).equalsIgnoreCase("css")) {
                String fname = f.getName();
                fname = fname.substring(0, fname.length() - 4);
                File scss = new File(f.getParentFile(), fname + ".scss");
                // System.out.println(scss.lastModified());
                // System.out.println(f.lastModified());
                if (scss.lastModified() > f.lastModified() || nowCssFile) {

                    ScssStylesheet stylesheet;
                    try {
                        stylesheet = ScssStylesheet.get(scss.getAbsolutePath());
                        stylesheet.compile();
                        //System.out.println(stylesheet.printState());
                        FileWriter fw = new FileWriter(f);
                        fw.append(stylesheet.printState());
                        fw.flush();
                        fw.close();
                    } catch (CSSException | IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }

            if (canJs) {
                runJs(uri, headers, f, mimeTypeForFile, session);
                return null;
            } else {
                response = serveFile(uri, headers, f, mimeTypeForFile);
            }

        }
        return response != null ? response : getNotFoundResponse();
    }


    final static BlockingQueue<Runnable> blockingQueue = new LinkedBlockingDeque<>();

    static void v8WOrker() {

        for (int i = 0; i < 16; i++) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    v8Worker v8worker = new v8Worker();
                    while (true) {
                        try {
                            Runnable jsRunnable;
                            jsRunnable = blockingQueue.take();

                            if (jsRunnable.getClass() == v8Run.class) {
                                v8Run v8run = (v8Run) jsRunnable;
                                v8run.data = v8worker.jsOutput(v8run);
                                v8run.isJson = v8worker.isJson;
                                new Thread(v8run).start();
                            } else if (jsRunnable.getClass() == v8Socket.class) {
                                v8Socket v8socket = (v8Socket) jsRunnable;
                                v8socket.data = v8worker.socketOutput(v8socket);
                                v8socket.isJson = v8worker.isJson;
                                new Thread(v8socket).start();
                            }
                        } catch (InterruptedException ex) {
                            System.out.println(ex);
                            return;
                        }
                    }
                }
            };
            t.setName("V8Worker");
            t.start();
        }

    }


    void runJs(String uri, Map<String, String> header, File file, String mime, IHTTPSession session) {
        session.extended(true);
        v8Run v8run = new v8Run();
        v8run.session = (HTTPSession) session;
        v8run.uri = uri;
        v8run.header = header;
        v8run.mime = mime;
        v8run.file = file;
        synchronized (blockingQueue) {
            blockingQueue.add(v8run);
        }
    }


    void runSocket(File file, String name, String json, String type, String path, WebSocket socket, int sentCount) {

        v8Socket v8run = new v8Socket();
        v8run.file = file;
        v8run.name = name;
        v8run.json = json;
        v8run.type = type;
        v8run.path = path;
        v8run.socket = socket;
        v8run.sentCount = sentCount;
        synchronized (blockingQueue) {
            blockingQueue.add(v8run);
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response res = super.serve(session);
        if (res != null) {
            return res;
        }

        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        Map<String, String> files = new HashMap<String, String>();
        ((HTTPSession) session).files = files;

        try {
            session.parseBody(files);
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (ResponseException e1) {
            e1.printStackTrace();
        }

        String uri = session.getUri();
        for (File homeDir : this.rootDirs) {
            // Make sure we won't die of an exception later
            if (!homeDir.isDirectory()) {
                return getInternalErrorResponse("given path is not a directory (" + homeDir + ").");
            }
        }
        return respond(Collections.unmodifiableMap(header), session, uri);
    }

    /**
     * Serves file from homeDir and its' subdirectories (only). Uses only URI,
     * ignores all headers and HTTP parameters.
     */
    Response serveFile(String uri, Map<String, String> header, File file, String mime) {
        Response res;

        try {
            // Calculate etag
            String etag = Integer.toHexString((file.getAbsolutePath() + file.lastModified() + "" + file.length()).hashCode());

            // Support (simple) skipping:
            long startFrom = 0;
            long endAt = -1;
            String range = header.get("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    try {
                        if (minus > 0) {
                            startFrom = Long.parseLong(range.substring(0, minus));
                            endAt = Long.parseLong(range.substring(minus + 1));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // get if-range header. If present, it must match etag or else we
            // should ignore the range request
            String ifRange = header.get("if-range");
            boolean headerIfRangeMissingOrMatching = (ifRange == null || etag.equals(ifRange));

            String ifNoneMatch = header.get("if-none-match");
            boolean headerIfNoneMatchPresentAndMatching = ifNoneMatch != null && (ifNoneMatch.equals("*") || ifNoneMatch.equals(etag));

            // Change return code and add Content-Range header when skipping is
            // requested
            long fileLen = file.length();

            if (headerIfRangeMissingOrMatching && range != null && startFrom >= 0 && startFrom < fileLen) {
                // range request that matches current etag
                // and the startFrom of the range is satisfiable
                if (headerIfNoneMatchPresentAndMatching) {
                    // range request that matches current etag
                    // and the startFrom of the range is satisfiable
                    // would return range from file
                    // respond with not-modified
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                } else {
                    if (endAt < 0) {
                        endAt = fileLen - 1;
                    }
                    long newLen = endAt - startFrom + 1;
                    if (newLen < 0) {
                        newLen = 0;
                    }

                    FileInputStream fis = new FileInputStream(file);
                    fis.skip(startFrom);

                    res = newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mime, fis, newLen);
                    res.addHeader("Accept-Ranges", "bytes");
                    res.addHeader("Content-Length", "" + newLen);
                    res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
                    res.addHeader("ETag", etag);
                }
            } else {

                /*if (headerIfRangeMissingOrMatching && range != null && startFrom >= fileLen) {
                    // return the size of the file
                    // 4xx responses are not trumped by if-none-match
                    res = newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
                    res.addHeader("Content-Range", "bytes " + fileLen);
                    res.addHeader("ETag", etag);
                } else if (range == null && headerIfNoneMatchPresentAndMatching) {
                    // full-file-fetch request
                    // would return entire file
                    // respond with not-modified
                	//System.out.print("would return entire file");
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag);
                    Date expdate = new Date ();
					expdate.setTime (expdate.getTime() + (3600 * 1000));
					DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
					df.setTimeZone(TimeZone.getTimeZone("GMT"));
					res.addHeader("Expires",df.format(expdate));
                } else if (!headerIfRangeMissingOrMatching && headerIfNoneMatchPresentAndMatching) {
                    // range request that doesn't match current etag
                    // would return entire (different) file
                    // respond with not-modified 
                	//System.out.print("would return entire (different) file");
                    res = newFixedLengthResponse(Response.Status.NOT_MODIFIED, mime, "");
                    res.addHeader("ETag", etag); 
                    /*Date expdate = new Date ();
					expdate.setTime (expdate.getTime() + (3600 * 1000));
					DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
					df.setTimeZone(TimeZone.getTimeZone("GMT"));
					res.addHeader("Expires",df.format(expdate));*/
               /* } else {*/
                // supply the file
                //System.out.print("supply the file");
                res = newFixedFileResponse(file, mime);
                res.addHeader("Content-Length", "" + fileLen);
                res.addHeader("ETag", etag);
                Date expdate = new Date();
                expdate.setTime(expdate.getTime() + (3600 * 1000));
                DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss zzz");
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                res.addHeader("Expires", df.format(expdate));
                /*}*/
            }
        } catch (IOException ioe) {
            res = getForbiddenResponse("Reading file failed.");
        }

        return res;
    }

    private Response newFixedFileResponse(File file, String mime) throws FileNotFoundException {
        Response res;
        res = newFixedLengthResponse(Response.Status.OK, mime, new FileInputStream(file), (int) file.length());
        res.addHeader("Accept-Ranges", "bytes");
        return res;
    }

    @Override
    protected void onClose(WebSocket webSocket, CloseCode code, String reason, boolean initiatedByRemote) {
        System.out.println("onClose");
    }

    @Override
    protected void onException(WebSocket webSocket, IOException e) {
        System.out.println("onException");
    }


    @Override
    protected void onMessage(WebSocket webSocket, WebSocketFrame messageFrame) {
        //System.out.println("onMessage");

        byte[] data = messageFrame.getBinaryPayload();
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        DataInputStream Din = new DataInputStream(in);
        int SentCount = 0;

        try {
             SentCount = Din.readInt();
             if(SentCount <= 100){
                 String msg = Din.readUTF();
                 ByteArrayOutputStream Bout = new ByteArrayOutputStream();
                 DataOutputStream Dout = new DataOutputStream(Bout);
                 try {
                     Dout.writeInt(SentCount);
                     Dout.writeUTF("json");
                     Dout.writeUTF(msg);
                     NanoWebSocketServer.sendToAll(Bout.toByteArray());
                 } catch (IOException e) {
                     // e.printStackTrace();
                 }
             }
        } catch (IOException e1) {
            e1.printStackTrace();
            myPrint("\nError in Data Procces." + e1.getMessage());
        }

        if(SentCount > 100){
            try {
                String path = Din.readUTF();
                String type = Din.readUTF();
                String name = Din.readUTF();
                String json = Din.readUTF();

                path = path.trim().replace(File.separatorChar, '/');
                if (path.indexOf('?') >= 0) {
                    path = path.substring(0, path.indexOf('?'));
                }
                if (path.indexOf('#') >= 0) {
                    path = path.substring(0, path.indexOf('#'));
                }

                File homeDir = this.rootDirs.get(0);
                File file = new File(homeDir, path + ".ssjs");
                if (file.exists() & file.isFile()) {
                    runSocket(file, name, json, type, path, webSocket, SentCount);
                } else {
                    System.out.println(path + ".ssjs");
                    System.out.println("File Not Found....!");
                }

            } catch (IOException e1) {
                e1.printStackTrace();
                myPrint("\nError in Data Procces." + e1.getMessage());
            }
        }
    }

    @Override
    protected void onPong(WebSocket webSocket, WebSocketFrame pongFrame) {
        System.out.println("onPong");
    }

    static boolean flag = false;

    public void myPrint(Object obj) {
        if (!flag) {
            return;
        }
        System.out.println(obj);
    }
}
