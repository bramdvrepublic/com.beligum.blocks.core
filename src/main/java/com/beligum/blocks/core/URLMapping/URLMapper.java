package com.beligum.blocks.core.URLMapping;

import com.beligum.blocks.core.exceptions.LanguageException;
import com.beligum.blocks.core.identifiers.BlocksID;

import java.net.URL;

/**
 * Created by bas on 23.02.15.
 */
public interface URLMapper
{
    public BlocksID getId(URL url);

    public URL getTranslation(URL url, String language);

    public URL getUrl(BlocksID id);

    /**
     * Add a url-id pair to the mapping
     * @param languagedUrl an url holding language-information
     * @param id
     * @throws LanguageException
     */
    public void add(URL languagedUrl, BlocksID id) throws LanguageException;

    public void addTranslation(URL url, String language);
}
