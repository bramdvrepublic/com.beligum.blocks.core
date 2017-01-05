package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.parsers.MinifiedInputStream;
import com.beligum.base.server.R;
import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.blocks.templating.blocks.HtmlTemplate;
import com.beligum.blocks.templating.blocks.TemplateResources;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.TemplateInitException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.beligum.blocks.templating.blocks.directives.TemplateResourcesDirective.Argument.externalScripts;
import static com.beligum.blocks.templating.blocks.directives.TemplateResourcesDirective.Argument.externalStyles;

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
            if (originalWriter != null) {
                originalWriter.write(((StringWriter) writer).toString());
                //don't know if this is needed, but it's nice to finish where we started
                writer = originalWriter;
            }
        }

        return retVal;
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private int writeResources(Iterable<TemplateResources.Resource> resources, StringBuffer buffer, int position) throws IOException
    {
        //this will be incrementally augmented with a all hashes
        int hash = 0;
        //this is what will be inserted
        StringBuilder sb = new StringBuilder();
        //this will make sure we only join consecutive types
        TemplateResourcesDirective.Argument lastType = null;
        //this will hold a segmented list of to-join assets
        List<URI> currentAssetPack = null;
        //this can be passed by reference to accumulate inlined bytes
        InlinedBytesAccumulator accumulator = new InlinedBytesAccumulator();

        for (TemplateResources.Resource res : resources) {
            if (!R.configuration().getResourceConfig().getPackResources() || res.getJoinHint() == HtmlTemplate.ResourceJoinHint.skip) {

                //let's see if we need and can inline this resource even though it's not packed
                boolean inlined = false;
                if (R.configuration().getResourceConfig().getEnableInlineResources()) {
                    switch (res.getType()) {
                        case externalStyles:
                        case externalScripts:
                            inlined = this.inlineResource(res, sb, accumulator);
                            break;
                    }
                }

                //if we didn't inline it, we just copy over the incoming html
                if (!inlined) {
                    sb.append(res.getElement());
                    sb.append("\n");
                }
            }
            //no need to calculate extra stuff if it's disabled anyway
            else {
                //bootstrap the data structures
                if (lastType == null) {
                    lastType = res.getType();
                }
                if (currentAssetPack == null) {
                    currentAssetPack = new ArrayList<>();
                }

                switch (res.getType()) {
                    //Note: for now, we won't be joining inline code, just render it out
                    case inlineStyles:
                    case inlineScripts:

                        //for now, we don't register inline styles/scripts,
                        // so we need to output any saved up assets here to maintain correct order
                        this.registerAssetPack(hash, lastType, currentAssetPack, sb, accumulator);
                        hash = 0;
                        lastType = res.getType();
                        currentAssetPack = new ArrayList<>();

                        sb.append(res.getElement());
                        sb.append("\n");

                        break;
                    case externalStyles:
                    case externalScripts:

                        //we only join consecutive types; break, register and reset if we encounter a non-consecutive type
                        if (!lastType.equals(res.getType())) {
                            this.registerAssetPack(hash, lastType, currentAssetPack, sb, accumulator);

                            hash = 0;
                            lastType = res.getType();
                            currentAssetPack = new ArrayList<>();
                        }

                        //Note that the equalsValue will contain the resource-URL for externalStyles and externalScripts
                        URI srcUri = URI.create(res.getValue());
                        //make the format universal so we can join local and non-local resources easily
                        if (!srcUri.isAbsolute()) {
                            srcUri = R.configuration().getSiteDomain().resolve(srcUri);
                        }
                        currentAssetPack.add(srcUri);
                        hash = 31 * hash + res.getValue().hashCode();

                        break;
                    default:
                        throw new IOException("Encountered unimplemented resource type while creating resource asset packs, this shouldn't happen; " + res.getType());
                }
            }
        }

        //make sure we register the last pending assets after the loop
        if (currentAssetPack != null && !currentAssetPack.isEmpty()) {
            this.registerAssetPack(hash, lastType, currentAssetPack, sb, accumulator);
        }

        buffer.insert(position, sb);
        return sb.length();
    }
    private void registerAssetPack(int hash, TemplateResourcesDirective.Argument lastType, List<URI> currentAssetPack, StringBuilder sb,
                                   InlinedBytesAccumulator accumulator) throws IOException
    {
//        //Note: see the commented code below; it uses a different approach so we can re-build the list of assets from it's cache key
//        //      in a relatively compressed way. Drawback is it generates very long URLs though (300+ chars for a basic admin page), but we might
//        //      consider it for later use if we run into caching problems
//        String cacheKey = StringFunctions.intToBase64(hash, true);
//
//        Resource assetPack = JoinResolver.instance().registerAssetPack(cacheKey, lastType.getMatchingMimeType(), currentAssetPack);
//
//        String mimeType = lastType.getMatchingMimeType().getMimeType().toString();
//        if (lastType == TemplateResourcesDirective.Argument.externalStyles) {
//            if (!this.inlineResource(assetPack, lastType, sb, accumulator)) {
//                sb.append("<link rel=\"stylesheet\" type=\"" + mimeType + "\" href=\"" + assetPack.getUri() + "\">");
//            }
//        }
//        else {
//            if (!this.inlineResource(assetPack, lastType, sb, accumulator)) {
//                String async = R.configuration().getResourceConfig().getEnableAsyncResources() ? "async " : "";
//                sb.append("<script " + async + "type=\"" + mimeType + "\" src=\"" + assetPack.getUri() + "\"></script>");
//            }
//        }
//        sb.append("\n");
//
//        //OLD, but useful code to show how we
//        //Note that we need to use a general inputstream (instead of the ResourceManager) to make dynamic resources (like reset.css) work
//        //                InputStream is = null;
//        //                try {
//        //                    URLConnection conn = srcUri.toURL().openConnection();
//        //
//        //                    //note the ETag needs to be parsed before it can be used
//        //                    String etag = null;
//        //                    String eTagRaw = conn.getHeaderField(HttpHeaders.ETAG);
//        //                    if (!StringUtils.isEmpty(eTagRaw)) {
//        //                        etag = EntityTag.valueOf(eTagRaw).getValue();
//        //                    }
//        //
//        //                    //for now, we'll only join local assets
//        //                    if (etag != null && srcUri.getHost().equals(R.configuration().getSiteDomain().getHost())) {
//        //                        byte[] etagHash = Base64.decodeBase64(etag);
//        //                        Logger.info(etagHash.length);
//        //                        hashBuf.write(etagHash);
//        //
//        //                        hashes/*.append("|")*/.append(etag);
//        //                    }
//        //
//        //                    //note: the name doesn't really matter; mainly used for debug messages
//        //                    inputs.add(SourceFile.fromInputStream(srcUri.toString(), is = conn.getInputStream()));
//        //                }
//        //                finally {
//        //                    IOUtils.closeQuietly(is);
//        //                }
    }
    /**
     * Checks and sees if we can optimize this page by inlining this resource
     * See https://developers.google.com/speed/docs/insights/OptimizeCSSDelivery for details
     * Note: the limits come from the sizes of the inlined styles and scripts of that Google page:
     * - <style> = 49K
     * - <script> = 139K
     */
    private boolean inlineResource(TemplateResources.Resource res, StringBuilder sb, InlinedBytesAccumulator accumulator) throws IOException
    {
        URI uri = URI.create(res.getValue());

        //make the format universal so we can join local and non-local resources easily
        if (!uri.isAbsolute()) {
            uri = R.configuration().getSiteDomain().resolve(uri);
        }

        //Note that the equalsValue will contain the resource-URL for externalStyles and externalScripts
        return this.inlineResource(R.resourceManager().getResource(uri.toString()), res.getType(), sb, accumulator);
    }
    private boolean inlineResource(Resource resource, TemplateResourcesDirective.Argument type, StringBuilder sb, InlinedBytesAccumulator accumulator) throws IOException
    {
        boolean retVal = false;

        long size = resource.getSize();
        if (accumulator.accumulate(size, type)) {
            InputStream inputStream = null;
            try {
                inputStream = resource.newInputStream();
                if (R.configuration().getResourceConfig().getMinifyResources()) {
                    inputStream = new MinifiedInputStream(inputStream, resource.getUri(), resource.getMimeType());
                }

                String content = IOUtils.toString(inputStream);

                //if we're pulling in a remote style, we might as well fingerprint the remote URIs
                //this is especiallly handy when using font-packs!
                if (R.configuration().getResourceConfig().getEnableFingerprintedResources()) {
                    if (type == externalStyles || type == externalScripts) {
                        content = R.resourceManager().getFingerprinter().fingerprintUris(content);
                    }
                }

                sb.append(this.getInlineStartTagFor(type));
                sb.append(content);
                sb.append(this.getInlineEndTagFor(type));
                retVal = true;
            }
            finally {
                IOUtils.closeQuietly(inputStream);
            }
        }

        return retVal;
    }
    private String getInlineStartTagFor(TemplateResourcesDirective.Argument type) throws IOException
    {
        return "<" + this.getTagInnerFor(type) + ">";
    }
    private String getInlineEndTagFor(TemplateResourcesDirective.Argument type) throws IOException
    {
        return "</" + this.getTagInnerFor(type) + ">";
    }
    private String getTagInnerFor(TemplateResourcesDirective.Argument type) throws IOException
    {
        String retVal = null;

        switch (type) {
            case inlineStyles:
            case externalStyles:
            case styles:

                retVal = "style";
                break;

            case inlineScripts:
            case externalScripts:
            case scripts:

                retVal = "script";
                break;

            default:
                throw new IOException("Encountered unimplemented resource type while getting the inner tag for it, this shouldn't happen; " + type);
        }

        return retVal;
    }
    private class InlinedBytesAccumulator
    {
        private long STYLE_THRES = R.configuration().getResourceConfig().getInlineStylesThreshold();
        private long SCRIPT_THRES = R.configuration().getResourceConfig().getInlineScriptsThreshold();
        private long STYLE_TOTAL_THRES = R.configuration().getResourceConfig().getInlineStylesTotalThreshold();
        private long SCRIPT_TOTAL_THRES = R.configuration().getResourceConfig().getInlineScriptsTotalThreshold();
        private long TOTAL_THRES = R.configuration().getResourceConfig().getInlineTotalThreshold();

        private long styleBytes = 0;
        private long scriptBytes = 0;

        public boolean accumulate(long bytes, TemplateResourcesDirective.Argument type) throws IOException
        {
            boolean retVal = false;

            switch (type) {
                case inlineStyles:
                case externalStyles:
                case styles:

                    if (bytes <= STYLE_THRES && this.styleBytes + bytes <= STYLE_TOTAL_THRES && this.styleBytes + this.scriptBytes + bytes <= TOTAL_THRES) {
                        this.styleBytes += bytes;
                        retVal = true;
                    }

                    break;

                case inlineScripts:
                case externalScripts:
                case scripts:

                    if (bytes <= SCRIPT_THRES && this.scriptBytes + bytes <= SCRIPT_TOTAL_THRES && this.styleBytes + this.scriptBytes + bytes <= TOTAL_THRES) {
                        this.scriptBytes += bytes;
                        retVal = true;
                    }

                    break;

                default:
                    throw new IOException("Encountered unimplemented resource type while accumulating a threshold, this shouldn't happen; " + type);
            }

            return retVal;
        }
    }
}
