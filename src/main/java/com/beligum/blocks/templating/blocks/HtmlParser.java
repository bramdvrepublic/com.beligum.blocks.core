package com.beligum.blocks.templating.blocks;

import com.beligum.base.resources.Asset;
import com.beligum.base.resources.ResourceDescriptor;
import com.beligum.base.resources.ResourceSearchResult;
import com.beligum.base.resources.parsers.AbstractAssetParser;
import com.beligum.base.resources.parsers.results.ParseResult;
import com.beligum.base.resources.parsers.results.StringParseResult;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.caching.CacheKeys;
import com.beligum.blocks.templating.blocks.directives.TagTemplateExternalScriptDirective;
import com.beligum.blocks.templating.blocks.directives.TagTemplateExternalStyleDirective;
import com.beligum.blocks.templating.blocks.directives.TagTemplateInlineScriptDirective;
import com.beligum.blocks.templating.blocks.directives.TagTemplateInlineStyleDirective;
import net.htmlparser.jericho.*;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by bram on 5/16/15.
 */
public class HtmlParser extends AbstractAssetParser
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private static final Pattern notEmptyPropertyAttrValue = Pattern.compile(".+");

    //-----CONSTRUCTORS-----
    public HtmlParser()
    {
    }

    //-----PUBLIC METHODS-----
    public static String eatVelocityComments(String html) throws IOException
    {
        StringBuilder retVal = new StringBuilder();

        try (BufferedReader stringReader = new BufferedReader(new StringReader(html))) {
            String line;
            while ((line = stringReader.readLine()) != null) {
                int commentIdx = line.indexOf("##");
                if (commentIdx != -1) {
                    line = line.substring(0, commentIdx);
                }
                //if the entire line got eaten, we can discard it?
                if (!StringUtils.isEmpty(line)) {
                    retVal.append(line).append("\n");
                }
            }
        }

        return retVal.toString();
    }
    public static TagTemplateCache getCachedTemplates()
    {
        TagTemplateCache retVal = (TagTemplateCache) R.cacheManager().getApplicationCache().get(CacheKeys.TAG_TEMPLATES);
        if (retVal == null) {
            R.cacheManager().getApplicationCache().put(CacheKeys.TAG_TEMPLATES, retVal = new TagTemplateCache());
            try {
                searchAllTemplates(retVal);
            }
            catch (Exception e) {
                Logger.error("Caught exception while searching for all the webcomponent templates in the current classpath; this is bad and needs to fixed", e);
            }
        }

        return retVal;
    }
    @Override
    public ParseResult parse(ResourceDescriptor src, Map<String, String> args) throws IOException
    {
        try {
            //the solves a lot of issues with inactive lines
            String sourceStr = eatVelocityComments(new String(Files.readAllBytes(src.getResolvedPath())));
            Source source = new Source(sourceStr);

            // if we're dealing with a tag template, we're only interested in the html between the <template> tags
            TagTemplateCache tagTemplateCache = this.getCachedTemplates();
            boolean isTagTemplate = false;
            if (tagTemplateCache.containsKey(src.getResolvedPath())) {
                //fetch the old value for the paths
                TagTemplate oldTemplate = tagTemplateCache.get(src.getResolvedPath());

                //since this method only gets called when the source file was changed, make sure we update the value in the template tag cache
                TagTemplate tagTemplate = new TagTemplate(source, oldTemplate.getAbsolutePath(), oldTemplate.getRelativePath());
                tagTemplateCache.put(src.getResolvedPath(), tagTemplate);

                StringBuilder builder = new StringBuilder();

                //make a "proxy" from the (lazy loaded) controlle map to the $controller variable
                builder.append("#set($controller=$").append(TagTemplateContextMap.TAG_TEMPLATE_CONTROLLERS_VARIABLE).append("['").append(tagTemplate.getTemplateName()).append("'])").append("\n");

                //this blocks the resources include from being evaluated repeatedly in loops; we only need to evaluate it once per call
                String resourceTestVar = tagTemplate.getVelocityTemplateName()+"Res";
                builder.append("#if(!$").append(resourceTestVar).append(")").append("\n");

                // note that the "false" argument in the directives means it must be eaten (and spit out somewhere else)
                for (Element style : tagTemplate.getInlineStyleElements()) {
                    builder.append("#").append(TagTemplateInlineStyleDirective.NAME).append("(false)").append(style.toString()).append("#end").append("\n");
                }
                for (Element style : tagTemplate.getExternalStyleElements()) {
                    builder.append("#").append(TagTemplateExternalStyleDirective.NAME).append("(false,'").append(style.getAttributeValue("href")).append("')").append(style.toString()).append("#end").append("\n");
                }
                for (Element script : tagTemplate.getInlineScriptElements()) {
                    builder.append("#").append(TagTemplateInlineScriptDirective.NAME).append("(false)").append(script.toString()).append("#end").append("\n");
                }
                for (Element script : tagTemplate.getExternalScriptElements()) {
                    builder.append("#").append(TagTemplateExternalScriptDirective.NAME).append("(false,'").append(script.getAttributeValue("src")).append("')").append(script.toString()).append("#end").append("\n");
                }

                //block this piece from evaluating again in loops
                builder.append(" #set($").append(resourceTestVar).append("=true)").append("\n");
                builder.append("#end").append("\n");

                //this is always there and is exactly one; see the TagTemplate constructor
                builder.append(source.getAllElements("template").get(0).getContent());

                builder.append("#set($controller=false)").append("\n");

                source = new Source(builder.toString());
                isTagTemplate = true;
            }

            OutputDocument output = new OutputDocument(source);
            for (TagTemplate tagTemplate : tagTemplateCache.values()) {
                List<net.htmlparser.jericho.Element> tagInstances = source.getAllElements(tagTemplate.getTemplateName());
                for (net.htmlparser.jericho.Element tagInstance : tagInstances) {

                    //build the attributes map
                    Map<String, String> attributes = new HashMap<>();
                    tagInstance.getAttributes().populateMap(attributes, false);

                    //copy in all the attributes of the template to the instance map, except the ones that were already set in the instance
                    Attributes templateAttributes = tagTemplate.getTemplateElement().getAttributes();
                    for (Attribute attribute : templateAttributes) {
                        if (!attributes.containsKey(attribute.getName())) {
                            attributes.put(attribute.getName(), attribute.getValue());
                        }
                    }

                    //build the properties map
                    Map<String, String> properties = new HashMap<>();
                    List<Element> allPropertyElements = tagInstance.getAllElements("property", notEmptyPropertyAttrValue);
                    for (Element property : allPropertyElements) {
                        String name = property.getAttributeValue("property");
                        String value = property.toString();
                        if (!properties.containsKey(name)) {
                            properties.put(name, value);
                        }
                    }

                    //now start building the new tag
                    StringBuilder builder = new StringBuilder();

                    //the start tag
                    builder.append("<").append(tagTemplate.getTemplateName());

                    //the optional attributes
                    if (!attributes.isEmpty()) {
                        builder.append(Attributes.generateHTML(attributes));
                    }
                    //close the start tag
                    builder.append(">").append("\n");

                    //the 'body'
                    for (Map.Entry<String, String> property : properties.entrySet()) {
                        builder.append("#define( $").append(property.getKey()).append(" )").append(property.getValue()).append("#end").append("\n");
                    }
                    // embedding the code chunk in the file (instead of linking to it) is a lot faster, but messes up the auto-cache-reload in dev mode
                    if (R.configuration().getProduction()) {
                        //by passing along the resourceloader (and enabling postprocessing and using the parsed path), we implement recursion
                        builder.append(new String(Files.readAllBytes(R.resourceLoader().getResource(tagTemplate.getRelativePath().toString(), true).getParsedPath())));
                    }
                    else {
                        //use a classic parse to parse the defines from above
                        builder.append("#parse('").append(tagTemplate.getRelativePath()).append("')").append("\n");
                    }

                    //the end tag
                    builder.append(tagInstance.getEndTag());

                    //replace the tag in the output with it's rendered tag
                    output.replace(tagInstance, builder);
                }
            }

            // if we're not dealing with a tag template, make sure we wrap all styles and scripts so we can perform a match-and-mix
            // note that the "true" argument in the directives means it really needs to end up in the html (after registering it)
            if (!isTagTemplate) {
                List<Element> elements = TagTemplate.getInlineStyles(source);
                for (Element element : elements) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("#").append(TagTemplateInlineStyleDirective.NAME).append("(true)").append(element.toString()).append("#end");
                    output.replace(element, builder.toString());
                }

                elements = TagTemplate.getExternalStyles(source);
                for (Element element : elements) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("#").append(TagTemplateExternalStyleDirective.NAME).append("(true,'").append(element.getAttributeValue("href")).append("')").append(element.toString()).append("#end");
                    output.replace(element, builder.toString());
                }

                elements = TagTemplate.getInlineScripts(source);
                for (Element element : elements) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("#").append(TagTemplateInlineScriptDirective.NAME).append("(true)").append(element.toString()).append("#end");
                    output.replace(element, builder.toString());
                }

                elements = TagTemplate.getExternalScripts(source);
                for (Element element : elements) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("#").append(TagTemplateExternalScriptDirective.NAME).append("(true,'").append(element.getAttributeValue("src")).append("')").append(element.toString()).append("#end");
                    output.replace(element, builder.toString());
                }
            }

            return new StringParseResult(src.getResolvedPath(), output.toString(), this.getCacheFile(src.getResolvedPath()), Asset.MimeType.HTML);
        }
        catch (Exception e) {
            throw new IOException("Caught exception while parsing html file", e);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private static void searchAllTemplates(TagTemplateCache templateCache) throws Exception
    {
        //start with a clean slate
        templateCache.clear();

        //TODO clean this up
        List<ResourceSearchResult> htmlFiles = new ArrayList<>();
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/templates/**.{html,htm}"));
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/views/**.{html,htm}"));
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/assets/imports/**.{html,htm}"));
        htmlFiles.addAll(R.resourceLoader().searchResourceGlob("/imports/**.{html,htm}"));

        for (ResourceSearchResult htmlFile : htmlFiles) {
            // Note: this code re-occurs in JsoupHtmlImportTemplate.checkReload(), so check there if you modify important things!
            try (Reader reader = Files.newBufferedReader(htmlFile.getResource(), Charset.forName(Charsets.UTF_8.name()))) {
                Path relativePath = Paths.get("/").resolve(htmlFile.getResourceFolder().relativize(htmlFile.getResource()));
                Path absolutePath = htmlFile.getResource();

                Source source = new Source(reader);
                if (TagTemplate.representsHtmlImportTemplate(source)) {
                    TagTemplate template = new TagTemplate(source, absolutePath, relativePath);
                    templateCache.put(absolutePath, template);
                }
            }
            catch (Exception e) {
                Logger.error("Exception caught while parsing a possible tag template file; "+htmlFile, e);
            }
        }
    }

    //-----PRIVATE CLASSES-----
}
