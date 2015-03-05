package com.beligum.blocks.core.URLMapping;

import java.util.Comparator;

/**
 * Created by bas on 25.02.15.
 * Comparator for comparing path-ids ordered by depth first and per depth from first assigned to last assigned (f.i. 1.1, 1.2, 1.3, 1.4, 2.1, 3.1, 3.2, 3.3)
 */
public class PathIdComparator implements Comparator<String>
{
    @Override
    public int compare(String pathId1, String pathId2)
    {
        String[] splitted1 = pathId1.split("\\.");
        String[] splitted2 = pathId2.split("\\.");
        int depth1 = Integer.parseInt(splitted1[0]);
        int depth2 = Integer.parseInt(splitted2[0]);
        //if depth is not equals, lowest depth is "lowest" string
        if(depth1 < depth2){
            return -1;
        }
        else if(depth1 == depth2){
            int depthId1 = Integer.parseInt(splitted1[1]);
            int depthId2 = Integer.parseInt(splitted2[1]);
            if(depthId1 < depthId2){
                return -1;
            }
            else if(depthId1 == depthId2){
                return 0;
            }
            else{
                return 1;
            }
        }
        else{
            return 1;
        }
    }
}
