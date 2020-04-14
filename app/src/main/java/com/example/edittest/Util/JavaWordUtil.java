package com.example.edittest.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaWordUtil {
    private static final String[] javaKeyWords = "null true false abstract default if private this boolean do implements protected throw break double import public throws byte else instanceof return transient case extends int short try catch final interface static void char finally long strictfp volatile class float super while const for new switch continue goto package synchronized const native strictfp transient volatile".split(" ");
    private static final char[] operator = "+-*/<>=&|".toCharArray();
    private Map<Integer, int[]> commentPos = new HashMap<>();

    public static boolean isKey(String key) {
        for (String s : javaKeyWords) {
            if (s.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public int[] getCommentPos(int row) {
        return commentPos.get(row);
    }

    public void setCommentPos(int row, int[] pos) {
        commentPos.put(row, pos);
    }

    public static boolean isNum(String key) {
        int i = 0;
        for (char c : key.toCharArray()) {
            if (!(c <= '9' && c >= '0')) {
                if (!((c == 'f' || c == 'F') && i == key.length() - 1)) {
                    return false;
                }
            }
            i++;
        }
        return true;
    }

    public static boolean isOperator(char c) {
        for (char ch : operator) {
            if (c == ch) return true;
        }
        return false;
    }

    public static boolean isMethod(int col, String line) {
        if (col < line.length()) {
            String string = line.substring(col);
            for (char c : string.toCharArray()) {
                if (c != ' ') {
                    if (c == '(') return true;
                    else return false;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public static String[] getJavaKeyWords() {
        return javaKeyWords;
    }


    public static boolean isConstructor(int f_col,int col,String line){
        if (isMethod(col,line)){
            if (f_col>0){
                String s=line.substring(0,f_col).trim();
                if (s.endsWith("new")) return true;
            }
        }
        return false;
    }
}
