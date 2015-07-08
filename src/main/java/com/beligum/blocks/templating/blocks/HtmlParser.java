package com.beligum.blocks.templating.blocks;

import com.beligum.base.resources.Asset;
import com.beligum.base.resources.ResourceDescriptor;
import com.beligum.base.resources.ResourceSearchResult;
import com.beligum.base.resources.parsers.AbstractAssetParser;
import com.beligum.base.resources.parsers.results.ParseResult;
import com.beligum.base.resources.parsers.results.StringParseResult;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.templating.blocks.directives.*;
import net.htmlparser.jericho.*;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by bram on 5/16/15.
 */
public class HtmlParser extends AbstractAssetParser
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private static final Pattern notEmptyPropertyAttrValue = Pattern.compile(".+");
    //this is the name of the RDFa property attribute (eg. the one that plays a role while parsing RDFa)
    private static final String RDF_PROPERTY_ATTR = "property";
    //this is the name of the property attribute that can be used in the template system, but doesn't 'mean' anything RDFa-wise
    private static final String NON_RDF_PROPERTY_ATTR = "data-property";

    //-----CONSTRUCTORS-----
    public HtmlParser()
    {
    }

    //-----PUBLIC METHODS-----
    public String parse(String rawSource) throws Exception
    {
        return this.doParse(null, rawSource);
    }
    @Override
    public ParseResult parse(ResourceDescriptor src, Map<String, String> args) throws IOException
    {
        try {
            Path sourcePath = src.getResolvedPath();
            String rawSource = new String(Files.readAllBytes(sourcePath));

            return new StringParseResult(sourcePath, this.doParse(sourcePath, rawSource), this.getCacheFile(sourcePath), Asset.MimeType.HTML);
        }
        catch (Exception e) {
            throw new IOException("Caught exception while parsing html file", e);
        }
    }
    /**
     * Strips all lines beginning with "##" (starting from that position) and returns all the rest.
     * @param html
     * @return
     * @throws IOException
     */
    public static String eatVelocityComments(String html) throws IOException
    {
        StringBuilder retVal = new StringBuilder();

        try (BufferedReader stringReader = new BufferedReader(new StringReader(html))) {
            String line;
            while ((line = stringReader.readLine()) != null) {
                int commentIdx = line.indexOf("##");
                if (commentIdx != -1) {
                    line = line.substring(0, commentIdx);
                }
                //if the entire line got eaten, I assume we can discard it
                if (!StringUtils.isEmpty(line)) {
                    retVal.append(line).append("\n");
                }
            }
        }

        return retVal.toString();
    }
    public static TemplateCache getCachedTemplates()
    {
        TemplateCache retVal = (TemplateCache) R.cacheManager().getApplicationCache().get(CacheKeys.TAG_TEMPLATES);
        if (retVal == null) {
            R.cacheManager().getApplicationCache().put(CacheKeys.TAG_TEMPLATES, retVal = new TemplateCache());
            try {
                searchAllTemplates(retVal);
            }
            catch (Exception e) {
                Logger.error("Caught exception while searching for all the webcomponent templates in the current classpath; this is bad and needs to fixed", e);
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private String doParse(Path sourcePath, String rawSource) throws Exception
    {
        //the solves a lot of issues with inactive lines
        String sourceStr = eatVelocityComments(rawSource);
        Source source = new Source(sourceStr);

        // if we're dealing with a template (eg. the file is a template, not an instance of a template), replace the source with the html in the template
        boolean isTagTemplate = false;
        TemplateCache templateCache = this.getCachedTemplates();
        if (sourcePath!=null && templateCache.containsKey(sourcePath)) {
            //first of all, since this method is only called when something changed, update the cache value
            HtmlTemplate oldTemplate = templateCache.get(sourcePath);//fetch the old value for the paths
            HtmlTemplate tagTemplate = HtmlTemplate.create(source, oldTemplate.getAbsolutePath(), oldTemplate.getRelativePath());
            templateCache.put(sourcePath, tagTemplate);

            StringBuilder builder = new StringBuilder();

            //this is the base for all coming preprocessing
            Segment templateHtml = tagTemplate.getHtml();
            OutputDocument output = new OutputDocument(templateHtml);

            if (tagTemplate instanceof TagTemplate) {
                isTagTemplate = true;

                //this blocks the resources include from being evaluated repeatedly in loops; we only need to evaluate it once per call
                String resourceTestVar = "R_"+tagTemplate.getVelocityTemplateName();
                builder.append("#if(!$").append(resourceTestVar).append(")").append("\n");

                // note that this "false" means it must be eaten (and spit out somewhere else)
                boolean renderResources = false;
                for (Element style : tagTemplate.getInlineStyleElements()) {
                    builder.append("#").append(TagTemplateInlineStyleResourceDirective.NAME).append("(").append(renderResources).append(")").append(style.toString()).append("#end").append("\n");
                }
                for (Element style : tagTemplate.getExternalStyleElements()) {
                    builder.append("#").append(TagTemplateExternalStyleResourceDirective.NAME).append("(").append(renderResources).append(",'").append(style.getAttributeValue("href")).append("')").append(style.toString())
                           .append("#end").append("\n");
                }
                for (Element script : tagTemplate.getInlineScriptElements()) {
                    builder.append("#").append(TagTemplateInlineScriptResourceDirective.NAME).append("(").append(renderResources).append(")").append(script.toString()).append("#end").append("\n");
                }
                for (Element script : tagTemplate.getExternalScriptElements()) {
                    builder.append("#").append(TagTemplateExternalScriptResourceDirective.NAME).append("(").append(renderResources).append(",'").append(script.getAttributeValue("src")).append("')")
                           .append(script.toString())
                           .append("#end").append("\n");
                }

                //block this piece from evaluating again in loops because it's expensive
                builder.append(" #set($").append(resourceTestVar).append("=true)").append("\n");
                builder.append("#end").append("\n");
            }
            // the only preprocessing we do with a page template is fill in the template attribute with the name of the template
            // so we know what template was used when the code comes back from the client
            else if (tagTemplate instanceof PageTemplate) {
                Element html = templateHtml.getFirstElement("template", null);
                if (!html.getName().equalsIgnoreCase("html")) {
                    throw new IOException("Found a template attribute on a non-html element, this shouldn't happen since it's been checked before; "+sourcePath);
                }
                //a little bit verbose, but I didn't find a shorter way...
                Attributes templateAttr = html.getAttributes();
                Map<String,String> attrs = new HashMap<>();
                templateAttr.populateMap(attrs, true);
                attrs.put("template", tagTemplate.getTemplateName());
                output.replace(templateAttr, Attributes.generateHTML(attrs));
            }

            //from here, it's the same for a Tag or Page template; preprocess the replaceable properties

            //find and replace all property tags to velocity variables
            List<Element> allPropertyElements = templateHtml.getAllElements(RDF_PROPERTY_ATTR, notEmptyPropertyAttrValue);
            for (Element property : allPropertyElements) {
                this.replaceTemplateProperty(RDF_PROPERTY_ATTR, property, output);
            }
            //now do the same for the non-rdf properties
            List<Element> allDataPropertyElements = templateHtml.getAllElements(NON_RDF_PROPERTY_ATTR, notEmptyPropertyAttrValue);
            for (Element property : allDataPropertyElements) {
                //don't re-do the elements that also have a property attribute (because that one has precedence over the non-rdf attribute)
                if (!allPropertyElements.contains(property)) {
                    this.replaceTemplateProperty(NON_RDF_PROPERTY_ATTR, property, output);
                }
            }

            builder.append(output.toString());

            //replace the source with the (parsed) <template> content
            source = new Source(builder.toString());
        }

        //parse the main html (same for templates and instances)
        OutputDocument output = new OutputDocument(source);
        Collection<HtmlTemplate> allTemplates = templateCache.values();
        for (HtmlTemplate tagTemplate : allTemplates) {

            boolean replaced = false;
            List<net.htmlparser.jericho.Element> templateInstances = source.getAllElements(tagTemplate.getTemplateName());
            for (net.htmlparser.jericho.Element templateInstance : templateInstances) {

                //build the attributes map
                Map<String, String> attributes = new HashMap<>();
                templateInstance.getAttributes().populateMap(attributes, false);

                //copy in all the attributes of the template to the attributes map, except the ones that were already set in the instance
                Attributes templateAttributes = tagTemplate.getAttributes();
                if (templateAttributes != null) {
                    for (Attribute attribute : templateAttributes) {
                        if (!attributes.containsKey(attribute.getName())) {
                            attributes.put(attribute.getName(), attribute.getValue());
                        }
                    }
                }

                //build the properties map
                Map<String, List<String>> properties = new HashMap<>();
                // note: this is a tricky one. It doesn't have to be the immediate children, but we can't cross the "boundary"
                // of another template instance either (otherwise the grandchild-properties would get assigned to the grandparent)
                // In practice, restricting this to the immediate children works pretty well (and neatly conforms to the WebComponents standard)
                List<Element> allImmediatePropertyElements = templateInstance.getChildElements();
                for (Element immediateChild : allImmediatePropertyElements) {
                    //since (for our template system) the RDF and non-RDF attributes are equal, we can treat them the same way
                    String name = immediateChild.getAttributeValue(RDF_PROPERTY_ATTR);
                    //note that this will be skipped when a valid RDF-attr is found (so if both are present, the RDF one has priority)
                    if (name==null) {
                        name = immediateChild.getAttributeValue(NON_RDF_PROPERTY_ATTR);
                    }
                    if (name!=null) {
                        String value = immediateChild.toString();

                        // this list allows us to specify multiple properties with the same name in the instance
                        List<String> values = properties.get(name);
                        if (values==null) {
                            properties.put(name, values = new ArrayList<String>());
                        }
                        values.add(value);
                    }
                }

                //now start building the new tag
                StringBuilder builder = new StringBuilder();

                //render the start tag, except if the template is eg. a page template
                if (tagTemplate.renderTemplateTag()) {
                    builder.append("<").append(tagTemplate.getTemplateName());

                    //the optional attributes
                    if (!attributes.isEmpty()) {
                        builder.append(Attributes.generateHTML(attributes));
                    }
                    //close the start tag
                    builder.append(">").append("\n");
                }

                //the 'body' starts here

                //push the controller with the tag-attributes as arguments
                builder.append("#").append(TemplateInstanceStackDirective.NAME).append("(").append(TemplateInstanceStackDirective.Action.STACK.ordinal()).append(",\"").append(tagTemplate.getTemplateName()).append("\"");
                for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                    String value = attribute.getValue();
                    builder.append(",\"").append(attribute.getKey()).append("\",\"").append(value == null? "" : value).append("\"");
                }
                builder.append(")").append("\n");

                //define the properties in the context
                for (Map.Entry<String, List<String>> property : properties.entrySet()) {
                    //this allows us to assign multiple tags to a single property key
                    List<String> values = property.getValue();
                    for (String value : values) {
                        builder.append("#").append(TemplateInstanceStackDirective.NAME).append("(").append(TemplateInstanceStackDirective.Action.DEFINE.ordinal()).append(",\"")
                               .append(property.getKey()).append("\")").append(value).append("#end").append("\n");
                    }
                }

                // embedding the code chunk in the file (instead of linking to it) is a lot faster, but messes up the auto-cache-reload in dev mode
                if (R.configuration().getProduction()) {
                    //by passing along the resourceloader (and enabling postprocessing and using the parsed path), we implement recursion
                    builder.append(new String(Files.readAllBytes(R.resourceLoader().getResource(tagTemplate.getRelativePath().toString(), true).getParsedPath())));
                }
                else {
                    //use a classic parse to parse the defines from above
                    builder.append("#parse('").append(tagTemplate.getRelativePath()).append("')").append("\n");
                }

                //pop the controller
                builder.append("#end").append("\n");

                //the end tag
                if (tagTemplate.renderTemplateTag()) {
                    builder.append(templateInstance.getEndTag());
                }

                //replace the tag in the output with it's instance
                output.replace(templateInstance, builder);
                replaced = true;
            }

            if (replaced) {
                //if we don't do this, and the inner tag templates get processed first, it will be overwritten by the output of the outer tag template
                source = new Source(output.toString());
                output = new OutputDocument(source);
            }
        }

        // if we're not dealing with a template (but a regular html file), make sure we wrap all styles and scripts so we can perform a match-and-mix
        // note that the "true" argument in the directives means it really needs to end up in the html (after registering it)
        if (!isTagTemplate) {
            List<Element> elements = TagTemplate.getInlineStyles(source);
            for (Element element : elements) {
                StringBuilder builder = new StringBuilder();
                builder.append("#").append(TagTemplateInlineStyleResourceDirective.NAME).append("(true)").append(element.toString()).append("#end");
                output.replace(element, builder.toString());
            }

            elements = TagTemplate.getExternalStyles(source);
            for (Element element : elements) {
                StringBuilder builder = new StringBuilder();
                builder.append("#").append(TagTemplateExternalStyleResourceDirective.NAME).append("(true,'").append(element.getAttributeValue("href")).append("')").append(element.toString()).append("#end");
                output.replace(element, builder.toString());
            }

            elements = TagTemplate.getInlineScripts(source);
            for (Element element : elements) {
                StringBuilder builder = new StringBuilder();
                builder.append("#").append(TagTemplateInlineScriptResourceDirective.NAME).append("(true)").append(element.toString()).append("#end");
                output.replace(element, builder.toString());
            }

            elements = TagTemplate.getExternalScripts(source);
            for (Element element : elements) {
                StringBuilder builder = new StringBuilder();
                builder.append("#").append(TagTemplateExternalScriptResourceDirective.NAME).append("(true,'").append(element.getAttributeValue("src")).append("')").append(element.toString()).append("#end");
                output.replace(element, builder.toString());
            }
        }

        return output.toString();
    }
    private void replaceTemplateProperty(String attribute, Element property, OutputDocument output)
    {
        String name = property.getAttributeValue(attribute);
        String value = property.toString();
        StringBuilder sb = new StringBuilder();
        //if there's no property active in this context, use the (default) value of the template
        sb.append("#if(!$").append(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE).append("['").append(name).append("'])");
        sb.append(property.toString());
        sb.append("#{else}");
        sb.append("$").append(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE).append("['").append(name).append("']");
        sb.append("#end");

        output.replace(property, sb);
    }
    private static void searchAllTemplates(TemplateCache templateCache) throws Exception
    {
        //start with a clean slate
        templateCache.clear();

        //TODO clean this up
        List<ResourceSearchResult> htmlFiles = new ArrayList<>();
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/templates/**.{html,htm}"));
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/views/**.{html,htm}"));
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/assets/imports/**.{html,htm}"));
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/imports/**.{html,htm}"));

        for (ResourceSearchResult htmlFile : htmlFiles) {
            try (Reader reader = Files.newBufferedReader(htmlFile.getResource(), Charset.forName(Charsets.UTF_8.name()))) {
                Path absolutePath = htmlFile.getResource();
                //note the toString(); it works around files found in jar files and throwing a ProviderMismatchException
                Path relativePath = Paths.get("/").resolve(htmlFile.getResourceFolder().relativize(htmlFile.getResource()).toString());
                Source source = new Source(reader);
                HtmlTemplate template = HtmlTemplate.create(source, absolutePath, relativePath);
                if (template!=null) {
                    templateCache.put(absolutePath, template);
                }
            }
            catch (Exception e) {
                Logger.error("Exception caught while parsing a possible tag template file; "+htmlFile, e);
            }
        }
    }

    //-----PRIVATE CLASSES-----
}
