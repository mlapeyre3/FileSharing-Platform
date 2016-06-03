package com.ece6102;

import com.ece6102.tools.JSONError;
import com.ece6102.tools.Sha1Hex;
import com.google.gson.*;

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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by HSD Brice on 18/04/2016.
 */
public class SetGroups extends HttpServlet {

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

        if (!inputJSON.has("token") || !inputJSON.has("group_name")) {
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
            String groupId = Sha1Hex.makeSHA1Hash(inputJSON.get("group_name").getAsString() + token);
            String groupName = inputJSON.get("group_name").getAsString();
            JsonArray members = inputJSON.getAsJsonArray("members");

            // Preexisting group
            statement = conn.prepareStatement("SELECT * FROM groups WHERE ID = ?");
            statement.setString(1, groupId);
            ResultSet rs = statement.executeQuery();
            if(rs.next()) {
                inputJSON.remove("token");
                resp = JSONError.errorResp(resp, "The group exist already", inputJSON);
                return;
            }

            // Transactional
            conn.setAutoCommit(false);

            // Request on the table
            statement = conn.prepareStatement("INSERT INTO groups (ID, name) VALUES (?, ?)");
            statement.setString(1, groupId);
            statement.setString(2, groupName);
            int success = statement.executeUpdate();

            if(success != 1) {
                conn.rollback();
                inputJSON.remove("token");
                resp = JSONError.errorResp(resp, "Error while adding the group to the DB", inputJSON);
                return;
            }

            // Add creator
            statement = conn.prepareStatement("INSERT INTO listusers (IDgrp, IDUser) VALUES" +
                    "(?, ?) ON DUPLICATE KEY UPDATE IDgrp=IDgrp");
            statement.setString(1, groupId);
            statement.setString(2, token);
            success = statement.executeUpdate();

            if (success != 0)
                conn.commit();
            else {
                conn.rollback();
                inputJSON.remove("token");
                resp = JSONError.errorResp(resp, "Error while adding the creator to the list of users", inputJSON);
                return;
            }

            outputJSON.addProperty("group_id", groupId);
            outputJSON.addProperty("group_name", groupName);

            // Add users
            JsonArray badList = new JsonArray();
            for (JsonElement member1 : members) {
                String member = member1.getAsString();
                statement = conn.prepareStatement("SELECT ID FROM user WHERE mail = ?");
                statement.setString(1, member);
                rs = statement.executeQuery();
                if (rs.next()) {
                    statement = conn.prepareStatement("INSERT INTO listusers (IDgrp, IDUser) VALUES (?, ?) ON DUPLICATE KEY UPDATE IDgrp=IDgrp");
                    statement.setString(1, groupId);
                    statement.setString(2, rs.getString(1));
                    statement.executeUpdate();
                } else
                    badList.add(member);
            }

            conn.commit();

            // Verify results
            if (badList.size() != 0) {
                outputJSON.add("members", badList);
                resp = JSONError.errorResp(resp, "Error while adding some users", outputJSON);
                return;
            }

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
                    conn.setAutoCommit(true);
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
