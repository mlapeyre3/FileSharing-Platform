package com.ece6102;

import com.ece6102.config.ReadConfigProp;
import com.ece6102.tools.AsyncPost;
import com.ece6102.tools.JSONError;
import com.ece6102.tools.MutableInt;
import com.ece6102.tools.ResponsesManager;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by HSD Brice on 02/05/2016.
 */
public class DisplayFiles extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Set response parameters
        resp.setContentType("application/json");
        final PrintWriter out = resp.getWriter();

        // Get the received JSON
        JsonObject inputJSON = JSONError.testJson(req);
        if (inputJSON.isJsonNull()) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }
        if (!inputJSON.has("token") || !inputJSON.has("group_id")) {
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
            listResults = AsyncPost.doAsyncPost("getfileslist", listIPSrv, inputJSON);
        } catch (Exception e) {
            resp = JSONError.errorResp(resp, "Error of the proxy server.");
        }

        // Compute outputJson
        JsonObject outputJSON = ResponsesManager.mergeMaps(listResults.get(0), listResults.get(1), new String[] {"error"},
                new String[] {}, new String[] {"files_list"}, listIPSrv.size());

        // Bad case
        if (!outputJSON.has("files_list") && !outputJSON.has("error"))
            resp = JSONError.errorResp(resp, "No consistent majority found");
        else {
            if (outputJSON.has("error"))
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(outputJSON.toString());
        }

    }

}
