// smallest element with 4 corners
// and a function to check if x,y is inside the surface
blocks
    .plugin("blocks.core.Elements.Surface", ["blocks.core.Class", "blocks.core.Constants", function (Class, Constants) {


        blocks.elements = blocks.elements || {};

        blocks.elements.Surface = Class.create({
            calculateTop: function (element, relative) {
                //if (relative)
                //    return this.findOuterBorder(element, Constants.SIDE.TOP);
                //else
                    return element.offset().top
            },

            calculateBottom: function (element, relative) {
                //if (relative)
                //    return this.findOuterBorder(element, Constants.SIDE.BOTTOM);
                //else
                    return  element.offset().top + element.outerHeight();

            },

            calculateLeft: function (element, relative) {
                //if (relative)
                //    return this.findOuterBorder(element, Constants.SIDE.LEFT);
                //else
                    return element.offset().left
            },

            calculateRight: function (element, relative) {
                //if (relative)
                //    return this.findOuterBorder(element, Constants.SIDE.RIGHT);
                //else
                    return element.offset().left + element.outerWidth()
            },

            /*
            * Find the outer border in pixels relative to parent
            * */
            findOuterBorder: function(source, side) {
                var THRESHOLD = 5;
                var retVal = 0;
                if (side == Constants.SIDE.LEFT) {
                    var offsetX = source.offset().left;
                    var offsetY = source.offset().top;
                    var parent = source.parent();
                    var parentOffset = this.parent.left;
                    var elementFound = null;
                    while (offsetX > parentOffset && elementFound == null) {
                        offsetX -= THRESHOLD;
                        elementFound = this.findSibling(document.elementFromPoint(offsetX, offsetY + THRESHOLD), source);
                    }

                    if (elementFound == null || elementFound == source || elementFound == source.parent()) {
                        retVal = parentOffset;
                    } else {
                        retVal = this.calculateRight(elementFound, true);
                    }
                } else if (side == Constants.SIDE.RIGHT) {
                    var offsetX = this.calculateRight(source);
                    var offsetY = source.offset().top;
                    var parent = source.parent();
                    var parentOffset = this.parent.right;
                    var elementFound = null;
                    while (offsetX < parentOffset && elementFound == null) {
                        offsetX += THRESHOLD;
                        elementFound = this.findSibling(document.elementFromPoint(offsetX, offsetY + THRESHOLD), source);
                    }

                    if (elementFound == null || elementFound == source || elementFound == source.parent()) {
                        retVal = parentOffset;
                    } else {
                        retVal = this.calculateLeft(elementFound, true);
                    }
                }  else if (side == Constants.SIDE.TOP) {
                    var offsetX = source.offset().left;
                    var offsetY = source.offset().top;
                    var parent = source.parent();
                    var parentOffset = this.parent.top;
                    var elementFound = null;
                    while (offsetY > parentOffset && elementFound == null) {
                        offsetY -= THRESHOLD;
                        elementFound = this.findSibling(document.elementFromPoint(offsetX + THRESHOLD, offsetY ), source);
                    }

                    if (elementFound == null || elementFound == source || elementFound == source.parent()) {
                        retVal = parentOffset;
                    } else {
                        retVal = this.calculateBottom(elementFound, true);
                    }
                }  else if (side == Constants.SIDE.BOTTOM) {
                    var offsetX = source.offset().left;
                    var offsetY = this.calculateBottom(source);
                    var parent = source.parent();
                    var parentOffset = this.parent.bottom;
                    var elementFound = null;
                    while (offsetY < parentOffset && elementFound == null) {
                        offsetY += THRESHOLD;
                        elementFound = this.findSibling(document.elementFromPoint(offsetX + THRESHOLD, offsetY), source);
                    }

                    if (elementFound == null || elementFound == source || elementFound == source.parent()) {
                        retVal = parentOffset;
                    } else {
                        retVal = this.calculateTop(elementFound, true);
                    }
                }


                return retVal;
            },

            findSibling: function(element, sibling) {
                element = $(element);
                var parent = sibling.parent();
                if (element == parent) return element;
                if (element == sibling) return null;

                var retVal = sibling;
                var prevElement = element;

                element = element.parent();
                while(element.length > 0 && element[0] != parent[0] && element[0] != sibling[0]) {
                    prevElement = element;
                    element = element.parent();
                }
                if (element.length > 0) retVal = prevElement;
                return retVal;
            },



            constructor: function (top, bottom, left, right) {
                if (top <= bottom) {
                    this.top = top;
                    this.bottom = bottom;
                } else {
                    this.top = bottom;
                    this.bottom = top;
                }
                if (left <= right) {
                    this.left = left;
                    this.right = right;
                } else {
                    this.left = right;
                    this.right = left;
                }
            },

            isTriggered: function (x, y) {
                var retVal = false;
                if (this.top <= y && y <= this.bottom && this.left <= x && x <= this.right) {
                    retVal = true;
                }
                return retVal;
            }
        });

    }]);