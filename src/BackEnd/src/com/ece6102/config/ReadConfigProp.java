package com.ece6102.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by HSD Brice on 12/04/2016.
 */
public class ReadConfigProp {

    private InputStream inputStream;

    public List<String> getIPMgt() throws IOException {

        List<String> result = new ArrayList<>();

        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            String[] tmp = prop.getProperty("MGT_SRV").split(",");
            for (String val : tmp)
                result.add(val.trim());

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public List<List<String>> getIPRowsCols() throws IOException {

        List<List<String>> result = new ArrayList<>();

        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            String TMP_DIR = System.getProperty("java.io.tmpdir");
            inputStream = new FileInputStream(TMP_DIR + "/" + propFileName);

            prop.load(inputStream);

            if (prop.containsKey("ROW")) {
                List<String> listTmp = new ArrayList<>();
                String[] tmp = prop.getProperty("ROW").split(",");
                for (String val : tmp)
                    listTmp.add(val.trim());
                result.add(listTmp);
            }

            if (prop.containsKey("COL")) {
                List<String> listTmp = new ArrayList<>();
                String[] tmp = prop.getProperty("COL").split(",");
                for (String val : tmp)
                    listTmp.add(val.trim());
                result.add(listTmp);
            }

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public List<String> getRowsIps(int number) throws IOException {

        List<String> result = new ArrayList<>();

        try {
            Properties prop = new Properties();
            String propFileName = "config.properties";

            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            String[] tmp = prop.getProperty("ROW" + String.valueOf(number)).split(",");
            for (String val : tmp)
                result.add(val.trim());

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

}
