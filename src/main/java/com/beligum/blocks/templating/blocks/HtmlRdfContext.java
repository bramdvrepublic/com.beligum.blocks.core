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

import com.beligum.blocks.config.Settings;
import com.beligum.blocks.utils.RdfTools;
import com.google.common.collect.ImmutableMap;
import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.StartTag;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * This class holds a representation of the RDF context, while parsing an RDFa annotated HTML file.
 * Eg. the currently active default vocabulary and the currently active RDF prefixes for CURIE expansion.
 */
public class HtmlRdfContext
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private static URI cachedDefaultRdfVocabAttr;
    private static Map<String, URI> cachedDefaultRdfPrefixAttr;

    private URI sourceUri;
    private Stack<URI> currentVocabStack;
    private Stack<Map<String, URI>> currentPrefixesStack;
    private Set<EndTag> vocabPopTags;
    private Set<EndTag> prefixPopTags;

    //-----CONSTRUCTORS-----
    public HtmlRdfContext(URI sourceUri)
    {
        this.sourceUri = sourceUri;

        this.currentVocabStack = new Stack<>();
        // It doesn't really make sense to start out without any vocab, because all non-prefixed URIs are
        // by default connected to our own local ontology.
        // So the scope of the default ontology is always assumed.
        this.currentVocabStack.push(getDefaultRdfVocab());

        this.vocabPopTags = new HashSet<>();
        this.prefixPopTags = new HashSet<>();

        //Note: the RDFa 1.1 spec clearly states the prefix mappings are locally scoped, which means they have to be organized in
        // a stack and built up/torn down according to the html tags.
        //Also note that this represents a stack of maps: the idea is to build a new map (or context) that's (possibly) based on the previous context,
        // and push it on the stack
        this.currentPrefixesStack = new Stack<>();
        this.currentPrefixesStack.push(getDefaultRdfPrefixes());
    }

    //-----PUBLIC STATIC METHODS-----
    public static URI getDefaultRdfVocab()
    {
        if (cachedDefaultRdfVocabAttr == null) {
            cachedDefaultRdfVocabAttr = Settings.instance().getRdfLocalOntologyNamespace().getUri();
        }

        return cachedDefaultRdfVocabAttr;
    }
    public static Map<String, URI> getDefaultRdfPrefixes()
    {
        if (cachedDefaultRdfPrefixAttr == null) {
            //TODO ideally, this should set the other prefixes too..., but it's more complex...
            cachedDefaultRdfPrefixAttr = ImmutableMap.<String, URI>builder().put(Settings.instance().getRdfLocalOntologyNamespace().getPrefix(),
                                                                                 Settings.instance().getRdfLocalOntologyNamespace().getUri())
                                                                            .build();
        }

        return cachedDefaultRdfPrefixAttr;
    }

    //-----PUBLIC METHODS-----
    /**
     * Update the internal context with relevant attributes from the supplied start tag
     */
    public void updateContext(StartTag tag) throws IOException
    {
        //if the tag is not stand-alone, parse and possibly push the new context on the stack and save it's end tag for popping
        if (!tag.isEmptyElementTag() && tag.getElement().getEndTag() != null) {

            //since it's not an empty element tag, it should have an end tag
            EndTag endTag = tag.getElement().getEndTag();

            URI newVocab = this.parseRdfVocabAttribute(tag);
            if (newVocab != null) {
                this.pushVocabulary(newVocab);
                //the idea is to keep a list of end tags where we need to pop the stack and
                // just assume they will be called in the right order (which should be the case because we have an end tag)
                this.vocabPopTags.add(endTag);
            }

            Map<String, URI> newPrefixes = this.parseRdfPrefixAttribute(tag);
            if (newPrefixes != null) {
                this.pushPrefixes(newPrefixes);
                this.prefixPopTags.add(endTag);
            }
        }
    }
    /**
     * Check to see if we need to pop the context for the supplied end tag
     */
    public void updateContext(EndTag tag) throws Exception
    {
        //pop the vocab stack
        if (this.vocabPopTags.contains(tag)) {
            this.currentVocabStack.pop();
            this.vocabPopTags.remove(tag);
        }

        //pop the prefix stack
        if (this.prefixPopTags.contains(tag)) {
            this.currentPrefixesStack.pop();
            this.prefixPopTags.remove(tag);
        }
    }
    /**
     * This will normalize (canonicalize) the supplied property-attribute value, taking into account the currently configured RDF context.
     * Eg. blabla 'can' become blabla, but also http://www.example.com/ontology/blabla, depending on the active default vocabulary.
     * Also, eg; the ex:blabla CURIE will be expanded to it's full form, but only if the 'ex' ontology is active in the current context
     */
    public String normalizeProperty(StartTag tag, String value) throws IOException
    {
        // According to http://www.w3.org/TR/rdfa-syntax/#A-property
        // a property is a "A white space separated list of TERMorCURIEorAbsIRIs"
        // We'll ignore the possibility it's a list for now, so a TERMorCURIEorAbsIRI can be one of these:
        // - TERM: a xs:Name with pattern [\i-[:]][/\c-[:]]*
        // - CURIE: a xs:string of min length 1 with pattern (([\i-[:]][\c-[:]]*)?:)?(/[^\s/][^\s]*|[^\s/][^\s]*|[^\s]?)
        // - AbsIRI: a xs:string with pattern [\i-[:]][\c-[:]]+:.+
        // TODO Start here (http://www.w3.org/TR/rdfa-syntax/#P_term) for a better implementation, this is just a first tryout

        String retVal = value;

        if (!StringUtils.isEmpty(value)) {

            retVal = value.trim();

            //this means it can be a URI or a CURIE
            if (retVal.contains(":")) {

                //first, check if we're dealing with a full blown URI
                URI uri = null;
                try {
                    //Note that this will NOT throw an exception in case of a CURIE (which is a valid URI)
                    uri = URI.create(retVal);
                }
                catch (IllegalArgumentException e) {
                    //ignored
                }

                //here we must try to expand a CURIE
                if (uri != null) {

                    if (RdfTools.isCurie(uri)) {

                        boolean validCurie = false;
                        if (!this.currentPrefixesStack.isEmpty()) {
                            URI prefix = this.currentPrefixesStack.peek().get(uri.getScheme());
                            if (prefix != null) {
                                String prefixUri = prefix.toString();
                                if (!prefixUri.endsWith("/")) {
                                    prefixUri += "/";
                                }
                                String suffix = uri.getSchemeSpecificPart();
                                while (suffix.startsWith("/")) {
                                    suffix = suffix.substring(1);
                                }
                                retVal = prefixUri + suffix;
                                validCurie = true;
                            }
                        }

                        //it makes sense to stop if we decided we're dealing with a curie, but it can't be expanded in the current context
                        if (!validCurie) {
                            throw new IOException("Encountered attribute '" + value + "' in tag <" + tag + "> as a CURIE with an unknown prefix '" + uri.getScheme() + "' in this context; " +
                                                  this.sourceUri);
                        }
                    }
                    else {
                        retVal = uri.toString();
                    }
                }
                else {
                    throw new IOException("Encountered attribute '" + value + "' in tag <" + tag + "> as a URI or CURIE but it didn't parse to a valid URI; " + this.sourceUri);
                }
            }
            //if the value is no CURIE or URI, prefix it with the currentVocab if we have one
            else if (!this.currentVocabStack.isEmpty()) {
                String prefixUri = this.currentVocabStack.peek().toString();
                if (!prefixUri.endsWith("/")) {
                    prefixUri += "/";
                }
                String suffix = retVal;
                while (suffix.startsWith("/")) {
                    suffix = suffix.substring(1);
                }
                retVal = prefixUri + suffix;
            }
            //the value is no URI, CURIE and we don't have a vocab; it's invalid
            else {
                throw new IOException("Encountered attribute '" + value + "' in tag <" + tag + "> that is not connected to any vocabulary or ontology. As much as I want to allow this, I can't; " +
                                      this.sourceUri);
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private URI pushVocabulary(URI vocab)
    {
        return this.currentVocabStack.push(vocab);
    }
    private Map<String, URI> pushPrefixes(Map<String, URI> prefixes)
    {
        Map<String, URI> frame = new LinkedHashMap<>();

        //we start by copying over all existing prefixes
        if (!this.currentPrefixesStack.isEmpty()) {
            frame.putAll(this.currentPrefixesStack.peek());
        }

        //then, we add all new prefixes, possibly overwriting existing ones in the frame
        //(which is exactly what we want)
        frame.putAll(prefixes);

        return this.currentPrefixesStack.push(frame);
    }
    private URI parseRdfVocabAttribute(StartTag tag) throws IOException
    {
        URI retVal = null;

        Attributes attrs = tag.getAttributes();
        if (attrs != null) {
            Attribute attr = attrs.get(HtmlParser.RDF_VOCAB_ATTR);
            if (attr != null && attr.hasValue()) {
                try {
                    retVal = URI.create(attr.getValue().trim());
                }
                catch (IllegalArgumentException e) {
                    throw new IOException("You supplied a '" + HtmlParser.RDF_VOCAB_ATTR + "' attribute value in tag <" + tag.getName() + "> of source '" + this.sourceUri +
                                          "', but it doesn't seem to be a valid URI; " + attr.getValue(), e);
                }
            }
        }

        return retVal;
    }
    private Map<String, URI> parseRdfPrefixAttribute(StartTag tag) throws IOException
    {
        Map<String, URI> retVal = null;

        Attributes attrs = tag.getAttributes();
        if (attrs != null) {
            Attribute attr = attrs.get(HtmlParser.RDF_PREFIX_ATTR);
            if (attr != null && attr.hasValue()) {
                String[] prefixAttrSplit = attr.getValue().trim().split(" ");
                if (prefixAttrSplit.length % 2 != 0) {
                    throw new IOException("You supplied a '" + HtmlParser.RDF_PREFIX_ATTR + "' attribute value in tag <" + tag.getName() + "> of source '" + this.sourceUri +
                                          "', but it doesn't contain an even space-separated list that form one (or more) key-value pairs; " + attr.getValue());
                }
                for (int i = 0; i < prefixAttrSplit.length; i += 2) {
                    String p = prefixAttrSplit[i];
                    URI uri = null;
                    try {
                        uri = URI.create(prefixAttrSplit[i + 1]);
                    }
                    catch (IllegalArgumentException e) {
                        throw new IOException("You supplied a '" + HtmlParser.RDF_PREFIX_ATTR + "' attribute value in tag <" + tag.getName() + "> of source '" + this.sourceUri +
                                              "', but the value for prefix '" + p + "' doesn't seem to be a valid URI; " + attr.getValue(), e);
                    }

                    if (retVal == null) {
                        retVal = new LinkedHashMap<>();
                    }
                    retVal.put(p, uri);
                }
            }
        }

        return retVal;
    }
}
