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
