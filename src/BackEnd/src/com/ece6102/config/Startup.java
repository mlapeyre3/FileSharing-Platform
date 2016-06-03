package com.ece6102.config;

import com.ece6102.raft.RaftServer;
import com.mashape.unirest.http.Unirest;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.*;
import java.net.InetAddress;
import java.util.*;

/**
 * Created by HSD Brice on 20/04/2016.
 */
public class Startup implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        Properties prop = new Properties();
        InputStream inputStream;
        OutputStream output = null;

        try {
            String propFileName = "config.properties";

            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            String IP = InetAddress.getLocalHost().getHostAddress();

            int nbrRows = Integer.parseInt(prop.getProperty("NBRROWS"));
            int nbrCols = Integer.parseInt(prop.getProperty("NBRCOLS"));

            String[] row = new String[0];
            for (int i = 1; i <= nbrRows; i++) {
                row = prop.getProperty("ROW" + String.valueOf(i)).split(",");
                if (Arrays.asList(row).contains(IP))
                    break;
            }

            String[] col = new String[0];
            for (int i = 1; i <= nbrCols; i++) {
                col = prop.getProperty("COL" + String.valueOf(i)).split(",");
                if (Arrays.asList(col).contains(IP))
                    break;
            }

            String rowFinal = "";
            for (String ip : row)
                if (!ip.equals(IP))
                    rowFinal += ip + ",";

            String colFinal = "";
            for (String ip : col)
                colFinal += ip + ",";

            String TMP_DIR = System.getProperty("java.io.tmpdir");
            output = new FileOutputStream(TMP_DIR + "/" + propFileName);

            // set the properties value
            prop.setProperty("COL", colFinal.substring(0, colFinal.length() - 1));
            prop.setProperty("ROW", rowFinal.substring(0, rowFinal.length() - 1));

            // save properties to project root folder
            prop.store(output, null);

            //Starting thread
            RaftServer raftServer = new RaftServer(10000, 2500, nbrRows);


        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        try {
            Unirest.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
