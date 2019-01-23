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

/**
 * Region where templates can be dragged
 * Created by wouter on 5/03/15.
 */
base.plugin("blocks.core.elements.Container", ["base.core.Class", "messages.blocks.core", function (Class, BlocksMessages)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Container = Class.create(blocks.elements.Surface, {

        //-----STATICS-----

        //-----CONSTANTS-----

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function (parentSurface, element)
        {
            blocks.elements.Container.Super.call(this, parentSurface, element);
        },

        //-----PUBLIC METHODS-----

        //-----PRIVATE METHODS-----
        _getType: function()
        {
            return 'container';
        },
        _getName: function()
        {
            return BlocksMessages.surfaceContainerName;
        },
        _newChildInstance: function(element)
        {
            return new blocks.elements.Row(this, element);
        },
        _isAcceptableChild: function(element)
        {
            return blocks.elements.Surface.isRow(element);
        },
        _getChildOrientation: function()
        {
            return blocks.elements.Surface.ORIENTATION.VERTICAL;
        },
        /**
         * Overloaded parent method to disable dropspots on containers
         */
        _createDropspots: function (surface, side, prevSurface)
        {
            return [];
        },
    });

}]);
