package com.beligum.blocks.rdf.exporters;

import com.beligum.base.utils.json.Json;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bram on 1/23/16.
 */
public class JenaExporter extends AbstractExporter
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----
    public JenaExporter(Format exportFormat)
    {
        super(exportFormat);
    }

    //-----PUBLIC METHODS-----
    @Override
    public void exportModel(Model model, OutputStream outputStream) throws IOException
    {
        if (this.exportFormat==Format.JSONLD) {
            //in the case of JSON, we compact it
            try (OutputStream tempOs = new ByteArrayOutputStream()) {
                //read to a string
                RDFDataMgr.write(tempOs, model, this.translateFormat(this.exportFormat));

                //See http://mirror.vsibiri.info/libraries.io/maven/com.github.jsonld-java-jsonld-java-jena.htm
                Object jsonObject = JsonUtils.fromString(tempOs.toString());

                // Create a context JSON map containing prefixes and definitions
                Map context = new HashMap();
                //TODO works but to change...
                //context.put("mot", "http://www.mot.be");

                // Create an instance of JsonLdOptions with the standard JSON-LD options
                JsonLdOptions options = new JsonLdOptions();

                Map<String, Object> compact = JsonLdProcessor.compact(jsonObject, context, options);
                //works
                //Object compactFlat = JsonLdProcessor.flatten(compact, options);

                Json.getObjectMapper().writeValue(outputStream, compact);
            }
            catch (JsonLdError e) {
                throw new IOException("Error while compacting JSON-LD code", e);
            }
        }
        //just use the default Jena conversion
        else {
            RDFDataMgr.write(outputStream, model, this.translateFormat(this.exportFormat));
        }
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----
    private Lang translateFormat(Format exportFormat) throws IOException
    {
        switch (exportFormat) {
            case JSONLD:
                return Lang.JSONLD;
            default:
                throw new IOException("Unsupported exporter format detected; "+exportFormat);
        }
    }
}
