package com.beligum.blocks.dynamic;

import com.beligum.base.utils.Logger;

import java.util.HashMap;

/**
 * Created by wouter on 3/03/15.
 */
public class DynamicBlockHandler
{
    HashMap<String, DynamicBlockListener> listeners = new HashMap<String, DynamicBlockListener>();

    public DynamicBlockHandler()
    {

    }

    public void register(DynamicBlockListener blockListener)
    {
        if (!listeners.containsKey(blockListener.getType())) {
            listeners.put(blockListener.getType(), blockListener);
        }
        else {
            Logger.error("Trying to register blocklistener twice: " + blockListener.getType());
        }
    }

    public boolean isDynamicBlock(String type)
    {
        return listeners.containsKey(type);
    }

    public DynamicBlockListener getDynamicBlock(String type)
    {
        return listeners.get(type);
    }

}
