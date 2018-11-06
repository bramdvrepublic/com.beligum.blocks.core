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
 * Special element that indicates a trigger where a block could be dropped
 * on an other block. the dropspot is located on a SIDE of the block and the
 * MIN and MAX value that defines the area on that side
 * e.g. side=TOP and min=0 and max=10 then this dropspot will be triggered with an
 * y coordinate of 6
 *
 * Created by wouter on 9/03/15.
 */
base.plugin("blocks.core.elements.Dropspot", ["base.core.Class", "constants.base.core.internal", function (Class, Constants)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Dropspot = Class.create({

        //-----STATICS-----

        //-----CONSTANTS-----

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function (side, anchor, index)
        {
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

        //-----PUBLIC METHODS-----

        //-----TODO UNCHECKED-----
        setOther: function ()
        {
            var retVal = null;

            if (this.anchor != null) {
                retVal = this.anchor.getElementAtSide(this.side);
            }

            return retVal;
        },
        makeTriggers: function (x, y, direction)
        {
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
                if (this.block.verticalDropspots != null) {
                    nrDropspots = this.block.verticalDropspots.length;
                } else {
                    Logger.debug("No vertical dropspots");
                }

                dropspots = this.block.verticalDropspots;

            } else {
                left = this.block.left;
                right = this.block.right;
                current = x;
//                    var prev = this.anchor.horizontalDropspots.slice(0, this.index);
//                    var next = this.anchor.horizontalDropspots.slice(this.index + 1, this.anchor.horizontalDropspots.length);
                if (this.block.horizontalDropspots != null) {
                    nrDropspots = this.block.horizontalDropspots.length;
                }
                dropspots = this.block.horizontalDropspots;
            }

            if (nrDropspots > 0) {
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
            }
            return true;
        },
        setTrigger: function (min, max)
        {
            if (min < max) {
                this.min = min;
                this.max = max;
            } else {
                this.max = min;
                this.min = max;
            }
        },
        // co is an x or y value that must be between min and max
        isTriggered: function (co)
        {
            var retVal = false;
            if (co >= this.min && co <= this.max) {
                retVal = true;
            }
            return retVal;
        },

        //-----PRIVATE METHODS-----

    });

}]);
