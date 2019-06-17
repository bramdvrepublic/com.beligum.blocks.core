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

package com.beligum.blocks.index.reindex.tasks;

import com.beligum.base.config.CoreConfiguration;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.endpoints.PageAdminEndpoint;
import com.beligum.blocks.filesystem.LockFile;
import com.beligum.blocks.filesystem.pages.NewPageSource;
import com.beligum.blocks.filesystem.pages.PageSource;
import com.beligum.blocks.filesystem.pages.ReadWritePage;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.index.reindex.ReindexTask;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.rdf.ifaces.RdfOntology;
import com.beligum.blocks.rdf.ifaces.RdfOntologyMember;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfResource;
import com.beligum.blocks.rdf.ontologies.Meta;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlRdfContext;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import net.htmlparser.jericho.OutputDocument;
import net.htmlparser.jericho.Source;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static com.beligum.blocks.filesystem.pages.PageSource.HTML_ROOT_PREFIX_ATTR;
import static com.beligum.blocks.filesystem.pages.PageSource.HTML_ROOT_VOCAB_ATTR;

/**
 * This is a general upgrade task, meant to be used now and in the future to upgrade existing data formats
 * to new ones, based on the version number of the pages on disk.
 *
 * Created by bram on 11/05/17.
 */
public class PageUpgradeTask extends ReindexTask
{
    //-----CONSTANTS-----
    // Starting from this blocks-core version, we'll upgrade the pages to the new format
    private static final ComparableVersion MIN_VERSION_BLOCKS_CORE = new ComparableVersion("0.7.3-SNAPSHOT");

    //-----VARIABLES-----
    private static boolean mavenInit = false;
    private static Object mavenLock = new Object();
    private static ComparableVersion mavenVersion_blocksCore;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    protected void runTaskFor(Resource resource, ResourceRepository.IndexOption indexConnectionsOption) throws IOException
    {
        com.beligum.base.utils.Logger.info("Checking page for upgrade " + resource);

        // cut short older versions that shouldn't be upgraded (yet)
        if (this.upgradeAllowed()) {

            Page page = resource.unwrap(Page.class);
            if (page == null) {
                throw new IOException("Unable to fix this resource, it's not a valid Page; " + resource);
            }

            Path originalFile = page.getLocalStoragePath();
            if (!page.getFileContext().util().exists(originalFile)) {
                throw new IOException("Original HTML file for this page is missing, can't fix it; " + page.getPublicAbsoluteAddress());
            }

            boolean somethingChanged = false;
            Document document = null;
            ReadWritePage rwPage = page.getReadWriteVariant();
            try (LockFile lock = rwPage.acquireLock()) {

                try (InputStream originalHtml = page.getFileContext().open(page.getLocalStoragePath())) {

                    document = Jsoup.parse(originalHtml, null, page.getUri().toString());

                    if (this.upgradeNeeded(document)) {

                        Elements htmlTags = document.getElementsByTag("html");
                        if (htmlTags.isEmpty()) {
                            throw new IOException("The supplied HTML must contain a <html> tag; " + page.getUri());
                        }
                        else {
                            Element htmlTag = htmlTags.first();

                            // update @vocab and @prefix of the root html tag
                            htmlTag.attr(HTML_ROOT_VOCAB_ATTR, HtmlRdfContext.getDefaultRdfVocab().toString());
                            htmlTag.attr(HTML_ROOT_PREFIX_ATTR, HtmlRdfContext.getDefaultRdfPrefixesAttribute());

                            Elements titleTags = document.getElementsByTag("title");
                            for (Element titleTag : titleTags) {
                                // Note: make sure we don't introduce the property attribute
                                if (titleTag.hasAttr(HtmlParser.RDF_PROPERTY_ATTR)) {
                                    titleTag.attr(HtmlParser.RDF_PROPERTY_ATTR, Settings.instance().getRdfLabelProperty().getCurie().toString());
                                }
                            }

                            // iterate the meta elements and update the ones without a 'meta:' prefix in their property
                            RdfOntology metaOntology = RdfFactory.lookup(Meta.NAMESPACE.getUri(), RdfOntology.class);
                            Elements metaTags = document.getElementsByTag(PageSource.HTML_META_ELEMENT);
                            for (Element metaTag : metaTags) {
                                String property = metaTag.attr(HtmlParser.RDF_PROPERTY_ATTR);

                                if (!StringUtils.isEmpty(property)) {

                                    RdfOntologyMember metaMember = metaOntology.getMember(property);
                                    if (metaMember != null && metaMember instanceof RdfProperty && !property.equals(metaMember.getCurie())) {
                                        RdfProperty metaProp = (RdfProperty) metaMember;
                                        metaTag.attr(HtmlParser.RDF_PROPERTY_ATTR, metaProp.getCurie().toString());
                                        metaTag.attr(HtmlParser.RDF_DATATYPE_ATTR, metaProp.getDataType().getCurie().toString());
                                        // note: we don't update the content, cause we want to save the value (note that it can be empty)
                                    }
                                }
                            }

                            somethingChanged = true;
                        }
                    }
                }
            }

            if (somethingChanged) {
                new PageAdminEndpoint().savePage(URI.create(document.baseUri()), document.outerHtml());
            }
        }
        else {
            Logger.info("Skipping resource because it's version says it doesn't need upgrading; " + resource);
        }
    }
    private void assertExistingVersions()
    {
        if (!mavenInit) {
            synchronized (mavenLock) {
                if (!mavenInit) {
                    mavenVersion_blocksCore = new ComparableVersion(gen.com.beligum.blocks.core.maven.maven_version);

                    mavenInit = true;
                }
            }
        }
    }
    /**
     * This is a barrier for the upgrade task: all running stralo versions that are below this minimum block-core version
     * shouldn't be upgraded, because it will break the system compatibility.
     */
    private boolean upgradeAllowed()
    {
        this.assertExistingVersions();

        // This means that the current version of blocks-core should be more recent or equal to
        // the one in the constant above.
        // Note that when comparing the same version, but with a -SNAPSHOT version, it works as expected:
        // the SNAPSHOT will be "smaller" than the release version.
        return mavenVersion_blocksCore.compareTo(MIN_VERSION_BLOCKS_CORE) >= 0;
    }
    /**
     * This is an extra condition on top of upgradeAllowed() that checks this specific page instance
     * if it _needs_ upgrading (on top of it's allowed to upgrade).
     */
    private boolean upgradeNeeded(Document document)
    {
        return this.upgradeAllowed();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
