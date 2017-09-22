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

package com.beligum.blocks.config;

import com.beligum.base.server.ifaces.SystemPropertyFactory;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bram on 1/5/17.
 */
public class BlocksSystemPropertyFactory implements SystemPropertyFactory
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public Map<String, String> getSystemProperties()
    {
        Map<String, String> retVal = new LinkedHashMap<>();

        Settings settings = Settings.instance();

        //If we're using a Bitronix transaction manager, we must add some extra properties to specify the location of the
        //tx log files and maybe specify the custom timeout value.

        //note this will boot the TX manager very early in the boot process, hope this is ok...
        //No: this doesn't work because the caching subsystem isn't booted when this is called,
        //let's just assume we're using bitronix for now

        //TransactionManager transactionManager = StorageFactory.getTransactionManager();
        //if (transactionManager.getClass().getCanonicalName().contains("bitronix")) {
        URI txRootPath = settings.getPagesRootPath().resolve(Settings.PAGES_DEFAULT_TRANSACTIONS_FOLDER + "/");
        retVal.put("bitronix.tm.journal.disk.logPart1Filename", txRootPath.resolve("btm1.tlog").getPath());
        retVal.put("bitronix.tm.journal.disk.logPart2Filename", txRootPath.resolve("btm2.tlog").getPath());

        //Note that bitronix transaction timeout is in seconds, not millis
        retVal.put("bitronix.tm.timer.defaultTransactionTimeout", String.valueOf((int) (settings.getTransactionTimeoutMillis() / 1000.0)));
        //}

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
