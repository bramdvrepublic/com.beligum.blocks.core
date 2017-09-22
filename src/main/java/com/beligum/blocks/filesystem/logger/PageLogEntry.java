/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beligum.blocks.filesystem.logger;

import com.beligum.base.models.Person;
import com.beligum.blocks.filesystem.logger.ifaces.LogWriter;
import com.beligum.blocks.filesystem.pages.ifaces.Page;

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
