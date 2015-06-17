// Special kind of row that can contains a template
// Draggable templates are the elements inside a column
base.plugin("blocks.core.Elements.Block", ["base.core.Class", "base.core.Constants", "constants.blocks.common", function (Class, Constants, BlocksConstants)
{
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};
    blocks.elements.Block = Class.create(blocks.elements.Property, {
        constructor: function (element, parent, index, canDrag)
        {
            blocks.elements.Block.Super.call(this, element, parent, index);
            // if a block is editable does not depend on the parent
            var prev = element.prev();
            var next = element.next();
            if (prev.length > 0) {
                this.top -= Math.floor((this.calculateBottom(prev) - this.top) / 2);
                this.overlay.addClass("top");
            }
            if (next.length > 0) {
                this.bottom += Math.floor((this.calculateTop(next) - this.bottom) / 2);
            }

            this.canDrag = canDrag;
            this.overlay.addClass(BlocksConstants.BLOCK_DRAGGABLE_CLASS);
            this.dropspots = {};
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
//                this.generateTriggersForSide(Constants.SIDE.TOP);
//                this.generateTriggersForSide(Constants.SIDE.BOTTOM);
//                this.generateTriggersForSide(Constants.SIDE.LEFT);
//                this.generateTriggersForSide(Constants.SIDE.RIGHT);

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
            Logger.debug("Recalculate triggers");
            if (currentDropspot == null || !currentDropspot.makeTriggers(x, y, direction)) {
                var newDropspot = currentDropspot;
                Logger.debug("Create new trigger triggers");
                try {
                    if (direction == Constants.DIRECTION.UP) {
                        newDropspot = this.verticalDropspots[this.verticalDropspots.length - 1];
                    } else if (direction == Constants.DIRECTION.DOWN) {
                        newDropspot = this.verticalDropspots[0];
                    } else if (direction == Constants.DIRECTION.LEFT) {
                        newDropspot = this.horizontalDropspots[this.horizontalDropspots.length - 1];
                    } else if (direction == Constants.DIRECTION.RIGHT) {
                        newDropspot = this.horizontalDropspots[0];
                    }
                } catch (e) {
                    Logger.error(this);
                }
                Logger.debug("Calculate at border");
                if (newDropspot != null) {
                    newDropspot.makeTriggers(x, y, direction);
                }
            }
        },

        calculateDropspots: function (side, dropspots)
        {
            if (side == Constants.SIDE.TOP || side == Constants.SIDE.BOTTOM) {
                dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
            } else if (this.element.siblings().length > 0) {
                dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
            }

            if (this.isOuter(side) && this.parent != null) dropspots = this.parent.calculateDropspots(side, dropspots);
            return dropspots;
        },


        getTotalBlocks: function ()
        {
            var c = this.getContainer();
            return c.getBlocks();
        }

    });
}]);