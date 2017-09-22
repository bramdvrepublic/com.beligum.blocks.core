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
