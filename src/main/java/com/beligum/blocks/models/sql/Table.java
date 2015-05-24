package com.beligum.blocks.models.sql;

import java.util.HashMap;

/**
 * Created by wouter on 19/05/15.
 */
public class Table
{
    // take in account translations
    private String name;
    private HashMap<String, Property> properties = new HashMap();

    public Table(String name) {
        this.name = name;
    }

    public Property getProperty(String name) {
        return properties.get(name);
    }

    public void setProperty(String name, Property property) {
        properties.put(name, property);
    }

}
