package com.ece6102.raft;

import com.ece6102.tools.JSONError;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static com.ece6102.raft.AppendToLogList.appendToLogsList;

/**
 * Created by HSD Brice on 25/04/2016.
 */
public class ReplicateFile extends HttpServlet {

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
        if (!inputJSON.has("group_id") || !inputJSON.has("file")) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }

        int indexLog = appendToLogsList(inputJSON);

        // Check the lastApply to see if the file is committed
        int nbrIter = 0;
        try {
            while (nbrIter < 15) {
                Thread.sleep(500);
                if (RaftServer.lastApplied >= indexLog) {
                    outputJSON.addProperty("status", "OK");
                    out.write(outputJSON.toString());
                    return;
                }
                nbrIter++;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        resp = JSONError.errorResp(resp, "File not replicated! Please try again.");

    }


}
