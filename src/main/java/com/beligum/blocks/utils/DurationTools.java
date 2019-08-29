package com.beligum.blocks.utils;

import java.time.Duration;

/**
 * Note that the following method have been added to JDK9, but I've added them here for older installs
 *
 * Created by bram on Aug 29, 2019
 */
public class DurationTools
{
    //-----CONSTANTS-----
    /**
     * Hours per day.
     */
    static final int HOURS_PER_DAY = 24;
    /**
     * Minutes per hour.
     */
    static final int MINUTES_PER_HOUR = 60;
    /**
     * Minutes per day.
     */
    static final int MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY;
    /**
     * Seconds per minute.
     */
    static final int SECONDS_PER_MINUTE = 60;
    /**
     * Seconds per hour.
     */
    static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
    /**
     * Seconds per day.
     */
    static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
    /**
     * Milliseconds per day.
     */
    static final long MILLIS_PER_DAY = SECONDS_PER_DAY * 1000L;
    /**
     * Microseconds per day.
     */
    static final long MICROS_PER_DAY = SECONDS_PER_DAY * 1000_000L;
    /**
     * Nanos per second.
     */
    static final long NANOS_PER_SECOND = 1000_000_000L;
    /**
     * Nanos per minute.
     */
    static final long NANOS_PER_MINUTE = NANOS_PER_SECOND * SECONDS_PER_MINUTE;
    /**
     * Nanos per hour.
     */
    static final long NANOS_PER_HOUR = NANOS_PER_MINUTE * MINUTES_PER_HOUR;
    /**
     * Nanos per day.
     */
    static final long NANOS_PER_DAY = NANOS_PER_HOUR * HOURS_PER_DAY;

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Extracts the number of days in the duration.
     * <p>
     * This returns the total number of days in the duration by dividing the
     * number of seconds by 86400.
     * This is based on the standard definition of a day as 24 hours.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return the number of days in the duration, may be negative
     */
    public static long toDaysPart(Duration duration)
    {
        return duration.getSeconds() / SECONDS_PER_DAY;
    }

    /**
     * Extracts the number of hours part in the duration.
     * <p>
     * This returns the number of remaining hours when dividing {@link #toHours}
     * by hours in a day.
     * This is based on the standard definition of a day as 24 hours.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return the number of hours part in the duration, may be negative
     */
    public static int toHoursPart(Duration duration)
    {
        return (int) (duration.toHours() % 24);
    }

    /**
     * Extracts the number of minutes part in the duration.
     * <p>
     * This returns the number of remaining minutes when dividing {@link #toMinutes}
     * by minutes in an hour.
     * This is based on the standard definition of an hour as 60 minutes.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return the number of minutes parts in the duration, may be negative
     * may be negative
     */
    public static int toMinutesPart(Duration duration)
    {
        return (int) (duration.toMinutes() % MINUTES_PER_HOUR);
    }

    /**
     * Extracts the number of seconds part in the duration.
     * <p>
     * This returns the remaining seconds when dividing {@link #toSeconds}
     * by seconds in a minute.
     * This is based on the standard definition of a minute as 60 seconds.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return the number of seconds parts in the duration, may be negative
     */
    public static int toSecondsPart(Duration duration)
    {
        return (int) (duration.getSeconds() % SECONDS_PER_MINUTE);
    }

    /**
     * Extracts the number of milliseconds part of the duration.
     * <p>
     * This returns the milliseconds part by dividing the number of nanoseconds by 1,000,000.
     * The length of the duration is stored using two fields - seconds and nanoseconds.
     * The nanoseconds part is a value from 0 to 999,999,999 that is an adjustment to
     * the length in seconds.
     * The total duration is defined by calling {@link #Duration.getNano()} and {@link #getSeconds()}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return the number of milliseconds part of the duration.
     */
    public static int toMillisPart(Duration duration)
    {
        return duration.getNano() / 1000_000;
    }

    /**
     * Get the nanoseconds part within seconds of the duration.
     * <p>
     * The length of the duration is stored using two fields - seconds and nanoseconds.
     * The nanoseconds part is a value from 0 to 999,999,999 that is an adjustment to
     * the length in seconds.
     * The total duration is defined by calling {@link #getNano()} and {@link #getSeconds()}.
     * <p>
     * This instance is immutable and unaffected by this method call.
     *
     * @return the nanoseconds within the second part of the length of the duration, from 0 to 999,999,999
     */
    public static int toNanosPart(Duration duration)
    {
        return duration.getNano();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
}
