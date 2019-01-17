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
base.plugin("blocks.core.elements.Row", ["base.core.Class", "constants.base.core.internal", "blocks.core.DOM", "constants.blocks.core", "messages.blocks.core", function (Class, Constants, DOM, BlocksConstants, BlocksMessages)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Row = Class.create(blocks.elements.Surface, {

        //-----STATICS-----
        STATIC: {

            MIN_COLS: 1,
            MAX_COLS: 12,

            createElement: function (tagName)
            {
                return $('<' + (tagName ? tagName : blocks.elements.Surface.DEFAULT_TAG) + ' class="' + BlocksConstants.ROW_CLASS + '" />');
            },
        },

        //-----CONSTANTS-----

        //-----VARIABLES-----
        //an array with references to the resizers of the columns in this row
        resizers: undefined,

        //-----CONSTRUCTORS-----
        constructor: function (parentSurface, element)
        {
            blocks.elements.Row.Super.call(this, parentSurface, element);

            this.resizers = [];
        },

        //-----PUBLIC METHODS-----

        //-----TODO UNCHECKED-----
        // // Override
        // getElementAtSide: function (side)
        // {
        //     if (side == Constants.SIDE.TOP) {
        //         return this.getPrevious();
        //     }
        //     else if (side == Constants.SIDE.BOTTOM) {
        //         return this.getNext();
        //     }
        //     else {
        //         return null;
        //     }
        // },
        //
        // // find all dropspots for an element
        // // is called for a block and returns all dropspots for this block and his parents.
        // calculateDropspots: function (side, dropspots)
        // {
        //     var isOuter = this.isOuter(side);
        //     if ((side == Constants.SIDE.TOP || side == Constants.SIDE.BOTTOM) && this.children.length > 1) {
        //         dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
        //     }
        //
        //     if (this.isOuter(side) && this.parent != null) dropspots = this.parent.calculateDropspots(side, dropspots);
        //     return dropspots;
        // },

        //-----PRIVATE METHODS-----
        _getType: function ()
        {
            return 'row';
        },
        _getName: function ()
        {
            return BlocksMessages.surfaceRowName;
        },
        _newChildInstance: function (element)
        {
            var retVal = new blocks.elements.Column(this, element);

            //if the new child has a previous column, put a resize handle between them
            if (retVal.index > 0) {
                this.resizers.push(new blocks.elements.Resizer(this.children[retVal.index - 1], retVal));
            }

            return retVal;
        },
        _isAcceptableChild: function (element)
        {
            return blocks.elements.Surface.isColumn(element);
        },
        _getChildOrientation: function ()
        {
            return blocks.elements.Surface.ORIENTATION.HORIZONTAL;
        },
        _refresh: function (deep)
        {
            blocks.elements.Row.Super.prototype._refresh.call(this, deep);

            //Next to refreshing this row, we also need to refresh the resizers
            //Note: this will be called while dragging around the resizer,
            //so make sure we don't rebuild the resizers here, just refresh their bounds.
            for (var i = 0; i < this.resizers.length; i++) {
                this.resizers[i]._refresh(deep);
            }
        },
        /**
         * Clears and re-initialize the resizers of all columns in this row
         *
         * @private
         */
        _updateResizers: function ()
        {
            //we should only delete our own resizers
            if (this.resizers) {
                for (var i = 0; i < this.resizers.length; i++) {
                    this.resizers[i].overlay.remove();
                }
            }

            this.resizers = [];
            //note: we start at index 1
            for (var i = 1; i < this.children.length; i++) {
                var newResizer = new blocks.elements.Resizer(this.children[i - 1], this.children[i]);
                newResizer._refresh();
                this.resizers.push(newResizer);
            }
        },
        /**
         * Overloads the parent surface function to re-distribute the columns in this row when one is removed
         *
         * @param surface
         * @private
         */
        _removeChild: function (surface)
        {
            blocks.elements.Row.Super.prototype._removeChild.call(this, surface);

            // re-distribute the extra space over the existing columns
            //note that the isColumn() is needed because when dropping new blocks, they sometimes
            //end up in a row first before they are moved to their final destination.
            if (this.children.length > 0 && surface.isColumn()) {

                var extraWidth = surface.columnWidth;
                var extraWidthPerCol = Math.floor(extraWidth / this.children.length);
                var extraWidthRounding = extraWidth - (extraWidthPerCol * this.children.length);

                for (var i = 0; i < this.children.length; i++) {
                    this.children[i].setColumnWidth(this.children[i].columnWidth + extraWidthPerCol + (i === 0 ? extraWidthRounding : 0));
                }
            }

            this._updateResizers();
        },
        /**
         * Overloads the parent surface function to simplify the row-in-col12-in-row situation.
         *
         * @param deep
         * @private
         */
        _simplify: function (deep)
        {
            //this row has one child and it's a column of maximum width
            if (this.children.length === 1 && this.children[0].isColumn() && this.children[0].columnWidth === blocks.elements.Row.MAX_COLS) {

                var fullWidthCol = this.children[0];

                //and that column has only one child: a row
                if (fullWidthCol.children.length === 1 && fullWidthCol.children[0].isRow()) {

                    var rowToEliminate = fullWidthCol.children[0];
                    for (var i = 0; i < rowToEliminate.children.length; i++) {
                        var child = rowToEliminate.children[i];
                        rowToEliminate._removeChild(child);
                        this._addChild(child);
                        i--;
                    }

                    this._removeChild(fullWidthCol);

                    //this will remove the resizers from the old column (because it has no children now)
                    rowToEliminate._updateResizers();
                    //and introduce them in the new column
                    this._updateResizers();
                }
            }

            //now call the superclass function to iterate the children
            blocks.elements.Row.Super.prototype._simplify.call(this, deep);
        }
    });

}]);