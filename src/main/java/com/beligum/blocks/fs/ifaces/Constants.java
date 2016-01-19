package com.beligum.blocks.fs.ifaces;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Created by bram on 1/19/16.
 */
public interface Constants
{
    String METADATA_FOLDER_PREFIX = ".";

    String METADATA_SUBFOLDER_PROXY = "proxy";
    String METADATA_SUBFOLDER_META = "meta";

    String METADATA_SUBFOLDER_HISTORY = "HISTORY";
    String METADATA_SUBFOLDER_MONITOR = "MONITOR";
    String METADATA_SUBFILE_HASH = "HASH";
    //String METADATA_SUBFILE_LOCK = "LOCK";
    //String METADATA_SUBFILE_ERROR = "ERROR";
    String METADATA_MONITOR_FILE_PROGRESS = "PROGRESS";
    String METADATA_MONITOR_FILE_LOG = "LOG";
    String METADATA_MONITOR_FILE_ERROR = "ERROR";

    //see http://stackoverflow.com/questions/27399419/giving-current-timestamp-as-folder-name-in-java/27399636#27399636
    // but not using full ISO 8601 because of the colon restriction; instead; we just omit the punctuation
    DateTimeFormatter FOLDER_TIMESTAMP_FORMAT = ISODateTimeFormat.basicDateTime();

    String METADATA_TEMP_META_FOLDER_SNAPSHOT_SUFFIX = ".snapshot";
}
