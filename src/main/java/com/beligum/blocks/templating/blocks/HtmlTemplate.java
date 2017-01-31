package com.beligum.blocks.templating.blocks;

import com.beligum.base.config.SecurityConfiguration;
import com.beligum.base.resources.MimeTypes;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.sources.StringSource;
import com.beligum.base.security.PermissionRole;
import com.beligum.base.security.PermissionsConfigurator;
import com.beligum.base.server.R;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.templating.blocks.directives.TagTemplateResourceDirective;
import com.beligum.blocks.templating.blocks.directives.TemplateResourcesDirective;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import net.htmlparser.jericho.*;
import net.htmlparser.jericho.Attribute;
import net.htmlparser.jericho.Attributes;
import net.htmlparser.jericho.Element;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

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

    /**
     * These are the names of first folders that won't be taken into account when building the name of the element
     * Eg. /imports/blocks/test/tag.html will have the name "blocks-test-tag"
     */
    protected static String[] INVISIBLE_START_FOLDERS = { "import", "imports" };
    protected static final Pattern styleLinkRelAttrValue = Pattern.compile("stylesheet");

    //-----VARIABLES-----
    protected Map<String, String> attributes;
    protected Path absolutePath;
    protected Path relativePath;
    protected String templateName;
    protected Map<Locale, String> titles;
    protected Map<Locale, String> descriptions;
    protected Map<Locale, String> icons;
    protected Class<TemplateController> controllerClass;
    protected Iterable<Element> inlineScriptElements;
    protected Iterable<Element> externalScriptElements;
    protected Iterable<Element> inlineStyleElements;
    protected Iterable<Element> externalStyleElements;
    protected MetaDisplayType displayType;
    protected URI rdfVocab;
    protected Map<String, URI> rdfPrefixes;
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

        this.rdfVocab = HtmlParser.parseRdfVocabAttribute(this, this.attributes.get(HtmlParser.RDF_VOCAB_ATTR));

        this.rdfPrefixes = new LinkedHashMap<>();
        HtmlParser.parseRdfPrefixAttribute(this, this.attributes.get(HtmlParser.RDF_PREFIX_ATTR), this.rdfPrefixes);

        //Note that we need to eat these values for PageTemplates because we don't want them to end up at the client side (no problem for TagTemplates)
        this.titles = superTemplate != null ? superTemplate.getTitles() : new HashMap<Locale, String>();
        this.fillMetaValues(tempHtml, this.titles, MetaProperty.title, true);

        this.descriptions = superTemplate != null ? superTemplate.getDescriptions() : new HashMap<Locale, String>();
        this.fillMetaValues(tempHtml, this.descriptions, MetaProperty.description, true);

        this.icons = superTemplate != null ? superTemplate.getIcons() : new HashMap<Locale, String>();
        this.fillMetaValues(tempHtml, this.icons, MetaProperty.icon, true);

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

        this.inlineStyleElements = getInlineStyles(tempHtml);
        this.externalStyleElements = getExternalStyles(tempHtml);
        this.inlineScriptElements = getInlineScripts(tempHtml);
        this.externalScriptElements = getExternalScripts(tempHtml);

        //prepend the html with the parent resources if it's there
        if (superTemplate != null) {
            StringBuilder superTemplateResourceHtml = new StringBuilder();
            this.inlineStyleElements =
                            addSuperTemplateResources(TemplateResourcesDirective.Argument.inlineStyles, superTemplateResourceHtml, this.inlineStyleElements, superTemplate.getAllInlineStyleElements(),
                                                      null);
            this.externalStyleElements =
                            addSuperTemplateResources(TemplateResourcesDirective.Argument.externalStyles, superTemplateResourceHtml, this.externalStyleElements,
                                                      superTemplate.getAllExternalStyleElements(), "href");
            this.inlineScriptElements =
                            addSuperTemplateResources(TemplateResourcesDirective.Argument.inlineScripts, superTemplateResourceHtml, this.inlineScriptElements,
                                                      superTemplate.getAllInlineScriptElements(), null);
            this.externalScriptElements =
                            addSuperTemplateResources(TemplateResourcesDirective.Argument.externalScripts, superTemplateResourceHtml, this.externalScriptElements,
                                                      superTemplate.getAllExternalScriptElements(), "src");
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
            // if you specifically set the mode flag, test it
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
     * Same test as the method above, but with a JSoup element
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
    public static Attribute getPropertyAttribute(StartTag startTag)
    {
        Attribute retVal = null;

        Attributes attributes = startTag.getAttributes();

        //regular property has precedence over data-property
        retVal = attributes.get(HtmlParser.RDF_PROPERTY_ATTR);

        //now try the data-property
        if (retVal == null) {
            retVal = attributes.get(HtmlParser.NON_RDF_PROPERTY_ATTR);
        }

        return retVal;
    }
    /**
     * Same test as the method above, but with a JSoup element
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

    //-----PUBLIC METHODS-----
    /**
     * Controls if we need to wrap the instance with the <template-name></template-name> tag or not (eg. for page templates, we don't want tturn
     */
    public abstract boolean renderTemplateTag();

    /**
     * @return the name of the file, without the file extension, where parent directories are represented by dashes.
     * Eg. /blocks/test/tag.html will have the name "blocks-test-tag" and result in tags like <blocks-test-tag></blocks-test-tag>
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
    public Map<Locale, String> getTitles()
    {
        return titles;
    }
    public Map<Locale, String> getDescriptions()
    {
        return descriptions;
    }
    public Map<Locale, String> getIcons()
    {
        return icons;
    }
    public Class<TemplateController> getControllerClass()
    {
        return controllerClass;
    }
    public HtmlTemplate getSuperTemplate()
    {
        return superTemplate;
    }
    public Iterable<Element> getAllInlineScriptElements()
    {
        return inlineScriptElements;
    }
    public Iterable<Element> getAllExternalScriptElements()
    {
        return externalScriptElements;
    }
    public Iterable<Element> getAllInlineStyleElements()
    {
        return inlineStyleElements;
    }
    public Iterable<Element> getAllExternalStyleElements()
    {
        return externalStyleElements;
    }
    //TODO we should probably optimize this a bit, but beware, it still needs to be user-dynamic...
    public Iterable<Element> getInlineScriptElementsForCurrentScope()
    {
        return this.buildScopeResourceIterator(this.getAllInlineScriptElements());
    }
    public Iterable<Element> getExternalScriptElementsForCurrentScope()
    {
        return this.buildScopeResourceIterator(this.getAllExternalScriptElements());
    }
    public Iterable<Element> getInlineStyleElementsForCurrentScope()
    {
        return this.buildScopeResourceIterator(this.getAllInlineStyleElements());
    }
    public Iterable<Element> getExternalStyleElementsForCurrentScope()
    {
        return this.buildScopeResourceIterator(this.getAllExternalStyleElements());
    }
    /**
     * Will create a new html tag; eg for <template class="classname"></template>,
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
    public URI getRdfVocab()
    {
        return rdfVocab;
    }
    public Map<String, URI> getRdfPrefixes()
    {
        return rdfPrefixes;
    }
    public MetaDisplayType getDisplayType()
    {
        return displayType;
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
    protected static String parseTemplateName(Path relativePath) throws ParseException
    {
        String retVal = null;

        Path namePath = relativePath;
        if (relativePath != null) {
            for (String invisiblePrefix : INVISIBLE_START_FOLDERS) {
                if (namePath.startsWith(invisiblePrefix) || namePath.startsWith(namePath.getFileSystem().getSeparator() + invisiblePrefix)) {
                    namePath = namePath.subpath(1, namePath.getNameCount());
                    //this is a safe choice that might change in the future: do we want to keep eating first folders? Of so, then we should actually created over, no?
                    break;
                }
            }
            retVal = StringUtils.strip(namePath.toString().replaceAll("/", "-"), "-");
            int lastDot = retVal.lastIndexOf(".");
            if (lastDot >= 0) {
                retVal = retVal.substring(0, lastDot);
            }
            //note: we may want to let the user override the name with an id attribute on the <template> tag

            // In Web Components speak, this new element is a Custom Element,
            // and the only two requirements are that its name must contain a dash,
            // and its prototype must extend HTMLElement.
            // See https://css-tricks.com/modular-future-web-components/
            if (!retVal.contains("-")) {
                throw new ParseException("The name of an import template should always contain at least one dash; '" + retVal + "' in " + relativePath, 0);
            }
        }

        return retVal;
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
    private void fillMetaValues(OutputDocument html, Map<Locale, String> target, MetaProperty property, boolean eatItUp)
    {
        List<Element> metas = html.getSegment().getAllElements("meta");
        Iterator<Element> iter = metas.iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            String propertyVal = element.getAttributeValue("property");
            if (propertyVal != null && propertyVal.equalsIgnoreCase(BLOCKS_META_TAG_PROPERTY_PREFIX + property.toString())) {
                Locale locale = Locale.ROOT;
                String localeStr = element.getAttributeValue("lang");
                if (localeStr != null) {
                    locale = Locale.forLanguageTag(localeStr);
                }
                String value = element.getAttributeValue("content");
                target.put(locale, value);

                if (eatItUp) {
                    html.remove(element);
                }
            }
        }
    }
    private String getMetaValue(OutputDocument output, MetaProperty property, boolean eatItUp)
    {
        String retVal = null;

        List<Element> metas = output.getSegment().getAllElements("meta");
        Iterator<Element> iter = metas.iterator();
        while (retVal == null && iter.hasNext()) {
            Element element = iter.next();
            String propertyVal = element.getAttributeValue("property");
            if (propertyVal != null && propertyVal.equalsIgnoreCase(BLOCKS_META_TAG_PROPERTY_PREFIX + property.toString())) {
                retVal = element.getAttributeValue("content");

                if (eatItUp) {
                    output.remove(element);
                }
            }
        }

        return retVal;
    }
    private Iterable<Element> getInlineStyles(OutputDocument html) throws IOException
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
    private Iterable<Element> getExternalStyles(OutputDocument html) throws IOException
    {
        List<Element> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements("rel", styleLinkRelAttrValue).iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (!element.getName().equals("link")) {
                iter.remove();
            }
            else {
                Element parsedElement = this.renderResourceElement(element);
                html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.externalStyles, parsedElement, "href"));
                retVal.add(parsedElement);
            }
        }

        return retVal;
    }
    private Iterable<Element> getInlineScripts(OutputDocument html) throws IOException
    {
        List<Element> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements("script").iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (element.getAttributeValue("src") != null) {
                iter.remove();
            }
            else {
                Element parsedElement = this.renderResourceElement(element);
                html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.inlineScripts, parsedElement, null));
                retVal.add(parsedElement);
            }
        }

        return retVal;
    }
    private Iterable<Element> getExternalScripts(OutputDocument html) throws IOException
    {
        List<Element> retVal = new ArrayList<>();

        Iterator<Element> iter = html.getSegment().getAllElements("script").iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (element.getAttributeValue("src") == null) {
                iter.remove();
            }
            else {
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
    private String buildResourceHtml(TemplateResourcesDirective.Argument type, Element element, String attr) throws IOException
    {
        final boolean print = false;
        StringBuilder builder = new StringBuilder();
        String attrValue = attr == null ? null : element.getAttributeValue(attr);
        String elementStr = element.toString();
        //Note: in case of a dynamic (=non-immutable) asset, we'll postpone the fingerprinting all the way till
        //com.beligum.blocks.templating.blocks.TemplateResources.Resource.getElement() and getValue()
        boolean isImmutable = false;

        //this means we're dealing with an external resource that may need fingerprinting
        if (R.configuration().getResourceConfig().getEnableFingerprintedResources()) {
            if (attrValue != null) {
                //validate the URI
                URI uri = UriBuilder.fromUri(attrValue).build();
                Resource resource = R.resourceManager().get(uri);
                //this means the resource exists in our local system
                if (resource != null) {
                    //if the resource is immutable (won't change anymore), we might as well calculate it's fingerprint now
                    if (resource.isImmutable()) {
                        //first, replace the attribute value
                        attrValue = R.resourceManager().getFingerprinter().fingerprintUri(uri.toString());

                        //but also replace the attribute in the element itself
                        Segment attrValueSeg = element.getAttributes().get(attr).getValueSegment();
                        OutputDocument outputDocument = new OutputDocument(element);
                        outputDocument.replace(attrValueSeg, attrValue);
                        elementStr = outputDocument.toString();

                        //signal our directive the fingerprinting is already performed
                        isImmutable = false;
                    }
                }
            }
        }

        //Note: we don't append a newline: it clouds the output html with too much extra whitespace...
        builder.append("#").append(TagTemplateResourceDirective.NAME).append("(")

               .append(type.ordinal()).append(",")
               .append(print).append(",'")
               .append(attrValue).append("',")
               .append(isImmutable).append(",'")
               .append(HtmlTemplate.getResourceRoleScope(element)).append("',")
               .append(HtmlTemplate.getResourceModeScope(element).ordinal()).append(",")
               .append(HtmlTemplate.getResourceJoinHint(element).ordinal())

               .append(")")
               .append(elementStr)
               .append("#end");

        return builder.toString();
    }
    /**
     * Puts the element's html through the template engine to render out all needed context variables (constants/messages)
     * This allows us to use velocity variables in the resource URLs
     */
    private Element renderResourceElement(Element element) throws IOException
    {
        Element retVal = null;

        // Warning: tag templates are stored/searched in the cache by their relative path (eg. see TemplateCache.putByRelativePath()),
        // so make sure you don't use this URI to create template or you'll end up overwriting the cache with this temporary StringSource
        Template template = R.resourceManager().newTemplate(new StringSource(element.toString(), MimeTypes.HTML, R.i18n().getOptimalLocale()));

        //there should always be a first element since we started out with an element, right?
        retVal = new Source(template.render()).getFirstElement();

        return retVal;
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
                    this.normalizationSubstitutions.add(new ReplaceVariableAttributeValue(this.cssSelector(e), a));
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
     * This is the exact same code as org.jsoup.nodes.Element.cssSelector() but with the classes commented out
     * because in our case, the classes are often constants and cause troubles. From what I tested, and because
     * of the way we double check by testing the rendered out values during comparison, I don't think this will
     * cause problems.
     * Note that we're still backwards compatible with the Element.select() code though...
     */
    private String cssSelector(org.jsoup.nodes.Element element)
    {
        if (element.id().length() > 0)
            return "#" + element.id();

        StringBuilder selector = new StringBuilder(element.tagName());
//        String classes = StringUtil.join(element.classNames(), ".");
//        if (classes.length() > 0)
//            selector.append('.').append(classes);

        if (element.parent() == null || element.parent() instanceof Document) // don't add Document to selector, as will always have a html node
            return selector.toString();

        selector.insert(0, " > ");
        if (element.parent().select(selector.toString()).size() > 1)
            selector.append(String.format(":nth-child(%d)", element.elementSiblingIndex() + 1));

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
        protected ReplaceVariableAttributeValue(String cssSelector, org.jsoup.nodes.Attribute attribute)
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
}
