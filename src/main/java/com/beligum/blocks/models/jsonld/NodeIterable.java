package com.beligum.blocks.models.jsonld;

import com.beligum.blocks.models.jsonld.interfaces.Node;


import java.util.Iterator;
import java.util.Locale;

/**
 * Created by wouter on 15/05/15.
 */
public class NodeIterable implements Iterable<Node>
{
    private Iterable wrappedObject;
    private Locale locale;

    @Override
    public Iterator<Node> iterator()
    {
        return new NodeIterator(wrappedObject.iterator(), locale);
    }

    public NodeIterable(Iterable value, Locale locale) {
        wrappedObject = value;
        this.locale = locale;
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
            return OrientResourceFactory.instance().asNode(internalIterator.next(), locale);
        }
        @Override
        public void remove()
        {
            internalIterator.remove();
        }
    }

}
