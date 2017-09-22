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

package com.beligum.blocks.filesystem.pages;

import com.beligum.base.resources.ResourceInputStream;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.base.resources.ifaces.ResourceRequest;
import com.beligum.blocks.config.StorageFactory;
import org.apache.hadoop.fs.FileContext;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Created by bram on 5/2/16.
 */
public class ReadOnlyPage extends DefaultPage
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    protected ReadOnlyPage(ResourceRequest request) throws IOException
    {
        // Note the selection of the filesystem: if we have a transaction running, we return a transactional file system (despite the fact this is a read-only page)
        // because it's expected behavior: if we're in the middle of manipulating files and we eg. doIsValid the existence of a new ReadOnlyPage, we expect it to seamlessly
        // enter the flow of the transaction, so it doesn't return a file if that file has been deleted during the request transaction.
        super(request, StorageFactory.hasCurrentRequestTx() ? StorageFactory.getPageStoreFileSystem() : StorageFactory.getPageViewFileSystem());
    }
    protected ReadOnlyPage(ResourceRepository repository, URI uri, Locale language, MimeType mimeType, boolean allowEternalCaching, FileContext fileContext) throws IOException
    {
        super(repository, uri, language, mimeType, allowEternalCaching, fileContext);
    }

    //-----PUBLIC METHODS-----
    @Override
    public ResourceInputStream newInputStream() throws IOException
    {
        return new ResourceInputStream(this.fileContext.open(this.getNormalizedHtmlFile()), this.fileContext.getFileStatus(this.getNormalizedHtmlFile()).getLen());
    }
    @Override
    public boolean isReadOnly()
    {
        return true;
    }
    @Override
    public ReadWritePage getReadWriteVariant() throws IOException
    {
        return new ReadWritePage(this.getRepository(), this);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
