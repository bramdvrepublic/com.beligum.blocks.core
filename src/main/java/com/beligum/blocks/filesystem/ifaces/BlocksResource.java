package com.beligum.blocks.filesystem.ifaces;

import com.beligum.base.resources.ifaces.Hash;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.blocks.filesystem.LockFile;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * A resource according to our HDFS blocks file system paradigm where each file has:
 * - a hidden dot folder counterpart with extra data about the resource like:
 * - history tracking
 * - action tracking in a log file
 * - tracking of changes in a history folder
 * - a distribute-safe file-locking strategy
 * - etc
 *
 * Created by bram on 9/17/15.
 */
public interface BlocksResource extends HdfsResource
{
    //-----INTERFACES-----

    //-----CONSTANTS-----
    String META_FOLDER_PREFIX = ".";
    String META_SUBFOLDER_PROXY = "proxy";
    String META_SUBFOLDER_METADATA = "meta";
    String META_METADATA_FILE_METADATA_XML = "metadata.xml";
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
    //when the meta folder is versioned, it is copied to a temp location (in the same folder als the original meta dot folder); this is it's suffix
    String TEMP_SNAPSHOT_SUFFIX = ".snapshot";
    //sync with eg. HDFS meta files (eg. hidden .crc files) (and also less chance on conflicts)
    String LOCK_FILE_PREFIX = ".";
    long DEFAULT_LOCK_BACK_OFF = 100;
    long DEFAULT_LOCK_TIMEOUT = 5000;
    //let's (possibly stale) locks only live for maximum one hour since that should be enough time
    //to do what we want to do.
    long DEFAULT_LOCK_MAX_AGE = 1000 * 60 * 60;

    //interesting: http://blog.gmane.org/gmane.comp.java.joda-time.user/month=20091101
    ZoneId FOLDER_TIMESTAMP_TIMEZONE = ZoneOffset.UTC;
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

    //-----PUBLIC METHODS-----
    /**
     * The path to the base meta dot (same name as original, prefixed with a dot) folder of this resource
     */
    Path getDotFolder();

    /**
     * The path to the hash file of this resource
     */
    Path getHashFile();

    /**
     * The path to the log file of this resource
     */
    Path getLogFile();

    /**
     * The path to the (stored, cached) mime type of this resource
     */
    Path getMimeFile();

    /**
     * The path to the history base folder of this resource
     */
    Path getHistoryFolder();

    /**
     * The path to the monitor base folder of this resource
     */
    Path getMonitorFolder();

    /**
     * The path to the base proxy folder of this resource
     */
    Path getProxyFolder();

    /**
     * The path to the proxy folder of this resource for the specified mime type
     */
    Path getProxyFolder(MimeType mimeType);

    /**
     * The path to the base metadata folder of this resource
     */
    Path getMetadataFolder();

    /**
     * Explicitly re-calculate the hash checksum (instead of using the cached version if it's present)
     */
    Hash calcHash() throws IOException;

    /**
     * Acquire a filesystem lock on this resource or throw an exception if it didn't succeed.
     */
    LockFile acquireLock() throws IOException;

    /**
     * Check if this resource is locked by a lock file.
     */
    boolean isLocked() throws IOException;

    /**
     * Release (delete) the lock file on this resource or throw an exception if no such lock exists or if the release was unsuccessful
     */
    void releaseLockFile(LockFile lock) throws IOException;

    /**
     * @return true if this resource supports writing to it or false (default) if not
     */
    boolean isReadOnly();

}
