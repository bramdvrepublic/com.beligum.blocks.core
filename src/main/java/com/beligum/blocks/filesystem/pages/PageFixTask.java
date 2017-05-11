package com.beligum.blocks.filesystem.pages;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.blocks.config.RdfFactory;
import com.beligum.blocks.filesystem.LockFile;
import com.beligum.blocks.filesystem.index.reindex.ReindexTask;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontology.vocabularies.RDF;
import com.beligum.blocks.rdf.sources.NewPageSource;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import net.htmlparser.jericho.*;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bram on 11/05/17.
 */
public class PageFixTask extends ReindexTask
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    protected void runTaskFor(Resource resource, ResourceRepository.IndexOption indexConnectionsOption) throws IOException
    {
        Page page = resource.unwrap(Page.class);
        if (page == null) {
            throw new IOException("Unable to fix this resource, it's not a valid Page; " + resource);
        }

        Path originalFile = page.getLocalStoragePath();
        if (!page.getFileContext().util().exists(originalFile)) {
            throw new IOException("Original HTML file for this page is missing, can't fix it; " + page.getPublicAbsoluteAddress());
        }

        ReadWritePage rwPage = new ReadWritePage(page.getRepository(), page);
        try (LockFile lock = rwPage.acquireLock()) {

            boolean somethingChanged = false;

            //Note: we can't use the NewPageSource(page) constructor because it reads the normalized html, not the raw original
            NewPageSource pageSource = null;
            try (InputStream originalHtml = page.getFileContext().open(page.getLocalStoragePath())) {

                Source htmlSource = new Source(originalHtml);
                OutputDocument htmlOutput = new OutputDocument(htmlSource);

                somethingChanged = this.fixBlocksFactLangString(page, htmlSource, htmlOutput);

                if (somethingChanged) {
                    pageSource = new NewPageSource(page.getUri(), htmlOutput.toString());
                }
            }

            if (somethingChanged) {
                com.beligum.base.utils.Logger.info("Fixing page "+page);
                rwPage.write(pageSource);
                rwPage.updateNormalizedProxy(pageSource);
                rwPage.updateRdfProxy(pageSource);

                //we changed the RDFs, so the page needs to be reindexed as well
                resource.getRepository().reindex(resource, indexConnectionsOption);
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private boolean fixBlocksFactLangString(Page page, Source source, OutputDocument output)
    {
        boolean retVal = false;

        for (StartTag factStartTag : source.getAllStartTags("blocks-fact-entry")) {
            Element factElement = factStartTag.getElement();
            Element propertyEl = factElement.getFirstElementByClass("property");
            if (propertyEl != null) {
                String resourceType = HtmlTemplate.getPropertyAttribute(propertyEl.getStartTag());
                if (!StringUtils.isEmpty(resourceType)) {
                    RdfClass rdfClass = RdfFactory.getClassForResourceType(URI.create(resourceType));
                    if (rdfClass != null && rdfClass instanceof RdfProperty) {
                        RdfProperty rdfProperty = (RdfProperty) rdfClass;

                        if (rdfProperty.getDataType().equals(RDF.LANGSTRING)) {
                            StartTag propertyStartTag = propertyEl.getStartTag();
                            Map<String, String> attrMap = new LinkedHashMap<>();
                            Attributes propertyAttributes = propertyStartTag.getAttributes();
                            propertyAttributes.populateMap(attrMap, false);

                            final String DATATYPE_ATTR = "datatype";
                            if (attrMap.containsKey(DATATYPE_ATTR) && attrMap.get(DATATYPE_ATTR).equals(com.beligum.blocks.rdf.ontology.vocabularies.XSD.STRING.getCurieName().toString())) {
                                //if we reach this point, we are dealing with a <blocks-fact-entry> start tag that has a datatype="xsd:string",
                                // but was later upgraded to rdf:langString. Following our own rules in RDF.LANGSTRING, we'll delete the datatype-attribute.
                                attrMap.remove(DATATYPE_ATTR);
                                output.replace(propertyAttributes, attrMap);

                                retVal = true;
                            }
                        }
                    }
                }
            }
        }

        return retVal;
    }
}
