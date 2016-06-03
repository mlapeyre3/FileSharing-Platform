package com.ece6102;

import com.ece6102.tools.JSONError;
import com.google.gson.JsonObject;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by HSD Brice on 30/04/2016.
 */
public class GetFileInfos extends HttpServlet {

    @Resource(name="jdbc/managment_bdd")
    private DataSource ds;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Set response parameters
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        JsonObject outputJSON = new JsonObject();
        outputJSON.addProperty("IP", req.getRemoteAddr());

        // Verify input
        JsonObject inputJSON = JSONError.testJson(req);
        if (inputJSON.isJsonNull()) {
            resp = JSONError.errorResp(resp, "Input request empty or not well formatted", outputJSON);
            return;
        }
        if (!inputJSON.has("file_id")) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted", outputJSON);
            return;
        }

        // Retrieve file information in the Database
        Connection conn = null;
        PreparedStatement statement = null;
        try {
            conn = ds.getConnection();

            // Get query data
            String fileId = inputJSON.get("file_id").getAsString();

            // Add users
            statement = conn.prepareStatement("SELECT timestamp FROM files WHERE fileID = ?");
            statement.setString(1, fileId);
            ResultSet result = statement.executeQuery();

            if ( result.next() ) {
                String timestamp = result.getString( "timestamp" );
                outputJSON.addProperty("timestamp", timestamp);
            } else
                outputJSON.addProperty("empty", "no such file");

            out.write(outputJSON.toString());

        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch(SQLException ignored) {}
            }
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            outputJSON.addProperty("error", "Error while querying the BDD " + e.toString());
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

    }

}
