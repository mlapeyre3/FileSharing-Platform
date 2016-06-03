package com.ece6102;

import com.ece6102.tools.AsyncPost;
import com.ece6102.tools.JSONError;
import com.ece6102.config.ReadConfigProp;
import com.ece6102.tools.MutableInt;
import com.ece6102.tools.ResponsesManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by HSD Brice on 19/04/2016.
 */
public class AddUsers extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Set response parameters
        resp.setContentType("application/json");
        final PrintWriter out = resp.getWriter();

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
        } catch (Exception e) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }

        // Retrieve the IP of the management servers
        ReadConfigProp readConfigProp = new ReadConfigProp();
        List<String> listIPSrv = readConfigProp.getIPMgt();

        // List of responses
        List<HashMap<String, MutableInt>> listResults = new ArrayList<>();

        // Asynchronous POST
        try {
            listResults = AsyncPost.doAsyncPost("setusers", listIPSrv, inputJSON);
        } catch (Exception e) {
            resp = JSONError.errorResp(resp, "Error of the proxy server.");
        }

        // Compute outputJSON
        JsonObject outputJSON = ResponsesManager.mergeMaps(listResults.get(0), listResults.get(1),
                new String[] {"group_id", "error", "status"}, new String[] {"members"}, new String[] {}, listIPSrv.size());

        // Bad case
        if (outputJSON.has("error")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(outputJSON.toString());
        } else {
            if (outputJSON.isJsonNull()) {
                resp = JSONError.errorResp(resp, "No consistency found");
            } else
                out.write(outputJSON.toString());
        }
    }
}
