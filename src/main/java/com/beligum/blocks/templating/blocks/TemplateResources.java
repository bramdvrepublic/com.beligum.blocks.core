package com.beligum.blocks.templating.blocks;

import com.google.common.collect.Iterators;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by bram on 5/18/15.
 */
public class TemplateResources
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Set<Resource> printedResources;
    private Set<Resource> styles;
    private Set<Resource> scripts;

    //-----CONSTRUCTORS-----
    public TemplateResources()
    {
        this.styles = new LinkedHashSet<>();
        this.scripts = new LinkedHashSet<>();

        //these doen't need order
        this.printedResources = new HashSet<>();
    }

    //-----PUBLIC METHODS-----
    public Iterable<Resource> getStyles()
    {
        final Iterator iter = styles.iterator();
        return new Iterable<Resource>() {
            @Override
            public Iterator iterator()
            {
                return iter;
            }
        };
    }
    public Iterable<Resource> getInlineStyles()
    {
        final Iterator iter = Iterators.filter(styles.iterator(), InlineStyle.class);
        return new Iterable<Resource>() {
            @Override
            public Iterator iterator()
            {
                return iter;
            }
        };
    }
    public Iterable<Resource> getExternalStyles()
    {
        final Iterator iter = Iterators.filter(styles.iterator(), ExternalStyle.class);
        return new Iterable<Resource>() {
            @Override
            public Iterator<Resource> iterator()
            {
                return iter;
            }
        };
    }
    public Iterable<Resource> getScripts()
    {
        final Iterator iter = scripts.iterator();
        return new Iterable<Resource>() {
            @Override
            public Iterator iterator()
            {
                return iter;
            }
        };
    }
    public Iterable<Resource> getInlineScripts()
    {
        final Iterator iter = Iterators.filter(scripts.iterator(), InlineScript.class);
        return new Iterable<Resource>() {
            @Override
            public Iterator iterator()
            {
                return iter;
            }
        };
    }
    public Iterable<Resource> getExternalScripts()
    {
        final Iterator iter = Iterators.filter(scripts.iterator(), ExternalScript.class);
        return new Iterable<Resource>() {
            @Override
            public Iterator iterator()
            {
                return iter;
            }
        };
    }
    public void addInlineStyle(boolean print, String value)
    {
        this.addResource(print, new InlineStyle(value), this.styles);
    }
    public void addExternalStyle(boolean print, String href, String element)
    {
        this.addResource(print, new ExternalStyle(href, element), this.styles);
    }
    public void addInlineScript(boolean print, String value)
    {
        this.addResource(print, new InlineScript(value), this.scripts);
    }
    public void addExternalScript(boolean print, String src, String element)
    {
        this.addResource(print, new ExternalScript(src, element), this.scripts);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private void addResource(boolean print, Resource res, Set<Resource> set)
    {
        if (print) {
            this.printedResources.add(res);
            // remove it from the set if it's there
            // this means the printed version came after the tempalte version
            set.remove(res);
        }
        else if (!this.printedResources.contains(res)) {
            set.add(res);
        }
    }

    //-----PRIVATE CLASSES-----
    public abstract class Resource
    {
        //this is the value that will be used to compare two resources
        protected String equalsValue;
        //this is the publicly accessible value
        protected String value;

        protected Resource(String value)
        {
            this(value, value);
        }
        protected Resource(String equalsValue, String value)
        {
            this.equalsValue = equalsValue;
            this.value = value;
        }

        public String getValue()
        {
            return this.value;
        }

        @Override
        public String toString()
        {
            return this.getValue();
        }
        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (!(o instanceof Resource))
                return false;

            Resource resource = (Resource) o;

            return !(equalsValue != null ? !equalsValue.equals(resource.equalsValue) : resource.equalsValue != null);

        }
        @Override
        public int hashCode()
        {
            return equalsValue != null ? equalsValue.hashCode() : 0;
        }
    }
    public class InlineStyle extends Resource
    {
        public InlineStyle(String value)
        {
            super(value);
        }
    }
    public class ExternalStyle extends Resource
    {
        public ExternalStyle(String href, String element)
        {
            super(href, element);
        }
    }
    public class InlineScript extends Resource
    {
        public InlineScript(String value)
        {
            super(value);
        }
    }
    public class ExternalScript extends Resource
    {
        public ExternalScript(String src, String element)
        {
            super(src, element);
        }
    }
}
