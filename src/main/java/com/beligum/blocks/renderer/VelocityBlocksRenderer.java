package com.beligum.blocks.renderer;

import com.beligum.base.i18n.I18nBundle;
import com.beligum.base.i18n.I18nFactory;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.base.templating.ifaces.TemplateContext;
import com.beligum.base.templating.velocity.VelocityTemplateEngine;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.config.ParserConstants;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.*;
import com.beligum.blocks.usermanagement.Permissions;
import com.beligum.blocks.utils.EntityPropertyFinder;
import com.beligum.blocks.utils.PropertyFinder;
import org.apache.shiro.SecurityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;

/**
 * Created by wouter on 8/04/15.
 */
public class VelocityBlocksRenderer implements BlocksTemplateRenderer
{
    private final Integer START_PROPERTY_LENGTH = ParserConstants.TEMPLATE_PROPERTY_START.length();
    private final Integer END_PROPERTY_LENGTH = ParserConstants.TEMPLATE_PROPERTY_END.length();


    private class Field {
        public Field(String name) {this.name = name;};
        public String name;
        public boolean property = false;
        public boolean list = false;
    }

    private VelocityTemplateEngine velocityTemplateEngine;
    private Locale locale;
    private StringBuilder buffer;
    private boolean useOnlyEntity = false;
    private boolean showResource = true;
    private boolean fetchSingletons = true;
    private boolean fetchEntities = false;
    private boolean renderDynamicBlocks = true;
    private boolean readOnly = false;
    private LinkedHashSet<String> links = new LinkedHashSet();
    private LinkedHashSet<String> scripts = new LinkedHashSet();


    public void setUseOnlyEntity(boolean useOnlyEntity)
    {
        this.useOnlyEntity = useOnlyEntity;
    }
    public void setShowResource(boolean showResource)
    {
        this.showResource = showResource;
    }
    public void setFetchSingletons(boolean fetchSingletons)
    {
        this.fetchSingletons = fetchSingletons;
    }
    public void setFetchEntities(boolean fetchEntities)
    {
        this.fetchEntities = fetchEntities;
    }
    public void setRenderDynamicBlocks(boolean renderDynamicBlocks)
    {
        this.renderDynamicBlocks = renderDynamicBlocks;
    }
    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    public VelocityBlocksRenderer() {
        this.velocityTemplateEngine = new VelocityTemplateEngine();
    }


    public String render(StoredTemplate storedTemplate, Entity entity, String language) {
        this.locale = new Locale(language);
        this.buffer = new StringBuilder();
        this.renderTemplate(storedTemplate, entity, this.readOnly);
        return this.buffer.toString();
    }


    public String render(PageTemplate pageTemplate, StoredTemplate storedTemplate, Entity entity, String language) {
        this.locale = new Locale(language);
        this.buffer = new StringBuilder();

        // Render everything in the page except main content
        this.scripts.addAll(pageTemplate.getScripts());
        this.links.addAll(pageTemplate.getLinks());

        this.fillTemplateWithProperties(pageTemplate.getValue(), this.readOnly, pageTemplate, pageTemplate, null);
        StringBuilder page = this.buffer;

        // Render main content
        this.buffer = new StringBuilder();
        this.showResource = false;
        this.renderTemplate(storedTemplate, entity, this.readOnly);

        page = replace(page, ParserConstants.TEMPLATE_CONTENT, this.buffer);


        // Add all links and scripts
        StringBuilder scriptsAndLinks = new StringBuilder();
        // Append Blocks client only if logged in
        if (SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
            for (String link : Blocks.templateCache().getBlocksLinks()) {
                scriptsAndLinks.append(link).append(System.lineSeparator());
            }
        }

        for (String link: this.links) {
            scriptsAndLinks.append(link).append(System.lineSeparator());
        }

        // Append Blocks client only if logged in
        if (SecurityUtils.getSubject().isPermitted(Permissions.ENTITY_MODIFY)) {
            for (String script : Blocks.templateCache().getBlocksScripts()) {
                scriptsAndLinks.append(script).append(System.lineSeparator());
            }
        }

        for (String script: this.scripts) {
            scriptsAndLinks.append(script).append(System.lineSeparator());
        }

        page = replace(page, ParserConstants.TEMPLATE_HEAD, scriptsAndLinks);

        this.buffer = new StringBuilder();

        return page.toString();
    }

    public StringBuilder replace(StringBuilder origin, String property, StringBuilder value) {
        StringBuilder retVal = new StringBuilder();
        int start = origin.indexOf(property);
        if (start > -1) {
            retVal.append(origin.substring(0, start));
            retVal.append(value);
            retVal.append(origin.substring(start+property.length()));
        }
        return retVal;
    }

    public void renderTemplate(BasicTemplate template, EntityField entity, boolean readOnly)
    {
        if (renderDynamicBlocks && Blocks.blockHandler().isDynamicBlock(template.getBlueprintName())) {
            this.buffer.append(Blocks.blockHandler().getDynamicBlock(template.getBlueprintName()).render(template));
            return;
        }

        this.buffer.append(template.renderStartElement(readOnly, showResource));
        if (!showResource) showResource = true;

        String stringTemplate = template.getValue();
        Blueprint blueprint = template.getBlueprint();

        if (blueprint == null) {
            // This is a basic block/property
            if (entity != null && !(entity instanceof Entity)) {
                this.buffer.append(entity.getValue());
            } else if (!useOnlyEntity) {
                this.buffer.append(stringTemplate);
            }

        } else {
            // Add links and scripts
            this.links.addAll(blueprint.getLinks());
            this.scripts.addAll(blueprint.getScripts());

            // If blueprint is fixed or readonly then use template of blueprint to fill with values
            if (blueprint.isFixed() || readOnly) {
//                Template velocityStringTemplate = this.velocityTemplateEngine.getEmptyStringTemplate(blueprint.getTemplate());
//                velocityStringTemplate.set(TemplateContext.InternalProperties.I18N.name(), I18nFactory.instance().getResourceBundle(this.locale));
                stringTemplate = blueprint.getTemplate();
            }

            if (readOnly) {
                if (blueprint.getProperties().size() > 0) {
                    this.fillTemplateWithProperties(stringTemplate, true, blueprint, template, entity);
                } else {
                    this.buffer.append(stringTemplate);
                }
            } else if (blueprint.getProperties().size() > 0 || template.getProperties().size() > 0) {
                this.fillTemplateWithProperties(stringTemplate, readOnly, blueprint, template, entity);
            } else {
                this.buffer.append(stringTemplate);
            }

        }
        this.buffer.append(template.renderEndElement());
    }

    protected void fillTemplateWithProperties(String templateToRender, boolean readOnly, Blueprint blueprint, BasicTemplate template, EntityField entity)
    {
        // find property
        ArrayList<Field> fields = findNextPropertyInTemplate(templateToRender);
        PropertyFinder<BasicTemplate> propertyFinder = new PropertyFinder();
        EntityPropertyFinder entityPropertyFinder = new EntityPropertyFinder();

        HashMap<String, Object> entityProperties = new HashMap<String, Object>();
        if (entity != null && entity instanceof Entity) entityProperties = ((Entity)entity).getProperties();

        for (Field nextProperty: fields) {
            // this is not a property but a piece of the template, so add and forward
            if (!nextProperty.property) {
                this.buffer.append(nextProperty.name);
                continue;
            }

            BasicTemplate property = propertyFinder.getProperty(nextProperty.name, template.getProperties());
            BasicTemplate blueprintProperty = propertyFinder.getProperty(nextProperty.name, blueprint.getProperties());
            EntityField entityProperty = entityPropertyFinder.getProperty(nextProperty.name, entityProperties, locale.getLanguage());
            propertyFinder.propertyFound(nextProperty.name);
            entityPropertyFinder.propertyFound(nextProperty.name);


            if (property == null && blueprintProperty != null) {
                property = blueprintProperty;
            } else if (property != null && property instanceof Singleton && this.fetchSingletons) {
                BlockId singletonId = ((Singleton)property).getId();
                StoredTemplate singleton = Blocks.database().fetch(singletonId, this.locale.getLanguage(), Blocks.factory().getSingletonClass());
                if (singleton != null) property = singleton;
            }


            if (property != null) {
                boolean propertyReadOnly = readOnly;
                if (!readOnly) {
                    if (blueprintProperty != null && blueprint.isReadOnly() || property.isReadOnly()) {
                        propertyReadOnly = true;
                    }
                }
                this.renderTemplate(property, entityProperty, propertyReadOnly);

            } else {
                // Do nothing
            }

        }
    }


    protected ArrayList<Field> findNextPropertyInTemplate(String template) {
        ArrayList<Field> retVal = new ArrayList<Field>();

        int templateLength = template.length();
        int oldStart = 0;
        int start = template.indexOf(ParserConstants.TEMPLATE_PROPERTY_START, oldStart);
        while (start > -1) {
            if (start != oldStart) {
                retVal.add(new Field(template.substring(oldStart, start)));
            }
            int end = template.indexOf(ParserConstants.TEMPLATE_PROPERTY_END, start);
            if (end > -1 && end > start) {
                start += START_PROPERTY_LENGTH;
                Field field = new Field(template.substring(start, end));
                field.property = true;
                retVal.add(field);
                oldStart = end + END_PROPERTY_LENGTH;
            }

            start = template.indexOf(ParserConstants.TEMPLATE_PROPERTY_START, oldStart);
        }
        if (oldStart < templateLength) {
            retVal.add(new Field(template.substring(oldStart, templateLength)));
        }

        return retVal;
    }



    public String getTemplatePropertyName(String property) {
        return "$!"+ getValidName(property);
    }

    private String getValidName(String property) {
        return property.replaceAll("[\\W]", "_");
    }

}
