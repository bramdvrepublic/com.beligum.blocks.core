package com.beligum.blocks.fs;

import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.FileFunctions;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.ifaces.Constants;
import com.beligum.blocks.fs.ifaces.ResourcePath;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Options;
import org.apache.hadoop.fs.Path;
import org.apache.tika.mime.MediaType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.EnumSet;
import java.util.zip.Adler32;

/**
 * Created by bram on 1/19/16.
 */
public class HdfsResourcePath implements ResourcePath
{
    //-----INTERFACES-----

    //-----CONSTANTS-----
    //sync with eg. HDFS meta files (eg. hidden .crc files) (and also less chance on conflicts)
    protected static final String LOCK_FILE_PREFIX = ".";
    protected static final long DEFAULT_LOCK_BACK_OFF = 100;
    protected static final long DEFAULT_LOCK_TIMEOUT = 5000;

    //-----VARIABLES-----
    private URI publicAddress;
    private FileContext fileContext;
    private org.apache.hadoop.fs.Path localPath;
    private org.apache.hadoop.fs.Path lockFile;
    private org.apache.hadoop.fs.Path metaFolder;

    private MediaType cachedMimeType = null;
    private Float cachedProgress = null;
    private float progress;

    //-----CONSTRUCTORS-----
    public HdfsResourcePath(FileContext fileContext, URI localPath) throws IOException
    {
        this(fileContext, new Path(localPath));
    }
    public HdfsResourcePath(FileContext fileContext, Path localPath) throws IOException
    {
        this.fileContext = fileContext;
        this.localPath = localPath;
    }

    //-----PUBLIC METHODS-----
    @Override
    public Path getLocalPath()
    {
        return this.localPath;
    }
    @Override
    public FileContext getFileContext()
    {
        return this.fileContext;
    }
    @Override
    public Path getMetaFolder()
    {
        //only needs to be done once
        if (this.metaFolder == null) {
            this.metaFolder = new Path(this.localPath.getParent(), Constants.META_FOLDER_PREFIX + this.localPath.getName().toString());
        }

        return this.metaFolder;
    }
    @Override
    public Path getMetaHashFile()
    {
        return new Path(this.getMetaFolder(), Constants.META_SUBFILE_HASH);
    }
    @Override
    public Path getMetaLogFile()
    {
        return new Path(this.getMetaFolder(), Constants.META_SUBFILE_LOG);
    }
    @Override
    public Path getMetaMimeFile()
    {
        return new Path(this.getMetaFolder(), Constants.META_SUBFILE_MIME);
    }
    @Override
    public MediaType getMimeType()
    {
        if (this.cachedMimeType == null) {

            Path storedMimeFile = this.getMetaMimeFile();
            boolean newlyDetected = false;
            try {
                if (this.fileContext.util().exists(storedMimeFile)) {
                    String content = HdfsUtils.readFile(this.fileContext, storedMimeFile);
                    if (!StringUtils.isEmpty(content)) {
                        this.cachedMimeType = MediaType.parse(content);
                    }
                }

                if (this.cachedMimeType == null) {
                    String mimeType = null;
                    //Note: the buffered input stream is needed for correct Mime detection !!
                    try (InputStream is = new BufferedInputStream(this.fileContext.open(this.localPath))) {
                        mimeType = FileFunctions.getMimeType(is, this.localPath.getName());
                        newlyDetected = true;
                    }

                    if (!StringUtils.isEmpty(mimeType)) {
                        this.cachedMimeType = MediaType.parse(mimeType);
                    }

                    //we choose to never return null
                    if (this.cachedMimeType == null) {
                        this.cachedMimeType = MediaType.OCTET_STREAM;
                    }
                }
            }
            catch (IOException e) {
                Logger.error("Caught exception while reading the stored mime type file contents of " + this.getLocalPath(), e);
            }
            finally {
                if (this.cachedMimeType != null) {
                    //store the mime type to a cache file for quick future detection
                    if (newlyDetected) {
                        try (OutputStream os = fileContext.create(storedMimeFile, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())) {
                            org.apache.commons.io.IOUtils.write(this.cachedMimeType.toString(), os);
                        }
                        catch (Exception e) {
                            Logger.error("Error while saving mime type '" + this.cachedMimeType + "' to cache file; this shouldn't happen; " + this.getLocalPath(), e);
                        }
                    }
                }
            }
        }

        return this.cachedMimeType;
    }
    @Override
    public Path getMetaHistoryFolder()
    {
        //Note: if you change something here, also check the code where we delete the history folder from the snapshots (in HdfsPageStore)
        return new Path(this.getMetaFolder(), Constants.META_SUBFOLDER_HISTORY);
    }
    @Override
    public Path getMetaMonitorFolder()
    {
        return new Path(this.getMetaFolder(), Constants.META_SUBFOLDER_MONITOR);
    }
    @Override
    public Path getMetaProxyFolder()
    {
        return new Path(this.getMetaFolder(), Constants.META_SUBFOLDER_PROXY);
    }
    @Override
    public Path getMetaProxyFolder(MediaType mimeType)
    {
        return new Path(new Path(this.getMetaProxyFolder(), mimeType.getType()), mimeType.getSubtype());
    }
    @Override
    public Path getMetaMetadataFolder()
    {
        return new Path(this.getMetaFolder(), Constants.META_SUBFOLDER_METADATA);
    }
    /**
     * Reads the stored checksum from the HASH file in the meta folder for the provided path (doesn't calculate anything)
     *
     * @return the hash or null if it doesn't exist
     */
    @Override
    public String getMetaHashChecksum()
    {
        String retVal = null;

        Path storedHashFile = this.getMetaHashFile();

        try {
            if (this.fileContext.util().exists(storedHashFile)) {
                retVal = HdfsUtils.readFile(this.fileContext, storedHashFile);
            }
        }
        catch (IOException e) {
            Logger.error("Caught exception while reading the stored hash file contents of " + this.getLocalPath(), e);
        }

        return retVal;
    }
    /**
     * Calculates the Adler32 checksum of the contents of the supplied path
     *
     * @return
     * @throws IOException
     */
    @Override
    public String calcChecksumHash() throws IOException
    {
        String retVal = null;

        try (InputStream is = fileContext.open(localPath)) {
            retVal = calcHashChecksumFor(is);
        }

        return retVal;
    }
    /**
     * Pretty simple locking mechanism, probably full of holes, but a first try to create something simple to set up (eg. no Zookeeper)
     * Note: this should work pretty ok, because creation/deletion of files MUST be atomic in HDFS;
     * see https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-common/filesystem/introduction.html
     *
     * @return the lock file
     */
    @Override
    public LockFile acquireLock() throws IOException
    {
        long timer = 0;

        Path lock = this.getLockFile();

        while (this.fileContext.util().exists(lock)) {
            try {
                Thread.sleep(DEFAULT_LOCK_BACK_OFF);
            }
            catch (InterruptedException e) {
                throw new IOException("Error happened while waiting on file lock; " + lock, e);
            }
            timer += DEFAULT_LOCK_BACK_OFF;

            if (timer >= DEFAULT_LOCK_TIMEOUT) {
                throw new IOException("Unable to get lock on file; timeout of (" + DEFAULT_LOCK_TIMEOUT + " ms exceeded); " + lock);
            }
        }

        //note: not possible another process 'gets between' the loop above and this, because this will throw an exception if the file already exists.
        if (!HdfsUtils.createNewFile(this.fileContext, lock, true)) {
            throw new IOException("Unable to create lock file because of an error or because (in the mean time) it already existed; " + lock);
        }

        //Since we're the one creating it, it makes sense (if anything goes wrong while releasing the lock) to remove it on JVM shutdown.
        this.fileContext.deleteOnExit(lock);

        return new LockFile(this, lock);
    }
    @Override
    public boolean isLocked() throws IOException
    {
        return this.fileContext.util().exists(this.getLockFile());
    }
    @Override
    public void releaseLockFile(LockFile lock) throws IOException
    {
        if (lock != null) {
            synchronized (lock) {

                if (!this.fileContext.util().exists(lock.getLockFile())) {
                    throw new IOException("Trying to release a lock file that doesn't exist; something's wrong; " + lock.getLockFile());
                }

                if (!this.fileContext.delete(lock.getLockFile(), false)) {
                    throw new IOException("Error happened while releasing a lock file; " + lock.getLockFile());
                }
            }
        }
    }
    @Override
    public boolean isMetaFile()
    {
        return HdfsUtils.isMetaPath(this.localPath);
    }

    //-----UTILITY METHODS-----
    public static String calcHashChecksumFor(InputStream is) throws IOException
    {
        //we want speed, not security (think large media files!)
        //return calcAdler32Checksum(is);
        return DigestUtils.md5Hex(is);
        //return DigestUtils.sha1Hex(is);
    }

    //-----PROTECTED METHODS-----
    protected Path getLockFile()
    {
        //only needs to be done once
        if (this.lockFile == null) {
            this.lockFile = createLockPath(this.localPath);
        }

        return this.lockFile;
    }

    //-----PRIVATE METHODS-----
    /**
     * Note: we're not responsible for closing the stream
     */
    private static String calcAdler32Checksum(InputStream is) throws IOException
    {
        Adler32 checksum = new Adler32();
        byte[] buffer = new byte[4096];

        while (true) {
            int length = is.read(buffer);

            if (length < 0) {
                break;
            }

            checksum.update(buffer, 0, length);
        }

        // Reduces it down to just 32 bits which we express in hex.
        return Long.toHexString(checksum.getValue());
    }

    //-----MANAGEMENT METHODS-----
    public static Path createLockPath(Path path)
    {
        Path retVal = null;

        Path parent = path.getParent();
        if (parent!=null) {
            retVal = new Path(parent, LOCK_FILE_PREFIX + path.getName() + Settings.instance().getPagesLockFileExtension());
        }

        return retVal;
    }
    @Override
    public String toString()
    {
        return "HdfsPathInfo{" +
               "path=" + localPath +
               ", metaFolder=" + metaFolder +
               '}';
    }
}
