// smallest element with 4 corners
// and a function to check if x,y is inside the surface
blocks
    .plugin("blocks.core.Elements.Surface", ["blocks.core.Class", function (Class) {


        blocks.elements = blocks.elements || {};

        blocks.elements.Surface = Class.create({
            calculateTop: function (element) {
                return element.offset().top
            },

            calculateBottom: function (element) {
                return element.offset().top + element.outerHeight()
            },

            calculateLeft: function (element) {
                return element.offset().left
            },

            calculateRight: function (element) {
                return element.offset().left + element.outerWidth()
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
