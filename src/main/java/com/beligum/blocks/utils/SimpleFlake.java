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

package com.beligum.blocks.utils;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by wouter on 2/05/15.
 * <p/>
 * copied from: https://github.com/jschwartz73/simpleflake-java/blob/master/src/main/java/com/outjected/simpleflake/SimpleFlake.java
 * <p/>
 * See: http://engineering.custommade.com/simpleflake-distributed-id-generation-for-the-lazy/
 */

public class SimpleFlake
{
    private static final long EPOCH = 1288834974657L;
    private static final long MAX_SIGNED_LONG = 2199023255551L;

    private static final int TIME_SHIFT = 22;
    private static final int RANDOM_SHIFT = 42;

    private long lastTimestamp = 0;
    private Set<Long> recentRandoms = new HashSet<>(5000);

    /**
     * Generates a Twitter Snowflake compatible id utilizing randomness for the right most 22 bits and
     * the Twitter standard EPOCH
     *
     * @return long
     */
    public long generate()
    {

        long currentTimestamp = System.currentTimeMillis();

        while (lastTimestamp > currentTimestamp) {
            // Clock is running backwards so wait until it isn't
            currentTimestamp = System.currentTimeMillis();
        }

        if (currentTimestamp < EPOCH || currentTimestamp > MAX_SIGNED_LONG) {
            // The current time cannot be less than the EPOCH
            throw new RuntimeException("Invalid System Clock was " + new Date(currentTimestamp));
        }

        final long customTimestamp = currentTimestamp - EPOCH;

        final long shiftedTimestamp = customTimestamp << TIME_SHIFT;

        long random = nextRandomPart();

        if (lastTimestamp != currentTimestamp) {
            // timestamp has advanced so reset it and clear the previous cache
            lastTimestamp = currentTimestamp;
            recentRandoms.clear();
        }
        else {
            // Same timestamp as previous keep generating randoms till new is found
            while (recentRandoms.contains(random)) {
                random = nextRandomPart();
            }
        }
        recentRandoms.add(random);
        return shiftedTimestamp | random;
    }

    /**
     * Generates a Twitter Snowflake compatible id utilizing randomness for the right most 22 bits and
     * the Twitter standard EPOCH
     *
     * @return byte[]
     */
    public byte[] generateBytes()
    {
        return ByteBuffer.allocate(8).putLong(generate()).array();
    }

    private long nextRandomPart()
    {
        return ThreadLocalRandom.current().nextLong() >>> RANDOM_SHIFT;
    }
}
