// A row inside a column or a container
// Can only contain columns
base.plugin("blocks.core.Elements.Row", ["base.core.Class", "base.core.Constants", function (Class, Constants)
{
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};
    blocks.elements.Row = Class.create(blocks.elements.LayoutElement, {

        constructor: function (element, parent, index)
        {
            blocks.elements.Row.Super.call(this, element, parent, index);

            this.canDrag = true;
            this.generateChildrenForRow();

            this.overlay = null;
        },

        isOuterTop: function ()
        {
            return this.element.prev().length == 0
        },
        isOuterBottom: function ()
        {
            return this.element.next().length == 0
        },

        // Override
        getElementAtSide: function (side)
        {
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
        calculateDropspots: function (side, dropspots)
        {
            var isOuter = this.isOuter(side);
            if ((side == Constants.SIDE.TOP || side == Constants.SIDE.BOTTOM) && this.children.length > 1) {
                dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
            }

            if (this.isOuter(side) && this.parent != null) dropspots = this.parent.calculateDropspots(side, dropspots);
            return dropspots;
        },

        findElements: function (minSearchLevel, maxSearchLevel)
        {
            minSearchLevel = minSearchLevel == null ? 0 : minSearchLevel;
            maxSearchLevel = maxSearchLevel == null ? -1 : maxSearchLevel;
            var retVal = [];
            if (minSearchLevel <= 0) {
                retVal.push(this);
            }
            if (maxSearchLevel != 0) {
                for (var i = 0; i < this.children.length; i++) {
                    var props = this.children[i].findElements(minSearchLevel, maxSearchLevel);
                    for (var j = 0; j < props.length; j++) {
                        retVal.push(props[j]);
                    }
                }
            }

            return retVal;
        },

        showOverlay: function ()
        {
            for (var j = 0; j < this.resizeHandles.length; j++) {
                this.resizeHandles[j].showOverlay();
            }
        },

        removeOverlay: function ()
        {

            for (var j = 0; j < this.resizeHandles.length; j++) {
                this.resizeHandles[j].removeOverlay();
            }

        },

        generateProperties: function() {

        }

    });

}]);