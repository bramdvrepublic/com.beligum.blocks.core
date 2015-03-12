/**
 * Created by wouter on 9/03/15.
 */



/*
 * Special element that indicates a trigger where a block could be dropped
 * on an other block. the dropspot is located on a SIDE of the block and the
 * MIN and MAX value that defines the area on that side
 * e.g. side = TOP and min =0 and max= 10 then this dropspot will be triggered with an
 * y coordinate of 6
 *
 * */
blocks
    .plugin("blocks.core.Elements.Dropspot", ["blocks.core.Class", "blocks.core.Constants",  function (Class, Constants) {

        blocks.elements = blocks.elements || {};
        blocks.elements.Dropspot = Class.create({

            constructor: function (side, anchor, index) {
                this.block = null;
                this.anchor = anchor;
                this.index = index;
                this.side = side;
                this.other = this.setOther();
                this.min = 0;
                this.max = 0;
                if (this.anchor.element.hasClass("column") && this.other != null) {
                    Logger.debug(this);
                }
            },

            setOther: function () {
                var retVal = null;
                if (this.anchor != null) {
                    retVal = this.anchor.getElementAtSide(this.side);
                }
                return retVal;
            },

            makeTriggers: function (x, y, direction) {
                var BORDER_THRESHOLD = 15;
                if (this.side != direction && this.side != Constants.OPPOSITE_DIRECTION[direction]) {
                    Logger.debug("exit because droploc not on this side " + direction);
                    return false;
                }
                var left = 0;
                var right = 0;
                var current = 0;
                var prevLength = this.index - 1 < 0 ? 0 : this.index - 1;
                var nrDropspots = 0;
                var dropspots = []
                if (direction == Constants.SIDE.TOP || direction == Constants.SIDE.BOTTOM) {
                    left = this.block.top;
                    right = this.block.bottom;
                    current = y;
//                    var prev = this.anchor.verticalDropspots.slice(0, this.index);
//                    var next = this.anchor.verticalDropspots.slice(this.index + 1, this.anchor.verticalDropspots.length);
                    nrDropspots = this.block.verticalDropspots.length;
                    dropspots = this.block.verticalDropspots;

                } else {
                    left = this.block.left;
                    right = this.block.right;
                    current = x;
//                    var prev = this.anchor.horizontalDropspots.slice(0, this.index);
//                    var next = this.anchor.horizontalDropspots.slice(this.index + 1, this.anchor.horizontalDropspots.length);
                    nrDropspots = this.block.horizontalDropspots.length;
                    dropspots = this.block.horizontalDropspots;
                }

                var border_threshold = Math.round((right - left) / nrDropspots);
                if (border_threshold > BORDER_THRESHOLD) border_threshold = BORDER_THRESHOLD;
                var inner_threshold = (right - left) - (border_threshold * (nrDropspots - 1));

//                var min = (current - ((threshold * prevLength) + (threshold/2)));
//                if (min < left) min = left;
//                if (right < (min + (threshold * nrDropspots))) {
//                    min = right - (threshold * nrDropspots);
//                }

                var i = 0;
                var min = left;
                while (i < nrDropspots) {
                    var currentDP = dropspots[i];
                    currentDP.setTrigger(min, min + border_threshold);

                    if (i + 1 < nrDropspots && (dropspots[i + 1].side != currentDP.side)) {
                        if (currentDP.side == direction) {
                            currentDP.setTrigger(min, min + inner_threshold);
                        } else if (dropspots[i + 1].side == direction) {
                            min += border_threshold;
                            dropspots[i + 1].setTrigger(min, min + inner_threshold);
                            i++;
                        }
                        min += inner_threshold
                    } else {
                        min += border_threshold
                    }

                    i++;
                }
                dropspots[0].min = left;
                dropspots[dropspots.length - 1].max = right;
                return true;
            },

            setTrigger: function (min, max) {
                if (min < max) {
                    this.min = min;
                    this.max = max;
                } else {
                    this.max = min;
                    this.min = max;
                }
            },

            // co is an x or y value that must be between min and max
            isTriggered: function (co) {
                var retVal = false;
                if (co >= this.min && co <= this.max) {
                    retVal = true;
                }
                return retVal;
            }

        });

    }]);

