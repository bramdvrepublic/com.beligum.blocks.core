package com.beligum.blocks.repositories;

import com.beligum.base.server.RequestContext;
import com.beligum.blocks.models.ResourceContext;
import com.beligum.blocks.models.jsonld.interfaces.Resource;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by wouter on 2/05/15.
 */
public class EntityRepository
{
    private static EntityRepository instance;

    private EntityRepository() {

    }

    public static EntityRepository instance() {
        if (EntityRepository.instance == null) {
            EntityRepository.instance = new EntityRepository();
        }
        return EntityRepository.instance;
    }

    public Resource findByURI(URI uri, Locale language) {
        Resource retVal = null;
        List<Resource> resources =  RequestContext.getEntityManager().createQuery("SELECT rc.data FROM ResourceContext as rc WHERE rc.blockId = :id AND rc.language = :language",
                                                                                  Resource.class).setParameter("id", uri.toString()).setParameter("language", language.getLanguage()).getResultList();
        if (resources.size() > 0) {
            retVal = resources.get(0);
        }
        return retVal;
    }

    public ResourceContext findContextByURI(URI uri, Locale language) {
        ResourceContext retVal = null;
        List<ResourceContext> resources =  RequestContext.getEntityManager().createQuery("FROM ResourceContext as rc WHERE rc.blockId = :id AND rc.language = :language",
                                                                                  ResourceContext.class).setParameter("id", uri.toString()).setParameter("language", language.getLanguage()).getResultList();
        if (resources.size() > 0) {
            retVal = resources.get(0);
        }
        return retVal;
    }

    public void save(Resource resource, Locale language) throws Exception
    {
        if (resource.getBlockId() == null) {
            throw new Exception("Could not save resource without id");
        }

        ResourceContext context = findContextByURI(UriBuilder.fromUri(resource.getBlockId()).build(), language);
        if (context != null) {
            context.setResource(resource);
            RequestContext.getEntityManager().merge(context);
        } else {
            context = new ResourceContext(resource, language);
            RequestContext.getEntityManager().persist(context);
        }


    }


    public List<Resource> getDocs(ArrayList<String> ids, Locale language) {
        List<Resource> retVal = new ArrayList<Resource>();

        retVal =  RequestContext.getEntityManager().createQuery("SELECT rc.data FROM ResourceContext as rc WHERE rc.blockId IN :ids AND rc.language = :language",
                                                                                  Resource.class).setParameter("ids", ids).setParameter("language", language.getLanguage()).getResultList();
        return retVal;
    }

    public void createResource() {
        // Create main resource with meta-data
        // add link with translation (english, dutch)
        // create Translation
        //

    }
}
