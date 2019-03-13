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

package com.beligum.blocks.templating.blocks;

import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ResourceInputStream;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceParser;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.UriDetector;
import com.beligum.blocks.templating.blocks.directives.PageTemplateWrapperDirective;
import com.beligum.blocks.templating.blocks.directives.ResourceUriDirective;
import com.beligum.blocks.templating.blocks.directives.TagTemplateResourceDirective;
import com.beligum.blocks.templating.blocks.directives.TemplateInstanceStackDirective;
import com.google.common.collect.Sets;
import gen.com.beligum.blocks.core.constants.blocks.core;
import net.htmlparser.jericho.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by bram on 5/16/15.
 * <p/>
 * TODO: replace property values not only on the string name of the property, but also
 * keep a reference to prefixes or the default vocab. (e.g. property="pagetitle" and
 * property="http://www.mot.be/ontology/pagetitle" and property="mot:pagetitle" could all reference the same
 * property.
 * <p>
 * Note: the reverse of this class is com.beligum.blocks.templating.blocks.analyzer.HtmlAnalyzer
 */
public class HtmlParser implements ResourceParser, UriDetector.ReplaceCallback
{
    //-----CONSTANTS-----
    private static final String NEWLINE = "\n";
    private static final Pattern notEmptyPropertyAttrValue = Pattern.compile(".+");
    //this is the name of the RDFa property attribute (eg. the one that plays a role while parsing RDFa)
    public static final String RDF_PROPERTY_ATTR = "property";
    //this is the name of the property attribute that can be used in the template system, but doesn't 'mean' anything RDFa-wise
    public static final String NON_RDF_PROPERTY_ATTR = "data-property";

    public static final String RDF_VOCAB_ATTR = "vocab";
    public static final String RDF_TYPEOF_ATTR = "typeof";
    public static final String RDF_ABOUT_ATTR = "about";
    public static final String RDF_RESOURCE_ATTR = "resource";
    public static final String RDF_CONTENT_ATTR = "content";
    public static final String RDF_DATATYPE_ATTR = "datatype";
    public static final String RDF_PREFIX_ATTR = "prefix";
    public static final String HTML_ROOT_ELEM = "html";
    public static final String WEBCOMPONENTS_TEMPLATE_ELEM = "template";
    //the prefix data- makes sure we're W3C compliant
    public static final String HTML_ROOT_TEMPLATE_ATTR = core.Entries.HTML_ROOT_TEMPLATE_ATTR.getValue();
    public static final String HTML_ROOT_ARGS_VARIABLE_NAME = "HTML_TAG_ARGS";

    //used this: http://stackoverflow.com/questions/2725156/complete-list-of-html-tag-attributes-which-have-a-url-value
    public static final Set<String> ALL_SIMPLE_URL_ATTR = Sets.newHashSet(
                    "action",
                    "background",
                    "cite",
                    "classid",
                    "codebase",
                    "data",
                    "dynsrc",
                    "formaction",
                    "href",
                    "icon",
                    "longdesc",
                    "lowsrc",
                    "manifest",
                    "poster",
                    "profile",
                    "src",
                    "usemap"
    );
    public static final Set<String> ALL_COMPLEX_URL_ATTR = Sets.newHashSet(
                    "srcset",
                    "archive",
                    "content",
                    "style"
    );
    public static final Set<String> ALL_URL_ATTR = Sets.union(ALL_SIMPLE_URL_ATTR, ALL_COMPLEX_URL_ATTR);

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public HtmlParser()
    {
    }

    //-----PUBLIC METHODS-----
    @Override
    public boolean test(com.beligum.base.resources.ifaces.Source source, MimeType requestedMimeType)
    {
        return true;
    }
    /**
     * This method is executed for all *.html files requested by the client (during postprocess phase of the ResourceLoader).
     * It should be optimized for speed but the result is cached by the ResourceManager, so in production mode,
     * the speed-importance of this method is relative.
     * <p>
     * Note that it's both used to parse the html templates (eg. files under /imports/...) as regular html with template instances.
     */
    @Override
    public ResourceInputStream parse(com.beligum.base.resources.ifaces.Source source, MimeType requestedMimeType) throws IOException
    {
        ResourceInputStream retVal = null;

        try {
            TemplateCache templateCache = TemplateCache.instance();

            //the default setRollbackOnly count is set to 2, meaning if an attribute throws two errors,
            // it's ignored, which is problematic with all the Velocity tags
            // Should we activate this?
            // -> I think not, it's good that eg. in <html ${blah}> the '${blah}' is skipped,
            //    that said, it's more problematic that eg. in <html${blah}>, the 'html${blah}' will probably be skipped...
            //Attributes.setDefaultMaxErrorCount(10);

            //the solves a lot of issues with inactive lines
            Source htmlSource;
            try (InputStream is = source.newInputStream()) {
                htmlSource = new Source(this.eatTemplateComments(is));
            }

            // this one was a bit problematic: we would like to surround all returning html with a #ptwd directive,
            // but we can't have it twice, so it's kind of hard to detectAndReplace the outer resource where we should add it.
            // Note that we can't really use a detector like 'template instanceof PageTemplate' below because we want
            // regular Velocity templates to be wrapped too (eg. #parse("/templates/main.html"))
            // So we switched to detecting all _source_ (not the output or we'll wrap page templates as well and end up with doubles)
            // resources that contain a <html> tag and hope for the best...
            boolean htmlPage = htmlSource.getFirstElement(HTML_ROOT_ELEM) != null;

            // if we're dealing with a template (eg. the file is a template, not an instance of a template),
            // store the external references and replace the source with the html in the <template> tag
            // Note that, at the moment, the only way we get here, is through the ResourceManager.get() call below
            HtmlTemplate template = templateCache.getByRelativePath(source.getUri().getPath());
            if (template != null) {
                //replace the source with the (parsed) <template> content
                htmlSource = new Source(this.processHtmlTemplate(source, htmlSource, template).toString());
            }

            //call the main processor (for both templates and template-instances)
            OutputDocument output = this.processSource(source, htmlSource);

            StringBuilder sb = new StringBuilder();
            if (htmlPage) {
                sb.append("#").append(PageTemplateWrapperDirective.NAME).append("()").append(NEWLINE);
                sb.append(output.toString());
                sb.append("#end");
            }
            else {
                sb.append(output.toString());
            }

            retVal = new ResourceInputStream(sb.toString());
        }
        catch (Exception e) {
            throw new IOException("Caught exception while parsing html file", e);
        }

        return retVal;
    }
    @Override
    public MimeType[] getSupportedMimeTypes()
    {
        return new MimeType[] {
                        MimeTypes.HTML
        };
    }
    @Override
    public MimeType getParsedMimeType(Resource resource)
    {
        //FIXME maybe this should be VELOCITY instead?
        return MimeTypes.HTML;
    }
    @Override
    public Priority getPriority()
    {
        //keep this high, so it executes before all other HTML parsers
        return Priority.HIGH;
    }
    /**
     * This is the main processor of html content and is called recursively.
     * It iterates all elements of the html source and replaces all template-instances with their parsed version.
     */
    private OutputDocument processSource(com.beligum.base.resources.ifaces.Source source, Source htmlSource) throws Exception
    {
        TemplateCache templateCache = TemplateCache.instance();

        OutputDocument retVal = new OutputDocument(htmlSource);

        //The idea is to traverse the entire html tree and parse/replace all (start tags of) template-instances, both page and tag
        Iterator<Segment> nodes = htmlSource.getNodeIterator();
        while (nodes.hasNext()) {
            Segment node = nodes.next();

            //if the segment is a tag, parse the element
            if (node instanceof StartTag) {
                Element element = ((StartTag) node).getElement();

                //fingerprint URIs in attributes
                this.processUris(element, retVal);

                //check if the element is an instance of a template and replace it with it's parsed version if it's the case
                HtmlTemplate template = templateCache.getByTagName(element.getName());
                if (template != null) {
                    retVal.replace(element, this.processTemplateInstance(source, element, template));
                }
            }
        }

        return retVal;
    }
    /**
     * Do a find/replace on all detected URIs in the element's attributes if fingerprinting is enabled.
     */
    private void processUris(Element element, OutputDocument retVal)
    {
        //for now, we only parse URIs for fingerprinting
        if (R.configuration().getResourceConfig().getEnableFingerprintedResources() && R.configuration().getResourceConfig().getEnableFingerprintedTemplates()) {

            //skip resource elements, the URI will be wrapped/parsed more extensively because of packing and inlining; see HtmlTemplate.buildResourceHtml()
            if (!HtmlTemplate.isResourceElement(element)) {
                Attributes attributes = element.getAttributes();
                if (attributes != null) {
                    for (Attribute att : attributes) {
                        String val = att.getValue();
                        if (!StringUtils.isEmpty(val)) {
                            //Note that we used to split up the simple and complex attributes here and treat them differently
                            // with a pre-validate step for the simple attributes (using UriBuilder.fromUri()) that escapes (invalid) spaces in URIs and so on,
                            // but because I forgot simple attributes can contain templating code, it's not always that easy to just take the entire
                            // attribute and try to parse it to a URI. Now, we fallback to detectAllResourceUris() instead.
                            String name = att.getName().toLowerCase();
                            if (ALL_URL_ATTR.contains(name)) {
                                retVal.replace(att, att.getName() + "=\"" + R.resourceManager().getFingerprinter().detectAllResourceUris(val, this) + "\"");
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * Implemented interface callback for the fingerprinter call above
     */
    @Override
    public String uriDetected(String uriStr)
    {
        //never return null; if nothing was found, the uri was probably an external one
        String retVal = uriStr;

        //if the endpoint is immutable, we'll generate our fingerprint right now,
        //if not, we'll wrap the URI in a directive to re-parse it on every request.
        //Note: this means we won't do any other post-processing next to fingerprinting in that directive anymore,
        //if that would change, we must wipe this optimization step
        Resource resource = R.resourceManager().get(URI.create(uriStr));

        //this means we're dealing with a local uri; we only support fingerprinting of local resources
        if (resource != null) {
            if (resource.isImmutable()) {
                retVal = resource.getFingerprintedUri().toString();
            }
            else {
                //this means we'll postpone the fingerprinting of the URI to the render phase, just wrap it in our fingerprint directive
                retVal = new StringBuilder("#").append(ResourceUriDirective.NAME).append("('").append(uriStr).append("')").toString();
            }
        }

        return retVal;
    }
    /**
     * Process an instance of a (page or tag) template to a HTML string
     */
    private StringBuilder processTemplateInstance(com.beligum.base.resources.ifaces.Source source, Element templateInstance, HtmlTemplate htmlTemplate) throws Exception
    {
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

        //After parsing the attributes, we iterate the body of the element to build the properties map
        //note: we want to save the Velocity codes, used inside a template instance, eg:
        //     <tag-template-name>
        // -->     #foreach ($val in $values)
        //             <div property="value">$val</div>
        // -->     #end
        //     </tag-template-name>
        // so instead of iterating over the child-elements, we iterate over all nodes, saving "other" text for future use
        List<Token> properties = new ArrayList<>();
        Map<String, PropertyToken> propertyRefs = new HashMap<>();

        //Create and initialize the RDF context with the root element attributes
        HtmlRdfContext rdfContext = new HtmlRdfContext(source.getUri());
        //Note: this means we initialize the (root of the) template _instance_ with the root of the template itself
        rdfContext.updateContext(htmlTemplate.getRootElement().getStartTag());

        // note: this is a tricky one. It doesn't have to be the immediate children, but we can't cross the "boundary"
        // of another template instance either (otherwise the grandchild-properties would get assigned to the grandparent)
        // In practice, restricting this to the immediate children works pretty well and neatly conforms to the WebComponents standard
        Iterator<Segment> iter = templateInstance.getContent().getNodeIterator();
        while (iter.hasNext()) {
            Segment seg = iter.next();

            //if the segment is a tag, parse the element
            if (seg instanceof StartTag) {
                StartTag startTag = ((StartTag) seg);
                Element child = startTag.getElement();

                // Note: this first check check if the tag (eg. a <link> tag that's not closed; eg. that's not <link/>)
                // occurs. For more details, see http://jericho.htmlparser.net/docs/javadoc/net/htmlparser/jericho/Element.html
                boolean isVoidTag = startTag.equals(child);

                //skip the entire tree of the element, we'll handle it right here
                // but watch out: an <img> element doens't have and end tag, so check for null
                if (!isVoidTag) {
                    if (child.getEndTag() != null) {
                        while (iter.hasNext() && !iter.next().equals(child.getEndTag())) ;
                    }
                }

                rdfContext.updateContext(startTag);

                //since (for our template system) the RDF and non-RDF attributes are equal, we can treat them the same way
                String name = child.getAttributeValue(RDF_PROPERTY_ATTR);
                //note that this will be skipped when a valid RDF-attr is found (so if both are present, the RDF one has priority)
                if (name == null) {
                    name = child.getAttributeValue(NON_RDF_PROPERTY_ATTR);
                }
                if (name != null) {

                    name = this.parsePropertyName(name, startTag, rdfContext);

                    // this list allows us to specify multiple properties with the same name in the instance
                    PropertyToken values = propertyRefs.get(name);
                    if (values == null) {
                        propertyRefs.put(name, values = new PropertyToken(name));
                        //note that this will 'eat up' all same properties that are coming in this tag
                        properties.add(values);
                    }
                    values.add(this.processSource(source, new Source(child)).toString());
                }
            }
            //if the segment is something else (than a start tag), save it in the right order for later use
            else {

                if (seg instanceof EndTag) {
                    rdfContext.updateContext((EndTag) seg);
                }

                // if we encounter a resource wrapper directive, the tag inside will be a html tag,
                // so we need to make sure it doesn't get eaten because there's no property attribute set
                // on it.
                String segStr = seg.toString().trim();
                if (segStr.startsWith("#" + TagTemplateResourceDirective.NAME)) {
                    StringBuilder res = new StringBuilder(seg.toString());
                    while (iter.hasNext() && !segStr.equals("#end")) {
                        seg = iter.next();
                        segStr = seg.toString().trim();
                        res.append(seg.toString());
                    }
                    segStr = res.toString();
                }
                properties.add(new OtherToken(segStr));
            }
        }

        //now start building the new tag
        StringBuilder builder = new StringBuilder();

        builder.append(htmlTemplate.getPrefixHtml());

        //render the start tag, except if the template is eg. a page template
        String renderTag = !htmlTemplate.renderTemplateTag() ? null : htmlTemplate.getTemplateName();
        if (renderTag != null) {

            String attr = templateInstance.getAttributeValue(HtmlTemplate.ATTRIBUTE_RENDER_TAG);

            //note: this is subtle; it means we'll only consider the data-render-tag on the htmlTemplate (the definition file)
            //if no such attribute is present on the instance (which is probably what is expected; the instances overrides the definition)
            if (attr == null) {
                Map<String, String> attrs = htmlTemplate.getAttributes();
                if (attrs != null) {
                    attr = attrs.get(HtmlTemplate.ATTRIBUTE_RENDER_TAG);
                }
            }

            //if it's not set, just render the tag name
            if (attr != null) {
                //NOTE: if this is set to empty, don't render the tag (and it's attributes) at all
                if (attr.equals("")) {
                    renderTag = null;
                }
                else {
                    renderTag = attr;
                }
            }
        }

        //the 'body' starts here

        if (renderTag != null) {
            builder.append("<").append(renderTag);

            //the optional attributes
            if (!attributes.isEmpty()) {
                builder.append(Attributes.generateHTML(attributes));
            }
            //close the start tag
            builder.append(">").append(NEWLINE);
        }
        // page templates are never rendered out (because their 'instance' is actually the <html> element)
        // but we use a little trick to render the arguments of their instance tag: we set a special velocity variable
        // that is added to the <html> tag (eg. <html $!HTML_TAG_ARGS>)
        else if (htmlTemplate instanceof PageTemplate) {
            if (!attributes.isEmpty()) {
                //quick loop to remove empty attributes (we want as little as possible in the <html> tag)
                Iterator<Map.Entry<String, String>> attIter = attributes.entrySet().iterator();
                while (attIter.hasNext()) {
                    if (StringUtils.isEmpty(attIter.next().getValue())) {
                        attIter.remove();
                    }
                }

                //we use a regular #define because we don't have a stack context yet (not very problematic because we only have one page template per page)
                if (!attributes.isEmpty()) {
                    //note: don't worry about a leading space that separates the value of the variable with the "html" word in the <html> tag, because generateHTML generates it
                    builder.append("#define($").append(HTML_ROOT_ARGS_VARIABLE_NAME).append(")").append(Attributes.generateHTML(attributes)).append("#end").append(NEWLINE);
                }
            }
        }

        // push the controller with the tag-attributes as arguments
        // note that we want to use the controller inside the <template> tag, so make sure it comes before the start tag
        builder.append("#").append(TemplateInstanceStackDirective.NAME).append("(").append(TemplateInstanceStackDirective.Action.STACK.ordinal()).append(",\"")
               .append(htmlTemplate.getTemplateName()).append("\"");
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            String value = attribute.getValue();
            //don't forget to escape quotes in the attribute values cause eg. this is a valid situation: style="background-image: url(&quot;/webhdfs/v1/mission_bg.png&quot;);"
            //and it results in the &quot; to be converted back to ", breaking the structure of the parsed html,
            //so I guess the best thing to do is to convert it back to &quot;
            String safeValue = value == null ? "" : value.replace("\"", "&quot;");
            builder.append(",\"").append(attribute.getKey()).append("\",\"").append(safeValue).append("\"");
        }
        builder.append(")").append(NEWLINE);

        //define the properties in the context
        for (Token token : properties) {
            //see above: if we have a value, it's a proper property
            if (token instanceof PropertyToken) {
                PropertyToken propertyToken = (PropertyToken) token;
                //this allows us to assign multiple tags to a single property key
                for (String value : ((PropertyToken) token).getValues()) {
                    builder.append("#").append(TemplateInstanceStackDirective.NAME).append("(").append(TemplateInstanceStackDirective.Action.DEFINE.ordinal()).append(",\"")
                           .append(propertyToken.getProperty()).append("\")").append(value).append("#end").append(NEWLINE);
                }
            }
            //otherwise it's something else, stored in the key
            else {
                builder.append(token.getValue());
            }
        }

        //fetch the template html through our resource system (which will end up here recursively if it needs to be parsed first)
        Resource htmlTemplateResource = R.resourceManager().get(htmlTemplate.getRelativePath().toUri());
        if (htmlTemplateResource == null) {
            throw new IOException("Encountered a html template that doesn't seem to exist. This shouldn't happen. " + htmlTemplate.getRelativePath());
        }
        else {
            //wire both resources together so the update mechanism works in dev mode
            //TODO we should factor away the casting...
            if (source instanceof Resource) {
                ((Resource) source).addChild(htmlTemplateResource);
            }
        }

        //inline the resource into the builder
        try (InputStream is = htmlTemplateResource.newInputStream()) {
            builder.append(IOUtils.toString(is));
        }

        //pop the controller
        builder.append("#end").append(NEWLINE);

        //the end tag
        if (renderTag != null) {
            builder.append("</").append(renderTag).append(">");
        }

        //the suffix html (mostly empty)
        builder.append(htmlTemplate.getSuffixHtml());

        return builder;
    }
    /**
     * this will allow us to use template variables (eg. $CONSTANTS) in property attributes
     * this will also normalize (expand) the property value to it's full URI, based on the supplied context
     */
    private String parsePropertyName(String name, StartTag tag, HtmlRdfContext rdfContext) throws IOException
    {
        name = name.trim();

        if (name.startsWith(R.resourceManager().getTemplateEngine().getVariablePrefix())) {
            name = this.renderTemplateValue(name);
        }

        try {
            return rdfContext.normalizeProperty(name);
        }
        //catch and add some additional debug information about the tag
        catch (IOException e) {
            throw new IOException("Error happended while parsing property attribute '" + name + "' of tag " + tag, e);
        }
    }
    /**
     * (Possibly) substitute a variable by it's (current) value (for the currently optimal locale)
     */
    private String renderTemplateValue(String value) throws IOException
    {
        return R.resourceManager().newTemplate(new StringSource(value, MimeTypes.HTML, R.i18n().getOptimalLocale())).render();
    }
    /**
     * Process the html tag/page template (not an instance, the real source template) and generate it's html
     */
    private OutputDocument processHtmlTemplate(com.beligum.base.resources.ifaces.Source source, Source htmlSource, HtmlTemplate template) throws Exception
    {
        //first of all, since this method is only called when something changed, update the cache value
        //(note we can use the old template value for the paths)
        HtmlTemplate sourceTemplate = TemplateCache.instance().update(template, htmlSource);

        //this is the base for all coming preprocessing
        //note that we only use the inner html, the prefix and suffix will be rendered out before and after every instance (see loops below)
        Segment templateHtml = sourceTemplate.getInnerHtml();
        OutputDocument output = new OutputDocument(templateHtml);

        //the same for a Tag or Page template; preprocess the replaceable properties

        //this will hold the RDF context while we parse our html elements
        HtmlRdfContext rdfContext = new HtmlRdfContext(source.getUri());

        //initialize the context with the root element attributes
        rdfContext.updateContext(sourceTemplate.getRootElement().getStartTag());

        for (Iterator<Segment> nodeIterator = templateHtml.getNodeIterator(); nodeIterator.hasNext(); ) {
            Segment nodeSegment = nodeIterator.next();
            if (nodeSegment instanceof StartTag) {
                StartTag startTag = (StartTag) nodeSegment;

                Attributes attributes = startTag.getAttributes();
                if (attributes != null) {

                    rdfContext.updateContext(startTag);

                    Attribute propertyAttr = attributes.get(RDF_PROPERTY_ATTR);
                    if (propertyAttr != null) {
                        this.replaceTemplateProperty(htmlSource, sourceTemplate, startTag, propertyAttr, output, rdfContext);
                    }

                    Attribute dataPropertyAttr = attributes.get(NON_RDF_PROPERTY_ATTR);
                    if (dataPropertyAttr != null) {
                        if (propertyAttr == null) {
                            this.replaceTemplateProperty(htmlSource, sourceTemplate, startTag, dataPropertyAttr, output, rdfContext);
                        }
                        else {
                            Logger.warn("Not using attribute '" + NON_RDF_PROPERTY_ATTR + "' of tag " + startTag.toString() + " because there's also a '" + RDF_PROPERTY_ATTR +
                                        "' attribute set that has precedence because it's an official RDFa attribute");
                        }
                    }
                }
            }
            else if (nodeSegment instanceof EndTag) {
                EndTag endTag = (EndTag) nodeSegment;

                rdfContext.updateContext(endTag);
            }
        }

        return output;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void replaceTemplateProperty(Source source, HtmlTemplate template, StartTag tag, Attribute attribute, OutputDocument output, HtmlRdfContext rdfContext)
                    throws Exception
    {
        //this will clean, parse and normalize the property value
        String fullName = this.parsePropertyName(attribute.getValue(), tag, rdfContext);

        StringBuilder sb = new StringBuilder();
        //if there's no property active in this context, use the (default) value of the template
        sb.append("#if(!$").append(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE).append("['").append(fullName).append("'])");
        sb.append(tag.getElement().toString());
        sb.append("#{else}");
        // This is something special:
        // If we have multiple properties, eg. like this:
        //     <meta property="description" content="Double tag Nederlands" lang="nl">
        //     <meta property="description" content="Double tag English" lang="en">
        // These will get serialized into an array object (actually a special PropertyArray that spits out the joined string), in the correct order
        // (see above, specifically in TemplateInstanceStackDirective). But when we would spit out that array for every occurrence of this array variable,
        // the output would get doubled (or more). By ensuring we only write one entry per one, with a special method, we keep the order and don't get doubles.
        //
        // Note that this construct will first check if it's an instance of the special class by testing a specific boolean (PROPARR_FIELD)
        // if not, the normal toString() method is called on the object. This will allow us to manually intervene in the process,
        // eg by using code like this:
        // #foreach($image in $PROPERTY["http://www.mot.be/ontology/image"])
        //   <blocks-image class="bordered">
        //     #set($PROPERTY['image']=$image)
        //   </blocks-image>
        // #end
        sb.append("#if($").append(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE).append("['").append(fullName).append("'].").append(PropertyArray.PROPARR_FIELD).append(")");
        sb.append("$").append(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE).append("['").append(fullName).append("'].").append(PropertyArray.WRITE_ONCE_METHOD_NAME).append("()");
        sb.append("#{else}");
        sb.append("$!").append(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE).append("['").append(fullName).append("']");
        sb.append("#end");

        sb.append("#end");

        output.replace(tag.getElement(), sb);
    }
    private int getAbsoluteTemplateTagLineNumber(Source source, int relativeBegin)
    {
        return source.getRow(source.getFirstElement(WEBCOMPONENTS_TEMPLATE_ELEM).getBegin()) + source.getRow(relativeBegin);
    }
    /**
     * Strips all lines beginning with "##" (starting from that position) and returns all the rest.
     */
    public static String eatTemplateComments(InputStream inputStream) throws IOException
    {
        StringBuilder retVal = new StringBuilder();

        final String commentPrefix = R.resourceManager().getTemplateEngine().getCommentPrefix();

        try (BufferedReader stringReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = stringReader.readLine()) != null) {
                int commentIdx = line.indexOf(commentPrefix);
                if (commentIdx != -1) {
                    line = line.substring(0, commentIdx);
                }

                //if the entire line got eaten, I assume we can discard it
                if (!StringUtils.isEmpty(line.trim())) {
                    //this will make sure a single line doesn't get a newline appended
                    if (retVal.length() > 0) {
                        retVal.append(NEWLINE);
                    }
                    retVal.append(line);
                }
            }
        }

        return retVal.toString();
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
