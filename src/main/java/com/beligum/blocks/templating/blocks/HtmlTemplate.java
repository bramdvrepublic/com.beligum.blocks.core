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
import com.google.common.base.CaseFormat;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import net.htmlparser.jericho.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;

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
    protected String velocityName;
    protected Map<Locale, String> titles;
    protected Map<Locale, String> descriptions;
    protected Map<Locale, String> icons;
    protected Class<TemplateController> controllerClass;
    protected Iterable<Element> inlineScriptElements;
    protected Iterable<Element> externalScriptElements;
    protected Iterable<Element> inlineStyleElements;
    protected Iterable<Element> externalStyleElements;
    protected MetaDisplayType displayType;
    protected URI vocab;
    protected Map<String, URI> prefixes;

    //this will enable us to save the 'inheritance tree'
    protected HtmlTemplate superTemplate;

    // This will hold the html before the <template> tags
    protected Segment prefixHtml;

    // This will hold the html inside the <template> tags
    protected Segment innerHtml;

    // This will hold the html after the <template> tags
    protected Segment suffixHtml;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static HtmlTemplate create(String templateName, Source source, Path absolutePath, Path relativePath, HtmlTemplate superTemplate) throws Exception
    {
        HtmlTemplate retVal = null;

        if (representsTagTemplate(source)) {
            retVal = new TagTemplate(templateName, source, absolutePath, relativePath, superTemplate);
        }
        else if (representsPageTemplate(source)) {
            retVal = new PageTemplate(templateName, source, absolutePath, relativePath, superTemplate);
        }

        return retVal;
    }
    private static boolean representsTagTemplate(Source source)
    {
        return !source.getAllElements(HtmlParser.WEBCOMPONENTS_TEMPLATE_ELEM).isEmpty();
    }
    private static boolean representsPageTemplate(Source source)
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
    public String getVelocityTemplateName()
    {
        return velocityName;
    }
    public Segment getPrefixHtml()
    {
        return prefixHtml;
    }
    public Segment getInnerHtml()
    {
        return innerHtml;
    }
    public Segment getSuffixHtml()
    {
        return suffixHtml;
    }
    //    public Segment buildFullHtml()
    //    {
    //        return new Source(Joiner.on("").join(this.getPrefixHtml(), this.getInnerHtml(), this.getSuffixHtml()));
    //    }
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
     * Will create a new html tag; eg for <template class="classname"></template>, this will return <tag-name class="classname"></tag-name>
     */
    public String createNewHtmlInstance()
    {
        StringBuilder retVal = new StringBuilder();

        retVal.append("<").append(this.getTemplateName());
        if (this.getAttributes() != null) {
            for (Map.Entry<String, String> a : this.getAttributes().entrySet()) {
                retVal.append(" ").append(a.getKey()).append("=\"").append(a.getValue()).append("\"");
            }
        }
        retVal.append("></").append(this.getTemplateName()).append(">");

        return retVal.toString();
    }
    public URI getVocab()
    {
        return vocab;
    }
    public Map<String, URI> getPrefixes()
    {
        return prefixes;
    }

    //-----PROTECTED METHODS-----
    protected void init(String templateName, Source source, Path absolutePath, Path relativePath, HtmlTemplate superTemplate) throws Exception
    {
        //INIT THE PATHS
        this.absolutePath = absolutePath;
        this.relativePath = relativePath;

        //INIT THE NAMES
        this.templateName = templateName;
        this.velocityName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, this.templateName);
        this.superTemplate = superTemplate;

        //INIT THE HTML
        //note: this should take the parent into account
        OutputDocument tempHtml = this.doInitHtmlPreparsing(new OutputDocument(source), superTemplate);

        this.vocab = HtmlParser.parseRdfVocabAttribute(this, this.attributes.get(HtmlParser.RDF_VOCAB_ATTR));

        this.prefixes = new LinkedHashMap<>();
        HtmlParser.parseRdfPrefixAttribute(this, this.attributes.get(HtmlParser.RDF_PREFIX_ATTR), this.prefixes);

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
    }
    protected abstract void saveHtml(OutputDocument document, HtmlTemplate superTemplate);

    //-----PROTECTED METHODS-----
    protected abstract OutputDocument doInitHtmlPreparsing(OutputDocument document, HtmlTemplate superTemplate) throws IOException;
    protected void setAttributes(Map<String, String> attributes)
    {
        this.attributes = attributes;
    }
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
    public MetaDisplayType getDisplayType()
    {
        return displayType;
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
     */
    private Element renderResourceElement(Element element) throws IOException
    {
        Element retVal = null;

        //this allows us to use velocity variables in the resource URLs
        Template template = R.resourceManager().newTemplate(new StringSource(this.getRelativePath().toUri(), element.toString(), MimeTypes.HTML, R.i18n().getOptimalLocale()));
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
    public static PermissionRole getResourceRoleScope(Element resource)
    {
        PermissionRole retVal = PermissionsConfigurator.ROLE_GUEST;

        Attribute scope = resource.getAttributes().get(ATTRIBUTE_RESOURCE_ROLE_SCOPE);
        if (scope != null && !StringUtils.isEmpty(scope.getValue())) {
            SecurityConfiguration securityConfig = R.configuration().getSecurityConfig();
            if (securityConfig != null) {
                retVal = securityConfig.lookupPermissionRole(scope.getValue());
            }
        }

        //possible that the above function re-fills it with null
        if (retVal == null) {
            retVal = PermissionsConfigurator.ROLE_GUEST;
        }

        return retVal;
    }
    public static ResourceScopeMode getResourceModeScope(Element resource)
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

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "<" + templateName + ">";
    }
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (!(o instanceof HtmlTemplate))
            return false;

        HtmlTemplate that = (HtmlTemplate) o;

        return !(templateName != null ? !templateName.equals(that.templateName) : that.templateName != null);

    }
    @Override
    public int hashCode()
    {
        return templateName != null ? templateName.hashCode() : 0;
    }
}
