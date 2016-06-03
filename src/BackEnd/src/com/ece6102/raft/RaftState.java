package com.ece6102.raft;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by HSD Brice on 23/04/2016.
 */
public class RaftState extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int status = RaftServer.state;
        resp.getWriter().write(status + "/" + RaftServer.currentTerm + "/" + RaftServer.votedFor + "/leader:" + RaftServer.leaderID + "\n" +
        "commit:" + RaftServer.commitIndex + " apply:" + RaftServer.lastApplied + "\nlogs:" + RaftServer.logRaftList.toString() + " nbr_logs:" +
        RaftServer.logrcvd + "listIPSrv:" + RaftServer.listIPSrv + " nextindex: " + RaftServer.nextIndex + " debug" + RaftServer.debug);
    }
}
