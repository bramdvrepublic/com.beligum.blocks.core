package com.beligum.blocks.fs.pages;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.ifaces.ResourcePath;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.sources.HtmlSource;
import com.beligum.blocks.rdf.sources.HtmlStreamSource;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
    protected final ResourcePath resourcePath;
    protected URI publicAddress;
    private boolean checkedAddress;

    //-----CONSTRUCTORS-----
    /**
     * This converts a public page URI to it's local resource counterpart.
     *
     * @param uri     the public uri for a page
     * @param baseUri the base uri of the page store, usually one of Settings.instance().getPagesStorePath() or Settings.instance().getPagesViewPath()
     * @return the URI of the resource that holds the data for that page in our configured server filesystem
     * @throws IOException
     */
    public static URI toResourceUri(URI uri, URI baseUri) throws IOException
    {
        URI retVal = null;

        if (uri != null) {
            Settings settings = Settings.instance();

            //if the uri is absolute (starts with http://...), check if the domain (authority = domain+port) matches,
            // otherwise (eg. when the URI is relative (eg. /resource/...) the authority will be null (and that's ok)
            if (uri.getAuthority()!=null && !uri.getAuthority().equals(settings.getSiteDomain().getAuthority())) {
                throw new SecurityException("Trying to create a page path from outside the domain (" + settings.getSiteDomain() + "), can't proceed; " + uri);
            }

            String relativeUrlStr = uri.getPath();
            String lang = getQueryParamLanguage(uri);
            // we map the "lang" query param to the beginning of the URL by appending its value as a prefix to the path
            // this way, we (locally) uniformize the "/en/page" path to the "/page?lang=en" path
            if (!StringUtils.isEmpty(lang)) {
                relativeUrlStr = "/"+lang+relativeUrlStr;
            }
            URI relativeUrl = URI.create(relativeUrlStr);
            //note: since we'll reuse the path below (under a different root path), we must make them relative (or they'll resolve to the real filesystem root later on instead of the chroot)
            relativeUrl = ROOT.relativize(relativeUrl);

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

            retVal = tempPagePath.resolve(pageName).normalize();
        }

        return retVal;
    }
    protected AbstractPage(ResourcePath resourcePath)
    {
        this.resourcePath = resourcePath;
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI buildAbsoluteAddress() throws IOException
    {
        if (!this.checkedAddress) {
            URI resourceUri = this.getResourcePath().getLocalPath().toUri();

            Settings settings = Settings.instance();
            URI fsUri = null;
            if (resourceUri.getScheme().equals(settings.getPagesViewPath().getScheme())) {
                fsUri = settings.getPagesViewPath();
            }
            else if (resourceUri.getScheme().equals(settings.getPagesStorePath().getScheme())) {
                fsUri = settings.getPagesStorePath();
            }
            else {
                throw new IOException("Unknown filesystem schema found in local page resource URI; " + resourceUri);
            }

            UriBuilder builder = UriBuilder.fromUri(settings.getSiteDomain().resolve(fsUri.relativize(resourceUri)));
            String pathStr = builder.build().getPath();
            if (pathStr.endsWith(settings.getPagesFileExtension())) {
                pathStr = pathStr.substring(0, pathStr.length() - settings.getPagesFileExtension().length());
                builder.replacePath(pathStr);
            }
            if (pathStr.endsWith(DIR_PAGE_NAME)) {
                pathStr = pathStr.substring(0, pathStr.length() - DIR_PAGE_NAME.length());
                builder.replacePath(pathStr);
            }
            //we save resources with a language-prefixed path just like all the other pages,
            // but use the 'lang' query parameter in public form, because it reflects the nature
            // of a resource url better, so if the second path-part (the first in public form) is a resource string,
            // we're dealing with a resource page and need to convert it back to it's public form
            Path path = Paths.get(pathStr);
            if (path.getNameCount()>1) {
                //note that subpath chops off the leading slash, so make sure we add it again before comparing to the endpoint string
                String noLangPath = "/" + path.subpath(1, path.getNameCount()).toString();
                if (noLangPath.startsWith(ParserConstants.RESOURCE_ENDPOINT)) {
                    builder.replacePath(noLangPath);
                    builder.queryParam(I18nFactory.LANG_QUERY_PARAM, path.getName(0).toString());
                }
            }

            this.publicAddress = builder.build();
            this.checkedAddress = true;
        }

        return this.publicAddress;
    }
    @Override
    public URI buildRelativeAddress() throws IOException
    {
        //note that buildAbsoluteAddress() uses the siteDomain setting to generate it's absolute URL,
        //the first resolve makes sure it always starts with a slash
        return URI.create("/").resolve(Settings.instance().getSiteDomain().relativize(this.buildAbsoluteAddress()));
    }
    @Override
    public ResourcePath getResourcePath()
    {
        return resourcePath;
    }
    @Override
    public HtmlSource readOriginalHtml() throws IOException
    {
        try (InputStream is = this.getResourcePath().getFileContext().open(this.getResourcePath().getLocalPath())) {
            return new HtmlStreamSource(this.buildAbsoluteAddress(), is);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private static String getQueryParamLanguage(URI uri)
    {
        String retVal = null;

        MultivaluedMap<String, String> query = StringFunctions.getQueryParameters(uri);
        List<String> queryLang = query.get(I18nFactory.LANG_QUERY_PARAM);
        if (queryLang!=null && !queryLang.isEmpty()) {
            retVal = queryLang.iterator().next();
        }

        return retVal;
    }
}
