package com.beligum.blocks.models.factory;

import com.beligum.blocks.exceptions.ParseException;
import com.beligum.blocks.models.*;
import com.beligum.blocks.renderer.BlocksTemplateRenderer;
import org.jsoup.nodes.Element;

import java.util.Locale;

/**
* Created by wouter on 26/03/15.
*/
public interface BlocksFactory
{
    public StoredTemplate createStoredTemplate(Element element, Locale language) throws ParseException;
    public StoredTemplate createStoredTemplate(Blueprint blueprint, Locale language) throws ParseException;
    public Blueprint createBlueprint(Element element, Locale language) throws ParseException;
    public PageTemplate createPageTemplate(Element element, Locale language)  throws ParseException;

    public Singleton createSingleton(Element element, Locale language)  throws ParseException;

    public BlocksTemplateRenderer createTemplateRenderer();

}
