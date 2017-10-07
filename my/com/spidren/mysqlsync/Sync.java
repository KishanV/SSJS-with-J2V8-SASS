package com.spidren.mysqlsync;


import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import javax.activation.CommandMap;
import java.io.IOException;

/**
 * Created by Pathik on 2/19/2017.
 */
public class Sync {
    public static void main(String args[]) throws IOException {

        String db = "app";
        String fname = "app.sql";
        MysqlHandler handler = new MysqlHandler();
        //handler.backup("pathik.com", "admin", "admin", db, fname);
        handler.restore("localhost", "root", "", db, fname);
    }
}
