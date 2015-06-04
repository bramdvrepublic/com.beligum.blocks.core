package com.beligum.blocks.models.resources.orient;

import com.beligum.blocks.models.resources.AbstractNode;
import com.beligum.blocks.models.resources.interfaces.Node;
import com.beligum.blocks.models.resources.interfaces.ResourceController;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.util.*;

/**
 * Created by wouter on 14/05/15.
 */
public class OrientNode extends AbstractNode
{

    protected OrientNode() {

    }

    protected OrientNode(Object value, Locale locale) {
        super(value, locale);
    }

    @Override
    public boolean isIterable() {
        boolean retVal = false;
        if ((isIterable != null && isIterable) ||  (wrappedObject instanceof Iterable && !(wrappedObject instanceof ODocument))){
            retVal = true;
        }
        isIterable = retVal;
        return retVal;
    }

    @Override
    public ResourceController getResourceController() {
        return OrientResourceController.instance();
    }

}
