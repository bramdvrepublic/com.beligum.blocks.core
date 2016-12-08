package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.resources.resolvers.JoinResolver;
import com.beligum.base.server.R;
import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.base.utils.toolkit.StringFunctions;
import com.beligum.blocks.templating.blocks.TemplateResources;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.javascript.jscomp.SourceFile;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by bram on 5/28/15.
 */
@VelocityDirective(PageTemplateWrapperDirective.NAME)
public class PageTemplateWrapperDirective extends Directive
{
    //-----CONSTANTS-----
    public static final String NAME = "ptwd";

    //-----VARIABLES-----
    //ugly static cache, but should remain fairly small and is only used when inlining is enabled
    private static Map<URI, Long> assetPackSizeCache = new HashMap<>();

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

        StringBuilder hashes = new StringBuilder();
        ByteArrayDataOutput hashBuf = ByteStreams.newDataOutput();
        List<SourceFile> inputs = new ArrayList<>();
        for (TemplateResources.Resource res : resources) {
            if (!R.configuration().getResourceConfig().getPackResources()) {
                sb.append(res.getValue());
                sb.append("\n");
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
                        this.registerAssetPack(hash, lastType, currentAssetPack, sb);
                        hash = 0;
                        lastType = res.getType();
                        currentAssetPack = new ArrayList<>();

                        sb.append(res.getValue());
                        sb.append("\n");

                        break;
                    case externalStyles:
                    case externalScripts:

                        //we only join consecutive types; break, register and reset if we encounter a non-consecutive type
                        if (!lastType.equals(res.getType())) {
                            this.registerAssetPack(hash, lastType, currentAssetPack, sb);

                            hash = 0;
                            lastType = res.getType();
                            currentAssetPack = new ArrayList<>();
                        }

                        //Note that the equalsValue will contain the resource-URL for externalStyles and externalScripts
                        URI srcUri = URI.create(res.getEqualsValue());
                        //make the format universal so we can join local and non-local resources easily
                        if (!srcUri.isAbsolute()) {
                            srcUri = R.configuration().getSiteDomain().resolve(res.getEqualsValue());
                        }
                        currentAssetPack.add(srcUri);
                        hash = 31 * hash + res.getEqualsValue().hashCode();

                        break;
                    default:
                        throw new IOException("Encountered unimplemented resource type while creating resource asset packs, this shouldn't happen; " + res.getType());
                }
            }
        }

        //make sure we register the last pending assets after the loop
        if (currentAssetPack != null && !currentAssetPack.isEmpty()) {
            this.registerAssetPack(hash, lastType, currentAssetPack, sb);
        }

        buffer.insert(position, sb);
        return sb.length();
    }
    private void registerAssetPack(int hash, TemplateResourcesDirective.Argument lastType, List<URI> currentAssetPack, StringBuilder sb) throws IOException
    {
        //Note: see the commented code below; it uses a different approach so we can re-build the list of assets from it's cache key
        //      in a relatively compressed way. Drawback is it generates very long URLs though (300+ chars for a basic admin page), but we might
        //      consider it for later use if we run into caching problems
        String cacheKey = StringFunctions.intToBase64(hash, true);

        URI joinUri = JoinResolver.instance().registerAssetPack(cacheKey, lastType.getMatchingMimeType(), currentAssetPack);
        String mimeType = lastType.getMatchingMimeType().getMimeType().toString();
        if (lastType == TemplateResourcesDirective.Argument.externalStyles) {
            //let's see if we can optimize this page by inlining this styles
            //See https://developers.google.com/speed/docs/insights/OptimizeCSSDelivery for details
            //Note: the limits come from the sizes of the inlined styles and scripts of that Google page:
            //      - <style> = 49K
            //      - <script> = 139K
            boolean inlined = false;
            if (R.configuration().getResourceConfig().getEnableInlineResources()) {
                long size = this.fetchRemoteSize(joinUri);
                if (size <= R.configuration().getResourceConfig().getInlineStylesThreshold()) {
                    sb.append("<style>");
                    sb.append(this.fetchRemoteContent(joinUri));
                    sb.append("</style>");
                    inlined = true;
                }
            }

            if (!inlined) {
                sb.append("<link rel=\"stylesheet\" type=\"" + mimeType + "\" href=\"" + joinUri.toString() + "\">");
            }
        }
        else {
            boolean inlined = false;
            if (R.configuration().getResourceConfig().getEnableInlineResources()) {
                long size = this.fetchRemoteSize(joinUri);
                if (size <= R.configuration().getResourceConfig().getInlineScriptsThreshold()) {
                    sb.append("<script>");
                    sb.append(this.fetchRemoteContent(joinUri));
                    sb.append("</script>");
                    inlined = true;
                }
            }

            if (!inlined) {
                String async = R.configuration().getResourceConfig().getEnableAsyncResources() ? "async " : "";
                sb.append("<script " + async + "type=\"" + mimeType + "\" src=\"" + joinUri.toString() + "\"></script>");
            }
        }
        sb.append("\n");

        //OLD, but useful code to show how we
        //Note that we need to use a general inputstream (instead of the ResourceManager) to make dynamic resources (like reset.css) work
        //                InputStream is = null;
        //                try {
        //                    URLConnection conn = srcUri.toURL().openConnection();
        //
        //                    //note the ETag needs to be parsed before it can be used
        //                    String etag = null;
        //                    String eTagRaw = conn.getHeaderField(HttpHeaders.ETAG);
        //                    if (!StringUtils.isEmpty(eTagRaw)) {
        //                        etag = EntityTag.valueOf(eTagRaw).getValue();
        //                    }
        //
        //                    //for now, we'll only join local assets
        //                    if (etag != null && srcUri.getHost().equals(R.configuration().getSiteDomain().getHost())) {
        //                        byte[] etagHash = Base64.decodeBase64(etag);
        //                        Logger.info(etagHash.length);
        //                        hashBuf.write(etagHash);
        //
        //                        hashes/*.append("|")*/.append(etag);
        //                    }
        //
        //                    //note: the name doesn't really matter; mainly used for debug messages
        //                    inputs.add(SourceFile.fromInputStream(srcUri.toString(), is = conn.getInputStream()));
        //                }
        //                finally {
        //                    IOUtils.closeQuietly(is);
        //                }
    }
    private static final String STREAM_TAIL = "\n";
    private static final int STREAM_TAIL_LEN = STREAM_TAIL.getBytes().length;
    /**
     * Note that we use caching here (because it's always checked when inlining is enabled),
     * but stream the content from the URL when there's a match, but it should be fairly fast
     * because it comes from this same server.
     */
    private long fetchRemoteSize(URI assetPack) throws IOException
    {
        Long retVal = null;

        if (assetPackSizeCache.containsKey(assetPack)) {
            retVal = assetPackSizeCache.get(assetPack);
        }

        if (retVal == null) {
            //contact the remote side to fetch the value
            retVal = Long.valueOf(assetPack.toURL().openConnection().getContentLength());

            //we only cache it in production so it can reload all possible changesh in dev mode
            if (R.configuration().getProduction()) {
                assetPackSizeCache.put(assetPack, retVal);
            }
        }

        return retVal;
    }
    private String fetchRemoteContent(URI assetPack) throws IOException
    {
        try (InputStream is = assetPack.toURL().openStream()) {
            return IOUtils.toString(is) + STREAM_TAIL;
        }
    }
}
