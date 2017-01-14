package com.beligum.blocks.filesystem.hdfs;

import com.beligum.base.utils.Logger;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

/**
 * Created by bram on 2/2/16.
 */
public class HdfsZipUtils
{
    //-----CONSTANTS-----
    private static final int COPY_BUFFER_SIZE = 4096;
    private static final CompressionCodec gzipCodec = new CompressionCodecFactory(new Configuration()).getCodecByName("gzip");

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static void gzipTarFolder(FileContext fs, Path input, Path output) throws IOException
    {
        // handy to know: remove the .gz extension
        //String outputUri = CompressionCodecFactory.removeSuffix(uri, codec.getDefaultExtension());

        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(gzipCodec.createOutputStream(fs.create(output, EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE))))) {
            // TAR has an 8 gig file limit by default, this gets around that
            taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR); // to get past the 8 gig limit
            // TAR originally didn't support long file names, so enable the support for it
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            addPath(fs, input, null, taos);
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private static void addPath(FileContext fs, Path path, Path parent, TarArchiveOutputStream taos) throws IOException
    {
        // Create an entry for the path
        Path entryPath = parent == null ? new Path(path.getName()) : new Path(parent, path.getName());
        String entryPathStr = entryPath.toString();
        FileStatus status = fs.getFileStatus(path);
        //needed for the tar entry, see TarArchiveEntry() constructor
        if (status.isDirectory() && !entryPathStr.endsWith("/")) {
            //signals the tar constructor this is a dir
            entryPathStr += "/";
        }
        // note that this will also create an entry for the top-level dir we're adding, but that's ok,
        // because otherwise that information would be lost
        taos.putArchiveEntry(pathToTarEntry(fs, path, status, entryPathStr));

        if (status.isFile()) {
            // Add the file to the archive
            try (InputStream bis = new BufferedInputStream(fs.open(path))) {
                IOUtils.copyBytes(bis, taos, COPY_BUFFER_SIZE);
                taos.closeArchiveEntry();
            }
        }
        else if (status.isDirectory()) {
            // close the archive entry
            taos.closeArchiveEntry();

            // go through all the files in the directory and using recursion, add them to the archive
            RemoteIterator<FileStatus> children = fs.listStatus(path);
            while (children.hasNext()) {
                Path child = children.next().getPath();
                addPath(fs, child, entryPath, taos);
            }
        }
        else {
            Logger.warn("Encountered unsupported file type while tar.gzipping a folder; " + path);
        }
    }
    private static TarArchiveEntry pathToTarEntry(FileContext fs, Path path, FileStatus status, String entryName) throws IOException
    {
        TarArchiveEntry entry = new TarArchiveEntry(entryName);

        entry.setNames(status.getOwner(), status.getGroup());
        entry.setSize(status.getLen());
        //not sure about the sticky bits here, taken from TarArchiveEntry default modes
        String octal = "" + status.getPermission().getUserAction().ordinal() + "" + status.getPermission().getGroupAction().ordinal() + "" + status.getPermission().getOtherAction().ordinal();
        if (status.isFile()) {
            entry.setMode(Integer.parseInt("0100" + octal, 8));
        }
        else if (status.isDirectory()) {
            entry.setMode(Integer.parseInt("040" + octal, 8));
        }
        entry.setModTime(status.getModificationTime());

        return entry;
    }
}
