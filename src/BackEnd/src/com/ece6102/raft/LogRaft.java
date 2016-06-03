package com.ece6102.raft;

import com.google.gson.JsonObject;

import java.io.Serializable;

/**
 * Created by HSD Brice on 21/04/2016.
 */
public class LogRaft implements Serializable{

    private int term;
    private int index;
    private JsonObject json;
    public long timestamp;

    public LogRaft(int term, int index, JsonObject json, long timestamp) {
        this.term = term;
        this.json = json;
        this.index = index;
        this.timestamp = timestamp;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setJson(JsonObject json) {
        this.json = json;
    }

    public int getTerm() {
        return term;
    }

    public int getIndex() {
        return index;
    }

    public JsonObject getJson() {
        return json;
    }

    public JsonObject toJson() {
        JsonObject ret = new JsonObject();
        ret.addProperty("term", this.term);
        ret.addProperty("index", this.index);
        ret.addProperty("timestamp", this.timestamp);
        ret.add("json", this.json);
        return ret;
    }

    @Override
    public String toString() {
        return "LogRaft{" +
                "term=" + term +
                ", index=" + index +
                ", json=" + json +
                ", timestamp=" + timestamp +
                '}';
    }
}
