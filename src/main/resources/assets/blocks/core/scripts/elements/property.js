/**
 * Created by wouter on 5/03/15.
 */

base.plugin("blocks.core.Elements.Property", ["base.core.Class", "constants.base.core.internal", "constants.blocks.core", "blocks.core.DomManipulation", "base.core.Commons", function (Class, BaseConstantsInternal, BlocksConstants, DOM, Commons)
{

    var body = $("body");
    // A container contains properties
    // A property can contain a new container itself to go up the tree
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};
    blocks.elements.Property = Class.create(blocks.elements.LayoutElement, {

        STATIC: {
            //will keep an index of all registerd properties (to back-reference from their overlays)
            INDEX: {},
            OVERLAY_INDEX_ATTR: "data-property-index"
        },

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

            //we removed the max z-index application because eg. the notifications needs to come in front
            this.overlay = $("<div />")/*.css("z-index", DOM.getMaxZIndex())*/.addClass(BlocksConstants.SURFACE_ELEMENT_CLASS);
            if (this.isTemplate) {
                this.overlay.addClass(BlocksConstants.BLOCK_OVERLAY_CLASS);
            }
            else {
                this.overlay.addClass(BlocksConstants.PROPERTY_OVERLAY_CLASS);
            }

            //will be used to back-reference from the overlay to this object
            this.id = Commons.generateId();
            blocks.elements.Property.INDEX[this.id] = this;
            this.overlay.attr(blocks.elements.Property.OVERLAY_INDEX_ATTR, this.id);

            // Remove sides of layout lines to prevent overlap
            var block = this.parent.parent;
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
                if (side == BaseConstantsInternal.SIDE.LEFT) {
                    return this.getPrevious();
                } else if (side == BaseConstantsInternal.SIDE.RIGHT) {
                    return this.getNext();
                } else {
                    return null;
                }
            } else if (side == BaseConstantsInternal.SIDE.TOP) {
                return this.getPrevious();
            } else if (side == BaseConstantsInternal.SIDE.BOTTOM) {
                return this.getNext();
            } else {
                return null;
            }
        },

        generateProperties: function (parent, index)
        {
            var children = parent.children();
            var childcount = children.length;
            for (var i = 0; i < childcount; i++) {
                var child = $(children[i]);
                if (child[0].tagName == "BLOCKS-LAYOUT") {
                    var b = new blocks.elements.Container($(child.children("[property=container], [data-property=container]")[0]), this, index);
                    this.children.push(b);
                    index++;
                    //} else if (child.hasAttribute("property") || child.hasAttribute("data-property")) {
                    //    var b = new blocks.elements.Property(child, this, index);
                    //    this.children.push(b);
                    //    index++;
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
