package com.beligum.blocks.models.sql;

import java.util.HashMap;

/**
 * Created by wouter on 19/05/15.
 */
public class Schema
{
    private static Schema instance;
    private HashMap<String, Table> resources;


    private Schema() {

    }

    public static Schema instance() {
        if (Schema.instance == null) {
            Schema.instance = new Schema();
        }
        return Schema.instance;
    }

    public static void generate() {

    }

    public Table getTable(String name) {
        return resources.get(name);
    }

    public void setTable(String name, Table table) {
        resources.put(name, table);
    }



}
