package com.beligum.blocks.models.resources.map;

import com.beligum.blocks.models.resources.AbstractNode;
import com.beligum.blocks.models.resources.interfaces.Node;

import java.util.*;

/**
 * Created by wouter on 31/05/15.
 */
public class MapNode  extends AbstractNode
{

    protected MapNode() {

    }

    public MapNode(Object value, Locale locale) {
        super(value, locale);
    }

    @Override
    public boolean isIterable() {
        boolean retVal = false;
        if ((isIterable != null && isIterable) ||  (wrappedObject instanceof List || wrappedObject instanceof Set)){
            retVal = true;
        }
        isIterable = retVal;
        return retVal;
    }

    @Override
    public Iterator<Node> iterator()
    {
        Iterator retVal = null;
        if (this.isIterable()) {
            retVal = new NodeIterator(((Iterable)wrappedObject).iterator(), language);
        } else {
            List list = new ArrayList();
            list.add(wrappedObject);
            retVal = new NodeIterator(((Iterable)wrappedObject).iterator(), language);
        }
        return retVal;
    }

    private class NodeIterator implements Iterator<Node>
    {
        private Iterator internalIterator;
        private Locale locale;

        public NodeIterator(Iterator value, Locale locale) {
            internalIterator = value;
            this.locale = locale;
        }

        @Override
        public boolean hasNext()
        {
            return internalIterator.hasNext();
        }

        @Override
        public Node next()
        {
            Object value = internalIterator.next();
            return MapResourceFactory.instance().asNode(value, locale);

        }
        @Override
        public void remove()
        {
            internalIterator.remove();
        }
    }
}
