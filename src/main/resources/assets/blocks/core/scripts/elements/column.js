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
 * A column (inside a row) -> Can contain rows or templates
 *
 * Created by wouter on 9/03/15.
 */
base.plugin("blocks.core.Elements.Column", ["base.core.Class", "constants.base.core.internal", function (Class, Constants)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Column = Class.create(blocks.elements.LayoutElement, {

        //-----STATICS-----

        //-----CONSTANTS-----

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function (parentSurface, element)
        {
            blocks.elements.Column.Super.call(this, parentSurface, element);

            //this will find and create the blocks in this column
            //this._generateVerticalChildren(false);
        },

        //-----PUBLIC METHODS-----
        isOuterLeft: function ()
        {
            return this.element.prev().length == 0
        },
        isOuterRight: function ()
        {
            return this.element.next().length == 0
        },

        // Override
        getElementAtSide: function (side)
        {
            if (side == Constants.SIDE.LEFT) {
                return this.getPrevious();
            }
            else if (side == Constants.SIDE.RIGHT) {
                return this.getNext();
            }
            else {
                return null;
            }
        },

        // find all dropspots for an element
        // is called for a block and returns all dropspots for this block and his parents.
        calculateDropspots: function (side, dropspots)
        {
            if ((side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT)) {
                dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
            }

            if (this.isOuter(side) && this.parent != null) {
                dropspots = this.parent.calculateDropspots(side, dropspots);
            }

            return dropspots;
        },

        //-----PRIVATE METHODS-----
        _newChildInstance: function(element)
        {
            return new blocks.elements.Block(this, element, true);
        },
        _isAcceptableChild: function(element)
        {
            return element[0].tagName.indexOf("-") > 0;
        },
        /**
         * Add a block to this column
         * @param blockSurface
         * @private
         * @override
         */
        _layoutChild: function (blockSurface)
        {
            blocks.elements.Column.Super.prototype._layoutChild.call(this, blockSurface);

            //TODO review this
            //these two classes will remove the borders left and top so we don't
            //have double borders when two blocks are next to each other
            if (this.index > 0) {
                blockSurface.overlay.addClass(blocks.elements.LayoutElement.LEFT_CLASS);
            }
            if (blockSurface.index > 0 || this.parent.index > 0) {
                blockSurface.overlay.addClass(blocks.elements.LayoutElement.TOP_CLASS);
            }

            return blockSurface;
        },
        _getChildOrientation: function()
        {
            return blocks.elements.LayoutElement.ORIENTATION_VERTICAL;
        },
    });
}]);
