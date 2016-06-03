package com.ece6102.tools;

/**
 * Created by HSD Brice on 13/04/2016.
 * Used to improve performance of Map-counting elements
 */
public class MutableInt {

    int val;

    public void increment() {
        ++val;
    }

    public void increment(int up) {
        val += up;
    }

    public int getVal() {
        return val;
    }

    public MutableInt(int val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return "MutableInt{" +
                "val=" + val +
                '}';
    }
}
