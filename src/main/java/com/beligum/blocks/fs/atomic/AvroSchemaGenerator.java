package com.beligum.blocks.fs.atomic;

import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by bram on 1/31/16.
 */
public class AvroSchemaGenerator
{
    //-----CONSTANTS-----

    //-----VARIABLES-----

    //-----CONSTRUCTORS-----

    //-----PUBLIC METHODS-----
    public static void main(String[] args) throws Exception
    {
        Schema avroSchema = ReflectData.get().getSchema(JournalFile.class);
        FileWriter schemaWriter = new FileWriter(new File("/home/bram/Projects/Workspace/idea/com.beligum.blocks.core/src/main/avro/JournalFile.avsc"));
        schemaWriter.write(avroSchema.toString(true));
        schemaWriter.flush();
        schemaWriter.close();
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
