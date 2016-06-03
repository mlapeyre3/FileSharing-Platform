package com.ece6102.raft;

import com.google.gson.JsonObject;

/**
 * Created by HSD Brice on 28/04/2016.
 */
public abstract class AppendToLogList {

    public static int appendToLogsList(JsonObject inputJSON) {
        // Create log for raft replication
        int index;
        if (RaftServer.logRaftList.size() == 0)
            index = 1;
        else
            index = RaftServer.logRaftList.getLastIndex() + 1;
        long timestamp = System.currentTimeMillis();
        LogRaft logRaft = new LogRaft(RaftServer.currentTerm, index, inputJSON, timestamp);

        RaftServer.logRaftList.put(logRaft);

        return index;
    }

}
