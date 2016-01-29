package com.beligum.blocks.fs.atomic.v2;

import java.io.File;
import java.io.IOException;

public class DumpJournals
{
    public static void main(String[] args) throws Exception
    {
        dump(args[0]);
    }

    public static void dump(String dir)
                    throws IOException, ClassNotFoundException
    {
        File journalDir = new File(dir);
        File[] files = journalDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().startsWith("journal-")) {
                System.out.println(files[i]);
                Journal.display(files[i]);
            }
        }
    }
}


		
		
		
