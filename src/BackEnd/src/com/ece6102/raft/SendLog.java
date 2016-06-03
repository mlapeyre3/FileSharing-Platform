package com.ece6102.raft;

import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by HSD Brice on 26/04/2016.
 */

public class SendLog implements Runnable {

    private String IPSrv;

    public SendLog(String IPSrv) {
        this.IPSrv = IPSrv;
    }

    @Override
    public void run() {
        // Get log to send
        int index = RaftServer.nextIndex.get(IPSrv);
        LogRaft logRaft = RaftServer.logRaftList.get(index);

        // Get previous log
        int indexPrec = index - 1;
        LogRaft logRaftPrec = RaftServer.logRaftList.get(indexPrec);

        // Post arguments
        JsonObject inputJSON = new JsonObject();
        inputJSON.addProperty("term", RaftServer.currentTerm);
        inputJSON.addProperty("leaderID", RaftServer.serverID);
        inputJSON.addProperty("leaderCommit", RaftServer.commitIndex);

        if (logRaftPrec != null) {
            inputJSON.addProperty("prevLogIndex", logRaftPrec.getIndex());
            inputJSON.addProperty("prevLogTerm", logRaftPrec.getTerm());
        } else {
            inputJSON.addProperty("prevLogIndex", 0);
            inputJSON.addProperty("prevLogTerm", 0);
        }

        if (logRaft != null)
            inputJSON.add("lograft", logRaft.toJson());

        // Outputs
        final HashMap<Integer, Integer> results = new HashMap<>();
        results.put(0, 0);

        // Post
        try {
            HttpResponse<JsonNode> jsonResponse = Unirest.post("http://" + IPSrv + ":8080/test/raft/receivelog")
                    .header("accept", "application/json")
                    .body(inputJSON.toString())
                    .asJson();

            // Response
            JsonNode body = jsonResponse.getBody();
            // Bad
            if (jsonResponse.getStatus() > 300) {
                if (body.getObject().has("term") && body.getObject().getInt("term") > RaftServer.currentTerm) {
                    RaftServer.currentTerm = body.getObject().getInt("term");
                    RaftServer.state = RaftServer.STATE_FOLLOWER;
                    RaftServer.cancelTimerHeartbeats();
                    RaftServer.launchTimerElection();
                    return;
                }
                if(body.getObject().has("term")){
                    RaftServer.nextIndex.put(IPSrv, index - 1);
                }
            //Good
            } else {
                if(logRaft != null) {
                    RaftServer.nextIndex.put(IPSrv, index + 1);
                    RaftServer.matchIndex.put(IPSrv, index);

                    // If replicated on the majority commit
                    int tmp = 0;
                    for (Map.Entry<String, Integer> e : RaftServer.matchIndex.entrySet()) {
                        if (e.getValue() >= index)
                            tmp++;
                    }

                    if (tmp >= RaftServer.majority) {
                        CommitLog commitLog = new CommitLog();
                        if (RaftServer.commitIndex < index) {
                            RaftServer.commitIndex = index;
                            int tmp2 = RaftServer.lastApplied + 1;
                            for (int i = tmp2; i <= RaftServer.commitIndex; i++) {
                                if (commitLog.Commit(RaftServer.logRaftList.get(i)))
                                    RaftServer.lastApplied++;
                                else
                                    break;
                            }
                        }
                    }

                }
            }

        } catch (UnirestException e) {
            e.printStackTrace();
        }


    }
}
