package com.beligum.blocks.models.jsonld;

import com.beligum.blocks.models.jsonld.interfaces.Node;
import com.beligum.blocks.models.jsonld.interfaces.Resource;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by wouter on 15/05/15.
 */
public class ResourceIterable implements Iterable<Resource>
{
    private Iterable<ODocument> wrappedObject;
    private Locale locale;

    @Override
    public Iterator<Resource> iterator()
    {
        return new ResourceIterator(wrappedObject.iterator(), locale);
    }

//    public ResourceIterable(Iterable value, Locale locale) {
//        wrappedObject = value;
//        this.locale = locale;
//    }

    public ResourceIterable(Object value, Locale locale) {
        if (value instanceof ODocument) {
            wrappedObject = new ArrayList<ODocument>();
            ((ArrayList) wrappedObject).add(value);
        } else {
            wrappedObject = (Iterable)value;
        }
        this.locale = locale;

    }

    private class ResourceIterator implements Iterator<Resource>
    {
        private Iterator<ODocument> internalIterator;
        private Locale locale;

        public ResourceIterator(Iterator<ODocument> value, Locale locale) {
            internalIterator = value;
            this.locale = locale;
        }

        @Override
        public boolean hasNext()
        {
            return internalIterator.hasNext();
        }

        @Override
        public Resource next()
        {
            return OrientResourceFactory.instance().asResource(internalIterator.next(), locale);
        }
        @Override
        public void remove()
        {
            internalIterator.remove();
        }
    }

}
