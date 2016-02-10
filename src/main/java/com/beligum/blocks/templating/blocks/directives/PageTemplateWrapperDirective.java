package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.blocks.templating.blocks.TemplateResources;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

/**
 * Created by bram on 5/28/15.
 */
@VelocityDirective(PageTemplateWrapperDirective.NAME)
public class PageTemplateWrapperDirective extends Directive
{
    //-----CONSTANTS-----
    public static final String NAME = "ptwd";

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    @Override
    public int getType()
    {
        return BLOCK;
    }
    @Override
    public String getName()
    {
        return NAME;
    }
    /**
     * Initialize and check arguments.
     *
     * @param rs
     * @param context
     * @param node
     * @throws TemplateInitException
     */
    public void init(RuntimeServices rs, InternalContextAdapter context,
                     Node node)
                    throws TemplateInitException
    {
        super.init(rs, context, node);
    }

    /**
     * Evaluate the argument, convert to a String, and evaluate again
     * (with the same context).
     *
     * @param context
     * @param writer
     * @param node
     * @return True if the directive rendered successfully.
     * @throws IOException
     * @throws ResourceNotFoundException
     * @throws ParseErrorException
     * @throws MethodInvocationException
     */
    public boolean render(InternalContextAdapter context, Writer writer,
                          Node node) throws IOException, ResourceNotFoundException,
                                            ParseErrorException, MethodInvocationException
    {
        boolean retVal = false;

        Writer originalWriter = null;
        try {
            // if the supplied writer is not a stringwriter,
            // we'll write to a temp stringwriter because we need to be able to
            // hack into the buffer when the normal Velocity parsing is done
            if (!(writer instanceof StringWriter)) {
                originalWriter = writer;
                writer = new StringWriter();
            }

            //this renders out the entire page directive
            retVal = node.jjtGetChild(0).render(context, writer);

            List<WriterBufferReference> inserts = (List<WriterBufferReference>) context.get(TemplateResourcesDirective.RESOURCES_INSERTS);
            if (inserts != null) {
                StringBuffer buffer = ((StringWriter) writer).getBuffer();
                TemplateResources resources = TemplateResourcesDirective.getContextResources(context);

                //note: we need to move along with the previously inserted char count
                int insertedChars = 0;
                for (WriterBufferReference ref : inserts) {

                    switch (ref.getType()) {
                        case inlineStyles:
                            insertedChars += this.writeResources(resources.getInlineStyles(), buffer, ref.getWriterBufferPosition() + insertedChars);
                            break;
                        case externalStyles:
                            insertedChars += this.writeResources(resources.getExternalStyles(), buffer, ref.getWriterBufferPosition() + insertedChars);
                            break;
                        case styles:
                            insertedChars += this.writeResources(resources.getStyles(), buffer, ref.getWriterBufferPosition() + insertedChars);
                            break;
                        case inlineScripts:
                            insertedChars += this.writeResources(resources.getInlineScripts(), buffer, ref.getWriterBufferPosition() + insertedChars);
                            break;
                        case externalScripts:
                            insertedChars += this.writeResources(resources.getExternalScripts(), buffer, ref.getWriterBufferPosition() + insertedChars);
                            break;
                        case scripts:
                            insertedChars += this.writeResources(resources.getScripts(), buffer, ref.getWriterBufferPosition() + insertedChars);
                            break;
                        default:
                            // default is to write everything out
                            insertedChars += this.writeResources(resources.getStyles(), buffer, ref.getWriterBufferPosition() + insertedChars);
                            insertedChars += this.writeResources(resources.getScripts(), buffer, ref.getWriterBufferPosition() + insertedChars);
                            break;
                    }
                }
            }
        }
        finally {
            if (originalWriter!=null) {
                originalWriter.write(((StringWriter)writer).toString());
                //don't know if this is needed, but it's nice to finish where we started
                writer = originalWriter;
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private int writeResources(Iterable<TemplateResources.Resource> resources, StringBuffer buffer, int position)
    {
        //this is what will be inserted
        StringBuilder sb = new StringBuilder();
        for (TemplateResources.Resource res : resources) {
            sb.append(res.getValue());
            sb.append("\n");
        }

        buffer.insert(position, sb);

        return sb.length();
    }
}
