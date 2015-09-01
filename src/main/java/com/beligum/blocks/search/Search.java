package com.beligum.blocks.search;

import java.util.List;

/**
 * Created by wouter on 1/09/15.
 */
public class Search
{



    private List<Field> or;
    private List<Field> and;

    public static enum Type {
        EXACT
    }

    public Search() {

    }

    public Search or(Field field, Search.Type type) {

        return this;
    }

    public Search and(Field field, Search.Type type) {

        return this;
    }

    public Search sort(Field field) {

        return this;
    }

    public void find() {

    }
}
