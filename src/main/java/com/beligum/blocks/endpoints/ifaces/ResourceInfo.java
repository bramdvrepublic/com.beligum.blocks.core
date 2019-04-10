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
//package com.beligum.blocks.endpoints.ifaces;
//
//import java.net.URI;
//import java.util.Locale;
//
///**
// * A general wrapper class for a resource that is returned from a JSON-endpoint, to be used (among others)
// * in the javascript while building the fact entries (and it's autocomplete functionality)
// *
// * Created by bram on 3/12/16.
// */
//public interface ResourceInfo
//{
//    /**
//     * The public ID of this resource
//     */
//    URI getResourceUri();
//
//    /**
//     * The curie URI type of this resource
//     */
//    URI getResourceType();
//
//    /**
//     * The label-value (eg. i18n caption-text) to render for this resource
//     */
//    String getLabel();
//
//    /**
//     * The hyperlink to attach to the label (may be null, then don't render a hyperlink, but plain text)
//     */
//    URI getLink();
//
//    /**
//     * Specifies if the link of getLink() (if any) is external (eg. should open in a new tab/window) or local to this site.
//     */
//    boolean isExternalLink();
//
//    /**
//     * The URL of the image-value of this resource or null if this value doesn't have an image. If not-null, this takes precedence over the label (which is attached to the alt attribute of this image)
//     */
//    URI getImage();
//
//    /**
//     * Unlike the label, this is the more 'official' name of this value; eg the name that will be placed in the autocomplete input box when we load the value back in
//     * For example: the label for "Brussels" might be "Brussels, capital of Belgium" and it's (Dutch) name could eg. be "Brussel"
//     */
//    String getName();
//
//    /**
//     * The language of this resource
//     */
//    Locale getLanguage();
//}
