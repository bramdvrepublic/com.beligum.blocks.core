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
 * A <blocks-layout> element
 */
base.plugin("blocks.core.elements.Layout", ["base.core.Class", "messages.blocks.core", function (Class, BlocksMessages)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Layout = Class.create(blocks.elements.Surface, {

        //-----STATICS-----

        //-----CONSTANTS-----

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function (parentSurface, element)
        {
            blocks.elements.Layout.Super.call(this, parentSurface, element);
        },

        //-----PUBLIC METHODS-----
        isBoundary: function ()
        {
            return true;
        },

        //-----PRIVATE METHODS-----
        _getType: function ()
        {
            return 'layout';
        },
        _getName: function ()
        {
            return BlocksMessages.surfaceLayoutName;
        },
        _newChildInstance: function (element)
        {
            // the layout wrapper is not part of the container-row-column-block hierarchy.
            // it's meant to mark a place in the page where we can change stuff.
            // So, basically, everything is allowed to be a child of it (except page of course)
            var retVal = null;

            // note that we can't use the super method because we need to attach 'this', not the parent
            if (blocks.elements.Surface.isContainer(element)) {
                retVal = new blocks.elements.Container(this, element);
            }
            else if (blocks.elements.Surface.isRow(element)) {
                retVal = new blocks.elements.Row(this, element);
            }
            else if (blocks.elements.Surface.isColumn(element)) {
                retVal = new blocks.elements.Column(this, element);
            }
            else if (blocks.elements.Surface.isBlock(element)) {
                retVal = new blocks.elements.Block(this, element);
            }

            return retVal;
        },
        _isAcceptableChild: function (element)
        {
            // the layout wrapper is not part of the container-row-column-block hierarchy.
            // it's meant to mark a place in the page where we can change stuff.
            // So, basically, everything the parent allows is allowed
            return this.parent._isAcceptableChild(element);
        },
        _getChildOrientation: function ()
        {
            return blocks.elements.Surface.ORIENTATION.NONE;
        },
        /**
         * Overloaded parent method to disable dropspots on layouts
         */
        _createDropspots: function (surface, side, prevSurface)
        {
            return [];
        },
        _isAllowedDropspot: function (childDropspot)
        {
            var retVal = false;

            // since the layout wrapper is not part of the container-row-column-block hierarchy,
            // we'll use the layout direction of the parent to decide if the child dropspot
            // is allowed inside the layout of the parent of this 'virtual' boundary
            if (this.parent._getChildOrientation() === blocks.elements.Surface.ORIENTATION.HORIZONTAL) {
                retVal = childDropspot.side.id === blocks.elements.Surface.SIDE.LEFT.id || childDropspot.side.id === blocks.elements.Surface.SIDE.RIGHT.id;
            }
            else if (this.parent._getChildOrientation() === blocks.elements.Surface.ORIENTATION.VERTICAL) {
                retVal = childDropspot.side.id === blocks.elements.Surface.SIDE.TOP.id || childDropspot.side.id === blocks.elements.Surface.SIDE.BOTTOM.id;
            }

            return retVal;
        },
    });

}]);
