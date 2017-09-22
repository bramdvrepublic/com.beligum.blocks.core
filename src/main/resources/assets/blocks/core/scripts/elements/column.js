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
 * Created by wouter on 9/03/15.
 */

base.plugin("blocks.core.Elements.Column", ["base.core.Class", "constants.base.core.internal", function (Class, Constants)
{
    blocks = window['blocks'] || {};
    // A column (inside a row) -> Can contain rows or templates
    blocks.elements = blocks.elements || {};
    blocks.elements.Column = Class.create(blocks.elements.LayoutElement, {
        constructor: function (element, parent, index)
        {
            blocks.elements.Column.Super.call(this, element, parent, index);

            this.canDrag = false;
            this.generateChildrenForColumn();
            this.overlay = null;
        },

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
            } else if (side == Constants.SIDE.RIGHT) {
                return this.getNext();
            } else {
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

        generateProperties: function ()
        {

        }

    });
}]);
