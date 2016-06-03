package com.ece6102;

import com.ece6102.tools.JSONError;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
 * Created by HSD Brice on 27/04/2016.
 */
public class RegisterUser extends HttpServlet {

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

        // Test fields
        if (!inputJSON.has("token")) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }

        // Authentication Part
        String token = inputJSON.get("token").getAsString();
        String googleResponse = Authentication.getUserInfo(token);
        JsonObject googleJSON = new JsonParser().parse(googleResponse).getAsJsonObject();

        // Test Google Response
        if (googleJSON.isJsonNull() || googleJSON.has("error")) {
            resp = JSONError.errorResp(resp, "User not authenticated with Google" + googleJSON.toString());
            return;
        }

        Connection conn = null;
        PreparedStatement statement = null;
        try {
            conn = ds.getConnection();

            // Get query data
            String userMail = googleJSON.get("email").getAsString();
            String userID = googleJSON.get("id").getAsString();

            // Preexisting group
            statement = conn.prepareStatement("INSERT INTO user VALUES (?,?) ON DUPLICATE KEY UPDATE ID=ID;");
            statement.setString(1, userID);
            statement.setString(2, userMail);
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
                } catch (SQLException ignored) {
                }
            }
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            outputJSON.addProperty("error", "Error while querying the BDD " + e.toString());
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException ignored) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }

        out.write(outputJSON.toString());

    }

}
