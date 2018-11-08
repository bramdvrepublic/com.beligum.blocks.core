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
 * A container contains properties
 * A property can contain a new container itself to go up the tree
 *
 * Created by wouter on 5/03/15.
 */
base.plugin("blocks.core.elements.Property", ["base.core.Class", "messages.blocks.core", function (Class, BlocksMessages)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Property = Class.create(blocks.elements.Surface, {

        //-----STATICS-----

        //-----CONSTANTS-----

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function (parentSurface, element)
        {
            blocks.elements.Property.Super.call(this, parentSurface, element);
        },

        //-----PUBLIC METHODS-----

        //-----PRIVATE METHODS-----
        _getType: function()
        {
            return 'property';
        },
        _getName: function()
        {
            return BlocksMessages.surfacePropertyName;
        },
        _canHaveChildren: function ()
        {
            return false;
        },
        _isAcceptableChild: function (element)
        {
            return false;
        },
        _isOuterTop: function ()
        {
            return this.element.prev().length == 0;
        },
        _isOuterBottom: function ()
        {
            return this.element.next().length == 0;
        },
    });

}]);
