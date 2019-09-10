package com.beligum.blocks.index.results;

import com.beligum.base.resources.ifaces.ResourceAction;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Permissions;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import javax.ws.rs.core.UriBuilder;

/**
 * A predicate to be used as a filter for search results, mainly for security reasons.
 * Note that this solution is not all that great because it's a last-resort to filter out the pages
 * that "don't match" (mainly: are forbidden for the current user), but all other metadata on the iterator
 * (eg. the total hits in the IndexSearchResult) are not updated by this filter and so if results
 * are encountered that are filtered by this class, that number and the number of search results
 * will be off. In that regard, this class is only a last-minute band aid to mask out hidden pages (and their data).
 *
 * The correct way of doing things is to add the security information to the index-query request so non-compatible
 * results are filtered out in the request, not the result.
 *
 * Created by bram on Aug 19, 2019
 */
public class SearchResultFilter implements Predicate<ResourceIndexEntry>
{

    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public boolean apply(@Nullable ResourceIndexEntry input)
    {
        // for now, we only check for forbidden entries, but we might add more (non-security related) filters in the future
        return input != null && input.isPermitted(ResourceAction.READ);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
