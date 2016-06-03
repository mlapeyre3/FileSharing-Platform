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
 * Created by HSD Brice on 21/04/2016.
 */
public class VerifyUser extends HttpServlet {

    @Resource(name="jdbc/managment_bdd")
    private DataSource ds;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Set response parameters
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        JsonObject outputJSON = new JsonObject();

        // Verify input
        JsonObject inputJSON = JSONError.testJson(req);
        if (inputJSON.isJsonNull()) {
            resp = JSONError.errorResp(resp, "Input request empty or not well formatted");
            return;
        }
        if (!inputJSON.has("token") || !inputJSON.has("group_id")) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }

        // Authentication Part
        // Not implemented
        String token = Authentication.authentication(inputJSON.get("token").getAsString());
        if (token.equals("")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            outputJSON.addProperty("error", "User not authenticated");
            out.write(outputJSON.toString());
            return;
        }

        Connection conn = null;
        PreparedStatement statement = null;
        try {
            conn = ds.getConnection();

            // Get query data
            String groupId = inputJSON.get("group_id").getAsString();

            // Preexisting group
            statement = conn.prepareStatement("SELECT * FROM listusers t2 WHERE t2.IDUser = ? AND t2.IDgrp = ?;");
            statement.setString(1, token);
            statement.setString(2, groupId);
            ResultSet rs = statement.executeQuery();
            if(!rs.next()) {
                resp = JSONError.errorResp(resp, "The client is not a member of the group or the group does not exist");
                return;
            }

            outputJSON.addProperty("status", "OK");

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
