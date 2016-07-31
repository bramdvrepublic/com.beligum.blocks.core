package com.beligum.blocks.fs.hdfs.bitronix;

import bitronix.tm.resource.common.AbstractXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;

import javax.transaction.xa.XAResource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Holder for XAResource. Used by {@link XAResourceProducer}.
 * <p>
 * Copied from http://blog.trixi.cz/2011/11/jta-transaction-manager-atomikos-or-bitronix/
 *
 * @author gargii
 */
public class XAResourceHolder extends AbstractXAResourceHolder
{
    private final XAResource resource;
    private final ResourceBean bean;

    /**
     * Create a new XaDiskXAResourceHolder for a particular XAResource
     *
     * @param resource the required XAResource
     * @param bean     the required ResourceBean
     */
    public XAResourceHolder(XAResource resource, ResourceBean bean)
    {
        this.resource = resource;
        this.bean = bean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XAResource getXAResource()
    {
        return resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceBean getResourceBean()
    {
        return bean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception
    {
        throw new UnsupportedOperationException("XaDiskXAResourceHolder cannot be used with an XAPool");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getConnectionHandle() throws Exception
    {
        throw new UnsupportedOperationException("XaDiskXAResourceHolder cannot be used with an XAPool");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getLastReleaseDate()
    {
        throw new UnsupportedOperationException("XaDiskXAResourceHolder cannot be used with an XAPool");
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public List getXAResourceHolders()
    {
        List xaResourceHolders = new ArrayList(1);
        xaResourceHolders.add(this);
        return xaResourceHolders;
    }
}
