package com.beligum.blocks.models.url;


import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.models.jsonld.interfaces.Resource;
import com.beligum.blocks.repositories.EntityRepository;

import javax.persistence.*;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.Locale;

/**
 * Created by wouter on 18/04/15.
 *
 * Shows the view/page for a clean url, with optionally a resource as argument
 */

@Entity
@DiscriminatorValue("OK")
public class OkURL extends BlocksURL
{
    private String view;
    private String resource;

    protected OkURL() {
        super();
    }

    public OkURL(URI url, URI view, URI resource, Locale language) {
        super(url, language);
        this.view = view.toString();
        if (resource != null) this.resource = resource.toString();
    }

    public URI getResourceUri()
    {
        URI retVal = null;
        if (resource != null) {
            retVal = UriBuilder.fromUri(resource).build();
        }
        return retVal;
    }

    public void setResourceUri(URI resourceId)
    {
        this.resource = resourceId.toString();
    }


    public URI getViewUri()
    {
        return UriBuilder.fromUri(view).build();
    }

    public void setViewUri(URI id)
    {
        this.view = id.toString();
    }

    @Override
    public Response response(Locale language) {

        Response retVal = null;
        URI resourceURI = null;
        if (this.resource != null) {
            resourceURI = UriBuilder.fromUri(this.getResourceUri()).build();
        }

        // Find the view in the database
        Resource page = EntityRepository.instance().findByURI(this.getViewUri(), language);

        Resource resource = null;
        if (resourceURI != null) {
            resource = EntityRepository.instance().findByURI(this.getResourceUri(), language);
            if (!Blocks.config().getDefaultLanguage().equals(language)) {
                Resource defaultResource = EntityRepository.instance().findByURI(this.getResourceUri(), language);
                defaultResource.merge(resource);
                resource = defaultResource;
            }
        }

//        StoredTemplate storedTemplate = null;

        if (page != null) {
            // create a new StoredTemplate and wrap the page from the database inside it.
//            storedTemplate = new StoredTemplate();
//            storedTemplate.wrap(page.unwrap());
//
//            // Get the page template to render the page
//            PageTemplate pageTemplate = Blocks.templateCache().getPageTemplate(storedTemplate.getPageTemplateName());
//            BlocksTemplateRenderer renderer = Blocks.factory().createTemplateRenderer();

            // Todo render entity
//            String renderedPage = renderer.render(pageTemplate, storedTemplate, resource, storedTemplate.getLanguage());
//            retVal = Response.ok(renderedPage).build();

        } else {
            throw new NotFoundException();
        }
        return retVal;


    }

    @Override
    public int statusCode() {
        return 200;
    }



}
