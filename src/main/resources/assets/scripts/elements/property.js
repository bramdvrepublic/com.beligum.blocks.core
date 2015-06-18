/**
 * Created by wouter on 5/03/15.
 */

base.plugin("blocks.core.Elements.Property", ["base.core.Class", "base.core.Constants", "constants.blocks.common", "blocks.core.DomManipulation", "blocks.core.Edit", function (Class, BaseConstants, BlocksConstants, DOM, Edit)
{

    var body = $("body");
    // A container contains properties
    // A property can contain a new container itself to go up the tree
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};
    blocks.elements.Property = Class.create(blocks.elements.LayoutElement, {
        constructor: function (element, parent, index)
        {
            blocks.elements.Property.Super.call(this, element, parent, index);
            //var ct = this.getContainer();
            //ct.blocks.push(this);

            if (this.element.siblings().length == 0 && this.element.parent == parent.element) {
                this.left = this.parent.left;
                this.right = this.parent.right;
                this.top = this.parent.top;
                this.bottom = this.parent.bottom;
            }

            this.canDrag = false;
            this.isField = !(this instanceof blocks.elements.Block);
            this.isTemplate = !this.isField;

            this.editFunction = Edit.makeEditable(this);
            this.canEdit = this.editFunction != null;

            this.overlay = $("<div />").css("z-index", base.utils.maxIndex);

            var block = this.parent.parent;
            if (this.isTemplate) {
                this.overlay.addClass(BlocksConstants.BLOCK_OVERLAY_CLASS);
            } else
            {
                this.overlay.addClass(BlocksConstants.PROPERTY_OVERLAY_CLASS);
            }

            // Remove sides of layout lines to prevent overlap
            if (!(this instanceof blocks.elements.Block) && block != null && block.overlay != null) {
                if (this.isNear(block.left, this.left)) this.overlay.addClass("left");
                if (this.isNear(block.top, this.top)) this.overlay.addClass("top");
                if (this.isNear(block.right, this.right)) this.overlay.addClass("right");
                if (this.isNear(block.bottom, this.bottom)) this.overlay.addClass("bottom");
            } else if (this instanceof blocks.elements.Block) {
                if (this.index == 0 && this.parent.parent.index == 0 && this.getContainer().parent != null && this.getContainer().parent.index > 0) {
                    this.overlay.addClass("top");
                }
            }
        },

        isNear: function (one, two)
        {
            var retVal = false;
            var THRESHOLD = 1;
            if (Math.abs(one - two) <= THRESHOLD) {
                retVal = true;
            }
            return retVal;
        },

        //findActiveElement: function (x, y)
        //{
        //    var retVal = null;
        //    if (this.isTriggered(x, y)) {
        //        retVal = this.findActiveElement(x, y, -1);
        //        if (retVal == null) {
        //            retVal = this;
        //        }
        //    }
        //    return retVal;
        //},

        // Easily walk the tree and find the block that contains the coordinates
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
                    var props = this.children[i].findElements(minSearchLevel - 1, maxSearchLevel - 1);
                    for (var j = 0; j < props.length; j++) {
                        retVal.push(props[j]);
                    }
                }
            }

            return retVal;
        },


        isOuterTop: function ()
        {
            retVal = this.element.prev().length == 0;

            return retVal;
        },
        isOuterBottom: function ()
        {
            retVal = this.element.next().length == 0;

            return retVal;
        },

        getElementAtSide: function (side)
        {
            if (DOM.isColumn(this.element)) {
                if (side == BaseConstants.SIDE.LEFT) {
                    return this.getPrevious();
                } else if (side == BaseConstants.SIDE.RIGHT) {
                    return this.getNext();
                } else {
                    return null;
                }
            } else if (side == BaseConstants.SIDE.TOP) {
                return this.getPrevious();
            } else if (side == BaseConstants.SIDE.BOTTOM) {
                return this.getNext();
            } else {
                return null;
            }
        },



        generateProperties: function(parent, index) {
            var children = parent.children();
            var childcount = children.length;
            for (var i=0; i < childcount; i++) {
                var child = $(children[i]);
                if (child[0].tagName == "BOOTSTRAP-LAYOUT") {
                    var b = new blocks.elements.Container($(child.children(".container")[0]), this, index);
                    this.children.push(b);
                    index++;
                } else if (child.hasAttribute("property")) {
                    var b = new blocks.elements.Property(child, this, index);
                    this.children.push(b);
                    index++;
                } else if (child[0].tagName.indexOf("-") > 0) {
                    var b = new blocks.elements.Block(child, this, index, false);
                    this.children.push(b);
                    index++;
                } else if (child.children.length > 0) {
                    this.generateProperties(child, index);
                }
            }
        }

    });

}]);
