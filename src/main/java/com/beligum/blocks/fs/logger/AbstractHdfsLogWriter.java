package com.beligum.blocks.fs.logger;

import com.beligum.base.config.CoreConfiguration;
import com.beligum.base.server.R;
import com.beligum.blocks.fs.ifaces.BlocksResource;
import com.beligum.blocks.fs.logger.ifaces.LogWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.Options;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.EnumSet;

/**
 * Created by bram on 1/20/16.
 */
public abstract class AbstractHdfsLogWriter implements LogWriter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    //valid during an entire session
    protected BlocksResource blocksResource;
    protected Writer logWriter;

    //-----CONSTRUCTORS-----
    protected AbstractHdfsLogWriter(BlocksResource blocksResource) throws IOException
    {
        this.blocksResource = blocksResource;
        this.logWriter = new BufferedWriter(new OutputStreamWriter(this.blocksResource.getFileContext().create(this.blocksResource.getLogFile(), EnumSet.of(CreateFlag.CREATE, CreateFlag.APPEND), Options.CreateOpts.createParent())));
    }

    //-----PUBLIC METHODS-----
    @Override
    public void close() throws IOException
    {
        if (this.logWriter!=null) {
            this.logWriter.close();
            this.logWriter = null;
        }
    }

    //-----PROTECTED METHODS-----
    /**
     * Returns an array of one (artifact) or two (artifact, version) entries
     */
    protected String[] buildSoftwareId() throws IOException
    {
        //this will return the maven package of the current module writing this metadata -> very useful
        CoreConfiguration.ProjectProperties properties = R.configuration().getCurrentProjectProperties();
        String artifactId = properties.getProperty(CoreConfiguration.ProjectProperties.Property.MAVEN_PROJECT_ARTIFACT_ID_KEY);
        String version = properties.getProperty(CoreConfiguration.ProjectProperties.Property.MAVEN_PROJECT_VERSION_KEY);
        if (StringUtils.isEmpty(artifactId)) {
            throw new IOException("Encountered an empty mvn artifactId; that shouldn't happen; "+this.blocksResource);
        }
        else {
            if (!StringUtils.isEmpty(version)) {
                return new String[]{artifactId, version};
            }
            else {
                return new String[]{artifactId};
            }
        }
    }

    //-----PRIVATE METHODS-----

}
