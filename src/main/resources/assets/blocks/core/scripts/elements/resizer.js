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

/*
 * defines a resizer. The surface of the resizer is the are that triggers when you hover over it
 * the draw-surface is the surface that will be drawn in the dom (can be bigger or smaller).
 * left and rightcolumn are the columns that this handle will resize when dragged
 */
base.plugin("blocks.core.elements.Resizer", ["base.core.Class", "constants.blocks.core", "blocks.core.Resizer", "blocks.core.UI", "messages.blocks.core", function (Class, BlocksConstants, Resizer, UI, BlocksMessages)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    var tempWidth = 6;
    blocks.elements.Resizer = Class.create(blocks.elements.Surface, {

        //-----STATICS-----
        STATIC: {
            WIDTH: tempWidth,
            HALF_WIDTH: Math.floor(tempWidth / 2.0),
        },

        //-----CONSTANTS-----

        //-----VARIABLES-----
        //the horizontal center of this resizer, for easy recalculations
        center: undefined,
        //a pointer to the left column-surface
        leftColumn: undefined,
        //a pointer to the right column-surface
        rightColumn: undefined,

        //-----CONSTRUCTORS-----
        constructor: function (leftColumn, rightColumn)
        {
            blocks.elements.Resizer.Super.call(this);

            this.leftColumn = leftColumn;
            this.rightColumn = rightColumn;

            this.overlay = this._createOverlay(UI.handleWrapper);
            this.overlay.addClass(BlocksConstants.COLUMN_RESIZER_CLASS);

            // var _this = this;
            // this.overlay.on("mousedown.resizer", function (event)
            // {
            //     // only start drag on left click
            //     if (event.which == 1) {
            //         Resizer.startDrag(_this);
            //         $(document).on("mouseup.resizer", function (event)
            //         {
            //             $(document).off("mouseup.resizer");
            //             Resizer.endDrag(null);
            //         });
            //     }
            // });
        },

        //-----PUBLIC METHODS-----
        previewMoveTo: function (surface, vector)
        {
            //the number of horizontal pixels we've dragged from the center of the resizer
            var offsetPx = vector.x1 - this.center;
            //right is positive, left is negative
            var side = Math.sign(offsetPx);
            //now we've stored the sign, cut it off
            offsetPx = Math.abs(offsetPx);

            var row = this.leftColumn.parent;
            //the width of one column in pixels in the parent row
            var oneColPx = row.realWidth() / 12;
            //the absolute number of columns we're dragging left or right
            //Note that this 'floor' will prevent flickering between two states
            //because it forces the amount of dragged pixels to exceed the width
            //of one column, and when the resizer 'jumps', the next call of this method
            //will result in zero columns because of this floor()
            var colOffset = Math.floor(offsetPx / oneColPx);

            if (colOffset > 0) {
                var colOffsetSigned = side * colOffset;
                var newLeftCols = this.leftColumn.columnWidth + colOffsetSigned;
                var newRightCols = this.rightColumn.columnWidth - colOffsetSigned;

                //make sure we don't create zero-width columns
                if (Math.min(newLeftCols, newRightCols) > 0) {

                    //note: these don't automatically refresh; see remark below
                    this.leftColumn.setColumnWidth(newLeftCols);
                    this.rightColumn.setColumnWidth(newRightCols);

                    //Note that we need to do a deep refresh to also refresh the blocks in this column
                    //Also note that this needs to happen _after_ both updates are done, because the one pushes
                    //forward and the other needs to make room for the first to change width
                    this.leftColumn._refresh(true);
                    this.rightColumn._refresh(true);

                    this._refresh();
                }
            }
        },

        //-----TODO UNCHECKED-----
        // update: function ()
        // {
        //     var left = Math.floor((this._calculateLeft(this.rightColumn.element) + this._calculateRight(this.leftColumn.element)) / 2) - Math.floor(blocks.elements.Resizer.WIDTH / 2);
        //     this.overlay.css("left", left);
        //     var siblings = this.leftColumn.parent.resizers;
        //     var height = this._calculateBottom(this.leftColumn.parent.element) - this._calculateTop(this.leftColumn.parent.element);
        //     for (var i = 0; i < siblings.length; i++) {
        //         siblings[i].overlay.css("height", height);
        //     }
        // },
        // updateHeight: function ()
        // {
        //     var height = this.leftColumn.parent.element.height();
        //     this.overlay.css("height", height);
        // },

        //-----PRIVATE METHODS-----
        _getType: function ()
        {
            return 'resizer';
        },
        _getName: function ()
        {
            return BlocksMessages.surfaceResizerName;
        },
        _refresh: function ()
        {
            var retVal = false;

            if (this.needsRefresh) {
                if (this.leftColumn && this.rightColumn) {

                    //make sure both columns are in the latest state
                    this.leftColumn._refresh();
                    this.rightColumn._refresh();

                    this.top = Math.min(this.leftColumn.top, this.rightColumn.top);
                    this.bottom = Math.max(this.leftColumn.bottom, this.rightColumn.bottom);

                    //important: don't use the right of the columns, since they seem to subtract the
                    //margin between two columns and don't return absolute bounds
                    this.left = this.rightColumn.left - blocks.elements.Resizer.HALF_WIDTH;
                    this.right = this.left + blocks.elements.Resizer.WIDTH;

                    //extra variable for easy deciding sidies
                    this.center = this.left + blocks.elements.Resizer.HALF_WIDTH;
                }

                this._redrawOverlay();

                retVal = true;
                this.needsRefresh = false;
            }

            return retVal;
        },
    });
}]);