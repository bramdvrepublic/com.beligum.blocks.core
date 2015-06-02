package com.beligum.blocks.models.resources.orient;

import com.beligum.blocks.models.resources.AbstractNode;
import com.beligum.blocks.models.resources.interfaces.Node;
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
            Node retVal = null;
            Object value = internalIterator.next();
            if (value instanceof ODocument) {
                retVal =  OrientResourceFactory.instance().asResource((ODocument)value, locale);
            } else {
                retVal =  OrientResourceFactory.instance().asNode(value, locale);
            }
            return retVal;

        }
        @Override
        public void remove()
        {
            internalIterator.remove();
        }
    }

}
