/*
 * Copyright 2018 Republic of Reinvention bvba. All Rights Reserved.
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

package com.beligum.blocks.filesystem.index.lucene;

import com.beligum.blocks.filesystem.index.ifaces.RdfIndexer;
import org.apache.lucene.document.*;
import org.apache.solr.legacy.LegacyDoubleField;
import org.apache.solr.legacy.LegacyFloatField;
import org.apache.solr.legacy.LegacyIntField;
import org.apache.solr.legacy.LegacyLongField;

public class LuceneRdfIndexer implements RdfIndexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Document document;

    //-----CONSTRUCTORS-----
    public LuceneRdfIndexer(Document document)
    {
        this.document = document;
    }

    //-----PUBLIC METHODS-----
    @Override
    public void indexIntegerField(String fieldName, int value)
    {
        this.document.add(new LegacyIntField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexLongField(String fieldName, long value)
    {
        this.document.add(new LegacyLongField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexFloatField(String fieldName, float value)
    {
        this.document.add(new LegacyFloatField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexDoubleField(String fieldName, double value)
    {
        this.document.add(new LegacyDoubleField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexStringField(String fieldName, String value)
    {
        this.document.add(new TextField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexConstantField(String fieldName, String value)
    {
        this.document.add(new StringField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
