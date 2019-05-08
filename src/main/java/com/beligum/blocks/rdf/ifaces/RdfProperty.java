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

package com.beligum.blocks.rdf.ifaces;

import com.beligum.blocks.config.WidgetType;
import com.beligum.blocks.config.InputTypeAdapter;
import com.beligum.blocks.config.WidgetTypeConfig;
import com.beligum.blocks.rdf.RdfPropertyImpl;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * This is more or less the OO representation of the RDF:Property
 *
 * Note that for our use, it doesn't make sense to implement super properties because there's not much to inherit.
 *
 * Note that, strictly speaking, this should subclass RdfClass, but we moved towards extending
 * a higher interface because too much methods of RdfClass didn't have any meaning here.
 *
 * Created by bram on 2/26/16.
 */
public interface RdfProperty extends RdfOntologyMember
{
    /**
     * The full datatype (can also be XSD) of this property. This is used by the client side code, together with the WidgetType (see below),
     * to instance an appropriate input method and validation for entering a value for this property.
     * Eg. http://www.w3.org/2001/XMLSchema#integer
     */
    RdfClass getDataType();

    /**
     * This widget-type to be used in the admin sidebar (or just inline, eg. in the case of the editor)
     * to enter a value for an instance of this property.
     * Eg. InlineEditor
     *
     * Note: we serialize this (eg. to JS client side) as it's constant, so we can easily check it's value client side
     */
    @XmlJavaTypeAdapter(InputTypeAdapter.class)
    WidgetType getWidgetType();

    /**
     * A map of key/value entries that contain specific settings for the input widget type
     */
    WidgetTypeConfig getWidgetConfig();

    /**
     * This allows us to pass certain options to specific properties of specific classes instead
     * of adding them globally.
     */
    interface Option
    {
        void apply(RdfPropertyImpl property);
    }
    class PublicOption implements Option
    {
        public static final PublicOption TRUE = new PublicOption(true);
        public static final PublicOption FALSE = new PublicOption(false);

        boolean value;

        private PublicOption(boolean value)
        {
            this.value = value;
        }

        @Override
        public void apply(RdfPropertyImpl property)
        {
            property.setPublic(this.value);
        }
    }
    class WeightOption implements Option
    {
        int value;

        private WeightOption(int value)
        {
            this.value = value;
        }
        public static WeightOption create(int value)
        {
            return new WeightOption(value);
        }

        @Override
        public void apply(RdfPropertyImpl property)
        {
            property.setWeight(this.value);
        }
    }
}
