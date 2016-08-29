package com.beligum.blocks.fs;

import com.beligum.blocks.fs.hdfs.HadoopBasicFileAttributes;
import com.beligum.blocks.fs.ifaces.Constants;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.util.EnumSet;

/**
 * Created by bram on 1/19/16.
 */
public class HdfsUtils
{
    //-----CONSTANTS-----
    public static final URI ROOT = URI.create("/");

    private static final SecureRandom random = new SecureRandom();

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static String readFile(FileContext fs, Path path) throws IOException
    {
        String retVal = null;

        try (InputStream is = fs.open(path)) {
            retVal = IOUtils.toString(is, Charsets.UTF_8);
        }

        return retVal;
    }
    public static void recursiveDeleteLockFiles(FileContext fs, Path path) throws IOException
    {
        HdfsUtils.walkFileTree(fs, path, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                return this.process(fs, file);
            }
            @Override
            public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) throws IOException
            {
                return this.process(fs, file);
            }

            private FileVisitResult process(FileContext fileContext, Path path) throws IOException
            {
                FileVisitResult retVal = FileVisitResult.CONTINUE;

                Path lockFile = HdfsResourcePath.createLockPath(path);
                if (lockFile!=null) {
                    if (fs.util().exists(lockFile)) {
                        fs.delete(lockFile, false);
                    }
                }

                return retVal;
            }
        });
    }
    /**
     * Basic implementation of a recursive, depth-first file visitor for HDFS paths,
     * mimicing the Files.walkFileTree() of Java NIO.
     * Note: if using a chrooted fileSystem, callback files will be absolute (not relative to the fileSystem chroot)
     *
     * @param fs the filesystem
     * @param folder the folder where to start
     * @param visitor the visitor implementation
     * @throws IOException
     */
    public static void walkFileTree(FileContext fs, Path folder, FileVisitor<Path> visitor) throws IOException
    {
        RemoteIterator<FileStatus> status = fs.listStatus(folder);

        URI fsRoot = fs.resolvePath(new Path("/")).toUri();
        while (status.hasNext()) {
            FileStatus childStatus = status.next();
            //this relativation is needed to be able to work with chrooted filesystems, because fs.listStatus() returns absolute file paths
            Path child = new Path(ROOT.resolve(fsRoot.relativize(childStatus.getPath().toUri())));
            if (childStatus.isDirectory()) {

                visitor.preVisitDirectory(child, new HadoopBasicFileAttributes(childStatus));

                IOException exception = null;
                try {
                    walkFileTree(fs, child, visitor);
                }
                catch (IOException e) {
                    exception = e;
                }

                visitor.postVisitDirectory(child, exception);
            }
            else {
                visitor.visitFile(child, new HadoopBasicFileAttributes(childStatus));
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
    public static boolean isMetaPath(Path localPath)
    {
        boolean retVal = false;

        // since we don't allow hidden files or folders as original files/folders,
        // all hidden files and folders inside the hdfs fs are meta files,
        // so if a hidden file is present somewhere on the path-names-way to the file,
        // it's a meta file

        Path p = localPath;
        while (p != null && !retVal) {
            if (p.getName().startsWith(Constants.META_FOLDER_PREFIX)) {
                retVal = true;
            }
            p = p.getParent();
        }

        return retVal;
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
    /**
     * Returns the current config.
     * TODO: hope this is ok!
     * @return
     */
    private static Configuration getConf()
    {
        return new Configuration();
    }

    //-----INNER CLASSES------

}
