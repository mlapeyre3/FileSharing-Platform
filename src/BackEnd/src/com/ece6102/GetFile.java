package com.ece6102;

import com.ece6102.config.ReadConfigProp;
import com.ece6102.tools.AsyncPost;
import com.ece6102.tools.JSONError;
import com.ece6102.tools.MutableInt;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by HSD Brice on 02/05/2016.
 */
public class GetFile extends HttpServlet {

    private static String TMP_DIR = System.getProperty("java.io.tmpdir");

    @Resource(name="jdbc/managment_bdd")
    private DataSource ds;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Set response parameters
        resp.setContentType("application/json");
        final PrintWriter out = resp.getWriter();
        JsonObject outputJSON = new JsonObject();

        // Get the received JSON
        JsonObject inputJSON = JSONError.testJson(req);
        if (inputJSON.isJsonNull()) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }
        if (!inputJSON.has("token") || !inputJSON.has("group_id") || !inputJSON.has("file_id")) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }

        // Redirect if necessary (for Google Cloud)
        if (inputJSON.has("IP")) {
            String IP = inputJSON.get("IP").getAsString();
            inputJSON.remove("IP");
            try {
                HttpResponse<JsonNode> jsonResponse = Unirest.post("http://" + IP + ":8080/test/getfile")
                        .header("accept", "application/json")
                        .body(inputJSON.toString())
                        .asJson();

                // Response
                JsonNode body = jsonResponse.getBody();
                out.write(body.getObject().toString());
                return;
            }
            catch (UnirestException e) {
                e.printStackTrace();
            }
            return;
        }

        // Verify User
        JsonObject verifyJSON = new JsonParser().parse(inputJSON.toString()).getAsJsonObject();
        verifyJSON.remove("file_id");
        // Retrieve the IP of the management servers
        ReadConfigProp readConfigProp = new ReadConfigProp();
        List<String> listIPSrv = readConfigProp.getIPMgt();

        // List of responses
        List<HashMap<String, MutableInt>> listResults = new ArrayList<>();

        // Asynchronous POST
        try {
            listResults = AsyncPost.doAsyncPost("verifyuser", listIPSrv, verifyJSON);
        } catch (Exception e) {
            resp = JSONError.errorResp(resp, "Error of the proxy server.");
        }

        // Get the file
        try {
            InputStream input = new FileInputStream(TMP_DIR + "/" + inputJSON.get("file_id").getAsString());
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            outputJSON.addProperty("file", stringBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
            resp = JSONError.errorResp(resp, "Error while reading the file");
        }

        out.write(outputJSON.toString());

    }
}
