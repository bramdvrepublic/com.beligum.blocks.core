package com.beligum.blocks.fs.hdfs.bitronix;

import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAStatefulHolder;
import bitronix.tm.resource.ehcache.EhCacheXAResourceProducer;
import com.beligum.base.utils.Logger;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.xa.XAResource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Inspired by {@link EhCacheXAResourceProducer}.
 * <p>
 * Copied from http://blog.trixi.cz/2011/11/jta-transaction-manager-atomikos-or-bitronix/
 *
 * @author gargii
 */
public class XAResourceProducer extends ResourceBean implements bitronix.tm.resource.common.XAResourceProducer
{
    //-----CONSTANTS-----
    private static final long serialVersionUID = -1026425674409225116L;

    private static final ConcurrentMap<String, XAResourceProducer> producers = new ConcurrentHashMap<>();

    //-----VARIABLES-----
    private final ConcurrentMap<Integer, XAResourceHolder> xaResourceHolders = new ConcurrentHashMap<>();
    private final AtomicInteger xaResourceHolderCounter = new AtomicInteger();
    private volatile RecoveryXAResourceHolder recoveryXAResourceHolder;

    //-----CONSTRUCTORS-----
    private XAResourceProducer()
    {
        setApplyTransactionTimeout(true);
    }

    //-----STATIS METHODS-----
    /**
     * Register an XAResource of a cache with BTM. The first time a XAResource is registered a new
     * EhCacheXAResourceProducer is created to hold it.
     *
     * @param uniqueName the uniqueName of this XAResourceProducer, usually the cache's name
     * @param xaResource the XAResource to be registered
     */
    public static void registerXAResource(String uniqueName, XAResource xaResource)
    {
        XAResourceProducer xaResourceProducer = producers.get(uniqueName);
        if (xaResourceProducer == null) {
            xaResourceProducer = new XAResourceProducer();
            xaResourceProducer.setUniqueName(uniqueName);
            // the initial xaResource must be added before init() can be called
            xaResourceProducer.addXAResource(xaResource);

            XAResourceProducer previous = producers.putIfAbsent(uniqueName, xaResourceProducer);
            if (previous == null) {
                xaResourceProducer.init();
            }
            else {
                previous.addXAResource(xaResource);
            }
        }
        else {
            xaResourceProducer.addXAResource(xaResource);
        }
    }

    /**
     * Unregister an XAResource of a cache from BTM.
     *
     * @param uniqueName the uniqueName of this XAResourceProducer, usually the cache's name
     * @param xaResource the XAResource to be registered
     */
    public static void unregisterXAResource(String uniqueName, XAResource xaResource)
    {
        XAResourceProducer xaResourceProducer = producers.get(uniqueName);

        if (xaResourceProducer != null) {
            boolean found = xaResourceProducer.removeXAResource(xaResource);
            if (!found) {
                Logger.error("no XAResource " + xaResource + " found in XAResourceProducer with name " + uniqueName);
            }
            if (xaResourceProducer.xaResourceHolders.isEmpty()) {
                xaResourceProducer.close();
                producers.remove(uniqueName);
            }
        }
        else {
            Logger.error("no XAResourceProducer registered with name " + uniqueName);
        }
    }

    /**
     * Since manually registered resource pools are left untouched by BitronixTransactionManager.shutdown(), we need to do this manually
     */
    public static void shutdown()
    {
        for (Map.Entry<String, XAResourceProducer> e : producers.entrySet()) {
            e.getValue().close();
        }
        producers.clear();
    }

    //-----PUBLIC METHODS-----
    /**
     * {@inheritDoc}
     */
    public XAResourceHolderState startRecovery() throws RecoveryException
    {
        if (recoveryXAResourceHolder != null) {
            throw new RecoveryException("recovery already in progress on " + this);
        }

        if (xaResourceHolders.isEmpty()) {
            throw new RecoveryException("no XAResource registered, recovery cannot be done on " + this);
        }

        recoveryXAResourceHolder = new RecoveryXAResourceHolder(xaResourceHolders.values().iterator().next());
        return new XAResourceHolderState(recoveryXAResourceHolder, this);
    }

    /**
     * {@inheritDoc}
     */
    public void endRecovery() throws RecoveryException
    {
        recoveryXAResourceHolder = null;
    }

    /**
     * {@inheritDoc}
     */
    public void setFailed(boolean failed)
    {
        // cache cannot fail as it's not connection oriented
    }

    /**
     * {@inheritDoc}
     */
    public XAResourceHolder findXAResourceHolder(XAResource xaResource)
    {
        for (XAResourceHolder xaResourceHolder : xaResourceHolders.values()) {
            if (xaResource == xaResourceHolder.getXAResource()) {
                return xaResourceHolder;
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void init()
    {
        try {
            ResourceRegistrar.register(this);
        }
        catch (RecoveryException ex) {
            throw new BitronixRuntimeException("error recovering " + this, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close()
    {
        xaResourceHolders.clear();
        xaResourceHolderCounter.set(0);
        ResourceRegistrar.unregister(this);
    }

    /**
     * {@inheritDoc}
     */
    public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception
    {
        throw new UnsupportedOperationException("Ehcache is not connection-oriented");
    }

    /**
     * {@inheritDoc}
     */
    public Reference getReference() throws NamingException
    {
        return new Reference(EhCacheXAResourceProducer.class.getName(),
                             new StringRefAddr("uniqueName", getUniqueName()),
                             ResourceObjectFactory.class.getName(), null);
    }

    //-----PRIVATE METHODS-----
    private void addXAResource(XAResource xaResource)
    {
        XAResourceHolder xaResourceHolder = new XAResourceHolder(xaResource, this);
        int key = xaResourceHolderCounter.incrementAndGet();

        xaResourceHolders.put(key, xaResourceHolder);
    }

    private boolean removeXAResource(XAResource xaResource)
    {
        for (Map.Entry<Integer, XAResourceHolder> entry : xaResourceHolders.entrySet()) {
            Integer key = entry.getKey();
            XAResourceHolder xaResourceHolder = entry.getValue();
            if (xaResourceHolder.getXAResource() == xaResource) {
                xaResourceHolders.remove(key);
                return true;
            }
        }
        return false;
    }

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "a XAResourceProducer with uniqueName " + getUniqueName();
    }
}
