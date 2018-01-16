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

package com.beligum.blocks.filesystem.index.entries.pages;

import com.beligum.blocks.filesystem.index.entries.RdfIndexer;
import org.apache.lucene.document.*;

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
        this.document.add(new IntField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexLongField(String fieldName, long value)
    {
        this.document.add(new LongField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexFloatField(String fieldName, float value)
    {
        this.document.add(new FloatField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
    }
    @Override
    public void indexDoubleField(String fieldName, double value)
    {
        this.document.add(new DoubleField(fieldName, value, org.apache.lucene.document.Field.Store.NO));
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
