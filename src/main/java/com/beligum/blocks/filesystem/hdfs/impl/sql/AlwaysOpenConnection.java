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

package com.beligum.blocks.filesystem.hdfs.impl.sql;

import java.sql.Connection;
import java.sql.SQLException;

public class AlwaysOpenConnection extends WrappedConnection
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public AlwaysOpenConnection(Connection wrappedConnection)
    {
        super(wrappedConnection);
    }

    //-----PUBLIC METHODS-----
    @Override
    public void close() throws SQLException
    {
        //Explicit NOOP: close with forceClose() if you really want to close
    }
    public void forceClose() throws SQLException
    {
        super.close();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
