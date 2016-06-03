package com.ece6102.raft;

import com.ece6102.config.ReadConfigProp;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by HSD Brice on 21/04/2016.
 */
public class RequestVotes implements Runnable {

    @Override
    public void run() {
        // Increment currentterm
        RaftServer.currentTerm += 1;

        // State Candidate for the white house
        RaftServer.state = RaftServer.STATE_CANDIDATE;

        // Vote for himself
        RaftServer.votedFor = RaftServer.serverID;

        // List of row servers
        ReadConfigProp readConfigProp = new ReadConfigProp();
        List<String> listIPSrv = null;
        try {
            listIPSrv = readConfigProp.getIPRowsCols().get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Post arguments
        JsonObject inputJSON = new JsonObject();
        inputJSON.addProperty("term", RaftServer.currentTerm);
        inputJSON.addProperty("candidateID", RaftServer.serverID);
        LogRaft lastLogRaft = RaftServer.logRaftList.getLast();
        if (lastLogRaft != null) {
            inputJSON.addProperty("lastLogTerm", lastLogRaft.getTerm());
            inputJSON.addProperty("lastLogIndex", lastLogRaft.getIndex());
        } else {
            inputJSON.addProperty("lastLogTerm", 0);
            inputJSON.addProperty("lastLogIndex", 0);
        }

        // Outputs
        final HashMap<Integer, Integer> results = new HashMap<>();
        results.put(0, 0); results.put(1, 0);

        // List of Future objects for handling the query
        List<Future<HttpResponse<JsonNode>>> futureList = new ArrayList<>();

        Unirest.setConcurrency(5, 5);

        for(String Ip : listIPSrv) {
            Future<HttpResponse<JsonNode>> future = Unirest.post("http://" + Ip + ":8080/test/raft/receivevotes")
                    .header("accept", "application/json")
                    .body(inputJSON.toString())
                    .asJsonAsync(new Callback<JsonNode>() {

                        public void failed(UnirestException e) {
                            results.put(0, results.get(0) + 1);
                        }

                        public void completed(HttpResponse<JsonNode> response) {
                            if (response.getStatus() > 300) {
                                results.put(0, results.get(0) + 1);
                            } else {
                                results.put(1, results.get(1) + 1);
                                JsonNode body = response.getBody();
                                if (body.getObject().has("term") && body.getObject().getInt("term") > RaftServer.currentTerm)
                                    RaftServer.currentTerm = body.getObject().getInt("term");
                            }
                        }

                        public void cancelled() {
                            results.put(0, results.get(0) + 1);
                        }

                    });

            futureList.add(future);
        }

        double majority = Math.ceil(listIPSrv.size() / 2);

        int nbrIter = 0;

        while (nbrIter < 4) {
            if (results.get(1) >= majority) {
                RaftServer.state = RaftServer.STATE_LEADER;
                break;
            }
            if (results.get(0) + results.get(1) >= listIPSrv.size())
                break;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nbrIter++;
        }

        for(Future<HttpResponse<JsonNode>> future : futureList)
            future.cancel(true);

        if (RaftServer.state == RaftServer.STATE_LEADER) {
            RaftServer.nextIndex = new HashMap<>();
            RaftServer.matchIndex = new ConcurrentHashMap<>();
            for (String IPs : listIPSrv) {
                RaftServer.nextIndex.put(IPs, inputJSON.get("lastLogIndex").getAsInt() + 1);
                RaftServer.matchIndex.put(IPs, 0);
            }
            RaftServer.leaderID = RaftServer.serverID;
            RaftServer.launchTimerHeartbeats();
            RaftServer.cancelTimerElection(false);
        }

    }

}
