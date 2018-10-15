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

package com.beligum.blocks.controllers;

import com.beligum.base.cache.CacheKey;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.index.entries.pages.IndexSearchResult;
import com.beligum.blocks.filesystem.index.entries.pages.PageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.LuceneQueryConnection;
import com.beligum.blocks.templating.blocks.DefaultTemplateController;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.TemplateCache;
import com.beligum.blocks.templating.blocks.TemplateConfig;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by bram on 6/6/16.
 */
public class BlocksReferenceController extends DefaultTemplateController
{
    //-----CONSTANTS-----
    public enum Type
    {
        TEMPLATE;

        /**
         * Same as valueOf(), but with html case converting
         */
        public static Type valueOfAttr(String value)
        {
            return valueOf(ATTR_ENUM_CONVERTER.convert(value));
        }
    }
    public enum TemplateRenderFilter
    {
        NONE,
        INLINE_STYLES,
        EXTERNAL_STYLES,
        STYLES,
        INLINE_SCRIPTS,
        EXTERNAL_SCRIPTS,
        SCRIPTS;

        /**
         * Same as valueOf(), but with html case converting
         */
        public static TemplateRenderFilter valueOfAttr(String value)
        {
            return valueOf(ATTR_ENUM_CONVERTER.convert(value));
        }
    }
    public static final String TAG_NAME = "blocks-reference";
    public static final String TYPE_ATTR = "data-type";
    public static final String ID_ATTR = "data-id";
    public static final String RENDER_FILTER_ATTR = "data-render-filter";

    private static final Converter<String, String> ATTR_ENUM_CONVERTER = CaseFormat.LOWER_HYPHEN.converterTo(CaseFormat.UPPER_UNDERSCORE);

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    @Override
    public void created()
    {
        Logger.info("");
    }

    //-----PUBLIC METHODS-----
    public Iterable<HtmlTemplate.ScopedResource> getResources()
    {
        Iterable<HtmlTemplate.ScopedResource> retVal = null;

//        String type = this.config.get(TYPE_ATTR);
//        if (!StringUtils.isEmpty(type)) {
//            HtmlTemplate template = TemplateCache.instance().getByTagName(tagName);
//            if (template != null) {
//                retVal = template.getExternalScriptElements();
//            }
//        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
