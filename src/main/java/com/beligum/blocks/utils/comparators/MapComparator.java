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

package com.beligum.blocks.utils.comparators;

import java.util.Comparator;
import java.util.Map;

/**
 * Created by wouter on 24/07/15.
 */
public class MapComparator implements Comparator<Map<String, String>>
{
    private final String key;

    public MapComparator(String key)
    {
        this.key = key;
    }

    public int compare(Map<String, String> first, Map<String, String> second)
    {
        String l = "";
        String r = "";
        if (first != null) {
            String val = first.get(key);
            if (val != null) {
                l = val;
            }
        }
        if (second != null) {
            String val = second.get(key);
            if (val != null) {
                r = val;
            }
        }

        return l.compareTo(r);
    }
}
