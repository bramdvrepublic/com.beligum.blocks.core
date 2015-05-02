package com.beligum.blocks.repositories;

import com.beligum.base.server.RequestContext;
import com.beligum.blocks.models.url.BlocksURL;
import sun.jvm.hotspot.opto.Block;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

/**
 * Created by wouter on 30/04/15.
 *
 * This class fetches urls from the routing table. The way you query the table you can decide how the routing should work.
 * Probably this class you should be able to override/inject this class with an other from an other module.
 *
 */
public class UrlRepository
{
    private static UrlRepository instance;

    private UrlRepository() {

    }

    public static UrlRepository instance() {
        if (UrlRepository.instance == null) {
            UrlRepository.instance = new UrlRepository();
        }
        return UrlRepository.instance;
    }

    public BlocksURL getUrlForURI(String domain, String path, String language) {
        BlocksURL retVal = null;
        List<BlocksURL> siteUrls = RequestContext.getEntityManager().createQuery("FROM BlocksURL u WHERE u.path = :path AND u.language = :language AND u.deleted = false", BlocksURL.class).setParameter("path", path).setParameter("language", language).getResultList();
        if (siteUrls != null && siteUrls.size() > 0) {
            retVal = siteUrls.get(0);
        }
        return retVal;
    }

    public String getUrlForID(String id) {
        String retVal = null;
        List<BlocksURL> urls = RequestContext.getEntityManager().createQuery("FROM BlocksURL url WHERE url.resource = :id", BlocksURL.class).setParameter("id", id).getResultList();
        if (urls.size() == 0) {
//            retVal = urls.get(0).getResource();
        }
        return retVal;
    }

    public BlocksURL getId(String url) {
        BlocksURL retVal = null;
//        List<BlocksURL> urls = RequestContext.getEntityManager().createQuery("SELECT BlocksURL url FROM BlocksURL WHERE url.resource = :id", BlocksURL.class).setParameter("id", id).getResultList();
//        if (urls.size() == 0) {
//            retVal = urls.get(0).getResourceId();
//        }
        return retVal;
    }

}
