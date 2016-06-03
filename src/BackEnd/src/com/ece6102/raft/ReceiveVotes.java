package com.ece6102.raft;

import com.ece6102.tools.JSONError;
import com.google.gson.JsonObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by HSD Brice on 22/04/2016.
 */
public class ReceiveVotes extends HttpServlet {

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
        if (!inputJSON.has("term") || !inputJSON.has("candidateID") || !inputJSON.has("lastLogIndex") || !inputJSON.has("lastLogTerm")) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }

        outputJSON.addProperty("term", RaftServer.currentTerm);
        if (inputJSON.get("term").getAsInt() < RaftServer.currentTerm) {
            resp = JSONError.errorResp(resp, "Lower term than the requested server", outputJSON);
            return;
        } else if (inputJSON.get("term").getAsInt() != RaftServer.currentTerm) {
            RaftServer.votedFor = "";
            RaftServer.currentTerm = inputJSON.get("term").getAsInt();
            if (RaftServer.state == RaftServer.STATE_LEADER) {
                RaftServer.cancelTimerHeartbeats();
                RaftServer.state = RaftServer.STATE_FOLLOWER;
                RaftServer.launchTimerElection();
            }
        }
        outputJSON.addProperty("term", RaftServer.currentTerm);

        if (!RaftServer.votedFor.equals("") && !RaftServer.votedFor.equals(inputJSON.get("candidateID").getAsString())) {
            resp = JSONError.errorResp(resp, "Requested server has already vote for this term", outputJSON);
            return;
        }

        LogRaft lastLog = RaftServer.logRaftList.getLast();
        if (lastLog == null)
            lastLog = new LogRaft(0, 0, null, 0);

        if (lastLog.getTerm() > inputJSON.get("lastLogTerm").getAsInt()) {
            resp = JSONError.errorResp(resp, "Requested last log has a higher term", outputJSON);
            return;
        }

        if (lastLog.getIndex() > inputJSON.get("lastLogIndex").getAsInt()) {
            resp = JSONError.errorResp(resp, "Requested last log has a higher index", outputJSON);
            return;
        }

        RaftServer.votedFor = inputJSON.get("candidateID").getAsString();

        out.write(outputJSON.toString());

        RaftServer.cancelTimerElection(true);
        RaftServer.launchTimerElection();

    }
}
