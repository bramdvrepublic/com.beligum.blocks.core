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
        STATIC: {

        },

        //-----CONSTANTS-----

        //-----VARIABLES-----
        // this will allow to create new blocks in the same way as moving
        // existing blocks
        isNew: false,

        //-----CONSTRUCTORS-----
        constructor: function (parentSurface, element)
        {
            blocks.elements.Block.Super.call(this, parentSurface, element);

            this.canDrag = true;
            this.dropspots = {};

            if (parentSurface && element) {
                this.overlay = this._createOverlay();
                this.overlay.addClass(BlocksConstants.OVERLAY_CLASS);
                UI.surfaceWrapper.append(this.overlay);

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
                this._redraw();
            }
            //if we call the constructor without arguments, we're creating a new block
            else {
                this.isNew = true;
            }
        },

        //-----PUBLIC METHODS-----
        isNewBlock: function()
        {
            return this.isNew;
        },

        //-----TODO UNCHECKED-----
        getElementAtSide: function (side)
        {
            if (DOM.isColumn(this.element)) {
                if (side == Constants.SIDE.LEFT) {
                    return this.getPrevious();
                }
                else if (side == Constants.SIDE.RIGHT) {
                    return this.getNext();
                }
                else {
                    return null;
                }
            }
            else if (side == Constants.SIDE.TOP) {
                return this.getPrevious();
            }
            else if (side == Constants.SIDE.BOTTOM) {
                return this.getNext();
            }
            else {
                return null;
            }
        },

        // gets all dropspots for this block and his parents, for each side
        // then generate the triggers (surfaces) for each dropspot
        generateDropspots: function ()
        {
            this.dropspots = {};
            this.dropspots[Constants.SIDE.TOP] = this.calculateDropspots(Constants.SIDE.TOP, []);
            this.dropspots[Constants.SIDE.BOTTOM] = this.calculateDropspots(Constants.SIDE.BOTTOM, []);
            this.dropspots[Constants.SIDE.LEFT] = this.calculateDropspots(Constants.SIDE.LEFT, []);
            this.dropspots[Constants.SIDE.RIGHT] = this.calculateDropspots(Constants.SIDE.RIGHT, []);

            this.generateTriggers();
        },

        // calculates the triggers (surface where mouse coordinates trigger a dropspot)
        generateTriggers: function ()
        {
            this.horizontalDropspots = [];
            this.verticalDropspots = [];

            var i = 0;
            for (i = this.dropspots[Constants.SIDE.TOP].length - 1; i >= 0; i--) {
                this.verticalDropspots.push(this.dropspots[Constants.SIDE.TOP][i]);
            }
            for (i = 0; i < this.dropspots[Constants.SIDE.BOTTOM].length; i++) {
                this.verticalDropspots.push(this.dropspots[Constants.SIDE.BOTTOM][i]);
            }

            for (i = this.dropspots[Constants.SIDE.LEFT].length - 1; i >= 0; i--) {
                this.horizontalDropspots.push(this.dropspots[Constants.SIDE.LEFT][i]);
            }
            for (i = 0; i < this.dropspots[Constants.SIDE.RIGHT].length; i++) {
                this.horizontalDropspots.push(this.dropspots[Constants.SIDE.RIGHT][i]);
            }
            for (i = 0; i < this.horizontalDropspots.length; i++) {
                this.horizontalDropspots[i].index = i;
                this.horizontalDropspots[i].block = this;
            }

            for (i = 0; i < this.verticalDropspots.length; i++) {
                this.verticalDropspots[i].index = i;
                this.verticalDropspots[i].block = this;
            }
        },

        // Find the dropspot that is triggered for x and y
        getTriggeredDropspot: function (direction, x, y)
        {
            var side = direction;
            var co = 0;
            var dp = [];
            if (direction == Constants.DIRECTION.UP || direction == Constants.DIRECTION.DOWN) {
                co = y;
                dp = this.verticalDropspots;
            } else if (direction == Constants.DIRECTION.LEFT || direction == Constants.DIRECTION.RIGHT) {
                co = x;
                dp = this.horizontalDropspots;
            }
            if (dp != null) {
                for (var i = 0; i < dp.length; i++) {
                    if (dp[i].isTriggered(co)) {
//                            Logger.debug("find triggered hotspot: " + dp[i]["min"] + " - " + dp[i]["max"]);
                        return dp[i];
                    }
                }
            }

            return null;
        },

        recalculateTriggers: function (direction, x, y, currentDropspot)
        {
            //Logger.debug("Recalculate triggers");
            if (currentDropspot == null || !currentDropspot.makeTriggers(x, y, direction)) {
                var newDropspot = currentDropspot;
                //Logger.debug("Create new trigger triggers");

                if (direction == Constants.DIRECTION.UP) {
                    newDropspot = this.verticalDropspots[this.verticalDropspots.length - 1];
                }
                else if (direction == Constants.DIRECTION.DOWN) {
                    newDropspot = this.verticalDropspots[0];
                }
                else if (direction == Constants.DIRECTION.LEFT) {
                    newDropspot = this.horizontalDropspots[this.horizontalDropspots.length - 1];
                }
                else if (direction == Constants.DIRECTION.RIGHT) {
                    newDropspot = this.horizontalDropspots[0];
                }

                //Logger.debug("Calculate at border - direction=" + direction);
                if (newDropspot != null) {
                    newDropspot.makeTriggers(x, y, direction);
                }
            }
        },

        calculateDropspots: function (side, dropspots)
        {
            if (side == Constants.SIDE.TOP || side == Constants.SIDE.BOTTOM) {
                dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
            }
            else if (this.element.siblings().length > 0) {
                dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
            }

            if (this.isOuter(side) && this.parent != null) {
                dropspots = this.parent.calculateDropspots(side, dropspots);
            }

            return dropspots;
        },

        getTotalBlocks: function ()
        {
            return this.getContainer().getBlocks();
        },

        //-----PRIVATE METHODS-----
        _getType: function()
        {
            return 'block';
        },
        _getName: function()
        {
            return BlocksMessages.surfaceBlockName;
        },
        _newChildInstance: function(element)
        {
            return new blocks.elements.Property(this, element);
        },
        _canHaveChildren: function ()
        {
            return true;
        },
        _isAcceptableChild: function(element)
        {
            return DOM.isProperty(element);
        },
        _isNear: function (one, two)
        {
            var THRESHOLD = 1;

            return Math.abs(one - two) <= THRESHOLD;
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