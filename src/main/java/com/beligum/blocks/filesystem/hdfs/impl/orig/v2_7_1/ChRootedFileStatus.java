package com.beligum.blocks.filesystem.hdfs.impl.orig.v2_7_1;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by bram on 1/23/17.
 */
public class ChRootedFileStatus extends FileStatus
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public ChRootedFileStatus(FileStatus fileStatus, URI chRootPathPartUri) throws IOException
    {
        super(fileStatus);

        //this only thing we need to change in the super is the path
        //Note: following code has two functions:
        // - change the schema back to the schema of the chrootFs by only using the path (and resolving to the chRootPathPartUri)
        // - strip the chroot from the path in the file status' path
        try {
            String path = this.getPath().toUri().getPath();
            if (path.startsWith(chRootPathPartUri.getPath())) {
                path = path.substring(chRootPathPartUri.getPath().length());
            }
            if (!path.startsWith("/")) {
                path = "/"+path;
            }

            URI newPath = new URI(chRootPathPartUri.getScheme(),
                              chRootPathPartUri.getUserInfo(),
                              chRootPathPartUri.getHost(),
                              chRootPathPartUri.getPort(),
                              path,
                              chRootPathPartUri.getQuery(),
                              chRootPathPartUri.getFragment());

            this.setPath(new Path(newPath));
        }
        catch (URISyntaxException e) {
            throw new IOException("Error while relativizing file status path "+this.getPath()+" to chroot "+chRootPathPartUri, e);
        }
    }

    //-----PUBLIC METHODS-----
    public static FileStatus[] wrap(FileStatus[] fileStatuses, URI chRootPathPartUri) throws IOException
    {
        FileStatus[] retVal = null;

        if (fileStatuses != null) {
            retVal = new FileStatus[fileStatuses.length];
            for (int i = 0; i < fileStatuses.length; i++) {
                //don't wrap twice
                if (fileStatuses[i] instanceof ChRootedFileStatus) {
                    retVal[i] = fileStatuses[i];
                }
                else {
                    retVal[i] = new ChRootedFileStatus(fileStatuses[i], chRootPathPartUri);
                }
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
