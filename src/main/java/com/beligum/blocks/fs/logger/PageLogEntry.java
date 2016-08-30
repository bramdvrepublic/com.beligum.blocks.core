package com.beligum.blocks.fs.logger;

import com.beligum.base.auth.models.Person;
import com.beligum.blocks.fs.logger.ifaces.LogWriter;
import com.beligum.blocks.fs.pages.ifaces.Page;

import java.time.Instant;


/**
 * Created by bram on 6/10/16.
 */
public class PageLogEntry extends LogWriter.AbstractEntry
{
    //-----CONSTANTS-----
    public enum Action
    {
        //Note: these are stringified to the persistent RDF stores, so don't change their names without knowing what you do
        CREATE("created"),
        UPDATE("updated"),
        DELETE("deleted")
        ;

        private String verb;
        Action(String verb)
        {
            this.verb = verb;
        }
        public String getVerb()
        {
            return verb;
        }
    }

    //-----VARIABLES-----
    private Page page;
    private Action action;

    //-----CONSTRUCTORS-----
    public PageLogEntry(Instant timestamp, Person creator, Page page, Action action)
    {
        super(timestamp, creator);

        this.page = page;
        this.action = action;
    }

    //-----PUBLIC METHODS-----
    public Page getPage()
    {
        return page;
    }
    public Action getAction()
    {
        return action;
    }
    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}