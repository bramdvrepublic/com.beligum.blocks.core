package com.beligum.blocks.models.url;

import com.beligum.base.models.BasicModelImpl;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.nio.file.Path;

/**
 * Created by wouter on 30/04/15.
 */
@Entity
@DiscriminatorValue(value = "MOVED")
public class MovedPermanentlyURL extends BlocksURL
{

    protected MovedPermanentlyURL() {

    }

    public MovedPermanentlyURL(URI url, URI newUrl, String language) {
        super(url, language);
    }

    @Override
    public Response response(String language)
    {
        return null;
    }
    @Override
    public int statusCode()
    {
        return 301;
    }

}
