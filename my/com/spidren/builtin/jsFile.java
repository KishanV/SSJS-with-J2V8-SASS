package com.spidren.builtin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.spidren.v8Worker;

public class jsFile implements JavaCallback {
    V8 v8;

    public jsFile(V8 v8) {
        this.v8 = v8;
    }

    @Override
    public Object invoke(V8Object receiver, V8Array parameters) {
        String path = "";
        if (parameters.length() > 0) {
            Object arg1 = parameters.get(0);
            path = arg1.toString();
            if (arg1 instanceof Releasable) {
                ((Releasable) arg1).release();
            }
        } else {
            //return null;
        }
        V8Object va = new V8Object(v8);
        FileOp qr = new FileOp(path, v8);
        va.registerJavaMethod(qr, "readString", "readString", new Class<?>[]{}, false);
        va.registerJavaMethod(qr, "readString0", "readString0", new Class<?>[]{}, false);
        va.registerJavaMethod(qr, "files", "files", new Class<?>[]{}, false);
        va.registerJavaMethod(qr, "folders", "folders", new Class<?>[]{}, false);
        va.registerJavaMethod(qr, "delete", "delete", new Class<?>[]{}, false);
        va.registerJavaMethod(qr, "list", "list", new Class<?>[]{}, false);
        va.registerJavaMethod(qr, "renameTo", "renameTo", new Class<?>[]{String.class, String.class, Boolean.class}, false);
        va.registerJavaMethod(qr, "setAbsolutePath", "setAbsolutePath", new Class<?>[]{String.class}, false);
        va.registerJavaMethod(qr, "overWrite", "overWrite", new Class<?>[]{Object.class}, false);
        va.registerJavaMethod(qr, "create", "create", new Class<?>[]{}, false);
        va.registerJavaMethod(qr, "createDir", "createDir", new Class<?>[]{}, false);
        va.registerJavaMethod(qr, "isExist", "isExist", new Class<?>[]{}, false);
        va.registerJavaMethod(qr, "getAbsolutePath", "getAbsolutePath", new Class<?>[]{}, false);
        va.registerJavaMethod(qr, "getPath", "getPath", new Class<?>[]{}, false);
        return va;
    }

    class FileOp {
        String Path = "";
        boolean AbsolutePath = false;

        public FileOp(String path, V8 v8) {
            Path = path;
        }

        public Object setAbsolutePath(String destPAth) {
            Path = destPAth;
            AbsolutePath = true;
            return null;
        }


        public String getAbsolutePath() {
            return new File(v8Worker.jsfile.getParent(), Path).getAbsolutePath();
        }

        public String getPath() {
            return "../"+v8Worker.jsfile.getParent();
        }

        public Object overWrite(Object obj) throws SQLException, IOException {
            String data = (String) obj;
            File file = new File(v8Worker.jsfile.getParent(), Path);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data.getBytes(StandardCharsets.UTF_8));
            fos.close();
            return null;
        }

        public Object create() throws SQLException, IOException {
            File file = new File(v8Worker.jsfile.getParent(), Path);
            if (!file.exists()) {
                file.createNewFile();
                return true;
            }
            return false;
        }

        public Object createDir() throws SQLException, IOException {
            File file = new File(v8Worker.jsfile.getParent(), Path);
            if (!file.exists()) {
                file.mkdirs();
                return true;
            }
            return false;
        }

        public Object delete() throws SQLException, FileNotFoundException, UnsupportedEncodingException {
            File file = new File(v8Worker.jsfile.getParent(), Path);
            return file.delete();
        }

        public Object isExist() throws SQLException, FileNotFoundException, UnsupportedEncodingException {
            File file = new File(v8Worker.jsfile.getParent(), Path);
            return file.exists();
        }

        public Object renameTo(String folder, String Name, Boolean delete) throws SQLException, IOException {
            File file;
            if (!AbsolutePath) {
                file = new File(v8Worker.jsfile.getParent(), Path);
            } else {
                file = new File(Path);
            }
            File destFolder = new File(v8Worker.jsfile.getParent(), folder);
            File destFile = new File(v8Worker.jsfile.getParent(), folder + '/' + Name);
            if (delete) {
                destFile.delete();
            }
            //destFile.createNewFile();
            destFolder.mkdirs();
            return file.renameTo(destFile);
        }

        public Object readString() throws SQLException, FileNotFoundException, UnsupportedEncodingException {
            File file = new File(v8Worker.jsfile.getParent(), Path);
            if (file.exists() && file.canRead()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        new FileInputStream(file), "UTF-8"));
                return v8Worker.ReadBigStringIn(in);
            }
            return null;
        }

        public Object readString0() throws SQLException, FileNotFoundException, UnsupportedEncodingException {
            File file = new File(v8Worker.jsfile.getParent(), Path);
            if (file.exists() && file.canRead()) {
                try {
                    byte[] encoded = Files.readAllBytes(Paths.get(v8Worker.jsfile.getParent(), Path));
                    return new String(encoded, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        public Object files() throws SQLException, FileNotFoundException,
                UnsupportedEncodingException {
            File file = new File(v8Worker.jsfile.getParent(), Path);
            if (file.exists() && file.canRead()) {
                V8Array vr = new V8Array(v8);
                File[] list = file.listFiles();
                for (int i = 0; i < list.length; i++) {
                    File f = list[i];
                    if (f.isFile()) {
                        vr.push(f.getName());
                    }
                }
                return vr;
            }
            return null;
        }

        public Object folders() throws SQLException, FileNotFoundException,
                UnsupportedEncodingException {
            File file = new File(v8Worker.jsfile.getParent(), Path);
            if (file.exists() && file.canRead()) {
                V8Array vr = new V8Array(v8);
                File[] list = file.listFiles();
                for (int i = 0; i < list.length; i++) {
                    File f = list[i];
                    if (f.isDirectory()) {
                        vr.push(f.getName());
                    }
                }
                return vr;
            }
            return null;
        }

        public Object list() throws SQLException, FileNotFoundException, UnsupportedEncodingException {
            File file = new File(v8Worker.jsfile.getParent(), Path);
            if (file.exists() && file.canRead()) {
                V8Array vr = new V8Array(v8);
                File[] list = file.listFiles();
                for (int i = 0; i < list.length; i++) {
                    V8Object fl = new V8Object(v8);
                    File f = list[i];
                    if (f.isDirectory()) {
                        fl.add("name", f.getName());
                        fl.add("isFolder", true);
                    } else {
                        fl.add("name", f.getName());
                        fl.add("isFolder", false);
                    }
                    vr.push(fl);
                }
                return vr;
            }
            return null;
        }

    }
}
