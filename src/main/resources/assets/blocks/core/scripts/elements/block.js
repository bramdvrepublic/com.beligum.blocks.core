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
 * Special kind of row that can contains a template
 * Draggable templates are the elements inside a column
 */
base.plugin("blocks.core.elements.Block", ["base.core.Class", "constants.base.core.internal", "constants.blocks.core", "messages.blocks.core", "blocks.core.DOM", "blocks.core.UI", function (Class, Constants, BlocksConstants, BlocksMessages, DOM, UI)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Block = Class.create(blocks.elements.Surface, {

        //-----STATICS-----
        STATIC: {},

        //-----CONSTANTS-----

        //-----VARIABLES-----
        // this will allow to create new blocks in the same way as moving
        // existing blocks
        isNew: false,

        //-----CONSTRUCTORS-----
        constructor: function (parentSurface, element)
        {
            blocks.elements.Block.Super.call(this, parentSurface, element);

            if (parentSurface && element) {
                this.overlay = this._createOverlay(UI.surfaceWrapper);
                this.overlay.addClass(BlocksConstants.OVERLAY_CLASS);

                //these two classes will remove the borders left and top so we don't
                //have double borders when two blocks are next to each other
                if (this.parent.index > 0) {
                    //if we're not in the leftmost column, we remove the left border
                    //and use the right border of the blocks in the previous column
                    this.overlay.addClass(blocks.elements.Surface.LEFT_CLASS);
                }
                if (this.index > 0 || this.parent.parent.index > 0) {
                    //- If we're not the first block in the column, we remove the top border
                    //  and use the bottom border of the previous block in this column instead
                    //- Or if we are the first block, but we're not in the first row of the page,
                    //  we also remove it, because we use the bottom border of the last block in the
                    //  previous row instead.
                    this.overlay.addClass(blocks.elements.Surface.TOP_CLASS);
                }

                //draw the newly created overlay
                this._redrawOverlay();
            }
            //if we call the constructor without arguments, we're creating a new block
            else {
                this.isNew = true;
            }
        },

        //-----PUBLIC METHODS-----
        isNewBlock: function ()
        {
            return this.isNew;
        },

        /**
         * Effectively move this block to the indicated side of the supplied surface.
         *
         * @param surface
         * @param side
         */
        moveTo: function (surface, side)
        {
            var description = 'moving ' + this.type + ' "' + this.id + '" to ' + side.name + ' of ' + surface.type + ' "' + surface.id + '"';
            Logger.info(description);

            // If we're moving this block to the side of another block,
            // we need to distinguish between:
            // - top/down
            //   Just move the source element before or after the target element
            // - left/right
            //   We need to introduce a new row instead of the target element and split it using two new columns
            if (surface.isBlock()) {

                //detach the child from its parent
                this.parent._removeChild(this);

                switch (side.id) {
                    case blocks.elements.Surface.SIDE.TOP.id:

                        surface.parent._addChild(this, surface.index);

                        break;
                    case blocks.elements.Surface.SIDE.BOTTOM.id:

                        surface.parent._addChild(this, surface.index + 1);

                        break;
                    case blocks.elements.Surface.SIDE.LEFT.id:
                    case blocks.elements.Surface.SIDE.RIGHT.id:

                        var parentRow = surface._getParent(blocks.elements.Row);
                        var parentCol = surface._getParent(blocks.elements.Column);

                        var rowTag = parentRow._getTagName();
                        var colTag = parentCol._getTagName();
                        var colSize = parentCol.columnSize;

                        // We'll create a new row at the same place of the dropped block and remove that block from it's parent.
                        // In that new row, we'll create two columns to put the dropped and dragged block.
                        // But we need to wrap the other children of the column in rows too because we either
                        // have all rows or all blocks in a column, so if we migrate one block to a row,
                        // we need to transform all other children to rows.
                        for (var i = 0; i < parentCol.children.length; i++) {

                            var child = parentCol.children[i];

                            //Note: no need to create a row if it would already be a row (shouldn't happen though)
                            if (child === surface || !child.isRow()) {

                                //in both cases, we'll replace the child with a row
                                parentCol._removeChild(child);
                                //note that the index of the child is the same as it's array index 'i'
                                var newRow = parentCol._addChild(parentCol._newChildInstance(blocks.elements.Row.createElement(rowTag)), i);

                                if (child === surface) {

                                    //create two equal columns in the new row and fill them with the old and new block
                                    var newColLeft = newRow._addChild(newRow._newChildInstance(blocks.elements.Column.createElement(colSize, 6, colTag)));
                                    var newColRight = newRow._addChild(newRow._newChildInstance(blocks.elements.Column.createElement(colSize, 6, colTag)));

                                    if (side.id == blocks.elements.Surface.SIDE.RIGHT.id) {
                                        newColLeft._addChild(child);
                                        newColRight._addChild(this);
                                    }
                                    else {
                                        newColLeft._addChild(this);
                                        newColRight._addChild(child);
                                    }
                                }
                                else {
                                    var newCol = newRow._addChild(newRow._newChildInstance(blocks.elements.Column.createElement(colSize, 12, colTag)));
                                    newCol._addChild(child);
                                }
                            }
                        }

                        break;
                }
            }
            // If we're moving this block to the side of a column,
            // we need to distinguish between:
            // - top/down
            //   This probably shouldn't happen because we build the dropspots in-to-out
            //   and skip the parent spots that have the same dimension as the child spots
            //   so the top dropspot of the first/last child in this column should get it.
            // - left/right
            //   Create  new column in the parent row and distribute the widths
            else if (surface.isColumn()) {

                //detach the child from its parent
                this.parent._removeChild(this);

                switch (side.id) {
                    case blocks.elements.Surface.SIDE.TOP.id:
                    case blocks.elements.Surface.SIDE.BOTTOM.id:
                        Logger.error('Encountered unimplemented drop situation, this shouldn\'t happen; ' + description);
                        break;

                    case blocks.elements.Surface.SIDE.LEFT.id:
                    case blocks.elements.Surface.SIDE.RIGHT.id:

                        var row = surface._getParent(blocks.elements.Row);

                        // in a variable for flexibility
                        var newColWidth = 1;

                        //we need to find a column in the row whose width is > 1
                        //so we can at least substract one to make room from the new
                        //one-sized row. Ideally, it's the column we're dropping on,
                        //but not necessarily.
                        var colToAdjust = null;
                        if (surface.columnWidth > newColWidth) {
                            colToAdjust = surface;
                        }
                        else {
                            //we'll just take the first column we find, but ideally
                            //it should probably be the closest to the dropspot, no?
                            for (var i = 0; i < row.children.length && !colToAdjust; i++) {
                                if (row.children[i].columnWidth > newColWidth) {
                                    colToAdjust = row.children[i];
                                }
                            }
                        }

                        colToAdjust.setColumnWidth(colToAdjust.columnWidth - newColWidth);

                        var newColumnIdx = side.id === blocks.elements.Surface.SIDE.LEFT.id ? surface.index : surface.index + 1;
                        var newColumn = row._addChild(row._newChildInstance(blocks.elements.Column.createElement(colToAdjust.columnSize, newColWidth, colToAdjust._getTagName())), newColumnIdx);
                        newColumn._addChild(this);

                        //we added columns, so update the resizers
                        row._updateResizers();

                        break;
                }
            }
            else if (surface.isRow()) {

                //detach the child from its parent
                this.parent._removeChild(this);

                switch (side.id) {
                    case blocks.elements.Surface.SIDE.TOP.id:
                    case blocks.elements.Surface.SIDE.BOTTOM.id:

                        var row = surface;
                        var container = row._getParent(blocks.elements.Container);

                        var newRowIdx = side.id === blocks.elements.Surface.SIDE.TOP.id ? row.index : row.index + 1;
                        var newRow = container._addChild(container._newChildInstance(blocks.elements.Row.createElement(row._getTagName())), newRowIdx);

                        var refCol = row.children[0];
                        var newCol = newRow._addChild(newRow._newChildInstance(blocks.elements.Column.createElement(refCol.columnSize, 12, refCol._getTagName())));
                        newCol._addChild(this);

                        break;

                    case blocks.elements.Surface.SIDE.LEFT.id:
                    case blocks.elements.Surface.SIDE.RIGHT.id:

                        Logger.error('Encountered unimplemented drop situation, this shouldn\'t happen; ' + description);

                        break;
                }

            }
            else if (surface.isContainer()) {

                //detach the child from its parent
                this.parent._removeChild(this);

                switch (side.id) {
                    case blocks.elements.Surface.SIDE.TOP.id:
                    case blocks.elements.Surface.SIDE.BOTTOM.id:
                        Logger.error('Encountered unimplemented drop situation, this shouldn\'t happen; ' + description);
                        break;

                    case blocks.elements.Surface.SIDE.LEFT.id:
                    case blocks.elements.Surface.SIDE.RIGHT.id:

                        var container = surface;
                        var oldRow = container.children[0];
                        var oldCol = oldRow.children[0];

                        // in a variable for flexibility
                        var newColWidth = 1;

                        var newRowIdx = 0;
                        var newRow = container._addChild(container._newChildInstance(blocks.elements.Row.createElement(oldRow._getTagName())), newRowIdx);

                        var leftColIdx = side.id === blocks.elements.Surface.SIDE.LEFT.id ? 1 : 0;
                        var leftCol = newRow._addChild(newRow._newChildInstance(blocks.elements.Column.createElement(oldCol.columnSize, 12 - newColWidth, oldCol._getTagName())), leftColIdx);

                        for (var i = 0; i < container.children.length; i++) {
                            var child = container.children[i];

                            if (child !== newRow) {
                                container._removeChild(child);
                                i--;
                                leftCol._addChild(child);
                            }
                        }

                        var rightColIdx = side.id === blocks.elements.Surface.SIDE.LEFT.id ? 0 : 1;
                        var rightCol = newRow._addChild(newRow._newChildInstance(blocks.elements.Column.createElement(oldCol.columnSize, newColWidth, oldCol._getTagName())), rightColIdx);
                        rightCol._addChild(this);

                        //we added columns, so update the resizers
                        oldRow._updateResizers();
                        newRow._updateResizers();

                        break;
                }

            }
            else {
                Logger.error('Encountered unimplemented drop-surface type (' + surface.type + '); this shouldn\'t happen; ' + description);
            }

            //TODO cleanup the parent structure of this and the other surface now we moved it to another location
            //TODO move the classes on the element we're moving?
            this._cleanup();

            //Once all is done, we need to force a deep refresh of the entire page
            var page = this._getParent(blocks.elements.Page);
            if (page) {
                page._refresh(true);
            }
        },

        //-----TODO UNCHECKED-----
//         getElementAtSide: function (side)
//         {
//             if (DOM.isColumn(this.element)) {
//                 if (side == Constants.SIDE.LEFT) {
//                     return this.getPrevious();
//                 }
//                 else if (side == Constants.SIDE.RIGHT) {
//                     return this.getNext();
//                 }
//                 else {
//                     return null;
//                 }
//             }
//             else if (side == Constants.SIDE.TOP) {
//                 return this.getPrevious();
//             }
//             else if (side == Constants.SIDE.BOTTOM) {
//                 return this.getNext();
//             }
//             else {
//                 return null;
//             }
//         },
//
//         // gets all dropspots for this block and his parents, for each side
//         // then generate the triggers (surfaces) for each dropspot
//         generateDropspots: function ()
//         {
//             this.dropspots = {};
//             this.dropspots[Constants.SIDE.TOP] = this.calculateDropspots(Constants.SIDE.TOP, []);
//             this.dropspots[Constants.SIDE.BOTTOM] = this.calculateDropspots(Constants.SIDE.BOTTOM, []);
//             this.dropspots[Constants.SIDE.LEFT] = this.calculateDropspots(Constants.SIDE.LEFT, []);
//             this.dropspots[Constants.SIDE.RIGHT] = this.calculateDropspots(Constants.SIDE.RIGHT, []);
//
//             this.generateTriggers();
//         },
//
//         // calculates the triggers (surface where mouse coordinates trigger a dropspot)
//         generateTriggers: function ()
//         {
//             this.horizontalDropspots = [];
//             this.verticalDropspots = [];
//
//             var i = 0;
//             for (i = this.dropspots[Constants.SIDE.TOP].length - 1; i >= 0; i--) {
//                 this.verticalDropspots.push(this.dropspots[Constants.SIDE.TOP][i]);
//             }
//             for (i = 0; i < this.dropspots[Constants.SIDE.BOTTOM].length; i++) {
//                 this.verticalDropspots.push(this.dropspots[Constants.SIDE.BOTTOM][i]);
//             }
//
//             for (i = this.dropspots[Constants.SIDE.LEFT].length - 1; i >= 0; i--) {
//                 this.horizontalDropspots.push(this.dropspots[Constants.SIDE.LEFT][i]);
//             }
//             for (i = 0; i < this.dropspots[Constants.SIDE.RIGHT].length; i++) {
//                 this.horizontalDropspots.push(this.dropspots[Constants.SIDE.RIGHT][i]);
//             }
//             for (i = 0; i < this.horizontalDropspots.length; i++) {
//                 this.horizontalDropspots[i].index = i;
//                 this.horizontalDropspots[i].block = this;
//             }
//
//             for (i = 0; i < this.verticalDropspots.length; i++) {
//                 this.verticalDropspots[i].index = i;
//                 this.verticalDropspots[i].block = this;
//             }
//         },
//
//         // Find the dropspot that is triggered for x and y
//         getTriggeredDropspot: function (direction, x, y)
//         {
//             var side = direction;
//             var co = 0;
//             var dp = [];
//             if (direction == Constants.DIRECTION.UP || direction == Constants.DIRECTION.DOWN) {
//                 co = y;
//                 dp = this.verticalDropspots;
//             } else if (direction == Constants.DIRECTION.LEFT || direction == Constants.DIRECTION.RIGHT) {
//                 co = x;
//                 dp = this.horizontalDropspots;
//             }
//             if (dp != null) {
//                 for (var i = 0; i < dp.length; i++) {
//                     if (dp[i].isTriggered(co)) {
// //                            Logger.debug("find triggered hotspot: " + dp[i]["min"] + " - " + dp[i]["max"]);
//                         return dp[i];
//                     }
//                 }
//             }
//
//             return null;
//         },
//
//         recalculateTriggers: function (direction, x, y, currentDropspot)
//         {
//             //Logger.debug("Recalculate triggers");
//             if (currentDropspot == null || !currentDropspot.makeTriggers(x, y, direction)) {
//                 var newDropspot = currentDropspot;
//                 //Logger.debug("Create new trigger triggers");
//
//                 if (direction == Constants.DIRECTION.UP) {
//                     newDropspot = this.verticalDropspots[this.verticalDropspots.length - 1];
//                 }
//                 else if (direction == Constants.DIRECTION.DOWN) {
//                     newDropspot = this.verticalDropspots[0];
//                 }
//                 else if (direction == Constants.DIRECTION.LEFT) {
//                     newDropspot = this.horizontalDropspots[this.horizontalDropspots.length - 1];
//                 }
//                 else if (direction == Constants.DIRECTION.RIGHT) {
//                     newDropspot = this.horizontalDropspots[0];
//                 }
//
//                 //Logger.debug("Calculate at border - direction=" + direction);
//                 if (newDropspot != null) {
//                     newDropspot.makeTriggers(x, y, direction);
//                 }
//             }
//         },
//
//         calculateDropspots: function (side, dropspots)
//         {
//             if (side == Constants.SIDE.TOP || side == Constants.SIDE.BOTTOM) {
//                 dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
//             }
//             else if (this.element.siblings().length > 0) {
//                 dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
//             }
//
//             if (this.isOuter(side) && this.parent != null) {
//                 dropspots = this.parent.calculateDropspots(side, dropspots);
//             }
//
//             return dropspots;
//         },
//
//         getTotalBlocks: function ()
//         {
//             return this.getContainer().getBlocks();
//         },
//         _isNear: function (one, two)
//         {
//             var THRESHOLD = 1;
//
//             return Math.abs(one - two) <= THRESHOLD;
//         },

        //-----PRIVATE METHODS-----
        _getType: function ()
        {
            return 'block';
        },
        _getName: function ()
        {
            return BlocksMessages.surfaceBlockName;
        },
        _newChildInstance: function (element)
        {
            return new blocks.elements.Property(this, element);
        },
        _canHaveChildren: function ()
        {
            return true;
        },
        _isAcceptableChild: function (element)
        {
            return blocks.elements.Surface.isProperty(element);
        },
        _cleanup: function ()
        {

        },
    });

}]);