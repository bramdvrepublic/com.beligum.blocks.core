package com.beligum.blocks.templating.blocks;

import com.google.common.base.CaseFormat;
import net.htmlparser.jericho.*;
import org.apache.commons.lang3.StringUtils;

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
    public static final String BLOCKS_META_TAG_PREFIX = "blocks:";
    /**
     * These are the names of first folders that won't be taken into account when building the name of the element
     * Eg. /imports/blocks/test/tag.html will have the name "blocks-test-tag"
     */
    protected String[] INVISIBLE_START_FOLDERS = { "import", "imports" };
    protected static final Pattern styleLinkRelAttrValue = Pattern.compile("stylesheet");

    public enum MetaDisplayType {
        DEFAULT,
        HIDDEN
    }

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
        if (html!=null && html.size()==1) {
            Attributes htmlAttr = html.get(0).getAttributes();
            if (htmlAttr.get("template")!=null) {
                retVal = true;
            }
        }

        return retVal;
    }

    /**
     * Controls if we need to wrap the instance with the <template-name></template-name> tag or not (eg. for page templates, we don't want this)
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
            if (iter.next().getAttributeValue("src")!=null) {
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
            if (iter.next().getAttributeValue("src")==null) {
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
    public List<Element> getInlineScriptElements()
    {
        return inlineScriptElements;
    }
    public List<Element> getExternalScriptElements()
    {
        return externalScriptElements;
    }
    public List<Element> getInlineStyleElements()
    {
        return inlineStyleElements;
    }
    public List<Element> getExternalStyleElements()
    {
        return externalStyleElements;
    }
    public Map<Locale, String> getTitles()
    {
        return titles;
    }
    public Map<Locale, String> getDescriptions()
    {
        return descriptions;
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
        this.fillMetaValues(this.document, html, this.titles = new HashMap<>(), BLOCKS_META_TAG_PREFIX+"title", true);
        this.fillMetaValues(this.document, html, this.descriptions = new HashMap<>(), BLOCKS_META_TAG_PREFIX+"description", true);
        String controllerClassStr = this.getMetaValue(this.document, html, BLOCKS_META_TAG_PREFIX+"controller", true);
        if (!StringUtils.isEmpty(controllerClassStr)) {
            Class<?> clazz = Class.forName(controllerClassStr);
            if (TemplateController.class.isAssignableFrom(clazz)) {
                this.controllerClass = (Class<TemplateController>) clazz;
            }
            else {
                throw new ParseException("Encountered template with a controller that doesn't implement "+TemplateController.class.getSimpleName()+"; "+relativePath, 0);
            }
        }

        this.displayType = MetaDisplayType.DEFAULT;
        String displayType = this.getMetaValue(this.document, html, "display", true);
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
    private void fillMetaValues(Source source, OutputDocument output, Map<Locale, String> target, String property, boolean eatItUp)
    {
        List<Element> metas = source.getAllElements("meta");
        Iterator<Element> iter = metas.iterator();
        while (iter.hasNext()) {
            Element element = iter.next();
            String propertyVal = element.getAttributeValue("property");
            if (propertyVal!=null && propertyVal.equalsIgnoreCase(property)) {
                Locale locale = Locale.ROOT;
                String localeStr = element.getAttributeValue("lang");
                if (localeStr!=null) {
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
    private String getMetaValue(Source source, OutputDocument output, String property, boolean eatItUp)
    {
        String retVal = null;

        List<Element> metas = source.getAllElements("meta");
        Iterator<Element> iter = metas.iterator();
        while (retVal==null && iter.hasNext()) {
            Element element = iter.next();
            String propertyVal = element.getAttributeValue("property");
            if (propertyVal!=null && propertyVal.equalsIgnoreCase(property)) {
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
