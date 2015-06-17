/**
 * Created by wouter on 9/03/15.
 */

base.plugin("blocks.core.Elements.Column", ["base.core.Class", "base.core.Constants", function (Class, Constants)
{
    blocks = window['blocks'] || {};
    // A column (inside a row) -> Can contain rows or templates
    blocks.elements = blocks.elements || {};
    blocks.elements.Column = Class.create(blocks.elements.LayoutElement, {
        constructor: function (element, parent, index)
        {
            blocks.elements.Column.Super.call(this, element, parent, index);
            this.canDrag = true;
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

        generateProperties: function() {

        }

    });
}]);
