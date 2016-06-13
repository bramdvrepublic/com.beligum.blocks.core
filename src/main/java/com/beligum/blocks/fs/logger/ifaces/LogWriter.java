package com.beligum.blocks.fs.logger.ifaces;

import com.beligum.base.auth.models.Person;

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
        com.beligum.base.auth.models.Person getCreator();
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
