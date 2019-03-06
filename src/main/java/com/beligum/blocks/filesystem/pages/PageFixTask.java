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

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.blocks.config.InputType;
import com.beligum.blocks.rdf.RdfFactory;
import com.beligum.blocks.utils.importer.ImportTools;
import com.beligum.blocks.filesystem.LockFile;
import com.beligum.blocks.filesystem.index.reindex.ReindexTask;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ontologies.RDF;
import com.beligum.blocks.rdf.ontologies.XSD;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import net.htmlparser.jericho.*;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.beligum.blocks.templating.blocks.HtmlParser.RDF_CONTENT_ATTR;
import static java.time.ZoneOffset.UTC;

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

                Source htmlSource = HtmlTemplate.readHtmlInputStream(originalHtml);
                OutputDocument htmlOutput = new OutputDocument(htmlSource);

                somethingChanged = this.fixBlocksFacts(page, htmlSource, htmlOutput);

                if (somethingChanged) {
                    pageSource = new NewPageSource(page.getUri(), htmlOutput.toString());
                }
            }

//            com.beligum.base.utils.Logger.warn("Watch out: DEBUGGING activated");
//            somethingChanged = false;

            if (somethingChanged) {
                com.beligum.base.utils.Logger.info("Fixing page " + page);
                rwPage.write(pageSource);

                // Note: reindexing the page will check if the normalized, n-triple and n-triple dep file exists, not if it was changed or not.
                // Since it's easy to update them here, so that the reindex step only need to reindex.
                rwPage.updateNormalizedProxy(pageSource);
                rwPage.updateRdfProxy(pageSource);

                //Update: after all, we decided it's best to do this in a separate run
                //we changed the RDFs, so the page needs to be reindexed as well
                //resource.getRepository().reindex(resource, indexConnectionsOption);
            }
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private boolean fixBlocksFacts(Page page, Source source, OutputDocument output) throws IOException
    {
        final String DATATYPE_ATTR = "datatype";
        final String BLOCKS_FACT_TAG = "blocks-fact-entry";
        final String PROPERTY_ATTR = "property";

        boolean retVal = false;

        for (StartTag factStartTag : source.getAllStartTags(BLOCKS_FACT_TAG)) {
            Element factElement = factStartTag.getElement();
            Element propertyEl = factElement.getFirstElementByClass(PROPERTY_ATTR);
            if (propertyEl != null) {
                String resourceType = HtmlTemplate.getPropertyAttribute(propertyEl.getStartTag());
                if (!StringUtils.isEmpty(resourceType)) {
                    RdfClass rdfClass = RdfFactory.getClassForResourceType(resourceType);
                    if (rdfClass != null && rdfClass instanceof RdfProperty) {
                        RdfProperty rdfProperty = (RdfProperty) rdfClass;
                        StartTag propertyStartTag = propertyEl.getStartTag();

                        //on 11/05/17, we started using the RDF.LANGSTRING datatype to mark a string as translatable,
                        //this will process any XSD.string datatypes that should be "upgraded" to RDF.LANGSTRING
                        if (rdfProperty.getDataType().equals(RDF.langString)) {
                            Map<String, String> attrMap = new LinkedHashMap<>();
                            Attributes propertyAttributes = propertyStartTag.getAttributes();
                            propertyAttributes.populateMap(attrMap, false);

                            if (attrMap.containsKey(DATATYPE_ATTR) && attrMap.get(DATATYPE_ATTR).equals(XSD.string.getCurieName().toString())) {
                                // if we reach this point, we are dealing with a <blocks-fact-entry> start tag that has a datatype="xsd:string",
                                // but was later upgraded to rdf:langString. Following our own rules in RDF.LANGSTRING,
                                // we'll delete the datatype-attribute.
                                attrMap.remove(DATATYPE_ATTR);
                                output.replace(propertyAttributes, attrMap);

                                retVal = true;
                            }
                        }
                        //we made a few mistakes in the past where we introduced the wrong inputType for date/time related dataTypes,
                        //this will make sure the inputType follows the dataType
                        else if (rdfProperty.getDataType().equals(XSD.date) || rdfProperty.getDataType().equals(XSD.time) || rdfProperty.getDataType().equals(XSD.dateTime)) {

                            Attributes propertyAttributes = propertyStartTag.getAttributes();

                            Set<String> classes = new HashSet<>();
                            String classAttr = propertyAttributes.getValue("class");
                            if (!StringUtils.isEmpty(classAttr)) {
                                classes.addAll(Arrays.asList(classAttr.trim().split(" ")));
                            }

                            //This is the class that is always there, regardless of the inputType and
                            // is actually FACT_ENTRY_PROPERTY_CLASS
                            final String generalClass = "property";

                            try {
                                //TODO this doesn't account for the GMT flag!
                                if ((rdfProperty.getDataType().equals(XSD.date) && !classes.contains(InputType.Date.getConstant()))
                                    || (rdfProperty.getDataType().equals(XSD.time) && !classes.contains(InputType.Time.getConstant()))
                                    || (rdfProperty.getDataType().equals(XSD.dateTime) && !classes.contains(InputType.DateTime.getConstant()))) {

                                    Object value = this.parseDateTimeRelatedValue(propertyAttributes.getValue(RDF_CONTENT_ATTR));
                                    String newHtml = ImportTools.propertyValueToHtml(rdfProperty, value, page.getLanguage(), null);
                                    output.replace(factElement, new Source(newHtml));

                                    retVal = true;
                                }
                            }
                            catch (Exception e) {
                                throw new IOException("Error while trying to fix a date/time fact of page " + page, e);
                            }

                        }
                    }
                }
            }
        }

        return retVal;
    }
    private Object parseDateTimeRelatedValue(String value)
    {
        try {
            //Note: all values are stored in UTC, so force the zone
            return DateTimeFormatter.ISO_DATE_TIME.withZone(UTC).parse(value);
        }
        catch (DateTimeParseException e) {
        }

        try {
            //Note: local because we only support timezones in dateTime
            return DateTimeFormatter.ISO_LOCAL_DATE.parse(value);
        }
        catch (DateTimeParseException e) {
        }

        try {
            //Note: local because we only support timezones in dateTime
            return DateTimeFormatter.ISO_LOCAL_TIME.parse(value);
        }
        catch (DateTimeParseException e) {
        }

        return null;
    }
}
