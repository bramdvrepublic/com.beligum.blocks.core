//package com.beligum.blocks.resources;
//
//import com.beligum.blocks.database.OBlocksDatabase;
//import com.beligum.blocks.database.interfaces.BlocksDatabase;
//import com.beligum.blocks.resources.AbstractNode;
//import com.orientechnologies.orient.core.record.impl.ODocument;
//
//import java.util.*;
//
///**
// * Created by wouter on 14/05/15.
// */
//public class OrientNode extends AbstractNode
//{
//
//    protected OrientNode() {
//
//    }
//
//    public OrientNode(Object value, Locale locale) {
//        super(value, locale);
//    }
//
//    @Override
//    public boolean isIterable() {
//        boolean retVal = false;
//        if ((isIterable != null && isIterable) ||  (wrappedObject instanceof Iterable && !(wrappedObject instanceof ODocument))){
//            retVal = true;
//        }
//        isIterable = retVal;
//        return retVal;
//    }
//
//    @Override
//    public BlocksDatabase getDatabase() {
//        return OBlocksDatabase.instance();
//    }
//
//}
