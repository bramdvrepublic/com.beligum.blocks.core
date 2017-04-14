package com.beligum.blocks.filesystem;

import com.beligum.base.resources.GuavaMimeType;
import com.beligum.base.resources.HashImpl;
import com.beligum.base.resources.ResourceInputStream;
import com.beligum.base.resources.ifaces.Hash;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.base.resources.mappers.AbstractResource;
import com.beligum.base.server.R;
import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.filesystem.hdfs.HdfsUtils;
import com.beligum.blocks.filesystem.ifaces.BlocksResource;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.Options;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.net.URI;
import java.util.EnumSet;
import java.util.Locale;
import java.util.zip.Adler32;

/**
 * Created by bram on 2/7/16.
 */
public abstract class AbstractBlocksResource extends AbstractResource implements BlocksResource
{
    //-----CONSTANTS-----
    protected static final String HASH_FIELD_SEP = "\t";

    //-----VARIABLES-----
    protected FileContext fileContext;
    protected Path localStoragePath;

    protected Path cachedDotFolder;
    protected Path cachedLockFile;

    //-----CONSTRUCTORS-----
    protected AbstractBlocksResource(ResourceRequest request, FileContext fileContext, Path localStoragePath)
    {
        super(request);

        this.fileContext = fileContext;
        this.localStoragePath = localStoragePath;
    }
    /**
     * Constructor without the localStorage (that need to be set manually)
     */
    protected AbstractBlocksResource(ResourceRequest request, FileContext fileContext)
    {
        //Note: don't forget to set the local storage path in the subclass!
        this(request, fileContext, null);
    }
    protected AbstractBlocksResource(ResourceRepository repository, URI uri, Locale language, MimeType mimeType, boolean allowEternalCaching, FileContext fileContext, Path localStoragePath)
    {
        super(repository, uri, language, mimeType, allowEternalCaching);

        this.fileContext = fileContext;
        this.localStoragePath = localStoragePath;
    }

    //-----PUBLIC METHODS-----
    @Override
    public ResourceInputStream newInputStream() throws IOException
    {
        return new ResourceInputStream(this.fileContext.open(this.localStoragePath), this.getSize());
    }
    @Override
    public boolean exists() throws IOException
    {
        return this.fileContext.util().exists(this.localStoragePath);
    }
    @Override
    public boolean isDirectory() throws IOException
    {
        return this.exists() && this.fileContext.getFileStatus(this.localStoragePath).isDirectory();
    }
    @Override
    public long getLastModifiedTime() throws IOException
    {
        return Math.max((this.fileContext == null ? this.getZeroLastModificationTime() : this.fileContext.getFileStatus(this.localStoragePath).getModificationTime()),
                        this.calcChildrenLastModificationTime());
    }
    @Override
    public long getSize() throws IOException
    {
        return this.fileContext.getFileStatus(this.localStoragePath).getLen();
    }
    @Override
    public Path getLocalStoragePath()
    {
        return localStoragePath;
    }
    @Override
    public FileContext getFileContext()
    {
        return fileContext;
    }
    @Override
    public Path getDotFolder()
    {
        //only needs to be done once
        if (this.cachedDotFolder == null) {
            this.cachedDotFolder = new Path(this.localStoragePath.getParent(), META_FOLDER_PREFIX + this.localStoragePath.getName().toString());
        }

        return this.cachedDotFolder;
    }
    @Override
    public Path getHashFile()
    {
        return new Path(this.getDotFolder(), META_SUBFILE_HASH);
    }
    @Override
    public Path getLogFile()
    {
        return new Path(this.getDotFolder(), META_SUBFILE_LOG);
    }
    @Override
    public Path getMimeFile()
    {
        return new Path(this.getDotFolder(), META_SUBFILE_MIME);
    }
    @Override
    public Path getHistoryFolder()
    {
        //Note: if you change something here, also check the code where we delete the history folder from the snapshots (in HdfsPageStore)
        return new Path(this.getDotFolder(), META_SUBFOLDER_HISTORY);
    }
    @Override
    public Path getMonitorFolder()
    {
        return new Path(this.getDotFolder(), META_SUBFOLDER_MONITOR);
    }
    @Override
    public Path getProxyFolder()
    {
        return new Path(this.getDotFolder(), META_SUBFOLDER_PROXY);
    }
    @Override
    public Path getProxyFolder(MimeType mimeType)
    {
        return new Path(new Path(this.getProxyFolder(), mimeType.type()), mimeType.subtype());
    }
    @Override
    public Path getMetadataFolder()
    {
        return new Path(this.getDotFolder(), META_SUBFOLDER_METADATA);
    }
    @Override
    public Hash getHash(boolean forceRecalculation)
    {
        Hash retVal = null;

        try {
            if (forceRecalculation) {
                retVal = super.getHash(true);
            }
            else {
                Path storedHashFile = this.getHashFile();

                //Note: this means we're responsible to update the content of the hash file every time this resource changes!
                if (this.fileContext.util().exists(storedHashFile)) {
                    try {
                        String hashStr = HdfsUtils.readFile(this.fileContext, storedHashFile);
                        if (!StringUtils.isEmpty(hashStr)) {
                            String[] fields = hashStr.split(HASH_FIELD_SEP);
                            if (fields.length == 2) {
                                Hash.Method method = Hash.Method.valueOf(fields[1]);
                                if (method.equals(HASH_METHOD)) {
                                    retVal = new HashImpl(fields[0], true, method);
                                }
                            }
                        }
                    }
                    catch (Exception e) {
                        Logger.error("Caught exception while parsing cached hash file of " + this.getUri(), e);
                    }
                }

                if (retVal == null) {
                    retVal = super.getHash(true);
                }
            }
        }
        catch (IOException e) {
            Logger.error("Caught exception while calculating/reading the stored hash file contents of " + this.getLocalStoragePath(), e);
        }

        return retVal;
    }
    @Override
    public void writeHash(Hash newHash) throws IOException
    {
        if (!this.isReadOnly()) {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(this.getFileContext().create(this.getHashFile(),
                                                                                                        EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE),
                                                                                                        Options.CreateOpts.createParent())))) {
                writer.write(new StringBuilder().append(newHash.getChecksum()).append(HASH_FIELD_SEP).append(newHash.getMethod().name()).toString());
            }
        }
        else {
            throw new IOException("Trying to update the hash value on a read-only blocks resource; "+this);
        }
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

        //this is some auto-cleanup code
        if (this.fileContext.util().exists(lock)) {
            long modTime = this.fileContext.getFileStatus(lock).getModificationTime();
            boolean delete = false;

            //lazy deletion: if lock files should be deleted at startup and the mod time is older than the startup, delete it
            if (Settings.instance().getDeleteLocksOnStartup() && R.startTime() >= modTime) {
                Logger.info("Deleting old lock file; " + lock);
                delete = true;
            }
            //delete stale lock file older than one hour
            else if (System.currentTimeMillis() - modTime >= DEFAULT_LOCK_MAX_AGE) {
                Logger.info("Deleting stale lock file; " + lock);
                delete = true;
            }

            if (delete) {
                this.fileContext.delete(lock, false);
            }
        }

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
            throw new IOException("Unable to create lock file because of an setRollbackOnly or because (in the mean time) it already existed; " + lock);
        }

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

    //-----PROTECTED METHODS-----
    protected Path getLockFile()
    {
        //only needs to be done once
        if (this.cachedLockFile == null) {
            this.cachedLockFile = createLockPath(this.localStoragePath);
        }

        return this.cachedLockFile;
    }
    protected MimeType readMimeType()
    {
        MimeType retVal = null;

        Path storedMimeFile = this.getMimeFile();
        boolean newlyDetected = true;
        try {
            if (this.fileContext.util().exists(storedMimeFile)) {
                String content = HdfsUtils.readFile(this.fileContext, storedMimeFile);
                if (!StringUtils.isEmpty(content)) {
                    retVal = GuavaMimeType.parse(content);
                }
            }

            if (retVal == null) {
                retVal = HdfsUtils.detectMimeType(this.fileContext, this.getLocalStoragePath());
            }
            else {
                newlyDetected = false;
            }
        }
        catch (IOException e) {
            Logger.error("Caught exception while reading the stored mime type file contents of " + this.getLocalStoragePath(), e);
        }
        finally {
            if (retVal != null) {
                //store the mime type to a cache file for quick future detection
                if (newlyDetected) {
                    try (OutputStream os = fileContext.create(storedMimeFile, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE), Options.CreateOpts.createParent())) {
                        org.apache.commons.io.IOUtils.write(retVal.toString(), os);
                    }
                    catch (Exception e) {
                        Logger.error("Error while saving mime type '" + retVal + "' to cache file; this shouldn't happen; " + this.getLocalStoragePath(), e);
                    }
                }
            }
        }

        return retVal;
    }

    //-----PRIVATE METHODS-----
    private Path createLockPath(Path path)
    {
        Path retVal = null;

        Path parent = path.getParent();
        if (parent != null) {
            retVal = new Path(parent, LOCK_FILE_PREFIX + path.getName() + Settings.instance().getPagesLockFileExtension());
        }

        return retVal;
    }
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
}
