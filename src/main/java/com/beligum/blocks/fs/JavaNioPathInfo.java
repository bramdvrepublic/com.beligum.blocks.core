package com.beligum.blocks.fs;

import com.beligum.base.utils.Logger;
import com.beligum.blocks.config.Settings;
import com.beligum.blocks.fs.ifaces.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.tika.mime.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by bram on 1/19/16.
 */
public class JavaNioPathInfo extends AbstractPathInfo<Path>
{
    //-----INTERFACES-----
    private static class JavaNioPathFactory implements PathFactory<Path>
    {
        public JavaNioPathFactory()
        {
        }

        @Override
        public Path create(URI uri)
        {
            return Paths.get(uri);
        }
        @Override
        public Path create(Path parent, Path child)
        {
            return parent.resolve(child);
        }
        @Override
        public Path create(Path parent, String childName)
        {
            return parent.resolve(childName);
        }
    }

    //-----CONSTANTS-----
    public static final PathFactory<Path> PATH_FACTORY = new JavaNioPathFactory();

    //-----VARIABLES-----
    private Path path;
    private Path lockFile;
    private Path metaFolder;

    //-----CONSTRUCTORS-----
    public JavaNioPathInfo(Path path) throws IOException
    {
        this.path = path;
    }

    //-----PUBLIC METHODS-----
    @Override
    public PathFactory<Path> getPathFactory()
    {
        return PATH_FACTORY;
    }
    @Override
    public Path getPath()
    {
        return this.path;
    }
    @Override
    public Object getPathFileSystem()
    {
        return this.path.getFileSystem();
    }
    @Override
    public Path getMetaFolder()
    {
        //only needs to be done once
        if (this.metaFolder == null) {
            this.metaFolder = this.path.getParent().resolve(Constants.META_FOLDER_PREFIX + this.path.getFileName().toString());
        }

        return this.metaFolder;
    }
    @Override
    public Path getMetaHashFile()
    {
        return this.getMetaFolder().resolve(Constants.META_SUBFILE_HASH);
    }
    @Override
    public Path getMetaHistoryFolder()
    {
        return this.getMetaFolder().resolve(Constants.META_SUBFOLDER_HISTORY);
    }
    @Override
    public Path getMetaMonitorFolder()
    {
        return this.getMetaFolder().resolve(Constants.META_SUBFOLDER_MONITOR);
    }
    @Override
    public Path getMetaProxyFolder()
    {
        return this.getMetaFolder().resolve(Constants.META_SUBFOLDER_PROXY);
    }
    @Override
    public Path getMetaProxyFolder(MediaType mimeType)
    {
        return this.getMetaProxyFolder().resolve(mimeType.getType()).resolve(mimeType.getSubtype());
    }
    @Override
    public Path getMetaMetadataFolder()
    {
        return this.getMetaFolder().resolve(Constants.META_SUBFOLDER_METADATA);
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
        if (Files.exists(storedHashFile)) {
            try {
                retVal = new String(Files.readAllBytes(storedHashFile));
            }
            catch (IOException e) {
                Logger.error("Caught exception while reading the stored hash file contents of " + this.getPath(), e);
            }
        }

        return retVal;
    }
    /**
     * Calculates the SHA-1 checksum of the contents of the supplied path
     *
     * @return
     * @throws IOException
     */
    @Override
    public String calcHashChecksum() throws IOException
    {
        String retVal = null;

        try (InputStream is = Files.newInputStream(this.getPath())) {
            retVal = DigestUtils.sha1Hex(is);
        }

        return retVal;
    }
    /**
     * Simple copy/paste from the HDFS counterpart and untested (should probably use FileLocks?)
     *
     * @return the lock file
     */
    @Override
    public LockFile<Path> acquireLock() throws IOException
    {
        long timer = 0;

        Path lock = this.getLockFile();

        while (Files.exists(lock)) {
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
        //From the docs:
        // Creates a new and empty file, failing if the file already exists. The check for the existence of the file and the creation of the new file
        // if it does not exist are a single operation that is atomic with respect to all other filesystem activities that might affect the directory.
        try {
            Files.createFile(lock);
        }
        catch (FileAlreadyExistsException e) {
            throw new IOException("Unable to create lock file because of an error or because (in the mean time) it already existed; " + lock, e);
        }

        return new LockFile(this, lock);
    }
    @Override
    public boolean isLocked() throws IOException
    {
        return Files.exists(this.getLockFile());
    }
    @Override
    public void releaseLockFile(LockFile<Path> lock) throws IOException
    {
        if (lock != null) {
            synchronized (lock) {

                if (!Files.exists(lock.getLockFile())) {
                    throw new IOException("Trying to release a lock file that doesn't exist; something's wrong...; " + lock.getLockFile());
                }

                try {
                    Files.delete(lock.getLockFile());
                }
                catch (FileAlreadyExistsException e) {
                    throw new IOException("Error happened while releasing a lock file; " + lock.getLockFile(), e);
                }
            }
        }
    }

    //-----PROTECTED METHODS-----
    @Override
    protected Path getLockFile()
    {
        //only needs to be done once
        if (this.lockFile == null) {
            this.lockFile = createLockPath(this.path);
        }

        return this.lockFile;
    }

    //-----PRIVATE METHODS-----

    //-----MANAGEMENT METHODS-----
    public static Path createLockPath(Path path)
    {
        return path.getParent().resolve(LOCK_FILE_PREFIX + path.getFileName() + Settings.instance().getPagesLockFileExtension());
    }
    @Override
    public String toString()
    {
        return "JavaNioPathInfo{" +
               "path=" + path +
               ", metaFolder=" + metaFolder +
               '}';
    }
}
