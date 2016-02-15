package com.beligum.blocks.fs.pages;

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.ifaces.PathInfo;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Source;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;

import java.io.IOException;
import java.net.URI;

/**
 * Created by bram on 1/27/16.
 */
public abstract class AbstractPage implements Page
{
    //-----CONSTANTS-----
    private static final URI ROOT = URI.create("/");
    //Note: don't change this or the entire DB will be corrupt
    private static final String DIR_PAGE_NAME = "index";

    //-----VARIABLES-----
    protected final PathInfo pathInfo;
    private Model rdfModel;
    private Source source;

    //-----CONSTRUCTORS-----
    public static URI create(URI uri, URI baseUri) throws IOException
    {
        Settings settings = Settings.instance();

        //note: the toString is mandatory, otherwise the Path creation fails because there's no scheme
        //note2: second one is for security
        String relativeUrlStr = uri.getPath();
        URI relativeUrl = URI.create(relativeUrlStr);
        //note: we need to make it relative to match the one below
        relativeUrl = ROOT.relativize(relativeUrl);
        URI relativeUrlTest = settings.getSiteDomain().relativize(uri);
        if (!relativeUrl.equals(relativeUrlTest)) {
            throw new SecurityException("Trying to create a page path from outside the domain (" + settings.getSiteDomain() + "), can't proceed; " + uri);
        }

        //note: we normalize before resolving for safety
        URI tempPagePath = baseUri.resolve(relativeUrl.normalize());

        //this is important: if the url ends with a slash, we're actually saving a 'directory', so it doesn't have a name (will become 'index' later on)
        String pageName = null;
        boolean isDir = relativeUrlStr.endsWith("/");
        if (!isDir) {
            pageName = FilenameUtils.getName(relativeUrlStr);
        }

        //this means we're dealing with a directory, not a file
        if (pageName == null) {
            pageName = DIR_PAGE_NAME;
        }
        else if (pageName.equals(DIR_PAGE_NAME)) {
            throw new IOException("You can't create a file with the same name of the directory filename store. Choose any other name, but not this one; " + pageName);
        }

        String ext = settings.getPagesFileExtension();
        if (!StringUtils.isEmpty(ext) && !pageName.endsWith(ext)) {
            pageName += ext;
        }

        //re-map to the final filename
        if (!isDir) {
            //this creates the 'parent' uri, so we have uniform code in the return statement using the pageName variable
            tempPagePath = tempPagePath.resolve(".").normalize();
        }

        return tempPagePath.resolve(pageName).normalize();
    }
    protected AbstractPage(PathInfo pathInfo)
    {
        this.pathInfo = pathInfo;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getUri()
    {
        return this.pathInfo == null ? null : this.pathInfo.getUri();
    }
    @Override
    public Model getRDFModel()
    {
        return this.rdfModel;
    }
    @Override
    public Source getSource()
    {
        return this.source;
    }
    @Override
    public PathInfo getPathInfo()
    {
        return pathInfo;
    }

    //-----PROTECTED METHODS-----
    //this should be set from a package private class
    protected void setRDFModel(Model rdfModel)
    {
        this.rdfModel = rdfModel;
    }
    //this should be set from a package private class
    protected void setSource(Source source)
    {
        this.source = source;
    }

    //-----PRIVATE METHODS-----

}
