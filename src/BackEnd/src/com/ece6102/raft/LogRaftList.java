package com.ece6102.raft;

import java.util.TreeMap;

/**
 * Created by HSD Brice on 27/04/2016.
 */
public class LogRaftList {

    private TreeMap<Integer, LogRaft> treeMap;

    public LogRaftList() {
        this.treeMap = new TreeMap<>();
    }

    public void put(LogRaft logRaft) {
        if (treeMap.size () < 100) {
            treeMap.put(logRaft.getIndex(), logRaft);
        } else {
            int first = treeMap.firstKey();
            if (first < logRaft.getIndex()) {
                treeMap.pollFirstEntry();
                put(logRaft);
            }
        }
    }

    public LogRaft get(int index) {
        if (treeMap.containsKey(index))
            return treeMap.get(index);
        else
            return null;
    }

    public int getLastIndex() {
        try {
            return treeMap.lastKey();
        } catch (Exception e) {
            return 1;
        }
    }

    public LogRaft getLast() {
        if (treeMap.size() == 0)
            return null;
        return treeMap.lastEntry().getValue();
    }

    public boolean contains(int index) {
        return treeMap.containsKey(index);
    }

    public void deleteFrom(int index) {
        treeMap.tailMap(index).clear();
    }

    @Override
    public String toString() {
        return "LogRaftList{" +
                "treeMap=" + treeMap.toString() +
                '}';
    }

    public int size() {
        return treeMap.size();
    }
}
