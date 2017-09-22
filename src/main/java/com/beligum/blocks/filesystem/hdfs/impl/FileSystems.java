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

package com.beligum.blocks.filesystem.hdfs.impl;

import com.beligum.blocks.filesystem.hdfs.HdfsImplDef;

/**
 * Created by bram on 1/14/17.
 */
public class FileSystems
{
    public static final HdfsImplDef LOCAL = new HdfsImplDef(LocalReadWriteFS.SCHEME, LocalReadWriteFS.class);
    public static final HdfsImplDef LOCAL_CHROOT = new HdfsImplDef(ChRootedLocalReadWriteFS.SCHEME, ChRootedLocalReadWriteFS.class);
    public static final HdfsImplDef LOCAL_RO = new HdfsImplDef(LocalReadOnlyFS.SCHEME, LocalReadOnlyFS.class);
    public static final HdfsImplDef LOCAL_RO_CHROOT = new HdfsImplDef(ChRootedLocalReadOnlyFS.SCHEME, ChRootedLocalReadOnlyFS.class);
    public static final HdfsImplDef LOCAL_TX = new HdfsImplDef(LocalTransactionalFS.SCHEME, LocalTransactionalFS.class);
    public static final HdfsImplDef LOCAL_TX_CHROOT = new HdfsImplDef(ChRootedLocalTransactionalFS.SCHEME, ChRootedLocalTransactionalFS.class);
}
