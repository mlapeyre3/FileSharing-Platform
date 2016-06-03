package com.ece6102.tools;

import com.google.gson.JsonObject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by HSD Brice on 20/04/2016.
 */
public abstract class AsyncPost {

    public static List<HashMap<String, MutableInt>> doAsyncPost (String URI, List<String> listIPSrv, JsonObject inputJSON) throws Exception {
        // Return list
        List<HashMap<String, MutableInt>> ret = new ArrayList<>();

        // Map of the responses given by the servers
        final HashMap<String, MutableInt> resultsMap = new HashMap<>();
        final HashMap<String, MutableInt> errorMap = new HashMap<>();

        // Count responses
        resultsMap.put("nbr_responses", new MutableInt(0));

        // List of Future objects for handling the query
        List<Future<HttpResponse<JsonNode>>> futureList = new ArrayList<>();

        for(String Ip : listIPSrv) {
            Future<HttpResponse<JsonNode>> future = Unirest.post("http://" + Ip + ":8080/test/" + URI)
                    .header("accept", "application/json")
                    .body(inputJSON.toString())
                    .asJsonAsync(new Callback<JsonNode>() {

                        public void failed(UnirestException e) {
                            MutableInt count = errorMap.get("fail");
                            if (count == null)
                                errorMap.put("failures", new MutableInt(1));
                            else
                                count.increment();
                            resultsMap.get("nbr_responses").increment();
                        }

                        public void completed(HttpResponse<JsonNode> response) {
                            JsonNode body = response.getBody();

                            MutableInt count = resultsMap.get(body.toString());
                            if (count == null)
                                resultsMap.put(body.toString(), new MutableInt(1));
                            else
                                count.increment();

                            resultsMap.get("nbr_responses").increment();
                        }

                        public void cancelled() {
                            MutableInt count = errorMap.get("cancelled");
                            if (count == null)
                                errorMap.put("cancelled", new MutableInt(1));
                            else
                                count.increment();
                            resultsMap.get("nbr_responses").increment();
                        }

                    });

            futureList.add(future);
        }

        try {
            int nbrTries = 0;
            while (resultsMap.get("nbr_responses").getVal() != listIPSrv.size() && nbrTries < 8) {
                Thread.sleep(500);
                nbrTries++;
            }
            errorMap.put("timeout", new MutableInt(listIPSrv.size() - resultsMap.get("nbr_responses").getVal()));
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new Exception();
        } finally {
            for(Future<HttpResponse<JsonNode>> future : futureList)
                future.cancel(true);
        }

        ret.add(resultsMap);
        ret.add(errorMap);

        return ret;

    }

}
