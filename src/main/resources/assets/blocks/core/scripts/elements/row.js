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
 * A row inside a column or a container
 * Can only contain columns
 */
base.plugin("blocks.core.Elements.Row", ["base.core.Class", "constants.base.core.internal", function (Class, Constants)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Row = Class.create(blocks.elements.LayoutElement, {

        //-----STATICS-----

        //-----CONSTANTS-----

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function (parentSurface, element)
        {
            blocks.elements.Row.Super.call(this, parentSurface, element);

            // we need this to show the resizeHandles
            this.canDrag = true;
        },

        //-----PUBLIC METHODS-----
        isOuterTop: function ()
        {
            return this.element.prev().length == 0
        },
        isOuterBottom: function ()
        {
            return this.element.next().length == 0
        },

        // Override
        getElementAtSide: function (side)
        {
            if (side == Constants.SIDE.TOP) {
                return this.getPrevious();
            }
            else if (side == Constants.SIDE.BOTTOM) {
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
            var isOuter = this.isOuter(side);
            if ((side == Constants.SIDE.TOP || side == Constants.SIDE.BOTTOM) && this.children.length > 1) {
                dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
            }

            if (this.isOuter(side) && this.parent != null) dropspots = this.parent.calculateDropspots(side, dropspots);
            return dropspots;
        },

        findElements: function (minSearchLevel, maxSearchLevel)
        {
            //TODO this looks a lot like layout.findElements()...
            minSearchLevel = minSearchLevel == null ? 0 : minSearchLevel;
            maxSearchLevel = maxSearchLevel == null ? -1 : maxSearchLevel;
            var retVal = [];
            if (minSearchLevel <= 0) {
                retVal.push(this);
            }
            if (maxSearchLevel != 0) {
                for (var i = 0; i < this.children.length; i++) {
                    var props = this.children[i].findElements(minSearchLevel, maxSearchLevel);
                    for (var j = 0; j < props.length; j++) {
                        retVal.push(props[j]);
                    }
                }
            }

            return retVal;
        },

        showOverlay: function ()
        {
            for (var j = 0; j < this.resizeHandles.length; j++) {
                this.resizeHandles[j].showOverlay();
            }
        },

        removeOverlay: function ()
        {
            for (var j = 0; j < this.resizeHandles.length; j++) {
                this.resizeHandles[j].removeOverlay();
            }
        },

        //-----PRIVATE METHODS-----
        _newChildInstance: function(element)
        {
            return new blocks.elements.Column(this, element);
        },
        _isAcceptableChild: function(element)
        {
            return element.is('[class*="col-"]');
        },
        _getChildOrientation: function()
        {
            return blocks.elements.LayoutElement.ORIENTATION_HORIZONTAL;
        },
        _layoutChild: function (childSurface)
        {
            blocks.elements.Row.Super.prototype._layoutChild.call(this, childSurface);

            if (childSurface.index > 0) {
                this.resizeHandles.push(new blocks.elements.ResizeHandle(this.children[childSurface.index - 1], childSurface));
            }

            return childSurface;
        },
    });

}]);