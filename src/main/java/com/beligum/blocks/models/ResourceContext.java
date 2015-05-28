//package com.beligum.blocks.models;
//
//import com.beligum.base.models.BasicModelImpl;
//import com.beligum.blocks.models.jsonld.interfaces.Resource;
//import com.beligum.blocks.models.jsonld.hibernate.ResourceUserType;
//import org.hibernate.annotations.Type;
//import org.hibernate.annotations.TypeDef;
//
//import javax.persistence.Table;
//import java.util.Locale;
//
///*
// * Created by wouter on 21/04/15.
// */
//@TypeDef(name = "jsonb", typeClass = ResourceUserType.class, parameters = {
//                @org.hibernate.annotations.Parameter(name = ResourceUserType.CLASS,
//                                value = "com.beligum.blocks.models.jsonld.interfaces.Resource")})
//@javax.persistence.Entity
//@Table(name="resource")
//public class ResourceContext extends BasicModelImpl
//{
//
//    @Type(type = "jsonb")
//    private Resource data;
//    private String blockId;
//    private Locale language;
//
//
//    public ResourceContext()
//    {
//    }
//
//    public ResourceContext(Resource resource, Locale language) {
//        this();
//        this.data = resource;
//        this.setBlockId(resource.getBlockId());
//        this.language = language;
//    }
//
//    public Resource getResource() {
//        return this.data;
//    }
//
//    public void setResource(Resource resource) {
//        this.data = resource;
//    }
//
//
//    public String getBlockId()
//    {
//        return blockId;
//    }
//    public void setBlockId(String blockId)
//    {
//        this.blockId = blockId;
//    }
//
//
//    public Locale getLanguage()
//    {
//        return language;
//    }
//
//    public void setLanguage(Locale language)
//    {
//        this.language = language;
//    }
//
//
//
//
//
//}
