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
 * Created by HSD Brice on 27/04/2016.
 */
public class ReceiveLog extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RaftServer.logrcvd++;

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
        if (!inputJSON.has("term") || !inputJSON.has("leaderID") || !inputJSON.has("leaderCommit")
                || !inputJSON.has("prevLogIndex") || !inputJSON.has("prevLogTerm")) {
            resp = JSONError.errorResp(resp, "The JSON is not well formatted");
            return;
        }

        RaftServer.cancelTimerElection(true);
        RaftServer.launchTimerElection();

        // Verify term
        outputJSON.addProperty("term", RaftServer.currentTerm);
        if (inputJSON.get("term").getAsInt() < RaftServer.currentTerm) {
            resp = JSONError.errorResp(resp, "Lower term than the requested server", outputJSON);
            return;
        } else {
            if (inputJSON.get("term").getAsInt() != RaftServer.currentTerm) {
                RaftServer.currentTerm = inputJSON.get("term").getAsInt();
                if (RaftServer.state == RaftServer.STATE_LEADER) {
                    RaftServer.cancelTimerHeartbeats();
                    RaftServer.state = RaftServer.STATE_FOLLOWER;
                    RaftServer.launchTimerElection();
                }
            }
            RaftServer.leaderID = inputJSON.get("leaderID").getAsString();
        }

        // Verify previous log
        LogRaft prevLogRaft;
        if (RaftServer.logRaftList.size() == 0)
            prevLogRaft = new LogRaft(0,0,null,0);
        else
            prevLogRaft = RaftServer.logRaftList.get(inputJSON.get("prevLogIndex").getAsInt());
        if (prevLogRaft == null) {
            resp = JSONError.errorResp(resp, "No previous Log matching (index)", outputJSON);
            return;
        } else if (prevLogRaft.getTerm() != inputJSON.get("prevLogTerm").getAsInt()) {
            resp = JSONError.errorResp(resp, "No previous Log matching (term)", outputJSON);
            return;
        }

        // Add and remove if required
        if (inputJSON.has("lograft")) {
            JsonObject logRaftJSON = inputJSON.get("lograft").getAsJsonObject();
            LogRaft logRaft = new LogRaft(logRaftJSON.get("term").getAsInt(), logRaftJSON.get("index").getAsInt(), logRaftJSON.get("json").getAsJsonObject(), logRaftJSON.get("timestamp").getAsLong());
            if (RaftServer.logRaftList.contains(logRaft.getIndex())) {
                // Remove next entries in the followers
                RaftServer.logRaftList.deleteFrom(logRaft.getIndex());
            }
            RaftServer.logRaftList.put(logRaft);
        }

        // Commit entries
        RaftServer.commitIndex = Math.min(inputJSON.get("leaderCommit").getAsInt(), RaftServer.logRaftList.getLastIndex());
        int tmp = RaftServer.lastApplied + 1;
        CommitLog commitLog = new CommitLog();
        for (int i = tmp; i <= RaftServer.commitIndex; i++) {
            if (commitLog.Commit(RaftServer.logRaftList.get(i)))
                RaftServer.lastApplied++;
            else
                break;
        }

        RaftServer.state = RaftServer.STATE_FOLLOWER;

        outputJSON.addProperty("term", RaftServer.currentTerm);

        out.write(outputJSON.toString());

    }

}
