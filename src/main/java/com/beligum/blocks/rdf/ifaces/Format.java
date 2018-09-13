/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.rdf.ifaces;

import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.MimeType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 2/20/16.
 */
public enum Format
{
    //-----CONSTANTS-----
    RDFA(MimeTypes.HTML, MimeTypes.HTML),
    JSONLD(MimeTypes.JSONLD, MimeTypes.JSON),
    RDF_XML(MimeTypes.RDF_XML, MimeTypes.XML),
    NTRIPLES(MimeTypes.NTRIPLES, MimeTypes.PLAINTEXT),
    TURTLE(MimeTypes.TURTLE, MimeTypes.PLAINTEXT),
    N3(MimeTypes.N3, MimeTypes.PLAINTEXT),
    ;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    /**
     * This is the mime type that designates this RDF format
     */
    private final MimeType mimeType;

    /**
     * This is the mime type that the real serialization of data uses
     */
    private final MimeType contentType;

    private static Map<String, Format> mimeTypeMap;

    Format(MimeType mimeType, MimeType contentType)
    {
        this.mimeType = mimeType;
        this.contentType = contentType;
    }

    //-----PUBLIC METHODS-----
    public MimeType getMimeType()
    {
        return mimeType;
    }
    public MimeType getContentType()
    {
        return contentType;
    }
    public static Format fromMimeType(String mimeType)
    {
        if (mimeTypeMap == null) {
            mimeTypeMap = new HashMap<>();
            for (Format f : Format.values()) {
                mimeTypeMap.put(f.getMimeType().toString(), f);
            }
        }

        return mimeTypeMap.get(mimeType);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
