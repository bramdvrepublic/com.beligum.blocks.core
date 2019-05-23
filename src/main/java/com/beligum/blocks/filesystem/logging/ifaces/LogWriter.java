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

package com.beligum.blocks.filesystem.logging.ifaces;

import com.beligum.base.models.Person;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Created by bram on 6/10/16.
 */
public interface LogWriter extends AutoCloseable
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    void writeLogEntry(Entry logEntry) throws IOException;

    @Override
    void close() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    //-----INNER CLASSES-----
    interface Entry
    {
        Instant getUTCTimestamp();
        com.beligum.base.models.Person getCreator();
    }
    abstract class AbstractEntry implements Entry
    {
        private Instant timestamp;
        private Person creator;

        protected AbstractEntry(Instant timestamp, Person creator)
        {
            //we save all log entries relative to UTC time zone
            this.timestamp = timestamp.atZone(ZoneOffset.UTC).toInstant();
            this.creator = creator;
        }

        public Instant getUTCTimestamp()
        {
            return timestamp;
        }
        public Person getCreator()
        {
            return creator;
        }
    }
}
