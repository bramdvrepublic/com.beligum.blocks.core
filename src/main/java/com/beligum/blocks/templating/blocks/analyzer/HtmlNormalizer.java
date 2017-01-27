package com.beligum.blocks.templating.blocks.analyzer;

import com.beligum.base.resources.ifaces.Source;
import com.beligum.blocks.rdf.sources.PageSource;
import com.google.common.collect.ImmutableSet;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.StartTag;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by bram on 1/26/17.
 */
public class HtmlNormalizer
{
    //-----CONSTANTS-----
    private static final Set<String> REFERENCE_ATTRS = ImmutableSet.of("src", "href", "content");

    //-----VARIABLES-----
    private Source source;
    private String title;

    //-----CONSTRUCTORS-----
    public HtmlNormalizer(Source source)
    {
        this.source = source;
    }

    //-----PUBLIC METHODS-----
    public CharSequence process(Element node) throws IOException
    {
        StartTag startTag = node.getStartTag();
        Iterator<Segment> nodeIter = node.getNodeIterator();

        //wind down the iterator until we reach the start tag (this should be immediate)
        // to sync with what the the parse() method expects (a pastStartIterator)
        while (nodeIter.hasNext() && !nodeIter.next().equals(startTag)) ;
        if (!nodeIter.hasNext()) {
            throw new IOException("Couldn't locate the start tag of this element; this shouldn't happen; " + node);
        }

        //we start out with no context at all, so all false
        HtmlTag root = new HtmlTag(this.source, startTag);
        this.parse(root, nodeIter, false);

        return root.toNormalizedString();
    }
    public String getTitle()
    {
        return title;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    /**
     * The basic idea here is to use recursive calls to create stack frames that go together with each new start tag.
     * <p>
     * There are two kinds of context-switches:
     * <p>
     * - when a template-tag is crossed
     * template tags demarcate property contexts, so if we cross one, we need to reset the property context
     * <p>
     * - when a property-tag is crossed
     * a new property context should be created and everything until the end tag should be saved as-is
     * <p>
     * - (and a combination of both; property-annotated template tags)
     * <p>
     * Page template tags are special cases because we want to save their start and end tags if they're in a property context or not.
     * <p>
     * All normalized data is stored inside the HtmlTags and the tag is responsible to add new data or not,
     * see eg. setNormalizedStartTag() and toNormalizedString()
     */
    private void parse(HtmlTag htmlTag, Iterator<Segment> pastStartIterator, boolean propertyContext) throws IOException
    {
        //1) process the start tag

        if (htmlTag.isProperty()) {
            propertyContext = true;
        }

        //because a start tag can be either stand-alone or not and we can't skip the very first tag,
        // we need to handle all start tags here (and not in the iterator below)
        htmlTag.setNormalizedStartTag(htmlTag.getStartTag(), propertyContext);

        //extract some properties to save them along the way
        this.extractMetaData(htmlTag);

        //no need to process the content or end of the tag if we don't have any (start tag will be rendered out differently too)
        if (!htmlTag.isStandAlone()) {

            //if we have crossed template context, we need to reset the property context for it's content,
            //except if we're immediately starting a new one because the template-tag has a property attribute
            //Note that we're defining a new flag here, because it's only the _content_ that needs a reset,
            //the start and end follow the parent content flags.
            boolean contentPropertyContext = propertyContext;
            if (htmlTag.isTemplate()) {
                contentPropertyContext = htmlTag.isProperty();
            }

            //2) process the content
            boolean encounteredEndTag = false;
            while (pastStartIterator.hasNext() && !encounteredEndTag) {
                Segment node = pastStartIterator.next();

                if (node instanceof StartTag) {
                    HtmlTag startTag = new HtmlTag(this.source, (StartTag) node);
                    //start a new tag; add a frame to the (java) stack by calling this function recursively
                    this.parse(startTag, pastStartIterator, contentPropertyContext);
                    //note that we force the normalized content to be appended, property context or not
                    // because what comes out of parse() is already normalized (and will yield "" if unused)
                    htmlTag.appendNormalizedSubtag(startTag);
                }
                else if (node instanceof EndTag) {
                    //stop processing the content if we reached the end
                    if (htmlTag.equalsEndTag(node)) {
                        encounteredEndTag = htmlTag.equalsEndTag(node);
                    }
                    else {
                        htmlTag.setNormalizedEndTag(htmlTag.getEndTag(), contentPropertyContext);
                    }
                }
                //not a start tag and not an end tag; all other in-between texts, templates, scripts, styles, comments, etc.
                else {
                    htmlTag.appendNormalizedContent(node, contentPropertyContext);
                }
            }

            //3) process the end tag
            if (encounteredEndTag) {
                htmlTag.setNormalizedEndTag(htmlTag.getEndTag(), propertyContext);
            }
        }
    }
    /**
     * Extract and store some general information about the HTML document in this class
     * while we're iterating the document anyway.
     */
    private void extractMetaData(HtmlTag htmlTag)
    {
        this.extractTitle(htmlTag);
        //        this.extractReferences(htmlTag);
    }
    /**
     * Check if the tag is a title tag and extract and save it's value if it is.
     */
    private void extractTitle(HtmlTag htmlTag)
    {
        if (htmlTag.getName().equalsIgnoreCase(PageSource.HTML_TITLE_ELEMENT)) {
            this.title = htmlTag.getContent();
            if (!StringUtils.isEmpty(this.title)) {
                this.title = this.title.trim();
            }
        }
    }

    //    /**
    //     * Extract and save the internal (internal pages to this site) and external (http/https/ftp/...) references
    //     * in this tag.
    //     */
    //    private void extractReferences(HtmlTag htmlTag)
    //    {
    //        Attributes startTagAttrs = startTag.getAttributes();
    //
    //        if (startTagAttrs != null) {
    //            for (Attribute attr : startTagAttrs) {
    //                if (REFERENCE_ATTRS.contains(attr.getName())) {
    //                    if (!StringUtils.isEmpty(attr.getValue())) {
    //                        try {
    //                            //validate the reference
    //                            URI uri = URI.create(attr.getValue());
    //                            //if the url is relative to this domain or is abolute and inside this domain, store as internal ref
    //                            //note that we need to include the port in the check (authority instead of host)
    //                            //TODO: note that, for now, this will also contain garbage URI's that passed the create() test above like "IE=edge"
    //                            if (StringUtils.isEmpty(uri.getAuthority()) || SITE_DOMAINS.contains(uri.getAuthority())) {
    //                                this.internalRefs.put(uri, new ReferenceRef(attr));
    //                            }
    //                            //otherwise it's an external ref
    //                            else {
    //                                this.externalRefs.put(uri, new ReferenceRef(attr));
    //                            }
    //                        }
    //                        catch (IllegalArgumentException e) {
    //                            Logger.debug("Encountered illegal URI as an attribute value of " + attr + " in " + startTag, e);
    //                        }
    //                    }
    //                }
    //            }
    //        }
    //    }
    //public class ReferenceRef
    //{
    //    public final Attribute attribute;
    //
    //    protected ReferenceRef(Attribute attribute)
    //    {
    //        this.attribute = attribute;
    //    }
    //}
}
