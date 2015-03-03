package com.beligum.blocks.core.dynamic;

import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.blocks.core.models.redis.templates.EntityTemplate;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by wouter on 3/03/15.
 */
public class DynamicBlockHandler
{
    HashMap<String, DynamicBlockListener> listeners = new HashMap<String, DynamicBlockListener>();

    public DynamicBlockHandler() {

    }

    public Element onShow(String type, Element element) throws ParseException
    {
        Element retVal = element;
        if (listeners.containsKey(type)) {
            retVal = listeners.get(type).onShow(element);
        }
        return retVal;
    }

    public Element onSave(String type, Element element) throws ParseException
    {
        Element retVal = element;
        if (listeners.containsKey(type)) {
            retVal = listeners.get(type).onSave(element);
        }
        return retVal;
    }

}
