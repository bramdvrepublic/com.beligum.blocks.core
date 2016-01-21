package com.beligum.blocks.fs.ifaces;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Created by bram on 1/19/16.
 */
public interface Constants
{
    String META_FOLDER_PREFIX = ".";

    String META_SUBFOLDER_PROXY = "proxy";

    String META_SUBFOLDER_METADATA = "meta";
    String META_METADATA_FILE_BASE_XML = "base.xml";
    String META_METADATA_FILE_BASE_XSD = "base.xsd.gz";

    String META_SUBFOLDER_HISTORY = "HISTORY";

    String META_SUBFOLDER_MONITOR = "MONITOR";
    String META_MONITOR_FILE_PROGRESS = "PROGRESS";
    String META_MONITOR_FILE_LOG = "LOG";
    String META_MONITOR_FILE_ERROR = "ERROR";

    String META_SUBFILE_HASH = "HASH";
    //String META_SUBFILE_LOCK = "LOCK";
    //String META_SUBFILE_ERROR = "ERROR";

    //see http://stackoverflow.com/questions/27399419/giving-current-timestamp-as-folder-name-in-java/27399636#27399636
    // but not using full ISO 8601 because of the colon restriction; instead; we just omit the punctuation
    DateTimeFormatter FOLDER_TIMESTAMP_FORMAT = ISODateTimeFormat.basicDateTime();

    //when the meta folder is versioned, it is copied to a temp location (in the same folder als the original meta dot folder); this is it's suffix
    String TEMP_META_FOLDER_SNAPSHOT_SUFFIX = ".snapshot";


    String RDF_RESOURCE_CLASS_PERSON = "person";
}
