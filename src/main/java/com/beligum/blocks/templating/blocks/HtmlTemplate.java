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
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceAction;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.security.SecurityManager;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.UriDetector;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.controllers.BlocksReferenceController;
import com.beligum.blocks.rdf.ifaces.RdfClass;
import com.beligum.blocks.rdf.ifaces.RdfProperty;
import com.beligum.blocks.rdf.ifaces.RdfOntology;
import com.beligum.blocks.rdf.ontologies.Blocks;
import com.beligum.blocks.templating.blocks.directives.TagTemplateResourceDirective;
import com.beligum.blocks.templating.blocks.directives.TemplateResourcesDirective;
import com.beligum.blocks.utils.RdfTools;
import com.google.common.base.Enums;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import gen.com.beligum.blocks.core.messages.blocks.core;
import net.htmlparser.jericho.*;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.annotation.Nullable;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

import static com.beligum.blocks.templating.blocks.HtmlParser.RDF_CONTENT_ATTR;
import static com.beligum.blocks.templating.blocks.HtmlParser.RDF_PROPERTY_ATTR;

/**
 * Created by bram on 5/13/15.
 */
public abstract class HtmlTemplate
{
    //-----CONSTANTS-----
    // Controls if the <script> or <style> tag needs to be included in the rendering
    // use it like this: <script data-scope-perm="$CONSTANTS.blocks.core.PAGE_CREATE_ALL_PERM"> to eg. only include the script when
    // the page create permission is required
    // Note that the value can be a CSV
    public static final String ATTRIBUTE_RESOURCE_PERMISSION_SCOPE = "data-scope-perm";

    // Set to the lower cased values of ResourceSecurityAction if you only want the resource
    // to be included when in that action scope
    // Note that the value can be a CSV
    public static final String ATTRIBUTE_RESOURCE_ACTION_SCOPE = "data-scope-action";

    //set to 'skip' to skip this resource during the resource collecting phase and instead render it out where it's defined
    public static final String ATTRIBUTE_RESOURCE_JOIN_HINT = "data-join-hint";

    public enum ResourceJoinHint
    {
        UNDEFINED,
        EMPTY,
        skip
    }

    public enum SpecialProperty
    {
        //specifying "_all" means we indicate we want to include/allow all properties of a specific vocabulary context
        _vocab,
        //specifying "_class" means we indicate we want to include/allow all properties of a specific class context
        _class
    }

    /**
     * Flag to either make this block publicly visible to the page editor
     * or make it visible internally only (eg. a "system" block)
     */
    public enum MetaDisplayType
    {
        DEFAULT,
        HIDDEN
    }

    /**
     * Flag to decide how to render out this block. Inline blocks are rendered out without their wrapping tags
     * and result in special behavior. Because inline blocks get serialized to the client without tag,
     * it's inner html is just spit out in the parent context, and the parser has no means of wiring possible properties back together
     * when the page is returned to be saved. On top, all properties in the template definition file are "attached" to the context
     * of the parent in which the inline block is instantiated. This also means that inline template instances can't have properties
     * because it doesn't make sense; we have no means to attach them back during serialization. So, property tags are only permitted
     * in the definition of the template, not instances.
     */
    public enum MetaRenderType
    {
        DEFAULT,
        INLINE
    }

    protected static final Pattern styleLinkRelAttrValue = Pattern.compile("stylesheet");
    protected static final ResourceAction[] NO_RESOURCE_ACTION = { ResourceAction.NONE };

    //-----VARIABLES-----
    protected Path absolutePath;
    protected Path relativePath;
    protected String templateName;
    //this will enable us to save the 'inheritance tree'
    protected HtmlTemplate superTemplate;
    protected boolean disabled;
    protected Map<String, String> attributes;
    protected Element rootElement;
    protected String title;
    protected String description;
    protected String icon;
    protected Class<TemplateController> controllerClass;
    protected MetaDisplayType displayType;
    protected MetaRenderType renderType;
    protected Iterable<ScopedResource> inlineScriptElements;
    protected Iterable<ScopedResource> externalScriptElements;
    protected Iterable<ScopedResource> inlineStyleElements;
    protected Iterable<ScopedResource> externalStyleElements;
    protected Iterable<ResourceReference> resourceReferences;
    // This will hold the html before the <template> tags
    protected Segment prefixHtml;
    // This will hold the html inside the <template> tags
    protected Segment innerHtml;
    // This will hold the html after the <template> tags
    protected Segment suffixHtml;
    protected List<SubstitionReference> normalizationSubstitutions;
    protected List<String> rdfProperties;
    protected SpecialProperty rdfPropertiesSpecial;
    protected List<String> nonRdfProperties;
    //make sure to check init() (and make sure it's re-inited when init() is called) if you add more variables

    //-----CONSTRUCTORS-----
    public static HtmlTemplate create(String templateName, Source source, Path absolutePath, Path relativePath, HtmlTemplate superTemplate) throws Exception
    {
        HtmlTemplate retVal = null;

        if (isTagTemplate(source)) {
            retVal = new TagTemplate(templateName, source, absolutePath, relativePath, superTemplate);
        }
        else if (isPageTemplate(source)) {
            retVal = new PageTemplate(templateName, source, absolutePath, relativePath, superTemplate);
        }
        else {
            com.beligum.base.utils.Logger.warn("Encountered a html template that not a page and not a tag, returning null; " + relativePath);
        }

        return retVal;
    }
    protected void init(String templateName, Source source, Path absolutePath, Path relativePath, HtmlTemplate superTemplate) throws Exception
    {
        //INIT THE PATHS
        this.absolutePath = absolutePath;
        this.relativePath = relativePath;

        //INIT THE NAMES
        this.templateName = templateName;
        this.superTemplate = superTemplate;
        this.disabled = false;

        //INIT THE HTML
        //note: this should take the parent into account
        OutputDocument tempHtml = this.doInitHtmlPreparsing(new OutputDocument(source), superTemplate);

        //Note that we need to eat these values for PageTemplates because we don't want them to end up at the client side (no problem for TagTemplates)
        this.title = superTemplate != null ? superTemplate.getTitle() : null;
        String thisTitle = this.getMetaValue(tempHtml, Blocks.title, true);
        if (!StringUtils.isEmpty(thisTitle)) {
            this.title = thisTitle;
        }
        if (StringUtils.isEmpty(this.title)) {
            this.title = core.Entries.emptyTemplateTitle.toString();
        }

        this.description = superTemplate != null ? superTemplate.getDescription() : null;
        String thisDescription = this.getMetaValue(tempHtml, Blocks.description, true);
        if (!StringUtils.isEmpty(thisDescription)) {
            this.description = thisDescription;
        }
        if (StringUtils.isEmpty(this.title)) {
            this.description = core.Entries.emptyTemplateTitle.toString();
        }

        this.icon = superTemplate != null ? superTemplate.getIcon() : null;
        String thisIcon = this.getMetaValue(tempHtml, Blocks.icon, true);
        if (!StringUtils.isEmpty(thisIcon)) {
            this.icon = thisIcon;
        }
        if (StringUtils.isEmpty(this.title)) {
            this.icon = core.Entries.emptyTemplateTitle.toString();
        }

        String controllerClassStr = this.getMetaValue(tempHtml, Blocks.controller, true);
        if (!StringUtils.isEmpty(controllerClassStr)) {
            Class<?> clazz = Class.forName(controllerClassStr);
            if (TemplateController.class.isAssignableFrom(clazz)) {
                this.controllerClass = (Class<TemplateController>) clazz;
            }
            else {
                throw new ParseException("Encountered template with a controller that doesn't implement " + TemplateController.class.getSimpleName() + "; " + relativePath, 0);
            }
        }
        //parent controller is the backup if this child doesn't have one
        if (this.controllerClass == null && superTemplate != null) {
            this.controllerClass = superTemplate.getControllerClass();
        }

        this.displayType = superTemplate != null ? superTemplate.getDisplayType() : MetaDisplayType.DEFAULT;
        String displayType = this.getMetaValue(tempHtml, Blocks.display, true);
        if (!StringUtils.isEmpty(displayType)) {
            this.displayType = MetaDisplayType.valueOf(displayType.toUpperCase());
        }

        this.renderType = superTemplate != null ? superTemplate.getRenderType() : MetaRenderType.DEFAULT;
        String renderType = this.getMetaValue(tempHtml, Blocks.render, true);
        if (!StringUtils.isEmpty(renderType)) {
            this.renderType = MetaRenderType.valueOf(renderType.toUpperCase());
            if (this instanceof PageTemplate && this.renderType.equals(MetaRenderType.INLINE)) {
                throw new ParseException("Encountered page template with an inline render meta tag. Inline rendering only makes sense for tag templates; " + relativePath, 0);
            }
        }

        //see below in parseHtml() for why these are initialized to null and not empty
        this.rdfProperties = null;
        this.rdfPropertiesSpecial = null;
        this.nonRdfProperties = null;
        String properties = this.getMetaValue(tempHtml, Blocks.properties, true);
        //note that we distinguish between non-existing and empty
        if (properties != null) {
            this.rdfProperties = new ArrayList<>();
            if (!StringUtils.isEmpty(properties)) {
                //let's support both spaces and commas as delimeters
                for (String prop : Splitter.onPattern("[ ,]").trimResults().omitEmptyStrings().split(properties)) {
                    //this will also support constants
                    String p = R.resourceManager().getTemplateEngine().serializePropertyKey(prop);
                    this.rdfPropertiesSpecial = Enums.getIfPresent(SpecialProperty.class, p).orNull();

                    //if it's not a special value, just add it to the list
                    if (this.rdfPropertiesSpecial == null) {
                        this.rdfProperties.add(p);
                    }
                    else {
                        break;
                    }
                }

                if (this.rdfPropertiesSpecial != null && !this.rdfProperties.isEmpty()) {
                    Logger.error("Encountered properties metadata in " + this.relativePath + " that contained other values next to a special value," +
                                 " This is not supported and only the first special value will be retained; " + this.rdfPropertiesSpecial);
                    this.rdfProperties.clear();
                }
            }
        }
        //note that for now, there's no meta tag for non-rdf properties (we just set it to zero and have it auto-filled in parseHtml())
        //(mainly because those are always there as placeholders for true rdf properties, see eg. blocks-fact-entry)

        this.inlineStyleElements = parseInlineStyles(tempHtml);
        this.externalStyleElements = parseExternalStyles(tempHtml);
        this.inlineScriptElements = parseInlineScripts(tempHtml);
        this.externalScriptElements = parseExternalScripts(tempHtml);
        // TODO working on this for future use, don't use it yet
        this.resourceReferences = parseResourceReferences(tempHtml);

        //prepend the html with the parent resources if it's there
        if (superTemplate != null) {
            StringBuilder superTemplateResourceHtml = new StringBuilder();
            this.inlineStyleElements = addSuperTemplateResources(TemplateResourcesDirective.Argument.inlineStyles,
                                                                 superTemplateResourceHtml,
                                                                 this.inlineStyleElements,
                                                                 superTemplate.getInlineStyleElements(),
                                                                 null);
            this.externalStyleElements = addSuperTemplateResources(TemplateResourcesDirective.Argument.externalStyles,
                                                                   superTemplateResourceHtml,
                                                                   this.externalStyleElements,
                                                                   superTemplate.getExternalStyleElements(),
                                                                   "href");
            this.inlineScriptElements = addSuperTemplateResources(TemplateResourcesDirective.Argument.inlineScripts,
                                                                  superTemplateResourceHtml,
                                                                  this.inlineScriptElements,
                                                                  superTemplate.getInlineScriptElements(),
                                                                  null);
            this.externalScriptElements = addSuperTemplateResources(TemplateResourcesDirective.Argument.externalScripts,
                                                                    superTemplateResourceHtml,
                                                                    this.externalScriptElements,
                                                                    superTemplate.getExternalScriptElements(),
                                                                    "src");

            this.resourceReferences = Iterables.concat(superTemplate.getResourceReferences(), this.resourceReferences);

            //insert all (processed) super resources at the beginning of the html
            tempHtml.insert(0, superTemplateResourceHtml);
        }

        //now save the (possibly altered) html source (and unwrap it in case of a tag template)
        this.saveHtml(tempHtml, superTemplate);

        //once we have the final html saved, we'll parse it again to mark the template variables for normalization,
        // calculate a standardized version for comparison, etc.
        this.parseHtml();
    }

    //-----PUBLIC STATIC METHODS-----
    /**
     * Returns the join hint of the supplied element (guaranteed non-null)
     */
    public static ResourceJoinHint getResourceJoinHint(Element resource)
    {
        ResourceJoinHint retVal = null;

        Attribute scope = resource.getAttributes().get(ATTRIBUTE_RESOURCE_JOIN_HINT);
        if (scope == null) {
            retVal = ResourceJoinHint.UNDEFINED;
        }
        else if (StringUtils.isEmpty(scope.getValue())) {
            retVal = ResourceJoinHint.EMPTY;
        }
        else {
            //this will throw an exception if nothing was found
            retVal = ResourceJoinHint.valueOf(scope.getValue());
        }

        return retVal;
    }
    /**
     * Test if ALL of the supplied permissions are permitted in the current context
     */
    public static boolean testResourcePermissionScope(Iterable<String> permissions)
    {
        // By default, we assume this is a public resource: we default to true
        boolean retVal = true;

        if (permissions != null) {

            Iterator<String> iter = permissions.iterator();
            if (iter.hasNext()) {
                SecurityManager securityManager = R.securityManager();
                while (iter.hasNext()) {
                    //note that multiple permissions are ANDed together;
                    //when the perm list is not empty, all should pass,
                    //so if one doesn't pass, we immediately return false
                    if (!securityManager.isPermitted(iter.next())) {
                        retVal = false;
                        break;
                    }
                }
            }
        }

        return retVal;
    }
    /**
     * Test if ANY of the supplied actions are permitted in the current context
     */
    public static boolean testResourceActionScope(Iterable<ResourceAction> actions)
    {
        // By default, we assume this is a public resource: we default to true
        boolean retVal = true;

        if (actions != null) {

            Iterator<ResourceAction> iter = actions.iterator();
            if (iter.hasNext()) {
                //as soon as we have one or more actions configured, at least one of the configured actions should be allowed,
                //so we'll switch to a default of false and break if we find a matching action
                retVal = false;

                ResourceAction currentResourceAction = R.cacheManager().getRequestCache().get(CacheKeys.RESOURCE_ACTION);
                //this means we'll return false if we're not doing anything
                if (currentResourceAction != null) {
                    while (iter.hasNext()) {
                        if (currentResourceAction.equals(iter.next())) {
                            retVal = true;
                            break;
                        }
                    }
                }
            }
        }

        return retVal;
    }
    /**
     * Returns true if the supplied tag is a property tag
     */
    public static boolean isPropertyTag(StartTag startTag)
    {
        return isRdfPropertyTag(startTag) || isNonRdfPropertyTag(startTag);
    }
    /**
     * Same as isPropertyTag, but only for rdf property attributes
     */
    public static boolean isRdfPropertyTag(StartTag startTag)
    {
        return startTag.getAttributeValue(HtmlParser.RDF_PROPERTY_ATTR) != null;
    }
    /**
     * Same as isPropertyTag, but only for non-rdf data-property attributes
     */
    public static boolean isNonRdfPropertyTag(StartTag startTag)
    {
        return startTag.getAttributeValue(HtmlParser.NON_RDF_PROPERTY_ATTR) != null;
    }
    /**
     * Same isPropertyTag as the method above, but with a JSoup element
     */
    public static boolean isPropertyTag(org.jsoup.nodes.Element element)
    {
        return isRdfPropertyTag(element) || isNonRdfPropertyTag(element);
    }
    /**
     * Same as isPropertyTag, but only for rdf property attributes
     */
    public static boolean isRdfPropertyTag(org.jsoup.nodes.Element element)
    {
        return element.hasAttr(HtmlParser.RDF_PROPERTY_ATTR);
    }
    /**
     * Same as isPropertyTag, but only for non-rdf data-property attributes
     */
    public static boolean isNonRdfPropertyTag(org.jsoup.nodes.Element element)
    {
        return element.hasAttr(HtmlParser.NON_RDF_PROPERTY_ATTR);
    }
    /**
     * Returns true if the supplied name is "property" or "data-property"
     */
    public static boolean isPropertyAttribute(String attributeName)
    {
        return attributeName.equals(HtmlParser.RDF_PROPERTY_ATTR) || attributeName.equals(HtmlParser.NON_RDF_PROPERTY_ATTR);
    }
    /**
     * Returns the value of the property attribute of the supplied tag, in predefined order
     */
    public static String getPropertyAttribute(StartTag startTag)
    {
        String retVal = null;

        Attributes attributes = startTag.getAttributes();

        //regular property has precedence over data-property
        Attribute attribute = attributes.get(HtmlParser.RDF_PROPERTY_ATTR);
        //now try the data-property
        if (attribute == null) {
            attribute = attributes.get(HtmlParser.NON_RDF_PROPERTY_ATTR);
        }

        if (attribute != null) {
            retVal = attribute.getValue();
        }

        return retVal;
    }
    /**
     * Same doIsValid as the method above, but with a JSoup element
     */
    public static String getPropertyAttribute(org.jsoup.nodes.Element element)
    {
        String retVal = null;

        org.jsoup.nodes.Attributes attributes = element.attributes();

        //regular property has precedence over data-property
        //note that we'll return null (compared to what JSoup prescribes) to be compatible with the Jericho method
        if (attributes.hasKey(HtmlParser.RDF_PROPERTY_ATTR)) {
            retVal = attributes.get(HtmlParser.RDF_PROPERTY_ATTR);
        }

        //now try the data-property
        if (retVal == null) {
            if (attributes.hasKey(HtmlParser.NON_RDF_PROPERTY_ATTR)) {
                retVal = attributes.get(HtmlParser.NON_RDF_PROPERTY_ATTR);
            }
        }

        return retVal;
    }
    /**
     * Finds the first tag with a property attribute inside the element and returns it's value
     */
    public static String getFirstPropertyInside(Element element)
    {
        String retVal = null;

        Iterator<Segment> nodes = element.getNodeIterator();
        while (nodes.hasNext() && StringUtils.isEmpty(retVal)) {
            Segment node = nodes.next();
            if (node instanceof StartTag) {
                retVal = HtmlTemplate.getPropertyAttribute((StartTag) node);
            }
        }

        return retVal;
    }
    /**
     * Same doIsValid as the method above, but with a JSoup element
     */
    public static String getFirstPropertyInside(org.jsoup.nodes.Element element)
    {
        String retVal = null;

        Elements propertyElements = element.select("[" + HtmlParser.RDF_PROPERTY_ATTR + "], [" + HtmlParser.NON_RDF_PROPERTY_ATTR + "]");
        if (!propertyElements.isEmpty()) {
            retVal = propertyElements.first().attr(HtmlParser.RDF_PROPERTY_ATTR);
            if (StringUtils.isEmpty(retVal)) {
                retVal = propertyElements.first().attr(HtmlParser.NON_RDF_PROPERTY_ATTR);
            }
        }

        return retVal;
    }
    public static boolean isTemplateInstanceTag(StartTag startTag)
    {
        return getTemplateInstance(startTag) != null;
    }
    public static HtmlTemplate getTemplateInstance(StartTag startTag)
    {
        //note: getByTagName() can handle null values
        return TemplateCache.instance().getByTagName(startTag.getName().equals("html") ? startTag.getAttributeValue(HtmlParser.HTML_ROOT_TEMPLATE_ATTR) : startTag.getName());
    }
    public static boolean isTemplateInstanceTag(org.jsoup.nodes.Element element)
    {
        return getTemplateInstance(element) != null;
    }
    public static HtmlTemplate getTemplateInstance(org.jsoup.nodes.Element element)
    {
        //note: getByTagName() can handle null values
        return TemplateCache.instance().getByTagName(element.tagName().equals("html") ? element.attr(HtmlParser.HTML_ROOT_TEMPLATE_ATTR) : element.tagName());
    }
    public static com.beligum.base.resources.ifaces.Source prepareForSave(com.beligum.base.resources.ifaces.Source source) throws IOException
    {
        String result = HtmlTemplate.callTemplateControllerMethods(source, new ControllerCallback()
        {
            @Override
            public void encounteredTemplateTagController(Source htmlSource, OutputDocument htmlOutput, Element element, TemplateController controller) throws IOException
            {
                controller.prepareForSave(source, element, htmlOutput);
            }
        });

        return new StringSource(source.getUri(), result, source.getMimeType(), source.getLanguage());
    }
    public static com.beligum.base.resources.ifaces.Source prepareForCopy(com.beligum.base.resources.ifaces.Source source, URI targetUri, Locale targetLanguage) throws IOException
    {
        String result = HtmlTemplate.callTemplateControllerMethods(source, new ControllerCallback()
        {
            @Override
            public void encounteredTemplateTagController(Source htmlSource, OutputDocument htmlOutput, Element element, TemplateController controller) throws IOException
            {
                controller.prepareForCopy(source, element, htmlOutput, targetUri, targetLanguage);
            }
        });

        return new StringSource(source.getUri(), result, source.getMimeType(), source.getLanguage());
    }
    public static boolean isResourceElement(Element element)
    {
        String name = element.getName().toLowerCase();
        String relAttr = element.getAttributeValue("rel");
        return name.equals("script") || name.equals("style") || (name.equals("link") && relAttr != null && relAttr.trim().equalsIgnoreCase("stylesheet"));
    }
    /**
     * Use this method instead of the Source constructor with an inputStream:
     * We got in trouble with special characters if we didn't explicitly read the file as UTF-8
     * because the auto-detect functions of Jericho mis-detected the encoding of the inputStream.
     * Because Jericho reads the entire file in anyway, let's do the same, but forced to the right encoding.
     */
    public static Source readHtmlInputStream(InputStream inputStream) throws IOException
    {
        return new Source(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
    }

    //-----PUBLIC METHODS-----
    /**
     * Controls if we need to wrap the instance with the <template-name></template-name> tag or not (eg. for page templates, we don't want tturn
     */
    public abstract boolean renderTemplateTag();

    /**
     * @return the name of the file, without the file extension, where parent directories are represented by dashes.
     * Eg. /blocks/doIsValid/tag.html will have the name "blocks-doIsValid-tag" and result in tags like <blocks-doIsValid-tag></blocks-doIsValid-tag>
     * Please note that the HTML spec forces you to include at lease one dash in custom tag names to ensure their future tag names.
     */
    public String getTemplateName()
    {
        return templateName;
    }
    /**
     * @return the html before the <template> tags
     */
    public Segment getPrefixHtml()
    {
        return prefixHtml;
    }
    /**
     * @return the html inside the <template> tags
     */
    public Segment getInnerHtml()
    {
        return innerHtml;
    }
    /**
     * @return the html after the <template> tags
     */
    public Segment getSuffixHtml()
    {
        return suffixHtml;
    }
    public Map<String, String> getAttributes()
    {
        return attributes;
    }
    public Path getAbsolutePath()
    {
        return absolutePath;
    }
    public Path getRelativePath()
    {
        return relativePath;
    }
    public String getTitle()
    {
        return title;
    }
    public String getDescription()
    {
        return description;
    }
    public String getIcon()
    {
        return icon;
    }
    public Iterable<String> getInlineScriptElementsForCurrentScope()
    {
        return this.buildScopedResourceIterator(this.getInlineScriptElements());
    }
    public Iterable<String> getExternalScriptElementsForCurrentScope()
    {
        return this.buildScopedResourceIterator(this.getExternalScriptElements());
    }
    public Iterable<String> getInlineStyleElementsForCurrentScope()
    {
        return this.buildScopedResourceIterator(this.getInlineStyleElements());
    }
    public Iterable<String> getExternalStyleElementsForCurrentScope()
    {
        return this.buildScopedResourceIterator(this.getExternalStyleElements());
    }
    /**
     * Will instance a new html tag; eg for <template class="classname"></template>,
     * this will return <tag-name class="classname"></tag-name>
     * Depending on the flag, it will include all inner (default) html or not
     */
    public CharSequence createNewHtmlInstance(boolean includeInnerHtml)
    {
        StringBuilder retVal = new StringBuilder();

        retVal.append(this.buildStartTag());

        if (includeInnerHtml) {
            retVal.append(this.getInnerHtml());
        }

        retVal.append(this.buildEndTag());

        return retVal;
    }
    public MetaDisplayType getDisplayType()
    {
        return displayType;
    }
    public MetaRenderType getRenderType()
    {
        return renderType;
    }
    public Element getRootElement()
    {
        return rootElement;
    }
    /**
     * Returns a mapping between JSoup cssSelector strings and places that can be folded/substituted in this template
     */
    public List<SubstitionReference> getNormalizationSubstitutions() throws IOException
    {
        return this.normalizationSubstitutions;
    }

    /**
     * Returns true if this template has or can have any properties at all, regardless of context, filters, etc. (see below)
     */
    public boolean hasProperties()
    {
        return this.hasRdfProperties() || this.hasNonRdfProperties();
    }

    /**
     * Returns true if this template has or can have any properties at all, regardless of context, filters, etc. (see below)
     */
    public boolean hasRdfProperties()
    {
        return this.rdfPropertiesSpecial != null || (this.rdfProperties != null && !this.rdfProperties.isEmpty());
    }

    /**
     * Returns true if this template has any non-rdf (data-property) properties, regardless of context, filters, etc. (see below)
     */
    public boolean hasNonRdfProperties()
    {
        return this.nonRdfProperties != null && !this.nonRdfProperties.isEmpty();
    }

    /**
     * Returns the properties in this template (note: it doesn't return the properties of possible sub-template instances).
     *
     * @param includeRdfProperties    add the rdf properties to the retVal
     * @param includeNonRdfProperties add the non-rdf properties (data-property attribute) to the retVal
     * @param defaultVocab            the vocabulary context in which this properties request is performed
     * @param defaultClass            the rdf class context in which this properties request is performed
     */
    public Iterable<String> getProperties(boolean includeRdfProperties, boolean includeNonRdfProperties, RdfOntology defaultVocab, RdfClass defaultClass)
    {
        //this initialization will make calling this method and parsing below a lot easier
        Iterable<String> retVal = Collections.emptySet();

        //use the vocab of the class if the default vocab is not specified
        final RdfOntology finalDefaultVocab = defaultVocab == null && defaultClass != null ? defaultClass.getOntology() : defaultVocab;

        //this function prefixes the properties with the prefix of the vocab
        // if it's set and it doesn't have another prefix.
        //Note though that data-property values actually shouldn't get a vocab prefix,
        //but we expand them too, for simplicity (and we do this throughout our entire app)
        Function<String, String> prefixProperty = new Function<String, String>()
        {
            @Nullable
            @Override
            public String apply(@Nullable String property)
            {
                String retVal = property;

                //if we have a vocab and it's not a URI, resolve it
                if (finalDefaultVocab != null && !RdfTools.isUri(retVal)) {
                    retVal = finalDefaultVocab.resolveCurie(retVal).toString();
                }

                return retVal;
            }
        };

        // 1) parse the RDF properties:
        if (includeRdfProperties ) {
            // Note that our rules dictate: if there's at least one special property set, we ignore all others
            if (this.rdfPropertiesSpecial != null) {

                Iterable<RdfProperty> dynamicProps = null;
                switch (this.rdfPropertiesSpecial) {
                    case _vocab:
                        dynamicProps = finalDefaultVocab != null ? finalDefaultVocab.getAllProperties() : Collections.emptySet();
                        break;
                    case _class:
                        dynamicProps = defaultClass != null ? defaultClass.getProperties() : Collections.emptySet();
                        break;
                    default:
                        Logger.error("Encountered unimplemented special properties value; " + this.rdfPropertiesSpecial);
                        break;
                }

                if (dynamicProps != null) {
                    retVal = Iterables.transform(dynamicProps, new Function<RdfProperty, String>()
                    {
                        @Nullable
                        @Override
                        public String apply(@Nullable RdfProperty property)
                        {
                            return property.getCurieName().toString();
                        }
                    });
                }
            }
            else {
                retVal = Iterables.transform(this.rdfProperties, prefixProperty);
            }
        }

        // 2) parse the non-RDF properties and add them if enabled
        if (includeNonRdfProperties) {
            //note that we also prefix the non-rdf properties, for uniformity
            retVal = Iterables.concat(retVal, Iterables.transform(this.nonRdfProperties, prefixProperty));
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----
    protected abstract void saveHtml(OutputDocument document, HtmlTemplate superTemplate);

    protected abstract OutputDocument doInitHtmlPreparsing(OutputDocument document, HtmlTemplate superTemplate) throws IOException;

    //we want these to only be used by the TemplateCache
    protected boolean isDisabled()
    {
        return disabled;
    }
    protected void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }
    protected Class<TemplateController> getControllerClass()
    {
        return controllerClass;
    }
    protected HtmlTemplate getSuperTemplate()
    {
        return superTemplate;
    }

    //-----PRIVATE METHODS-----
    private static boolean isTagTemplate(Source source)
    {
        return !source.getAllElements(HtmlParser.WEBCOMPONENTS_TEMPLATE_ELEM).isEmpty();
    }
    private static boolean isPageTemplate(Source source)
    {
        boolean retVal = false;

        List<Element> html = source.getAllElements(HtmlParser.HTML_ROOT_ELEM);
        if (html != null && html.size() == 1) {
            Attributes htmlAttr = html.get(0).getAttributes();
            if (htmlAttr.get(HtmlParser.HTML_ROOT_TEMPLATE_ATTR) != null) {
                retVal = true;
            }
        }

        return retVal;
    }
    private static String callTemplateControllerMethods(com.beligum.base.resources.ifaces.Source source, ControllerCallback callback) throws IOException
    {
        String retVal;

        try (InputStream is = source.newInputStream()) {
            Source htmlSource = HtmlTemplate.readHtmlInputStream(is);
            OutputDocument htmlOutput = new OutputDocument(htmlSource);

            Iterator<Segment> nodes = htmlSource.getNodeIterator();
            while (nodes.hasNext()) {
                Segment node = nodes.next();

                //if the segment is a tag, parse the element
                if (node instanceof StartTag) {
                    Element element = ((StartTag) node).getElement();
                    HtmlTemplate templateInstance = TemplateCache.instance().getByTagName(element.getName());
                    if (templateInstance != null) {
                        Class<TemplateController> controllerClass = templateInstance.getControllerClass();
                        if (controllerClass != null) {
                            try {
                                callback.encounteredTemplateTagController(htmlSource, htmlOutput, element, controllerClass.newInstance());
                            }
                            catch (Exception e) {
                                throw new IOException("Error while creating new template controller instance; this shouldn't happen; " + controllerClass, e);
                            }
                        }
                    }
                }
            }

            retVal = htmlOutput.toString();
        }

        return retVal;
    }
    private String getMetaValue(OutputDocument output, RdfProperty property, boolean eatItUp)
    {
        String retVal = null;

        List<Element> metas = output.getSegment().getAllElements("meta");
        Iterator<Element> iter = metas.iterator();
        while (retVal == null && iter.hasNext()) {
            Element element = iter.next();
            String propertyVal = element.getAttributeValue(RDF_PROPERTY_ATTR);
            if (propertyVal != null && propertyVal.equalsIgnoreCase(property.getCurieName().toString())) {
                retVal = element.getAttributeValue(RDF_CONTENT_ATTR);

                if (eatItUp) {
                    output.remove(element);
                }
            }
        }

        return retVal;
    }
    private Iterable<ScopedResource> getInlineScriptElements()
    {
        return inlineScriptElements;
    }
    private Iterable<ScopedResource> getExternalScriptElements()
    {
        return externalScriptElements;
    }
    private Iterable<ScopedResource> getInlineStyleElements()
    {
        return inlineStyleElements;
    }
    private Iterable<ScopedResource> getExternalStyleElements()
    {
        return externalStyleElements;
    }
    private Iterable<ResourceReference> getResourceReferences()
    {
        return resourceReferences;
    }
    private Iterable<ScopedResource> parseInlineStyles(OutputDocument html) throws IOException
    {
        List<ScopedResource> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements("style").iterator();
        while (iter.hasNext()) {
            Element element = iter.next();

            Element parsedElement = this.renderResourceElement(element);
            html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.inlineStyles, parsedElement, null));
            retVal.add(new ScopedResource(parsedElement));
        }

        return retVal;
    }
    private Iterable<ScopedResource> parseExternalStyles(OutputDocument html) throws IOException
    {
        List<ScopedResource> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements("rel", styleLinkRelAttrValue).iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (element.getName().equals("link")) {
                Element parsedElement = this.renderResourceElement(element);
                html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.externalStyles, parsedElement, "href"));
                retVal.add(new ScopedResource(parsedElement));
            }
        }

        return retVal;
    }
    private Iterable<ScopedResource> parseInlineScripts(OutputDocument html) throws IOException
    {
        List<ScopedResource> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements("script").iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (element.getAttributeValue("src") == null) {
                Element parsedElement = this.renderResourceElement(element);
                html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.inlineScripts, parsedElement, null));
                retVal.add(new ScopedResource(parsedElement));
            }
        }

        return retVal;
    }
    private Iterable<ScopedResource> parseExternalScripts(OutputDocument html) throws IOException
    {
        List<ScopedResource> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements("script").iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (element.getAttributeValue("src") != null) {
                Element parsedElement = this.renderResourceElement(element);
                html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.externalScripts, parsedElement, "src"));
                retVal.add(new ScopedResource(parsedElement));
            }
        }

        return retVal;
    }
    // TODO working on this for future use, needs tender loving care
    private Iterable<ResourceReference> parseResourceReferences(OutputDocument html) throws IOException
    {
        List<ResourceReference> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements(BlocksReferenceController.TAG_NAME).iterator();
        while (iter.hasNext()) {
            Element element = iter.next();

            String typeStr = element.getAttributeValue(BlocksReferenceController.TYPE_ATTR);
            if (!StringUtils.isEmpty(typeStr)) {
                BlocksReferenceController.Type type = BlocksReferenceController.Type.valueOfAttr(typeStr);
                String id = element.getAttributeValue(BlocksReferenceController.ID_ATTR);
                BlocksReferenceController.TemplateRenderFilter filter = BlocksReferenceController.TemplateRenderFilter.NONE;
                String filterStr = element.getAttributeValue(BlocksReferenceController.RENDER_FILTER_ATTR);
                if (!StringUtils.isEmpty(filterStr)) {
                    filter = BlocksReferenceController.TemplateRenderFilter.valueOfAttr(filterStr);
                }

                ResourceReference reference = new ResourceReference(element, type, id, filter);
                //html.replace(element, reference.getVelocityTag());
                retVal.add(reference);
            }
            else {
                throw new IOException("Encountered a blocks reference tag without a type. Please supply at least a " + BlocksReferenceController.TYPE_ATTR + " attribute.");
            }
        }

        return retVal;
    }
    private CharSequence buildStartTag()
    {
        StringBuilder retVal = new StringBuilder();

        retVal.append("<" + this.getTemplateName());
        if (!this.attributes.isEmpty()) {
            retVal.append(Attributes.generateHTML(this.attributes));
        }
        retVal.append(">");

        return retVal;
    }
    private CharSequence buildEndTag()
    {
        return new StringBuilder().append("</").append(this.getTemplateName()).append(">").toString();
    }
    /**
     * Build a #btrd() directive for the supplied element's attribute
     */
    private StringBuilder buildResourceHtml(TemplateResourcesDirective.Argument type, Element element, String attr) throws IOException
    {
        final boolean print = false;
        String attrValue = attr == null ? null : element.getAttributeValue(attr);
        String elementStr = element.toString();
        Resource resource = null;

        //activate this if the resource needs to be fingerprinted dynamically;
        //eg. during (each) rendering of the template
        boolean enableDynamicFingerprinting = false;

        if (R.configuration().getResourceConfig().getEnableFingerprintedResources()) {
            //this means we're dealing with an external resource
            if (attrValue != null) {
                if ((type.isScript() && R.configuration().getResourceConfig().getEnableFingerprintedExternalScripts()) ||
                    (type.isStyle() && R.configuration().getResourceConfig().getEnableFingerprintedExternalStyles())) {
                    //validate the URI
                    resource = R.resourceManager().get(UriBuilder.fromUri(attrValue).build());

                    //this means the resource exists in our local system
                    if (resource != null) {

                        //if the resource is immutable (won't change anymore), we might as well calculate it's fingerprint now
                        if (resource.isImmutable()) {
                            //first, replace the attribute value
                            String fingerprintedUri = resource.getFingerprintedUri().toString();

                            attrValue = fingerprintedUri;

                            //also replace the attribute in the element itself
                            Segment attrValueSeg = element.getAttributes().get(attr).getValueSegment();
                            OutputDocument outputDocument = new OutputDocument(element);
                            outputDocument.replace(attrValueSeg, fingerprintedUri);
                            elementStr = outputDocument.toString();
                        }
                        else {
                            enableDynamicFingerprinting = true;
                        }
                    }
                }
            }
            //this means we're dealing with an inline resource; iterate all uri's to see if we can fingerprint them
            else {
                if ((type.isScript() && R.configuration().getResourceConfig().getEnableFingerprintedInlineScripts()) ||
                    (type.isStyle() && R.configuration().getResourceConfig().getEnableFingerprintedInlineStyles())) {
                    //Tried to wrap all URIs in a #brud directive, but that didn't really work,
                    //so switched to "block-mode" where we'll parsed all URIs in this inline resource and keep
                    //some statistics to see if we can alter the resource code right now, or need to defer to dynamic fingerprinting
                    InlineUriDetector inlineDetector = new InlineUriDetector();
                    String fingerprintedResource = R.resourceManager().getFingerprinter().detectAllResourceUris(elementStr, inlineDetector);

                    //no need to do anything if no URIs were detected
                    if (inlineDetector.hasUri) {
                        //if all detected URIs were immutable, we can safely use the fingerprinted code
                        // instead of the original and disable dynamic fingerprinting
                        if (inlineDetector.allImmutable) {
                            elementStr = fingerprintedResource;
                        }
                        //here, we have at least one non-immutable uri in the inline code,
                        //so we can't replace the code, but need to activate dynamic fingerprinting instead
                        else {
                            //since we only fingerprint local URIs, we don't need to activate dynamic fingerprinting
                            //if no local URIs were detected
                            if (inlineDetector.hasInternal) {
                                enableDynamicFingerprinting = true;
                            }
                        }
                    }
                }
            }
        }

        Attribute permissionScopeAttr = element.getAttributes().get(ATTRIBUTE_RESOURCE_PERMISSION_SCOPE);
        String permissionScopeStr = permissionScopeAttr == null ? "" : permissionScopeAttr.getValue();
        Attribute actionScopeAttr = element.getAttributes().get(ATTRIBUTE_RESOURCE_ACTION_SCOPE);
        String actionScopeStr = actionScopeAttr == null ? "" : actionScopeAttr.getValue();

        //Note: we don't append a newline: it clouds the output html with too much extra whitespace...
        return new StringBuilder().append("#").append(TagTemplateResourceDirective.NAME)

                                  .append("(")
                                  .append(type.ordinal()).append(",")
                                  .append(print).append(",")
                                  //Note: this value will be used to calculate the hashes of the asset packs,
                                  //so make sure you pass the fingerprinted uri here
                                  .append("'").append(attrValue).append("',")
                                  .append(enableDynamicFingerprinting).append(",")
                                  .append("'").append(permissionScopeStr).append("',")
                                  .append("'").append(actionScopeStr).append("',")
                                  .append(HtmlTemplate.getResourceJoinHint(element).ordinal())
                                  .append(")")

                                  .append(elementStr)

                                  .append("#end");
    }
    /**
     * This serializes all constants in the template.
     * Note that this is executed during boot, so eg. the language for the messages will probably not be initialized correctly,
     * so avoid using $MESSAGES in resource element names. This is mainly made for using $CONSTANTS in the resource URLs.
     */
    private Element renderResourceElement(Element element) throws IOException
    {
        //the idea (in the new method) is to create a dummy template with the provided html to be able to access
        //the template context (and it's evaluate() method), but not render it out (because that would cause infinite recursion),
        //but feed it's own html to the evaluate method, so it's serialized in the right context.
        String html = element.toString();

        //note that if you debug this variable during boot, the toString() method will throw an exception because it tries
        //to render out the template and a recursive init TemplateCache exception occurs (that doesn't occur during normal operation)
        Template template = R.resourceManager().newTemplate(new StringSource(html, MimeTypes.HTML, R.i18n().getOptimalLocale()));

        String parsedHtml = template.getContext().evaluate(html, true, false);

        //there should always be a first element since we started out with an element, right?
        return new Source(parsedHtml).getFirstElement();

        // This is the old way that parses the html but we can't use it anymore because it causes a recursive call to the TemplateCache during boot.
        //        // Warning: tag templates are stored/searched in the cache by their relative path (eg. see TemplateCache.putByRelativePath()),
        //        // so make sure you don't use this URI to instance template or you'll end up overwriting the cache with this temporary StringSource
        //        Template template = R.resourceManager().newTemplate(new StringSource(element.toString(), MimeTypes.HTML, R.i18n().getOptimalLocale()));
        //
        //        //there should always be a first element since we started out with an element, right?
        //        return new Source(template.render()).getFirstElement();
    }
    private Iterable<ScopedResource> addSuperTemplateResources(TemplateResourcesDirective.Argument type, StringBuilder html,
                                                               Iterable<ScopedResource> templateResources, Iterable<ScopedResource> superTemplateResources,
                                                               String attribute) throws IOException
    {
        Iterable<ScopedResource> retVal = templateResources;

        if (superTemplateResources != null && superTemplateResources.iterator().hasNext()) {
            for (ScopedResource r : superTemplateResources) {
                html.append(buildResourceHtml(type, r.getElement(), attribute));
            }
            retVal = Iterables.concat(superTemplateResources, templateResources);
        }

        return retVal;
    }
    private Iterable<String> buildScopedResourceIterator(Iterable<ScopedResource> resources)
    {
        return Iterables.transform(Iterables.filter(resources, new Predicate<ScopedResource>()
                                   {
                                       @Override
                                       public boolean apply(ScopedResource resource)
                                       {
                                           return testResourceActionScope(resource.getActions()) && testResourcePermissionScope(resource.getPermissions());
                                       }
                                   }),
                                   new Function<ScopedResource, String>()
                                   {
                                       @Nullable
                                       @Override
                                       public String apply(@Nullable ScopedResource input)
                                       {
                                           return input.getElement().toString();
                                       }
                                   }
        );
    }
    /**
     * Post-parse the html of this template to save variable selectors, standardize html, etc.
     */
    private void parseHtml() throws Exception
    {
        //Note that JSoup wraps all html in a <html><head></head><body></body>[inserted]</html> container...
        Document templateHtml = Jsoup.parseBodyFragment(this.createNewHtmlInstance(true).toString());

        //BIG NOTE: since these substitutions will be executed in the same order we enter them,
        //          the variable substitutions must be executed before the equality substitutions
        //          or we'll compare rendered html with unrendered html and nothing will match!
        //          ---> That's why we need multiple phases.

        //We currently support two places where we'll normalize the variables back into the html:
        //- as the value of an attribute, eg. <blah class="$CONSTANTS.blocks.core.CLASS_NAME">
        //- as the full value of an element, eg. <blah>$CONSTANTS.blocks.core.CONTENT</blah>
        //On top of that, we support the normalization of unchanged property elements
        this.normalizationSubstitutions = new ArrayList<>();
        List<SubstitionReference> phase2 = new ArrayList<>();
        List<String> explicitRdfProperties = new ArrayList<>();
        List<String> explicitNonRdfProperties = new ArrayList<>();
        Elements templateIter = templateHtml.body().children().select("*");
        for (org.jsoup.nodes.Element e : templateIter) {

            //(1) check if the attribute value is a variable
            org.jsoup.nodes.Attributes attributes = e.attributes();
            for (org.jsoup.nodes.Attribute a : attributes) {
                if (this.isTemplateVariable(a.getValue().trim())) {
                    this.normalizationSubstitutions.add(new ReplaceVariableAttributeValue(this.cssSelector(e), attributes, a));
                }
            }

            //(2) check if the content is a single variable
            if (this.isTemplateVariable(e.html().trim())) {
                this.normalizationSubstitutions.add(new ReplaceVariableContent(this.cssSelector(e), e));
            }

            // Some notes for the two checks below:
            // Note that we instance a template of our own name (see createNewHtmlInstance() in the beginning of this method),
            // but inside the html of that template can also be instances of other templates (eg. <blocks-text>).
            // Since this method call is mainly meant to deal with the normalization of _this_ template (eg. not the <blocks-text> one),
            // we must distinguish between 'our context' and 'other contexts'. For instance, supposed we define a default page template
            // in which we have some <blocks-text> instances, but we have added a class to those instances (<blocks-text class="blah">),
            // that class can't be normalized away, because we don't know if the <blocks-text> definition template has this class set too.
            // And since only those attributes are added (the ones present in the definition file of <blocks-text>) during rendering of
            // that <blocks-text> block, they must be left alone.

            //(3) check if the element is a property
            if (isPropertyTag(e)) {
                //Since we iterate _all_ properties, we'll also iterate properties
                //of other template instances (eg. when a template uses other templates).
                //A property 'belongs' to it's parent template instance,
                //we shouldn't collapse the properties of others
                org.jsoup.nodes.Element parentTemplateInstance = e.parent();
                while (parentTemplateInstance != null && !parentTemplateInstance.tagName().contains("-")) {
                    parentTemplateInstance = parentTemplateInstance.parent();
                }

                if (parentTemplateInstance == null) {
                    throw new IOException(
                                    "Found a property tag (" + e + ") in a template file (" + this.getRelativePath() + ") that's not in the context of any template-instance; this shouldn't happen");
                }

                //only add references to properties that belong to us
                if (parentTemplateInstance.tagName().equals(this.getTemplateName())) {

                    TemplateProperty prop = new TemplateProperty(this.cssSelector(e), e);
                    phase2.add(prop);

                    //store the explicit properties (the ones that are present in the template definition) to a temp list, see below why
                    //make sure constants are serialized and the value is trimmed
                    String propName = R.resourceManager().getTemplateEngine().serializePropertyKey(getPropertyAttribute(e)).trim();
                    if (isRdfPropertyTag(e)) {
                        explicitRdfProperties.add(propName);
                    }
                    else {
                        explicitNonRdfProperties.add(propName);
                    }
                }
            }

            //(4) check if the element is a template
            //Note that we can't use isTemplateInstanceTag() here because this is called
            //*during* template cache building, so we'll add a semi-check and postpone
            //to the parsing in the substitution
            //Updated to only accepting our own (root) template instance tag, because
            //if we would normalize an instance tag in another instance tag, the removed attributes
            //wouldn't be added again during rendering because the attribtues that are added are controlled
            //by the definition html file of _that_ template tag, not the ones we might have added
            //to this sub-instance-tag
            //Also note that we could remove the dash-check, but we kept it to make the remake above valid
            //(because it's also relevant for the property-checking above)
            if (e.tagName().contains("-") && e.tagName().equals(this.getTemplateName())) {
                phase2.add(new TemplateInstance(this.cssSelector(e), e));
            }
        }
        //now add all phase 2 substitutions at the end of the existing list so we're sure they're executed after phase 1
        this.normalizationSubstitutions.addAll(phase2);

        //First of all, please note that both collections below are already (possibly) initialized during parsing of the meta tag.
        //This is subtle: we allow the developer to explicitly define the supported properties
        //in the meta tags (to allow for javascript-generated properties, see blocks-fact-entry),
        //but if such meta information doesn't exist, it's pulled from the properties in the html itself.
        //Note that there's a difference between a null collection or an empty one:
        // - a null collection means: no metadata was found
        // - an empty collection means: the metadata explicitly set it to empty
        // --> so we only override on null.
        if (this.rdfProperties == null) {
            this.rdfProperties = explicitRdfProperties;
        }
        if (this.nonRdfProperties == null) {
            this.nonRdfProperties = explicitNonRdfProperties;
        }

        /*
        // This is a bit stupid to iterate the DOM another time, but the written Jericho code to detect RDF properties
        // doens't match the JSoup API above, so I've chosen the quick way out and re-used existing code to re-iterate
        // TODO: not done yet, needs more work and more thinking through...
        this.rdfPropertyRefs = new ArrayList<>();
        Element renderedRootElement = this.getRootElement();

        //this will hold the RDF context while we parse our html elements
        HtmlRdfContext rdfContext = new HtmlRdfContext(HtmlRdfContext.getDefaultRdfVocab());

        //initialize the context with the root element attributes
        rdfContext.updateContext(renderedRootElement.getStartTag());

        for (Iterator<Segment> nodeIterator = renderedRootElement.getNodeIterator(); nodeIterator.hasNext(); ) {
            Segment nodeSegment = nodeIterator.next();
            if (nodeSegment instanceof StartTag) {
                StartTag startTag = (StartTag) nodeSegment;

                Attributes attributes = startTag.getAttributes();
                if (attributes != null) {

                    rdfContext.updateContext(startTag);

                    Attribute propertyAttr = attributes.get(RDF_PROPERTY_ATTR);
                    if (propertyAttr != null) {
                        String rawPropValue = propertyAttr.getValue().trim();
                        String renderedPropValue = R.resourceManager().newTemplate(new StringSource(rawPropValue, MimeTypes.HTML, R.i18n().getOptimalLocale())).render();
                        String normalizedPropValue = rdfContext.normalizeProperty(startTag, renderedPropValue);

                        this.rdfPropertyRefs.add(new HtmlRdfPropertyRef(startTag, normalizedPropValue));
                    }
                }
            }
            else if (nodeSegment instanceof EndTag) {
                EndTag endTag = (EndTag) nodeSegment;
                rdfContext.updateContext(endTag);
            }
        }
        */
    }
    /**
     * This is the exact same code as org.jsoup.nodes.Element.cssSelector() but with the id and classes commented out
     * because in our case, the id or classes are often constants and the special chars cause trouble. From what I tested, and because
     * of the way we double check by testing the rendered out values during comparison, I don't think this will
     * cause problems.
     * Note that we're still backwards compatible with the Element.select() code though...
     */
    private String cssSelector(org.jsoup.nodes.Element element)
    {
        //        if (element.id().length() > 0) {
        //            return "#" + element.id();
        //        }

        StringBuilder selector = new StringBuilder(element.tagName());
        //        String classes = StringUtil.join(element.classNames(), ".");
        //        if (classes.length() > 0)
        //            selector.append('.').append(classes);

        if (element.parent() == null || element.parent() instanceof Document) // don't add Document to selector, as will always have a html node
        {
            return selector.toString();
        }

        selector.insert(0, " > ");
        if (element.parent().select(selector.toString()).size() > 1) {
            selector.append(String.format(":nth-child(%d)", element.elementSiblingIndex() + 1));
        }

        return this.cssSelector(element.parent()) + selector.toString();
    }
    /**
     * Check if the supplied string is a template variable
     */
    private boolean isTemplateVariable(String html)
    {
        //note that we chose to not search for specific variables (eg; MESSAGES or CONSTANTS) because
        // of the possibility of different prefix syntaxes (eg. for Velocity ${MESSAGES} and $MESSAGES)
        // and because we'll have a safeguard check (comparing the rendered variable)
        return html.startsWith(R.resourceManager().getTemplateEngine().getVariablePrefix());
    }

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "<" + templateName + ">";
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HtmlTemplate)) {
            return false;
        }

        HtmlTemplate that = (HtmlTemplate) o;

        return !(templateName != null ? !templateName.equals(that.templateName) : that.templateName != null);

    }
    @Override
    public int hashCode()
    {
        return templateName != null ? templateName.hashCode() : 0;
    }

    //-----INNER CLASSES-----
    private interface ControllerCallback
    {
        void encounteredTemplateTagController(Source htmlSource, OutputDocument htmlOutput, Element element, TemplateController controller) throws IOException;
    }

    public interface SubstitionReferenceRenderer
    {
        String renderTemplateString(String value) throws IOException;
    }

    public abstract static class SubstitionReference
    {
        protected String cssSelector;
        protected SubstitionReference(String cssSelector)
        {
            this.cssSelector = cssSelector;
        }

        public abstract boolean replaceIn(Document document, SubstitionReferenceRenderer renderer) throws IOException;

        //bring the supplied argument in a state where it's can be compared for equality regardless of white space, formatting, etc...
        protected String standardize(String html)
        {
            final boolean USE_JERICHO = false;

            String retVal;
            if (USE_JERICHO) {
                retVal = new SourceFormatter(new Source(html)).setCollapseWhiteSpace(true).setIndentString("").setNewLine("").toString();
            }
            else {
                Document doc = Jsoup.parseBodyFragment(html);
                //we'll standardize everything to a compact xhtml document
                doc = doc.outputSettings(doc.outputSettings().indentAmount(0).prettyPrint(false).syntax(Document.OutputSettings.Syntax.xml));
                retVal = doc.body().html();
            }

            return retVal;
        }
    }

    public static class ReplaceVariableAttributeValue extends SubstitionReference
    {
        protected String attributeName;
        protected String variableAttributeValue;
        protected ReplaceVariableAttributeValue(String cssSelector, org.jsoup.nodes.Attributes attributes, org.jsoup.nodes.Attribute attribute)
        {
            super(cssSelector);

            this.attributeName = attribute.getKey();
            this.variableAttributeValue = this.standardize(attribute);
        }

        @Override
        public boolean replaceIn(Document document, SubstitionReferenceRenderer renderer) throws IOException
        {
            boolean retVal = false;

            Elements selectedEls = document.select(this.cssSelector);
            if (!selectedEls.isEmpty()) {
                //since we're checking if we need to normalize to a variable again, we need to render out or own (variable) value first
                String thisValue = this.standardize(renderer.renderTemplateString(this.variableAttributeValue));
                for (org.jsoup.nodes.Element e : selectedEls) {
                    String thatValue = this.standardize(e.attr(this.attributeName));
                    if (thisValue.equals(thatValue)) {
                        e.attr(this.attributeName, this.variableAttributeValue);
                        retVal = true;
                    }
                }
            }

            return retVal;
        }
        protected String standardize(org.jsoup.nodes.Attribute attribute)
        {
            return super.standardize(attribute.getValue().trim());
        }
    }

    public static class ReplaceVariableContent extends SubstitionReference
    {
        protected String variableElementContent;
        protected ReplaceVariableContent(String cssSelector, org.jsoup.nodes.Element element)
        {
            super(cssSelector);

            this.variableElementContent = this.standardize(element);
        }
        @Override
        public boolean replaceIn(Document document, SubstitionReferenceRenderer renderer) throws IOException
        {
            boolean retVal = false;

            Elements selectedEls = document.select(this.cssSelector);
            if (!selectedEls.isEmpty()) {
                //since we're checking if we need to normalize to a variable again, we need to render out or own (variable) value first
                String thisValue = this.standardize(renderer.renderTemplateString(this.variableElementContent));
                for (org.jsoup.nodes.Element e : selectedEls) {
                    String thatValue = this.standardize(e);
                    if (thisValue.equals(thatValue)) {
                        e.html(this.variableElementContent);
                        retVal = true;
                    }
                }
            }

            return retVal;
        }
        protected String standardize(org.jsoup.nodes.Element element)
        {
            return super.standardize(element.html().trim());
        }
    }

    public static class TemplateProperty extends SubstitionReference
    {
        protected String element;

        protected TemplateProperty(String cssSelector, org.jsoup.nodes.Element propertyElement)
        {
            super(cssSelector);

            //we can optimize a bit and standardize here because we're not supposed to have variables
            this.element = this.standardize(propertyElement);
        }
        @Override
        public boolean replaceIn(Document document, SubstitionReferenceRenderer renderer) throws IOException
        {
            boolean retVal = false;

            Elements selectedEls = document.select(this.cssSelector);
            if (!selectedEls.isEmpty()) {
                String thisValue = this.element;
                for (org.jsoup.nodes.Element e : selectedEls) {
                    String thatValue = this.standardize(e);
                    if (thisValue.equals(thatValue)) {
                        //we can just remove the property element because the default behavior of the HtmlParser is to render out it's default value
                        e.remove();
                        retVal = true;
                    }
                }
            }

            return retVal;
        }
        protected String standardize(org.jsoup.nodes.Element element)
        {
            return super.standardize(element.outerHtml().trim());
        }
    }

    public static class TemplateInstance extends SubstitionReference
    {
        protected Map<String, String> attributes;
        protected TemplateInstance(String cssSelector, org.jsoup.nodes.Element element)
        {
            super(cssSelector);

            this.attributes = new HashMap<>();
            org.jsoup.nodes.Attributes attrs = element.attributes();
            for (org.jsoup.nodes.Attribute a : attrs) {
                //note: we shouldn't wipe properties, right?
                if (!HtmlTemplate.isPropertyAttribute(a.getKey())) {
                    this.attributes.put(a.getKey(), a.getValue());
                }
            }
        }
        @Override
        public boolean replaceIn(Document document, SubstitionReferenceRenderer renderer) throws IOException
        {
            boolean retVal = false;

            //little optimization
            if (!this.attributes.isEmpty()) {
                Elements selectedEls = document.select(this.cssSelector);
                //note that we need the extra isTemplateInstanceTag() (see instance creation of this class for why)
                if (!selectedEls.isEmpty() && HtmlTemplate.isTemplateInstanceTag(selectedEls.first())) {
                    //remove all attributes that are the same as the source tag
                    for (Map.Entry<String, String> a : this.attributes.entrySet()) {
                        if (selectedEls.attr(a.getKey()).equals(a.getValue())) {
                            selectedEls.removeAttr(a.getKey());
                            retVal = true;
                        }
                    }
                }
            }

            return retVal;
        }
    }

    private class InlineUriDetector implements UriDetector.ReplaceCallback
    {
        /**
         * Will be true if we detected at least one URI
         */
        private boolean hasUri;

        /**
         * Will be true if and only if all detected URIs were immutable
         */
        private boolean allImmutable;

        /**
         * Will be true if at least one of the detected URIs were non-local
         */
        private boolean hasExternal;

        /**
         * Will be true if at least one of the detected URIs were local
         */
        private boolean hasInternal;

        public InlineUriDetector()
        {
            this.hasUri = false;
            this.allImmutable = false;
            this.hasExternal = false;
            this.hasInternal = false;
        }

        @Override
        public String uriDetected(String uriStr)
        {
            //never return null; if nothing was found, the uri was probably an external one
            String retVal = uriStr;

            //we keep allImmutable false until we encounter the first URI,
            if (!this.hasUri) {
                this.allImmutable = true;
            }
            this.hasUri = true;

            //if the endpoint is immutable, we'll generate our fingerprint right now,
            //if not, we'll wrap the URI in a directive to re-parse it on every request.
            //Note: this means we won't do any other post-processing next to fingerprinting in that directive anymore,
            //if that would change, we must wipe this optimization step
            Resource resource = R.resourceManager().get(URI.create(uriStr));

            //this means we're dealing with a local uri; we only support fingerprinting of local resources
            if (resource != null) {
                this.hasInternal = true;
                if (resource.isImmutable()) {
                    retVal = resource.getFingerprintedUri().toString();
                }
                else {
                    this.allImmutable = false;
                }
            }
            else {
                this.hasExternal = true;
                //note that external resources are never immutable
                this.allImmutable = false;
            }

            return retVal;
        }
    }

    public static class ScopedResource
    {
        private static Splitter csvSplitter = Splitter.on(",").trimResults().omitEmptyStrings();
        private static Function<String, ResourceAction> actionParser = new Function<String, ResourceAction>()
        {
            @Nullable
            @Override
            public ResourceAction apply(@Nullable String input)
            {
                return ResourceAction.valueOfIgnoreCase(input);
            }
        };

        private Element element;
        private Iterable<String> permissions;
        private Iterable<ResourceAction> actions;

        public ScopedResource(Element element)
        {
            this.element = element;

            Attribute permScopeAttr = this.element.getAttributes().get(ATTRIBUTE_RESOURCE_PERMISSION_SCOPE);
            if (permScopeAttr != null && permScopeAttr.hasValue()) {
                this.permissions = parsePermissions(permScopeAttr.getValue());
            }
            else {
                this.permissions = Collections.emptyList();
            }

            Attribute actionScopeAttr = this.element.getAttributes().get(ATTRIBUTE_RESOURCE_ACTION_SCOPE);
            if (actionScopeAttr != null && actionScopeAttr.hasValue()) {
                this.actions = parseActions(actionScopeAttr.getValue());
            }
            else {
                this.actions = Collections.emptyList();
            }
        }
        public static Iterable<String> parsePermissions(String value)
        {
            //note that permissions are just strings
            return csvSplitter.split(value);
        }
        public static Iterable<ResourceAction> parseActions(String value)
        {
            return Iterables.transform(csvSplitter.split(value), actionParser);
        }
        public Element getElement()
        {
            return element;
        }
        public Iterable<String> getPermissions()
        {
            return permissions;
        }
        public Iterable<ResourceAction> getActions()
        {
            return actions;
        }
    }

    public static class ResourceReference
    {
        private Element element;
        private BlocksReferenceController.Type type;
        private String id;
        private BlocksReferenceController.TemplateRenderFilter filter;

        public ResourceReference(Element element, BlocksReferenceController.Type type, String id, BlocksReferenceController.TemplateRenderFilter filter)
        {
            this.element = element;
            this.type = type;
            this.id = id;
            this.filter = filter;
        }
    }
}
