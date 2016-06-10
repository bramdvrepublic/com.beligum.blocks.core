package com.beligum.blocks.fs.logger;

import com.beligum.base.auth.models.Person;
import com.beligum.blocks.fs.logger.ifaces.LogWriter;
import com.beligum.blocks.fs.pages.ifaces.Page;

import java.time.ZonedDateTime;


/**
 * Created by bram on 6/10/16.
 */
public class PageLogEntry extends LogWriter.AbstractEntry
{
    //-----CONSTANTS-----
    public enum Action
    {
        CREATE,
        UPDATE,
        DELETE
    }

    //-----VARIABLES-----
    private Page page;
    private Action action;

    //-----CONSTRUCTORS-----
    public PageLogEntry(ZonedDateTime timestamp, Person creator, Page page, Action action)
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
