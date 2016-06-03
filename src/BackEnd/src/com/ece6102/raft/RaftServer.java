package com.ece6102.raft;

import com.ece6102.config.ReadConfigProp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by HSD Brice on 21/04/2016.
 */
public class RaftServer {

    // Static states
    public static final int STATE_LEADER = 0;
    public static final int STATE_FOLLOWER = 1;
    public static final int STATE_CANDIDATE = 2;

    // Timers
    private static ScheduledThreadPoolExecutor scheduledThreadPool1;
    private static ScheduledThreadPoolExecutor scheduledThreadPool2;
    private static ScheduledThreadPoolExecutor scheduledThreadPool3;
    private static ScheduledThreadPoolExecutor scheduledThreadPool4;
    public static ScheduledFuture scheduledElection;
    public static ScheduledFuture scheduledHeartbeat;
    public static ScheduledFuture scheduledHeartbeat1;
    public static ScheduledFuture scheduledHeartbeat2;

    // Attributes
    // Todo : make some parameters persistent
    public static int currentTerm;
    public static String votedFor;
    public static LogRaftList logRaftList;
    public static int state;
    public static String serverID;
    public static String leaderID;
    public static int commitIndex;
    public static int lastApplied;
    public static double majority;
    public static int logrcvd;
    public static String debug = null;

    // Timers attributes
    private static int electionTimeout;
    private static int heartbeatTimeout;

    // States of followers
    static HashMap<String, Integer> nextIndex;
    static ConcurrentHashMap<String, Integer> matchIndex;

    // List others srvs
    public static List<String> listIPSrv = new ArrayList<>();

    // General attributes
    public static int nbrRows;

    public RaftServer(int electionTimeout, int heartbeatTimeout, int nbrRows) {
        state = STATE_FOLLOWER;
        currentTerm = 0;
        votedFor = "";
        this.nbrRows = nbrRows;
        this.logRaftList = new LogRaftList();
        this.electionTimeout = electionTimeout;
        this.heartbeatTimeout = heartbeatTimeout;
        try {
            serverID = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        launchTimerElection();

        // List of row servers
        final ReadConfigProp readConfigProp = new ReadConfigProp();
        try {
            listIPSrv = readConfigProp.getIPRowsCols().get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        majority = Math.ceil(RaftServer.listIPSrv.size() / 2);
        logrcvd = 0;

    }

    public static void cancelTimerElection(boolean force) {
        scheduledElection.cancel(force);
        scheduledThreadPool1.shutdown();
    }

    public static void launchTimerElection () {
        scheduledThreadPool1 = new ScheduledThreadPoolExecutor(1);
        int randomInt =  heartbeatTimeout + 3500 + (int) (Math.random() * (electionTimeout - heartbeatTimeout - 500));
        scheduledElection = scheduledThreadPool1.scheduleAtFixedRate(new RequestVotes(), randomInt, randomInt, TimeUnit.MILLISECONDS);
    }

    public static void cancelTimerHeartbeats() {
        scheduledHeartbeat.cancel(true);
        scheduledThreadPool2.shutdown();
        scheduledHeartbeat1.cancel(true);
        scheduledThreadPool3.shutdown();
        scheduledHeartbeat2.cancel(true);
        scheduledThreadPool4.shutdown();
    }

    public static void launchTimerHeartbeats () {
        SendLog sendLog1 = new SendLog(listIPSrv.get(0));
        scheduledThreadPool2 = new ScheduledThreadPoolExecutor(5);
        scheduledHeartbeat = scheduledThreadPool2.scheduleAtFixedRate(sendLog1, 0, heartbeatTimeout, TimeUnit.MILLISECONDS);
        SendLog sendLog2 = new SendLog(listIPSrv.get(1));
        scheduledThreadPool3 = new ScheduledThreadPoolExecutor(1);
        scheduledHeartbeat1 = scheduledThreadPool3.scheduleAtFixedRate(sendLog2, 0, heartbeatTimeout, TimeUnit.MILLISECONDS);
        SendLog sendLog3 = new SendLog(listIPSrv.get(2));
        scheduledThreadPool4 = new ScheduledThreadPoolExecutor(1);
        scheduledHeartbeat2 = scheduledThreadPool4.scheduleAtFixedRate(sendLog3, 0, heartbeatTimeout, TimeUnit.MILLISECONDS);
    }

}
