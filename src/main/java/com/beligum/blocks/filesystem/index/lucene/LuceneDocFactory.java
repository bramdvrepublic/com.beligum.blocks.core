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

import com.beligum.base.utils.Logger;
import com.beligum.blocks.endpoints.ifaces.RdfQueryEndpoint;
import com.beligum.blocks.filesystem.index.entries.pages.SimplePageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.IndexEntryField;
import com.beligum.blocks.filesystem.index.ifaces.PageIndexEntry;
import com.beligum.blocks.filesystem.index.ifaces.RdfIndexer;
import com.beligum.blocks.filesystem.pages.PageModel;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.utils.RdfTools;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.protobuf.ProtobufMapper;
import com.fasterxml.jackson.dataformat.protobuf.schema.NativeProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchema;
import com.fasterxml.jackson.dataformat.protobuf.schema.ProtobufSchemaLoader;
import com.fasterxml.jackson.dataformat.protobuf.schemagen.ProtobufSchemaGenerator;
import com.google.common.base.Joiner;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.eclipse.rdf4j.model.Statement;

import java.io.IOException;
import java.net.URI;
import java.util.*;

import static org.apache.lucene.util.ByteBlockPool.BYTE_BLOCK_SIZE;

public class LuceneDocFactory
{
    //-----CONSTANTS-----
    public static LuceneDocFactory INSTANCE = new LuceneDocFactory();

    //interesting alternative guide if you ever need it: http://www.citrine.io/blog/2015/2/14/building-a-custom-analyzer-in-lucene
    private static Analyzer SORTFIELD_ANALYZER;

    static {
        try {
            /**
             * This seems to do a pretty good job at normalizing the sort values:
             * - first, split on whitespace (which will later be joined again, see preprocessSortValue() and SORTFIELD_ANALYZER_JOIN_CHAR)
             * - convert all to regular ASCII characters
             * - make all lowercase
             */
            SORTFIELD_ANALYZER = CustomAnalyzer.builder()
                                               //difference between these two is that 'standard' also strips punctuation characters like (),' etc.
                                               //.withTokenizer("whitespace")
                                               .withTokenizer("standard")

                                               .addTokenFilter("asciifolding", "preserveOriginal", "false")
                                               .addTokenFilter("lowercase")
                                               .build();
        }
        catch (Exception e) {
            Logger.error("Error while building sortField analyzer, this shouldn't happen; ", e);
        }
    }

    //-----VARIABLES-----
    private final ProtobufSchema protobufSchema;
    private final ObjectMapper objectMapper;

    //-----CONSTRUCTORS-----
    private LuceneDocFactory()
    {
        try {
            //Note: order is important since createProtobufSchema() needs the objectMapper
            objectMapper = new ProtobufMapper();
            protobufSchema = ProtobufSchemaLoader.std.parse(this.createProtobufSchema(SimplePageIndexEntry.class));
        }
        catch (Exception e) {
            throw new RuntimeException("Error while initializing the Lucene document factory; this shouldn't happen", e);
        }
    }

    //-----PUBLIC METHODS-----
    public Term toLuceneId(String id)
    {
        return new Term(IndexEntry.id.getName(), id);
    }
    /**
     * This method deserializes the binary object stream data of a Lucene entry back to an instance of this class
     */
    public PageIndexEntry fromLuceneDoc(Document document) throws IOException
    {
        return getProtobufMapper().readerFor(SimplePageIndexEntry.class).with(getProtobufSchema()).readValue(document.getBinaryValue(PageIndexEntry.object.getName()).bytes);
    }
    /**
     * This method converts this IndexEntry instance to a Lucene document (and ID)
     * Note: never serialize this to the protobuf stored field
     * Note 2: we updated the return value to map out the resource URI to document,
     * instead of a single Document to facilitate the new inline objects implementation
     * because pages can produce multiple (new) resources now.
     */
    public Document toLuceneDoc(PageIndexEntry indexEntry) throws IOException
    {
        Document retVal = new Document();

        //note: StringField = un-analyzed + indexed
        //      TextField = standard analyzed + indexed
        //      StoredField = not indexed at all

        //Note: we also need to insert the id of the doc even though it's an index
        retVal.add(new StringField(IndexEntry.id.getName(), indexEntry.getId(), org.apache.lucene.document.Field.Store.NO));

        //don't store it, we just add it to the index to be able to query the URI (again) more naturally
        retVal.add(new TextField(IndexEntry.tokenisedId.getName(), indexEntry.getId(), org.apache.lucene.document.Field.Store.NO));

        retVal.add(new StringField(PageIndexEntry.parentId.getName(),
                                   indexEntry.getParentId() == null ? IndexEntryField.NULL_VALUE : indexEntry.getParentId(),
                                   org.apache.lucene.document.Field.Store.NO));

        retVal.add(new StringField(PageIndexEntry.resource.getName(),
                                   indexEntry.getResource() == null ? IndexEntryField.NULL_VALUE : indexEntry.getResource(),
                                   org.apache.lucene.document.Field.Store.NO));

        retVal.add(new StringField(PageIndexEntry.typeOf.getName(),
                                   indexEntry.getTypeOf() == null ? IndexEntryField.NULL_VALUE : indexEntry.getTypeOf(),
                                   org.apache.lucene.document.Field.Store.NO));

        retVal.add(new TextField(IndexEntry.label.getName(),
                                 indexEntry.getLabel() == null ? IndexEntryField.NULL_VALUE : indexEntry.getLabel(),
                                 org.apache.lucene.document.Field.Store.NO));

        retVal.add(new StringField(PageIndexEntry.language.getName(),
                                   indexEntry.getLanguage() == null ? IndexEntryField.NULL_VALUE : indexEntry.getLanguage(),
                                   org.apache.lucene.document.Field.Store.NO));

        retVal.add(new StringField(PageIndexEntry.canonicalAddress.getName(),
                                   indexEntry.getCanonicalAddress() == null ? IndexEntryField.NULL_VALUE : indexEntry.getCanonicalAddress(),
                                   org.apache.lucene.document.Field.Store.NO));

        retVal.add(new TextField(IndexEntry.description.getName(),
                                 indexEntry.getDescription() == null ? IndexEntryField.NULL_VALUE : indexEntry.getDescription(),
                                 org.apache.lucene.document.Field.Store.NO));

        retVal.add(new StringField(IndexEntry.image.getName(),
                                   indexEntry.getImage() == null ? IndexEntryField.NULL_VALUE : indexEntry.getImage(),
                                   org.apache.lucene.document.Field.Store.NO));

        //stores this entire object in the index (using Protocol Buffers)
        //see https://github.com/FasterXML/jackson-dataformats-binary/tree/master/protobuf
        byte[] serializedObject = getProtobufMapper().writer(getProtobufSchema()).writeValueAsBytes(indexEntry);
        retVal.add(new StoredField(PageIndexEntry.object.getName(), serializedObject));
        //this is the old JSON-alternative
        //retVal.add(new StoredField(PageIndexEntry.object.name(), Json.write(indexEntry)));

        return retVal;
    }
    /**
     * Adds the RDF statements in the model to the lucene document, next to some general sort and search-all fields.
     */
    public Document indexRdfModel(Document document, PageModel subModel, RdfQueryEndpoint.SearchOption... options) throws IOException
    {
        //makes sense to eliminate double values for the sort list, I think
        Map<RdfProperty, Set<String>> sortFieldMapping = new LinkedHashMap<>();

        RdfIndexer rdfIndexer = new LuceneRdfIndexer(document);

        for (Statement stmt : subModel.getSubModel()) {

            URI predicateCurie = RdfTools.fullToCurie(URI.create(stmt.getPredicate().toString()));
            if (predicateCurie != null) {
                RdfProperty predicate = RdfFactory.getProperty(predicateCurie);
                if (predicate != null) {

                    //This performs the main indexing operation:
                    // ask the RDF property to index itself to the lucene index
                    RdfIndexer.IndexResult value = predicate.indexValue(rdfIndexer, subModel.getSubResource(), stmt.getObject(), subModel.getPage().getLanguage(), options);

                    //index it with the default analyzer so we can search it lowercase, without punctuation, etc...
                    rdfIndexer.indexStringField(LucenePageIndexer.CUSTOM_FIELD_ALL, value.stringValue);

                    //also index the raw value to the constant all field
                    rdfIndexer.indexConstantField(LucenePageIndexer.CUSTOM_FIELD_ALL_VERBATIM, value.indexValue.toString());

                    Set<String> sortField = sortFieldMapping.get(predicate);
                    if (sortField == null) {
                        sortFieldMapping.put(predicate, sortField = new LinkedHashSet<>());
                    }
                    //makes sense to sort on the human-readable value (eg. 'Belgium' instead of '/resource/Country/1938216')
                    String sortValue = value.stringValue;
                    //this was introduced after experiencing a "java.lang.IllegalArgumentException: DocValuesField "mot:text" is too large, must be <= 32766"
                    //see https://issues.apache.org/jira/browse/LUCENE-4583
                    //Makes sense to crop this to a reasonable value since sorting on thousand-characters-long sort values is't really a valid use case, right?
                    final int MAX_SORT_VALUE_LENGTH = 128;
                    if (sortValue.length() > MAX_SORT_VALUE_LENGTH) {
                        sortValue = sortValue.substring(0, MAX_SORT_VALUE_LENGTH);
                    }
                    sortField.add(sortValue);
                }
                else {
                    Logger.error("Encountered unknown RDF predicate (" + predicateCurie + "); this probably means something is wrong or something won't get indexed; " + stmt);
                }
            }
            else {
                Logger.error("Unable to build RDF curie from predicate (" + stmt.getPredicate() + "); this probably means something is wrong or something won't get indexed; " + stmt);
            }
        }

        //TODO this is a (shitty) temp workaround to solve Lucene's IllegalArgumentException when trying to index large text chunks.
        //According to http://stackoverflow.com/questions/24019868/utf8-encoding-is-longer-than-the-max-length-32766
        // we should index these fields (especially _all) with a different analyzer...
        Iterator<IndexableField> fieldIter = document.iterator();
        //found this here: SortedDocValuesWriter.addValue()
        final int MAX_FIELD_SIZE = BYTE_BLOCK_SIZE - 2;
        while (fieldIter.hasNext()) {
            IndexableField docField = fieldIter.next();
            String stringVal = docField.stringValue();
            //non-string values are probably not too large?
            if (stringVal != null && stringVal.length() > MAX_FIELD_SIZE) {
                Logger.warn("Removing field '" + docField.name() + "' from the lucene document we're indexing because it's too large. Value is " + stringVal.length() + " and starts with '" +
                            stringVal.substring(0, 20) + "...' (max " + MAX_FIELD_SIZE + " bytes allowed). Note that this means the content of this value won't be searchable (!)");
                fieldIter.remove();
            }
        }

        //details on DocValues,
        // see https://www.norconex.com/facets-with-lucene/
        //See this discussion for background on multivalues
        // see http://stackoverflow.com/questions/21002042/adding-a-multi-valued-string-field-to-a-lucene-document-do-commas-matter
        for (Map.Entry<RdfProperty, Set<String>> e : sortFieldMapping.entrySet()) {

            RdfProperty predicate = e.getKey();
            String key = predicate.getCurieName().toString();

            //this will allow us to sort on this field
            Set<String> sortField = sortFieldMapping.get(predicate);
            if (sortField != null && !sortField.isEmpty()) {
                String sortValue = preprocessSortValue(Joiner.on(LucenePageIndexer.DEFAULT_FIELD_JOINER).join(sortField));
                document.add(new SortedDocValuesField(key, new BytesRef(sortValue)));
            }

            //index all field names so we can search for documents that do/don't have a certain field set
            document.add(new StringField(LucenePageIndexer.CUSTOM_FIELD_FIELDS, key, org.apache.lucene.document.Field.Store.NO));
        }

        return document;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private ProtobufSchema getProtobufSchema()
    {
        return protobufSchema;
    }
    private ObjectMapper getProtobufMapper()
    {
        return objectMapper;
    }
    //see https://github.com/FasterXML/jackson-dataformats-binary/tree/master/protobuf
    private String createProtobufSchema(Class<?> clazz) throws JsonMappingException
    {
        ObjectMapper mapper = getProtobufMapper();
        ProtobufSchemaGenerator gen = new ProtobufSchemaGenerator();
        mapper.acceptJsonFormatVisitor(clazz, gen);
        ProtobufSchema schemaWrapper = gen.getGeneratedSchema();
        NativeProtobufSchema nativeProtobufSchema = schemaWrapper.getSource();

        return nativeProtobufSchema.toString();
    }

    //see https://mail-archives.apache.org/mod_mbox/lucene-solr-user/201507.mbox/%3CCALvb29w7A6TZVEtiYajDZBPGik8JjQd2tAyo2JTskVrR_JdsuQ@mail.gmail.com%3E
    // and https://examples.javacodegeeks.com/core-java/apache/lucene/lucene-indexing-example-2/
    private String preprocessSortValue(String value) throws IOException
    {
        String retVal = value;

        if (value != null) {
            StringBuilder sb = new StringBuilder();
            try (TokenStream ts = SORTFIELD_ANALYZER.tokenStream(null, value)) {
                CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
                ts.reset();
                while (ts.incrementToken()) {
                    if (sb.length() > 0) {
                        sb.append(LucenePageIndexer.DEFAULT_FIELD_JOINER);
                    }
                    sb.append(term.toString());
                }
                ts.end();
            }

            retVal = sb.toString().trim();
        }

        return retVal;
    }
}
