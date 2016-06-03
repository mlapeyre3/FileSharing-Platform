package com.ece6102.raft;

import com.ece6102.tools.Sha1Hex;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Date;

/**
 * Created by HSD Brice on 28/04/2016.
 */
public class CommitLog {

    private static String TMP_DIR = System.getProperty("java.io.tmpdir");

    public boolean Commit(LogRaft logRaft) {

        // Return
        boolean ret = false;

        // Split the base64 to get the file name
        String fileName = logRaft.getJson().get("file").getAsString().split("/")[0];
        String groupId = logRaft.getJson().get("group_id").getAsString();
        long timestamp = logRaft.timestamp;

        // Forge the fileId
        String fileId = null;
        try {
            fileId = Sha1Hex.makeSHA1Hash(groupId + fileName);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // Write file to the server
        try {
            OutputStream output = new FileOutputStream(TMP_DIR + "/" + fileId);
            output.write(logRaft.getJson().get("file").getAsString().getBytes(Charset.forName("UTF-8")));
            output.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load context
        DataSource ds = null;
        Context initContext = null;
        try {
            initContext = new InitialContext();
            Context envContext  = (Context)initContext.lookup("java:/comp/env");
            ds = (DataSource)envContext.lookup("jdbc/managment_bdd");
        } catch (NamingException e) {
            e.printStackTrace();
        }

        // Add informations to the local Database
        Connection conn = null;
        PreparedStatement statement = null;
        try {
            conn = ds.getConnection();

            // Get timestamp
            Date date= new java.util.Date();

            // Request on the table
            statement = conn.prepareStatement("INSERT INTO files (fileID, groupID, timestamp) VALUES (?, ?, ?) ON DUPLICATE" +
                    " KEY UPDATE timestamp = ?");
            statement.setString(1, fileId);
            statement.setString(2, groupId);
            statement.setString(3, String.valueOf(timestamp));
            statement.setString(4, String.valueOf(timestamp));
            statement.executeUpdate();

            ret = true;

        } catch (Exception e) {
            e.printStackTrace();
            ret = false;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {}
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }

        return ret;

    }

}
