package com.beligum.blocks.models.url;


import com.beligum.base.models.BasicModelImpl;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.endpoints.ApplicationEndpoint;
import com.beligum.blocks.models.PageTemplate;
import com.beligum.blocks.models.StoredTemplate;
import com.beligum.blocks.models.jsonld.Resource;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import com.beligum.blocks.repositories.ResourceRepository;

import javax.persistence.*;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Path;

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

    public OkURL(URI url, URI view, URI resource, String language) {
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
    public Response response(String language) {

        Response retVal = null;
        URI resourceURI = null;
        if (this.resource != null) {
            resourceURI = UriBuilder.fromUri(this.getResourceUri().getAuthority()).path(this.getResourceUri().getPath().toString()).build();
        }

        // Find the view in the database
        Resource page = ResourceRepository.instance().findByURI(this.getViewUri(), language);

        StoredTemplate storedTemplate = null;
        Resource resource = null;
        if (page != null) {
            // create a new StoredTemplate and wrap the page from the database inside it.
            storedTemplate = new StoredTemplate();
            storedTemplate.wrap(page.unwrap());

            // Get the page template to render the page
            PageTemplate pageTemplate = Blocks.templateCache().getPageTemplate(storedTemplate.getPageTemplateName());
            BlocksTemplateRenderer renderer = Blocks.factory().createTemplateRenderer();

            // Todo render entity
            String renderedPage = renderer.render(pageTemplate, storedTemplate, resource, storedTemplate.getLanguage());
            retVal = Response.ok(renderedPage).build();

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
