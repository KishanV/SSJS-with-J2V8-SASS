package com.spidren.mysqlsync;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * Created by Pathik on 2/20/2017.
 */
public class MysqlHandler {
    private static final String MYSQL_BIN = "mysql\\";

    public MysqlHandler() throws IOException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public boolean backup(String host, String user, String pass, String db, String fname) {
        File file = new File(fname);
        if (file.exists() && file.isFile()) {
            file.delete();
        }

        CommandLine line = new CommandLine(MYSQL_BIN + "mysqldump.exe");
        line.addArgument(db);
        line.addArgument("--host=" + host);
        line.addArgument("--user=" + user);
        line.addArgument("--password=" + pass);
        line.addArgument("--result-file=" + fname);
        return executeCommand(line) == 0;
    }

    public boolean restore(String host, String user, String pass, String db, String fname) {
        File file = new File(fname);
        updateQuery("DROP DATABASE IF EXISTS " + db, host, user, pass);
        updateQuery("CREATE DATABASE " + db, host, user, pass);

        if (file.exists() && file.isFile()) {
            CommandLine line = new CommandLine(MYSQL_BIN + "mysql.exe");
            line.addArgument("-u" + user);
            line.addArgument("-p" + pass);
            line.addArgument(db);
            line.addArgument("-e");
            line.addArgument("source " + fname);
            return executeCommand(line) == 0;
        }
        return false;
    }


    private void updateQuery(String sql, String host, String user, String pass) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":3306/", user, pass);
            statement = connection.createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    private int executeCommand(CommandLine line) {
        DefaultExecutor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler());
        try {
            return executor.execute(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }


    public void log(String cmd, String msg) {
        System.out.println("[" + cmd + "]::" + msg);
    }
}
