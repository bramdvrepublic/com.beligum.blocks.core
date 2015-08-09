package com.beligum.blocks.templating.blocks;

import com.beligum.base.security.PermissionRole;
import com.beligum.base.security.PermissionsConfigurator;
import com.beligum.base.server.R;
import com.google.common.base.CaseFormat;
import com.google.common.base.Predicate;
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
    /**
     * These are the names of first folders that won't be taken into account when building the name of the element
     * Eg. /imports/blocks/test/tag.html will have the name "blocks-test-tag"
     */
    protected String[] INVISIBLE_START_FOLDERS = { "import", "imports" };
    protected static final Pattern styleLinkRelAttrValue = Pattern.compile("stylesheet");

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

    // controls if the <script> or <style> tag needs to be included in the rendering
    // use it like this: <script data-scope-role="admin"> to eg. only include the script when and ADMIN-role is logged in
    // Role names are the same ones we use for Shiro
    public static final String RESOURCE_ROLE_SCOPE_ATTRIBUTE = "data-scope-role";

    //-----VARIABLES-----
    protected Source document;
    protected Segment html;
    protected Attributes attributes;
    protected Path absolutePath;
    protected Path relativePath;
    protected String templateName;
    protected String velocityName;
    protected Map<Locale, String> titles;
    protected Map<Locale, String> descriptions;
    protected Map<Locale, String> icons;
    protected Class<TemplateController> controllerClass;
    protected List<Element> inlineScriptElements;
    protected List<Element> externalScriptElements;
    protected List<Element> inlineStyleElements;
    protected List<Element> externalStyleElements;
    protected MetaDisplayType displayType;

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static HtmlTemplate create(Source source, Path absolutePath, Path relativePath) throws Exception
    {
        HtmlTemplate retVal = null;

        if (representsTagTemplate(source)) {
            retVal = new TagTemplate(source, absolutePath, relativePath);
        }
        else if (representsPageTemplate(source)) {
            retVal = new PageTemplate(source, absolutePath, relativePath);
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
     * Controls if we need to wrap the instance with the <template-name></template-name> tag or not (eg. for page templates, we don't want this)
     *
     * @return
     */
    public abstract boolean renderTemplateTag();

    public static List<Element> getInlineStyles(Source source)
    {
        return source.getAllElements("style");
    }
    public static List<Element> getExternalStyles(Source source)
    {
        List<Element> retVal = source.getAllElements("rel", styleLinkRelAttrValue);
        Iterator<Element> iter = retVal.iterator();
        while (iter.hasNext()) {
            if (!iter.next().getName().equals("link")) {
                iter.remove();
            }
        }

        return retVal;
    }
    public static List<Element> getInlineScripts(Source source)
    {
        List<Element> retVal = source.getAllElements("script");
        Iterator<Element> iter = retVal.iterator();
        while (iter.hasNext()) {
            if (iter.next().getAttributeValue("src") != null) {
                iter.remove();
            }
        }

        return retVal;
    }
    public static List<Element> getExternalScripts(Source source)
    {
        List<Element> retVal = source.getAllElements("script");
        Iterator<Element> iter = retVal.iterator();
        while (iter.hasNext()) {
            if (iter.next().getAttributeValue("src") == null) {
                iter.remove();
            }
        }

        return retVal;
    }
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
    public Segment getHtml()
    {
        return html;
    }
    public Attributes getAttributes()
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
    public List<Element> getAllInlineScriptElements()
    {
        return inlineScriptElements;
    }
    public List<Element> getAllExternalScriptElements()
    {
        return externalScriptElements;
    }
    public List<Element> getAllInlineStyleElements()
    {
        return inlineStyleElements;
    }
    public List<Element> getAllExternalStyleElements()
    {
        return externalStyleElements;
    }
    //TODO we should probably optimize this a bit, but beware, it still needs to be user-dynamic...
    public Iterable<Element> getInlineScriptElementsForCurrentUser()
    {
        return this.buildRoleScopeResourceIterator(this.getAllInlineScriptElements());
    }
    public Iterable<Element> getExternalScriptElementsForCurrentUser()
    {
        return this.buildRoleScopeResourceIterator(this.getAllExternalScriptElements());
    }
    public Iterable<Element> getInlineStyleElementsForCurrentUser()
    {
        return this.buildRoleScopeResourceIterator(this.getAllInlineStyleElements());
    }
    public Iterable<Element> getExternalStyleElementsForCurrentUser()
    {
        return this.buildRoleScopeResourceIterator(this.getAllExternalStyleElements());
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
    public Class<?> getControllerClass()
    {
        return controllerClass;
    }

    //-----PROTECTED METHODS-----
    protected void init(Source document, Path absolutePath, Path relativePath) throws Exception
    {
        //INIT THE PATHS
        this.absolutePath = absolutePath;
        this.relativePath = relativePath;

        Path namePath = this.relativePath;
        if (this.relativePath != null) {
            for (String invisiblePrefix : INVISIBLE_START_FOLDERS) {
                if (namePath.startsWith(invisiblePrefix) || namePath.startsWith(namePath.getFileSystem().getSeparator() + invisiblePrefix)) {
                    namePath = namePath.subpath(1, namePath.getNameCount());
                    //this is a safe choice that might change in the future: do we want to keep eating first folders? Of so, then we should actually created over, no?
                    break;
                }
            }
            this.templateName = StringUtils.strip(namePath.toString().replaceAll("/", "-"), "-");
            int lastDot = this.templateName.lastIndexOf(".");
            if (lastDot >= 0) {
                this.templateName = this.templateName.substring(0, lastDot);
            }
            //note: we may want to let the user override the name with an id attribute on the <template> tag

            // In Web Components speak, this new element is a Custom Element,
            // and the only two requirements are that its name must contain a dash,
            // and its prototype must extend HTMLElement.
            // See https://css-tricks.com/modular-future-web-components/
            if (!this.templateName.contains("-")) {
                throw new ParseException("The name of an import template should always contain at least one dash; '" + this.templateName + "' in " + relativePath, 0);
            }

            this.velocityName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, this.templateName);
        }
        else {
            this.templateName = null;
            this.velocityName = null;
        }

        //INIT THE HTML
        this.document = document;
        OutputDocument html = new OutputDocument(document);
        this.doInitHtmlPreparsing(document, html);
        //TODO this.attributes = html.

        //Note that we need to eat these values for PageTemplates because we don't want them to end up at the client side (no problem for TagTemplates)
        this.fillMetaValues(this.document, html, this.titles = new HashMap<>(), MetaProperty.title, true);
        this.fillMetaValues(this.document, html, this.descriptions = new HashMap<>(), MetaProperty.description, true);
        this.fillMetaValues(this.document, html, this.icons = new HashMap<>(), MetaProperty.icon, true);
        String controllerClassStr = this.getMetaValue(this.document, html, MetaProperty.controller, true);
        if (!StringUtils.isEmpty(controllerClassStr)) {
            Class<?> clazz = Class.forName(controllerClassStr);
            if (TemplateController.class.isAssignableFrom(clazz)) {
                this.controllerClass = (Class<TemplateController>) clazz;
            }
            else {
                throw new ParseException("Encountered template with a controller that doesn't implement " + TemplateController.class.getSimpleName() + "; " + relativePath, 0);
            }
        }

        this.displayType = MetaDisplayType.DEFAULT;
        String displayType = this.getMetaValue(this.document, html, MetaProperty.display, true);
        if (!StringUtils.isEmpty(displayType)) {
            this.displayType = MetaDisplayType.valueOf(displayType.toUpperCase());
        }

        this.inlineStyleElements = getInlineStyles(this.document);
        this.externalStyleElements = getExternalStyles(this.document);

        this.inlineScriptElements = getInlineScripts(this.document);
        this.externalScriptElements = getExternalScripts(this.document);

        //now save the (possibly altered) html source
        this.html = new Source(html.toString());
    }

    //-----PROTECTED METHODS-----
    protected abstract void doInitHtmlPreparsing(Source document, OutputDocument html) throws IOException;
    protected void setAttributes(Attributes attributes)
    {
        this.attributes = attributes;
    }

    //-----PRIVATE METHODS-----
    private void fillMetaValues(Source source, OutputDocument output, Map<Locale, String> target, MetaProperty property, boolean eatItUp)
    {
        List<Element> metas = source.getAllElements("meta");
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
                    output.remove(element);
                }
            }
        }
    }
    private String getMetaValue(Source source, OutputDocument output, MetaProperty property, boolean eatItUp)
    {
        String retVal = null;

        List<Element> metas = source.getAllElements("meta");
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
    public static PermissionRole getResourceRoleScope(Element resource)
    {
        PermissionRole retVal = PermissionsConfigurator.ROLE_GUEST;

        Attribute scope = resource.getAttributes().get(RESOURCE_ROLE_SCOPE_ATTRIBUTE);
        if (scope!=null && !StringUtils.isEmpty(scope.getValue())) {
            retVal = R.configuration().getSecurityConfig().lookupPermissionRole(scope.getValue());
        }

        //possible that the above function re-fills it with null
        if (retVal==null) {
            retVal = PermissionsConfigurator.ROLE_GUEST;
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
        if (role==null || role==PermissionsConfigurator.ROLE_GUEST) {
            return true;
        }
        else {
            return SecurityUtils.getSubject().hasRole(role.getRoleName());
        }
    }
    private Iterable<Element> buildRoleScopeResourceIterator(List<Element> elements)
    {
        final Iterator iter = Iterators.filter(elements.iterator(), new Predicate<Element>()
        {
            @Override
            public boolean apply(Element element)
            {
                return testResourceRoleScope(getResourceRoleScope(element));
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
