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

package com.beligum.blocks.filesystem.tx.bitronix;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.common.*;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interesting links:
 * http://blog.trixi.cz/2011/11/jta-transaction-manager-atomikos-or-bitronix/
 * https://github.com/akubra/akubra/blob/master/akubra-tck/src/main/java/org/akubraproject/tck/BtmUtils.java
 * https://github.com/maxant/genericconnector/blob/master/connector/genericconnector-bitronix-impl/src/main/java/ch/maxant/generic_jca_adapter/MicroserviceResourceProducer.java
 * https://github.com/pkelchner/btm-infinispan/blob/master/src/main/java/bitronix/tm/resource/infinispan/InfinispanXAResourceProducer.java
 *
 * @author gargii
 */
public class SimpleXAResourceProducer extends ResourceBean implements CustomBitronixResourceProducer
{
    //-----CONSTANTS-----
    public static final String UNIQUE_NAME = SimpleXAResourceProducer.class.getCanonicalName();
    private static final long serialVersionUID = -1026425674409225116L;

    //-----VARIABLES-----
    private transient Map<XAResource, bitronix.tm.resource.common.XAResourceHolder> resourceHolders;

    //-----CONSTRUCTORS-----
    public SimpleXAResourceProducer()
    {
        //don't know if we need to call this manually
        this.init();
    }

    //-----STATIC METHODS-----

    //-----SERIALIZATION METHODS-----
    private Object readResolve()
    {
        //don't know if we need to call this manually
        this.init();

        return this;
    }

    //-----PUBLIC METHODS-----
    @Override
    public synchronized void registerResource(XAResource resource)
    {
        this.resourceHolders.put(resource, createResHolder(resource));
    }
    @Override
    public synchronized void unregisterResource(XAResource resource)
    {
        this.resourceHolders.remove(resource);
    }
    @Override
    public XAResourceHolderState startRecovery() throws RecoveryException
    {
        return createResHolder(new RecoveryXAResource()).getXAResourceHolderState();
    }
    @Override
    public void endRecovery() throws RecoveryException
    {
        //?
    }
    @Override
    public void setFailed(boolean failed)
    {
        // cache cannot fail as it's not connection oriented
    }
    @Override
    public bitronix.tm.resource.common.XAResourceHolder findXAResourceHolder(XAResource xaResource)
    {
        return resourceHolders.get(xaResource);
    }
    @Override
    public void init()
    {
        this.setUniqueName(UNIQUE_NAME);
        this.resourceHolders = new ConcurrentHashMap<>();
    }
    @Override
    public void close()
    {
        if (this.resourceHolders != null) {
            this.resourceHolders.clear();
            this.resourceHolders = null;
        }
    }
    @Override
    public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception
    {
        throw new UnsupportedOperationException("Ehcache is not connection-oriented");
    }
    @Override
    public Reference getReference() throws NamingException
    {
        return new Reference(this.getClass().getName(),
                             new StringRefAddr("uniqueName", getUniqueName()),
                             ResourceObjectFactory.class.getName(), null);
    }

    //-----PRIVATE METHODS-----
    private static SimpleXAResourceHolder createResHolder(XAResource xaResource)
    {
        ResourceBean rb = new ResourceBean()
        {
        };
        rb.setUniqueName(xaResource.getClass().getName() + System.identityHashCode(xaResource));
        rb.setApplyTransactionTimeout(true);

        SimpleXAResourceHolder resHolder = new SimpleXAResourceHolder(xaResource, rb);
        resHolder.setXAResourceHolderState(new XAResourceHolderState(resHolder, rb));

        return resHolder;
    }

    //-----MANAGEMENT METHODS-----
    @Override
    public String toString()
    {
        return "a XAResourceProducer with uniqueName " + getUniqueName();
    }

    //-----INNER CLASSES-----
    private static class SimpleXAResourceHolder extends AbstractXAResourceHolder
    {
        private final XAResource xares;
        private final ResourceBean creator;
        private XAResourceHolderState state;

        SimpleXAResourceHolder(XAResource xares, ResourceBean creator)
        {
            this.xares = xares;
            this.creator = creator;
        }

        public void close()
        {
        }

        public Object getConnectionHandle()
        {
            return null;
        }

        public Date getLastReleaseDate()
        {
            return null;
        }

        @SuppressWarnings("unchecked")
        public List getXAResourceHolders()
        {
            return null;
        }

        public XAResource getXAResource()
        {
            return xares;
        }

        public ResourceBean getResourceBean()
        {
            return creator;
        }

        public XAResourceHolderState getXAResourceHolderState()
        {
            return state;
        }

        public void setXAResourceHolderState(XAResourceHolderState state)
        {
            this.state = state;
        }
    }

    private static class RecoveryXAResource implements XAResource
    {
        public void start(Xid xid, int flags)
        {
        }

        public void end(Xid xid, int flags)
        {
        }

        public int prepare(Xid xid)
        {
            return XA_OK;
        }

        public void commit(Xid xid, boolean onePhase)
        {
        }

        public void rollback(Xid xid)
        {
        }

        public Xid[] recover(int flag)
        {
            // recovery not supported (yet)
            return new Xid[0];
        }

        public void forget(Xid xid)
        {
        }

        public int getTransactionTimeout()
        {
            return 10;
        }

        public boolean setTransactionTimeout(int transactionTimeout)
        {
            return false;
        }

        public boolean isSameRM(XAResource xaResource)
        {
            return xaResource == this;
        }
    }
}
