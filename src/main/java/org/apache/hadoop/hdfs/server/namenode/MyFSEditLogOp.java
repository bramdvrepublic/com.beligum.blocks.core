package org.apache.hadoop.hdfs.server.namenode;

import org.apache.hadoop.hdfs.util.XMLUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by bram on 2/1/16.
 */
public abstract class MyFSEditLogOp extends FSEditLogOp
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    /**
     * Constructor for an EditLog Op. EditLog ops cannot be constructed
     * directly, but only through Reader#readOp.
     *
     * @param opCode
     */
    protected MyFSEditLogOp(FSEditLogOpCodes opCode)
    {
        super(opCode);
    }

    //-----PUBLIC METHODS-----

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

    public static class DeleteFSEditLogOp extends WrappedFSEditLogOp<DeleteOp>
    {
        private DeleteFSEditLogOp()
        {
            super(FSEditLogOpCodes.OP_DELETE);
        }

        public DeleteOp setPath(String path)
        {
            return this.wrappedOp.setPath(path);
        }
        public DeleteOp setTimestamp(long timestamp)
        {
            return this.wrappedOp.setTimestamp(timestamp);
        }
    }

    public static class WrappedFSEditLogOp<T extends FSEditLogOp> extends FSEditLogOp
    {
        private static OpInstanceCache opInstanceCache = new OpInstanceCache();

        protected T wrappedOp;

        private WrappedFSEditLogOp(FSEditLogOpCodes opCode)
        {
            super(opCode);

            this.wrappedOp = (T) opInstanceCache.get(opCode);
        }
        @Override
        void resetSubFields()
        {
            this.wrappedOp.resetSubFields();
        }
        @Override
        void readFields(DataInputStream in, int logVersion) throws IOException
        {
            this.wrappedOp.readFields(in, logVersion);
        }
        @Override
        public void writeFields(DataOutputStream out) throws IOException
        {
            this.wrappedOp.writeFields(out);
        }
        @Override
        protected void toXml(ContentHandler contentHandler) throws SAXException
        {
            this.wrappedOp.toXml(contentHandler);
        }
        @Override
        void fromXml(XMLUtils.Stanza st) throws XMLUtils.InvalidXmlException
        {
            this.wrappedOp.fromXml(st);
        }
    }
}
