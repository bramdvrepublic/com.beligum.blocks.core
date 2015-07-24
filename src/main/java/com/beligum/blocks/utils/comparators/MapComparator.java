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

    public int compare(Map<String, String> first,
                       Map<String, String> second)
    {
        // TODO: Null checking, both for maps and values
        String firstValue = first.get(key);
        String secondValue = second.get(key);
        return firstValue.compareTo(secondValue);
    }
}
