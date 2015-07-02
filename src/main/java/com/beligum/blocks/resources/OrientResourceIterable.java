//package com.beligum.blocks.resources;
//
//import com.beligum.blocks.database.DummyBlocksDatabase;
//import com.beligum.blocks.database.OBlocksDatabase;
//import com.beligum.blocks.resources.interfaces.Resource;
//
//import java.util.Iterator;
//import java.util.Locale;
//
///**
// * Created by wouter on 15/05/15.
// */
//public class OrientResourceIterable implements Iterable<Resource>
//{
//    private Iterable<ODocument> wrappedObject;
//    private Locale locale;
//
//    @Override
//    public Iterator<Resource> iterator()
//    {
//        return new ResourceIterator(wrappedObject.iterator(), locale);
//    }
//
//
//    public OrientResourceIterable(Iterable<ODocument> value, Locale locale) {
//        wrappedObject = (Iterable)value;
//        this.locale = locale;
//
//    }
//
//    private class ResourceIterator implements Iterator<Resource>
//    {
//        private Iterator<ODocument> internalIterator;
//        private Locale locale;
//
//        public ResourceIterator(Iterator<ODocument> value, Locale locale) {
//            internalIterator = value;
//            this.locale = locale;
//        }
//
//        @Override
//        public boolean hasNext()
//        {
//            return internalIterator.hasNext();
//        }
//
//        @Override
//        public Resource next()
//        {
//            return (Resource) DummyBlocksDatabase.instance().createNode(internalIterator.next(), locale);
//        }
//        @Override
//        public void remove()
//        {
//            internalIterator.remove();
//        }
//    }
//
//}
