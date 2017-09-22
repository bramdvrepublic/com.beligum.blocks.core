/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        if (fileStatuses != null) {
            for (int i = 0; i < fileStatuses.length; i++) {
                fileStatuses[i] = wrap(fileStatuses[i], chRootPathPartUri);
            }
        }

        return fileStatuses;
    }
    public static FileStatus wrap(FileStatus fileStatus, URI chRootPathPartUri) throws IOException
    {
        //don't wrap twice
        return fileStatus instanceof ChRootedFileStatus ? fileStatus : new ChRootedFileStatus(fileStatus, chRootPathPartUri);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
