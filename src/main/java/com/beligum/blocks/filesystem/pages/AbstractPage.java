/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.filesystem.pages;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.resources.ifaces.ResourceAction;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.config.Permissions;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.config.StorageFactory;
import com.beligum.blocks.filesystem.AbstractBlocksResource;
import com.beligum.blocks.filesystem.hdfs.HdfsUtils;
import com.beligum.blocks.filesystem.ifaces.ResourceMetadata;
import com.beligum.blocks.index.ifaces.*;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.Importer;
import com.beligum.blocks.templating.blocks.analyzer.HtmlAnalyzer;
import com.beligum.blocks.utils.RdfTools;
import com.beligum.blocks.utils.SecurityTools;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Path;
import org.apache.shiro.authz.UnauthorizedException;
import org.eclipse.rdf4j.model.Model;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static com.beligum.blocks.config.Settings.DEFAULT_MAIN_ONTOLOGY_ENDPOINT;
import static com.beligum.blocks.config.Settings.RESOURCE_ENDPOINT;

/**
 * Created by bram on 1/27/16.
 */
public abstract class AbstractPage extends AbstractBlocksResource implements Page
{
    //-----CONSTANTS-----
    //Note: don't change this or the entire DB will be corrupt
    protected static final String DIR_PAGE_NAME = "index";

    /**
     * This is a collection of page names that can't be created by end users
     * because their usage is too common in our own code
     */
    protected static final String[] RESERVED_PAGE_NAMES = {
                    //this is the file name of the directory with this name (stored on disk inside a directory with the page name)
                    DIR_PAGE_NAME,
                    //this is used to create a virtual endpoint into the ontology metadata of this system, eg. http://www.reinvention.be/ontology/
                    StringUtils.strip(DEFAULT_MAIN_ONTOLOGY_ENDPOINT, "/"),
                    //these two are future placeholders to query metadata information about certain pages/media in the system, eg. http://www.reinvention.be/contact/meta?type=creator
                    "meta", "metadata",
                    //the special resources url prefix; sync this with com.beligum.blocks.config.Settings.RESOURCE_ENDPOINT
                    StringUtils.strip(RESOURCE_ENDPOINT, "/"),
                    };

    //-----VARIABLES-----
    // this is the 'most naked' address of the page, relative, without language params or storage filenames
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
    @Override
    public boolean isPermitted(ResourceAction action)
    {
        boolean retVal = false;

        try {
            switch (action) {
                case READ:
                    retVal = R.securityManager().isPermitted(Permissions.PAGE_READ_ALL_PERM)
                             || R.securityManager().isPermitted(Permissions.PAGE_READ_ALL_HTML_PERM)
                             || R.securityManager().isPermitted(Permissions.PAGE_READ_ALL_RDF_PERM);

                    //if all is well, and a custom ACL is set, also check the ACL
                    if (retVal && this.getMetadata().getReadAcl() != null) {
                        retVal = SecurityTools.isPermitted(R.securityManager().getCurrentRole(), this.getMetadata().getReadAcl());
                    }

                    break;

                case CREATE:
                    retVal = R.securityManager().isPermitted(Permissions.PAGE_CREATE_ALL_PERM)
                             || R.securityManager().isPermitted(Permissions.PAGE_CREATE_TEMPLATE_ALL_PERM)
                             || R.securityManager().isPermitted(Permissions.PAGE_CREATE_COPY_ALL_PERM);

                    //note: we don't have a create ACL

                    break;

                case UPDATE:
                    //we start out with the general permission
                    retVal = R.securityManager().isPermitted(Permissions.PAGE_UPDATE_ALL_PERM);

                    //if the settings say a user can only edit it's own creations, make sure
                    //we have a creator and a matching current user before we allow editing
                    if (!retVal && R.securityManager().isPermitted(Permissions.PAGE_UPDATE_OWN_PERM)) {
                        retVal = currentPersonIsCreator();
                    }

                    //if all is well, and a custom ACL is set, also check the ACL
                    if (retVal && this.getMetadata().getUpdateAcl() != null) {
                        retVal = SecurityTools.isPermitted(R.securityManager().getCurrentRole(), this.getMetadata().getUpdateAcl());
                    }

                    break;

                case DELETE:
                    retVal = R.securityManager().isPermitted(Permissions.PAGE_DELETE_ALL_PERM);

                    //if the settings say a user can only edit it's own creations, make sure
                    //we have a creator and a matching current user before we allow editing
                    if (!retVal && R.securityManager().isPermitted(Permissions.PAGE_DELETE_OWN_PERM)) {
                        retVal = currentPersonIsCreator();
                    }

                    //if all is well, and a custom ACL is set, also check the ACL
                    if (retVal && this.getMetadata().getDeleteAcl() != null) {
                        retVal = SecurityTools.isPermitted(R.securityManager().getCurrentRole(), this.getMetadata().getDeleteAcl());
                    }

                    break;
            }
        }
        catch (Throwable e) {
            Logger.error("Caught unexpected exception while checking '" + action + "' access permission to this page; " + this, e);
            throw new UnauthorizedException("Error happened while checking permission to execute action '" + action + "', can't continue");
        }

        return retVal;
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
            RdfTools.RdfResourceUri rdfResourceUri = new RdfTools.RdfResourceUri(this.getCanonicalAddress());
            if (!rdfResourceUri.isValid() && this.getLanguage() != null) {
                //note: no leading slash, same reason as above
                relativePath = URI.create(this.getLanguage().getLanguage() + "/").resolve(relativePath);
            }

            //note: since we start with the canonical address, we don't have to strip off any storage filenames or extensions
            UriBuilder builder = UriBuilder.fromUri(R.configuration().getSiteDomain().resolve(relativePath));

            //we save resources with a language-prefixed path just like all the other pages,
            // but use the 'lang' query parameter in public form, because it reflects the nature
            // of a resource url better, so if the second path-part (the first in public form) is a resource string,
            // we're dealing with a resource page and need to convert it back to it's public form
            if (rdfResourceUri.isValid() && this.getLanguage() != null) {
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
    public URI getAbsoluteResourceAddress() throws IOException
    {
        URI retVal = null;

        HtmlAnalyzer.AttributeRef aboutAttr = this.createAnalyzer().getHtmlAbout();
        if (aboutAttr != null && !StringUtils.isBlank(aboutAttr.value)) {
            retVal = URI.create(aboutAttr.value.trim());
            if (!retVal.isAbsolute()) {
                retVal = R.configuration().getSiteDomain().resolve(retVal);
            }
        }

        return retVal;
    }
    @Override
    public Model readRdfModel() throws IOException
    {
        return this.readRdfFile(this.getRdfExportFile());
    }
    @Override
    public Model readRdfDependenciesModel() throws IOException
    {
        return this.readRdfFile(this.getRdfDependenciesExportFile());
    }
    @Override
    public Map<Locale, Page> getTranslations() throws IOException
    {
        return this.getTranslations(this.createAnalyzer());
    }
    @Override
    public Map<Locale, Page> getTranslations(HtmlAnalyzer analyzer) throws IOException
    {
        Map<Locale, Page> retVal = new LinkedHashMap<>();

        Locale thisLang = this.getLanguage();
        Map<String, Locale> siteLanguages = R.configuration().getLanguages();

        //We read the resource uri from disk. Note that resource URIs don't have languages set
        URI resourceNoLangUri = UriBuilder.fromUri(analyzer.getHtmlAbout().value).build();

        //Now, we'll search the index for all pages with this resource URI
        //Note that resource URIs are the only way to effectively get translations. We can't rely
        //     on the filesystem names because public URIs in a different language can have completely
        //     different URIs (eg. for SEO purposes).
        //Note that this also means the lucene index needs to be up-to-date (eg. this is important during reindexing;
        // see the notes in the delete() method of the triple store index connection)
        IndexSearchRequest searchRequest = IndexSearchRequest.createFor(StorageFactory.getJsonIndexer().connect());
        searchRequest.filter(PageIndexEntry.resourceField, StringFunctions.getRightOfDomain(resourceNoLangUri).toString(), IndexSearchRequest.FilterBoolean.AND);
        searchRequest.pageSize(siteLanguages.size());
        IndexSearchResult searchResult = searchRequest.getIndexConnection().search(searchRequest);
        for (ResourceProxy entry : searchResult) {
            Page transPage = R.resourceManager().get(entry.getUri(), MimeTypes.HTML, Page.class);
            if (transPage != null && !transPage.getLanguage().equals(thisLang)) {
                retVal.put(transPage.getLanguage(), transPage);
            }
        }

        return retVal;
    }
    @Override
    public Iterable<URI> getSubResources() throws IOException
    {
        return this.getSubResources(this.createAnalyzer());
    }
    @Override
    public Iterable<URI> getSubResources(HtmlAnalyzer analyzer) throws IOException
    {
        return analyzer.getSubResources();
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
            throw new SecurityException("Trying to instance a page path from outside the domain (" + R.configuration().getSiteDomain() + "), can't proceed; " + normalizedUri);
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
        else if (Arrays.binarySearch(RESERVED_PAGE_NAMES, pageName) >= 0) {
            throw new IOException("You can't instance a page named '" + pageName + "' because it's a reserved name." +
                                  " This is the list of reserved page names: " + Arrays.toString(RESERVED_PAGE_NAMES));
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
    private Model readRdfFile(Path modelFile) throws IOException
    {
        Model retVal = null;

        //Note: explicitly read the model from disk so we can use this stand alone
        //don't let it throw an exception if the file doesn't exist, just return null
        if (this.getFileContext().util().exists(modelFile)) {
            try (InputStream is = this.getFileContext().open(modelFile)) {
                //note that all RDF needs absolute addresses
                retVal = this.createImporter(this.getRdfExportFileFormat()).importDocument(this.getPublicAbsoluteAddress(), is);
            }
        }

        return retVal;
    }
    private boolean currentPersonIsCreator()
    {
        boolean retVal = false;

        try {
            ResourceMetadata metadata = this.getMetadata();
            if (metadata != null && metadata.getCreator() != null) {
                URI currentPerson = R.securityManager().getCurrentPersonUri(metadata.getCreator().isAbsolute());
                if (currentPerson != null && metadata.getCreator().equals(currentPerson)) {
                    retVal = true;
                }
            }
        }
        catch (Exception e) {
            Logger.error("Error while checking security of page " + this.getClass().getSimpleName() + "; " + this.getUri(), e);
        }

        return retVal;
    }
}
