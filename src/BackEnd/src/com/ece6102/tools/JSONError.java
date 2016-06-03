package com.ece6102.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Created by HSD Brice on 12/04/2016.
 */
public abstract class JSONError {

    public static HttpServletResponse errorResp (HttpServletResponse resp, String error) throws IOException {
        JsonObject outputJSON = new JsonObject();
        PrintWriter out = resp.getWriter();
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        outputJSON.addProperty("error", error);
        out.write(outputJSON.toString());
        return resp;
    }

    public static HttpServletResponse errorResp (HttpServletResponse resp, String error, JsonObject outputJSON) throws IOException {
        PrintWriter out = resp.getWriter();
        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        outputJSON.addProperty("error", error);
        out.write(outputJSON.toString());
        return resp;
    }

    public static JsonObject testJson (HttpServletRequest req) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
        String inputJSONStr;
        inputJSONStr = br.readLine();

        JsonObject inputJSON = new JsonObject();

        if (inputJSONStr == null || inputJSONStr.equals("")) {
            return inputJSON;
        }

        try {
            inputJSON = new JsonParser().parse(inputJSONStr).getAsJsonObject();
        } catch (Exception e) {
            return inputJSON;
        }

        return inputJSON;
    }

}
