package com.beligum.blocks.fs.hdfs.bitronix;

import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.*;
import bitronix.tm.resource.ehcache.EhCacheXAResourceProducer;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.xa.XAResource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final Map<String, XAResourceProducer> PRODUCERS = new HashMap<>();

    //-----VARIABLES-----
    private final List<XAResourceHolder> xaResourceHolders;
    private RecoveryXAResourceHolder recoveryXAResourceHolder;

    //-----CONSTRUCTORS-----
    private XAResourceProducer()
    {
        this.xaResourceHolders = new ArrayList<>();

        setApplyTransactionTimeout(true);
    }

    //-----STATIS METHODS-----
    /**
     * Register an XAResource of a XADisk with BTM. The first time a XAResource is registered a new
     * XaDiskResourceProducer is created to hold it.
     *
     * @param uniqueName the uniqueName of this XAResourceProducer, usually the XADisk's name
     * @param xaResource the XAResource to be registered
     */
    public static void registerXAResource(String uniqueName, XAResource xaResource)
    {
        synchronized (PRODUCERS) {
            XAResourceProducer xaResourceProducer = PRODUCERS.get(uniqueName);

            if (xaResourceProducer == null) {
                xaResourceProducer = new XAResourceProducer();
                xaResourceProducer.setUniqueName(uniqueName);
                // the initial xaResource must be added before init() is called
                xaResourceProducer.addXAResource(xaResource);
                xaResourceProducer.init();

                PRODUCERS.put(uniqueName, xaResourceProducer);
            }
            else {
                xaResourceProducer.addXAResource(xaResource);

                if (xaResourceProducer.xaResourceHolders.size() == 1)
                    // was empty, init needed
                    xaResourceProducer.init();
            }
        }
    }

    /**
     * Unregister an XAResource of a XADisk from BTM.
     *
     * @param uniqueName the uniqueName of this XAResourceProducer, usually the XADisk's name
     * @param xaResource the XAResource to be registered
     */
    public static synchronized void unregisterXAResource(String uniqueName, XAResource xaResource)
    {
        synchronized (PRODUCERS) {
            XAResourceProducer xaResourceProducer = PRODUCERS.get(uniqueName);

            if (xaResourceProducer != null) {
                boolean found = xaResourceProducer.removeXAResource(xaResource);
                if (!found) {
                    com.beligum.base.utils.Logger.error("no XAResource " + xaResource + " found in XAResourceProducer with name " + uniqueName);
                }

                if (xaResourceProducer.xaResourceHolders.isEmpty()) {
                    ResourceRegistrar.unregister(xaResourceProducer);
                }
            }
            else {
                com.beligum.base.utils.Logger.error("no XAResourceProducer registered with name " + uniqueName);
            }
        }
    }

    //-----PUBLIC METHODS-----
    /**
     * {@inheritDoc}
     */
    @Override
    public XAResourceHolderState startRecovery() throws RecoveryException
    {
        synchronized (xaResourceHolders) {
            if (recoveryXAResourceHolder != null) {
                throw new RecoveryException("recovery already in progress on " + this);
            }

            if (xaResourceHolders.isEmpty()) {
                throw new RecoveryException("no XAResource registered, recovery cannot be done on " + this);
            }

            recoveryXAResourceHolder = new RecoveryXAResourceHolder(xaResourceHolders.get(0));
            return new XAResourceHolderState(recoveryXAResourceHolder, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endRecovery() throws RecoveryException
    {
        recoveryXAResourceHolder = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFailed(boolean failed)
    {
        // XADisk cannot fail as it's not connection oriented
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public bitronix.tm.resource.common.XAResourceHolder findXAResourceHolder(XAResource xaResource)
    {
        synchronized (xaResourceHolders) {
            for (int i = 0; i < xaResourceHolders.size(); i++) {
                XAResourceHolder xaResourceHolder = xaResourceHolders.get(i);
                if (xaResource == xaResourceHolder.getXAResource()) {
                    return xaResourceHolder;
                }
            }

            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init()
    {
        try {
            ResourceRegistrar.register(this);
        }
        catch (RecoveryException e) {
            throw new BitronixRuntimeException("error recovering " + this, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
    {
        synchronized (xaResourceHolders) {
            xaResourceHolders.clear();
            ResourceRegistrar.unregister(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception
    {
        throw new UnsupportedOperationException("XaDisk is not connection-oriented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reference getReference() throws NamingException
    {
        return new Reference(XAResourceProducer.class.getName(), new StringRefAddr("uniqueName", getUniqueName()), ResourceObjectFactory.class.getName(), null);
    }

    //-----PRIVATE METHODS-----
    private void addXAResource(XAResource xaResource)
    {
        synchronized (xaResourceHolders) {
            XAResourceHolder xaResourceHolder = new XAResourceHolder(xaResource, this);

            xaResourceHolders.add(xaResourceHolder);
        }
    }
    private boolean removeXAResource(XAResource xaResource)
    {
        synchronized (xaResourceHolders) {
            for (int i = 0; i < xaResourceHolders.size(); i++) {
                XAResourceHolder xaResourceHolder = xaResourceHolders.get(i);
                if (xaResourceHolder.getXAResource() == xaResource) {
                    xaResourceHolders.remove(i);
                    return true;
                }
            }
            return false;
        }
    }

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "a XAResourceProducer with uniqueName " + getUniqueName();
    }
}
