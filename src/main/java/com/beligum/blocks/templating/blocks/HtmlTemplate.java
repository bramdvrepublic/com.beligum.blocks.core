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

import com.beligum.base.config.SecurityConfiguration;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.security.PermissionRole;
import com.beligum.base.security.PermissionsConfigurator;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.utils.UriDetector;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.templating.blocks.directives.TagTemplateResourceDirective;
import com.beligum.blocks.templating.blocks.directives.TemplateResourcesDirective;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import gen.com.beligum.blocks.core.messages.blocks.core;
import net.htmlparser.jericho.*;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
    //this is the prefix to use in the <meta property="prefix:your-name" value="value comes here" > so that it doesn't get sent to the client
    public static final String BLOCKS_META_TAG_PROPERTY_PREFIX = "blocks:";

    // controls if the <script> or <style> tag needs to be included in the rendering
    // use it like this: <script data-scope-role="admin"> to eg. only include the script when and ADMIN-role is logged in
    // Role names are the same ones we use for Shiro
    public static final String ATTRIBUTE_RESOURCE_ROLE_SCOPE = "data-scope-role";

    //set to 'edit' if you only want the resource to be included if the $BLOCKS_MODE variable is set
    public static final String ATTRIBUTE_RESOURCE_MODE_SCOPE = "data-scope-mode";

    //set to 'skip' to skip this resource during the resource collecting phase and instead render it out where it's defined
    public static final String ATTRIBUTE_RESOURCE_JOIN_HINT = "data-join-hint";

    // set this attribute on an instance to not render the tag itself, but mimic another tag name
    // (eg <blocks-text data-render-tag="div"> to render out a <div> instead of a <blocks-text>)
    // Beware: this will make the tag's direction server-to-client only!!
    //NOTE: if this is set to empty, don't render the tag (and it's attributes) at all
    public static final String ATTRIBUTE_RENDER_TAG = "data-render-tag";

    public enum ResourceScopeMode
    {
        UNDEFINED,
        EMPTY,
        create,
        edit
    }

    public enum ResourceJoinHint
    {
        UNDEFINED,
        EMPTY,
        skip
    }

    public enum MetaProperty
    {
        title,
        description,
        icon,
        controller,
        display
    }

    public enum MetaDisplayType
    {
        DEFAULT,
        HIDDEN
    }

    protected static final Pattern styleLinkRelAttrValue = Pattern.compile("stylesheet");

    //-----VARIABLES-----
    protected Map<String, String> attributes;
    protected Path absolutePath;
    protected Path relativePath;
    protected String templateName;
    protected String title;
    protected String description;
    protected String icon;
    protected Class<TemplateController> controllerClass;
    protected Iterable<Element> inlineScriptElements;
    protected Iterable<Element> externalScriptElements;
    protected Iterable<Element> inlineStyleElements;
    protected Iterable<Element> externalStyleElements;
    protected MetaDisplayType displayType;
    protected Element rootElement;
    protected List<SubstitionReference> normalizationSubstitutions;

    //this will enable us to save the 'inheritance tree'
    protected HtmlTemplate superTemplate;

    // This will hold the html before the <template> tags
    protected Segment prefixHtml;

    // This will hold the html inside the <template> tags
    protected Segment innerHtml;

    // This will hold the html after the <template> tags
    protected Segment suffixHtml;

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
    protected HtmlTemplate(String templateName, Source source, Path absolutePath, Path relativePath, HtmlTemplate superTemplate) throws Exception
    {
        //INIT THE PATHS
        this.absolutePath = absolutePath;
        this.relativePath = relativePath;

        //INIT THE NAMES
        this.templateName = templateName;
        this.superTemplate = superTemplate;

        //INIT THE HTML
        //note: this should take the parent into account
        OutputDocument tempHtml = this.doInitHtmlPreparsing(new OutputDocument(source), superTemplate);

        //Note that we need to eat these values for PageTemplates because we don't want them to end up at the client side (no problem for TagTemplates)
        this.title = superTemplate != null ? superTemplate.getTitle() : null;
        String thisTitle = this.getMetaValue(tempHtml, MetaProperty.title, true);
        if (!StringUtils.isEmpty(thisTitle)) {
            this.title = thisTitle;
        }
        if (StringUtils.isEmpty(this.title)) {
            this.title = core.Entries.emptyTemplateTitle.toString();
        }

        this.description = superTemplate != null ? superTemplate.getDescription() : null;
        String thisDescription = this.getMetaValue(tempHtml, MetaProperty.description, true);
        if (!StringUtils.isEmpty(thisDescription)) {
            this.description = thisDescription;
        }
        if (StringUtils.isEmpty(this.title)) {
            this.description = core.Entries.emptyTemplateTitle.toString();
        }

        this.icon = superTemplate != null ? superTemplate.getIcon() : null;
        String thisIcon = this.getMetaValue(tempHtml, MetaProperty.icon, true);
        if (!StringUtils.isEmpty(thisIcon)) {
            this.icon = thisIcon;
        }
        if (StringUtils.isEmpty(this.title)) {
            this.icon = core.Entries.emptyTemplateTitle.toString();
        }

        String controllerClassStr = this.getMetaValue(tempHtml, MetaProperty.controller, true);
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
        String displayType = this.getMetaValue(tempHtml, MetaProperty.display, true);
        if (!StringUtils.isEmpty(displayType)) {
            this.displayType = MetaDisplayType.valueOf(displayType.toUpperCase());
        }

        this.inlineStyleElements = parseInlineStyles(tempHtml);
        this.externalStyleElements = parseExternalStyles(tempHtml);
        this.inlineScriptElements = parseInlineScripts(tempHtml);
        this.externalScriptElements = parseExternalScripts(tempHtml);

        //prepend the html with the parent resources if it's there
        if (superTemplate != null) {
            StringBuilder superTemplateResourceHtml = new StringBuilder();
            this.inlineStyleElements =
                            addSuperTemplateResources(TemplateResourcesDirective.Argument.inlineStyles, superTemplateResourceHtml, this.inlineStyleElements, superTemplate.getInlineStyleElements(),
                                                      null);
            this.externalStyleElements =
                            addSuperTemplateResources(TemplateResourcesDirective.Argument.externalStyles, superTemplateResourceHtml, this.externalStyleElements,
                                                      superTemplate.getExternalStyleElements(), "href");
            this.inlineScriptElements =
                            addSuperTemplateResources(TemplateResourcesDirective.Argument.inlineScripts, superTemplateResourceHtml, this.inlineScriptElements,
                                                      superTemplate.getInlineScriptElements(), null);
            this.externalScriptElements =
                            addSuperTemplateResources(TemplateResourcesDirective.Argument.externalScripts, superTemplateResourceHtml, this.externalScriptElements,
                                                      superTemplate.getExternalScriptElements(), "src");

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
     * Returns the permission role of the supplied element (guaranteed not-null)
     */
    public static PermissionRole getResourceRoleScope(Element resource)
    {
        PermissionRole retVal = null;

        Attribute scope = resource.getAttributes().get(ATTRIBUTE_RESOURCE_ROLE_SCOPE);
        if (scope != null && !StringUtils.isEmpty(scope.getValue())) {
            SecurityConfiguration securityConfig = R.configuration().getSecurityConfig();
            if (securityConfig != null) {
                retVal = securityConfig.lookupPermissionRole(scope.getValue());
            }
        }

        //we guarantee non-null
        if (retVal == null) {
            retVal = PermissionsConfigurator.ROLE_GUEST;
        }

        return retVal;
    }
    /**
     * Returns the scope mode of the supplied element (guaranteed non-null)
     */
    public static ResourceScopeMode getResourceModeScope(Element resource) throws IllegalArgumentException
    {
        ResourceScopeMode retVal = null;

        Attribute scope = resource.getAttributes().get(ATTRIBUTE_RESOURCE_MODE_SCOPE);
        if (scope == null) {
            retVal = ResourceScopeMode.UNDEFINED;
        }
        else if (StringUtils.isEmpty(scope.getValue())) {
            retVal = ResourceScopeMode.EMPTY;
        }
        else {
            //this will throw an exception if nothing was found
            retVal = ResourceScopeMode.valueOf(scope.getValue());
        }

        return retVal;
    }
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
     * Supply a scope attached to a html resource (with data-scope-role attribute)
     * and return if that resource should be included or not.
     */
    public static boolean testResourceRoleScope(PermissionRole role)
    {
        // guest role is not always added by default to the current principal by the security system,
        // so if the resource is annotated GUEST (or nothing at all), assume this is a public resource
        // so the check below would fail, while it's actually ok
        if (role == null || role == PermissionsConfigurator.ROLE_GUEST) {
            return true;
        }
        else {
            return SecurityUtils.getSubject().hasRole(role.getRoleName());
        }
    }
    /**
     * Return if we should currently (in this request context) render out for the specified mode.
     */
    public static boolean testResourceModeScope(ResourceScopeMode mode)
    {
        switch (mode) {
            // if you specifically set the mode flag, doIsValid it
            case edit:
                return mode.equals(R.cacheManager().getRequestCache().get(CacheKeys.BLOCKS_MODE));

            //in default mode (no specific mode set), we always display the resource since this is what you expect as a web developer
            case EMPTY:
            case UNDEFINED:
            default:
                return true;
        }
    }
    /**
     * Returns true if the supplied tag is a property tag
     */
    public static boolean isPropertyTag(StartTag startTag)
    {
        return startTag.getAttributeValue(HtmlParser.RDF_PROPERTY_ATTR) != null || startTag.getAttributeValue(HtmlParser.NON_RDF_PROPERTY_ATTR) != null;
    }
    /**
     * Same doIsValid as the method above, but with a JSoup element
     */
    public static boolean isPropertyTag(org.jsoup.nodes.Element element)
    {
        return element.hasAttr(HtmlParser.RDF_PROPERTY_ATTR) || element.hasAttr(HtmlParser.NON_RDF_PROPERTY_ATTR);
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
        return new Source(IOUtils.toString(inputStream, Charsets.UTF_8));
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
    public Class<TemplateController> getControllerClass()
    {
        return controllerClass;
    }
    public HtmlTemplate getSuperTemplate()
    {
        return superTemplate;
    }
    public Iterable<Element> getInlineScriptElements()
    {
        return inlineScriptElements;
    }
    public Iterable<Element> getExternalScriptElements()
    {
        return externalScriptElements;
    }
    public Iterable<Element> getInlineStyleElements()
    {
        return inlineStyleElements;
    }
    public Iterable<Element> getExternalStyleElements()
    {
        return externalStyleElements;
    }
    //TODO we should probably optimize this a bit, but beware, it still needs to be user-dynamic...
    public Iterable<Element> getInlineScriptElementsForCurrentScope()
    {
        return this.buildScopeResourceIterator(this.getInlineScriptElements());
    }
    public Iterable<Element> getExternalScriptElementsForCurrentScope()
    {
        return this.buildScopeResourceIterator(this.getExternalScriptElements());
    }
    public Iterable<Element> getInlineStyleElementsForCurrentScope()
    {
        return this.buildScopeResourceIterator(this.getInlineStyleElements());
    }
    public Iterable<Element> getExternalStyleElementsForCurrentScope()
    {
        return this.buildScopeResourceIterator(this.getExternalStyleElements());
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

    //-----PROTECTED METHODS-----
    protected abstract void saveHtml(OutputDocument document, HtmlTemplate superTemplate);

    protected abstract OutputDocument doInitHtmlPreparsing(OutputDocument document, HtmlTemplate superTemplate) throws IOException;

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
    private String getMetaValue(OutputDocument output, MetaProperty property, boolean eatItUp)
    {
        String retVal = null;

        List<Element> metas = output.getSegment().getAllElements("meta");
        Iterator<Element> iter = metas.iterator();
        while (retVal == null && iter.hasNext()) {
            Element element = iter.next();
            String propertyVal = element.getAttributeValue(RDF_PROPERTY_ATTR);
            if (propertyVal != null && propertyVal.equalsIgnoreCase(BLOCKS_META_TAG_PROPERTY_PREFIX + property.toString())) {
                retVal = element.getAttributeValue(RDF_CONTENT_ATTR);

                if (eatItUp) {
                    output.remove(element);
                }
            }
        }

        return retVal;
    }
    private Iterable<Element> parseInlineStyles(OutputDocument html) throws IOException
    {
        List<Element> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements("style").iterator();
        while (iter.hasNext()) {
            Element element = iter.next();

            Element parsedElement = this.renderResourceElement(element);
            html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.inlineStyles, parsedElement, null));
            retVal.add(parsedElement);
        }

        return retVal;
    }
    private Iterable<Element> parseExternalStyles(OutputDocument html) throws IOException
    {
        List<Element> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements("rel", styleLinkRelAttrValue).iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (element.getName().equals("link")) {
                Element parsedElement = this.renderResourceElement(element);
                html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.externalStyles, parsedElement, "href"));
                retVal.add(parsedElement);
            }
        }

        return retVal;
    }
    private Iterable<Element> parseInlineScripts(OutputDocument html) throws IOException
    {
        List<Element> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements("script").iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (element.getAttributeValue("src") == null) {
                Element parsedElement = this.renderResourceElement(element);
                html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.inlineScripts, parsedElement, null));
                retVal.add(parsedElement);
            }
        }

        return retVal;
    }
    private Iterable<Element> parseExternalScripts(OutputDocument html) throws IOException
    {
        List<Element> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements("script").iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (element.getAttributeValue("src") != null) {
                Element parsedElement = this.renderResourceElement(element);
                html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.externalScripts, parsedElement, "src"));
                retVal.add(parsedElement);
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
                if ((type.isScript() && R.configuration().getResourceConfig().getEnableFingerprintedExternalScripts()) || (type.isStyle() && R.configuration().getResourceConfig().getEnableFingerprintedExternalStyles())) {
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
                if ((type.isScript() && R.configuration().getResourceConfig().getEnableFingerprintedInlineScripts()) || (type.isStyle() && R.configuration().getResourceConfig().getEnableFingerprintedInlineStyles())) {
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

        //Note: we don't append a newline: it clouds the output html with too much extra whitespace...
        return new StringBuilder().append("#").append(TagTemplateResourceDirective.NAME)

                                  .append("(")
                                  .append(type.ordinal()).append(",")
                                  .append(print).append(",")
                                  //Note: this value will be used to calculate the hashes of the asset packs,
                                  //so make sure you pass the fingerprinted uri here
                                  .append("'").append(attrValue).append("',")
                                  .append(enableDynamicFingerprinting).append(",")
                                  .append("'").append(HtmlTemplate.getResourceRoleScope(element)).append("',")
                                  .append(HtmlTemplate.getResourceModeScope(element).ordinal()).append(",")
                                  .append(HtmlTemplate.getResourceJoinHint(element).ordinal())
                                  .append(")")

                                  .append(elementStr)

                                  .append("#end");
    }
    /**
     * Puts the element's html through the template engine to render out all needed context variables (constants/messages)
     * Note that this is executed during boot, so eg. the language for the messages will probably not be initialized correctly,
     * so avoid using $MESSAGES in resource element names. This is mainly made for using $CONSTANTS in the resource URLs.
     * Also note that this will automatically fingerprint the URIs in the resources.
     */
    private Element renderResourceElement(Element element) throws IOException
    {
        // Warning: tag templates are stored/searched in the cache by their relative path (eg. see TemplateCache.putByRelativePath()),
        // so make sure you don't use this URI to instance template or you'll end up overwriting the cache with this temporary StringSource
        Template template = R.resourceManager().newTemplate(new StringSource(element.toString(), MimeTypes.HTML, R.i18n().getOptimalLocale()));

        //there should always be a first element since we started out with an element, right?
        return new Source(template.render()).getFirstElement();
    }
    private Iterable<Element> addSuperTemplateResources(TemplateResourcesDirective.Argument type, StringBuilder html, Iterable<Element> templateElements, Iterable<Element> superTemplateElements,
                                                        String attribute)
                    throws IOException
    {
        Iterable<Element> retVal = templateElements;

        if (superTemplateElements != null && superTemplateElements.iterator().hasNext()) {
            for (Element element : superTemplateElements) {
                html.append(buildResourceHtml(type, element, attribute));
            }
            retVal = Iterables.concat(superTemplateElements, templateElements);
        }

        return retVal;
    }
    private Iterable<Element> buildScopeResourceIterator(Iterable<Element> elements)
    {
        final Iterator iter = Iterators.filter(elements.iterator(), new Predicate<Element>()
        {
            @Override
            public boolean apply(Element element)
            {
                return testResourceModeScope(getResourceModeScope(element)) && testResourceRoleScope(getResourceRoleScope(element));
            }
        });

        return new Iterable<Element>()
        {
            @Override
            public Iterator iterator()
            {
                return iter;
            }
        };
    }
    /**
     * Post-parse the html of this template to save variable selectors, standardize html, etc.
     */
    private void parseHtml()
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

            //(3) check if the element is a property
            if (this.isPropertyTag(e)) {
                phase2.add(new CollapseTemplateProperty(this.cssSelector(e), e));
            }

            //(4) check if the element is a template
            //Note that we can't use isTemplateInstanceTag() here because this is called
            //*during* template caching building, so we'll add a semi-check and postpone
            //to the parsing in the substitution
            if (e.tagName().contains("-")) {
                phase2.add(new CollapseTemplateInstance(this.cssSelector(e), e));
            }
        }
        //now add all phase 2 substitutions at the end of the existing list so we're sure they're executed after phase 1
        this.normalizationSubstitutions.addAll(phase2);
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

    public static class CollapseTemplateProperty extends SubstitionReference
    {
        protected String element;
        protected CollapseTemplateProperty(String cssSelector, org.jsoup.nodes.Element propertyElement)
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

    public static class CollapseTemplateInstance extends SubstitionReference
    {
        protected Map<String, String> attributes;
        protected CollapseTemplateInstance(String cssSelector, org.jsoup.nodes.Element element)
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
}
