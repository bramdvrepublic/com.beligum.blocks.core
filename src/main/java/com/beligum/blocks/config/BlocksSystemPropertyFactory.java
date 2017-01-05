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

        //we'll instruct the JVM that our temp dir is located in the tmp folder of our local context dir
        //(and create it if it doesn't exist)
        retVal.put("java.io.tmpdir", settings.getLocalContextSubdir("tmp", true).getPath());

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
        retVal.put("bitronix.tm.timer.defaultTransactionTimeout", String.valueOf(settings.getTransactionTimeoutMillis() / 1000));
        //}

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
