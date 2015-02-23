package com.beligum.blocks.core.URLMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bas on 23.02.15.
 */
public class Path
{
    private String id;

    private Map<String, String> translations;

    public Path(String id){
        this.id = id;
        this.translations = new HashMap<>();
    }

    public void addTranslation(String language, String pathTranslation){
        this.translations.put(language, pathTranslation);
    }
}
