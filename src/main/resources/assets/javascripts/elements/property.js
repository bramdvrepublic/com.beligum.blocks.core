/**
 * Created by wouter on 5/03/15.
 */

blocks
    .plugin("blocks.core.Elements.Property", ["blocks.core.Class", "blocks.core.Constants", "blocks.core.DomManipulation",  function (Class, Constants, DOM) {


// A container contains properties
// A property can contain a new container itself to go up the tree
        blocks.elements = blocks.elements || {};
        blocks.elements.Property = Class.create(blocks.elements.LayoutElement, {
            constructor: function (element, parent, index) {
                blocks.elements.Property.Super.call(this, element, parent, index);
                var ct = this.getContainer();
                ct.blocks.push(this);
                this.canDrag = ct.canLayout;
                this.isField = !DOM.isEntity(element);
                this.canEdit = DOM.canEdit(element);

                this.container = new blocks.elements.Container(element, this);
            },

            findActiveElement: function (x, y, minSearchLevel, maxSearchLevel) {
                maxSearchLevel = maxSearchLevel == null ? -1 : maxSearchLevel;
                var retVal = null;
                if (this.isTriggered(x, y)) {
                    if (maxSearchLevel != 0) {
                        retVal = this.container.findActiveElement(x, y, minSearchLevel - 1, maxSearchLevel - 1);
                    }
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
