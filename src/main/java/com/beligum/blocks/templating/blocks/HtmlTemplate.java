package com.beligum.blocks.templating.blocks;

import com.beligum.base.security.PermissionRole;
import com.beligum.base.security.PermissionsConfigurator;
import com.beligum.base.server.R;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.templating.blocks.directives.*;
import com.google.common.base.CaseFormat;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import net.htmlparser.jericho.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;

import java.io.IOException;
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

    // set this attribute on an instance to not render the tag itself, but mimic another tag name
    // (eg <blocks-text data-render-tag="div"> to render out a <div> instead of a <blocks-text>)
    // Beware: this will make the tag's direction server-to-client only!!
    //NOTE: if this is set to empty, don't render the tag (and it's attributes) at all
    public static final String ATTRIBUTE_RENDER_TAG = "data-render-tag";

    public enum ResourceScopeMode
    {
        UNDEFINED,
        EMPTY,
        edit
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

    //this will enable us to save the 'inheritance tree'
    protected HtmlTemplate parent;

    // This will hold the html before the <template> tags
    protected Segment prefixHtml;

    // This will hold the html inside the <template> tags
    protected Segment innerHtml;

    // This will hold the html after the <template> tags
    protected Segment suffixHtml;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static HtmlTemplate create(String templateName, Source source, Path absolutePath, Path relativePath, HtmlTemplate parent) throws Exception
    {
        HtmlTemplate retVal = null;

        if (representsTagTemplate(source)) {
            retVal = new TagTemplate(templateName, source, absolutePath, relativePath, parent);
        }
        else if (representsPageTemplate(source)) {
            retVal = new PageTemplate(templateName, source, absolutePath, relativePath, parent);
        }

        return retVal;
    }
    private static boolean representsTagTemplate(Source source)
    {
        return !source.getAllElements("template").isEmpty();
    }
    private static boolean representsPageTemplate(Source source)
    {
        boolean retVal = false;

        List<Element> html = source.getAllElements("html");
        if (html != null && html.size() == 1) {
            Attributes htmlAttr = html.get(0).getAttributes();
            if (htmlAttr.get("template") != null) {
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
    public HtmlTemplate getParent()
    {
        return parent;
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

    //-----PROTECTED METHODS-----
    protected void init(String templateName, Source source, Path absolutePath, Path relativePath, HtmlTemplate parent) throws Exception
    {
        //INIT THE PATHS
        this.absolutePath = absolutePath;
        this.relativePath = relativePath;

        //INIT THE NAMES
        this.templateName = templateName;
        this.velocityName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, this.templateName);
        this.parent = parent;

        //INIT THE HTML
        //note: this should take the parent into account
        OutputDocument tempHtml = this.doInitHtmlPreparsing(new OutputDocument(source), parent);

        //Note that we need to eat these values for PageTemplates because we don't want them to end up at the client side (no problem for TagTemplates)
        this.titles = parent != null ? parent.getTitles() : new HashMap<Locale, String>();
        this.fillMetaValues(tempHtml, this.titles, MetaProperty.title, true);

        this.descriptions = parent != null ? parent.getDescriptions() : new HashMap<Locale, String>();
        this.fillMetaValues(tempHtml, this.descriptions, MetaProperty.description, true);

        this.icons = parent != null ? parent.getIcons() : new HashMap<Locale, String>();
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
        if (this.controllerClass == null && parent != null) {
            this.controllerClass = parent.getControllerClass();
        }

        this.displayType = parent != null ? parent.getDisplayType() : MetaDisplayType.DEFAULT;
        String displayType = this.getMetaValue(tempHtml, MetaProperty.display, true);
        if (!StringUtils.isEmpty(displayType)) {
            this.displayType = MetaDisplayType.valueOf(displayType.toUpperCase());
        }

        this.inlineStyleElements = getInlineStyles(tempHtml);
        this.externalStyleElements = getExternalStyles(tempHtml);
        this.inlineScriptElements = getInlineScripts(tempHtml);
        this.externalScriptElements = getExternalScripts(tempHtml);

        //prepend the html with the parent resources if it's there
        if (parent!=null) {
            StringBuilder parentResourceHtml = new StringBuilder();
            this.inlineStyleElements = addParentResources(TemplateResourcesDirective.Argument.inlineStyles, parentResourceHtml, this.inlineStyleElements, parent.getAllInlineStyleElements(), null);
            this.externalStyleElements = addParentResources(TemplateResourcesDirective.Argument.externalStyles, parentResourceHtml, this.externalStyleElements, parent.getAllExternalStyleElements(), "href");
            this.inlineScriptElements = addParentResources(TemplateResourcesDirective.Argument.inlineScripts, parentResourceHtml, this.inlineScriptElements, parent.getAllInlineScriptElements(), null);
            this.externalScriptElements = addParentResources(TemplateResourcesDirective.Argument.externalScripts, parentResourceHtml, this.externalScriptElements, parent.getAllExternalScriptElements(), "src");
            tempHtml.insert(0, parentResourceHtml);
        }

        //now save the (possibly altered) html source (and unwrap it in case of a tag template)
        this.saveHtml(tempHtml, parent);
    }
    protected abstract void saveHtml(OutputDocument document, HtmlTemplate parent);

    //-----PROTECTED METHODS-----
    protected abstract OutputDocument doInitHtmlPreparsing(OutputDocument document, HtmlTemplate parent) throws IOException;
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
    private Iterable<Element> getInlineStyles(OutputDocument html)
    {
        Iterable<Element> retVal = html.getSegment().getAllElements("style");

        Iterator<Element> iter = retVal.iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            //html.remove(retVal);
            html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.inlineStyles, element, null));
        }

        return retVal;
    }
    private Iterable<Element> getExternalStyles(OutputDocument html)
    {
        Iterable<Element> retVal = html.getSegment().getAllElements("rel", styleLinkRelAttrValue);

        Iterator<Element> iter = retVal.iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (!element.getName().equals("link")) {
                iter.remove();
            }
            else {
                //html.remove(el);
                html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.externalStyles, element, element.getAttributeValue("href")));
            }
        }

        return retVal;
    }
    private Iterable<Element> getInlineScripts(OutputDocument html)
    {
        Iterable<Element> retVal = html.getSegment().getAllElements("script");

        Iterator<Element> iter = retVal.iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (element.getAttributeValue("src") != null) {
                iter.remove();
            }
            else {
                //html.remove(el);
                html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.inlineScripts, element, null));
            }
        }

        return retVal;
    }
    private Iterable<Element> getExternalScripts(OutputDocument html)
    {
        Iterable<Element> retVal = html.getSegment().getAllElements("script");

        Iterator<Element> iter = retVal.iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            if (element.getAttributeValue("src") == null) {
                iter.remove();
            }
            else {
                //html.remove(el);
                html.replace(element, buildResourceHtml(TemplateResourcesDirective.Argument.externalScripts, element, element.getAttributeValue("src")));
            }
        }

        return retVal;
    }
    private String buildResourceHtml(TemplateResourcesDirective.Argument type, Element element, String attr)
    {
        final boolean print = false;
        StringBuilder builder = new StringBuilder();

        builder.append("#").append(TagTemplateResourceDirective.NAME).append("(").append(type.ordinal()).append(",").append(print).append(",'").append(attr).append("','")
               .append(HtmlTemplate.getResourceRoleScope(element)).append("',").append(HtmlTemplate.getResourceModeScope(element).ordinal()).append(")").append(element.toString())
               .append("#end").append("\n");

        return builder.toString();
    }
    private Iterable<Element> addParentResources(TemplateResourcesDirective.Argument type, StringBuilder html, Iterable<Element> templateElements, Iterable<Element> parentElements, String attribute)
    {
        Iterable<Element> retVal = templateElements;

        if (parentElements!=null && parentElements.iterator().hasNext()) {
            for (Element element : parentElements) {
                html.append(buildResourceHtml(type, element, attribute==null?null:element.getAttributeValue(attribute)));
            }
            retVal = Iterables.concat(parentElements, templateElements);
        }

        return retVal;
    }
    public static PermissionRole getResourceRoleScope(Element resource)
    {
        PermissionRole retVal = PermissionsConfigurator.ROLE_GUEST;

        Attribute scope = resource.getAttributes().get(ATTRIBUTE_RESOURCE_ROLE_SCOPE);
        if (scope != null && !StringUtils.isEmpty(scope.getValue())) {
            retVal = R.configuration().getSecurityConfig().lookupPermissionRole(scope.getValue());
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
