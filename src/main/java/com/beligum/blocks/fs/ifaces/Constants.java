package com.beligum.blocks.fs.ifaces;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Created by bram on 1/19/16.
 */
public interface Constants
{
    String META_FOLDER_PREFIX = ".";

    String META_SUBFOLDER_PROXY = "proxy";

    String META_SUBFOLDER_METADATA = "meta";
    String META_METADATA_FILE_METADATA_XML = "metadata.xml";
    //TODO this should be better linked to the source
    String META_METADATA_FILE_METADATA_XSD = "metadata.xsd.gz";

    String META_SUBFOLDER_HISTORY = "HISTORY";

    String META_SUBFOLDER_MONITOR = "MONITOR";
    String META_MONITOR_FILE_PROGRESS = "PROGRESS";
    String META_MONITOR_FILE_LOG = "LOG";
    String META_MONITOR_FILE_ERROR = "ERROR";

    String META_SUBFILE_HASH = "HASH";
    String META_SUBFILE_LOG = "LOG";
    String META_SUBFILE_MIME = "MIME";
    //String META_SUBFILE_LOCK = "LOCK";
    //String META_SUBFILE_ERROR = "ERROR";

    //see http://stackoverflow.com/questions/27399419/giving-current-timestamp-as-folder-name-in-java/27399636#27399636
    // but not using full ISO 8601 because of the colon restriction; instead; we just omit the punctuation
    //DateTimeFormatter FOLDER_TIMESTAMP_FORMAT = ISODateTimeFormat.basicDateTime();
    //DateTimeFormatter FOLDER_TIMESTAMP_FORMAT = java.time.format.DateTimeFormatter.BASIC_ISO_DATE;

    //org.elasticsearch.common.joda.time.format.DateTimeFormatter FOLDER_TIMESTAMP_FORMAT = ISODateTimeFormat.basicDateTime();

    //org.elasticsearch.common.joda.time.format.DateTimeFormatter blah = ISODateTimeFormat.basicDateTime();
    //appendFractionOfSecond(3, 9).appendTimeZoneOffset("Z", false, 2, 2)

    ZoneId FOLDER_TIMESTAMP_TIMEZONE = ZoneOffset.UTC;

    //interesting: http://blog.gmane.org/gmane.comp.java.joda-time.user/month=20091101
    DateTimeFormatter FOLDER_TIMESTAMP_FORMAT = new DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 4)
                                                                              .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                                                                              .appendValue(ChronoField.DAY_OF_MONTH, 2)
                                                                              .appendLiteral('T')
                                                                              .appendValue(ChronoField.HOUR_OF_DAY, 2)
                                                                              .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                                                                              .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                                                                              .appendLiteral('.')
                                                                              .appendValue(ChronoField.MILLI_OF_SECOND, 3)
                                                                              .appendPattern("XX")
                                                                              .toFormatter()
                                                                              //needed to avoid "Unsupported field: Year" exceptions when using Instants
                                                                              .withZone(FOLDER_TIMESTAMP_TIMEZONE);


//    //this should mimic the original ISODateTimeFormat.basicDateTime() the best...
//    DateTimeFormatter FOLDER_TIMESTAMP_FORMAT = new DateTimeFormatterBuilder().appendPattern(DateTimeFormatterBuilder.BASE_PATTERN)
//
//                    new DateTimeFormatterBuilder()
//                    .parseCaseInsensitive()
//                    .appendValue(ChronoField.YEAR, 4)
//                    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
//                    .appendValue(ChronoField.DAY_OF_MONTH, 2)
//                    .appendOffset("+HHMMss", "Z")
//                    .toFormatter();


    //when the meta folder is versioned, it is copied to a temp location (in the same folder als the original meta dot folder); this is it's suffix
    String TEMP_SNAPSHOT_SUFFIX = ".snapshot";

    String RDF_RESOURCE_CLASS_PERSON = "person";
}
