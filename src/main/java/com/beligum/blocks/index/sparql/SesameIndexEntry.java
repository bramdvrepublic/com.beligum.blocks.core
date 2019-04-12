//package com.beligum.blocks.index.sparql;
//
//import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
//import org.eclipse.rdf4j.query.BindingSet;
//
//import java.net.URI;
//
//public class SesameIndexEntry implements ResourceIndexEntry
//{
//    //-----CONSTANTS-----
//
//    //-----VARIABLES-----
//    private final BindingSet bindingSet;
//
//    //-----CONSTRUCTORS-----
//    public SesameIndexEntry(BindingSet bindingSet)
//    {
//        this.bindingSet = bindingSet;
//    }
//
//    //-----PUBLIC METHODS-----
//    public BindingSet getBindingSet()
//    {
//        return bindingSet;
//    }
//    @Override
//    public URI getUri()
//    {
//        return null;
//    }
//    @Override
//    public String getLabel()
//    {
//        return null;
//    }
//    @Override
//    public String getDescription()
//    {
//        return null;
//    }
//    @Override
//    public URI getImage()
//    {
//        return null;
//    }
//
//    //-----PROTECTED METHODS-----
//
//    //-----PRIVATE METHODS-----
//
//    //-----MGMT METHODS-----
//
//    @Override
//    public String toString()
//    {
//        return this.bindingSet.toString();
//    }
//}
