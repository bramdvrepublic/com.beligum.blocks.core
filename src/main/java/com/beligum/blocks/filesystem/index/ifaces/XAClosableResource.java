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

package com.beligum.blocks.filesystem.index.ifaces;

import javax.transaction.xa.XAResource;
import java.io.IOException;

/**
 * This extends the regular XAResource with closing facilities. Note that we didn't want to use the XAConnection class
 * because it's part of the sql package and this interface is used broader than just SQL. Also, we didn't want to use
 * AutoClosable because we don't want to hint users to close the connections themselves.
 */
public interface XAClosableResource extends XAResource
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    /**
     * Called at the very end of each request transaction in TX.doClose()
     * Note that we explicitly don't implement AutoClosable because we don't want to hint users to close
     * the connections themselves.
     */
    void close() throws IOException;

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
