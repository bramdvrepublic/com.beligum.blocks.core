package com.beligum.blocks.index.solr;

import com.beligum.blocks.index.fields.ResourceTypeField;
import com.beligum.blocks.index.ifaces.IndexEntryField;
import com.beligum.blocks.index.ifaces.ResourceIndexEntry;
import com.beligum.blocks.index.ifaces.PageIndexEntry;

public class SolrConfigs
{
    //-----CONSTANTS-----
    /**
     * This is the default global config in Solr 8.0
     */
    public static final String SOLR_CONFIG = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                                             "<!--\n" +
                                             " Licensed to the Apache Software Foundation (ASF) under one or more\n" +
                                             " contributor license agreements.  See the NOTICE file distributed with\n" +
                                             " this work for additional information regarding copyright ownership.\n" +
                                             " The ASF licenses this file to You under the Apache License, Version 2.0\n" +
                                             " (the \"License\"); you may not use this file except in compliance with\n" +
                                             " the License.  You may obtain a copy of the License at\n" +
                                             "\n" +
                                             "     http://www.apache.org/licenses/LICENSE-2.0\n" +
                                             "\n" +
                                             " Unless required by applicable law or agreed to in writing, software\n" +
                                             " distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                                             " WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                                             " See the License for the specific language governing permissions and\n" +
                                             " limitations under the License.\n" +
                                             "-->\n" +
                                             "\n" +
                                             "<!--\n" +
                                             "   This is an example of a simple \"solr.xml\" file for configuring one or \n" +
                                             "   more Solr Cores, as well as allowing Cores to be added, removed, and \n" +
                                             "   reloaded via HTTP requests.\n" +
                                             "\n" +
                                             "   More information about options available in this configuration file, \n" +
                                             "   and Solr Core administration can be found online:\n" +
                                             "   http://wiki.apache.org/solr/CoreAdmin\n" +
                                             "-->\n" +
                                             "\n" +
                                             "<solr>\n" +
                                             "\n" +
                                             "  <solrcloud>\n" +
                                             "\n" +
                                             "    <str name=\"host\">${host:}</str>\n" +
                                             "    <int name=\"hostPort\">${jetty.port:8983}</int>\n" +
                                             "    <str name=\"hostContext\">${hostContext:solr}</str>\n" +
                                             "\n" +
                                             "    <bool name=\"genericCoreNodeNames\">${genericCoreNodeNames:true}</bool>\n" +
                                             "\n" +
                                             "    <int name=\"zkClientTimeout\">${zkClientTimeout:30000}</int>\n" +
                                             "    <int name=\"distribUpdateSoTimeout\">${distribUpdateSoTimeout:600000}</int>\n" +
                                             "    <int name=\"distribUpdateConnTimeout\">${distribUpdateConnTimeout:60000}</int>\n" +
                                             "    <str name=\"zkCredentialsProvider\">${zkCredentialsProvider:org.apache.solr.common.cloud.DefaultZkCredentialsProvider}</str>\n" +
                                             "    <str name=\"zkACLProvider\">${zkACLProvider:org.apache.solr.common.cloud.DefaultZkACLProvider}</str>\n" +
                                             "\n" +
                                             "  </solrcloud>\n" +
                                             "\n" +
                                             "  <shardHandlerFactory name=\"shardHandlerFactory\"\n" +
                                             "    class=\"HttpShardHandlerFactory\">\n" +
                                             "    <int name=\"socketTimeout\">${socketTimeout:600000}</int>\n" +
                                             "    <int name=\"connTimeout\">${connTimeout:60000}</int>\n" +
                                             "    <str name=\"shardsWhitelist\">${solr.shardsWhitelist:}</str>\n" +
                                             "  </shardHandlerFactory>\n" +
                                             "\n" +
                                             "</solr>\n";

    /**
     * This is the default core config in Solr 8.0
     */
    public static final String CORE_CONFIG = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                                             "<!--\n" +
                                             " Licensed to the Apache Software Foundation (ASF) under one or more\n" +
                                             " contributor license agreements.  See the NOTICE file distributed with\n" +
                                             " this work for additional information regarding copyright ownership.\n" +
                                             " The ASF licenses this file to You under the Apache License, Version 2.0\n" +
                                             " (the \"License\"); you may not use this file except in compliance with\n" +
                                             " the License.  You may obtain a copy of the License at\n" +
                                             "\n" +
                                             "     http://www.apache.org/licenses/LICENSE-2.0\n" +
                                             "\n" +
                                             " Unless required by applicable law or agreed to in writing, software\n" +
                                             " distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                                             " WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                                             " See the License for the specific language governing permissions and\n" +
                                             " limitations under the License.\n" +
                                             "-->\n" +
                                             "\n" +
                                             "<!--\n" +
                                             "     For more details about configurations options that may appear in\n" +
                                             "     this file, see http://wiki.apache.org/solr/SolrConfigXml.\n" +
                                             "-->\n" +
                                             "<config>\n" +
                                             "  <!-- In all configuration below, a prefix of \"solr.\" for class names\n" +
                                             "       is an alias that causes solr to search appropriate packages,\n" +
                                             "       including org.apache.solr.(search|update|request|core|analysis)\n" +
                                             "\n" +
                                             "       You may also specify a fully qualified Java classname if you\n" +
                                             "       have your own custom plugins.\n" +
                                             "    -->\n" +
                                             "\n" +
                                             "  <!-- Controls what version of Lucene various components of Solr\n" +
                                             "       adhere to.  Generally, you want to use the latest version to\n" +
                                             "       get all bug fixes and improvements. It is highly recommended\n" +
                                             "       that you fully re-index after changing this setting as it can\n" +
                                             "       affect both how text is indexed and queried.\n" +
                                             "  -->\n" +
                                             "  <luceneMatchVersion>8.0.0</luceneMatchVersion>\n" +
                                             "\n" +
                                             "  <!-- <lib/> directives can be used to instruct Solr to load any Jars\n" +
                                             "       identified and use them to resolve any \"plugins\" specified in\n" +
                                             "       your solrconfig.xml or schema.xml (ie: Analyzers, Request\n" +
                                             "       Handlers, etc...).\n" +
                                             "\n" +
                                             "       All directories and paths are resolved relative to the\n" +
                                             "       instanceDir.\n" +
                                             "\n" +
                                             "       Please note that <lib/> directives are processed in the order\n" +
                                             "       that they appear in your solrconfig.xml file, and are \"stacked\"\n" +
                                             "       on top of each other when building a ClassLoader - so if you have\n" +
                                             "       plugin jars with dependencies on other jars, the \"lower level\"\n" +
                                             "       dependency jars should be loaded first.\n" +
                                             "\n" +
                                             "       If a \"./lib\" directory exists in your instanceDir, all files\n" +
                                             "       found in it are included as if you had used the following\n" +
                                             "       syntax...\n" +
                                             "\n" +
                                             "              <lib dir=\"./lib\" />\n" +
                                             "    -->\n" +
                                             "\n" +
                                             "  <!-- A 'dir' option by itself adds any files found in the directory\n" +
                                             "       to the classpath, this is useful for including all jars in a\n" +
                                             "       directory.\n" +
                                             "\n" +
                                             "       When a 'regex' is specified in addition to a 'dir', only the\n" +
                                             "       files in that directory which completely match the regex\n" +
                                             "       (anchored on both ends) will be included.\n" +
                                             "\n" +
                                             "       If a 'dir' option (with or without a regex) is used and nothing\n" +
                                             "       is found that matches, a warning will be logged.\n" +
                                             "\n" +
                                             "       The examples below can be used to load some solr-contribs along\n" +
                                             "       with their external dependencies.\n" +
                                             "    -->\n" +
                                             "  <lib dir=\"${solr.install.dir:../../../..}/contrib/extraction/lib\" regex=\".*\\.jar\" />\n" +
                                             "  <lib dir=\"${solr.install.dir:../../../..}/dist/\" regex=\"solr-cell-\\d.*\\.jar\" />\n" +
                                             "\n" +
                                             "  <lib dir=\"${solr.install.dir:../../../..}/contrib/clustering/lib/\" regex=\".*\\.jar\" />\n" +
                                             "  <lib dir=\"${solr.install.dir:../../../..}/dist/\" regex=\"solr-clustering-\\d.*\\.jar\" />\n" +
                                             "\n" +
                                             "  <lib dir=\"${solr.install.dir:../../../..}/contrib/langid/lib/\" regex=\".*\\.jar\" />\n" +
                                             "  <lib dir=\"${solr.install.dir:../../../..}/dist/\" regex=\"solr-langid-\\d.*\\.jar\" />\n" +
                                             "\n" +
                                             "  <lib dir=\"${solr.install.dir:../../../..}/contrib/velocity/lib\" regex=\".*\\.jar\" />\n" +
                                             "  <lib dir=\"${solr.install.dir:../../../..}/dist/\" regex=\"solr-velocity-\\d.*\\.jar\" />\n" +
                                             "  <lib dir=\"${solr.install.dir:../../../..}/dist/\" regex=\"solr-ltr-\\d.*\\.jar\" />\n" +
                                             "\n" +
                                             "  <!-- an exact 'path' can be used instead of a 'dir' to specify a\n" +
                                             "       specific jar file.  This will cause a serious error to be logged\n" +
                                             "       if it can't be loaded.\n" +
                                             "    -->\n" +
                                             "  <!--\n" +
                                             "     <lib path=\"../a-jar-that-does-not-exist.jar\" />\n" +
                                             "  -->\n" +
                                             "\n" +
                                             "  <!-- Data Directory\n" +
                                             "\n" +
                                             "       Used to specify an alternate directory to hold all index data\n" +
                                             "       other than the default ./data under the Solr home.  If\n" +
                                             "       replication is in use, this should match the replication\n" +
                                             "       configuration.\n" +
                                             "    -->\n" +
                                             "  <dataDir>${solr.data.dir:}</dataDir>\n" +
                                             "\n" +
                                             "\n" +
                                             "  <!-- The DirectoryFactory to use for indexes.\n" +
                                             "\n" +
                                             "       solr.StandardDirectoryFactory is filesystem\n" +
                                             "       based and tries to pick the best implementation for the current\n" +
                                             "       JVM and platform.  solr.NRTCachingDirectoryFactory, the default,\n" +
                                             "       wraps solr.StandardDirectoryFactory and caches small files in memory\n" +
                                             "       for better NRT performance.\n" +
                                             "\n" +
                                             "       One can force a particular implementation via solr.MMapDirectoryFactory,\n" +
                                             "       solr.NIOFSDirectoryFactory, or solr.SimpleFSDirectoryFactory.\n" +
                                             "\n" +
                                             "       solr.RAMDirectoryFactory is memory based and not persistent.\n" +
                                             "    -->\n" +
                                             "  <directoryFactory name=\"DirectoryFactory\"\n" +
                                             "                    class=\"${solr.directoryFactory:solr.NRTCachingDirectoryFactory}\"/>\n" +
                                             "\n" +
                                             "  <!-- The CodecFactory for defining the format of the inverted index.\n" +
                                             "       The default implementation is SchemaCodecFactory, which is the official Lucene\n" +
                                             "       index format, but hooks into the schema to provide per-field customization of\n" +
                                             "       the postings lists and per-document values in the fieldType element\n" +
                                             "       (postingsFormat/docValuesFormat). Note that most of the alternative implementations\n" +
                                             "       are experimental, so if you choose to customize the index format, it's a good\n" +
                                             "       idea to convert back to the official format e.g. via IndexWriter.addIndexes(IndexReader)\n" +
                                             "       before upgrading to a newer version to avoid unnecessary reindexing.\n" +
                                             "       A \"compressionMode\" string element can be added to <codecFactory> to choose\n" +
                                             "       between the existing compression modes in the default codec: \"BEST_SPEED\" (default)\n" +
                                             "       or \"BEST_COMPRESSION\".\n" +
                                             "  -->\n" +
                                             "  <codecFactory class=\"solr.SchemaCodecFactory\"/>\n" +
                                             "\n" +
                                             "  <!-- ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                                             "       Index Config - These settings control low-level behavior of indexing\n" +
                                             "       Most example settings here show the default value, but are commented\n" +
                                             "       out, to more easily see where customizations have been made.\n" +
                                             "\n" +
                                             "       Note: This replaces <indexDefaults> and <mainIndex> from older versions\n" +
                                             "       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->\n" +
                                             "  <indexConfig>\n" +
                                             "    <!-- maxFieldLength was removed in 4.0. To get similar behavior, include a\n" +
                                             "         LimitTokenCountFilterFactory in your fieldType definition. E.g.\n" +
                                             "     <filter class=\"solr.LimitTokenCountFilterFactory\" maxTokenCount=\"10000\"/>\n" +
                                             "    -->\n" +
                                             "    <!-- Maximum time to wait for a write lock (ms) for an IndexWriter. Default: 1000 -->\n" +
                                             "    <!-- <writeLockTimeout>1000</writeLockTimeout>  -->\n" +
                                             "\n" +
                                             "    <!-- Expert: Enabling compound file will use less files for the index,\n" +
                                             "         using fewer file descriptors on the expense of performance decrease.\n" +
                                             "         Default in Lucene is \"true\". Default in Solr is \"false\" (since 3.6) -->\n" +
                                             "    <!-- <useCompoundFile>false</useCompoundFile> -->\n" +
                                             "\n" +
                                             "    <!-- ramBufferSizeMB sets the amount of RAM that may be used by Lucene\n" +
                                             "         indexing for buffering added documents and deletions before they are\n" +
                                             "         flushed to the Directory.\n" +
                                             "         maxBufferedDocs sets a limit on the number of documents buffered\n" +
                                             "         before flushing.\n" +
                                             "         If both ramBufferSizeMB and maxBufferedDocs is set, then\n" +
                                             "         Lucene will flush based on whichever limit is hit first.  -->\n" +
                                             "    <!-- <ramBufferSizeMB>100</ramBufferSizeMB> -->\n" +
                                             "    <!-- <maxBufferedDocs>1000</maxBufferedDocs> -->\n" +
                                             "\n" +
                                             "    <!-- Expert: Merge Policy\n" +
                                             "         The Merge Policy in Lucene controls how merging of segments is done.\n" +
                                             "         The default since Solr/Lucene 3.3 is TieredMergePolicy.\n" +
                                             "         The default since Lucene 2.3 was the LogByteSizeMergePolicy,\n" +
                                             "         Even older versions of Lucene used LogDocMergePolicy.\n" +
                                             "      -->\n" +
                                             "    <!--\n" +
                                             "        <mergePolicyFactory class=\"org.apache.solr.index.TieredMergePolicyFactory\">\n" +
                                             "          <int name=\"maxMergeAtOnce\">10</int>\n" +
                                             "          <int name=\"segmentsPerTier\">10</int>\n" +
                                             "          <double name=\"noCFSRatio\">0.1</double>\n" +
                                             "        </mergePolicyFactory>\n" +
                                             "      -->\n" +
                                             "\n" +
                                             "    <!-- Expert: Merge Scheduler\n" +
                                             "         The Merge Scheduler in Lucene controls how merges are\n" +
                                             "         performed.  The ConcurrentMergeScheduler (Lucene 2.3 default)\n" +
                                             "         can perform merges in the background using separate threads.\n" +
                                             "         The SerialMergeScheduler (Lucene 2.2 default) does not.\n" +
                                             "     -->\n" +
                                             "    <!--\n" +
                                             "       <mergeScheduler class=\"org.apache.lucene.index.ConcurrentMergeScheduler\"/>\n" +
                                             "       -->\n" +
                                             "\n" +
                                             "    <!-- LockFactory\n" +
                                             "\n" +
                                             "         This option specifies which Lucene LockFactory implementation\n" +
                                             "         to use.\n" +
                                             "\n" +
                                             "         single = SingleInstanceLockFactory - suggested for a\n" +
                                             "                  read-only index or when there is no possibility of\n" +
                                             "                  another process trying to modify the index.\n" +
                                             "         native = NativeFSLockFactory - uses OS native file locking.\n" +
                                             "                  Do not use when multiple solr webapps in the same\n" +
                                             "                  JVM are attempting to share a single index.\n" +
                                             "         simple = SimpleFSLockFactory  - uses a plain file for locking\n" +
                                             "\n" +
                                             "         Defaults: 'native' is default for Solr3.6 and later, otherwise\n" +
                                             "                   'simple' is the default\n" +
                                             "\n" +
                                             "         More details on the nuances of each LockFactory...\n" +
                                             "         http://wiki.apache.org/lucene-java/AvailableLockFactories\n" +
                                             "    -->\n" +
                                             "    <lockType>${solr.lock.type:native}</lockType>\n" +
                                             "\n" +
                                             "    <!-- Commit Deletion Policy\n" +
                                             "         Custom deletion policies can be specified here. The class must\n" +
                                             "         implement org.apache.lucene.index.IndexDeletionPolicy.\n" +
                                             "\n" +
                                             "         The default Solr IndexDeletionPolicy implementation supports\n" +
                                             "         deleting index commit points on number of commits, age of\n" +
                                             "         commit point and optimized status.\n" +
                                             "\n" +
                                             "         The latest commit point should always be preserved regardless\n" +
                                             "         of the criteria.\n" +
                                             "    -->\n" +
                                             "    <!--\n" +
                                             "    <deletionPolicy class=\"solr.SolrDeletionPolicy\">\n" +
                                             "    -->\n" +
                                             "    <!-- The number of commit points to be kept -->\n" +
                                             "    <!-- <str name=\"maxCommitsToKeep\">1</str> -->\n" +
                                             "    <!-- The number of optimized commit points to be kept -->\n" +
                                             "    <!-- <str name=\"maxOptimizedCommitsToKeep\">0</str> -->\n" +
                                             "    <!--\n" +
                                             "        Delete all commit points once they have reached the given age.\n" +
                                             "        Supports DateMathParser syntax e.g.\n" +
                                             "      -->\n" +
                                             "    <!--\n" +
                                             "       <str name=\"maxCommitAge\">30MINUTES</str>\n" +
                                             "       <str name=\"maxCommitAge\">1DAY</str>\n" +
                                             "    -->\n" +
                                             "    <!--\n" +
                                             "    </deletionPolicy>\n" +
                                             "    -->\n" +
                                             "\n" +
                                             "    <!-- Lucene Infostream\n" +
                                             "\n" +
                                             "         To aid in advanced debugging, Lucene provides an \"InfoStream\"\n" +
                                             "         of detailed information when indexing.\n" +
                                             "\n" +
                                             "         Setting The value to true will instruct the underlying Lucene\n" +
                                             "         IndexWriter to write its debugging info the specified file\n" +
                                             "      -->\n" +
                                             "    <!-- <infoStream file=\"INFOSTREAM.txt\">false</infoStream> -->\n" +
                                             "  </indexConfig>\n" +
                                             "\n" +
                                             "\n" +
//                                             "  <!-- JMX\n" +
//                                             "\n" +
//                                             "       This example enables JMX if and only if an existing MBeanServer\n" +
//                                             "       is found, use this if you want to configure JMX through JVM\n" +
//                                             "       parameters. Remove this to disable exposing Solr configuration\n" +
//                                             "       and statistics to JMX.\n" +
//                                             "\n" +
//                                             "       For more details see http://wiki.apache.org/solr/SolrJmx\n" +
//                                             "    -->\n" +
//                                             "  <jmx />\n" +
//                                             "  <!-- If you want to connect to a particular server, specify the\n" +
//                                             "       agentId\n" +
//                                             "    -->\n" +
//                                             "  <!-- <jmx agentId=\"myAgent\" /> -->\n" +
//                                             "  <!-- If you want to start a new MBeanServer, specify the serviceUrl -->\n" +
//                                             "  <!-- <jmx serviceUrl=\"service:jmx:rmi:///jndi/rmi://localhost:9999/solr\"/>\n" +
//                                             "    -->\n" +
                                             "\n" +
                                             "  <!-- The default high-performance update handler -->\n" +
                                             "  <updateHandler class=\"solr.DirectUpdateHandler2\">\n" +
                                             "\n" +
                                             "    <!-- Enables a transaction log, used for real-time get, durability, and\n" +
                                             "         and solr cloud replica recovery.  The log can grow as big as\n" +
                                             "         uncommitted changes to the index, so use of a hard autoCommit\n" +
                                             "         is recommended (see below).\n" +
                                             "         \"dir\" - the target directory for transaction logs, defaults to the\n" +
                                             "                solr data directory.\n" +
                                             "         \"numVersionBuckets\" - sets the number of buckets used to keep\n" +
                                             "                track of max version values when checking for re-ordered\n" +
                                             "                updates; increase this value to reduce the cost of\n" +
                                             "                synchronizing access to version buckets during high-volume\n" +
                                             "                indexing, this requires 8 bytes (long) * numVersionBuckets\n" +
                                             "                of heap space per Solr core.\n" +
                                             "    -->\n" +
                                             "    <updateLog>\n" +
                                             "      <str name=\"dir\">${solr.ulog.dir:}</str>\n" +
                                             "      <int name=\"numVersionBuckets\">${solr.ulog.numVersionBuckets:65536}</int>\n" +
                                             "    </updateLog>\n" +
                                             "\n" +
                                             "    <!-- AutoCommit\n" +
                                             "\n" +
                                             "         Perform a hard commit automatically under certain conditions.\n" +
                                             "         Instead of enabling autoCommit, consider using \"commitWithin\"\n" +
                                             "         when adding documents.\n" +
                                             "\n" +
                                             "         http://wiki.apache.org/solr/UpdateXmlMessages\n" +
                                             "\n" +
                                             "         maxDocs - Maximum number of documents to add since the last\n" +
                                             "                   commit before automatically triggering a new commit.\n" +
                                             "\n" +
                                             "         maxTime - Maximum amount of time in ms that is allowed to pass\n" +
                                             "                   since a document was added before automatically\n" +
                                             "                   triggering a new commit.\n" +
                                             "         openSearcher - if false, the commit causes recent index changes\n" +
                                             "           to be flushed to stable storage, but does not cause a new\n" +
                                             "           searcher to be opened to make those changes visible.\n" +
                                             "\n" +
                                             "         If the updateLog is enabled, then it's highly recommended to\n" +
                                             "         have some sort of hard autoCommit to limit the log size.\n" +
                                             "      -->\n" +
                                             "    <autoCommit>\n" +
                                             "      <maxTime>${solr.autoCommit.maxTime:15000}</maxTime>\n" +
                                             "      <openSearcher>false</openSearcher>\n" +
                                             "    </autoCommit>\n" +
                                             "\n" +
                                             "    <!-- softAutoCommit is like autoCommit except it causes a\n" +
                                             "         'soft' commit which only ensures that changes are visible\n" +
                                             "         but does not ensure that data is synced to disk.  This is\n" +
                                             "         faster and more near-realtime friendly than a hard commit.\n" +
                                             "      -->\n" +
                                             "\n" +
                                             "    <autoSoftCommit>\n" +
                                             "      <maxTime>${solr.autoSoftCommit.maxTime:-1}</maxTime>\n" +
                                             "    </autoSoftCommit>\n" +
                                             "\n" +
                                             "    <!-- Update Related Event Listeners\n" +
                                             "\n" +
                                             "         Various IndexWriter related events can trigger Listeners to\n" +
                                             "         take actions.\n" +
                                             "\n" +
                                             "         postCommit - fired after every commit or optimize command\n" +
                                             "         postOptimize - fired after every optimize command\n" +
                                             "      -->\n" +
                                             "\n" +
                                             "  </updateHandler>\n" +
                                             "\n" +
                                             "  <!-- IndexReaderFactory\n" +
                                             "\n" +
                                             "       Use the following format to specify a custom IndexReaderFactory,\n" +
                                             "       which allows for alternate IndexReader implementations.\n" +
                                             "\n" +
                                             "       ** Experimental Feature **\n" +
                                             "\n" +
                                             "       Please note - Using a custom IndexReaderFactory may prevent\n" +
                                             "       certain other features from working. The API to\n" +
                                             "       IndexReaderFactory may change without warning or may even be\n" +
                                             "       removed from future releases if the problems cannot be\n" +
                                             "       resolved.\n" +
                                             "\n" +
                                             "\n" +
                                             "       ** Features that may not work with custom IndexReaderFactory **\n" +
                                             "\n" +
                                             "       The ReplicationHandler assumes a disk-resident index. Using a\n" +
                                             "       custom IndexReader implementation may cause incompatibility\n" +
                                             "       with ReplicationHandler and may cause replication to not work\n" +
                                             "       correctly. See SOLR-1366 for details.\n" +
                                             "\n" +
                                             "    -->\n" +
                                             "  <!--\n" +
                                             "  <indexReaderFactory name=\"IndexReaderFactory\" class=\"package.class\">\n" +
                                             "    <str name=\"someArg\">Some Value</str>\n" +
                                             "  </indexReaderFactory >\n" +
                                             "  -->\n" +
                                             "\n" +
                                             "  <!-- ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                                             "       Query section - these settings control query time things like caches\n" +
                                             "       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ -->\n" +
                                             "  <query>\n" +
                                             "\n" +
                                             "    <!-- Maximum number of clauses in each BooleanQuery,  an exception\n" +
                                             "         is thrown if exceeded.  It is safe to increase or remove this setting,\n" +
                                             "         since it is purely an arbitrary limit to try and catch user errors where\n" +
                                             "         large boolean queries may not be the best implementation choice.\n" +
                                             "      -->\n" +
                                             "    <maxBooleanClauses>${solr.max.booleanClauses:1024}</maxBooleanClauses>\n" +
                                             "\n" +
                                             "    <!-- Solr Internal Query Caches\n" +
                                             "\n" +
                                             "         There are two implementations of cache available for Solr,\n" +
                                             "         LRUCache, based on a synchronized LinkedHashMap, and\n" +
                                             "         FastLRUCache, based on a ConcurrentHashMap.\n" +
                                             "\n" +
                                             "         FastLRUCache has faster gets and slower puts in single\n" +
                                             "         threaded operation and thus is generally faster than LRUCache\n" +
                                             "         when the hit ratio of the cache is high (> 75%), and may be\n" +
                                             "         faster under other scenarios on multi-cpu systems.\n" +
                                             "    -->\n" +
                                             "\n" +
                                             "    <!-- Filter Cache\n" +
                                             "\n" +
                                             "         Cache used by SolrIndexSearcher for filters (DocSets),\n" +
                                             "         unordered sets of *all* documents that match a query.  When a\n" +
                                             "         new searcher is opened, its caches may be prepopulated or\n" +
                                             "         \"autowarmed\" using data from caches in the old searcher.\n" +
                                             "         autowarmCount is the number of items to prepopulate.  For\n" +
                                             "         LRUCache, the autowarmed items will be the most recently\n" +
                                             "         accessed items.\n" +
                                             "\n" +
                                             "         Parameters:\n" +
                                             "           class - the SolrCache implementation LRUCache or\n" +
                                             "               (LRUCache or FastLRUCache)\n" +
                                             "           size - the maximum number of entries in the cache\n" +
                                             "           initialSize - the initial capacity (number of entries) of\n" +
                                             "               the cache.  (see java.util.HashMap)\n" +
                                             "           autowarmCount - the number of entries to prepopulate from\n" +
                                             "               and old cache.\n" +
                                             "           maxRamMB - the maximum amount of RAM (in MB) that this cache is allowed\n" +
                                             "                      to occupy. Note that when this option is specified, the size\n" +
                                             "                      and initialSize parameters are ignored.\n" +
                                             "      -->\n" +
                                             "    <filterCache class=\"solr.FastLRUCache\"\n" +
                                             "                 size=\"512\"\n" +
                                             "                 initialSize=\"512\"\n" +
                                             "                 autowarmCount=\"0\"/>\n" +
                                             "\n" +
                                             "    <!-- Query Result Cache\n" +
                                             "\n" +
                                             "         Caches results of searches - ordered lists of document ids\n" +
                                             "         (DocList) based on a query, a sort, and the range of documents requested.\n" +
                                             "         Additional supported parameter by LRUCache:\n" +
                                             "            maxRamMB - the maximum amount of RAM (in MB) that this cache is allowed\n" +
                                             "                       to occupy\n" +
                                             "      -->\n" +
                                             "    <queryResultCache class=\"solr.LRUCache\"\n" +
                                             "                      size=\"512\"\n" +
                                             "                      initialSize=\"512\"\n" +
                                             "                      autowarmCount=\"0\"/>\n" +
                                             "\n" +
                                             "    <!-- Document Cache\n" +
                                             "\n" +
                                             "         Caches Lucene Document objects (the stored fields for each\n" +
                                             "         document).  Since Lucene internal document ids are transient,\n" +
                                             "         this cache will not be autowarmed.\n" +
                                             "      -->\n" +
                                             "    <documentCache class=\"solr.LRUCache\"\n" +
                                             "                   size=\"512\"\n" +
                                             "                   initialSize=\"512\"\n" +
                                             "                   autowarmCount=\"0\"/>\n" +
                                             "\n" +
                                             "    <!-- custom cache currently used by block join -->\n" +
                                             "    <cache name=\"perSegFilter\"\n" +
                                             "           class=\"solr.search.LRUCache\"\n" +
                                             "           size=\"10\"\n" +
                                             "           initialSize=\"0\"\n" +
                                             "           autowarmCount=\"10\"\n" +
                                             "           regenerator=\"solr.NoOpRegenerator\" />\n" +
                                             "\n" +
                                             "    <!-- Field Value Cache\n" +
                                             "\n" +
                                             "         Cache used to hold field values that are quickly accessible\n" +
                                             "         by document id.  The fieldValueCache is created by default\n" +
                                             "         even if not configured here.\n" +
                                             "      -->\n" +
                                             "    <!--\n" +
                                             "       <fieldValueCache class=\"solr.FastLRUCache\"\n" +
                                             "                        size=\"512\"\n" +
                                             "                        autowarmCount=\"128\"\n" +
                                             "                        showItems=\"32\" />\n" +
                                             "      -->\n" +
                                             "\n" +
                                             "    <!-- Custom Cache\n" +
                                             "\n" +
                                             "         Example of a generic cache.  These caches may be accessed by\n" +
                                             "         name through SolrIndexSearcher.getCache(),cacheLookup(), and\n" +
                                             "         cacheInsert().  The purpose is to enable easy caching of\n" +
                                             "         user/application level data.  The regenerator argument should\n" +
                                             "         be specified as an implementation of solr.CacheRegenerator\n" +
                                             "         if autowarming is desired.\n" +
                                             "      -->\n" +
                                             "    <!--\n" +
                                             "       <cache name=\"myUserCache\"\n" +
                                             "              class=\"solr.LRUCache\"\n" +
                                             "              size=\"4096\"\n" +
                                             "              initialSize=\"1024\"\n" +
                                             "              autowarmCount=\"1024\"\n" +
                                             "              regenerator=\"com.mycompany.MyRegenerator\"\n" +
                                             "              />\n" +
                                             "      -->\n" +
                                             "\n" +
                                             "\n" +
                                             "    <!-- Lazy Field Loading\n" +
                                             "\n" +
                                             "         If true, stored fields that are not requested will be loaded\n" +
                                             "         lazily.  This can result in a significant speed improvement\n" +
                                             "         if the usual case is to not load all stored fields,\n" +
                                             "         especially if the skipped fields are large compressed text\n" +
                                             "         fields.\n" +
                                             "    -->\n" +
                                             "    <enableLazyFieldLoading>true</enableLazyFieldLoading>\n" +
                                             "\n" +
                                             "    <!-- Use Filter For Sorted Query\n" +
                                             "\n" +
                                             "         A possible optimization that attempts to use a filter to\n" +
                                             "         satisfy a search.  If the requested sort does not include\n" +
                                             "         score, then the filterCache will be checked for a filter\n" +
                                             "         matching the query. If found, the filter will be used as the\n" +
                                             "         source of document ids, and then the sort will be applied to\n" +
                                             "         that.\n" +
                                             "\n" +
                                             "         For most situations, this will not be useful unless you\n" +
                                             "         frequently get the same search repeatedly with different sort\n" +
                                             "         options, and none of them ever use \"score\"\n" +
                                             "      -->\n" +
                                             "    <!--\n" +
                                             "       <useFilterForSortedQuery>true</useFilterForSortedQuery>\n" +
                                             "      -->\n" +
                                             "\n" +
                                             "    <!-- Result Window Size\n" +
                                             "\n" +
                                             "         An optimization for use with the queryResultCache.  When a search\n" +
                                             "         is requested, a superset of the requested number of document ids\n" +
                                             "         are collected.  For example, if a search for a particular query\n" +
                                             "         requests matching documents 10 through 19, and queryWindowSize is 50,\n" +
                                             "         then documents 0 through 49 will be collected and cached.  Any further\n" +
                                             "         requests in that range can be satisfied via the cache.\n" +
                                             "      -->\n" +
                                             "    <queryResultWindowSize>20</queryResultWindowSize>\n" +
                                             "\n" +
                                             "    <!-- Maximum number of documents to cache for any entry in the\n" +
                                             "         queryResultCache.\n" +
                                             "      -->\n" +
                                             "    <queryResultMaxDocsCached>200</queryResultMaxDocsCached>\n" +
                                             "\n" +
                                             "    <!-- Query Related Event Listeners\n" +
                                             "\n" +
                                             "         Various IndexSearcher related events can trigger Listeners to\n" +
                                             "         take actions.\n" +
                                             "\n" +
                                             "         newSearcher - fired whenever a new searcher is being prepared\n" +
                                             "         and there is a current searcher handling requests (aka\n" +
                                             "         registered).  It can be used to prime certain caches to\n" +
                                             "         prevent long request times for certain requests.\n" +
                                             "\n" +
                                             "         firstSearcher - fired whenever a new searcher is being\n" +
                                             "         prepared but there is no current registered searcher to handle\n" +
                                             "         requests or to gain autowarming data from.\n" +
                                             "\n" +
                                             "\n" +
                                             "      -->\n" +
                                             "    <!-- QuerySenderListener takes an array of NamedList and executes a\n" +
                                             "         local query request for each NamedList in sequence.\n" +
                                             "      -->\n" +
                                             "    <listener event=\"newSearcher\" class=\"solr.QuerySenderListener\">\n" +
                                             "      <arr name=\"queries\">\n" +
                                             "        <!--\n" +
                                             "           <lst><str name=\"q\">solr</str><str name=\"sort\">price asc</str></lst>\n" +
                                             "           <lst><str name=\"q\">rocks</str><str name=\"sort\">weight asc</str></lst>\n" +
                                             "          -->\n" +
                                             "      </arr>\n" +
                                             "    </listener>\n" +
                                             "    <listener event=\"firstSearcher\" class=\"solr.QuerySenderListener\">\n" +
                                             "      <arr name=\"queries\">\n" +
                                             "        <!--\n" +
                                             "        <lst>\n" +
                                             "          <str name=\"q\">static firstSearcher warming in solrconfig.xml</str>\n" +
                                             "        </lst>\n" +
                                             "        -->\n" +
                                             "      </arr>\n" +
                                             "    </listener>\n" +
                                             "\n" +
                                             "    <!-- Use Cold Searcher\n" +
                                             "\n" +
                                             "         If a search request comes in and there is no current\n" +
                                             "         registered searcher, then immediately register the still\n" +
                                             "         warming searcher and use it.  If \"false\" then all requests\n" +
                                             "         will block until the first searcher is done warming.\n" +
                                             "      -->\n" +
                                             "    <useColdSearcher>false</useColdSearcher>\n" +
                                             "\n" +
                                             "  </query>\n" +
                                             "\n" +
                                             "\n" +
                                             "  <!-- Request Dispatcher\n" +
                                             "\n" +
                                             "       This section contains instructions for how the SolrDispatchFilter\n" +
                                             "       should behave when processing requests for this SolrCore.\n" +
                                             "\n" +
                                             "    -->\n" +
                                             "  <requestDispatcher>\n" +
                                             "    <!-- Request Parsing\n" +
                                             "\n" +
                                             "         These settings indicate how Solr Requests may be parsed, and\n" +
                                             "         what restrictions may be placed on the ContentStreams from\n" +
                                             "         those requests\n" +
                                             "\n" +
                                             "         enableRemoteStreaming - enables use of the stream.file\n" +
                                             "         and stream.url parameters for specifying remote streams.\n" +
                                             "\n" +
                                             "         multipartUploadLimitInKB - specifies the max size (in KiB) of\n" +
                                             "         Multipart File Uploads that Solr will allow in a Request.\n" +
                                             "\n" +
                                             "         formdataUploadLimitInKB - specifies the max size (in KiB) of\n" +
                                             "         form data (application/x-www-form-urlencoded) sent via\n" +
                                             "         POST. You can use POST to pass request parameters not\n" +
                                             "         fitting into the URL.\n" +
                                             "\n" +
                                             "         addHttpRequestToContext - if set to true, it will instruct\n" +
                                             "         the requestParsers to include the original HttpServletRequest\n" +
                                             "         object in the context map of the SolrQueryRequest under the\n" +
                                             "         key \"httpRequest\". It will not be used by any of the existing\n" +
                                             "         Solr components, but may be useful when developing custom\n" +
                                             "         plugins.\n" +
                                             "\n" +
                                             "         *** WARNING ***\n" +
                                             "         Before enabling remote streaming, you should make sure your\n" +
                                             "         system has authentication enabled.\n" +
                                             "\n" +
                                             "    <requestParsers enableRemoteStreaming=\"false\"\n" +
                                             "                    multipartUploadLimitInKB=\"-1\"\n" +
                                             "                    formdataUploadLimitInKB=\"-1\"\n" +
                                             "                    addHttpRequestToContext=\"false\"/>\n" +
                                             "      -->\n" +
                                             "\n" +
                                             "    <!-- HTTP Caching\n" +
                                             "\n" +
                                             "         Set HTTP caching related parameters (for proxy caches and clients).\n" +
                                             "\n" +
                                             "         The options below instruct Solr not to output any HTTP Caching\n" +
                                             "         related headers\n" +
                                             "      -->\n" +
                                             "    <httpCaching never304=\"true\" />\n" +
                                             "    <!-- If you include a <cacheControl> directive, it will be used to\n" +
                                             "         generate a Cache-Control header (as well as an Expires header\n" +
                                             "         if the value contains \"max-age=\")\n" +
                                             "\n" +
                                             "         By default, no Cache-Control header is generated.\n" +
                                             "\n" +
                                             "         You can use the <cacheControl> option even if you have set\n" +
                                             "         never304=\"true\"\n" +
                                             "      -->\n" +
                                             "    <!--\n" +
                                             "       <httpCaching never304=\"true\" >\n" +
                                             "         <cacheControl>max-age=30, public</cacheControl>\n" +
                                             "       </httpCaching>\n" +
                                             "      -->\n" +
                                             "    <!-- To enable Solr to respond with automatically generated HTTP\n" +
                                             "         Caching headers, and to response to Cache Validation requests\n" +
                                             "         correctly, set the value of never304=\"false\"\n" +
                                             "\n" +
                                             "         This will cause Solr to generate Last-Modified and ETag\n" +
                                             "         headers based on the properties of the Index.\n" +
                                             "\n" +
                                             "         The following options can also be specified to affect the\n" +
                                             "         values of these headers...\n" +
                                             "\n" +
                                             "         lastModFrom - the default value is \"openTime\" which means the\n" +
                                             "         Last-Modified value (and validation against If-Modified-Since\n" +
                                             "         requests) will all be relative to when the current Searcher\n" +
                                             "         was opened.  You can change it to lastModFrom=\"dirLastMod\" if\n" +
                                             "         you want the value to exactly correspond to when the physical\n" +
                                             "         index was last modified.\n" +
                                             "\n" +
                                             "         etagSeed=\"...\" is an option you can change to force the ETag\n" +
                                             "         header (and validation against If-None-Match requests) to be\n" +
                                             "         different even if the index has not changed (ie: when making\n" +
                                             "         significant changes to your config file)\n" +
                                             "\n" +
                                             "         (lastModifiedFrom and etagSeed are both ignored if you use\n" +
                                             "         the never304=\"true\" option)\n" +
                                             "      -->\n" +
                                             "    <!--\n" +
                                             "       <httpCaching lastModifiedFrom=\"openTime\"\n" +
                                             "                    etagSeed=\"Solr\">\n" +
                                             "         <cacheControl>max-age=30, public</cacheControl>\n" +
                                             "       </httpCaching>\n" +
                                             "      -->\n" +
                                             "  </requestDispatcher>\n" +
                                             "\n" +
                                             "  <!-- Request Handlers\n" +
                                             "\n" +
                                             "       http://wiki.apache.org/solr/SolrRequestHandler\n" +
                                             "\n" +
                                             "       Incoming queries will be dispatched to a specific handler by name\n" +
                                             "       based on the path specified in the request.\n" +
                                             "\n" +
                                             "       If a Request Handler is declared with startup=\"lazy\", then it will\n" +
                                             "       not be initialized until the first request that uses it.\n" +
                                             "\n" +
                                             "    -->\n" +
                                             "  <!-- SearchHandler\n" +
                                             "\n" +
                                             "       http://wiki.apache.org/solr/SearchHandler\n" +
                                             "\n" +
                                             "       For processing Search Queries, the primary Request Handler\n" +
                                             "       provided with Solr is \"SearchHandler\" It delegates to a sequent\n" +
                                             "       of SearchComponents (see below) and supports distributed\n" +
                                             "       queries across multiple shards\n" +
                                             "    -->\n" +
                                             "  <requestHandler name=\"/select\" class=\"solr.SearchHandler\">\n" +
                                             "    <!-- default values for query parameters can be specified, these\n" +
                                             "         will be overridden by parameters in the request\n" +
                                             "      -->\n" +
                                             "    <lst name=\"defaults\">\n" +
                                             "      <str name=\"echoParams\">explicit</str>\n" +
                                             "      <int name=\"rows\">10</int>\n" +
                                             "      <!-- Default search field\n" +
                                             "         <str name=\"df\">text</str> \n" +
                                             "        -->\n" +
                                             "      <!-- Change from JSON to XML format (the default prior to Solr 7.0)\n" +
                                             "         <str name=\"wt\">xml</str> \n" +
                                             "        -->\n" +
                                             "    </lst>\n" +
                                             "    <!-- In addition to defaults, \"appends\" params can be specified\n" +
                                             "         to identify values which should be appended to the list of\n" +
                                             "         multi-val params from the query (or the existing \"defaults\").\n" +
                                             "      -->\n" +
                                             "    <!-- In this example, the param \"fq=instock:true\" would be appended to\n" +
                                             "         any query time fq params the user may specify, as a mechanism for\n" +
                                             "         partitioning the index, independent of any user selected filtering\n" +
                                             "         that may also be desired (perhaps as a result of faceted searching).\n" +
                                             "\n" +
                                             "         NOTE: there is *absolutely* nothing a client can do to prevent these\n" +
                                             "         \"appends\" values from being used, so don't use this mechanism\n" +
                                             "         unless you are sure you always want it.\n" +
                                             "      -->\n" +
                                             "    <!--\n" +
                                             "       <lst name=\"appends\">\n" +
                                             "         <str name=\"fq\">inStock:true</str>\n" +
                                             "       </lst>\n" +
                                             "      -->\n" +
                                             "    <!-- \"invariants\" are a way of letting the Solr maintainer lock down\n" +
                                             "         the options available to Solr clients.  Any params values\n" +
                                             "         specified here are used regardless of what values may be specified\n" +
                                             "         in either the query, the \"defaults\", or the \"appends\" params.\n" +
                                             "\n" +
                                             "         In this example, the facet.field and facet.query params would\n" +
                                             "         be fixed, limiting the facets clients can use.  Faceting is\n" +
                                             "         not turned on by default - but if the client does specify\n" +
                                             "         facet=true in the request, these are the only facets they\n" +
                                             "         will be able to see counts for; regardless of what other\n" +
                                             "         facet.field or facet.query params they may specify.\n" +
                                             "\n" +
                                             "         NOTE: there is *absolutely* nothing a client can do to prevent these\n" +
                                             "         \"invariants\" values from being used, so don't use this mechanism\n" +
                                             "         unless you are sure you always want it.\n" +
                                             "      -->\n" +
                                             "    <!--\n" +
                                             "       <lst name=\"invariants\">\n" +
                                             "         <str name=\"facet.field\">cat</str>\n" +
                                             "         <str name=\"facet.field\">manu_exact</str>\n" +
                                             "         <str name=\"facet.query\">price:[* TO 500]</str>\n" +
                                             "         <str name=\"facet.query\">price:[500 TO *]</str>\n" +
                                             "       </lst>\n" +
                                             "      -->\n" +
                                             "    <!-- If the default list of SearchComponents is not desired, that\n" +
                                             "         list can either be overridden completely, or components can be\n" +
                                             "         prepended or appended to the default list.  (see below)\n" +
                                             "      -->\n" +
                                             "    <!--\n" +
                                             "       <arr name=\"components\">\n" +
                                             "         <str>nameOfCustomComponent1</str>\n" +
                                             "         <str>nameOfCustomComponent2</str>\n" +
                                             "       </arr>\n" +
                                             "      -->\n" +
                                             "  </requestHandler>\n" +
                                             "\n" +
                                             "  <!-- A request handler that returns indented JSON by default -->\n" +
                                             "  <requestHandler name=\"/query\" class=\"solr.SearchHandler\">\n" +
                                             "    <lst name=\"defaults\">\n" +
                                             "      <str name=\"echoParams\">explicit</str>\n" +
                                             "      <str name=\"wt\">json</str>\n" +
                                             "      <str name=\"indent\">true</str>\n" +
                                             "    </lst>\n" +
                                             "  </requestHandler>\n" +
                                             "\n" +
                                             "\n" +
                                             "  <!-- A Robust Example\n" +
                                             "\n" +
                                             "       This example SearchHandler declaration shows off usage of the\n" +
                                             "       SearchHandler with many defaults declared\n" +
                                             "\n" +
                                             "       Note that multiple instances of the same Request Handler\n" +
                                             "       (SearchHandler) can be registered multiple times with different\n" +
                                             "       names (and different init parameters)\n" +
                                             "    -->\n" +
                                             "  <requestHandler name=\"/browse\" class=\"solr.SearchHandler\" useParams=\"query,facets,velocity,browse\">\n" +
                                             "    <lst name=\"defaults\">\n" +
                                             "      <str name=\"echoParams\">explicit</str>\n" +
                                             "    </lst>\n" +
                                             "  </requestHandler>\n" +
                                             "\n" +
                                             "  <initParams path=\"/update/**,/query,/select,/tvrh,/elevate,/spell,/browse\">\n" +
                                             "    <lst name=\"defaults\">\n" +
                                             "      <str name=\"df\">_text_</str>\n" +
                                             "    </lst>\n" +
                                             "  </initParams>\n" +
                                             "\n" +
                                             "  <!-- Solr Cell Update Request Handler\n" +
                                             "\n" +
                                             "       http://wiki.apache.org/solr/ExtractingRequestHandler\n" +
                                             "\n" +
                                             "    -->\n" +
                                             "  <requestHandler name=\"/update/extract\"\n" +
                                             "                  startup=\"lazy\"\n" +
                                             "                  class=\"solr.extraction.ExtractingRequestHandler\" >\n" +
                                             "    <lst name=\"defaults\">\n" +
                                             "      <str name=\"lowernames\">true</str>\n" +
                                             "      <str name=\"fmap.content\">_text_</str>\n" +
                                             "    </lst>\n" +
                                             "  </requestHandler>\n" +
                                             "\n" +
                                             "  <!-- Search Components\n" +
                                             "\n" +
                                             "       Search components are registered to SolrCore and used by\n" +
                                             "       instances of SearchHandler (which can access them by name)\n" +
                                             "\n" +
                                             "       By default, the following components are available:\n" +
                                             "\n" +
                                             "       <searchComponent name=\"query\"     class=\"solr.QueryComponent\" />\n" +
                                             "       <searchComponent name=\"facet\"     class=\"solr.FacetComponent\" />\n" +
                                             "       <searchComponent name=\"mlt\"       class=\"solr.MoreLikeThisComponent\" />\n" +
                                             "       <searchComponent name=\"highlight\" class=\"solr.HighlightComponent\" />\n" +
                                             "       <searchComponent name=\"stats\"     class=\"solr.StatsComponent\" />\n" +
                                             "       <searchComponent name=\"debug\"     class=\"solr.DebugComponent\" />\n" +
                                             "\n" +
                                             "       Default configuration in a requestHandler would look like:\n" +
                                             "\n" +
                                             "       <arr name=\"components\">\n" +
                                             "         <str>query</str>\n" +
                                             "         <str>facet</str>\n" +
                                             "         <str>mlt</str>\n" +
                                             "         <str>highlight</str>\n" +
                                             "         <str>stats</str>\n" +
                                             "         <str>debug</str>\n" +
                                             "       </arr>\n" +
                                             "\n" +
                                             "       If you register a searchComponent to one of the standard names,\n" +
                                             "       that will be used instead of the default.\n" +
                                             "\n" +
                                             "       To insert components before or after the 'standard' components, use:\n" +
                                             "\n" +
                                             "       <arr name=\"first-components\">\n" +
                                             "         <str>myFirstComponentName</str>\n" +
                                             "       </arr>\n" +
                                             "\n" +
                                             "       <arr name=\"last-components\">\n" +
                                             "         <str>myLastComponentName</str>\n" +
                                             "       </arr>\n" +
                                             "\n" +
                                             "       NOTE: The component registered with the name \"debug\" will\n" +
                                             "       always be executed after the \"last-components\"\n" +
                                             "\n" +
                                             "     -->\n" +
                                             "\n" +
                                             "  <!-- Spell Check\n" +
                                             "\n" +
                                             "       The spell check component can return a list of alternative spelling\n" +
                                             "       suggestions.\n" +
                                             "\n" +
                                             "       http://wiki.apache.org/solr/SpellCheckComponent\n" +
                                             "    -->\n" +
                                             "  <searchComponent name=\"spellcheck\" class=\"solr.SpellCheckComponent\">\n" +
                                             "\n" +
                                             "    <str name=\"queryAnalyzerFieldType\">text_general</str>\n" +
                                             "\n" +
                                             "    <!-- Multiple \"Spell Checkers\" can be declared and used by this\n" +
                                             "         component\n" +
                                             "      -->\n" +
                                             "\n" +
                                             "    <!-- a spellchecker built from a field of the main index -->\n" +
                                             "    <lst name=\"spellchecker\">\n" +
                                             "      <str name=\"name\">default</str>\n" +
                                             "      <str name=\"field\">_text_</str>\n" +
                                             "      <str name=\"classname\">solr.DirectSolrSpellChecker</str>\n" +
                                             "      <!-- the spellcheck distance measure used, the default is the internal levenshtein -->\n" +
                                             "      <str name=\"distanceMeasure\">internal</str>\n" +
                                             "      <!-- minimum accuracy needed to be considered a valid spellcheck suggestion -->\n" +
                                             "      <float name=\"accuracy\">0.5</float>\n" +
                                             "      <!-- the maximum #edits we consider when enumerating terms: can be 1 or 2 -->\n" +
                                             "      <int name=\"maxEdits\">2</int>\n" +
                                             "      <!-- the minimum shared prefix when enumerating terms -->\n" +
                                             "      <int name=\"minPrefix\">1</int>\n" +
                                             "      <!-- maximum number of inspections per result. -->\n" +
                                             "      <int name=\"maxInspections\">5</int>\n" +
                                             "      <!-- minimum length of a query term to be considered for correction -->\n" +
                                             "      <int name=\"minQueryLength\">4</int>\n" +
                                             "      <!-- maximum threshold of documents a query term can appear to be considered for correction -->\n" +
                                             "      <float name=\"maxQueryFrequency\">0.01</float>\n" +
                                             "      <!-- uncomment this to require suggestions to occur in 1% of the documents\n" +
                                             "        <float name=\"thresholdTokenFrequency\">.01</float>\n" +
                                             "      -->\n" +
                                             "    </lst>\n" +
                                             "\n" +
                                             "    <!-- a spellchecker that can break or combine words.  See \"/spell\" handler below for usage -->\n" +
                                             "    <!--\n" +
                                             "    <lst name=\"spellchecker\">\n" +
                                             "      <str name=\"name\">wordbreak</str>\n" +
                                             "      <str name=\"classname\">solr.WordBreakSolrSpellChecker</str>\n" +
                                             "      <str name=\"field\">name</str>\n" +
                                             "      <str name=\"combineWords\">true</str>\n" +
                                             "      <str name=\"breakWords\">true</str>\n" +
                                             "      <int name=\"maxChanges\">10</int>\n" +
                                             "    </lst>\n" +
                                             "    -->\n" +
                                             "  </searchComponent>\n" +
                                             "\n" +
                                             "  <!-- A request handler for demonstrating the spellcheck component.\n" +
                                             "\n" +
                                             "       NOTE: This is purely as an example.  The whole purpose of the\n" +
                                             "       SpellCheckComponent is to hook it into the request handler that\n" +
                                             "       handles your normal user queries so that a separate request is\n" +
                                             "       not needed to get suggestions.\n" +
                                             "\n" +
                                             "       IN OTHER WORDS, THERE IS REALLY GOOD CHANCE THE SETUP BELOW IS\n" +
                                             "       NOT WHAT YOU WANT FOR YOUR PRODUCTION SYSTEM!\n" +
                                             "\n" +
                                             "       See http://wiki.apache.org/solr/SpellCheckComponent for details\n" +
                                             "       on the request parameters.\n" +
                                             "    -->\n" +
                                             "  <requestHandler name=\"/spell\" class=\"solr.SearchHandler\" startup=\"lazy\">\n" +
                                             "    <lst name=\"defaults\">\n" +
                                             "      <!-- Solr will use suggestions from both the 'default' spellchecker\n" +
                                             "           and from the 'wordbreak' spellchecker and combine them.\n" +
                                             "           collations (re-written queries) can include a combination of\n" +
                                             "           corrections from both spellcheckers -->\n" +
                                             "      <str name=\"spellcheck.dictionary\">default</str>\n" +
                                             "      <str name=\"spellcheck\">on</str>\n" +
                                             "      <str name=\"spellcheck.extendedResults\">true</str>\n" +
                                             "      <str name=\"spellcheck.count\">10</str>\n" +
                                             "      <str name=\"spellcheck.alternativeTermCount\">5</str>\n" +
                                             "      <str name=\"spellcheck.maxResultsForSuggest\">5</str>\n" +
                                             "      <str name=\"spellcheck.collate\">true</str>\n" +
                                             "      <str name=\"spellcheck.collateExtendedResults\">true</str>\n" +
                                             "      <str name=\"spellcheck.maxCollationTries\">10</str>\n" +
                                             "      <str name=\"spellcheck.maxCollations\">5</str>\n" +
                                             "    </lst>\n" +
                                             "    <arr name=\"last-components\">\n" +
                                             "      <str>spellcheck</str>\n" +
                                             "    </arr>\n" +
                                             "  </requestHandler>\n" +
                                             "\n" +
                                             "  <!-- Term Vector Component\n" +
                                             "\n" +
                                             "       http://wiki.apache.org/solr/TermVectorComponent\n" +
                                             "    -->\n" +
                                             "  <searchComponent name=\"tvComponent\" class=\"solr.TermVectorComponent\"/>\n" +
                                             "\n" +
                                             "  <!-- A request handler for demonstrating the term vector component\n" +
                                             "\n" +
                                             "       This is purely as an example.\n" +
                                             "\n" +
                                             "       In reality you will likely want to add the component to your\n" +
                                             "       already specified request handlers.\n" +
                                             "    -->\n" +
                                             "  <requestHandler name=\"/tvrh\" class=\"solr.SearchHandler\" startup=\"lazy\">\n" +
                                             "    <lst name=\"defaults\">\n" +
                                             "      <bool name=\"tv\">true</bool>\n" +
                                             "    </lst>\n" +
                                             "    <arr name=\"last-components\">\n" +
                                             "      <str>tvComponent</str>\n" +
                                             "    </arr>\n" +
                                             "  </requestHandler>\n" +
                                             "\n" +
                                             "  <!-- Clustering Component. (Omitted here. See the default Solr example for a typical configuration.) -->\n" +
                                             "\n" +
                                             "  <!-- Terms Component\n" +
                                             "\n" +
                                             "       http://wiki.apache.org/solr/TermsComponent\n" +
                                             "\n" +
                                             "       A component to return terms and document frequency of those\n" +
                                             "       terms\n" +
                                             "    -->\n" +
                                             "  <searchComponent name=\"terms\" class=\"solr.TermsComponent\"/>\n" +
                                             "\n" +
                                             "  <!-- A request handler for demonstrating the terms component -->\n" +
                                             "  <requestHandler name=\"/terms\" class=\"solr.SearchHandler\" startup=\"lazy\">\n" +
                                             "    <lst name=\"defaults\">\n" +
                                             "      <bool name=\"terms\">true</bool>\n" +
                                             "      <bool name=\"distrib\">false</bool>\n" +
                                             "    </lst>\n" +
                                             "    <arr name=\"components\">\n" +
                                             "      <str>terms</str>\n" +
                                             "    </arr>\n" +
                                             "  </requestHandler>\n" +
                                             "\n" +
                                             "\n" +
                                             "  <!-- Query Elevation Component\n" +
                                             "\n" +
                                             "       http://wiki.apache.org/solr/QueryElevationComponent\n" +
                                             "\n" +
                                             "       a search component that enables you to configure the top\n" +
                                             "       results for a given query regardless of the normal lucene\n" +
                                             "       scoring.\n" +
                                             "    -->\n" +
                                             "  <searchComponent name=\"elevator\" class=\"solr.QueryElevationComponent\" >\n" +
                                             "    <!-- pick a fieldType to analyze queries -->\n" +
                                             "    <str name=\"queryFieldType\">string</str>\n" +
                                             "  </searchComponent>\n" +
                                             "\n" +
                                             "  <!-- A request handler for demonstrating the elevator component -->\n" +
                                             "  <requestHandler name=\"/elevate\" class=\"solr.SearchHandler\" startup=\"lazy\">\n" +
                                             "    <lst name=\"defaults\">\n" +
                                             "      <str name=\"echoParams\">explicit</str>\n" +
                                             "    </lst>\n" +
                                             "    <arr name=\"last-components\">\n" +
                                             "      <str>elevator</str>\n" +
                                             "    </arr>\n" +
                                             "  </requestHandler>\n" +
                                             "\n" +
                                             "  <!-- Highlighting Component\n" +
                                             "\n" +
                                             "       http://wiki.apache.org/solr/HighlightingParameters\n" +
                                             "    -->\n" +
                                             "  <searchComponent class=\"solr.HighlightComponent\" name=\"highlight\">\n" +
                                             "    <highlighting>\n" +
                                             "      <!-- Configure the standard fragmenter -->\n" +
                                             "      <!-- This could most likely be commented out in the \"default\" case -->\n" +
                                             "      <fragmenter name=\"gap\"\n" +
                                             "                  default=\"true\"\n" +
                                             "                  class=\"solr.highlight.GapFragmenter\">\n" +
                                             "        <lst name=\"defaults\">\n" +
                                             "          <int name=\"hl.fragsize\">100</int>\n" +
                                             "        </lst>\n" +
                                             "      </fragmenter>\n" +
                                             "\n" +
                                             "      <!-- A regular-expression-based fragmenter\n" +
                                             "           (for sentence extraction)\n" +
                                             "        -->\n" +
                                             "      <fragmenter name=\"regex\"\n" +
                                             "                  class=\"solr.highlight.RegexFragmenter\">\n" +
                                             "        <lst name=\"defaults\">\n" +
                                             "          <!-- slightly smaller fragsizes work better because of slop -->\n" +
                                             "          <int name=\"hl.fragsize\">70</int>\n" +
                                             "          <!-- allow 50% slop on fragment sizes -->\n" +
                                             "          <float name=\"hl.regex.slop\">0.5</float>\n" +
                                             "          <!-- a basic sentence pattern -->\n" +
                                             "          <str name=\"hl.regex.pattern\">[-\\w ,/\\n\\&quot;&apos;]{20,200}</str>\n" +
                                             "        </lst>\n" +
                                             "      </fragmenter>\n" +
                                             "\n" +
                                             "      <!-- Configure the standard formatter -->\n" +
                                             "      <formatter name=\"html\"\n" +
                                             "                 default=\"true\"\n" +
                                             "                 class=\"solr.highlight.HtmlFormatter\">\n" +
                                             "        <lst name=\"defaults\">\n" +
                                             "          <str name=\"hl.simple.pre\"><![CDATA[<em>]]></str>\n" +
                                             "          <str name=\"hl.simple.post\"><![CDATA[</em>]]></str>\n" +
                                             "        </lst>\n" +
                                             "      </formatter>\n" +
                                             "\n" +
                                             "      <!-- Configure the standard encoder -->\n" +
                                             "      <encoder name=\"html\"\n" +
                                             "               class=\"solr.highlight.HtmlEncoder\" />\n" +
                                             "\n" +
                                             "      <!-- Configure the standard fragListBuilder -->\n" +
                                             "      <fragListBuilder name=\"simple\"\n" +
                                             "                       class=\"solr.highlight.SimpleFragListBuilder\"/>\n" +
                                             "\n" +
                                             "      <!-- Configure the single fragListBuilder -->\n" +
                                             "      <fragListBuilder name=\"single\"\n" +
                                             "                       class=\"solr.highlight.SingleFragListBuilder\"/>\n" +
                                             "\n" +
                                             "      <!-- Configure the weighted fragListBuilder -->\n" +
                                             "      <fragListBuilder name=\"weighted\"\n" +
                                             "                       default=\"true\"\n" +
                                             "                       class=\"solr.highlight.WeightedFragListBuilder\"/>\n" +
                                             "\n" +
                                             "      <!-- default tag FragmentsBuilder -->\n" +
                                             "      <fragmentsBuilder name=\"default\"\n" +
                                             "                        default=\"true\"\n" +
                                             "                        class=\"solr.highlight.ScoreOrderFragmentsBuilder\">\n" +
                                             "        <!--\n" +
                                             "        <lst name=\"defaults\">\n" +
                                             "          <str name=\"hl.multiValuedSeparatorChar\">/</str>\n" +
                                             "        </lst>\n" +
                                             "        -->\n" +
                                             "      </fragmentsBuilder>\n" +
                                             "\n" +
                                             "      <!-- multi-colored tag FragmentsBuilder -->\n" +
                                             "      <fragmentsBuilder name=\"colored\"\n" +
                                             "                        class=\"solr.highlight.ScoreOrderFragmentsBuilder\">\n" +
                                             "        <lst name=\"defaults\">\n" +
                                             "          <str name=\"hl.tag.pre\"><![CDATA[\n" +
                                             "               <b style=\"background:yellow\">,<b style=\"background:lawgreen\">,\n" +
                                             "               <b style=\"background:aquamarine\">,<b style=\"background:magenta\">,\n" +
                                             "               <b style=\"background:palegreen\">,<b style=\"background:coral\">,\n" +
                                             "               <b style=\"background:wheat\">,<b style=\"background:khaki\">,\n" +
                                             "               <b style=\"background:lime\">,<b style=\"background:deepskyblue\">]]></str>\n" +
                                             "          <str name=\"hl.tag.post\"><![CDATA[</b>]]></str>\n" +
                                             "        </lst>\n" +
                                             "      </fragmentsBuilder>\n" +
                                             "\n" +
                                             "      <boundaryScanner name=\"default\"\n" +
                                             "                       default=\"true\"\n" +
                                             "                       class=\"solr.highlight.SimpleBoundaryScanner\">\n" +
                                             "        <lst name=\"defaults\">\n" +
                                             "          <str name=\"hl.bs.maxScan\">10</str>\n" +
                                             "          <str name=\"hl.bs.chars\">.,!? &#9;&#10;&#13;</str>\n" +
                                             "        </lst>\n" +
                                             "      </boundaryScanner>\n" +
                                             "\n" +
                                             "      <boundaryScanner name=\"breakIterator\"\n" +
                                             "                       class=\"solr.highlight.BreakIteratorBoundaryScanner\">\n" +
                                             "        <lst name=\"defaults\">\n" +
                                             "          <!-- type should be one of CHARACTER, WORD(default), LINE and SENTENCE -->\n" +
                                             "          <str name=\"hl.bs.type\">WORD</str>\n" +
                                             "          <!-- language and country are used when constructing Locale object.  -->\n" +
                                             "          <!-- And the Locale object will be used when getting instance of BreakIterator -->\n" +
                                             "          <str name=\"hl.bs.language\">en</str>\n" +
                                             "          <str name=\"hl.bs.country\">US</str>\n" +
                                             "        </lst>\n" +
                                             "      </boundaryScanner>\n" +
                                             "    </highlighting>\n" +
                                             "  </searchComponent>\n" +
                                             "\n" +
                    //                                             "  <!-- Update Processors\n" +
                    //                                             "\n" +
                    //                                             "       Chains of Update Processor Factories for dealing with Update\n" +
                    //                                             "       Requests can be declared, and then used by name in Update\n" +
                    //                                             "       Request Processors\n" +
                    //                                             "\n" +
                    //                                             "       http://wiki.apache.org/solr/UpdateRequestProcessor\n" +
                    //                                             "\n" +
                    //                                             "    -->\n" +
                    //                                             "\n" +
                    //                                             "  <!-- Add unknown fields to the schema\n" +
                    //                                             "\n" +
                    //                                             "       Field type guessing update processors that will\n" +
                    //                                             "       attempt to parse string-typed field values as Booleans, Longs,\n" +
                    //                                             "       Doubles, or Dates, and then add schema fields with the guessed\n" +
                    //                                             "       field types. Text content will be indexed as \"text_general\" as\n" +
                    //                                             "       well as a copy to a plain string version in *_str.\n" +
                    //                                             "\n" +
                    //                                             "       These require that the schema is both managed and mutable, by\n" +
                    //                                             "       declaring schemaFactory as ManagedIndexSchemaFactory, with\n" +
                    //                                             "       mutable specified as true.\n" +
                    //                                             "\n" +
                    //                                             "       See http://wiki.apache.org/solr/GuessingFieldTypes\n" +
                    //                                             "    -->\n" +
                    //                                             "  <updateProcessor class=\"solr.UUIDUpdateProcessorFactory\" name=\"uuid\"/>\n" +
                    //                                             "  <updateProcessor class=\"solr.RemoveBlankFieldUpdateProcessorFactory\" name=\"remove-blank\"/>\n" +
                    //                                             "  <updateProcessor class=\"solr.FieldNameMutatingUpdateProcessorFactory\" name=\"field-name-mutating\">\n" +
                    //                                             "    <str name=\"pattern\">[^\\w-\\.]</str>\n" +
                    //                                             "    <str name=\"replacement\">_</str>\n" +
                    //                                             "  </updateProcessor>\n" +
                    //                                             "  <updateProcessor class=\"solr.ParseBooleanFieldUpdateProcessorFactory\" name=\"parse-boolean\"/>\n" +
                    //                                             "  <updateProcessor class=\"solr.ParseLongFieldUpdateProcessorFactory\" name=\"parse-long\"/>\n" +
                    //                                             "  <updateProcessor class=\"solr.ParseDoubleFieldUpdateProcessorFactory\" name=\"parse-double\"/>\n" +
                    //                                             "  <updateProcessor class=\"solr.ParseDateFieldUpdateProcessorFactory\" name=\"parse-date\">\n" +
                    //                                             "    <arr name=\"format\">\n" +
                    //                                             "      <str>yyyy-MM-dd['T'[HH:mm[:ss[.SSS]][z</str>\n" +
                    //                                             "      <str>yyyy-MM-dd['T'[HH:mm[:ss[,SSS]][z</str>\n" +
                    //                                             "      <str>yyyy-MM-dd HH:mm[:ss[.SSS]][z</str>\n" +
                    //                                             "      <str>yyyy-MM-dd HH:mm[:ss[,SSS]][z</str>\n" +
                    //                                             "      <str>[EEE, ]dd MMM yyyy HH:mm[:ss] z</str>\n" +
                    //                                             "      <str>EEEE, dd-MMM-yy HH:mm:ss z</str>\n" +
                    //                                             "      <str>EEE MMM ppd HH:mm:ss [z ]yyyy</str>\n" +
                    //                                             "    </arr>\n" +
                    //                                             "  </updateProcessor>\n" +
                    //                                             "  <updateProcessor class=\"solr.AddSchemaFieldsUpdateProcessorFactory\" name=\"add-schema-fields\">\n" +
                    //                                             "    <lst name=\"typeMapping\">\n" +
                    //                                             "      <str name=\"valueClass\">java.lang.String</str>\n" +
                    //                                             "      <str name=\"fieldType\">text_general</str>\n" +
                    //                                             "      <lst name=\"copyField\">\n" +
                    //                                             "        <str name=\"dest\">*_str</str>\n" +
                    //                                             "        <int name=\"maxChars\">256</int>\n" +
                    //                                             "      </lst>\n" +
                    //                                             "      <!-- Use as default mapping instead of defaultFieldType -->\n" +
                    //                                             "      <bool name=\"default\">true</bool>\n" +
                    //                                             "    </lst>\n" +
                    //                                             "    <lst name=\"typeMapping\">\n" +
                    //                                             "      <str name=\"valueClass\">java.lang.Boolean</str>\n" +
                    //                                             "      <str name=\"fieldType\">booleans</str>\n" +
                    //                                             "    </lst>\n" +
                    //                                             "    <lst name=\"typeMapping\">\n" +
                    //                                             "      <str name=\"valueClass\">java.util.Date</str>\n" +
                    //                                             "      <str name=\"fieldType\">pdates</str>\n" +
                    //                                             "    </lst>\n" +
                    //                                             "    <lst name=\"typeMapping\">\n" +
                    //                                             "      <str name=\"valueClass\">java.lang.Long</str>\n" +
                    //                                             "      <str name=\"valueClass\">java.lang.Integer</str>\n" +
                    //                                             "      <str name=\"fieldType\">plongs</str>\n" +
                    //                                             "    </lst>\n" +
                    //                                             "    <lst name=\"typeMapping\">\n" +
                    //                                             "      <str name=\"valueClass\">java.lang.Number</str>\n" +
                    //                                             "      <str name=\"fieldType\">pdoubles</str>\n" +
                    //                                             "    </lst>\n" +
                    //                                             "  </updateProcessor>\n" +
                    //                                             "\n" +
                    //                                             "  <!-- The update.autoCreateFields property can be turned to false to disable schemaless mode -->\n" +
                    //                                             "  <updateRequestProcessorChain name=\"add-unknown-fields-to-the-schema\" default=\"${update.autoCreateFields:true}\"\n" +
                    //                                             "           processor=\"uuid,remove-blank,field-name-mutating,parse-boolean,parse-long,parse-double,parse-date,add-schema-fields\">\n" +
                    //                                             "    <processor class=\"solr.LogUpdateProcessorFactory\"/>\n" +
                    //                                             "    <processor class=\"solr.DistributedUpdateProcessorFactory\"/>\n" +
                    //                                             "    <processor class=\"solr.RunUpdateProcessorFactory\"/>\n" +
                    //                                             "  </updateRequestProcessorChain>\n" +
                                             "\n" +
                                             "  <!-- Deduplication\n" +
                                             "\n" +
                                             "       An example dedup update processor that creates the \"id\" field\n" +
                                             "       on the fly based on the hash code of some other fields.  This\n" +
                                             "       example has overwriteDupes set to false since we are using the\n" +
                                             "       id field as the signatureField and Solr will maintain\n" +
                                             "       uniqueness based on that anyway.\n" +
                                             "\n" +
                                             "    -->\n" +
                                             "  <!--\n" +
                                             "     <updateRequestProcessorChain name=\"dedupe\">\n" +
                                             "       <processor class=\"solr.processor.SignatureUpdateProcessorFactory\">\n" +
                                             "         <bool name=\"enabled\">true</bool>\n" +
                                             "         <str name=\"signatureField\">id</str>\n" +
                                             "         <bool name=\"overwriteDupes\">false</bool>\n" +
                                             "         <str name=\"fields\">name,features,cat</str>\n" +
                                             "         <str name=\"signatureClass\">solr.processor.Lookup3Signature</str>\n" +
                                             "       </processor>\n" +
                                             "       <processor class=\"solr.LogUpdateProcessorFactory\" />\n" +
                                             "       <processor class=\"solr.RunUpdateProcessorFactory\" />\n" +
                                             "     </updateRequestProcessorChain>\n" +
                                             "    -->\n" +
                                             "\n" +
                                             "  <!-- Language identification\n" +
                                             "\n" +
                                             "       This example update chain identifies the language of the incoming\n" +
                                             "       documents using the langid contrib. The detected language is\n" +
                                             "       written to field language_s. No field name mapping is done.\n" +
                                             "       The fields used for detection are text, title, subject and description,\n" +
                                             "       making this example suitable for detecting languages form full-text\n" +
                                             "       rich documents injected via ExtractingRequestHandler.\n" +
                                             "       See more about langId at http://wiki.apache.org/solr/LanguageDetection\n" +
                                             "    -->\n" +
                                             "  <!--\n" +
                                             "   <updateRequestProcessorChain name=\"langid\">\n" +
                                             "     <processor class=\"org.apache.solr.update.processor.TikaLanguageIdentifierUpdateProcessorFactory\">\n" +
                                             "       <str name=\"langid.fl\">text,title,subject,description</str>\n" +
                                             "       <str name=\"langid.langField\">language_s</str>\n" +
                                             "       <str name=\"langid.fallback\">en</str>\n" +
                                             "     </processor>\n" +
                                             "     <processor class=\"solr.LogUpdateProcessorFactory\" />\n" +
                                             "     <processor class=\"solr.RunUpdateProcessorFactory\" />\n" +
                                             "   </updateRequestProcessorChain>\n" +
                                             "  -->\n" +
                                             "\n" +
                                             "  <!-- Script update processor\n" +
                                             "\n" +
                                             "    This example hooks in an update processor implemented using JavaScript.\n" +
                                             "\n" +
                                             "    See more about the script update processor at http://wiki.apache.org/solr/ScriptUpdateProcessor\n" +
                                             "  -->\n" +
                                             "  <!--\n" +
                                             "    <updateRequestProcessorChain name=\"script\">\n" +
                                             "      <processor class=\"solr.StatelessScriptUpdateProcessorFactory\">\n" +
                                             "        <str name=\"script\">update-script.js</str>\n" +
                                             "        <lst name=\"params\">\n" +
                                             "          <str name=\"config_param\">example config parameter</str>\n" +
                                             "        </lst>\n" +
                                             "      </processor>\n" +
                                             "      <processor class=\"solr.RunUpdateProcessorFactory\" />\n" +
                                             "    </updateRequestProcessorChain>\n" +
                                             "  -->\n" +
                                             "\n" +
                                             "  <!-- Response Writers\n" +
                                             "\n" +
                                             "       http://wiki.apache.org/solr/QueryResponseWriter\n" +
                                             "\n" +
                                             "       Request responses will be written using the writer specified by\n" +
                                             "       the 'wt' request parameter matching the name of a registered\n" +
                                             "       writer.\n" +
                                             "\n" +
                                             "       The \"default\" writer is the default and will be used if 'wt' is\n" +
                                             "       not specified in the request.\n" +
                                             "    -->\n" +
                                             "  <!-- The following response writers are implicitly configured unless\n" +
                                             "       overridden...\n" +
                                             "    -->\n" +
                                             "  <!--\n" +
                                             "     <queryResponseWriter name=\"xml\"\n" +
                                             "                          default=\"true\"\n" +
                                             "                          class=\"solr.XMLResponseWriter\" />\n" +
                                             "     <queryResponseWriter name=\"json\" class=\"solr.JSONResponseWriter\"/>\n" +
                                             "     <queryResponseWriter name=\"python\" class=\"solr.PythonResponseWriter\"/>\n" +
                                             "     <queryResponseWriter name=\"ruby\" class=\"solr.RubyResponseWriter\"/>\n" +
                                             "     <queryResponseWriter name=\"php\" class=\"solr.PHPResponseWriter\"/>\n" +
                                             "     <queryResponseWriter name=\"phps\" class=\"solr.PHPSerializedResponseWriter\"/>\n" +
                                             "     <queryResponseWriter name=\"csv\" class=\"solr.CSVResponseWriter\"/>\n" +
                                             "     <queryResponseWriter name=\"schema.xml\" class=\"solr.SchemaXmlResponseWriter\"/>\n" +
                                             "    -->\n" +
                                             "\n" +
                                             "  <queryResponseWriter name=\"json\" class=\"solr.JSONResponseWriter\">\n" +
                                             "    <!-- For the purposes of the tutorial, JSON responses are written as\n" +
                                             "     plain text so that they are easy to read in *any* browser.\n" +
                                             "     If you expect a MIME type of \"application/json\" just remove this override.\n" +
                                             "    -->\n" +
                                             "    <str name=\"content-type\">text/plain; charset=UTF-8</str>\n" +
                                             "  </queryResponseWriter>\n" +
                                             "\n" +
                                             "  <!--\n" +
                                             "     Custom response writers can be declared as needed...\n" +
                                             "    -->\n" +
                                             "  <queryResponseWriter name=\"velocity\" class=\"solr.VelocityResponseWriter\" startup=\"lazy\">\n" +
                                             "    <str name=\"template.base.dir\">${velocity.template.base.dir:}</str>\n" +
                                             "    <str name=\"solr.resource.loader.enabled\">${velocity.solr.resource.loader.enabled:true}</str>\n" +
                                             "    <str name=\"params.resource.loader.enabled\">${velocity.params.resource.loader.enabled:false}</str>\n" +
                                             "  </queryResponseWriter>\n" +
                                             "\n" +
                                             "  <!-- XSLT response writer transforms the XML output by any xslt file found\n" +
                                             "       in Solr's conf/xslt directory.  Changes to xslt files are checked for\n" +
                                             "       every xsltCacheLifetimeSeconds.\n" +
                                             "    -->\n" +
                                             "  <queryResponseWriter name=\"xslt\" class=\"solr.XSLTResponseWriter\">\n" +
                                             "    <int name=\"xsltCacheLifetimeSeconds\">5</int>\n" +
                                             "  </queryResponseWriter>\n" +
                                             "\n" +
                                             "  <!-- Query Parsers\n" +
                                             "\n" +
                                             "       https://lucene.apache.org/solr/guide/query-syntax-and-parsing.html\n" +
                                             "\n" +
                                             "       Multiple QParserPlugins can be registered by name, and then\n" +
                                             "       used in either the \"defType\" param for the QueryComponent (used\n" +
                                             "       by SearchHandler) or in LocalParams\n" +
                                             "    -->\n" +
                                             "  <!-- example of registering a query parser -->\n" +
                                             "  <!--\n" +
                                             "     <queryParser name=\"myparser\" class=\"com.mycompany.MyQParserPlugin\"/>\n" +
                                             "    -->\n" +
                                             "\n" +
                                             "  <!-- Function Parsers\n" +
                                             "\n" +
                                             "       http://wiki.apache.org/solr/FunctionQuery\n" +
                                             "\n" +
                                             "       Multiple ValueSourceParsers can be registered by name, and then\n" +
                                             "       used as function names when using the \"func\" QParser.\n" +
                                             "    -->\n" +
                                             "  <!-- example of registering a custom function parser  -->\n" +
                                             "  <!--\n" +
                                             "     <valueSourceParser name=\"myfunc\"\n" +
                                             "                        class=\"com.mycompany.MyValueSourceParser\" />\n" +
                                             "    -->\n" +
                                             "\n" +
                                             "\n" +
                                             "  <!-- Document Transformers\n" +
                                             "       http://wiki.apache.org/solr/DocTransformers\n" +
                                             "    -->\n" +
                                             "  <!--\n" +
                                             "     Could be something like:\n" +
                                             "     <transformer name=\"db\" class=\"com.mycompany.LoadFromDatabaseTransformer\" >\n" +
                                             "       <int name=\"connection\">jdbc://....</int>\n" +
                                             "     </transformer>\n" +
                                             "\n" +
                                             "     To add a constant value to all docs, use:\n" +
                                             "     <transformer name=\"mytrans2\" class=\"org.apache.solr.response.transform.ValueAugmenterFactory\" >\n" +
                                             "       <int name=\"value\">5</int>\n" +
                                             "     </transformer>\n" +
                                             "\n" +
                                             "     If you want the user to still be able to change it with _value:something_ use this:\n" +
                                             "     <transformer name=\"mytrans3\" class=\"org.apache.solr.response.transform.ValueAugmenterFactory\" >\n" +
                                             "       <double name=\"defaultValue\">5</double>\n" +
                                             "     </transformer>\n" +
                                             "\n" +
                                             "      If you are using the QueryElevationComponent, you may wish to mark documents that get boosted.  The\n" +
                                             "      EditorialMarkerFactory will do exactly that:\n" +
                                             "     <transformer name=\"qecBooster\" class=\"org.apache.solr.response.transform.EditorialMarkerFactory\" />\n" +
                                             "    -->\n" +
                                             "</config>\n";

    /**
     * This is the default (schemaless-oriented) schema in Solr 8.0
     */
    public static final String DEFAULT_SCHEMA = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                                                "<!--\n" +
                                                " Licensed to the Apache Software Foundation (ASF) under one or more\n" +
                                                " contributor license agreements.  See the NOTICE file distributed with\n" +
                                                " this work for additional information regarding copyright ownership.\n" +
                                                " The ASF licenses this file to You under the Apache License, Version 2.0\n" +
                                                " (the \"License\"); you may not use this file except in compliance with\n" +
                                                " the License.  You may obtain a copy of the License at\n" +
                                                "\n" +
                                                "     http://www.apache.org/licenses/LICENSE-2.0\n" +
                                                "\n" +
                                                " Unless required by applicable law or agreed to in writing, software\n" +
                                                " distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                                                " WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                                                " See the License for the specific language governing permissions and\n" +
                                                " limitations under the License.\n" +
                                                "-->\n" +
                                                "\n" +
                                                "<!--\n" +
                                                "\n" +
                                                " This example schema is the recommended starting point for users.\n" +
                                                " It should be kept correct and concise, usable out-of-the-box.\n" +
                                                "\n" +
                                                "\n" +
                                                " For more information, on how to customize this file, please see\n" +
                                                " http://lucene.apache.org/solr/guide/documents-fields-and-schema-design.html\n" +
                                                "\n" +
                                                " PERFORMANCE NOTE: this schema includes many optional features and should not\n" +
                                                " be used for benchmarking.  To improve performance one could\n" +
                                                "  - set stored=\"false\" for all fields possible (esp large fields) when you\n" +
                                                "    only need to search on the field but don't need to return the original\n" +
                                                "    value.\n" +
                                                "  - set indexed=\"false\" if you don't need to search on the field, but only\n" +
                                                "    return the field as a result of searching on other indexed fields.\n" +
                                                "  - remove all unneeded copyField statements\n" +
                                                "  - for best index size and searching performance, set \"index\" to false\n" +
                                                "    for all general text fields, use copyField to copy them to the\n" +
                                                "    catchall \"text\" field, and use that for searching.\n" +
                                                "-->\n" +
                                                "\n" +
                                                "<schema name=\"default-config\" version=\"1.6\">\n" +
                                                "    <!-- attribute \"name\" is the name of this schema and is only used for display purposes.\n" +
                                                "       version=\"x.y\" is Solr's version number for the schema syntax and \n" +
                                                "       semantics.  It should not normally be changed by applications.\n" +
                                                "\n" +
                                                "       1.0: multiValued attribute did not exist, all fields are multiValued \n" +
                                                "            by nature\n" +
                                                "       1.1: multiValued attribute introduced, false by default \n" +
                                                "       1.2: omitTermFreqAndPositions attribute introduced, true by default \n" +
                                                "            except for text fields.\n" +
                                                "       1.3: removed optional field compress feature\n" +
                                                "       1.4: autoGeneratePhraseQueries attribute introduced to drive QueryParser\n" +
                                                "            behavior when a single string produces multiple tokens.  Defaults \n" +
                                                "            to off for version >= 1.4\n" +
                                                "       1.5: omitNorms defaults to true for primitive field types \n" +
                                                "            (int, float, boolean, string...)\n" +
                                                "       1.6: useDocValuesAsStored defaults to true.\n" +
                                                "    -->\n" +
                                                "\n" +
                                                "    <!-- Valid attributes for fields:\n" +
                                                "     name: mandatory - the name for the field\n" +
                                                "     type: mandatory - the name of a field type from the \n" +
                                                "       fieldTypes section\n" +
                                                "     indexed: true if this field should be indexed (searchable or sortable)\n" +
                                                "     stored: true if this field should be retrievable\n" +
                                                "     docValues: true if this field should have doc values. Doc Values is\n" +
                                                "       recommended (required, if you are using *Point fields) for faceting,\n" +
                                                "       grouping, sorting and function queries. Doc Values will make the index\n" +
                                                "       faster to load, more NRT-friendly and more memory-efficient. \n" +
                                                "       They are currently only supported by StrField, UUIDField, all \n" +
                                                "       *PointFields, and depending on the field type, they might require\n" +
                                                "       the field to be single-valued, be required or have a default value\n" +
                                                "       (check the documentation of the field type you're interested in for\n" +
                                                "       more information)\n" +
                                                "     multiValued: true if this field may contain multiple values per document\n" +
                                                "     omitNorms: (expert) set to true to omit the norms associated with\n" +
                                                "       this field (this disables length normalization and index-time\n" +
                                                "       boosting for the field, and saves some memory).  Only full-text\n" +
                                                "       fields or fields that need an index-time boost need norms.\n" +
                                                "       Norms are omitted for primitive (non-analyzed) types by default.\n" +
                                                "     termVectors: [false] set to true to store the term vector for a\n" +
                                                "       given field.\n" +
                                                "       When using MoreLikeThis, fields used for similarity should be\n" +
                                                "       stored for best performance.\n" +
                                                "     termPositions: Store position information with the term vector.  \n" +
                                                "       This will increase storage costs.\n" +
                                                "     termOffsets: Store offset information with the term vector. This \n" +
                                                "       will increase storage costs.\n" +
                                                "     required: The field is required.  It will throw an error if the\n" +
                                                "       value does not exist\n" +
                                                "     default: a value that should be used if no value is specified\n" +
                                                "       when adding a document.\n" +
                                                "    -->\n" +
                                                "\n" +
                                                "    <!-- field names should consist of alphanumeric or underscore characters only and\n" +
                                                "      not start with a digit.  This is not currently strictly enforced,\n" +
                                                "      but other field names will not have first class support from all components\n" +
                                                "      and back compatibility is not guaranteed.  Names with both leading and\n" +
                                                "      trailing underscores (e.g. _version_) are reserved.\n" +
                                                "    -->\n" +
                                                "\n" +
                                                "    <!-- In this _default configset, only four fields are pre-declared:\n" +
                                                "         id, _version_, and _text_ and _root_. All other fields will be type guessed and added via the\n" +
                                                "         \"add-unknown-fields-to-the-schema\" update request processor chain declared in solrconfig.xml.\n" +
                                                "         \n" +
                                                "         Note that many dynamic fields are also defined - you can use them to specify a \n" +
                                                "         field's type via field naming conventions - see below.\n" +
                                                "  \n" +
                                                "         WARNING: The _text_ catch-all field will significantly increase your index size.\n" +
                                                "         If you don't need it, consider removing it and the corresponding copyField directive.\n" +
                                                "    -->\n" +
                                                "\n" +
                                                "    <field name=\"id\" type=\"string\" indexed=\"true\" stored=\"true\" required=\"true\" multiValued=\"false\" />\n" +
                                                "    <!-- docValues are enabled by default for long type so we don't need to index the version field  -->\n" +
                                                "    <field name=\"_version_\" type=\"plong\" indexed=\"false\" stored=\"false\"/>\n" +
                                                "\n" +
                                                "    <!-- If you don't use child/nested documents, then you should remove the next two fields:  -->\n" +
                                                "    <!-- for nested documents (minimal; points to root document) -->\n" +
                                                "    <field name=\"_root_\" type=\"string\" indexed=\"true\" stored=\"false\" docValues=\"false\" />\n" +
                                                "    <!-- for nested documents (relationship tracking) -->\n" +
                                                "    <field name=\"_nest_path_\" type=\"_nest_path_\" /><fieldType name=\"_nest_path_\" class=\"solr.NestPathField\" />\n" +
                                                "\n" +
                                                "    <field name=\"_text_\" type=\"text_general\" indexed=\"true\" stored=\"false\" multiValued=\"true\"/>\n" +
                                                "\n" +
                                                "    <!-- This can be enabled, in case the client does not know what fields may be searched. It isn't enabled by default\n" +
                                                "         because it's very expensive to index everything twice. -->\n" +
                                                "    <!-- <copyField source=\"*\" dest=\"_text_\"/> -->\n" +
                                                "\n" +
                                                "    <!-- Dynamic field definitions allow using convention over configuration\n" +
                                                "       for fields via the specification of patterns to match field names.\n" +
                                                "       EXAMPLE:  name=\"*_i\" will match any field ending in _i (like myid_i, z_i)\n" +
                                                "       RESTRICTION: the glob-like pattern in the name attribute must have a \"*\" only at the start or the end.  -->\n" +
                                                "   \n" +
                                                "    <dynamicField name=\"*_i\"  type=\"pint\"    indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_is\" type=\"pints\"    indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_s\"  type=\"string\"  indexed=\"true\"  stored=\"true\" />\n" +
                                                "    <dynamicField name=\"*_ss\" type=\"strings\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_l\"  type=\"plong\"   indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_ls\" type=\"plongs\"   indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_t\" type=\"text_general\" indexed=\"true\" stored=\"true\" multiValued=\"false\"/>\n" +
                                                "    <dynamicField name=\"*_txt\" type=\"text_general\" indexed=\"true\" stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_b\"  type=\"boolean\" indexed=\"true\" stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_bs\" type=\"booleans\" indexed=\"true\" stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_f\"  type=\"pfloat\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_fs\" type=\"pfloats\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_d\"  type=\"pdouble\" indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_ds\" type=\"pdoubles\" indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"random_*\" type=\"random\"/>\n" +
                                                "    <dynamicField name=\"ignored_*\" type=\"ignored\"/>\n" +
                                                "\n" +
                                                "    <!-- Type used for data-driven schema, to add a string copy for each text field -->\n" +
                                                "    <dynamicField name=\"*_str\" type=\"strings\" stored=\"false\" docValues=\"true\" indexed=\"false\" useDocValuesAsStored=\"false\"/>\n" +
                                                "\n" +
                                                "    <dynamicField name=\"*_dt\"  type=\"pdate\"    indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_dts\" type=\"pdate\"    indexed=\"true\"  stored=\"true\" multiValued=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_p\"  type=\"location\" indexed=\"true\" stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_srpt\"  type=\"location_rpt\" indexed=\"true\" stored=\"true\"/>\n" +
                                                "\n" +
                                                "    <!-- payloaded dynamic fields -->\n" +
                                                "    <dynamicField name=\"*_dpf\" type=\"delimited_payloads_float\" indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_dpi\" type=\"delimited_payloads_int\" indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <dynamicField name=\"*_dps\" type=\"delimited_payloads_string\" indexed=\"true\"  stored=\"true\"/>\n" +
                                                "\n" +
                                                "    <dynamicField name=\"attr_*\" type=\"text_general\" indexed=\"true\" stored=\"true\" multiValued=\"true\"/>\n" +
                                                "\n" +
                                                "    <!-- Field to use to determine and enforce document uniqueness.\n" +
                                                "      Unless this field is marked with required=\"false\", it will be a required field\n" +
                                                "    -->\n" +
                                                "    <uniqueKey>id</uniqueKey>\n" +
                                                "\n" +
                                                "    <!-- copyField commands copy one field to another at the time a document\n" +
                                                "       is added to the index.  It's used either to index the same field differently,\n" +
                                                "       or to add multiple fields to the same field for easier/faster searching.\n" +
                                                "\n" +
                                                "    <copyField source=\"sourceFieldName\" dest=\"destinationFieldName\"/>\n" +
                                                "    -->\n" +
                                                "\n" +
                                                "    <!-- field type definitions. The \"name\" attribute is\n" +
                                                "       just a label to be used by field definitions.  The \"class\"\n" +
                                                "       attribute and any other attributes determine the real\n" +
                                                "       behavior of the fieldType.\n" +
                                                "         Class names starting with \"solr\" refer to java classes in a\n" +
                                                "       standard package such as org.apache.solr.analysis\n" +
                                                "    -->\n" +
                                                "\n" +
                                                "    <!-- sortMissingLast and sortMissingFirst attributes are optional attributes are\n" +
                                                "         currently supported on types that are sorted internally as strings\n" +
                                                "         and on numeric types.\n" +
                                                "       This includes \"string\", \"boolean\", \"pint\", \"pfloat\", \"plong\", \"pdate\", \"pdouble\".\n" +
                                                "       - If sortMissingLast=\"true\", then a sort on this field will cause documents\n" +
                                                "         without the field to come after documents with the field,\n" +
                                                "         regardless of the requested sort order (asc or desc).\n" +
                                                "       - If sortMissingFirst=\"true\", then a sort on this field will cause documents\n" +
                                                "         without the field to come before documents with the field,\n" +
                                                "         regardless of the requested sort order.\n" +
                                                "       - If sortMissingLast=\"false\" and sortMissingFirst=\"false\" (the default),\n" +
                                                "         then default lucene sorting will be used which places docs without the\n" +
                                                "         field first in an ascending sort and last in a descending sort.\n" +
                                                "    -->\n" +
                                                "\n" +
                                                "    <!-- The StrField type is not analyzed, but indexed/stored verbatim. -->\n" +
                                                "    <fieldType name=\"string\" class=\"solr.StrField\" sortMissingLast=\"true\" docValues=\"true\" />\n" +
                                                "    <fieldType name=\"strings\" class=\"solr.StrField\" sortMissingLast=\"true\" multiValued=\"true\" docValues=\"true\" />\n" +
                                                "\n" +
                                                "    <!-- boolean type: \"true\" or \"false\" -->\n" +
                                                "    <fieldType name=\"boolean\" class=\"solr.BoolField\" sortMissingLast=\"true\"/>\n" +
                                                "    <fieldType name=\"booleans\" class=\"solr.BoolField\" sortMissingLast=\"true\" multiValued=\"true\"/>\n" +
                                                "\n" +
                                                "    <!--\n" +
                                                "      Numeric field types that index values using KD-trees.\n" +
                                                "      Point fields don't support FieldCache, so they must have docValues=\"true\" if needed for sorting, faceting, functions, etc.\n" +
                                                "    -->\n" +
                                                "    <fieldType name=\"pint\" class=\"solr.IntPointField\" docValues=\"true\"/>\n" +
                                                "    <fieldType name=\"pfloat\" class=\"solr.FloatPointField\" docValues=\"true\"/>\n" +
                                                "    <fieldType name=\"plong\" class=\"solr.LongPointField\" docValues=\"true\"/>\n" +
                                                "    <fieldType name=\"pdouble\" class=\"solr.DoublePointField\" docValues=\"true\"/>\n" +
                                                "\n" +
                                                "    <fieldType name=\"pints\" class=\"solr.IntPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
                                                "    <fieldType name=\"pfloats\" class=\"solr.FloatPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
                                                "    <fieldType name=\"plongs\" class=\"solr.LongPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
                                                "    <fieldType name=\"pdoubles\" class=\"solr.DoublePointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
                                                "    <fieldType name=\"random\" class=\"solr.RandomSortField\" indexed=\"true\"/>\n" +
                                                "\n" +
                                                "    <!-- since fields of this type are by default not stored or indexed,\n" +
                                                "       any data added to them will be ignored outright.  -->\n" +
                                                "    <fieldType name=\"ignored\" stored=\"false\" indexed=\"false\" multiValued=\"true\" class=\"solr.StrField\" />\n" +
                                                "\n" +
                                                "    <!-- The format for this date field is of the form 1995-12-31T23:59:59Z, and\n" +
                                                "         is a more restricted form of the canonical representation of dateTime\n" +
                                                "         http://www.w3.org/TR/xmlschema-2/#dateTime    \n" +
                                                "         The trailing \"Z\" designates UTC time and is mandatory.\n" +
                                                "         Optional fractional seconds are allowed: 1995-12-31T23:59:59.999Z\n" +
                                                "         All other components are mandatory.\n" +
                                                "\n" +
                                                "         Expressions can also be used to denote calculations that should be\n" +
                                                "         performed relative to \"NOW\" to determine the value, ie...\n" +
                                                "\n" +
                                                "               NOW/HOUR\n" +
                                                "                  ... Round to the start of the current hour\n" +
                                                "               NOW-1DAY\n" +
                                                "                  ... Exactly 1 day prior to now\n" +
                                                "               NOW/DAY+6MONTHS+3DAYS\n" +
                                                "                  ... 6 months and 3 days in the future from the start of\n" +
                                                "                      the current day\n" +
                                                "                      \n" +
                                                "      -->\n" +
                                                "    <!-- KD-tree versions of date fields -->\n" +
                                                "    <fieldType name=\"pdate\" class=\"solr.DatePointField\" docValues=\"true\"/>\n" +
                                                "    <fieldType name=\"pdates\" class=\"solr.DatePointField\" docValues=\"true\" multiValued=\"true\"/>\n" +
                                                "    \n" +
                                                "    <!--Binary data type. The data should be sent/retrieved in as Base64 encoded Strings -->\n" +
                                                "    <fieldType name=\"binary\" class=\"solr.BinaryField\"/>\n" +
                                                "\n" +
                                                "    <!-- solr.TextField allows the specification of custom text analyzers\n" +
                                                "         specified as a tokenizer and a list of token filters. Different\n" +
                                                "         analyzers may be specified for indexing and querying.\n" +
                                                "\n" +
                                                "         The optional positionIncrementGap puts space between multiple fields of\n" +
                                                "         this type on the same document, with the purpose of preventing false phrase\n" +
                                                "         matching across fields.\n" +
                                                "\n" +
                                                "         For more info on customizing your analyzer chain, please see\n" +
                                                "         http://lucene.apache.org/solr/guide/understanding-analyzers-tokenizers-and-filters.html#understanding-analyzers-tokenizers-and-filters\n" +
                                                "     -->\n" +
                                                "\n" +
                                                "    <!-- One can also specify an existing Analyzer class that has a\n" +
                                                "         default constructor via the class attribute on the analyzer element.\n" +
                                                "         Example:\n" +
                                                "    <fieldType name=\"text_greek\" class=\"solr.TextField\">\n" +
                                                "      <analyzer class=\"org.apache.lucene.analysis.el.GreekAnalyzer\"/>\n" +
                                                "    </fieldType>\n" +
                                                "    -->\n" +
                                                "\n" +
                                                "    <!-- A text field that only splits on whitespace for exact matching of words -->\n" +
                                                "    <dynamicField name=\"*_ws\" type=\"text_ws\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_ws\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer>\n" +
                                                "        <tokenizer class=\"solr.WhitespaceTokenizerFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- A general text field that has reasonable, generic\n" +
                                                "         cross-language defaults: it tokenizes with StandardTokenizer,\n" +
                                                "\t       removes stop words from case-insensitive \"stopwords.txt\"\n" +
                                                "\t       (empty by default), and down cases.  At query time only, it\n" +
                                                "\t       also applies synonyms.\n" +
                                                "\t  -->\n" +
                                                "    <fieldType name=\"text_general\" class=\"solr.TextField\" positionIncrementGap=\"100\" multiValued=\"true\">\n" +
                                                "      <analyzer type=\"index\">\n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" />\n" +
                                                "        <!-- in this example, we will only use synonyms at query time\n" +
                                                "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"index_synonyms.txt\" ignoreCase=\"true\" expand=\"false\"/>\n" +
                                                "        <filter class=\"solr.FlattenGraphFilterFactory\"/>\n" +
                                                "        -->\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "      <analyzer type=\"query\">\n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" />\n" +
                                                "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"true\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    \n" +
                                                "    <!-- SortableTextField generaly functions exactly like TextField,\n" +
                                                "         except that it supports, and by default uses, docValues for sorting (or faceting)\n" +
                                                "         on the first 1024 characters of the original field values (which is configurable).\n" +
                                                "         \n" +
                                                "         This makes it a bit more useful then TextField in many situations, but the trade-off\n" +
                                                "         is that it takes up more space on disk; which is why it's not used in place of TextField\n" +
                                                "         for every fieldType in this _default schema.\n" +
                                                "\t  -->\n" +
                                                "    <dynamicField name=\"*_t_sort\" type=\"text_gen_sort\" indexed=\"true\" stored=\"true\" multiValued=\"false\"/>\n" +
                                                "    <dynamicField name=\"*_txt_sort\" type=\"text_gen_sort\" indexed=\"true\" stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_gen_sort\" class=\"solr.SortableTextField\" positionIncrementGap=\"100\" multiValued=\"true\">\n" +
                                                "      <analyzer type=\"index\">\n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" />\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "      <analyzer type=\"query\">\n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" />\n" +
                                                "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"true\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- A text field with defaults appropriate for English: it tokenizes with StandardTokenizer,\n" +
                                                "         removes English stop words (lang/stopwords_en.txt), down cases, protects words from protwords.txt, and\n" +
                                                "         finally applies Porter's stemming.  The query time analyzer also applies synonyms from synonyms.txt. -->\n" +
                                                "    <dynamicField name=\"*_txt_en\" type=\"text_en\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_en\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer type=\"index\">\n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <!-- in this example, we will only use synonyms at query time\n" +
                                                "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"index_synonyms.txt\" ignoreCase=\"true\" expand=\"false\"/>\n" +
                                                "        <filter class=\"solr.FlattenGraphFilterFactory\"/>\n" +
                                                "        -->\n" +
                                                "        <!-- Case insensitive stop word removal.\n" +
                                                "        -->\n" +
                                                "        <filter class=\"solr.StopFilterFactory\"\n" +
                                                "                ignoreCase=\"true\"\n" +
                                                "                words=\"lang/stopwords_en.txt\"\n" +
                                                "            />\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.EnglishPossessiveFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.KeywordMarkerFilterFactory\" protected=\"protwords.txt\"/>\n" +
                                                "        <!-- Optionally you may want to use this less aggressive stemmer instead of PorterStemFilterFactory:\n" +
                                                "        <filter class=\"solr.EnglishMinimalStemFilterFactory\"/>\n" +
                                                "\t      -->\n" +
                                                "        <filter class=\"solr.PorterStemFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "      <analyzer type=\"query\">\n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"true\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\"\n" +
                                                "                ignoreCase=\"true\"\n" +
                                                "                words=\"lang/stopwords_en.txt\"\n" +
                                                "        />\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.EnglishPossessiveFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.KeywordMarkerFilterFactory\" protected=\"protwords.txt\"/>\n" +
                                                "        <!-- Optionally you may want to use this less aggressive stemmer instead of PorterStemFilterFactory:\n" +
                                                "        <filter class=\"solr.EnglishMinimalStemFilterFactory\"/>\n" +
                                                "\t      -->\n" +
                                                "        <filter class=\"solr.PorterStemFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- A text field with defaults appropriate for English, plus\n" +
                                                "         aggressive word-splitting and autophrase features enabled.\n" +
                                                "         This field is just like text_en, except it adds\n" +
                                                "         WordDelimiterGraphFilter to enable splitting and matching of\n" +
                                                "         words on case-change, alpha numeric boundaries, and\n" +
                                                "         non-alphanumeric chars.  This means certain compound word\n" +
                                                "         cases will work, for example query \"wi fi\" will match\n" +
                                                "         document \"WiFi\" or \"wi-fi\".\n" +
                                                "    -->\n" +
                                                "    <dynamicField name=\"*_txt_en_split\" type=\"text_en_splitting\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_en_splitting\" class=\"solr.TextField\" positionIncrementGap=\"100\" autoGeneratePhraseQueries=\"true\">\n" +
                                                "      <analyzer type=\"index\">\n" +
                                                "        <tokenizer class=\"solr.WhitespaceTokenizerFactory\"/>\n" +
                                                "        <!-- in this example, we will only use synonyms at query time\n" +
                                                "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"index_synonyms.txt\" ignoreCase=\"true\" expand=\"false\"/>\n" +
                                                "        -->\n" +
                                                "        <!-- Case insensitive stop word removal.\n" +
                                                "        -->\n" +
                                                "        <filter class=\"solr.StopFilterFactory\"\n" +
                                                "                ignoreCase=\"true\"\n" +
                                                "                words=\"lang/stopwords_en.txt\"\n" +
                                                "        />\n" +
                                                "        <filter class=\"solr.WordDelimiterGraphFilterFactory\" generateWordParts=\"1\" generateNumberParts=\"1\" catenateWords=\"1\" catenateNumbers=\"1\" catenateAll=\"0\" splitOnCaseChange=\"1\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.KeywordMarkerFilterFactory\" protected=\"protwords.txt\"/>\n" +
                                                "        <filter class=\"solr.PorterStemFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.FlattenGraphFilterFactory\" />\n" +
                                                "      </analyzer>\n" +
                                                "      <analyzer type=\"query\">\n" +
                                                "        <tokenizer class=\"solr.WhitespaceTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"true\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\"\n" +
                                                "                ignoreCase=\"true\"\n" +
                                                "                words=\"lang/stopwords_en.txt\"\n" +
                                                "        />\n" +
                                                "        <filter class=\"solr.WordDelimiterGraphFilterFactory\" generateWordParts=\"1\" generateNumberParts=\"1\" catenateWords=\"0\" catenateNumbers=\"0\" catenateAll=\"0\" splitOnCaseChange=\"1\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.KeywordMarkerFilterFactory\" protected=\"protwords.txt\"/>\n" +
                                                "        <filter class=\"solr.PorterStemFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- Less flexible matching, but less false matches.  Probably not ideal for product names,\n" +
                                                "         but may be good for SKUs.  Can insert dashes in the wrong place and still match. -->\n" +
                                                "    <dynamicField name=\"*_txt_en_split_tight\" type=\"text_en_splitting_tight\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_en_splitting_tight\" class=\"solr.TextField\" positionIncrementGap=\"100\" autoGeneratePhraseQueries=\"true\">\n" +
                                                "      <analyzer type=\"index\">\n" +
                                                "        <tokenizer class=\"solr.WhitespaceTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"false\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_en.txt\"/>\n" +
                                                "        <filter class=\"solr.WordDelimiterGraphFilterFactory\" generateWordParts=\"0\" generateNumberParts=\"0\" catenateWords=\"1\" catenateNumbers=\"1\" catenateAll=\"0\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.KeywordMarkerFilterFactory\" protected=\"protwords.txt\"/>\n" +
                                                "        <filter class=\"solr.EnglishMinimalStemFilterFactory\"/>\n" +
                                                "        <!-- this filter can remove any duplicate tokens that appear at the same position - sometimes\n" +
                                                "             possible with WordDelimiterGraphFilter in conjuncton with stemming. -->\n" +
                                                "        <filter class=\"solr.RemoveDuplicatesTokenFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.FlattenGraphFilterFactory\" />\n" +
                                                "      </analyzer>\n" +
                                                "      <analyzer type=\"query\">\n" +
                                                "        <tokenizer class=\"solr.WhitespaceTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"false\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_en.txt\"/>\n" +
                                                "        <filter class=\"solr.WordDelimiterGraphFilterFactory\" generateWordParts=\"0\" generateNumberParts=\"0\" catenateWords=\"1\" catenateNumbers=\"1\" catenateAll=\"0\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.KeywordMarkerFilterFactory\" protected=\"protwords.txt\"/>\n" +
                                                "        <filter class=\"solr.EnglishMinimalStemFilterFactory\"/>\n" +
                                                "        <!-- this filter can remove any duplicate tokens that appear at the same position - sometimes\n" +
                                                "             possible with WordDelimiterGraphFilter in conjuncton with stemming. -->\n" +
                                                "        <filter class=\"solr.RemoveDuplicatesTokenFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- Just like text_general except it reverses the characters of\n" +
                                                "\t       each token, to enable more efficient leading wildcard queries.\n" +
                                                "    -->\n" +
                                                "    <dynamicField name=\"*_txt_rev\" type=\"text_general_rev\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_general_rev\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer type=\"index\">\n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" />\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.ReversedWildcardFilterFactory\" withOriginal=\"true\"\n" +
                                                "                maxPosAsterisk=\"3\" maxPosQuestion=\"2\" maxFractionAsterisk=\"0.33\"/>\n" +
                                                "      </analyzer>\n" +
                                                "      <analyzer type=\"query\">\n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.SynonymGraphFilterFactory\" synonyms=\"synonyms.txt\" ignoreCase=\"true\" expand=\"true\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"stopwords.txt\" />\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <dynamicField name=\"*_phon_en\" type=\"phonetic_en\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"phonetic_en\" stored=\"false\" indexed=\"true\" class=\"solr.TextField\" >\n" +
                                                "      <analyzer>\n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.DoubleMetaphoneFilterFactory\" inject=\"false\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- lowercases the entire field value, keeping it as a single token.  -->\n" +
                                                "    <dynamicField name=\"*_s_lower\" type=\"lowercase\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"lowercase\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer>\n" +
                                                "        <tokenizer class=\"solr.KeywordTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\" />\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- \n" +
                                                "      Example of using PathHierarchyTokenizerFactory at index time, so\n" +
                                                "      queries for paths match documents at that path, or in descendent paths\n" +
                                                "    -->\n" +
                                                "    <dynamicField name=\"*_descendent_path\" type=\"descendent_path\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"descendent_path\" class=\"solr.TextField\">\n" +
                                                "      <analyzer type=\"index\">\n" +
                                                "        <tokenizer class=\"solr.PathHierarchyTokenizerFactory\" delimiter=\"/\" />\n" +
                                                "      </analyzer>\n" +
                                                "      <analyzer type=\"query\">\n" +
                                                "        <tokenizer class=\"solr.KeywordTokenizerFactory\" />\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!--\n" +
                                                "      Example of using PathHierarchyTokenizerFactory at query time, so\n" +
                                                "      queries for paths match documents at that path, or in ancestor paths\n" +
                                                "    -->\n" +
                                                "    <dynamicField name=\"*_ancestor_path\" type=\"ancestor_path\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"ancestor_path\" class=\"solr.TextField\">\n" +
                                                "      <analyzer type=\"index\">\n" +
                                                "        <tokenizer class=\"solr.KeywordTokenizerFactory\" />\n" +
                                                "      </analyzer>\n" +
                                                "      <analyzer type=\"query\">\n" +
                                                "        <tokenizer class=\"solr.PathHierarchyTokenizerFactory\" delimiter=\"/\" />\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- This point type indexes the coordinates as separate fields (subFields)\n" +
                                                "      If subFieldType is defined, it references a type, and a dynamic field\n" +
                                                "      definition is created matching *___<typename>.  Alternately, if \n" +
                                                "      subFieldSuffix is defined, that is used to create the subFields.\n" +
                                                "      Example: if subFieldType=\"double\", then the coordinates would be\n" +
                                                "        indexed in fields myloc_0___double,myloc_1___double.\n" +
                                                "      Example: if subFieldSuffix=\"_d\" then the coordinates would be indexed\n" +
                                                "        in fields myloc_0_d,myloc_1_d\n" +
                                                "      The subFields are an implementation detail of the fieldType, and end\n" +
                                                "      users normally should not need to know about them.\n" +
                                                "     -->\n" +
                                                "    <dynamicField name=\"*_point\" type=\"point\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"point\" class=\"solr.PointType\" dimension=\"2\" subFieldSuffix=\"_d\"/>\n" +
                                                "\n" +
                                                "    <!-- A specialized field for geospatial search filters and distance sorting. -->\n" +
                                                "    <fieldType name=\"location\" class=\"solr.LatLonPointSpatialField\" docValues=\"true\"/>\n" +
                                                "\n" +
                                                "    <!-- A geospatial field type that supports multiValued and polygon shapes.\n" +
                                                "      For more information about this and other spatial fields see:\n" +
                                                "      http://lucene.apache.org/solr/guide/spatial-search.html\n" +
                                                "    -->\n" +
                                                "    <fieldType name=\"location_rpt\" class=\"solr.SpatialRecursivePrefixTreeFieldType\"\n" +
                                                "               geo=\"true\" distErrPct=\"0.025\" maxDistErr=\"0.001\" distanceUnits=\"kilometers\" />\n" +
                                                "\n" +
                                                "    <!-- Payloaded field types -->\n" +
                                                "    <fieldType name=\"delimited_payloads_float\" stored=\"false\" indexed=\"true\" class=\"solr.TextField\">\n" +
                                                "      <analyzer>\n" +
                                                "        <tokenizer class=\"solr.WhitespaceTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.DelimitedPayloadTokenFilterFactory\" encoder=\"float\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    <fieldType name=\"delimited_payloads_int\" stored=\"false\" indexed=\"true\" class=\"solr.TextField\">\n" +
                                                "      <analyzer>\n" +
                                                "        <tokenizer class=\"solr.WhitespaceTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.DelimitedPayloadTokenFilterFactory\" encoder=\"integer\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    <fieldType name=\"delimited_payloads_string\" stored=\"false\" indexed=\"true\" class=\"solr.TextField\">\n" +
                                                "      <analyzer>\n" +
                                                "        <tokenizer class=\"solr.WhitespaceTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.DelimitedPayloadTokenFilterFactory\" encoder=\"identity\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- some examples for different languages (generally ordered by ISO code) -->\n" +
                                                "\n" +
                                                "    <!-- Arabic -->\n" +
                                                "    <dynamicField name=\"*_txt_ar\" type=\"text_ar\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_ar\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <!-- for any non-arabic -->\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_ar.txt\" />\n" +
                                                "        <!-- normalizes ﻯ to ﻱ, etc -->\n" +
                                                "        <filter class=\"solr.ArabicNormalizationFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.ArabicStemFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- Bulgarian -->\n" +
                                                "    <dynamicField name=\"*_txt_bg\" type=\"text_bg\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_bg\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/> \n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_bg.txt\" /> \n" +
                                                "        <filter class=\"solr.BulgarianStemFilterFactory\"/>       \n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Catalan -->\n" +
                                                "    <dynamicField name=\"*_txt_ca\" type=\"text_ca\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_ca\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <!-- removes l', etc -->\n" +
                                                "        <filter class=\"solr.ElisionFilterFactory\" ignoreCase=\"true\" articles=\"lang/contractions_ca.txt\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_ca.txt\" />\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Catalan\"/>       \n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- CJK bigram (see text_ja for a Japanese configuration using morphological analysis) -->\n" +
                                                "    <dynamicField name=\"*_txt_cjk\" type=\"text_cjk\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_cjk\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer>\n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <!-- normalize width before bigram, as e.g. half-width dakuten combine  -->\n" +
                                                "        <filter class=\"solr.CJKWidthFilterFactory\"/>\n" +
                                                "        <!-- for any non-CJK -->\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.CJKBigramFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- Czech -->\n" +
                                                "    <dynamicField name=\"*_txt_cz\" type=\"text_cz\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_cz\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_cz.txt\" />\n" +
                                                "        <filter class=\"solr.CzechStemFilterFactory\"/>       \n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Danish -->\n" +
                                                "    <dynamicField name=\"*_txt_da\" type=\"text_da\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_da\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_da.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Danish\"/>       \n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- German -->\n" +
                                                "    <dynamicField name=\"*_txt_de\" type=\"text_de\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_de\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_de.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.GermanNormalizationFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.GermanLightStemFilterFactory\"/>\n" +
                                                "        <!-- less aggressive: <filter class=\"solr.GermanMinimalStemFilterFactory\"/> -->\n" +
                                                "        <!-- more aggressive: <filter class=\"solr.SnowballPorterFilterFactory\" language=\"German2\"/> -->\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Greek -->\n" +
                                                "    <dynamicField name=\"*_txt_el\" type=\"text_el\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_el\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <!-- greek specific lowercase for sigma -->\n" +
                                                "        <filter class=\"solr.GreekLowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"false\" words=\"lang/stopwords_el.txt\" />\n" +
                                                "        <filter class=\"solr.GreekStemFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Spanish -->\n" +
                                                "    <dynamicField name=\"*_txt_es\" type=\"text_es\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_es\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_es.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.SpanishLightStemFilterFactory\"/>\n" +
                                                "        <!-- more aggressive: <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Spanish\"/> -->\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Basque -->\n" +
                                                "    <dynamicField name=\"*_txt_eu\" type=\"text_eu\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_eu\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_eu.txt\" />\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Basque\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Persian -->\n" +
                                                "    <dynamicField name=\"*_txt_fa\" type=\"text_fa\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_fa\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer>\n" +
                                                "        <!-- for ZWNJ -->\n" +
                                                "        <charFilter class=\"solr.PersianCharFilterFactory\"/>\n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.ArabicNormalizationFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.PersianNormalizationFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_fa.txt\" />\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Finnish -->\n" +
                                                "    <dynamicField name=\"*_txt_fi\" type=\"text_fi\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_fi\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_fi.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Finnish\"/>\n" +
                                                "        <!-- less aggressive: <filter class=\"solr.FinnishLightStemFilterFactory\"/> -->\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- French -->\n" +
                                                "    <dynamicField name=\"*_txt_fr\" type=\"text_fr\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_fr\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <!-- removes l', etc -->\n" +
                                                "        <filter class=\"solr.ElisionFilterFactory\" ignoreCase=\"true\" articles=\"lang/contractions_fr.txt\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_fr.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.FrenchLightStemFilterFactory\"/>\n" +
                                                "        <!-- less aggressive: <filter class=\"solr.FrenchMinimalStemFilterFactory\"/> -->\n" +
                                                "        <!-- more aggressive: <filter class=\"solr.SnowballPorterFilterFactory\" language=\"French\"/> -->\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Irish -->\n" +
                                                "    <dynamicField name=\"*_txt_ga\" type=\"text_ga\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_ga\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <!-- removes d', etc -->\n" +
                                                "        <filter class=\"solr.ElisionFilterFactory\" ignoreCase=\"true\" articles=\"lang/contractions_ga.txt\"/>\n" +
                                                "        <!-- removes n-, etc. position increments is intentionally false! -->\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/hyphenations_ga.txt\"/>\n" +
                                                "        <filter class=\"solr.IrishLowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_ga.txt\"/>\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Irish\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Galician -->\n" +
                                                "    <dynamicField name=\"*_txt_gl\" type=\"text_gl\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_gl\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_gl.txt\" />\n" +
                                                "        <filter class=\"solr.GalicianStemFilterFactory\"/>\n" +
                                                "        <!-- less aggressive: <filter class=\"solr.GalicianMinimalStemFilterFactory\"/> -->\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Hindi -->\n" +
                                                "    <dynamicField name=\"*_txt_hi\" type=\"text_hi\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_hi\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <!-- normalizes unicode representation -->\n" +
                                                "        <filter class=\"solr.IndicNormalizationFilterFactory\"/>\n" +
                                                "        <!-- normalizes variation in spelling -->\n" +
                                                "        <filter class=\"solr.HindiNormalizationFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_hi.txt\" />\n" +
                                                "        <filter class=\"solr.HindiStemFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Hungarian -->\n" +
                                                "    <dynamicField name=\"*_txt_hu\" type=\"text_hu\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_hu\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_hu.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Hungarian\"/>\n" +
                                                "        <!-- less aggressive: <filter class=\"solr.HungarianLightStemFilterFactory\"/> -->   \n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Armenian -->\n" +
                                                "    <dynamicField name=\"*_txt_hy\" type=\"text_hy\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_hy\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_hy.txt\" />\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Armenian\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Indonesian -->\n" +
                                                "    <dynamicField name=\"*_txt_id\" type=\"text_id\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_id\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_id.txt\" />\n" +
                                                "        <!-- for a less aggressive approach (only inflectional suffixes), set stemDerivational to false -->\n" +
                                                "        <filter class=\"solr.IndonesianStemFilterFactory\" stemDerivational=\"true\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Italian -->\n" +
                                                "  <dynamicField name=\"*_txt_it\" type=\"text_it\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "  <fieldType name=\"text_it\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <!-- removes l', etc -->\n" +
                                                "        <filter class=\"solr.ElisionFilterFactory\" ignoreCase=\"true\" articles=\"lang/contractions_it.txt\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_it.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.ItalianLightStemFilterFactory\"/>\n" +
                                                "        <!-- more aggressive: <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Italian\"/> -->\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Japanese using morphological analysis (see text_cjk for a configuration using bigramming)\n" +
                                                "\n" +
                                                "         NOTE: If you want to optimize search for precision, use default operator AND in your request\n" +
                                                "         handler config (q.op) Use OR if you would like to optimize for recall (default).\n" +
                                                "    -->\n" +
                                                "    <dynamicField name=\"*_txt_ja\" type=\"text_ja\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_ja\" class=\"solr.TextField\" positionIncrementGap=\"100\" autoGeneratePhraseQueries=\"false\">\n" +
                                                "      <analyzer>\n" +
                                                "        <!-- Kuromoji Japanese morphological analyzer/tokenizer (JapaneseTokenizer)\n" +
                                                "\n" +
                                                "           Kuromoji has a search mode (default) that does segmentation useful for search.  A heuristic\n" +
                                                "           is used to segment compounds into its parts and the compound itself is kept as synonym.\n" +
                                                "\n" +
                                                "           Valid values for attribute mode are:\n" +
                                                "              normal: regular segmentation\n" +
                                                "              search: segmentation useful for search with synonyms compounds (default)\n" +
                                                "            extended: same as search mode, but unigrams unknown words (experimental)\n" +
                                                "\n" +
                                                "           For some applications it might be good to use search mode for indexing and normal mode for\n" +
                                                "           queries to reduce recall and prevent parts of compounds from being matched and highlighted.\n" +
                                                "           Use <analyzer type=\"index\"> and <analyzer type=\"query\"> for this and mode normal in query.\n" +
                                                "\n" +
                                                "           Kuromoji also has a convenient user dictionary feature that allows overriding the statistical\n" +
                                                "           model with your own entries for segmentation, part-of-speech tags and readings without a need\n" +
                                                "           to specify weights.  Notice that user dictionaries have not been subject to extensive testing.\n" +
                                                "\n" +
                                                "           User dictionary attributes are:\n" +
                                                "                     userDictionary: user dictionary filename\n" +
                                                "             userDictionaryEncoding: user dictionary encoding (default is UTF-8)\n" +
                                                "\n" +
                                                "           See lang/userdict_ja.txt for a sample user dictionary file.\n" +
                                                "\n" +
                                                "           Punctuation characters are discarded by default.  Use discardPunctuation=\"false\" to keep them.\n" +
                                                "        -->\n" +
                                                "        <tokenizer class=\"solr.JapaneseTokenizerFactory\" mode=\"search\"/>\n" +
                                                "        <!--<tokenizer class=\"solr.JapaneseTokenizerFactory\" mode=\"search\" userDictionary=\"lang/userdict_ja.txt\"/>-->\n" +
                                                "        <!-- Reduces inflected verbs and adjectives to their base/dictionary forms (辞書形) -->\n" +
                                                "        <filter class=\"solr.JapaneseBaseFormFilterFactory\"/>\n" +
                                                "        <!-- Removes tokens with certain part-of-speech tags -->\n" +
                                                "        <filter class=\"solr.JapanesePartOfSpeechStopFilterFactory\" tags=\"lang/stoptags_ja.txt\" />\n" +
                                                "        <!-- Normalizes full-width romaji to half-width and half-width kana to full-width (Unicode NFKC subset) -->\n" +
                                                "        <filter class=\"solr.CJKWidthFilterFactory\"/>\n" +
                                                "        <!-- Removes common tokens typically not useful for search, but have a negative effect on ranking -->\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_ja.txt\" />\n" +
                                                "        <!-- Normalizes common katakana spelling variations by removing any last long sound character (U+30FC) -->\n" +
                                                "        <filter class=\"solr.JapaneseKatakanaStemFilterFactory\" minimumLength=\"4\"/>\n" +
                                                "        <!-- Lower-cases romaji characters -->\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Korean morphological analysis -->\n" +
                                                "    <dynamicField name=\"*_txt_ko\" type=\"text_ko\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_ko\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer>\n" +
                                                "        <!-- Nori Korean morphological analyzer/tokenizer (KoreanTokenizer)\n" +
                                                "          The Korean (nori) analyzer integrates Lucene nori analysis module into Solr.\n" +
                                                "          It uses the mecab-ko-dic dictionary to perform morphological analysis of Korean texts.\n" +
                                                "\n" +
                                                "          This dictionary was built with MeCab, it defines a format for the features adapted\n" +
                                                "          for the Korean language.\n" +
                                                "          \n" +
                                                "          Nori also has a convenient user dictionary feature that allows overriding the statistical\n" +
                                                "          model with your own entries for segmentation, part-of-speech tags and readings without a need\n" +
                                                "          to specify weights. Notice that user dictionaries have not been subject to extensive testing.\n" +
                                                "\n" +
                                                "          The tokenizer supports multiple schema attributes:\n" +
                                                "            * userDictionary: User dictionary path.\n" +
                                                "            * userDictionaryEncoding: User dictionary encoding.\n" +
                                                "            * decompoundMode: Decompound mode. Either 'none', 'discard', 'mixed'. Default is 'discard'.\n" +
                                                "            * outputUnknownUnigrams: If true outputs unigrams for unknown words.\n" +
                                                "        -->\n" +
                                                "        <tokenizer class=\"solr.KoreanTokenizerFactory\" decompoundMode=\"discard\" outputUnknownUnigrams=\"false\"/>\n" +
                                                "        <!-- Removes some part of speech stuff like EOMI (Pos.E), you can add a parameter 'tags',\n" +
                                                "          listing the tags to remove. By default it removes: \n" +
                                                "          E, IC, J, MAG, MAJ, MM, SP, SSC, SSO, SC, SE, XPN, XSA, XSN, XSV, UNA, NA, VSV\n" +
                                                "          This is basically an equivalent to stemming.\n" +
                                                "        -->\n" +
                                                "        <filter class=\"solr.KoreanPartOfSpeechStopFilterFactory\" />\n" +
                                                "        <!-- Replaces term text with the Hangul transcription of Hanja characters, if applicable: -->\n" +
                                                "        <filter class=\"solr.KoreanReadingFormFilterFactory\" />\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\" />\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- Latvian -->\n" +
                                                "    <dynamicField name=\"*_txt_lv\" type=\"text_lv\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_lv\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_lv.txt\" />\n" +
                                                "        <filter class=\"solr.LatvianStemFilterFactory\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Dutch -->\n" +
                                                "    <dynamicField name=\"*_txt_nl\" type=\"text_nl\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_nl\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_nl.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.StemmerOverrideFilterFactory\" dictionary=\"lang/stemdict_nl.txt\" ignoreCase=\"false\"/>\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Dutch\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Norwegian -->\n" +
                                                "    <dynamicField name=\"*_txt_no\" type=\"text_no\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_no\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_no.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Norwegian\"/>\n" +
                                                "        <!-- less aggressive: <filter class=\"solr.NorwegianLightStemFilterFactory\"/> -->\n" +
                                                "        <!-- singular/plural: <filter class=\"solr.NorwegianMinimalStemFilterFactory\"/> -->\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Portuguese -->\n" +
                                                "  <dynamicField name=\"*_txt_pt\" type=\"text_pt\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "  <fieldType name=\"text_pt\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_pt.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.PortugueseLightStemFilterFactory\"/>\n" +
                                                "        <!-- less aggressive: <filter class=\"solr.PortugueseMinimalStemFilterFactory\"/> -->\n" +
                                                "        <!-- more aggressive: <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Portuguese\"/> -->\n" +
                                                "        <!-- most aggressive: <filter class=\"solr.PortugueseStemFilterFactory\"/> -->\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Romanian -->\n" +
                                                "    <dynamicField name=\"*_txt_ro\" type=\"text_ro\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_ro\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_ro.txt\" />\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Romanian\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Russian -->\n" +
                                                "    <dynamicField name=\"*_txt_ru\" type=\"text_ru\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_ru\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_ru.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Russian\"/>\n" +
                                                "        <!-- less aggressive: <filter class=\"solr.RussianLightStemFilterFactory\"/> -->\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Swedish -->\n" +
                                                "    <dynamicField name=\"*_txt_sv\" type=\"text_sv\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_sv\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_sv.txt\" format=\"snowball\" />\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Swedish\"/>\n" +
                                                "        <!-- less aggressive: <filter class=\"solr.SwedishLightStemFilterFactory\"/> -->\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Thai -->\n" +
                                                "    <dynamicField name=\"*_txt_th\" type=\"text_th\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_th\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer>\n" +
                                                "        <tokenizer class=\"solr.ThaiTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" words=\"lang/stopwords_th.txt\" />\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "    \n" +
                                                "    <!-- Turkish -->\n" +
                                                "    <dynamicField name=\"*_txt_tr\" type=\"text_tr\"  indexed=\"true\"  stored=\"true\"/>\n" +
                                                "    <fieldType name=\"text_tr\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                                "      <analyzer> \n" +
                                                "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                                "        <filter class=\"solr.TurkishLowerCaseFilterFactory\"/>\n" +
                                                "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"false\" words=\"lang/stopwords_tr.txt\" />\n" +
                                                "        <filter class=\"solr.SnowballPorterFilterFactory\" language=\"Turkish\"/>\n" +
                                                "      </analyzer>\n" +
                                                "    </fieldType>\n" +
                                                "\n" +
                                                "    <!-- Similarity is the scoring routine for each document vs. a query.\n" +
                                                "       A custom Similarity or SimilarityFactory may be specified here, but \n" +
                                                "       the default is fine for most applications.  \n" +
                                                "       For more info: http://lucene.apache.org/solr/guide/other-schema-elements.html#OtherSchemaElements-Similarity\n" +
                                                "    -->\n" +
                                                "    <!--\n" +
                                                "     <similarity class=\"com.example.solr.CustomSimilarityFactory\">\n" +
                                                "       <str name=\"paramkey\">param value</str>\n" +
                                                "     </similarity>\n" +
                                                "    -->\n" +
                                                "\n" +
                                                "</schema>\n";

    //This is a list of field types that are used below
    public static final String CORE_SCHEMA_TYPE_NEST_PATH = "_nest_path_";
    public static final String CORE_SCHEMA_TYPE_BOOLEAN = "boolean";
    public static final String CORE_SCHEMA_TYPE_BOOLEANS = "booleans";
    public static final String CORE_SCHEMA_TYPE_PINT = "pint";
    public static final String CORE_SCHEMA_TYPE_PINTS = "pints";
    public static final String CORE_SCHEMA_TYPE_PFLOAT = "pfloat";
    public static final String CORE_SCHEMA_TYPE_PFLOATS = "pfloats";
    public static final String CORE_SCHEMA_TYPE_PLONG = "plong";
    public static final String CORE_SCHEMA_TYPE_PLONGS = "plongs";
    public static final String CORE_SCHEMA_TYPE_PDOUBLE = "pdouble";
    public static final String CORE_SCHEMA_TYPE_PDOUBLES = "pdoubles";
    public static final String CORE_SCHEMA_TYPE_PDATE = "pdate";
    public static final String CORE_SCHEMA_TYPE_PDATES = "pdates";
    public static final String CORE_SCHEMA_TYPE_STRING = "string";
    public static final String CORE_SCHEMA_TYPE_STRINGS = "strings";
    public static final String CORE_SCHEMA_TYPE_RANDOM = "random";
    public static final String CORE_SCHEMA_TYPE_IGNORED = "ignored";
    public static final String CORE_SCHEMA_TYPE_BINARY = "binary";
    public static final String CORE_SCHEMA_TYPE_TEXT_GENERAL = "text_general";
    public static final String CORE_SCHEMA_TYPE_TEXT_GENERAL_REV = "text_general_rev";
    public static final String CORE_SCHEMA_TYPE_TEXT_SORTABLE = "text_sort";

    //solr-internal fields
    public static SolrField _version_ = new SolrField("_version_", CORE_SCHEMA_TYPE_PLONG);
    public static SolrField _root_ = new SolrField("_root_", CORE_SCHEMA_TYPE_STRING);
    public static SolrField _nest_path_ = new SolrField("_nest_path_", CORE_SCHEMA_TYPE_NEST_PATH);
    public static SolrField _nest_parent_ = new SolrField("_nest_parent_", CORE_SCHEMA_TYPE_STRING);
    public static SolrField _text_ = new SolrField("_text_", CORE_SCHEMA_TYPE_TEXT_SORTABLE);

    // Sync this with the field list above !
    public static SolrField[] RESERVED_FIELDS = {
                    _version_,
                    _root_,
                    _nest_path_,
                    _nest_parent_,
                    _text_
    };

    //our internal fields
    public static final String CORE_SCHEMA_FIELD_URI = ResourceIndexEntry.uriField.getName();
    public static final String CORE_SCHEMA_FIELD_TOKENISED_URI = ResourceIndexEntry.tokenisedUriField.getName();
    public static final String CORE_SCHEMA_FIELD_RESOURCE_TYPE = ResourceIndexEntry.resourceTypeField.getName();
    public static final String CORE_SCHEMA_FIELD_LABEL = ResourceIndexEntry.labelField.getName();
    public static final String CORE_SCHEMA_FIELD_DESCRIPTION = ResourceIndexEntry.descriptionField.getName();
    public static final String CORE_SCHEMA_FIELD_IMAGE = ResourceIndexEntry.imageField.getName();
    public static final String CORE_SCHEMA_FIELD_PARENT_URI = PageIndexEntry.parentUriField.getName();
    public static final String CORE_SCHEMA_FIELD_RESOURCE = PageIndexEntry.resourceField.getName();
    public static final String CORE_SCHEMA_FIELD_TYPE_OF = PageIndexEntry.typeOfField.getName();
    public static final String CORE_SCHEMA_FIELD_LANGUAGE = PageIndexEntry.languageField.getName();

    // For details, see https://lucene.apache.org/solr/guide/7_7/field-types-included-with-solr.html#field-types-included-with-solr
    public static final String CORE_SCHEMA = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                                             "<schema name=\"default-config\" version=\"1.6\">\n" +

                                             // To be used for field {@link IndexSchema#NEST_PATH_FIELD_NAME} for enhanced nested doc information.
                                             // By defining a field type, we can encapsulate the configuration here so that the schema is free of it.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_NEST_PATH + "\" class=\"solr.NestPathField\" />\n" +

                                             // Contains either true or false. Values of 1, t, or T in the first character are interpreted as true.
                                             // Any other values in the first character are interpreted as false.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_BOOLEAN + "\" class=\"solr.BoolField\" sortMissingLast=\"true\"/>\n" +
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_BOOLEANS + "\" class=\"solr.BoolField\" sortMissingLast=\"true\" multiValued=\"true\"/>\n" +

                                             // Integer field (32-bit signed integer). This class encodes int values using a "Dimensional Points" based data structure that allows
                                             // for very efficient searches for specific values, or ranges of values. For single valued fields, docValues="true" must be used to enable sorting.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_PINT + "\" class=\"solr.IntPointField\" docValues=\"true\"/>\n" +
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_PINTS + "\" class=\"solr.IntPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +

                                             // Floating point field (32-bit IEEE floating point). This class encodes float values using a "Dimensional Points" based data structure that allows
                                             // for very efficient searches for specific values, or ranges of values. For single valued fields, docValues="true" must be used to enable sorting.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_PFLOAT + "\" class=\"solr.FloatPointField\" docValues=\"true\"/>\n" +
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_PFLOATS + "\" class=\"solr.FloatPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +

                                             // Long field (64-bit signed integer). This class encodes foo values using a "Dimensional Points" based data structure that allows
                                             // for very efficient searches for specific values, or ranges of values. For single valued fields, docValues="true" must be used to enable sorting.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_PLONG + "\" class=\"solr.LongPointField\" docValues=\"true\"/>\n" +
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_PLONGS + "\" class=\"solr.LongPointField\" docValues=\"true\" multiValued=\"true\"/>\n" +

                                             // Double field (64-bit IEEE floating point). This class encodes double values using a "Dimensional Points" based data structure that allows
                                             // for very efficient searches for specific values, or ranges of values. For single valued fields, docValues="true" must be used to enable sorting.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_PDOUBLE + "\" class=\"solr.DoublePointField\" docValues=\"true\"/>\n" +
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_PDOUBLES + "\" class=\"solr.DoublePointField\" docValues=\"true\" multiValued=\"true\"/>\n" +

                                             // Date field. Represents a point in time with millisecond precision, encoded using a "Dimensional Points" based data structure that allows
                                             // for very efficient searches for specific values, or ranges of values. See the section Working with Dates for more details on the supported syntax.
                                             // For single valued fields, docValues="true" must be used to enable sorting.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_PDATE + "\" class=\"solr.DatePointField\" docValues=\"true\"/>\n" +
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_PDATES + "\" class=\"solr.DatePointField\" docValues=\"true\" multiValued=\"true\"/>\n" +

                                             // String (UTF-8 encoded string or Unicode). Strings are intended for small fields and are not tokenized or analyzed in any way.
                                             // They have a hard limit of slightly less than 32K.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_STRING + "\" class=\"solr.StrField\" sortMissingLast=\"true\" docValues=\"true\" />\n" +
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_STRINGS + "\" class=\"solr.StrField\" sortMissingLast=\"true\" multiValued=\"true\" docValues=\"true\" />\n" +

                                             // Does not contain a value. Queries that sort on this field type will return results in random order. Use a dynamic field to use this feature.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_RANDOM + "\" class=\"solr.RandomSortField\" indexed=\"true\"/>\n" +

                                             // Note: this is an unstored, unindexed string field
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_IGNORED + "\" stored=\"false\" indexed=\"false\" multiValued=\"true\" class=\"solr.StrField\" />\n" +

                                             // Binary data.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_BINARY + "\" class=\"solr.BinaryField\"/>\n" +

                                             // Text, usually multiple words or tokens.
                                             // Note: positionIncrementGap is used for phrase query of multi-value fields
                                             // e.g. doc1 has two titles.
                                             //   title1: ab cd
                                             //   title2: xy zz
                                             // If your positionIncrementGap is 0, then the position of the 4 terms are 0,1,2,3. If you search phrase "cd xy", it will hit.
                                             // But you may think it should not match so you can adjust positionIncrementGap to a larger one. e.g. 100.
                                             // Then the positions now are 0,1,100,101. the phrase query will not match it.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_TEXT_GENERAL + "\" class=\"solr.TextField\" positionIncrementGap=\"100\" multiValued=\"true\">\n" +
                                             // We switched to using this analyzer instead of the standard one because of better support for french words.
                                             // Eg. if the user looks for "écope à grain" and the indexed string was "Ecope à grain", the standard analyzer won't find it.
                                             "      <analyzer type=\"index\">\n" +
                                             "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                             "        <filter class=\"solr.ASCIIFoldingFilterFactory\" preserveOriginal=\"false\" />\n" +
                                             "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                             "        <filter class=\"solr.StopFilterFactory\" />" +
                                             "      </analyzer>\n" +
                                             "      <analyzer type=\"query\">\n" +
                                             "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                             "        <filter class=\"solr.ASCIIFoldingFilterFactory\" preserveOriginal=\"false\" />\n" +
                                             "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                             "        <filter class=\"solr.StopFilterFactory\" />" +
                                             "      </analyzer>\n" +
                                             "    </fieldType>\n" +

                                             // SortableTextField generaly functions exactly like TextField,
                                             // except that it supports, and by default uses, docValues for sorting (or faceting)
                                             // on the first 1024 characters of the original field values (which is configurable).
                                             // This makes it a bit more useful then TextField in many situations, but the trade-off
                                             // is that it takes up more space on disk; which is why it's not used in place of TextField
                                             // for every fieldType in this _default schema.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_TEXT_SORTABLE + "\" class=\"solr.SortableTextField\" positionIncrementGap=\"100\" multiValued=\"true\">\n" +
                                             "      <analyzer type=\"index\">\n" +
                                             "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                             "        <filter class=\"solr.ASCIIFoldingFilterFactory\" preserveOriginal=\"false\" />\n" +
                                             "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" />\n" +
                                             "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                             "      </analyzer>\n" +
                                             "      <analyzer type=\"query\">\n" +
                                             "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                             "        <filter class=\"solr.ASCIIFoldingFilterFactory\" preserveOriginal=\"false\" />\n" +
                                             "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" />\n" +
                                             "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                             "      </analyzer>\n" +
                                             "    </fieldType>\n" +

                                             // Just like text_general except it reverses the characters of
                                             // each token, to enable more efficient leading wildcard queries.
                                             "    <fieldType name=\"" + CORE_SCHEMA_TYPE_TEXT_GENERAL_REV + "\" class=\"solr.TextField\" positionIncrementGap=\"100\">\n" +
                                             "      <analyzer type=\"index\">\n" +
                                             "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                             "        <filter class=\"solr.ASCIIFoldingFilterFactory\" preserveOriginal=\"false\" />\n" +
                                             "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" />\n" +
                                             "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                             "        <filter class=\"solr.ReversedWildcardFilterFactory\" withOriginal=\"true\" maxPosAsterisk=\"3\" maxPosQuestion=\"2\" maxFractionAsterisk=\"0.33\"/>\n" +
                                             "      </analyzer>\n" +
                                             "      <analyzer type=\"query\">\n" +
                                             "        <tokenizer class=\"solr.StandardTokenizerFactory\"/>\n" +
                                             "        <filter class=\"solr.ASCIIFoldingFilterFactory\" preserveOriginal=\"false\" />\n" +
                                             "        <filter class=\"solr.StopFilterFactory\" ignoreCase=\"true\" />\n" +
                                             "        <filter class=\"solr.LowerCaseFilterFactory\"/>\n" +
                                             "      </analyzer>\n" +
                                             "    </fieldType>\n" +

                                             // Note: no default value means they will be omitted when not provided
                                             // (the default value used to be "null", we we changed our mind,
                                             // see https://github.com/republic-of-reinvention/com.stralo.framework/issues/60)
                                             "    <field name=\"" + CORE_SCHEMA_FIELD_URI + "\" type=\"" + CORE_SCHEMA_TYPE_STRING +
                                             "\" indexed=\"true\" stored=\"true\" required=\"true\" multiValued=\"false\" />\n" +
                                             "    <field name=\"" + CORE_SCHEMA_FIELD_TOKENISED_URI + "\" type=\"" + CORE_SCHEMA_TYPE_TEXT_GENERAL +
                                             "\" indexed=\"true\" stored=\"false\" required=\"true\" multiValued=\"false\" />\n" +
                                             "    <copyField source=\"" + CORE_SCHEMA_FIELD_URI + "\" dest=\"" + CORE_SCHEMA_FIELD_TOKENISED_URI + "\"/>\n" +
                                             "    <field name=\"" + CORE_SCHEMA_FIELD_RESOURCE_TYPE + "\" type=\"" + CORE_SCHEMA_TYPE_STRING +
                                             "\" indexed=\"true\" stored=\"true\" required=\"true\" multiValued=\"false\" />\n" +
                                             "    <field name=\"" + CORE_SCHEMA_FIELD_LABEL + "\" type=\"" + CORE_SCHEMA_TYPE_TEXT_GENERAL +
                                             "\" indexed=\"true\" stored=\"true\" required=\"false\" multiValued=\"false\" />\n" +
                                             "    <field name=\"" + CORE_SCHEMA_FIELD_DESCRIPTION + "\" type=\"" + CORE_SCHEMA_TYPE_TEXT_GENERAL +
                                             "\" indexed=\"true\" stored=\"true\" required=\"false\" multiValued=\"false\" />\n" +
                                             "    <field name=\"" + CORE_SCHEMA_FIELD_IMAGE + "\" type=\"" + CORE_SCHEMA_TYPE_STRING +
                                             "\" indexed=\"true\" stored=\"true\" required=\"false\" multiValued=\"false\" />\n" +
                                             "    <field name=\"" + CORE_SCHEMA_FIELD_PARENT_URI + "\" type=\"" + CORE_SCHEMA_TYPE_STRING +
                                             "\" indexed=\"true\" stored=\"true\" required=\"false\" multiValued=\"false\" />\n" +
                                             "    <field name=\"" + CORE_SCHEMA_FIELD_RESOURCE + "\" type=\"" + CORE_SCHEMA_TYPE_STRING +
                                             "\" indexed=\"true\" stored=\"true\" required=\"false\" multiValued=\"false\" />\n" +
                                             "    <field name=\"" + CORE_SCHEMA_FIELD_TYPE_OF + "\" type=\"" + CORE_SCHEMA_TYPE_STRING +
                                             "\" indexed=\"true\" stored=\"true\" required=\"false\" multiValued=\"false\" />\n" +
                                             "    <field name=\"" + CORE_SCHEMA_FIELD_LANGUAGE + "\" type=\"" + CORE_SCHEMA_TYPE_STRING +
                                             "\" indexed=\"true\" stored=\"true\" required=\"false\" multiValued=\"false\" />\n" +

                                             // To support Optimistic Concurrency; see https://lucene.apache.org/solr/guide/7_7/updating-parts-of-documents.html#optimistic-concurrency
                                             "    <field name=\"" + _version_.getName() + "\" type=\"" + _version_.getType() + "\" indexed=\"false\" stored=\"false\"/>\n" +

                                             // To support block-join support; see https://lucene.apache.org/solr/guide/7_7/uploading-data-with-index-handlers.html#nested-child-documents
                                             // For nested documents (minimal; points to root document)
                                             "    <field name=\"" + _root_.getName() + "\" type=\"" + _root_.getType() + "\" indexed=\"true\" stored=\"false\" docValues=\"false\" />\n" +

                                             // For nested documents (relationship tracking)
                                             "    <field name=\"" + _nest_path_.getName() + "\" type=\"" + _nest_path_.getType() + "\" />\n" +
                                             "    <field name=\"" + _nest_parent_.getName() + "\" type=\"" + _nest_parent_.getType() + "\" />\n" +

                                             // This field is there in case the client does not know what fields may be searched to support Google-style search over everything.
                                             // Note that this effectively indexes everything twice, so this is an expensive feature, both in terms of disk space and indexing speed
                                             "    <field name=\"" + _text_.getName() + "\" type=\"" + _text_.getType() + "\" indexed=\"true\" stored=\"false\" multiValued=\"true\"/>\n" +
                                             "    <copyField source=\"*\" dest=\"" + _text_.getName() + "\"/>\n" +

                                             // Field to use to determine and enforce document uniqueness.
                                             "    <uniqueKey>" + CORE_SCHEMA_FIELD_URI + "</uniqueKey>\n" +

                                             "</schema>\n";

}
