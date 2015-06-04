package com.beligum.blocks.pages;

import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.exceptions.RdfException;
import com.beligum.blocks.models.resources.interfaces.Resource;
import com.beligum.blocks.pages.ifaces.WebPage;
import com.beligum.blocks.routing.Route;
import com.beligum.blocks.routing.ifaces.nodes.RouteController;
import com.beligum.blocks.templating.blocks.HtmlParser;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import net.htmlparser.jericho.*;

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
 * we save used templates, resources, links and linked pages, a short html version for the renderer and
 * a text version of the body element
 *
 */
public class WebPageParser
{
    private WebPage webPage;
    private Source source;
    private RouteController routeController;
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
    private HashMap<String, String> prefixes;

    public WebPageParser(WebPage webpage, URI uri, String source, RouteController routeController) throws RdfException, URISyntaxException
    {
        this.webPage = webpage;
        this.source = new Source(source);
        this.source.fullSequentialParse();
        this.routeController = routeController;
        this.vocab = BlocksConfig.instance().getDefaultRdfSchema().toString();

        Element base = this.source.getFirstElement("base");
        this.setBase(base, uri);

        this.elements  = this.source.getAllElements();
        for (HtmlTemplate template: HtmlParser.getCachedTemplates().values()) {
            this.siteTags.add(template.getTemplateName());
        }

        Element next = findNextElement();
        while (next != null) {
            next = parse(next, next.getBegin(), null, false);
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

        if (element.getAttributes().contains("typeof")) {
            // set resource id on tag.
            // set new resource to add values to.
            URI typeOf = getAbsoluterRdfName(element.getAttributeValue("typeof"));

            if (!element.getAttributes().contains("resource")) {
                // create new resource
                textPos = element.getStartTag().getEnd();
            }
        } else if (element.getAttributes().contains("property")) {

        }

        if (siteTags.contains(element.getStartTag().getName())) {
            // write startTag
            if (textPos < element.getStartTag().getEnd()) {
                parsedHtml.append(source.subSequence(element.getStartTag().getBegin(), element.getStartTag().getEnd()));
            }

            while (element.encloses(next)) {
                next = parse(next, textPos, resource, false);
                textPos = element.getEnd();
            }
            // write end tag
            if (textPos < element.getEndTag().getEnd()) {
                parsedHtml.append(source.subSequence(element.getEndTag().getBegin(), element.getEndTag().getEnd()));
            }
        }
        else if (element.getAttributes().contains("property") || element.getAttributes().contains("data-property")) {
            while (element.encloses(next)) {
                next = parse(next, textPos, resource, true);
                textPos = next.getEnd();
            }

            if (textPos < element.getEnd()) {
                parsedHtml.append(source.subSequence(textPos, element.getEnd()));
                textPos = element.getEnd();
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
            if (retVal.getAttributes().contains("typeof")) {
                found = true;
            } else if (retVal.getAttributes().contains("property")) {
                found = true;
            } else if (retVal.getAttributes().contains("data-property")) {
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
                if (retVal.getAttributes().contains("src")) {
                    addLink(retVal.getAttributeValue("src"));
                }
                else if (retVal.getAttributes().contains("href")) {
                    // add to links
                    addLink(retVal.getAttributeValue("href"));
                }
            }

            if (retVal.getAttributes().contains("vocab")) {
                // set vocab
                this.vocab = retVal.getAttributeValue("vocab");
            } else if (retVal.getAttributes().contains("prefixes")) {
                // add prefixes
                String prefixString = retVal.getAttributes().get("prefixes").getValue().trim();
                prefixString = prefixString.replaceAll(" *?", " ");
                String[] prefixes = prefixString.split(" ");

                if (prefixes.length % 2 != 0) {
                    throw new RdfException("Prefixes could not be parsed. Uneven number. Prefix attribute value should be of type 'ex: http://www.example.com/' ");
                }

                int count = 0;
                while (count < prefixes.length) {
                    String prefix = prefixes[count];
                    String prefixValue = prefixes[count + 1];
                    count = count + 2;
                    if (prefix.endsWith(":") && prefix.length() > 1) {
                        prefix = prefix.substring(0, prefix.length() -1);
                    } else {
                        throw new RdfException("Prefixes could not be parsed. Uneven number. Prefix attribute value should be of type 'ex: http://www.example.com/' ");
                    }
                    this.prefixes.put(prefix, prefixValue);
                }

            }

        }
        return retVal;
    }

    private void addLink(String link) {
        URI absoluteLink = makeLinkAbsolute(link);
        String pageId = getPageId(absoluteLink);
        HashMap<String, String> newLink = new HashMap<String, String>();
        newLink.put("html", link);
        newLink.put("absolute", absoluteLink.toString());
        newLink.put("page", pageId);
        this.links.add(newLink);
    }

    /*
    * Makes a relative link absolute based on the base for this page
    * */
    private URI makeLinkAbsolute(String link) {
        String retVal = link;
        URI uri = UriBuilder.fromUri(link).build();
        String host = uri.getHost();
        String scheme = uri.getScheme();
        String path = uri.getPath();

        if (host == null) {
            host = this.base.getHost();
        }
        if (scheme == null) {
            scheme = this.base.getScheme();
        }
        Path mergedPath = Paths.get(this.base.getPath()).resolve(path);

        return UriBuilder.fromUri("#").scheme(scheme).host(host).path(mergedPath.toString()).build();
    }

    /*
    * Get the pageid for a give url. Returns null if non page exists at this url
    * */
    private String getPageId(URI link) {
        String retVal= null;
        Route route = new Route(link, this.routeController);
        if (route.exists() && route.getNode().getStatusCode() == 200) {
            retVal = route.getNode().getPageUrl();
        }
        return retVal;
    }

    /*
    * Sets the base for all relative urls in this page
    * */
    private void setBase(Element base, URI contextUri) {
        URI baseUri = UriBuilder.fromUri(base.getAttributeValue("href")).build();

        if (base != null && baseUri.getHost() != null && baseUri.getScheme() != null) {
            this.base = cleanUri(baseUri);
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
    public URI getAbsoluterRdfName(String name) throws RdfException, URISyntaxException
    {
        URI retVal = null;
        name = name.trim();
        if (!name.startsWith("http://")) {
            int index = name.indexOf(":");
            if (index > -1) {
                String prefix = name.substring(0, index);
                name = name.substring(index+1);
                String prefixValue = this.prefixes.get(prefix);
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
    * Clean up a url, remove userinfo, port and fragments
    * */
    private URI cleanUri(URI uri) {
        return UriBuilder.fromUri("").scheme(uri.getScheme()).host(uri.getHost()).path(uri.getPath()).build();
    }


}
