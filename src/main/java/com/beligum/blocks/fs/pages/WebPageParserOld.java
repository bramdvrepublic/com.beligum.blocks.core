package com.beligum.blocks.fs.pages;

import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.controllers.interfaces.PersistenceController;
import com.beligum.blocks.exceptions.RdfException;
import com.beligum.blocks.models.factories.ResourceFactoryImpl;
import com.beligum.blocks.models.interfaces.Node;
import com.beligum.blocks.models.interfaces.Resource;
import com.beligum.blocks.models.interfaces.WebPage;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.utils.RdfTools;
import gen.com.beligum.blocks.core.messages.blocks.core;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

/**
 * Created by wouter on 2/06/15.
 * <p/>
 * This baby rips everything interesting out of a webpage and puts it in a WebPage object
 * values for resources are put inside resource
 * <p/>
 * We save used templates, resources, links and linked pages, a short html version for the renderer and
 * a text version of the body element
 */
public class WebPageParserOld
{
    private URI uri;
    private Locale locale;
    private Source source;
    private PersistenceController database;
    private StringBuilder parsedHtml = new StringBuilder();
    private String pageTemplate;
    private String pageTitle;
    private String text = "";
    private Set<Map<String, String>> links = new HashSet<Map<String, String>>();
    private Set<String> templates = new HashSet<String>();
    private Map<String, Resource> resources = new HashMap<String, Resource>();

    // All elements in this html
    private List<Element> elements;
    // current search position
    private int elementPosition = 0;
    // All the special tags used on this site
    private Set<String> siteTags = new HashSet<String>();
    // the base for the urls
    private URI base;
    private boolean inBody = false;
    private URI vocab;
    private HashMap<String, URI> prefixes = new HashMap<>();
    private HashMap<URI, Map<URI, List<PropertyValue>>> resourceMap = new HashMap<>();
    // Resource that contains all properties that are attached to the page
    private Resource pageResource;

    public WebPageParserOld(URI uri, Locale locale, String source, PersistenceController database) throws Exception
    {
        this.uri = uri;
        this.locale = locale;
        if (source == null)
            source = "";
        this.source = new Source(source);
        this.source.fullSequentialParse();
        this.database = database;
        this.vocab = Settings.instance().getDefaultRdfSchema();
        this.pageTitle = I18nFactory.instance().getResourceBundle(locale).get(core.Entries.defaultPageTitle);
        Element base = this.source.getFirstElement("base");
        this.setBase(base, uri);
        pageResource = ResourceFactoryImpl.instance().createResource(RdfTools.createRelativeResourceId("webpage"), RdfTools.createLocalType("Webpage"), locale);
        this.resources.put(pageResource.getBlockId().toString(), pageResource);
        this.elements = this.source.getAllElements();
        for (HtmlTemplate template : HtmlParser.getTemplateCache().values()) {
            this.siteTags.add(template.getTemplateName());
        }

        Element next = findNextElement();
        while (next != null) {
            next = parse(next, next.getBegin(), this.pageResource, false);
        }
    }

    public String getParsedHtml()
    {
        return this.parsedHtml.toString();
    }

    public String getText()
    {
        return this.text;
    }

    public Set<Map<String, String>> getLinks()
    {
        return this.links;
    }

    public Set<String> getTemplates()
    {
        return this.templates;
    }

    public String getPageTemplate()
    {
        return this.pageTemplate;
    }

    public String getPageTitle()
    {
        return this.pageTitle;
    }

    public Resource getPageResource()
    {
        return this.pageResource;
    }

    public Map<String, Resource> getResources()
    {
        return this.resources;
    }

    public Map<URI, Map<URI, List<PropertyValue>>> getResourceProperties()
    {
        return resourceMap;
    }

    // --------- PRIVATE METHODS -----------------

    /*
    * Parse the html, retain all elements that have a template tag or property attribute
    * For new resources (elements with typeof attribute but without resource attriubute), add a resource id
    * Put all property values in resource objects to save for the database
    * */
    private Element parse(Element element, int textPos, Resource resource, boolean addContent) throws Exception
    {
        Element next = findNextElement();

        // If we are inside a property, add content to parsedHtml
        if (addContent) {
            parsedHtml.append(source.subSequence(textPos, element.getBegin()));
        }
        textPos = element.getBegin();

        Resource newResource = null;
        if (element.getAttributeValue("typeof") != null) {
            // set resource id on tag.
            // set new resource to add values to.
            URI typeOf = getAbsoluteRdfName(element.getAttributeValue("typeof"));

            if (element.getAttributeValue("resource") == null) {
                // create a new resource
                URI resourceid = RdfTools.createRelativeResourceId(getShortTypeNamefromUri(typeOf));
                newResource = ResourceFactoryImpl.instance().createResource(resourceid, typeOf, locale);

                // add new resource id to parsed html
                parsedHtml.append(source.subSequence(textPos, element.getStartTag().getEnd() - 1));
                parsedHtml.append("resource=\"").append(resourceid.toString()).append("\" >");
                textPos = element.getStartTag().getEnd();
            }
            else {
                // Fetch resource from db with id
                URI resourceId = getAbsoluteRdfName(element.getAttributeValue("resource"));
                if (!RdfTools.isValidAbsoluteURI(resourceId)) {
                    Logger.error("Resource found in html but not with a valid uri.");
                    throw new RdfException("Resource found in html but not with a valid uri.");
                }
                newResource = this.database.getResource(resourceId, locale);
                if (newResource == null) {
                    // create a new resource
                    //                    Logger.warn("Resource found in html with a resource id, but no resource found in db. Creating a new resource...");
                    newResource = ResourceFactoryImpl.instance().createResource(resourceId, typeOf, locale);
                }
            }

            // Add new resource to resources
            if (newResource != null && newResource.getBlockId() != null) {
                this.resources.put(newResource.getBlockId().toString(), newResource);
            }
        }

        // Set property value of current resource when property is content-editable
        if (isProperty(element)) {
            URI property = getAbsoluteRdfName(getPropertyName(element));
            Locale propertyLocale = locale;
            //TODO this is not right?
            if (element.getAttributeValue("lang") != null) {
                propertyLocale = Locale.ROOT;
            }

            Node content;
            if (newResource != null && !(resource instanceof WebPage)) {
                content = newResource;
            }
            else if (element.getAttributeValue("content") != null) {
                content = ResourceFactoryImpl.instance().createNode(element.getAttributeValue("content"), propertyLocale);
            }
            else if (element.getAttributeValue("src") != null) {
                content = ResourceFactoryImpl.instance().createNode(element.getAttributeValue("src"), propertyLocale);
            }
            else if (element.getAttributeValue("href") != null) {
                content = ResourceFactoryImpl.instance().createNode(element.getAttributeValue("href"), propertyLocale);
            }
            else {
                //                Renderer textRenderer = new Renderer(element);
                //                String text = textRenderer.toString();
                content = ResourceFactoryImpl.instance().createNode(element.getContent().toString(), propertyLocale);
            }

            readProperty(resource, property, content);
        }

        // If new resource was found, make this the new basic resource
        // so all nested properties are added to this resource
        if (newResource == null) {
            newResource = resource;
        }

        // A template tag was found. Only add this to the parsed html if this tag has a property attribute,
        // or if this tag sits inside a property
        if (siteTags.contains(element.getStartTag().getName()) && (addContent || isProperty(element))) {
            // write startTag
            if (textPos < element.getStartTag().getEnd()) {
                parsedHtml.append(source.subSequence(element.getStartTag().getBegin(), element.getStartTag().getEnd()));
            }

            while (next != null && element.encloses(next)) {
                Element prev = next;
                next = parse(next, textPos, newResource, false);
                textPos = prev.getEnd();
            }
            // write end tag
            if (textPos < element.getEndTag().getEnd()) {
                parsedHtml.append(source.subSequence(element.getEndTag().getBegin(), element.getEndTag().getEnd()));
            }
        }
        else if (isProperty(element)) {
            while (next != null && element.encloses(next)) {
                Element prev = next;
                next = parse(next, textPos, newResource, true);
                textPos = prev.getEnd();
            }

            if (textPos < element.getEnd()) {
                parsedHtml.append(source.subSequence(textPos, element.getEnd()));
            }
        }

        return next;
    }

    private boolean isProperty(Element element)
    {
        return element.getAttributeValue(HtmlParser.RDF_PROPERTY_ATTR) != null || element.getAttributeValue(HtmlParser.NON_RDF_PROPERTY_ATTR) != null;
    }

    private String getPropertyName(Element element)
    {
        String retVal = element.getAttributeValue(HtmlParser.RDF_PROPERTY_ATTR);
        if (retVal == null) {
            retVal = element.getAttributeValue(HtmlParser.NON_RDF_PROPERTY_ATTR);
        }
        return retVal;
    }
    /*
    * Get the next special element that has to be saved
    * Used while parsing the html
    * */
    private Element findNextElement() throws RdfException, URISyntaxException, IOException
    {
        Element retVal = null;
        boolean found = false;
        while (elementPosition < elements.size() && !found) {
            retVal = elements.get(elementPosition);
            elementPosition++;
            if (retVal.getAttributeValue("typeof") != null) {
                found = true;
            }
            else if (isProperty(retVal)) {
                found = true;
            }
            else if (siteTags.contains(retVal.getStartTag().getName())) {
                found = true;
                this.templates.add(retVal.getStartTag().getName());
            }

            if (retVal.getName().equals("html")) {
                this.pageTemplate = retVal.getAttributeValue("template");
                if (StringUtils.isEmpty(this.pageTemplate)) {
                    throw new IOException("Encountered an attempt to save html without a page template; this shouldn't happen; "+retVal);
                }
            }

            if (retVal.getName().equals("title")) {
                // full text
                this.pageTitle = retVal.getContent().toString();
            }

            if (retVal.getName().equals("body")) {
                // full text
                TextExtractor extractor = retVal.getTextExtractor();
                extractor.setIncludeAttributes(false);
                this.text = extractor.toString();
                inBody = true;
            }

            if (inBody) {
                if (retVal.getAttributeValue("src") != null) {
                    addLink(retVal.getAttributeValue("src"));
                }
                else if (retVal.getAttributeValue("href") != null) {
                    // add to links
                    addLink(retVal.getAttributeValue("href"));
                }
            }

            if (retVal.getAttributeValue("vocab") != null) {
                // set vocab
                String vocabString = retVal.getAttributeValue("vocab");

                this.vocab = new URI(vocabString);
            }
            else if (retVal.getAttributeValue("prefixes") != null) {
                // add prefixes
                HashMap<String, URI> prefix = RdfTools.parsePrefixes(retVal.getAttributeValue("prefixes"));
                for (String p : prefix.keySet()) {
                    if (this.prefixes.containsKey(p) && !(this.prefixes.get(p).equals(prefix.get(p)))) {
                        throw new RdfException("Prefix is used twice in page with different URI's; "+p);
                    }
                    else {
                        this.prefixes.put(p, prefix.get(p));
                    }
                }
            }
        }
        return retVal;
    }

    /*
    * Add the found link to this parser, to be used by WebPage Object
    * */
    private void addLink(String link)
    {
        // We only save the link without the query params
        String[] parts = link.split("\\?");
        if (parts.length > 1) {
            link = parts[0];
        }

        URI absoluteLink = makeLinkAbsolute(link);
        URI pageId = getPageId(absoluteLink);
        HashMap<String, String> newLink = new HashMap<String, String>();
        newLink.put("html", link);
        newLink.put("absolute", absoluteLink.toString());
        if (pageId != null) {
            newLink.put("page", pageId.toString());
        }
        this.links.add(newLink);
    }

    /*
    * Makes a relative link absolute based on the base for this page
    *
    * TODO: see difference between 'test.html' '/test.html' and '../test.html'
    * */
    private URI makeLinkAbsolute(String link)
    {
        URI retVal;
        URI uri = UriBuilder.fromUri(link).build();
        if (RdfTools.isValidAbsoluteURI(uri)) {
            retVal = uri;
        }
        else {
            UriBuilder builder = UriBuilder.fromUri(this.base).path(link);
            retVal = builder.build();
        }
        return retVal;
    }

    /*
    * Get the pageid for a give url. Returns null if no page exists at this url
    * */
    private URI getPageId(URI link)
    {
        URI retVal = null;
        //        Route route = new Route(link, database);
        //        if (route.exists() && route.getNode().isPage()) {
        //            retVal = route.getNode().getPageUrl();
        //        }
        return retVal;
    }

    /*
    * Sets the base for all relative urls in this page
    * */
    private void setBase(Element base, URI contextUri)
    {
        URI baseUri = null;
        if (base != null) {
            baseUri = UriBuilder.fromUri(base.getAttributeValue("href")).build();
        }
        if (base != null && baseUri != null && baseUri.getHost() != null && baseUri.getScheme() != null) {
            this.base = cleanUri(UriBuilder.fromUri(base.getAttributeValue("href")).build());
        }
        else if (contextUri != null && contextUri.getHost() != null && contextUri.getScheme() != null) {
            this.base = cleanUri(contextUri);
            this.base = UriBuilder.fromUri(this.base).replacePath("").build();
        }
        else {
            this.base = cleanUri(Settings.instance().getSiteDomain());
        }
    }

    /*
    * Get the absolute value of a rdf type or property based in vocab and prefixes.
    * Of none are found we assume vocab = default ontology in the config
    * */
    public URI getAbsoluteRdfName(String name) throws RdfException, URISyntaxException
    {
        URI retVal = null;
        name = name.trim();
        if (!name.startsWith("http://")) {
            int index = name.indexOf(":");
            if (index > -1) {
                String prefix = name.substring(0, index);
                name = name.substring(index + 1);
                URI prefixValue = this.prefixes.get(prefix);
                if (prefixValue != null) {
                    retVal = RdfTools.addToUri(prefixValue, name);
                }
                else {
                    throw new RdfException("Property or resource found with prefix but prefix value is not defined: " + prefix + ":" + name);
                }
            }
            else {
                retVal = RdfTools.addToUri(this.vocab, name);
            }
        }
        else {
            retVal = new URI(name);
        }
        return retVal;
    }

    /*
   * Get the absolute value of a rdf type or property based in vocab and prefixes.
   * Of none are found we assume vocab = default ontology in the config
   * */
    public String getShortTypeNamefromUri(URI uri) throws RdfException, URISyntaxException
    {
        // First we take a shortname out of the typeof uri to create the resourceid
        // if the typeof uri has a fragment, use that, else use the last part of the path
        // else use unknown
        String retVal = uri.getFragment();
        if (retVal != null && retVal.indexOf("/") < retVal.length() - 1) {
            retVal = retVal.trim();
            if (retVal.indexOf("/") > -1) {
                retVal = retVal.substring(retVal.indexOf("/"));
            }
        }
        else {
            retVal = uri.getPath().trim();
            Path typePath = Paths.get(retVal);
            if (typePath.getNameCount() > 0) {
                retVal = typePath.getName(typePath.getNameCount() - 1).toString();
            }
            else {
                retVal = "unknown";
            }
        }
        return retVal;
    }

    /*
    * Clean up a url, remove userinfo, port and fragments
    * */
    private URI cleanUri(URI uri)
    {
        return UriBuilder.fromUri("").scheme(uri.getScheme()).host(uri.getHost()).path(uri.getPath()).build();
    }

    /*
    * Adds a property to a map that sorts all properties / resource
    * this just collects the properties and determines the order.
    * When all properties for a resource are read, we can analyse them
    * and change the resource in the DB -> see setResourceProperties
    */
    private void readProperty(Resource resource, URI propertyName, Node node) throws ParseException
    {
        // Resource -> property -> List of values
        if (resource.getBlockId() != null && !resourceMap.containsKey(resource.getBlockId())) {
            resourceMap.put(resource.getBlockId(), new HashMap<URI, List<PropertyValue>>());
        }
        Map<URI, List<PropertyValue>> properties = resourceMap.get(resource.getBlockId());
        if (properties != null) {
            if (!properties.containsKey(propertyName)) {
                properties.put(propertyName, new ArrayList<PropertyValue>());
            }
            else {
                //throw new ParseException("Don't know what to do with multiple entries yet, please fix this; URI="+this.uri+", property="+propertyName+", html="+this.parsedHtml, 0);
            }
            List<PropertyValue> propertyValues = properties.get(propertyName);
            if (propertyValues == null) {
                propertyValues = new ArrayList<PropertyValue>();
                properties.put(propertyName, propertyValues);
            }
            propertyValues.add(new PropertyValue(node));
        }

    }

    /*
    * This sets the properties for the resource in the DB based on the values on the page
    *
    * Check shown properties to compare with saved properties (sometimes not all properies of resource are shown,
    * so we an not simply delete them. Only delete shown properties that are no longer saved)
    *
    * TODO: Compare values to values in webpage (not value in resource object). This way default values are not saved.
    * */
    public static void fillResourceProperties(Map<String, Resource> resources, Map<URI, Map<URI, List<PropertyValue>>> resourceProperties,
                                              Map<URI, Map<URI, List<PropertyValue>>> oldResourceProperties, PersistenceController database, Locale locale)
    {
        // TODO return only changed resources
        for (URI resourceID : resourceProperties.keySet()) {
            Resource resource = resources.get(resourceID.toString());
            Map<URI, List<PropertyValue>> properties = resourceProperties.get(resource.getBlockId());
            Map<URI, List<PropertyValue>> oldProperties = oldResourceProperties.get(resource.getBlockId());
            if (oldProperties == null)
                oldProperties = new HashMap<URI, List<PropertyValue>>();

            // Check for each property if it has changed
            for (URI propertyName : properties.keySet()) {
                List<PropertyValue> propertyValues = properties.get(propertyName);
                if (propertyValues.size() == 0) {

                }
                else if (propertyValues.size() == 1) {
                    resource.set(propertyName, propertyValues.get(0).getNode());
                }
                else {
                    // We deal with a list, compare this with old property value
                    // was previous property value also a list? did the list change?
                    // Try to decide as fast as possible if the two lists are the same
                    List<PropertyValue> oldPropertyValues = oldProperties.get(propertyName);
                    if (resource.get(propertyName).isIterable() && oldProperties.containsKey(propertyName) &&
                        oldProperties.get(propertyName).size() != ((Collection) resource.get(propertyName).getValue()).size()) {
                        // Not all values where shown in prev page, select not shown values to keep for later
                        Set bag = new HashSet<Object>();
                        List<Node> notShownProperties = new ArrayList<Node>();
                        for (PropertyValue pv : oldProperties.get(propertyName)) {
                            bag.add(pv.getNode().getValue());
                        }
                        for (Node node : resource.get(propertyName)) {
                            if (!bag.contains(node.getValue())) {
                                notShownProperties.add(node);
                            }
                        }
                    }

                    Node resourceNode = resource.get(propertyName);
                    boolean changed = false;
                    // we have 2 lists (old value and new value are both lists)
                    if (resourceNode.isIterable() && propertyValues.size() > 1) {
                        int counter = 0;
                        for (Node node : resourceNode) {
                            if (counter > propertyValues.size()) {
                                changed = true;
                                break;
                            }
                            else if (node.getValue() != propertyValues.get(counter).getNode().getValue()) {
                                changed = true;
                                break;
                            }
                            counter++;
                        }
                        // it wasn't a list but now it is a list
                    }
                    else {
                        changed = true;
                    }

                    if (changed) {
                        resource.remove(propertyName);
                        for (PropertyValue value : propertyValues) {
                            resource.add(propertyName, value.getNode());
                        }
                    }
                }

            }
        }

    }

    private class PropertyValue
    {
        private Node node;
        private Resource resource;

        public PropertyValue(Resource node)
        {
            this.resource = node;
        }

        public PropertyValue(Node node)
        {
            if (node.isResource()) {
                this.resource = (Resource) node;
            }
            else {
                this.node = node;
            }
        }

        public Node getNode()
        {
            if (node != null) {
                return node;
            }
            else {
                return resource;
            }
        }

    }

}
