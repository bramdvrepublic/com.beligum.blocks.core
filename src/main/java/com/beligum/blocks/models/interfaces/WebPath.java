package com.beligum.blocks.models.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.URI;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Created by wouter on 28/05/15.
 */
public interface WebPath
{
    public final Integer NOT_FOUND = 404;
    public final Integer REDIRECT = 303;
    public final Integer OK = 200;

    public String getDBid();

    public URI getBlockId();

    public Locale getLanguage();

    public Path getUrl();

    public Path getLocalizedUrl();

    public int getStatusCode();

    public void setPageOk(URI pageUrl);

    public void setPageRedirect(URI pageUrl);

    public void setPageNotFound();

    public boolean isNotFound();

    public boolean isPage();

    public boolean isRedirect();

    public String toJson() throws JsonProcessingException;

}
