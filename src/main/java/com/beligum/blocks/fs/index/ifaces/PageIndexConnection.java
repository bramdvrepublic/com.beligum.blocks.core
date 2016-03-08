package com.beligum.blocks.fs.index.ifaces;

import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.fs.index.entries.PageIndexEntry;
import com.beligum.blocks.fs.pages.ifaces.Page;
import org.apache.lucene.search.BooleanClause;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Created by bram on 2/21/16.
 */
public interface PageIndexConnection<T extends PageIndexEntry> extends IndexConnection<T>
{
    //-----CONSTANTS-----
    class FieldQuery
    {
        public enum Type
        {
            EXACT,
            WILDCARD
        }

        private IndexEntry.IndexEntryField field;
        private String query;
        private BooleanClause.Occur bool;
        private Type type;

        public FieldQuery(IndexEntry.IndexEntryField field, String query, BooleanClause.Occur bool, Type type)
        {
            this.field = field;
            this.query = query;
            this.bool = bool;
            this.type = type;
        }
        public IndexEntry.IndexEntryField getField()
        {
            return field;
        }
        public String getQuery()
        {
            return query;
        }
        public BooleanClause.Occur getBool()
        {
            return bool;
        }
        public Type getType()
        {
            return type;
        }
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    T get(URI key) throws IOException;
    void delete(Page page) throws IOException;
    void update(Page page) throws IOException;

    /**
     * Search each field on its corresponding query, indicating of it's a AND (true) or OR (false) query, returning the maxResults best results
     */
    List<T> search(FieldQuery[] fieldQueries, int maxResults) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
