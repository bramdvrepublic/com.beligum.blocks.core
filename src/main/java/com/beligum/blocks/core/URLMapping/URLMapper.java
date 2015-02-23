package com.beligum.blocks.core.URLMapping;

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

    public void addUrl(URL url, String language, BlocksID id);

    public void addTranslation(Path path, String language);
}
