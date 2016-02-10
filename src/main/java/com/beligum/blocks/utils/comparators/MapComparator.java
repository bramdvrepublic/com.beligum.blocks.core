package com.beligum.blocks.utils.comparators;

import java.util.Comparator;
import java.util.Map;

/**
 * Created by wouter on 24/07/15.
 */
public class MapComparator implements Comparator<Map<String, String>>
{
    private final String key;

    public MapComparator(String key)
    {
        this.key = key;
    }

    public int compare(Map<String, String> first, Map<String, String> second)
    {
        String l = "";
        String r = "";
        if (first != null) {
            String val = first.get(key);
            if (val != null) {
                l = val;
            }
        }
        if (second != null) {
            String val = second.get(key);
            if (val != null) {
                r = val;
            }
        }

        return l.compareTo(r);
    }
}
