package com.beligum.blocks.core.dynamic;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.exceptions.ParseException;
import com.beligum.core.framework.utils.Logger;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.util.HashMap;

/**
 * Created by wouter on 3/03/15.
 */
public class DynamicBlockHandler
{
    HashMap<String, DynamicBlockListener> listeners = new HashMap<String, DynamicBlockListener>();

    private static DynamicBlockHandler instance = null;

    private DynamicBlockHandler() {
        try {
            //TODO: use an Annotation to discover all dynamic blocks and register them here
            register(new TranslationList(BlocksConfig.getDefaultLanguage(), BlocksConfig.getSiteDomainUrl()));
        }catch(MalformedURLException e){
            Logger.error("Found bad site domain: " + BlocksConfig.getSiteDomain());
        }
    }

    public static DynamicBlockHandler getInstance() {
        if(instance == null){
            instance = new DynamicBlockHandler();
        }
        return instance;
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

    public void register(DynamicBlockListener blockListener) {
        listeners.put(blockListener.getType(), blockListener);
    }

    public boolean isDynamicBlock(String type){
        return listeners.containsKey(type);
    }

}
