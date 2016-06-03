package com.ece6102;

import com.ece6102.config.ReadConfigProp;

import java.io.*;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.DataSource;

/**
 * Created by HSD Brice on 24/03/2016.
 */

public class HelloWorld extends HttpServlet {

    @Resource(name="jdbc/managment_bdd")
    private DataSource ds;

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
    {
        response.setContentType("plain/text");
        PrintWriter out = response.getWriter();

        out.println("<html>");
        out.println("<body>");

        ReadConfigProp readConfigProp = new ReadConfigProp();
        List<List<String>> listSrv = readConfigProp.getIPRowsCols();

        if (listSrv.size() == 2)
            out.println(listSrv.get(0).toString() + "  " + listSrv.get(1).toString());
        else
            out.println("Empty list.");

        out.println("</body>");
        out.println("</html>");

    }

}
