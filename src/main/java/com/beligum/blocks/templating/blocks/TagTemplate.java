package com.beligum.blocks.templating.blocks;

import com.google.common.base.CaseFormat;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Source;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by bram on 5/13/15.
 */
public class TagTemplate
{
    //-----CONSTANTS-----
    /**
     * These are the names of first folders that won't be taken into account when building the name of the element
     * Eg. /imports/blocks/test/tag.html will have the name "blocks-test-tag"
     */
    private String[] INVISIBLE_START_FOLDERS = { "import", "imports" };
    private static final Pattern styleLinkRelAttrValue = Pattern.compile("stylesheet");

    //-----VARIABLES-----
    private Source source;
    private Element templateElement;
    private String templateContent;
    private Path absolutePath;
    private Path relativePath;
    private String name;
    private String velocityName;
    private String controllerMapVariable;
    private Map<Locale, String> titles;
    private Map<Locale, String> descriptions;
    private Class<TagTemplateController> controllerClass;
    private List<Element> inlineScriptElements;
    private List<Element> externalScriptElements;
    private List<Element> inlineStyleElements;
    private List<Element> externalStyleElements;

    //-----CONSTRUCTORS-----
    public TagTemplate(Source source, Path absolutePath, Path relativePath) throws Exception
    {
        this.source = source;
        this.absolutePath = absolutePath;
        this.relativePath = relativePath;

        List<Element> templateElements = this.source.getAllElements("template");
        if (templateElements != null && !templateElements.isEmpty() && templateElements.size() == 1) {
            this.templateElement = templateElements.get(0);
        }
        else {
            throw new Exception("Encountered tag template with an invalid <template> tag config (found " + (templateElements == null ? null : templateElements.size()) + " tags); " + this.absolutePath);
        }

        //we might as well do this here to save time
        this.templateContent = HtmlParser.eatVelocityComments(this.templateElement.getContent().toString());

        Path namePath = this.relativePath;
        if (this.relativePath != null) {
            for (String invisiblePrefix : INVISIBLE_START_FOLDERS) {
                if (namePath.startsWith(invisiblePrefix) || namePath.startsWith(namePath.getFileSystem().getSeparator() + invisiblePrefix)) {
                    namePath = namePath.subpath(1, namePath.getNameCount());
                    //this is a safe choice that might change in the future: do we want to keep eating first folders? Of so, then we should actually created over, no?
                    break;
                }
            }
            this.name = StringUtils.strip(namePath.toString().replaceAll("/", "-"), "-");
            int lastDot = this.name.lastIndexOf(".");
            if (lastDot >= 0) {
                this.name = this.name.substring(0, lastDot);
            }
            //note: we may want to let the user override the name with an id attribute on the <template> tag

            // In Web Components speak, this new element is a Custom Element,
            // and the only two requirements are that its name must contain a dash,
            // and its prototype must extend HTMLElement.
            // See https://css-tricks.com/modular-future-web-components/
            if (!this.name.contains("-")) {
                throw new ParseException("The name of an import template should always contain at least one dash; '" + this.name + "' in " + relativePath, 0);
            }

            this.velocityName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, this.name);
            this.controllerMapVariable = new StringBuilder().append("$").append(TagTemplateContextMap.TAG_TEMPLATE_CONTROLLERS_VARIABLE).append("['").append(this.getTemplateName()).append("']").toString();
        }
        else {
            this.name = null;
            this.velocityName = null;
            this.controllerMapVariable = null;
        }

        this.fillMetaValues(this.source, this.titles = new HashMap<>(), "title");
        this.fillMetaValues(this.source, this.descriptions = new HashMap<>(), "description");
        String controllerClassStr = this.getMetaValue(this.source, "controller");
        if (!StringUtils.isEmpty(controllerClassStr)) {
            Class<?> clazz = Class.forName(controllerClassStr);
            if (TagTemplateController.class.isAssignableFrom(clazz)) {
                this.controllerClass = (Class<TagTemplateController>) clazz;
            }
            else {
                throw new ParseException("Encountered template with a controller that doesn't implement "+TagTemplateController.class.getSimpleName()+"; "+relativePath, 0);
            }
        }

        this.inlineStyleElements = getInlineStyles(this.source);
        this.externalStyleElements = getExternalStyles(this.source);

        this.inlineScriptElements = getInlineScripts(this.source);
        this.externalScriptElements = getExternalScripts(this.source);
    }

    //-----PUBLIC METHODS-----
    public static boolean representsHtmlImportTemplate(Source source)
    {
        return !source.getAllElements("template").isEmpty();
    }
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
        return this.name;
    }
    public String getVelocityTemplateName()
    {
        return velocityName;
    }
    public String getControllerMapVariable()
    {
        return controllerMapVariable;
    }
    public Element getTemplateElement()
    {
        return templateElement;
    }
    public String getTemplateContent()
    {
        return templateContent;
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

    //-----PRIVATE METHODS-----
    private void fillMetaValues(Source source, Map<Locale, String> target, String property)
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
            }
        }
    }
    private String getMetaValue(Source source, String property)
    {
        String retVal = null;

        List<Element> metas = source.getAllElements("meta");
        Iterator<Element> iter = metas.iterator();
        while (retVal==null && iter.hasNext()) {
            Element element = iter.next();
            String propertyVal = element.getAttributeValue("property");
            if (propertyVal!=null && propertyVal.equalsIgnoreCase(property)) {
                retVal = element.getAttributeValue("content");
            }
        }

        return retVal;
    }
}
