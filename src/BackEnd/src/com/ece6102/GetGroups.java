package com.ece6102;

import com.ece6102.raft.RaftServer;
import com.ece6102.tools.JSONError;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.*;

/**
 * Created by HSD Brice on 12/04/2016.
 */
public class GetGroups extends HttpServlet {

    @Resource(name="jdbc/managment_bdd")
    private DataSource ds;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Set response parameters
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        JsonObject outputJSON = new JsonObject();

        // Get the received JSON
        BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
        String inputJSONStr = "";
        inputJSONStr = br.readLine();

        if (inputJSONStr == null || inputJSONStr.equals("")) {
            resp = JSONError.errorResp(resp, "The request is empty");
            return;
        }

        JsonObject inputJSON;
        try {
            inputJSON = new JsonParser().parse(inputJSONStr).getAsJsonObject();
        }
        catch (Exception e) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }

        if (!inputJSON.has("token")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            outputJSON.addProperty("error", "The JSON is not well formatted");
            out.write(outputJSON.toString());
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

            // Request on the table
            statement = conn.prepareStatement("SELECT name, ID FROM groups INNER JOIN " +
                    "listusers ON groups.id = listusers.idgrp WHERE listusers.iduser = ?");
            statement.setString(1, token);
            ResultSet result = statement.executeQuery();

            // Add the results to Json output
            JsonArray jsonArray = new JsonArray();
            while ( result.next() ) {
                String name = result.getString( "name" );
                String ID = result.getString( "ID" );
                JsonObject tmpJSON = new JsonObject();
                tmpJSON.addProperty("id", ID);
                tmpJSON.addProperty("name", name);
                jsonArray.add(tmpJSON);
            }
            outputJSON.add("groups", jsonArray);

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch(SQLException ignored) {}
            }
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            outputJSON.addProperty("error", "Error while querying the BDD" + e.toString());
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
