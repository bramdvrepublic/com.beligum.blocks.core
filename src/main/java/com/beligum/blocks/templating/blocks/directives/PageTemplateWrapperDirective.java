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

package com.beligum.blocks.templating.blocks.directives;

import com.beligum.base.resources.ResourceInputStream;
import com.beligum.base.resources.ifaces.MimeType;
import com.beligum.base.resources.ifaces.Resource;
import com.beligum.base.resources.repositories.JoinRepository;
import com.beligum.base.server.R;
import com.beligum.base.templating.velocity.directives.VelocityDirective;
import com.beligum.base.utils.Logger;
import com.beligum.base.utils.toolkit.StringFunctions;
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
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
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
     * @return True if the directive rendered successfully.
     */
    public boolean render(InternalContextAdapter context, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException
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

            //iterate over all saved resource locations and see what we need to do with them (inline, print, pack, etc.)
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
        //this will be incrementally augmented with all hashes
        int hash = R.fingerprintHash();
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
                            TemplateResources.ExternalResource extRes = (TemplateResources.ExternalResource) res;
                            inlined = this.inlineExternalResource(extRes.getResource(), extRes.getType(), sb, accumulator);
                            break;
                    }
                }

                //if we didn't inline it, we just copy over the incoming html
                if (!inlined) {
                    sb.append(res.getElement());
                    sb.append("\n");
                }
            }
            //this means we need to pack the resources together according to type
            //and write out the joined URIs instead of the individual URIs
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

                        //for now, we don't register inline styles/scripts, but before we start a new pack,
                        // we need to output any saved up assets here to maintain correct order
                        // Note that the asset pack can be empty, in which case we would register an empty URL
                        //that will throw an error later on
                        if (!currentAssetPack.isEmpty()) {
                            this.registerAssetPack(hash, lastType, currentAssetPack, sb, accumulator);
                        }
                        //reset the hash
                        hash = R.fingerprintHash();
                        lastType = res.getType();
                        currentAssetPack = new ArrayList<>();

                        sb.append(res.getElement());
                        sb.append("\n");

                        break;
                    case externalStyles:
                    case externalScripts:

                        //we only join consecutive types; break, register and reset if we encounter a non-consecutive type
                        if (!lastType.equals(res.getType())) {
                            if (!currentAssetPack.isEmpty()) {
                                this.registerAssetPack(hash, lastType, currentAssetPack, sb, accumulator);
                            }

                            //reset the hash
                            hash = R.fingerprintHash();
                            lastType = res.getType();
                            currentAssetPack = new ArrayList<>();
                        }

                        TemplateResources.ExternalResource externalResource = (TemplateResources.ExternalResource) res;

                        //Note: the internals of getUri() decide on it's fingerprinting, no need to check that here anymore
                        URI resourceUri = externalResource.getUri();

                        //if this is not null if we're dealing with a local resource URI;
                        //check if it has a language set if it's not language agnostic.
                        //This is important for eg. message resources that automatically/dynamically
                        //select the right message bundle according to the request uri, but all have
                        //the same uri. If we would build an asset pack for a certain language and don't
                        //change the url here, it would be returned for other languages too because
                        // the hash (and thus fingerprint) of the pack will be the same.
                        Resource resource = externalResource.getResource();
                        if (resource != null && !resource.isLanguageAgnostic()) {
                            resourceUri = R.i18n().setUrlLocale(resourceUri);
                        }

                        currentAssetPack.add(resourceUri);

                        //Note: the asset pack does nothing more than group together a bunch of URIs,
                        //but if the URI of the asset pack would get cached eternally, this can potentially
                        //lead to stale client-side files. As a safeguard, we'll always start out with the fingerprint
                        //of the server, so we're sure the uri will change after a server restart
                        hash = 31 * hash + resourceUri.hashCode();

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
        MimeType mimeType = lastType.getMatchingMimeType();

        //Note: see the commented code below; it uses a different approach so we can re-build the list of assets from it's cache key
        //      in a relatively compressed way. Drawback is it generates very long URLs though (300+ chars for a basic admin page), but we might
        //      consider it for later use if we run into caching problems
        //Note about the caching: Asset packs should be allowed to be cached on the client side, either for a long time or not so long.
        //                        We enable the caching of asset packs in production mode because it's handy not to cache it in dev mode.
        //                        Since the name of the asset pack is built from the members, we also allow eternal caching if those member-uris are fingerprinted.
        Resource assetPack = JoinRepository.registerAssetPack(StringFunctions.intToBase64(hash, true),
                                                              currentAssetPack,
                                                              mimeType,
                                                              R.configuration().getProduction(),
                                                              R.configuration().getResourceConfig().getEnableFingerprintedResources()
        );

        boolean inlined = false;
        if (R.configuration().getResourceConfig().getEnableInlineResources()) {
            inlined = this.inlineExternalResource(assetPack, lastType, sb, accumulator);
        }

        if (!inlined) {
            if (lastType.equals(TemplateResourcesDirective.Argument.externalStyles) || lastType.equals(TemplateResourcesDirective.Argument.inlineStyles)) {
                sb.append("<link rel=\"stylesheet\" type=\"" + mimeType + "\" href=\"" + assetPack.getUri() + "\">");
            }
            else {
                String async = R.configuration().getResourceConfig().getEnableAsyncResources() ? "async " : "";
                sb.append("<script " + async + "type=\"" + mimeType + "\" src=\"" + assetPack.getUri() + "\"></script>");
            }
        }

        sb.append("\n");

        //OLD, but useful code to show how we can possibly generate a join URL that can be un-joined
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
    /**
     * Checks and sees if we can optimize this page by inlining this resource
     * See https://developers.google.com/speed/docs/insights/OptimizeCSSDelivery for details
     * Note: the limits come from the sizes of the inlined styles and scripts of that Google page:
     * - <style> = 49K
     * - <script> = 139K
     */
    private boolean inlineExternalResource(Resource resource, TemplateResourcesDirective.Argument type, StringBuilder sb, InlinedBytesAccumulator accumulator) throws IOException
    {
        boolean retVal = false;

        //Note: sometimes this will be null in case fo dynamically generated resources like reset.css and the like
        if (resource != null) {
            //if we know the size, let's see if the accumulater can take it, otherwise,
            //we'll need to open the stream to get the real size and check again
            long size = resource.getSize();

            //if size is unknown, proceed, open the stream and check again
            if (size < 0 || accumulator.accumulate(size, type)) {

                boolean proceed = size >= 0;

                //This is probably not so performant, but I didn't find any other good way to get the actual stream size,
                //because for all parsed resources, Resource.getSize() just returns -1, so we don't know what we'll end up with
                //without opening up the stream and checking the actual size.
                try (ResourceInputStream inputStream = resource.newInputStream()) {

                    if (!proceed) {
                        //Note: we'll assume we won't be inlining resources that don't have an explicit size because
                        // we don't know what we get into (we might end up inlining thousands of bytes).
                        size = inputStream.getSize();
                        proceed = size >= 0 && accumulator.accumulate(size, type);
                    }

                    if (proceed) {
                        sb.append(this.getInlineStartTagFor(type));
                        sb.append(IOUtils.toString(inputStream));
                        sb.append(this.getInlineEndTagFor(type));
                        retVal = true;
                    }
                }
            }
        }

        return retVal;
    }
    private CharSequence getInlineStartTagFor(TemplateResourcesDirective.Argument type) throws IOException
    {
        StringBuilder retVal = new StringBuilder("<" + this.getTagInnerFor(type) + ">");

        //this will make sure the RDFa parses correctly
        if (type.isScript()) {
            retVal.append("//<![CDATA[\n");
        }

        return retVal;
    }
    private CharSequence getInlineEndTagFor(TemplateResourcesDirective.Argument type) throws IOException
    {
        StringBuilder retVal = new StringBuilder( );

        //this will make sure the RDFa parses correctly
        if (type.isScript()) {
            retVal.append("\n//]]>");
        }

        retVal.append("</" + this.getTagInnerFor(type) + ">");

        return retVal;
    }
    private String getTagInnerFor(TemplateResourcesDirective.Argument type) throws IOException
    {
        String retVal = null;

        if (type.isStyle()) {
            retVal = "style";
        }
        else if (type.isScript()) {
            retVal = "script";
        }
        else {
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
