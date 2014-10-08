package com.beligum.blocks.core.models;

import com.beligum.blocks.core.config.BlocksConfig;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Created by bas on 08.10.14.
 */
public class PageClass extends AbstractPage
{
    //string with the most outer velocity template of this page
    private String outerVelocityTemplate;

    /**
     *
     * @param pageClassName the name-id for this class (f.i. "default" for a pageClass filtered from the file "pages/default/index.html"
     * @param outerVelocityTemplate velocity-string representing the outer most layer of the row- and block-tree in this page
     */
    public PageClass(String pageClassName, String outerVelocityTemplate){
        super(pageClassName);
        this.outerVelocityTemplate = outerVelocityTemplate;
    }


    public String getOuterVelocityTemplate()
    {
        return outerVelocityTemplate;
    }
    public void setOuterVelocityTemplate(String outerVelocityTemplate)
    {
        this.outerVelocityTemplate = outerVelocityTemplate;
    }

    public String getPageClassName(){
        return this.uid;
    }

    /**
     * Method for getting a new randomly determined page-uid (with versioning) for a pageInstance of this pageClass
     * @return a randomly generated page-id of the form "/pages/<pageClassId>/<randomInt>:<version>"
     */
    public String getNewPageID(){
        //TODO BAS: site-identifier should be added to pageclass-ids: f.i. MOT/'pageClassId' should be used instead of only 'pageClassId'
        Random randomGenerator = new Random();
        int positiveNumber = Math.abs(randomGenerator.nextInt());
        long currentTime = System.currentTimeMillis();
        return this.getPageClassName() + "/" + positiveNumber + ":" + currentTime;
    }

    /**
     * returns the path to the html-template of this page-class
     */
    public String getTemplatePath(){
        return getTemplatePath(this.getPageClassName());
    }

    /**
     * returns the path to the html-template of a page-class
     * @param pageClassName name of the page-class
     */
    public static String getTemplatePath(String pageClassName){

        return BlocksConfig.getTemplateFolder() + "/pages/" + pageClassName + "/index.html";
    }
}
