///*
// * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.beligum.blocks.rdf.ontologies.main;
//
//import com.beligum.blocks.endpoints.ifaces.AutocompleteSuggestion;
//
//import java.net.URI;
//
///**
// * Created by bram on 3/12/16.
// */
//public class ResourceSuggestion implements AutocompleteSuggestion
//{
//    //-----CONSTANTS-----
//
//    //-----VARIABLES-----
//    private URI resourceId;
//    private URI resourceType;
//    private URI page;
//    private String title;
//    private String subTitle;
//
//    //-----CONSTRUCTORS-----
//    public ResourceSuggestion(URI resourceId, URI resourceType, URI page, String title, String subTitle)
//    {
//        this.resourceId = resourceId;
//        this.resourceType = resourceType;
//        this.title = title;
//        this.subTitle = subTitle;
//        this.page = page;
//    }
//
//    //-----PUBLIC METHODS-----
//    @Override
//    public String getValue()
//    {
//        return resourceId == null ? null : resourceId.toString();
//    }
//    @Override
//    public URI getResourceType()
//    {
//        return resourceType;
//    }
//    @Override
//    public URI getPublicPage()
//    {
//        return page;
//    }
//    @Override
//    public String getTitle()
//    {
//        return title;
//    }
//    @Override
//    public String getSubTitle()
//    {
//        return subTitle;
//    }
//
//    //-----PROTECTED METHODS-----
//
//    //-----PRIVATE METHODS-----
//
//}
