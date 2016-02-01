package com.beligum.blocks.fs;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.EnumSet;

/**
 * Created by bram on 1/19/16.
 */
public class HdfsUtils
{
    //-----CONSTANTS-----
    private static final SecureRandom random = new SecureRandom();

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Returns the current config.
     * TODO: hope this is ok!
     * @return
     */
    public static Configuration getConf()
    {
        return new Configuration();
    }
    public static String readFile(FileContext fs, Path path) throws IOException
    {
        String retVal = null;

        try (InputStream is = fs.open(path)) {
            IOUtils.toString(is, Charsets.UTF_8);
        }

        return retVal;
    }
    public static void recursiveDeleteLockFiles(FileContext fs, Path path) throws IOException
    {
        RemoteIterator<FileStatus> status = fs.listStatus(path);
        while (status.hasNext()) {
            FileStatus fileStatus = status.next();
            Path lockFile = HdfsPathInfo.createLockPath(fileStatus.getPath());
            if (fs.util().exists(lockFile)) {
                fs.delete(lockFile, false);
            }

            if (fileStatus.isDirectory()) {
                recursiveDeleteLockFiles(fs, fileStatus.getPath());
            }
        }
    }
    /**
     * Copy/pasted from
     * @see org.apache.hadoop.fs.FileSystem#createNewFile(Path)
     */
    public static boolean createNewFile(FileContext context, Path f, boolean createParents) throws IOException
    {
        if (context.util().exists(f)) {
            return false;
        } else {
            if (createParents) {
                context.create(f, EnumSet.of(CreateFlag.CREATE), Options.CreateOpts.bufferSize(getConf().getInt("io.file.buffer.size", 4096)), Options.CreateOpts.createParent()).close();
            }
            else {
                context.create(f, EnumSet.of(CreateFlag.CREATE), Options.CreateOpts.bufferSize(getConf().getInt("io.file.buffer.size", 4096))).close();
            }
            return true;
        }
    }
    /**
     * Mainly copied from java.io.File to mimic the creation of temp files on HDFS
     * @see java.io.File#createTempFile(String, String, File)
     */
    public static Path createTempFile(FileContext context, String prefix, String suffix, Path directory) throws IOException
    {
        if (prefix.length() < 3) {
            throw new IllegalArgumentException("Prefix string too short");
        }
        if (suffix == null) {
            suffix = ".tmp";
        }

        Path tmpdir = (directory != null) ? directory : new Path(getConf().get("hadoop.tmp.dir"));

        Path f;
        do {
            f = generateFile(prefix, suffix, tmpdir);
        } while (context.util().exists(f));

        if (!createNewFile(context, f, true)) {
            throw new IOException("Unable to create temporary file");
        }

        return f;
    }
    public static Path createTempFile(FileContext context, String prefix, String suffix) throws IOException
    {
        return createTempFile(context, prefix, suffix, null);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private static Path generateFile(String prefix, String suffix, Path dir) throws IOException
    {
        long n = random.nextLong();
        if (n == Long.MIN_VALUE) {
            n = 0;      // corner case
        } else {
            n = Math.abs(n);
        }

        // Use only the file name from the supplied prefix
        prefix = (new Path(prefix)).getName();

        String name = prefix + Long.toString(n) + suffix;
        Path f = new Path(dir, name);
        //commented out the validity check; I assume the Path creation will fail of not valid?
        if (!name.equals(f.getName())/* || f.isInvalid()*/) {
            throw new IOException("Unable to create temporary file, " + f);
        }
        return f;
    }
}
