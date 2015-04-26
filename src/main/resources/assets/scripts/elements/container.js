/**
 * Created by wouter on 5/03/15.
 */

base.plugin("blocks.core.Elements.Container", ["base.core.Class", "blocks.core.Constants", "blocks.core.DomManipulation", function (Class, Constants, DOM)
{
    blocks = window['blocks'] || {};
    // Region where templates can be dragged
    blocks.elements = blocks.elements || {};
    blocks.elements.Container = Class.create(blocks.elements.LayoutElement, {
        constructor: function (element, parent)
        {
            blocks.elements.Container.Super.call(this, element, parent, 0);
            this.blocks = [];

            if (this.parent != null && this.parent instanceof blocks.elements.Block) {
                this.left = this.parent.left;
                this.right = this.parent.right;
                this.top = this.parent.top;
                this.bottom = this.parent.bottom;
            }

            if (DOM.canLayout(element)) {
                this.canLayout = true;
                this.generateChildrenForColumn();
                this.fillRows();


            } else {
                for (var i = 0; i < this.element.children().length; i++) {
                    this.generateProperties($(this.element.children()[i]));
                }
            }
            this.overlay = null;
        },

        getElementAtSide: function (side)
        {
            return null;
        },

        calculateDropspots: function (side, dropspots)
        {
            if ((side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT) && this.children.length > 1) {
                dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
            }

            return dropspots;
        },


        generateProperties: function (element)
        {
            var prop = null;

            if (DOM.canLayout(element) || DOM.isEntity(element) || DOM.canEdit(element)) {
                prop = new blocks.elements.Property(element, this);
                this.children.push(prop);
            } else if (element.children().length > 0) {
                for (var i = 0; i < element.children().length; i++) {
                    this.generateProperties($(element.children()[i]));
                }
            }

        }

    });

}]);