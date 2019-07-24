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
 * A resizer that sits between two columns and can be dragged left and right to resize those columns.
 * It is created and lives inside row objects.
 */
base.plugin("blocks.core.elements.Resizer", ["base.core.Class", "constants.blocks.core", "messages.blocks.core", "blocks.core.UI", function (Class, BlocksConstants, BlocksMessages, UI)
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

            this.overlay = this._createOverlay(UI.resizerWrapper);
        },

        //-----PUBLIC METHODS-----
        /**
         * Override the parent function to really move the columns during preview (when the mouse is still down)
         * instead of during mouse release.
         *
         * @param surface The surface we're currently hovering on
         * @param vector The dragging vector
         */
        previewMoveTo: function (surface, vector, stats)
        {
            //the number of horizontal pixels we've dragged from the center of the resizer
            var offsetPx = vector.x1 - this.center;
            //right is +1, left is -1
            var side = Math.sign(offsetPx);
            //now we've stored the sign, cut it off
            offsetPx = Math.abs(offsetPx);

            //Logger.info(offsetPx+', '+vector.x1+', '+this.center);

            var row = this.leftColumn.parent;
            //the width of one column in pixels in the parent row
            var oneColPx = row.realWidth() / blocks.elements.Row.MAX_COLS;

            //The 'distance' we've dragged away from the center of the resizer,
            //expressed in columns. This will usually be in the [0-1] range,
            //because we move the resizer when nearing 1
            var colOffset = offsetPx / oneColPx;

            //note that using 0.75 (instead of 1.0) make the snapping feel more
            //natural when moving the mouse faster
            if (colOffset > 0.75) {
                var newLeftCols = this.leftColumn.columnWidth + side;
                var newRightCols = this.rightColumn.columnWidth - side;

                //make sure we don't create zero-width columns
                if (Math.min(newLeftCols, newRightCols) > 0) {

                    //note: these don't automatically refresh; see remark below
                    this.leftColumn.setColumnWidth(newLeftCols);
                    this.rightColumn.setColumnWidth(newRightCols);

                    //Force a deep refresh on the entire page because the height of the entire page can change when
                    //a columns gets resized.
                    //Also note that this needs to happen _after_ both updates are done, because the one pushes
                    //forward and the other needs to make room for the first to change width
                    this.leftColumn._getParent(blocks.elements.Page)._refresh(true);
                }
            }
        },

        //-----PRIVATE METHODS-----
        _getType: function ()
        {
            return 'resizer';
        },
        _getName: function ()
        {
            return BlocksMessages.surfaceResizerName;
        },
        _refresh: function (deep)
        {
            if (this.leftColumn && this.rightColumn) {

                //this is a rare situation where a parent surface (this resizer is a child of the parent row,
                // so is this resizer, so it's actually a sibling of the columns, but hey...)
                // depends on the bounds of it's children and not the other way around, so make sure
                // they are refreshed before we re-build the bounds of this resizer
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
        },
    });
}]);