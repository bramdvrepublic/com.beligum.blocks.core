package com.beligum.blocks.filesystem.pages;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.filesystem.AbstractBlocksResource;
import com.beligum.blocks.filesystem.hdfs.HdfsUtils;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.utils.RdfTools;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;
import org.openrdf.model.Model;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by bram on 1/27/16.
 */
public abstract class AbstractPage extends AbstractBlocksResource implements Page
{
    //-----CONSTANTS-----
    //Note: don't change this or the entire DB will be corrupt
    protected static final String DIR_PAGE_NAME = "index";

    //-----VARIABLES-----
    protected URI canonicalAddress;

    protected URI cachedAbsoluteAddress;
    protected boolean checkedAbsoluteAddress;
    protected URI cachedRelativeAddress;
    protected boolean checkedRelativeAddress;

    //-----CONSTRUCTORS-----
    /**
     * Constructor that builds a page instance from a public request-URI,
     * handling all internal path specific translations
     */
    protected AbstractPage(ResourceRequest request, FileContext fileContext) throws IOException
    {
        super(request, fileContext);

        //After some super-preparsing, we need to do our own page-related post-parsing
        this.init(this.getUri());
    }
    protected AbstractPage(ResourceRepository repository, URI uri, Locale language, MimeType mimeType, boolean allowEternalCaching, FileContext fileContext) throws IOException
    {
        super(repository, uri, language, mimeType, allowEternalCaching, fileContext, null);

        //After some super-preparsing, we need to do our own page-related post-parsing
        this.init(this.getUri());
    }

    //-----PUBLIC METHODS-----
    /**
     * We overload the exists() because a page only exists if it's normalized file exists (see PageRepository.get())
     */
    @Override
    public boolean exists() throws IOException
    {
        return this.fileContext.util().exists(this.getNormalizedHtmlFile());
    }
    /**
     * This is mainly for debugging, but is probably what we want
     */
    @Override
    public long getLastModifiedTime() throws IOException
    {
        return Math.max((this.fileContext == null ? this.getZeroLastModificationTime() : this.fileContext.getFileStatus(this.getNormalizedHtmlFile()).getModificationTime()),
                        this.calcChildrenLastModificationTime());
    }
    /**
     * We'll overload this method to just return the default URI because we don't want page URIs to get fingerprinted
     */
    @Override
    public URI getFingerprintedUri()
    {
        return this.getUri();
    }
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
    public URI getPublicAbsoluteAddress()
    {
        if (!this.checkedAbsoluteAddress) {
            //Note: the relative-root removed the leading slash, allowing us to let siteDomain have a prefix path (which is unlikely, but still)
            URI relativePath = HdfsUtils.ROOT.relativize(this.getCanonicalAddress());
            boolean isResource = RdfTools.isResourceUrl(this.getCanonicalAddress());
            if (!isResource && this.getLanguage() != null) {
                //note: no leading slash, same reason as above
                relativePath = URI.create(this.getLanguage().getLanguage() + "/").resolve(relativePath);
            }

            //note: since we start with the canonical address, we don't have to strip off any storage filenames or extensions
            UriBuilder builder = UriBuilder.fromUri(R.configuration().getSiteDomain().resolve(relativePath));

            //we save resources with a language-prefixed path just like all the other pages,
            // but use the 'lang' query parameter in public form, because it reflects the nature
            // of a resource url better, so if the second path-part (the first in public form) is a resource string,
            // we're dealing with a resource page and need to convert it back to it's public form
            if (isResource && this.getLanguage() != null) {
                builder.queryParam(I18nFactory.LANG_QUERY_PARAM, this.getLanguage().getLanguage());
            }

            this.cachedAbsoluteAddress = builder.build().normalize();
            this.checkedAbsoluteAddress = true;
        }

        return this.cachedAbsoluteAddress;
    }
    @Override
    public URI getPublicRelativeAddress()
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
    public Model readRdfModel() throws IOException
    {
        Model retVal = null;

        //explicitly read the model from disk so we can use this stand alone
        Path modelFile = this.getRdfExportFile();
        //don't let it throw an exception if the file doesn't exist, just return null
        if (this.getFileContext().util().exists(modelFile)) {
            Importer rdfImporter = this.createImporter(this.getRdfExportFileFormat());
            try (InputStream is = this.getFileContext().open(modelFile)) {
                //note that all RDF needs absolute addresses
                retVal = rdfImporter.importDocument(this.getPublicAbsoluteAddress(), is);
            }
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
                if (R.i18n().getUrlLocale(this.getPublicAbsoluteAddress(), translatedUri, lang) != null) {
                    Page transPage = R.resourceManager().get(translatedUri.build(), MimeTypes.HTML, Page.class);
                    if (transPage != null) {
                        retVal.put(lang, transPage);
                    }
                }
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * Note: this will both set super.fileContext and super.localStoragePath
     */
    private void init(URI uri) throws IOException
    {
        //Note: the code below is meant to be used with an absolute public uri, so make sure it's uniform
        if (!uri.isAbsolute()) {
            uri = R.configuration().getSiteDomain().resolve(uri);
        }

        //we start off by stripping (and extracting) the language parameters from the URI
        UriBuilder uriBuilder = UriBuilder.fromUri(uri);

        //strip off the language and save it in a local variable
        //note that this will be null if the uri doesn't contain any language; that's ok for now
        Locale urlLang = R.i18n().getUrlLocale(uri, uriBuilder, null);
        //let's not overwrite a possible initialized this.language with null
        if (urlLang != null) {
            if (this.language != null && !this.language.equals(urlLang)) {
                Logger.warn("Overwriting the language of page '" + uri + "' with a new value '" + urlLang + "', this is probably a mistake.");
            }
            this.language = urlLang;
        }

        //we strip off all extra parameters: for now we don't support query (or other) parameters during page requests,
        // mainly because they don't map well to file/folder names
        uriBuilder = uriBuilder.replaceQuery(null);

        //this will be the normalized uri (cleaned and stripped from all language references; the main language is stored earlier)
        URI normalizedUri = uriBuilder.build();

        //if the uri is absolute (starts with http://...), check if the domain (authority = domain + port) matches,
        // otherwise (eg. when the URI is relative (eg. /resource/...) the authority will be null (and that's ok)
        if (normalizedUri.getAuthority() != null && !normalizedUri.getAuthority().equals(R.configuration().getSiteDomain().getAuthority())) {
            throw new SecurityException("Trying to create a page path from outside the domain (" + R.configuration().getSiteDomain() + "), can't proceed; " + normalizedUri);
        }

        //Now, build the relative, normalized storage path of this page (where this page will be stored relative to the chroot)
        //Note that this path has a leading slash
        String relativeAddressPath = normalizedUri.getPath();

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
        if (this.language != null) {
            this.localStoragePath = new Path("/" + this.language.getLanguage() + relativeAddressPath);
        }
        else {
            //note: URIs are immutable, so this is safe
            this.localStoragePath = new Path(relativeAddressPath);
        }

        if (!isDir) {
            //note: we need to go one 'up' first, to be able to replace the fileName with the new (with ext) fileName
            this.localStoragePath = this.localStoragePath.getParent();
        }
        this.localStoragePath = new Path(this.localStoragePath, pageName);

        //this is the 'most naked' address of the page, without language params or storage filenames
        this.canonicalAddress = URI.create(relativeAddressPath);
    }
}
