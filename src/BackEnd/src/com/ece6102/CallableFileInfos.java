package com.ece6102;

import com.ece6102.config.ReadConfigProp;
import com.ece6102.tools.MutableInt;
import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Created by HSD Brice on 28/04/2016.
 */
public class CallableFileInfos implements Callable<JsonObject> {

    private List<String> IPs;
    private JsonObject inputJSON;

    public CallableFileInfos(int row, JsonObject inputJSON) throws IOException {
        ReadConfigProp readConfigProp = new ReadConfigProp();
        this.IPs = readConfigProp.getRowsIps(row);
        this.inputJSON = inputJSON;
    }

    @Override
    public JsonObject call() throws Exception {
        // Return JSON
        JsonObject returnJSON = new JsonObject();

        // Results
        final String IP = "";
        final String timestamp = "";

        // Map of the responses given by the servers
        final ConcurrentHashMap<String, MutableInt> resultsMap = new ConcurrentHashMap<>();
        final HashMap<String, String> finalMap = new HashMap<>();

        // List of Future objects for handling the query
        List<Future<HttpResponse<JsonNode>>> futureList = new ArrayList<>();

        boolean stop = false;
        for (int i = 0; i < IPs.size()/2; i++) {

            for (int j = 0; j < 2; j++) {
                Future<HttpResponse<JsonNode>> future = Unirest.post("http://" + IPs.get(2*i + j) + ":8080/test/getfileinfos")
                        .header("accept", "application/json")
                        .body(inputJSON.toString())
                        .asJsonAsync(new Callback<JsonNode>() {

                            public void failed(UnirestException e) {}

                            public void completed(HttpResponse<JsonNode> response) {
                                JsonNode body = response.getBody();

                                String input = "";
                                if (body.getObject().has("timestamp"))
                                    input = body.getObject().getString("timestamp");
                                if (body.getObject().has("empty"))
                                    input = "empty";

                                if (!input.equals("")) {
                                    MutableInt count = resultsMap.get(input);
                                    if (count == null)
                                        resultsMap.put(input, new MutableInt(1));
                                    else
                                        count.increment();
                                    if (count != null && count.getVal() >= 2) {
                                        finalMap.put("done", input);
                                        finalMap.put("IP", body.getObject().getString("IP"));
                                    }
                                }
                            }

                            public void cancelled() {}

                        });

                futureList.add(future);
            }

            try {
                int nbrTries = 0;
                while (nbrTries < 4) {
                    Thread.sleep(500);
                    if (finalMap.containsKey("done")) {
                        if (finalMap.get("done").equals("empty"))
                            returnJSON.addProperty("empty", "");
                        else
                            returnJSON.addProperty("timestamp", finalMap.get("done"));
                        returnJSON.addProperty("IP", finalMap.get("IP"));
                        stop = true;
                        break;
                    }
                    nbrTries++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new Exception();
            }

            if (stop)
                break;

        }

        for(Future<HttpResponse<JsonNode>> future : futureList)
            future.cancel(true);

        return returnJSON;
    }

}
