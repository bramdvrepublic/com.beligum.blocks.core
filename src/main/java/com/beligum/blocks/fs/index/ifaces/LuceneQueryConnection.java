package com.beligum.blocks.fs.index.ifaces;

import com.beligum.blocks.fs.index.entries.IndexEntry;
import com.beligum.blocks.fs.index.entries.pages.PageIndexEntry;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;

import java.io.IOException;
import java.util.List;

/**
 * Created by bram on 4/7/16.
 */
public interface LuceneQueryConnection<T extends PageIndexEntry> extends QueryConnection<T>
{
    //-----CONSTANTS-----
    class FieldQuery
    {
        public enum Type
        {
            /**
             * Search for the exact value in the query, nothing more, nothing less
             */
            EXACT,
            /**
             * Search for the value in the query, analyzed (eg. chopped into pieces on whitespace) and wildcarded.
             * Eg. a search for "Top Gu" on 'title' will result in this query "title:top title:gu*"
             */
            WILDCARD,
            /**
             * More or less the same as WILDCARD, but the query string won't be tokenized.
             * Eg. a search for "Top Gu" on 'title' will result in this query "title:top\ gu*"
             * Will result in less (but more correct) matches than WILDCARD.
             */
            WILDCARD_COMPLEX
        }

        private IndexEntry.IndexEntryField field;
        private String query;
        private BooleanClause.Occur bool;
        private Type type;
        private Integer group;

        public FieldQuery(IndexEntry.IndexEntryField field, String query, BooleanClause.Occur bool, Type type)
        {
            this(field, query, bool, type, null);
        }
        public FieldQuery(IndexEntry.IndexEntryField field, String query, BooleanClause.Occur bool, Type type, Integer group)
        {
            this.field = field;
            this.query = query;
            this.bool = bool;
            this.type = type;
            this.group = group;
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
        public Integer getGroup()
        {
            return group;
        }
    }

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Search each field on its corresponding query, indicating of it's a AND (true) or OR (false) query, returning the maxResults best results
     */
    List<T> search(FieldQuery[] fieldQueries, int maxResults) throws IOException;

    /**
     * Search for the low-level lucene query, returning the maxResults best results
     */
    List<T> search(Query luceneQuery, int maxResults) throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
