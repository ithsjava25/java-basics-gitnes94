package com.example;

import java.util.HashMap;
import java.util.Map;

public class ArgParser {

    public static Map<String, String> parse(String[] args) {
        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String key = a.substring(2);
                String value = "true";

                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    value = args[++i];
                }
                map.put(key, value);
            }
        }
        return map;
    }
}
