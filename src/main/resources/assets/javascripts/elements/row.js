

// A row inside a column or a container
// Can only contain columns
blocks
    .plugin("blocks.core.Elements.Row", ["blocks.core.Class", "blocks.core.Constants",  function (Class, Constants) {


        blocks.elements = blocks.elements || {};
        blocks.elements.Row = Class.create(blocks.elements.LayoutElement, {

            constructor: function (element, parent, index) {
                blocks.elements.Row.Super.call(this, element, parent, index);
                this.generateChildrenForRow();
            },

            isOuterTop: function () {
                return this.element.prev().length == 0
            },
            isOuterBottom: function () {
                return this.element.next().length == 0
            },

            // Override
            getElementAtSide: function (side) {
                if (side == Constants.SIDE.TOP) {
                    return this.getPrevious();
                } else if (side == Constants.SIDE.BOTTOM) {
                    return this.getNext();
                } else {
                    return null;
                }
            },

            // find all dropspots for an element
            // is called for a block and returns all dropspots for this block and his parents.
            calculateDropspots: function (side, dropspots) {
                var isOuter = this.isOuter(side);
                if ((side == Constants.SIDE.TOP || side == Constants.SIDE.BOTTOM) && this.children.length > 1) {
                    dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
                }

                if (this.isOuter(side) && this.parent != null) dropspots = this.parent.calculateDropspots(side, dropspots);
                return dropspots;
            }

        });

    }]);