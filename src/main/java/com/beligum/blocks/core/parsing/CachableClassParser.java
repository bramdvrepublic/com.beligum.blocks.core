package com.beligum.blocks.core.parsing;

import com.beligum.blocks.core.exceptions.ParserException;
import com.beligum.blocks.core.models.ifaces.CachableClass;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by bas on 03.11.14.
 * Interface defining what you need to be able to parse a CachableClass (like f.i. a PageClass or BlockClass)
 */
public abstract class CachableClassParser<T extends CachableClass>
{
    //TODO BAS SH: je hebt net een heleboel refactoring gedaan, om deze klasse te maken en nu de BlockParser te implementeren (misschien moeten er nog methodes gemeenschappelijk worden tussen PageParser en BlockParser en in deze klasse geplaats worden)

    /** the name of the cachable-class currently being parsed */
    protected String cachableClassName;
    /** boolean whether or not this parser is filled with cachable-class-data */
    private boolean filled;

    protected CachableClassParser(){
        this.filled = false;
    }

    public boolean isFilled(){
        return filled;
    }
    public void setFilled(boolean filled)
    {
        this.filled = filled;
    }

    /**
     * Parse the default template-file of the cachable-class and return a CachableClass-object, filled with it's blocks, rows and the template of the cachable-class
     * @param cachableClassName the name of the cachable-class to be parsed (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
     * @return a cachable-class parsed from file system
     * @throws ParserException
     */
    public T parseCachableClass(String cachableClassName) throws ParserException
    {
        try{
            String templateFilename = this.getTemplatePath(cachableClassName);
            File cachableClassTemplate = new File(templateFilename);
            //get the url used for identifying blocks and rows for this cachable-class
            URL cachableClassBaseUrl = this.getBaseUrl(cachableClassName);
            //fill up this parser-class, with the elements and template filtered from the default html-file
            String foundPageClassName = this.fill(cachableClassTemplate, cachableClassBaseUrl);
            if(!foundPageClassName.equals(cachableClassName)){
                throw new ParserException("The name of the cachable-class (" + this.getCssClassPrefix() + cachableClassName + ") does not match the cachable-class-name found in the template: " + this.getCssClassPrefix() + foundPageClassName);
            }
            return this.getInternalCachableClass();
        }
        catch(ParserException e){
            throw e;
        }
        catch(Exception e){
            throw new ParserException("Error while parsing cachable-class '" + this.getCssClassPrefix() + cachableClassName + "' from template.", e);
        }
    }

    /**
     * Parses a html-file, containing a cachable-class, to blocks and rows containing variables and fills this parser up with the found content.
     * After the parse, a string containing the template of this cachable will be saved in the field 'cachableTemplate' and the found blocks and rows will be stored in the fields 'blocks' and 'rows
     * @param cachableClassTemplate the file containing html of a cachable-class
     * @param baseUrl the base-url which will be used to define the row- and block-ids if
     * @return the name of the cachable-class found in the template
     */
    abstract protected String fill(File cachableClassTemplate, URL baseUrl) throws ParserException;


    /**
     * returns the path to the html-template of a cachable-class
     * @param cachableClassName name of the cachable-class
     */
    abstract protected String getTemplatePath(String cachableClassName);

    /**
     * returns the base-url for the cachable-class
     * @param cachableClassName the name of the cachable-class (f.i. "default" for a pageClass filtered from the file "pages/default/index.html")
     * @return
     */
    abstract public URL getBaseUrl(String cachableClassName) throws MalformedURLException;

    /**
     *
     * @return the prefix of the cachable class parsed by this CacableClassParser, used in the class-attribute of the html-template (f.i. "page-" for a PageParser)
     */
    abstract public String getCssClassPrefix();

    /**
     * Returns the cachable-class present in the internal parser-data. The parser needs to be filled, before you can use this method.
     * @return get the cachable-class with which this parser is filled up
     * @throws ParserException if no cachable-class could be constructed out of the internal parser-data
     */
    abstract protected T getInternalCachableClass() throws ParserException;


}
