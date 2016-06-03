package com.ece6102.tools;

import com.ece6102.raft.RaftServer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by HSD Brice on 19/04/2016.
 */
public abstract class ResponsesManager {

    public static JsonObject mergeMaps(HashMap<String, MutableInt> resultsMap, HashMap<String, MutableInt> errorMap,
                                String[] attributesArray, String[] multipleAttributesArray, String[] multipleJsonArray, int ipSize) {

        // HashMap to get the attributes of all the Json
        HashMap<String, HashMap<String, MutableInt>> finalMap = new HashMap<>();

        // Initialisation
        for (String attr : attributesArray)
            finalMap.put(attr, new HashMap<String, MutableInt>());
        for (String attr : multipleAttributesArray)
            finalMap.put(attr, new HashMap<String, MutableInt>());
        for (String attr : multipleJsonArray)
            finalMap.put(attr, new HashMap<String, MutableInt>());

        //Remove nbr_responses
        resultsMap.remove("nbr_responses");

        // Number corresponding to a majority of responses
        double majority = Math.ceil(ipSize / 2.);

        for (Map.Entry<String, MutableInt> e : resultsMap.entrySet()) {
            JsonObject tmpJson = new JsonParser().parse(e.getKey()).getAsJsonObject();
            for (String attr : attributesArray) {
                if (tmpJson.has(attr)) {
                    String attributeTmp = tmpJson.get(attr).getAsString();
                    MutableInt count = finalMap.get(attr).get(attributeTmp);
                    if (count == null)
                        finalMap.get(attr).put(attributeTmp, e.getValue());
                    else
                        count.increment(e.getValue().getVal());
                }
            }
            for (String attr : multipleAttributesArray) {
                if (tmpJson.has(attr)) {
                    JsonArray multipleAttrArray = tmpJson.getAsJsonArray(attr);
                    for (JsonElement attr1 : multipleAttrArray) {
                        String member = attr1.getAsString();
                        MutableInt count = finalMap.get(attr).get(member);
                        if (count == null)
                            finalMap.get(attr).put(member, e.getValue());
                        else
                            count.increment(e.getValue().getVal());
                    }
                }
            }
            for (String attr : multipleJsonArray) {
                if (tmpJson.has(attr)) {
                    JsonArray multipleAttrArray = tmpJson.getAsJsonArray(attr);
         //           RaftServer.debug += multipleAttrArray.toString();
                    for (JsonElement attr1 : multipleAttrArray) {
                        JsonObject member = attr1.getAsJsonObject();
                        MutableInt count = finalMap.get(attr).get(member.toString());
                        if (count == null)
                            finalMap.get(attr).put(member.toString(), e.getValue());
                        else
                            count.increment(e.getValue().getVal());
                    }
                }
            }
        }

        for (Map.Entry<String, MutableInt> e : errorMap.entrySet()) {
            MutableInt count = finalMap.get("error").get(e.getKey());
            if (count == null)
                finalMap.get("error").put(e.getKey(), e.getValue());
            else
                count.increment(e.getValue().getVal());
        }


        // Go through the finalMap the find the entries present in majority
        // Form the output
        JsonObject outputJSON = new JsonObject();
        for (Map.Entry<String, HashMap<String, MutableInt>> e : finalMap.entrySet()) {
            if (Arrays.asList(multipleAttributesArray).contains(e.getKey())) {
                JsonArray membersArray = new JsonArray();
                for (Map.Entry<String, MutableInt> e2 : e.getValue().entrySet()) {
                    if (e2.getValue().getVal() >= majority)
                        membersArray.add(e2.getKey());
                }
                if (membersArray.size() != 0)
                    outputJSON.add(e.getKey(), membersArray);
            } else {
                if (Arrays.asList(multipleJsonArray).contains(e.getKey())) {
                    JsonArray membersArray = new JsonArray();
                    for (Map.Entry<String, MutableInt> e2 : e.getValue().entrySet()) {
                        if (e2.getValue().getVal() >= majority)
                            membersArray.add(new JsonParser().parse(e2.getKey()).getAsJsonObject());
                    }
                    if (membersArray.size() != 0)
                        outputJSON.add(e.getKey(), membersArray);
                } else {
                    for (Map.Entry<String, MutableInt> e2 : e.getValue().entrySet()) {
                        if (e2.getValue().getVal() >= majority)
                            outputJSON.addProperty(e.getKey(), e2.getKey());
                    }
                }
            }
        }

        return outputJSON;
    }

}
