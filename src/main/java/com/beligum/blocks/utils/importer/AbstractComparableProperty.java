package com.beligum.blocks.utils.importer;

import com.beligum.blocks.utils.importer.interfaces.ComparableProperty;

import java.util.Comparator;

public abstract class AbstractComparableProperty implements ComparableProperty
{
    protected Integer index;
    public Integer getIndex()
    {
        return index;
    }

    public static class MapComparator implements Comparator<ComparableProperty>
    {
        @Override
        public int compare(ComparableProperty o1, ComparableProperty o2)
        {
            if(o1 == null || o2 == null){
                return -1;
            }
            else if (o1.getIndex() >= o2.getIndex()) {
                return 1;
            }
            else {
                return -1;
            }
        }
    }
}
