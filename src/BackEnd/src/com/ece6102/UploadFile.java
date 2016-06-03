package com.ece6102;

import com.ece6102.config.ReadConfigProp;
import com.ece6102.raft.RaftServer;
import com.ece6102.raft.ReplicateFile;
import com.ece6102.tools.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.codec.digest.DigestUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static com.ece6102.raft.AppendToLogList.appendToLogsList;

/**
 * Created by HSD Brice on 25/04/2016.
 */
public class UploadFile extends HttpServlet {

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
        if (!inputJSON.has("token") || !inputJSON.has("group_id") || !inputJSON.has("file") || inputJSON.get("file").getAsString().length() < 2) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }

        // Verify User
        JsonObject verifyJSON = new JsonParser().parse(inputJSON.toString()).getAsJsonObject();
        verifyJSON.remove("file");
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

        // Treat responses
        JsonObject verifJSON = ResponsesManager.mergeMaps(listResults.get(0), listResults.get(1), new String[] {"status", "error"},
                new String[] {}, new String[] {}, listIPSrv.size());
        if (!verifJSON.has("status")) {
            resp = JSONError.errorResp(resp, "User not verified !");
            return;
        }

        // Replicate the file with raft
        if(!(RaftServer.serverID.equals(RaftServer.leaderID))) {
            // Post
            try {
                HttpResponse<JsonNode> jsonResponse = Unirest.post("http://" + RaftServer.leaderID + ":8080/test/raft/replicatefile")
                        .header("accept", "application/json")
                        .body(inputJSON.toString())
                        .asJson();

                // Response
                JsonNode body = jsonResponse.getBody();
                // Bad
                if (jsonResponse.getStatus() > 300) {
                    resp = JSONError.errorResp(resp, "File not replicated! Please try again." + body + inputJSON);
                    return;
                }
            }
            catch (UnirestException e) {
                e.printStackTrace();
            }
        }
        else {  // I am the leader

            int indexLog = appendToLogsList(inputJSON);

            // Check the lastApply to see if the file is committed
            int nbrIter = 0;
            try {
                while (nbrIter < 15) {
                    Thread.sleep(500);
                    if (RaftServer.lastApplied >= indexLog) {
                        break;
                    }
                    if(nbrIter++ >= 15){
                        resp = JSONError.errorResp(resp, "File not replicated! Please try again.");
                        return;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //Add file to database
        // Asynchronous POST
        List<String> listIPSrvMgt = readConfigProp.getIPMgt();
        try {
            JsonObject resultJSON = new JsonObject();

            String grpId = inputJSON.get("group_id").getAsString();
            String fileName = inputJSON.get("file").getAsString().split("/")[0];
            String fileId = Sha1Hex.makeSHA1Hash(grpId + fileName);

            resultJSON.addProperty("group_id",grpId);
            resultJSON.addProperty("file_id",fileId);
            resultJSON.addProperty("file_name",fileName);
            listResults = AsyncPost.doAsyncPost("setfile", listIPSrvMgt, resultJSON);
            outputJSON = ResponsesManager.mergeMaps(listResults.get(0), listResults.get(1), new String[] {"status", "error"},
                    new String[] {}, new String[] {}, listIPSrvMgt.size());
        } catch (Exception e) {
            resp = JSONError.errorResp(resp, "Error of the proxy server.");
            return;
        }

        if (!outputJSON.has("status")) {
            if (outputJSON.has("error"))
                out.write(outputJSON.toString() + "test");
            else
                resp = JSONError.errorResp(resp, "File not replicated! Please try again (BDD).");
        }

        out.write(outputJSON.toString());

        //If status OK post sur asyncpost sur /test/setfile
        //Send json avec comme attribut: le groupID, (le fileId) et le fileName

    }
}
