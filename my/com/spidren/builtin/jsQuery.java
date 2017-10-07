package com.spidren.builtin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;
import com.spidren.config.MysqlConfig;

public class jsQuery {
    V8 v8;
    String query = "";
    public Connection conn = null;

    NamedParameterStatement prest;

    public jsQuery(String queryStr, Connection connection, V8 v8) {
        query = queryStr;
        conn = connection;
        this.v8 = v8;
        try {
            if (jsMysql.conn == null || jsMysql.conn.isClosed()) {
                try {
                    jsMysql.conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + MysqlConfig.DB + "?useUnicode=yes&characterEncoding=UTF-8", "root", MysqlConfig.mysql_pass);

                } catch (SQLException e) {
                    e.printStackTrace();
                    jsMysql.conn = null;
                }
                conn = jsMysql.conn;
            }
            prest = new NamedParameterStatement(conn, query);


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public Object setString(String index, String str) {

        try {
            prest.setString(index, str);
        } catch (SQLException e) {
            System.err.println("value : " + str);
            System.err.println(prest.toString());
            e.printStackTrace();

        }
        return "Ok";
    }

    public Object getQuery() throws SQLException {
        return prest.statement.toString();
    }

    public Object execute() throws SQLException {
        V8Array data = new V8Array(v8);
        ResultSet rs = null;

        rs = prest.executeQuery();

        if (rs != null) {
            try {
                int count = getRowCount(rs);
                ResultSetMetaData rsmd = rs.getMetaData();
                int callcount = rsmd.getColumnCount();
                for (int j = 1; j < count + 1; j++) {
                    rs.next();
                    V8Array vr = new V8Array(v8);
                    V8Value vl = vr;
                    data.push(vl);
                    for (int i = 1; i < callcount + 1; i++) {
                        vr.push(rs.getString(i));
                    }
                    vl.release();
                }
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return data;
    }

    public Object executeWithFields() throws SQLException {
        V8Object data = null;
        V8Object send = new V8Object(v8);
        V8Array field = new V8Array(v8);
        ResultSet rs = null;
        rs = prest.executeQuery();
        if (rs != null) {
            try {

                data = new V8Object(v8);
                int count = getRowCount(rs);
                ResultSetMetaData rsmd = rs.getMetaData();
                int callcount = rsmd.getColumnCount();

                V8Array[] v8Fields = new V8Array[callcount];
                for (int a = 0; a < callcount; a++) {
                    v8Fields[a] = new V8Array(v8);
                    send.add(rsmd.getColumnLabel(a + 1), v8Fields[a]);
                    field.push(rsmd.getColumnLabel(a + 1));
                }

                for (int j = 1; j < count + 1; j++) {
                    rs.next();
                    for (int i = 1; i < callcount + 1; i++) {
                        v8Fields[i - 1].push(rs.getString(i));
                    }
                }

                data.add("data", send);
                data.add("fields", field);
                data.add("count", count);
                send.release();
                field.release();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    public Object update() throws SQLException {
        ResultSet rs = null;
        return prest.executeUpdate() + "";
    }

    public Object insert() throws SQLException {
        int generatedkey = -1;
        prest.executeUpdate();
        ResultSet rs = prest.statement.getGeneratedKeys();
        if (rs.next()) {
            generatedkey = rs.getInt(1);
        }
        return generatedkey;
    }

    public static int getRowCount(ResultSet set) throws SQLException {
        int rowCount;
        int currentRow = set.getRow();            // Get current row
        rowCount = set.last() ? set.getRow() : 0; // Determine number of rows
        if (currentRow == 0)                      // If there was no current row
            set.beforeFirst();                     // We want next() to go to first row
        else                                      // If there WAS a current row
            set.absolute(currentRow);              // Restore it
        return rowCount;
    }

}
