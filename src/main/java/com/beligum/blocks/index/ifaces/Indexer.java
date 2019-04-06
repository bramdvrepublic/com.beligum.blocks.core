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

package com.beligum.blocks.index.ifaces;

import com.beligum.blocks.filesystem.hdfs.TX;

import java.io.IOException;

/**
 * In Stralo, an 'indexer' is a sub-system that extracts data from the main storage (HDFS-based)
 * and stores it in a different manner; eg. a triple store, a full-text-search index, etc.
 * Connections to an indexer are transactional and are attached to the request-scoped TX transactions.
 *
 * Created by bram on 1/26/16.
 */
public interface Indexer
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----

    /**
     * This method starts up a new, transactional session, connected to the supplied transaction.
     *
     * Note: it's ok to pass null as the transaction object to explicitly indicate the session shouldn't be transactional,
     * throwing an exception if any method that requires a transaction would be accessed during the session.
     */
    IndexConnection connect(TX tx) throws IOException;

    /**
     * Sometimes (eg. after a serious setRollbackOnly) it may help to reboot the indexer (and have the transaction log do it's work).
     * This should basically do a shutdown() and re-initialize().
     */
    void reboot() throws IOException;

    /**
     * Permanently shutdown this indexer (on application shutdown)
     */
    void shutdown() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
