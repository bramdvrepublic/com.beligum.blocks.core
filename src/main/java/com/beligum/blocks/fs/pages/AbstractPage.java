package com.beligum.blocks.fs.pages;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.resources.ResourceRequestImpl;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.server.R;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.HdfsResource;
import com.beligum.blocks.fs.ifaces.ResourcePath;
import com.beligum.blocks.fs.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.rdf.sources.HtmlSource;
import com.beligum.blocks.rdf.sources.HtmlStreamSource;
import com.beligum.blocks.rdf.sources.HtmlStringSource;
import com.beligum.blocks.utils.RdfTools;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.Path;
import org.openrdf.model.Model;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by bram on 1/27/16.
 */
public abstract class AbstractPage implements Page
{
    //-----CONSTANTS-----
    protected static final URI ROOT = URI.create("/");

    //Note: don't change this or the entire DB will be corrupt
    private static final String DIR_PAGE_NAME = "index";

    //-----VARIABLES-----
    protected Locale language;
    protected URI canonicalAddress;
    protected URI relativeStoragePath;
    protected ResourcePath resourcePath;
    protected URI cachedAbsoluteAddress;
    private boolean checkedAbsoluteAddress;
    protected URI cachedRelativeAddress;
    private boolean checkedRelativeAddress;

    //-----CONSTRUCTORS-----
    /**
     * Constructor that builds a page instance from a public request-URI,
     * handling all internal path specific translations
     */
    protected AbstractPage(URI publicUri) throws IOException
    {
        //First, preparse the uri

        //Note: this code is meant to be used with an absolute public uri, so make sure it's uniform
        if (!publicUri.isAbsolute()) {
            publicUri = R.configuration().getSiteDomain().resolve(publicUri);
        }

        //we start off by stripping (and extracting) the language parameters from the URI
        UriBuilder uriBuilder = UriBuilder.fromUri(publicUri);

        //note that this will be null if the uri doesn't contain any language; that's ok for now
        this.language = R.i18nFactory().getUrlLocale(publicUri, uriBuilder, null);

        //we strip off all extra parameters: for now we don't support query (or other) parameters, mainly because they don't map well to file/folder names
        uriBuilder = uriBuilder.replaceQuery(null);

        //this will be the normalized uri (cleaned and stripped from all language references; the main language is stored earlier)
        URI normalizedUri = uriBuilder.build();

        //if the uri is absolute (starts with http://...), check if the domain (authority = domain+port) matches,
        // otherwise (eg. when the URI is relative (eg. /resource/...) the authority will be null (and that's ok)
        if (normalizedUri.getAuthority()!=null && !normalizedUri.getAuthority().equals(R.configuration().getSiteDomain().getAuthority())) {
            throw new SecurityException("Trying to create a page path from outside the domain (" + R.configuration().getSiteDomain() + "), can't proceed; " + normalizedUri);
        }

        //Now, build the relative, normalized storage path of this page (where this page will be stored relative to the chroot)
        //Note that this path has a leading slash
        String relativeAddressPath = normalizedUri.getPath();

        //this is the 'most naked' address of the page, without language params or storage filenames
        this.canonicalAddress = URI.create(relativeAddressPath);

        //this is important: if the url ends with a slash or is just empty, we're actually saving a 'directory', so it doesn't have a name (will become 'index' later on)
        String pageName = null;
        boolean isDir = StringUtils.isEmpty(relativeAddressPath) || relativeAddressPath.endsWith("/");
        if (!isDir) {
            pageName = FilenameUtils.getName(relativeAddressPath);
        }
        //this means we're dealing with a directory, not a file
        if (pageName == null) {
            pageName = DIR_PAGE_NAME;
        }
        else if (pageName.equals(DIR_PAGE_NAME)) {
            throw new IOException("You can't create a file with the same name of the directory filename store. Choose any other name, but not this one; " + pageName);
        }

        String ext = Settings.instance().getPagesFileExtension();
        if (!StringUtils.isEmpty(ext) && !pageName.endsWith(ext)) {
            pageName += ext;
        }

        // we map the "lang" query param to the beginning of the URL by appending its value as a prefix to the path
        // this way, we (locally) uniformize the "/en/page" path to the "/page?lang=en" path format
        if (this.language!=null) {
            this.relativeStoragePath = URI.create("/"+this.language.getLanguage()+this.canonicalAddress);
        }
        else {
            //note: URIs are immutable, so this is safe
            this.relativeStoragePath = this.canonicalAddress;
        }

        if (!isDir) {
            //note: we need to go one 'up' first, to be able to replace the fileName with the new (with ext) fileName
            this.relativeStoragePath = this.relativeStoragePath.resolve(".");
        }
        this.relativeStoragePath = this.relativeStoragePath.resolve(pageName);

        //note: the subclass is responsible for setting this.resourcePath
    }
    protected AbstractPage(Path relativeLocalFile) throws IOException
    {
        String ext = Settings.instance().getPagesFileExtension();

        String filename = relativeLocalFile.getName();
        if (!filename.endsWith(ext)) {
            throw new IOException("Can't create a page from a file that doesn't end with '"+ext+"'");
        }
        else {
            this.relativeStoragePath = relativeLocalFile.toUri();

            //strip the storage file extension and check if we're dealing with a folder
            String urlFilename = filename.substring(0, filename.length()-ext.length());
            if (urlFilename.equals(DIR_PAGE_NAME)) {
                urlFilename = null;
            }

            //we strip off the name and re-build it if necessary
            URI canonicalTemp = relativeLocalFile.toUri().resolve(".");
            //means we're dealing with a file-url, not a folder-url
            if (urlFilename!=null) {
                canonicalTemp = canonicalTemp.resolve(urlFilename);
            }

            //strip off the language prefix (note that this retuns a path without a leading slash)
            UriBuilder uriBuilder = UriBuilder.fromUri(canonicalTemp);
            this.language = R.i18nFactory().getUrlLocale(canonicalTemp, uriBuilder, null);

            //make it 'relative' by re-adding the leading slash
            this.canonicalAddress = ROOT.resolve(uriBuilder.build());
        }
    }

    //-----PUBLIC METHODS-----
    @Override
    public URI getCanonicalAddress()
    {
        return this.canonicalAddress;
    }
    @Override
    public Locale getLanguage()
    {
        return this.language;
    }
    @Override
    public URI getPublicAbsoluteAddress() throws IOException
    {
        if (!this.checkedAbsoluteAddress) {
            //Note: the relative-root removed the leading slash, allowing us to let siteDomain have a prefix path (which is unlikely, but still)
            URI relativePath = ROOT.relativize(this.getCanonicalAddress());
            boolean isResource = RdfTools.isResourceUrl(this.getCanonicalAddress());
            if (!isResource && this.getLanguage()!=null) {
                //note: no leading slash, same reason as above
                relativePath = URI.create(this.getLanguage().getLanguage()+"/").resolve(relativePath);
            }

            //note: since we start with the canonical address, we don't have to strip off any storage filenames or extensions
            UriBuilder builder = UriBuilder.fromUri(R.configuration().getSiteDomain().resolve(relativePath));

            //we save resources with a language-prefixed path just like all the other pages,
            // but use the 'lang' query parameter in public form, because it reflects the nature
            // of a resource url better, so if the second path-part (the first in public form) is a resource string,
            // we're dealing with a resource page and need to convert it back to it's public form
            if (isResource && this.getLanguage()!=null) {
                builder.queryParam(I18nFactory.LANG_QUERY_PARAM, this.getLanguage().getLanguage());
            }

            this.cachedAbsoluteAddress = builder.build().normalize();
            this.checkedAbsoluteAddress = true;
        }

        return this.cachedAbsoluteAddress;
    }
    @Override
    public URI getPublicRelativeAddress() throws IOException
    {
        if (!this.checkedRelativeAddress) {
            //note that getPublicAbsoluteAddress() uses the siteDomain setting to generate it's absolute URL,
            //the first resolve makes sure it always starts with a slash
            this.cachedRelativeAddress = URI.create("/").resolve(R.configuration().getSiteDomain().relativize(this.getPublicAbsoluteAddress()));
            this.checkedRelativeAddress = true;
        }

        return this.cachedRelativeAddress;
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
            return new HtmlStreamSource(this.getPublicAbsoluteAddress(), is);
        }
    }
    @Override
    public HtmlSource readNormalizedHtml() throws IOException
    {
        Resource resource = R.resourceFactory().lookup(new HdfsResource(new ResourceRequestImpl(this.getPublicAbsoluteAddress(), Resource.MimeType.HTML),
                                                                        this.getResourcePath().getFileContext(), this.getNormalizedPageProxyPath()));

        //note: no need to wrap in an auto-close because the .close() on a StringWriter is a NOOP
        Writer writer = new StringWriter();

        //we need to pull the normalized html through the template engine for this to work
        R.templateEngine().getNewTemplate(resource).render(writer);

        return new HtmlStringSource(this.getPublicAbsoluteAddress(), writer.toString());
    }
    @Override
    public Model readRdfModel() throws IOException
    {
        Model retVal = null;

        //explicitly read the model from disk so we can use this stand alone
        Importer rdfImporter = this.createImporter(this.getRdfExportFileFormat());
        try (InputStream is = this.getResourcePath().getFileContext().open(this.getRdfExportFile())) {
            retVal = rdfImporter.importDocument(is, this.getPublicRelativeAddress());
        }

        return retVal;
    }
    @Override
    public Map<Locale, Page> getTranslations() throws IOException
    {
        Map<Locale, Page> retVal = new LinkedHashMap<>();

        Locale thisLang = this.getLanguage();
        Map<String, Locale> siteLanguages = R.configuration().getLanguages();

        for (Map.Entry<String, Locale> l : siteLanguages.entrySet()) {
            Locale lang = l.getValue();
            //we're searching for a translation, not the same language
            if (!lang.equals(thisLang)) {
                UriBuilder translatedUri = UriBuilder.fromUri(this.getPublicAbsoluteAddress());
                if (R.i18nFactory().getUrlLocale(this.getPublicAbsoluteAddress(), translatedUri, lang) != null) {
                    URI transPagePublicUri = translatedUri.build();
                    Page transPage = new ReadOnlyPage(transPagePublicUri);
                    if (this.getResourcePath().getFileContext().util().exists(transPage.getResourcePath().getLocalPath())) {
                        retVal.put(lang, transPage);
                    }
                }
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
