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

package com.beligum.blocks.index.reindex;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.ifaces.ResourceRepository;
import com.beligum.blocks.filesystem.pages.ifaces.Page;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * Created by bram on 11/05/17.
 */
public class PageReimportTask extends ReindexTask
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    protected void runTaskFor(Resource resource, ResourceRepository.IndexOption indexConnectionsOption) throws IOException
    {
        Page page = resource.unwrap(Page.class);
        if (page == null) {
            throw new IOException("Unable to reimport this resource, it's not a valid Page; " + resource);
        }

        Path originalFile = page.getLocalStoragePath();
        if (!page.getFileContext().util().exists(originalFile)) {
            throw new IOException("Original HTML file for this page is missing, can't fix it; " + page.getPublicAbsoluteAddress());
        }

        resource.getRepository().save(page, null);
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
