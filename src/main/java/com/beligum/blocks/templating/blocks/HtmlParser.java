package com.beligum.blocks.templating.blocks;

import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ResourceInputStream;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceParser;
import com.beligum.base.resources.parsers.MinifiedInputStream;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.UriDetector;
import com.beligum.blocks.templating.blocks.directives.PageTemplateWrapperDirective;
import com.beligum.blocks.templating.blocks.directives.ResourceUriDirective;
import com.beligum.blocks.templating.blocks.directives.TagTemplateResourceDirective;
import com.beligum.blocks.templating.blocks.directives.TemplateInstanceStackDirective;
import com.google.common.collect.Sets;
import net.htmlparser.jericho.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.UriBuilder;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by bram on 5/16/15.
 * <p/>
 * TODO: replace property values not only on the string name of the property, but also
 * keep a refernece to prefixes or the default vocab. (e.g. property="pagetitle" and
 * property="http://www.mot.be/ontology/pagetitle" and property="mot:pagetitle" could all reference the same
 * property.
 * <p>
 * Note: the reverse of this class is com.beligum.blocks.templating.blocks.HtmlAnalyzer
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
    public static final String RDF_PREFIX_ATTR = "prefix";
    public static final String HTML_ROOT_ELEM = "html";
    public static final String WEBCOMPONENTS_TEMPLATE_ELEM = "template";
    //the prefix data- makes sure we're W3C compliant
    public static final String HTML_ROOT_TEMPLATE_ATTR = "data-template";
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

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public HtmlParser()
    {
    }

    //-----PUBLIC METHODS-----
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

            //the default error count is set to 2, meaning if an attribute throws two errors,
            // it's ignored, which is problematic with all the Velocity tags
            // Should we activate this?
            // -> I think not, it's good that eg. in <html ${blah}> the '${blah}' is skipped,
            //    that said, it's more problematic that eg. in <html${blah}>, the 'html${blah}' will probably be skipped...
            //Attributes.setDefaultMaxErrorCount(10);

            //the solves a lot of issues with inactive lines
            Source htmlSource;
            try (InputStream is = source.newInputStream()) {
                htmlSource = new Source(this.eatVelocityComments(is));
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
            //Note that, at the moment, the only way we get here, is through the ResourceManager.get() call below
            HtmlTemplate template = templateCache.getByRelativePath(source.getUri().getPath());
            if (template != null) {
                //replace the source with the (parsed) <template> content
                htmlSource = new Source(this.processHtmlTemplate(htmlSource, template).toString());
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

            //TODO should we still decorate here?
            if (R.configuration().getResourceConfig().getMinifyResources()) {
                retVal = new ResourceInputStream(new MinifiedInputStream(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)), source.getUri(), source.getMimeType()));
            }
            else {
                retVal = new ResourceInputStream(sb.toString());
            }
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
    /**
     * This is the main processor of html content and is called (recursively) from a lot of places.
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
        if (R.configuration().getResourceConfig().getEnableFingerprintedResources()) {

            //skip resource elements, the URI will be wrapped/parsed more extensively because of packing and inlining; see HtmlTemplate.buildResourceHtml()
            if (!HtmlTemplate.isResourceElement(element)) {
                Attributes attributes = element.getAttributes();
                if (attributes != null) {
                    for (Attribute att : attributes) {
                        String val = att.getValue();
                        if (ALL_SIMPLE_URL_ATTR.contains(att.getName().toLowerCase())) {
                            if (!StringUtils.isEmpty(val)) {
                                //Note: by pulling the URI string through the UriBuilder parser, we get a chance to straighten invalid URIs
                                // eg. replace all spaces with %20 and so on...

                                retVal.replace(att, att.getName() + "=\"" + R.resourceManager().getFingerprinter().detectAllUris(UriBuilder.fromUri(val).build().toString(), this) + "\"");
                            }
                        }
                        else if (ALL_COMPLEX_URL_ATTR.contains(att.getName().toLowerCase())) {
                            //note that we can't check for spaces here, let's just hope for the best...
                            if (!StringUtils.isEmpty(val)) {
                                //sadly, we can't use our extra validation step here, so make sure all uris are valid ;-)
                                retVal.replace(att, att.getName() + "=\"" + R.resourceManager().getFingerprinter().detectAllUris(val, this) + "\"");
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
    public String uriDetected(URI uri)
    {
        String retVal = null;

        //if the endpoint is immutable, we'll generate our fingerprint right now,
        //if not, we'll wrap the URI in a directive to re-parse it on every request.
        //Note: this means we won't do any other post-processing next to fingerprinting in that directive anymore,
        //if that would change, we must wipe this optimization step
        Resource resource = R.resourceManager().get(uri);

        //this means we're dealing with a local uri
        if (resource != null) {
            if (resource.isImmutable()) {
                retVal = resource.getFingerprintedUri().toString();
            }
            else {
                //this means we'll postpone the fingerprinting of the URI to the render phase, just wrap it in our fingerprint directive
                retVal = new StringBuilder("#").append(ResourceUriDirective.NAME).append("('").append(uri.toString()).append("')").toString();
            }
        }

        //never return null; if nothing was found, the uri was probably an external one
        if (retVal == null) {
            retVal = uri.toString();
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

                //skip the entire tree of the element, we'll handle it right here
                // but watch out: an <img> element doens't have and end tag, so check for null
                if (!isVoidTag) {
                    if (immediateChild.getEndTag() != null) {
                        while (iter.hasNext() && !iter.next().equals(immediateChild.getEndTag())) ;
                    }
                }

                //since (for our template system) the RDF and non-RDF attributes are equal, we can treat them the same way
                String name = immediateChild.getAttributeValue(RDF_PROPERTY_ATTR);
                //note that this will be skipped when a valid RDF-attr is found (so if both are present, the RDF one has priority)
                if (name == null) {
                    name = immediateChild.getAttributeValue(NON_RDF_PROPERTY_ATTR);
                }
                if (name != null) {
                    name = this.preprocessPropertyName(name);

                    String value = immediateChild.toString();

                    OutputDocument parsedValue = this.processSource(source, new Source(value));
                    value = parsedValue.toString();

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
            //if the segment is something else (than a start tag), save it in the right order for later use
            else {
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
            builder.append(",\"").append(attribute.getKey()).append("\",\"").append(value == null ? "" : value).append("\"");
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
     */
    private String preprocessPropertyName(String name) throws IOException
    {
        if (name.startsWith(R.resourceManager().getTemplateEngine().getVariablePrefix())) {
            name = this.renderTemplateValue(name);
        }

        return name;
    }
    private String renderTemplateValue(String value) throws IOException
    {
        return R.resourceManager().newTemplate(new StringSource(value, MimeTypes.HTML, R.i18n().getOptimalLocale())).render();
    }
    /**
     * Process the html tag/page template (not an instance, the real source template) and generate it's html
     */
    private OutputDocument processHtmlTemplate(Source htmlSource, HtmlTemplate template) throws Exception
    {
        //first of all, since this method is only called when something changed, update the cache value
        //use the old template value for the paths
        HtmlTemplate sourceTemplate = HtmlTemplate.create(template.getTemplateName(), htmlSource, template.getAbsolutePath(), template.getRelativePath(), template.getSuperTemplate());
        TemplateCache.instance().putByRelativePath(template.getRelativePath().toString(), sourceTemplate);

        //this is the base for all coming preprocessing
        //note that we only use the inner html, the prefix and suffix will be rendered out before and after every instance (see loops below)
        Segment templateHtml = sourceTemplate.getInnerHtml();
        OutputDocument output = new OutputDocument(templateHtml);

        //the same for a Tag or Page template; preprocess the replaceable properties

        Stack<URI> currentVocabStack = new Stack<>();
        if (sourceTemplate.getRdfVocab() != null) {
            currentVocabStack.push(sourceTemplate.getRdfVocab());
        }

        Stack<Map<String, URI>> currentPrefixesStack = new Stack<>();
        if (sourceTemplate.getRdfPrefixes() != null && !sourceTemplate.getRdfPrefixes().isEmpty()) {
            currentPrefixesStack.push(sourceTemplate.getRdfPrefixes());
        }

        Set<EndTag> vocabPopTags = new HashSet<>();
        Set<EndTag> prefixPopTags = new HashSet<>();

        for (Iterator<Segment> nodeIterator = templateHtml.getNodeIterator(); nodeIterator.hasNext(); ) {
            Segment nodeSegment = nodeIterator.next();
            if (nodeSegment instanceof StartTag) {
                StartTag tag = (StartTag) nodeSegment;

                URI currentVocab = currentVocabStack.isEmpty() ? null : currentVocabStack.peek();
                Map<String, URI> currentPrefixes = currentPrefixesStack.isEmpty() ? null : currentPrefixesStack.peek();

                Attributes attributes = tag.getAttributes();
                if (attributes != null) {

                    //if we encounter a vocab attribute, we push a new vocab value on the stack
                    Attribute vocabAttr = attributes.get(RDF_VOCAB_ATTR);

                    if (vocabAttr != null) {
                        currentVocab = parseRdfVocabAttribute(sourceTemplate, vocabAttr.getValue());
                        //if the tag is not stand-alone, push it on the stack and save it's end tag for popping
                        if (!tag.isEmptyElementTag()) {
                            currentVocabStack.push(currentVocab);

                            //since it's not an empty element tag, it should have an end tag
                            EndTag endTag = tag.getElement().getEndTag();
                            if (endTag == null) {
                                throw new Exception("Encountered non-empty element '" + tag.toString() + "' (at line " + getAbsoluteTemplateTagLineNumber(htmlSource, tag.getBegin()) +
                                                    ")  without a matching end tag; " + sourceTemplate.getAbsolutePath());
                            }
                            else {
                                vocabPopTags.add(endTag);
                            }
                        }
                    }

                    //if we encounter a prefix attribute, we push a (list of) new prefixes on the stack
                    Attribute prefixAttr = attributes.get(RDF_PREFIX_ATTR);
                    if (prefixAttr != null) {
                        if (currentPrefixes == null) {
                            currentPrefixes = new LinkedHashMap<>();
                        }
                        else {
                            //merge it with the already active prefixes
                            currentPrefixes = new LinkedHashMap<>(currentPrefixes);
                        }
                        parseRdfPrefixAttribute(sourceTemplate, prefixAttr.getValue(), currentPrefixes);
                        //if the tag is not stand-alone, merge it with the active prefixes, push it on the stack and save it's end tag for popping
                        if (!tag.isEmptyElementTag()) {
                            currentPrefixesStack.push(currentPrefixes);
                            prefixPopTags.add(tag.getElement().getEndTag());
                        }
                    }

                    Attribute propertyAttr = attributes.get(RDF_PROPERTY_ATTR);
                    if (propertyAttr != null) {
                        this.replaceTemplateProperty(htmlSource, sourceTemplate, tag.getElement(), propertyAttr, output, currentVocab, currentPrefixes);
                    }

                    Attribute dataPropertyAttr = attributes.get(NON_RDF_PROPERTY_ATTR);
                    if (dataPropertyAttr != null) {
                        if (propertyAttr == null) {
                            this.replaceTemplateProperty(htmlSource, sourceTemplate, tag.getElement(), dataPropertyAttr, output, currentVocab, currentPrefixes);
                        }
                        else {
                            Logger.warn("Not using attribute '" + NON_RDF_PROPERTY_ATTR + "' of tag " + tag.toString() + " because there's also a '" + RDF_PROPERTY_ATTR +
                                        "' attribute set that has precedence because it's an official RDFa attribute");
                        }
                    }
                }
            }
            else if (nodeSegment instanceof EndTag) {
                EndTag tag = (EndTag) nodeSegment;

                //pop the vocab stack
                if (vocabPopTags.contains(tag)) {
                    currentVocabStack.pop();
                    vocabPopTags.remove(tag);
                }

                //pop the prefix stack
                if (prefixPopTags.contains(tag)) {
                    currentPrefixesStack.pop();
                    prefixPopTags.remove(tag);
                }
            }
        }

        return output;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void replaceTemplateProperty(Source source, HtmlTemplate template, Element tag, Attribute attribute, OutputDocument output, URI currentVocab, Map<String, URI> currentPrefixes)
                    throws Exception
    {
        String name = this.expandProperty(source, template, tag, attribute.getValue(), currentVocab, currentPrefixes);

        StringBuilder sb = new StringBuilder();
        //if there's no property active in this context, use the (default) value of the template
        sb.append("#if(!$").append(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE).append("['").append(name).append("'])");
        sb.append(tag.toString());
        sb.append("#{else}");
        // This is something special:
        // If we have multiple properties, eg. like this:
        //     <meta property="description" content="Double tag test Nederlands" lang="nl">
        //     <meta property="description" content="Double tag test English" lang="en">
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
        sb.append("#if($").append(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE).append("['").append(name).append("'].").append(PropertyArray.PROPARR_FIELD).append(")");
        sb.append("$").append(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE).append("['").append(name).append("'].").append(PropertyArray.WRITE_ONCE_METHOD_NAME).append("()");
        sb.append("#{else}");
        sb.append("$!").append(TemplateContextMap.TAG_TEMPLATE_PROPERTIES_VARIABLE).append("['").append(name).append("']");
        sb.append("#end");

        sb.append("#end");

        output.replace(tag, sb);
    }
    /**
     * Expands the property, based on the current vocab and prefix settings of this template
     *
     * @param attributeValue the value you want to expand
     * @return the full absolute RDFa URI that describes this attribute
     */
    private String expandProperty(Source source, HtmlTemplate template, Element tag, String attributeValue, URI currentVocab, Map<String, URI> currentPrefixes) throws Exception
    {
        // According to http://www.w3.org/TR/rdfa-syntax/#A-property
        // a property is a "A white space separated list of TERMorCURIEorAbsIRIs"
        // We'll ignore the possibility it's a list for now, so a TERMorCURIEorAbsIRI can be one of these:
        // - TERM: a xs:Name with pattern [\i-[:]][/\c-[:]]*
        // - CURIE: a xs:string of min length 1 with pattern (([\i-[:]][\c-[:]]*)?:)?(/[^\s/][^\s]*|[^\s/][^\s]*|[^\s]?)
        // - AbsIRI: a xs:string with pattern [\i-[:]][\c-[:]]+:.+
        // TODO Start here (http://www.w3.org/TR/rdfa-syntax/#P_term) for a better implementation, this is just a first tryout
        String retVal = null;

        if (!StringUtils.isEmpty(attributeValue)) {
            if (attributeValue.contains(":")) {
                URI uri = null;
                try {
                    uri = URI.create(attributeValue);
                }
                catch (IllegalArgumentException e) {
                    //ignored
                }

                //if the value is not a valid URI, try the CURIE syntax if we have a valid prefix
                if (uri != null) {
                    retVal = uri.toString();
                }
                else {
                    //this means the value is possibly a CURIE, so look up the prefix in the currentPrefixes
                    String[] colonSplit = attributeValue.split(":");
                    if (colonSplit.length != 2) {
                        throw new Exception("Encountered attribute '" + attributeValue + "' in tag '" + tag.getStartTag().toString() + "' (at line " +
                                            getAbsoluteTemplateTagLineNumber(source, tag.getStartTag().getBegin()) +
                                            ") as a CURIE with more than one colon, this is not supported (and I can't seem to find if it's valid or not); " + template.getAbsolutePath());
                    }
                    URI prefix = currentPrefixes.get(colonSplit[0]);
                    if (prefix != null) {
                        String prefixUri = prefix.toString();
                        if (!prefixUri.endsWith("/")) {
                            prefixUri += "/";
                        }
                        String suffix = colonSplit[1];
                        while (suffix.startsWith("/")) {
                            suffix = suffix.substring(1);
                        }
                        retVal = prefixUri + suffix;
                    }
                    else {
                        throw new Exception("Encountered attribute '" + attributeValue + "' in tag '" + tag.getStartTag().toString() + "' (at line " +
                                            getAbsoluteTemplateTagLineNumber(source, tag.getStartTag().getBegin()) + ") as a CURIE with an unknown prefix '\"+(colonSplit[0])+\"' in this context; " +
                                            template.getAbsolutePath());
                    }
                }
            }
            //if the value is no CURIE or URI, prefix it with the currentVocab if we have one
            else if (currentVocab != null) {
                String prefixUri = currentVocab.toString();
                if (!prefixUri.endsWith("/")) {
                    prefixUri += "/";
                }
                String suffix = attributeValue;
                while (suffix.startsWith("/")) {
                    suffix = suffix.substring(1);
                }
                retVal = prefixUri + suffix;
            }
            //the value is no curie and we don't have a vocab; it's invalid
            else {
                //TODO: check the commented exception below, do we want this to be an exception?
                retVal = attributeValue;

                int row = -1;
                try {
                    //hmm, this seems to happen sometimes...
                    row = source.getRow(tag.getStartTag().getBegin());
                }
                catch (IndexOutOfBoundsException e) {
                }
                Logger.warn("Encountered attribute '" + attributeValue + "' in tag '" + tag.getStartTag().toString() + "' around line " + row +
                            " that is not connected to any vocabulary or ontology and is in fact invalid; " + template.getAbsolutePath());
                //throw new Exception("Encountered attribute '"+attributeValue+"' in tag '"+tag.getStartTag().toString()+"' around line "+source.getRow(tag.getStartTag().getBegin())+" that is not connected to any vocabulary or ontology. As much as I want to allow this, I can't; " + template.getAbsolutePath());
            }
        }

        //this will allow us to use template variables as property attribute values and goes hand in hand with
        retVal = this.preprocessPropertyName(retVal);

        return retVal;
    }
    private int getAbsoluteTemplateTagLineNumber(Source source, int relativeBegin)
    {
        return source.getRow(source.getFirstElement(WEBCOMPONENTS_TEMPLATE_ELEM).getBegin()) + source.getRow(relativeBegin);
    }
    public static URI parseRdfVocabAttribute(HtmlTemplate sourceTemplate, String vocabAttrValue) throws Exception
    {
        URI retVal = null;

        if (vocabAttrValue != null) {
            try {
                retVal = URI.create(vocabAttrValue);
            }
            catch (IllegalArgumentException e) {
                throw new Exception("You supplied a 'vocab' attribute in template '" + sourceTemplate.getTemplateName() + "', but it's not a valid URI; " + vocabAttrValue + "; " +
                                    sourceTemplate.getAbsolutePath(), e);
            }
        }

        return retVal;
    }
    public static void parseRdfPrefixAttribute(HtmlTemplate sourceTemplate, String prefixAttrValue, Map<String, URI> retVal) throws Exception
    {
        if (prefixAttrValue != null) {
            if (!StringUtils.isEmpty(prefixAttrValue)) {
                String[] prefixAttrSplit = prefixAttrValue.split(" ");
                if (prefixAttrSplit.length % 2 != 0) {
                    throw new Exception("You supplied a 'prefix' attribute in template '" + sourceTemplate.getTemplateName() +
                                        "', but it doesn't contain an even space-separated list that form one (or more) key-value pairs; " + sourceTemplate.getAbsolutePath());
                }
                for (int i = 0; i < prefixAttrSplit.length; i += 2) {
                    String p = prefixAttrSplit[i];
                    URI uri = null;
                    try {
                        uri = URI.create(prefixAttrSplit[i + 1]);
                    }
                    catch (IllegalArgumentException e) {
                        throw new Exception("You supplied a 'prefix' attribute in template '" + sourceTemplate.getTemplateName() + "', but the value for prefix '" + p +
                                            "' is not a valid URI; " + uri + "; " + sourceTemplate.getAbsolutePath(), e);
                    }

                    retVal.put(p, uri);
                }
            }
        }
    }
    /**
     * Strips all lines beginning with "##" (starting from that position) and returns all the rest.
     */
    public static String eatVelocityComments(InputStream inputStream) throws IOException
    {
        StringBuilder retVal = new StringBuilder();

        try (BufferedReader stringReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = stringReader.readLine()) != null) {
                int commentIdx = line.indexOf("##");
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
