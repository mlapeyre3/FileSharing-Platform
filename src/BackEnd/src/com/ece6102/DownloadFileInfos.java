package com.ece6102;

import com.ece6102.config.ReadConfigProp;
import com.ece6102.raft.RaftServer;
import com.ece6102.tools.AsyncPost;
import com.ece6102.tools.JSONError;
import com.ece6102.tools.MutableInt;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by HSD Brice on 28/04/2016.
 */
public class DownloadFileInfos extends HttpServlet {

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
        if (!inputJSON.has("token") || !inputJSON.has("group_id") || !inputJSON.has("file_id")) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
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

        // ThreadPool for querying each rows
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

        List<Future<JsonObject>> resultList = new ArrayList<>();

        for (int i = 1; i <= RaftServer.nbrRows; i++) {
            CallableFileInfos requester;
            try {
                requester = new CallableFileInfos(i, inputJSON);
            } catch (Exception e) {
                resp = JSONError.errorResp(resp, "Error while retrieving parameters for row: " + i);
                executor.shutdown();
                return;
            }
            Future<JsonObject> result = executor.submit(requester);
            resultList.add(result);
        }

        String IPSrv = "";
        long timestampMax = 0;
        for(Future<JsonObject> future : resultList) {
            try {
                RaftServer.debug += future.get().toString();
                if (future.get().has("timestamp") && Long.valueOf(future.get().get("timestamp").getAsString()) > timestampMax) {
                    IPSrv = future.get().get("IP").getAsString();
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        //shut down the executor service now
        executor.shutdown();

        if (IPSrv.equals("")) {
            resp = JSONError.errorResp(resp, "No such file found.", outputJSON);
            return;
        }

        outputJSON.addProperty("server", IPSrv);
        out.write(outputJSON.toString());

    }

}
