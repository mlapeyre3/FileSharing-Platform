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
import java.sql.SQLException;

/**
 * Created by HSD Brice on 28/04/2016.
 */
public class SetFile extends HttpServlet {

    @Resource(name="jdbc/managment_bdd")
    private DataSource ds;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Set response parameters
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        JsonObject outputJSON = new JsonObject();

        // Get the received JSON
        JsonObject inputJSON = JSONError.testJson(req);
        if (inputJSON == null) {
            JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }

        if (!inputJSON.has("file_name") || !inputJSON.has("group_id") || !inputJSON.has("file_id")) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }

        Connection conn = null;
        PreparedStatement statement = null;
        try {
            conn = ds.getConnection();

            // Get query data
            String groupId = inputJSON.get("group_id").getAsString();
            String fileName = inputJSON.get("file_name").getAsString();
            String fileId = inputJSON.get("file_id").getAsString();

            // Add users
            statement = conn.prepareStatement("INSERT INTO listfiles VALUES (?,?,?) ON DUPLICATE KEY UPDATE fileID=fileID;");
            statement.setString(1, fileName);
            statement.setString(2, groupId);
            statement.setString(3, fileId);
            int success = statement.executeUpdate();
            if(success != 1) {
                resp = JSONError.errorResp(resp, "Error while adding user to the DB");
                return;
            }
            outputJSON.addProperty("status", "OK");
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

        out.write(outputJSON.toString());
    }
}
