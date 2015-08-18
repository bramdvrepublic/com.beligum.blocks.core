package com.beligum.blocks.templating.blocks;

import com.beligum.base.resources.Asset;
import com.beligum.base.resources.ResourceDescriptor;
import com.beligum.base.resources.ResourceSearchResult;
import com.beligum.base.resources.parsers.AbstractAssetParser;
import com.beligum.base.resources.parsers.AssetParser;
import com.beligum.base.resources.parsers.results.ParseResult;
import com.beligum.base.resources.parsers.results.StringParseResult;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.templating.blocks.directives.PageTemplateWrapperDirective;
import com.beligum.blocks.templating.blocks.directives.TagTemplateResourceDirective;
import com.beligum.blocks.templating.blocks.directives.TemplateInstanceStackDirective;
import net.htmlparser.jericho.*;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by bram on 5/16/15.
 * <p/>
 * TODO: replace property values not only on the string name of the property, but also
 * keep a refernece to prefixes or the default vocab. (e.g. property="pagetitle" and
 * property="http://www.mot.be/ontology/pagetitle" and property="mot:pagetitle" could all reference the same
 * property.
 */
public class HtmlParser extends AbstractAssetParser
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private static final Pattern notEmptyPropertyAttrValue = Pattern.compile(".+");
    //this is the name of the RDFa property attribute (eg. the one that plays a role while parsing RDFa)
    public static final String RDF_PROPERTY_ATTR = "property";
    //this is the name of the property attribute that can be used in the template system, but doesn't 'mean' anything RDFa-wise
    public static final String NON_RDF_PROPERTY_ATTR = "data-property";

    //-----CONSTRUCTORS-----
    public HtmlParser()
    {
    }

    //-----PUBLIC METHODS-----
    /**
     * This method is executed for all *.html files requested by the client. It should be optimized for speed but the result
     * is cached by the ResourceManager, so in production mode, the speed-importance of this method is relative.
     * However; there is currently a MAJOR issue with StringTemplates (see the other parse() method) where every request
     * is parsed through this method; BAD and a very big TODO
     *
     * @param src
     * @param args
     * @return
     * @throws IOException
     */
    @Override
    public ParseResult parse(ResourceDescriptor src, Map<String, String> args) throws IOException
    {
        try {
            Path sourcePath = src.getResolvedPath();
            String rawSource = new String(Files.readAllBytes(sourcePath));

            //allows for the ResourceManager to cache the result of doParse()
            return new StringParseResult(sourcePath, this.doParse(sourcePath, rawSource), this.getCacheFile(sourcePath), Asset.MimeType.HTML);
        }
        catch (Exception e) {
            throw new IOException("Caught exception while parsing html file", e);
        }
    }
    /**
     * This is a temporary workaround function to (also) make StringTemplates work; see com.beligum.base.templating.velocity.VelocityStringTemplate
     * It basically parses every request through the doParse() method, regardless of any caching.
     *
     * @param rawSource
     * @return
     * @throws Exception
     */
    public String parse(String rawSource) throws Exception
    {
        return this.doParse(null, rawSource);
    }
    /**
     * Strips all lines beginning with "##" (starting from that position) and returns all the rest.
     *
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
    public static TemplateCache getTemplateCache()
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
    public void resetTemplateCache() throws IOException
    {
        //needed for the html files, because they're not postprocessed in the ResouceManager and changes are not detected throug the hash system
        AssetParser htmlParser = R.resourceLoader().getAssetParserFor(Asset.MimeType.HTML);
        for (HtmlTemplate htmlTemplate : HtmlParser.getTemplateCache().values()) {
            Path cacheFile = this.getCacheFile(htmlTemplate.getAbsolutePath());
            if (cacheFile!=null) {
                Files.deleteIfExists(cacheFile);
            }
        }

        R.cacheManager().getApplicationCache().put(CacheKeys.TAG_TEMPLATES, null);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private String doParse(Path sourcePath, String rawSource) throws Exception
    {
        //the solves a lot of issues with inactive lines
        String sourceStr = eatVelocityComments(rawSource);
        Source source = new Source(sourceStr);

        boolean htmlPage = source.getFirstElement("html") != null;
        HtmlTemplate sourceTemplate = null;
        // if we're dealing with a template (eg. the file is a template, not an instance of a template), replace the source with the html in the template
        TemplateCache templateCache = this.getTemplateCache();
        if (sourcePath != null && templateCache.containsKey(sourcePath)) {
            //first of all, since this method is only called when something changed, update the cache value
            HtmlTemplate oldTemplate = templateCache.get(sourcePath);//fetch the old value for the paths
            sourceTemplate = HtmlTemplate.create(oldTemplate.getTemplateName(), source, oldTemplate.getAbsolutePath(), oldTemplate.getRelativePath(), oldTemplate.getParent());
            templateCache.put(sourcePath, sourceTemplate);

            //this is the base for all coming preprocessing
            //note that we only use the inner html, the prefix and suffix will be rendered out before and after every instance (see loops below)
            Segment templateHtml = sourceTemplate.getInnerHtml();
            OutputDocument output = new OutputDocument(templateHtml);

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

            //replace the source with the (parsed) <template> content
            source = new Source(output.toString());
        }

        //parse the main html (same for templates and instances)
        OutputDocument output = new OutputDocument(source);
        Collection<HtmlTemplate> allTemplates = templateCache.values();
        for (HtmlTemplate htmlTemplate : allTemplates) {

            boolean replaced = false;
            List<net.htmlparser.jericho.Element> templateInstances = source.getAllElements(htmlTemplate.getTemplateName());
            for (net.htmlparser.jericho.Element templateInstance : templateInstances) {

                //build the attributes map
                Map<String, String> attributes = new LinkedHashMap<>();
                templateInstance.getAttributes().populateMap(attributes, false);

                //copy in all the attributes of the template to the attributes map, except the ones that were already set in the instance
                Map<String, String> templateAttributes = htmlTemplate.getAttributes();
                if (templateAttributes != null) {
                    for (Map.Entry<String, String> attribute : templateAttributes.entrySet()) {
                        if (!attributes.containsKey(attribute.getKey())) {
                            attributes.put(attribute.getKey(), attribute.getValue());
                        }
                    }
                }

                //build the properties map
                //note: we want to save the Velocity codes, used inside a template instance, eg:
                //     <tag-template-name>
                // -->     #foreach ($val in $values)
                //             <div property="value">$val</div>
                // -->     #end
                //     </tag-template-name>
                // so instead of iterating over the child-elements, we iterate over all nodes, saving "other" text for future use
                List<Token> properties = new ArrayList<>();
                Map<String, PropertyToken> propertyRefs = new HashMap<>();
                // note: this is a tricky one. It doesn't have to be the immediate children, but we can't cross the "boundary"
                // of another template instance either (otherwise the grandchild-properties would get assigned to the grandparent)
                // In practice, restricting this to the immediate children works pretty well (and neatly conforms to the WebComponents standard)
                Iterator<Segment> iter = templateInstance.getContent().getNodeIterator();
                while (iter.hasNext()) {
                    Segment seg = iter.next();

                    //if the segment is a tag, parse the element
                    if (seg instanceof StartTag) {
                        StartTag startTag = ((StartTag) seg);
                        Element immediateChild = startTag.getElement();

                        // Note: this first check check if the tag (eg. a <link> tag that's not closed; eg. that's not <link/>)
                        // occurs. For more details, see http://jericho.htmlparser.net/docs/javadoc/net/htmlparser/jericho/Element.html
                        boolean isVoidTag = startTag.equals(immediateChild);

                        //skip the entire tree of the element, we'll handle it now
                        // but watch out: an <img> element doens't have and end tag, so check for null
                        if (!isVoidTag) {
                            if (immediateChild.getEndTag() != null) {
                                while (iter.hasNext() && !iter.next().equals(immediateChild.getEndTag()))
                                    ;
                            }
                        }

                        //since (for our template system) the RDF and non-RDF attributes are equal, we can treat them the same way
                        String name = immediateChild.getAttributeValue(RDF_PROPERTY_ATTR);
                        //note that this will be skipped when a valid RDF-attr is found (so if both are present, the RDF one has priority)
                        if (name == null) {
                            name = immediateChild.getAttributeValue(NON_RDF_PROPERTY_ATTR);
                        }
                        if (name != null) {
                            String value = immediateChild.toString();

                            // this list allows us to specify multiple properties with the same name in the instance
                            PropertyToken values = propertyRefs.get(name);
                            if (values == null) {
                                propertyRefs.put(name, values = new PropertyToken(name));
                                //note that this will 'eat up' all same properties that are coming in this tag
                                properties.add(values);
                            }
                            values.add(value);
                        }
                    }
                    //if the segment is something else, save it in the right order for later use
                    else {
                        // if we encounter a resource wrapper diretive, the tag inside will be a html tag,
                        // so we need to make sure it doesn't get eaten because there's no property attribute set
                        // on it.
                        String segStr = seg.toString().trim();
                        if (segStr.startsWith("#" + TagTemplateResourceDirective.NAME)) {
                            StringBuilder resource = new StringBuilder(seg.toString());
                            while (iter.hasNext() && !segStr.equals("#end")) {
                                seg = iter.next();
                                segStr = seg.toString().trim();
                                resource.append(seg.toString());
                            }
                            segStr = resource.toString();
                        }
                        properties.add(new OtherToken(segStr));
                    }
                }

                //now start building the new tag
                StringBuilder builder = new StringBuilder();

                builder.append(htmlTemplate.getPrefixHtml());

                //render the start tag, except if the template is eg. a page template
                String renderTag = !htmlTemplate.renderTemplateTag() ? null : htmlTemplate.getTemplateName();
                if (renderTag!=null) {
                    String attr = templateInstance.getAttributeValue(HtmlTemplate.ATTRIBUTE_RENDER_TAG);
                    //if it's not set, just render the tag name
                    if (attr!=null) {
                        //NOTE: if this is set to empty, don't render the tag (and it's attributes) at all
                        if (attr.equals("")) {
                            renderTag = null;
                        }
                        else {
                            renderTag = attr;
                        }
                    }
                }


                if (renderTag!=null) {
                    builder.append("<").append(renderTag);

                    //the optional attributes
                    if (!attributes.isEmpty()) {
                        builder.append(Attributes.generateHTML(attributes));
                    }
                    //close the start tag
                    builder.append(">").append("\n");
                }

                //the 'body' starts here

                //push the controller with the tag-attributes as arguments
                builder.append("#").append(TemplateInstanceStackDirective.NAME).append("(").append(TemplateInstanceStackDirective.Action.STACK.ordinal()).append(",\"")
                       .append(htmlTemplate.getTemplateName()).append("\"");
                for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                    String value = attribute.getValue();
                    builder.append(",\"").append(attribute.getKey()).append("\",\"").append(value == null ? "" : value).append("\"");
                }
                builder.append(")").append("\n");

                //define the properties in the context
                for (Token token : properties) {
                    //see above: if we have a value, it's a proper property
                    if (token instanceof PropertyToken) {
                        PropertyToken propertyToken = (PropertyToken)token;
                        //this allows us to assign multiple tags to a single property key
                        for (String value : ((PropertyToken) token).getValues()) {
                            builder.append("#").append(TemplateInstanceStackDirective.NAME).append("(").append(TemplateInstanceStackDirective.Action.DEFINE.ordinal()).append(",\"")
                                   .append(propertyToken.getProperty()).append("\")").append(value).append("#end").append("\n");
                        }
                    }
                    //otherwise it's something else, stored in the key
                    else {
                        builder.append(token.getValue());
                    }
                }

                // embedding the code chunk in the file (instead of linking to it) is a lot faster, but messes up the auto-cache-reload in dev mode
                if (R.configuration().getProduction()) {
                    //by passing along the resourceloader (and enabling postprocessing and using the parsed path), we implement recursion
                    builder.append(new String(Files.readAllBytes(R.resourceLoader().getResource(htmlTemplate.getRelativePath().toString(), true).getParsedPath())));
                }
                else {
                    //use a classic parse to parse the defines from above
                    builder.append("#parse('").append(htmlTemplate.getRelativePath()).append("')").append("\n");
                }

                //pop the controller
                builder.append("#end").append("\n");

                //the end tag
                if (renderTag!=null) {
                    builder.append("</").append(renderTag).append(">");
                }

                //the suffix html (mostly empty)
                builder.append(htmlTemplate.getSuffixHtml());


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

        StringBuilder retVal = new StringBuilder();
        if (htmlPage) {
            retVal.append("#").append(PageTemplateWrapperDirective.NAME).append("()").append("\n");
            retVal.append(output.toString());
            retVal.append("#end");
        }
        else {
            retVal.append(output.toString());
        }

        return retVal.toString();
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
        // This is something special:
        // If we have multiple properties, eg. like this:
        //     <meta property="description" content="Double tag test Nederlands" lang="nl">
        //     <meta property="description" content="Double tag test English" lang="en">
        // These will get serialized into an array object (actually a special PropertyArray that spits out the joined string), in the correct order
        // (see above, specifically in TemplateInstanceStackDirective). But when we would spit out that array for every occurrence of this array variable,
        // the output would get doubled (or more). By ensuring we only write one entry per one, with a special method, we keep the order and don't get doubles.
        //
        sb.append("$").append(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE).append("['").append(name).append("']." + PropertyArray.WRITE_ONCE_METHOD_NAME + "()");
        sb.append("#end");

        output.replace(property, sb);
    }
    private static void searchAllTemplates(TemplateCache templateCache) throws Exception
    {
        //start with a clean slate
        templateCache.clear();

        List<ResourceSearchResult> htmlFiles = new ArrayList<>();
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/imports/**.{html,htm}"));

        // first, we'll keep a reference to all the templates with the same name in the path
        // they're returned priority-first, so the parents and grandparents will end up deepest in the list
        Map<String, List<Path[]>> inheritanceTree = new HashMap<>();
        for (ResourceSearchResult htmlFile : htmlFiles) {
            Path absolutePath = htmlFile.getResource();
            //note the toString(); it works around files found in jar files and throwing a ProviderMismatchException
            Path relativePath = Paths.get("/").resolve(htmlFile.getResourceFolder().relativize(htmlFile.getResource()).toString());
            String templateName = HtmlTemplate.parseTemplateName(relativePath);

            List<Path[]> entries = inheritanceTree.get(templateName);
            if (entries == null) {
                inheritanceTree.put(templateName, entries = new ArrayList<>());
            }

            //let's keep both so we don't have to recalculate
            entries.add(new Path[] { absolutePath, relativePath });
        }

        // now iterate the list in reverse order to parse the grandparents first
        // and be able to pass them to the overloading children
        for (Map.Entry<String, List<Path[]>> entry : inheritanceTree.entrySet()) {
            String templateName = entry.getKey();
            List<Path[]> inheritList = entry.getValue();
            HtmlTemplate parent = null;
            for (int i = inheritList.size() - 1; i >= 0; i--) {
                Path[] absRelArr = inheritList.get(i);;
                Path absolutePath = absRelArr[0];
                Path relativePath = absRelArr[1];

                try (Reader reader = Files.newBufferedReader(absolutePath, Charset.forName(Charsets.UTF_8.name()))) {
                    Source source = new Source(reader);
                    HtmlTemplate template = HtmlTemplate.create(templateName, source, absolutePath, relativePath, parent);

                    if (template != null) {
                        // Note: because this will return the files in priority order, don't overwrite an already parsed template,
                        // because we want to be able to 'overload' the files with our priority system.
                        // we're iterating in reverse order for the parent system, so keep the 'last' = last (grand)child
                        if (i==0) {
                            templateCache.put(absolutePath, template);
                        }

                        //this can be used by the next loop
                        parent = template;
                    }
                    else {
                        Logger.warn("Encountered null-valued html template after parsing it; " + absolutePath.toString());
                    }
                }
                catch (Exception e) {
                    Logger.error("Exception caught while parsing a possible tag template file; " + absolutePath, e);
                }
            }
        }
    }

    //-----PRIVATE CLASSES-----
    private static abstract class Token
    {
        public abstract String getValue();
    }
    private static class PropertyToken extends Token
    {
        String property;
        List<String> values;
        public PropertyToken(String property)
        {
            this.property = property;
            this.values = new ArrayList<>();
        }
        public void add(String value)
        {
            this.values.add(value);
        }
        public String getProperty()
        {
            return property;
        }
        public List<String> getValues()
        {
            return values;
        }
        //NOT USED, see loops above
        @Override
        public String getValue()
        {
            return null;
        }

        @Override
        public String toString()
        {
            return this.values.toString();
        }
    }
    private static class OtherToken extends Token
    {
        String value;
        public OtherToken(String value)
        {
            this.value = value;
        }

        @Override
        public String getValue()
        {
            return value;
        }

        @Override
        public String toString()
        {
            return value;
        }
    }
}
