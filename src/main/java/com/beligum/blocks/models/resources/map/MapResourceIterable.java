package com.beligum.blocks.models.resources.map;

import com.beligum.blocks.models.resources.interfaces.Resource;
import com.beligum.blocks.models.resources.orient.OrientResourceFactory;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 1/06/15.
 */
public class MapResourceIterable implements Iterable<Resource>
{
    private Iterable<SearchHit> wrappedObject;
    private Locale locale;

    @Override
    public Iterator<Resource> iterator()
    {
        return new ResourceIterator(wrappedObject.iterator(), locale);
    }


    public MapResourceIterable(SearchHits value, Locale locale) {
        wrappedObject = (Iterable)value;
        this.locale = locale;

    }

    private class ResourceIterator implements Iterator<Resource>
    {
        private Iterator<SearchHit> internalIterator;
        private Locale locale;

        public ResourceIterator(Iterator<SearchHit> value, Locale locale) {
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
            return MapResourceFactory.instance().asResource(internalIterator.next().getSource(), locale);
        }
        @Override
        public void remove()
        {
            internalIterator.remove();
        }
    }
}
