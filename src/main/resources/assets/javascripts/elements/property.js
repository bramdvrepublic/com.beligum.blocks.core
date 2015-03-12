/**
 * Created by wouter on 5/03/15.
 */

blocks
    .plugin("blocks.core.Elements.Property", ["blocks.core.Class", "blocks.core.Constants", "blocks.core.DomManipulation", "blocks.core.Edit",  function (Class, Constants, DOM, Edit) {


// A container contains properties
// A property can contain a new container itself to go up the tree
        blocks.elements = blocks.elements || {};
        blocks.elements.Property = Class.create(blocks.elements.LayoutElement, {
            constructor: function (element, parent, index) {
                blocks.elements.Property.Super.call(this, element, parent, index);
                var ct = this.getContainer();
                ct.blocks.push(this);

                if (this.element.siblings().length == 0 && this.element.parent == parent.element) {
                    this.left = this.parent.left;
                    this.right = this.parent.right;
                    this.top = this.parent.top;
                    this.bottom = this.parent.bottom;
                }

                this.canDrag = false;
                this.isField = !DOM.isEntity(element);
                this.isEntity = !this.isField;
                this.canEdit = DOM.canEdit(element);

                var edit = Edit.makeEditable(this);
                this.editFunction = edit.editFunction;
                this.editType = edit.editType;

                this.overlay = $("<div />").css("z-index", Constants.maxIndex);

                var block = this.parent.parent;
                if (this.isEntity) {
                    this.overlay.addClass(Constants.BLOCK_OVERLAY_CLASS);
                } else //if  (this.parent.parent.isEntity)
                {
                    block.overlay.append(this.overlay.addClass(Constants.PROPERTY_OVERLAY_CLASS));
                    if (this.editType == Constants.EDIT_OTHER) {
                        this.overlay.addClass(Constants.NO_TEXT_CLASS);
                    }
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


                this.container = new blocks.elements.Container(element, this);
            },

            isNear: function(one, two) {
                var retVal = false;
                var THRESHOLD = 1;
                if (Math.abs(one - two) <= THRESHOLD) {
                    retVal = true;
                }
                return retVal;
            },

            findActiveElement: function (x, y) {
                var retVal = null;
                if (this.isTriggered(x, y)) {
                    retVal = this.container.findActiveElement(x, y);
                    if (retVal == null || retVal == this.container) {
                        retVal = this;
                    }
                }
                return retVal;
            },

            // Easily walk the tree and find the block that contains the coordinates
            findElements: function (minSearchLevel, maxSearchLevel) {
                minSearchLevel = minSearchLevel == null ? 0 : minSearchLevel;
                maxSearchLevel = maxSearchLevel == null ? -1 : maxSearchLevel;
                var retVal = [];
                if (minSearchLevel <= 0) {
                    retVal.push(this);
                }
                if (maxSearchLevel != 0) {
                    for (var i = 0; i < this.container.children.length; i++) {
                        var props = this.container.children[i].findElements(minSearchLevel - 1, maxSearchLevel - 1);
                        for (var j = 0; j < props.length; j++) {
                            retVal.push(props[j]);
                        }
                    }
                }

                return retVal;
            },


            isOuterTop: function () {
                retVal = this.element.prev().length == 0;

                return retVal;
            },
            isOuterBottom: function () {
                retVal = this.element.next().length == 0;

                return retVal;
            },

            getElementAtSide: function (side) {
                if (DOM.isColumn(this.element)) {
                    if (side == Constants.SIDE.LEFT) {
                        return this.getPrevious();
                    } else if (side == Constants.SIDE.RIGHT) {
                        return this.getNext();
                    } else {
                        return null;
                    }
                } else if (side == Constants.SIDE.TOP) {
                    return this.getPrevious();
                } else if (side == Constants.SIDE.BOTTOM) {
                    return this.getNext();
                } else {
                    return null;
                }
            },

            // Container is a LayoutElement without a parent
            getContainer: function () {
                var parent = this.parent;
                while (parent != null && !(parent instanceof blocks.elements.Container)) {
                    parent = parent.parent;
                }
                return parent;
            }

        });

    }]);
