package com.beligum.blocks.pages;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.database.interfaces.BlocksDatabase;
import com.beligum.blocks.exceptions.RdfException;
import com.beligum.blocks.resources.interfaces.Resource;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.routing.Route;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.utils.RdfTools;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by wouter on 2/06/15.
 *
 * This baby rips everything interesting out of a webpage and puts it in a WebPage object
 * values for resources are put inside resource
 *
 * We save used templates, resources, links and linked pages, a short html version for the renderer and
 * a text version of the body element
 *
 */
public class WebPageParser
{
    private WebPage webPage;
    private Source source;
    private BlocksDatabase database;
    private StringBuilder parsedHtml = new StringBuilder();
    private String text = "";
    private Set<HashMap<String, String>> links = new HashSet<HashMap<String, String>>();
    private Set<String> templates = new HashSet<String>();
    private HashMap<String, Resource> resources = new HashMap<String, Resource>();


    // All elements in this html
    private List<Element> elements;
    // current search position
    private int elementPosition = 0;
    // All the special tags used on this site
    private Set<String> siteTags = new HashSet<String>();
    // the base for the urls
    private URI base;
    private boolean inBody = false;
    private String vocab;
    private HashMap<String, URI> prefixes = new HashMap<>();

    public WebPageParser(WebPage webpage, URI uri, String source, BlocksDatabase database) throws RdfException, URISyntaxException
    {
        this.webPage = webpage;
        this.source = new Source(source);
        this.source.fullSequentialParse();
        this.database = database;
        this.vocab = BlocksConfig.instance().getDefaultRdfSchema().toString();

        Element base = this.source.getFirstElement("base");
        this.setBase(base, uri);

        this.elements  = this.source.getAllElements();
        for (HtmlTemplate template: HtmlParser.getCachedTemplates().values()) {
            this.siteTags.add(template.getTemplateName());
        }

        Element next = findNextElement();
        while (next != null) {
            next = parse(next, next.getBegin(), webpage, false);
        }
    }

    public String getParsedHtml() {
        return this.parsedHtml.toString();
    }

    public String getText() {
        return this.text;
    }

    public Set<HashMap<String, String>> getLinks() {
        return this.links;
    }

    public Set<String> getTemplates() {
        return this.templates;
    }

    public WebPage getWebPage() {
        if (this.webPage.getParsedHtml() != this.getParsedHtml()) {
            this.webPage.setParsedHtml(this.getParsedHtml());
            this.webPage.setLinks(this.getLinks());
            this.webPage.setResources(this.resources.keySet());
            this.webPage.setTemplates(this.getTemplates());
        }
        return this.webPage;
    }

    // --------- PRIVATE METHODS -----------------

    /*
    * Parse the html, retain all elements that have a template tag or property attribute
    * For new resources (elements with typeof attribute but without resource attriubute), add a resource id
    * Put all property values in resource objects to save for the database
    *
    * */
    private Element parse(Element element, int textPos, Resource resource, boolean addContent) throws URISyntaxException, RdfException
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
                URI resourceid = RdfTools.createLocalResourceId(getShortTypeNamefromUri(typeOf));
                newResource = this.database.createResource(resourceid, typeOf, webPage.getLanguage());

                // add new resource id to parsed html
                parsedHtml.append(source.subSequence(textPos, element.getStartTag().getEnd()-1));
                parsedHtml.append("resource=\"").append(resourceid.toString()).append("\" >");
                textPos = element.getStartTag().getEnd();
            } else {
                // Fetch resource from db with id
                URI resourceId = getAbsoluteRdfName(element.getAttributeValue("resource"));
                if (!RdfTools.isValidAbsoluteURI(resourceId)) {
                    Logger.error("Resource found in html but not with a valid uri.");
                    throw new RdfException("Resource found in html but not with a valid uri.");
                }
                newResource = this.database.getResource(resourceId, this.webPage.getLanguage());
                if (newResource == null) {
                    // create a new resource
                    Logger.warn("Resource found in html with a resource id, but no resource found in db. Creating a new resource...");
                    URI resourceid = RdfTools.createLocalResourceId(getShortTypeNamefromUri(typeOf));
                    newResource = this.database.createResource(resourceid, typeOf, webPage.getLanguage());
                }
            }

            // Add new resource to resources
            if (newResource != null) {
                this.resources.put(newResource.getBlockId().toString(), newResource);
            }
        }

        // Set property value of current resource when property is content-editable
        if (element.getAttributeValue("property") != null && element.getAttributeValue("contenteditable") != null && element.getAttributeValue("contenteditable").toLowerCase().equals("true")) {
            URI property = getAbsoluteRdfName(element.getAttributeValue("property"));

            if (newResource != null && !(resource instanceof WebPage)) {
                resource.set(property, newResource);
            } else if (element.getAttributeValue("content") != null) {
                resource.set(property, this.database.createNode(element.getAttributeValue("content"), this.webPage.getLanguage()));
            } else if (element.getAttributeValue("src") != null) {
                resource.set(property, this.database.createNode(element.getAttributeValue("src"), Locale.ROOT));
            } else if (element.getAttributeValue("href") != null) {
                resource.set(property, this.database.createNode(element.getAttributeValue("href"), Locale.ROOT));
            } else {
                resource.set(property, this.database.createNode(element.getContent().toString(), Locale.ROOT));
            }
        }

        // If new resource was found, make this the new basic resource
        // so all nested properties are added to this resource
        if (newResource != null) {
            resource= newResource;
        }

        if (siteTags.contains(element.getStartTag().getName())) {
            // write startTag
            if (textPos < element.getStartTag().getEnd()) {
                parsedHtml.append(source.subSequence(element.getStartTag().getBegin(), element.getStartTag().getEnd()));
            }

            while (next != null && element.encloses(next)) {
                Element prev = next;
                next = parse(next, textPos, resource, false);
                textPos = prev.getEnd();
            }
            // write end tag
            if (textPos < element.getEndTag().getEnd()) {
                parsedHtml.append(source.subSequence(element.getEndTag().getBegin(), element.getEndTag().getEnd()));
            }
        }
        else if (element.getAttributeValue("property") != null || element.getAttributeValue("data-property") != null) {
            while (next != null && element.encloses(next)) {
                Element prev = next;
                next = parse(next, textPos, resource, true);
                textPos = prev.getEnd();
            }

            if (textPos < element.getEnd()) {
                parsedHtml.append(source.subSequence(textPos, element.getEnd()));
            }
        }

        return next;
    }

    /*
    * Get the next special element that has to be saved
    * Used while parsing the html
    * */
    private Element findNextElement() throws RdfException
    {
        Element retVal = null;
        boolean found = false;
        while (elementPosition < elements.size() && !found) {
            retVal = elements.get(elementPosition);
            elementPosition++;
            if (retVal.getAttributeValue("typeof") != null) {
                found = true;
            } else if (retVal.getAttributeValue("property") != null) {
                found = true;
            } else if (retVal.getAttributeValue("data-property") != null) {
                found = true;
            } else if (siteTags.contains(retVal.getStartTag().getName())) {
                found = true;
                this.templates.add(retVal.getStartTag().getName());
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
                this.vocab = retVal.getAttributeValue("vocab");
            } else if (retVal.getAttributeValue("prefixes") != null) {
                // add prefixes
                HashMap<String, URI> prefix = RdfTools.parsePrefixes(retVal.getAttributeValue("prefixes"));
                for (String p: prefix.keySet()) {
                    if (this.prefixes.containsKey(p) && !(this.prefixes.get(p).equals(prefix.get(p)))) {
                        throw new RdfException("Prefix is used twice in page with different URI's");
                    } else {
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
    private void addLink(String link) {
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
    private URI makeLinkAbsolute(String link) {
        URI retVal;
        URI uri = UriBuilder.fromUri(link).build();
        if (RdfTools.isValidAbsoluteURI(uri)) {
            retVal = uri;
        } else  {
            UriBuilder builder = UriBuilder.fromUri(this.base).path(link);
            retVal = builder.build();
        }
        return retVal;
    }

    /*
    * Get the pageid for a give url. Returns null if non page exists at this url
    * */
    private URI getPageId(URI link) {
        URI retVal= null;
        Route route = new Route(link, database);
        if (route.exists() && route.getNode().isPage()) {
            retVal = route.getNode().getPageUrl();
        }
        return retVal;
    }

    /*
    * Sets the base for all relative urls in this page
    * */
    private void setBase(Element base, URI contextUri) {
        URI baseUri = null;
        if (base != null) {
            baseUri = UriBuilder.fromUri(base.getAttributeValue("href")).build();
        }
        if (base != null && baseUri != null && baseUri.getHost() != null && baseUri.getScheme() != null) {
            this.base = cleanUri(UriBuilder.fromUri(base.getAttributeValue("href")).build());
        } else if (contextUri != null && contextUri.getHost() != null && contextUri.getScheme() != null) {
            this.base = cleanUri(contextUri);
        } else {
            this.base = cleanUri(BlocksConfig.instance().getSiteDomain());
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
                name = name.substring(index+1);
                String prefixValue = this.prefixes.get(prefix).toString();
                if (prefixValue != null) {
                    retVal = new URI(prefixValue + name);
                } else {
                    throw new RdfException("Property or resource found with prefix but prefix value is not defined: " + prefix + ":" + name);
                }
            }
            else {
               retVal = new URI(this.vocab + name);
            }
        } else {
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
        String retVal = uri.getFragment().trim();
        if (retVal != null && retVal.indexOf("/") < retVal.length() - 1) {
            if (retVal.indexOf("/") > -1) {
                retVal = retVal.substring(retVal.indexOf("/"));
            }
        } else {
            retVal = uri.getPath().trim();
            Path typePath = Paths.get(retVal);
            if (typePath.getNameCount() > 0) {
                retVal = typePath.getName(typePath.getNameCount() - 1).toString();
            } else {
                retVal = "unknown";
            }
        }
        return retVal;
    }


    /*
    * Clean up a url, remove userinfo, port and fragments
    * */
    private URI cleanUri(URI uri) {
        return UriBuilder.fromUri("").scheme(uri.getScheme()).host(uri.getHost()).path(uri.getPath()).build();
    }


}