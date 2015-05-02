package com.beligum.blocks.repositories;

import com.beligum.base.server.RequestContext;
import com.beligum.blocks.models.ResourceContext;
import com.beligum.blocks.models.jsonld.Resource;

import java.net.URI;
import java.util.List;

/**
 * Created by wouter on 2/05/15.
 */
public class ResourceRepository
{
    private static ResourceRepository instance;

    private ResourceRepository() {

    }

    public static ResourceRepository instance() {
        if (ResourceRepository.instance == null) {
            ResourceRepository.instance = new ResourceRepository();
        }
        return ResourceRepository.instance;
    }

    public Resource findByURI(URI uri, String language) {
        Resource retVal = null;
        List<Resource> resources =  RequestContext.getEntityManager().createQuery("SELECT rc.data FROM ResourceContext as rc WHERE rc.blockId = :id AND rc.language = :language",
                                                                                  Resource.class).setParameter("id", uri.toString()).setParameter("language", language).getResultList();
        if (resources.size() > 0) {
            retVal = resources.get(0);
        }
        return retVal;
    }
}
