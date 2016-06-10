package com.beligum.blocks.fs.logger.ifaces;

import com.beligum.base.auth.models.Person;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;

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
        Instant getTimestamp();
        com.beligum.base.auth.models.Person getCreator();
    }
    abstract class AbstractEntry implements Entry
    {
        private Instant timestamp;
        private Person creator;

        protected AbstractEntry(ZonedDateTime timestamp, Person creator)
        {
            this.timestamp = timestamp;
            this.creator = creator;
        }

        public Instant getTimestamp()
        {
            return timestamp;
        }
        public Person getCreator()
        {
            return creator;
        }
    }
}
