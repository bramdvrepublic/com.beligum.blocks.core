/*
 * defines a resizehandle. The surface of the resizeHandle is the are that triggers when you hoover over it
 * the draw-surface is the surface that will be drawn in the dom (can be bigger or smaller).
 * left and rightcolumn are the columns that this handle will resize when dragged
 */
blocks
    .plugin("blocks.core.Elements.ResizeHandle", ["blocks.core.Class", function (Class) {

        blocks.elements = blocks.elements || {};
        blocks.elements.ResizeHandle = Class.create(blocks.elements.Surface, {
            STATIC: {
                DRAW_WIDTH: 30,
                TRIGGER_WIDTH: 40
            },

            constructor: function (leftColumn, rightColumn) {
                this.leftColumn = leftColumn;
                this.rightColumn = rightColumn;
                this.updateSurface();

            },

            // calculate location by location of left and right column
            calculateSurface: function (t, b, left, right) {
                var l = left - blocks.elements.ResizeHandle.TRIGGER_WIDTH;
                var r = right + blocks.elements.ResizeHandle.TRIGGER_WIDTH;
                blocks.elements.ResizeHandle.Super.call(this, t, b, l, r);
                this.drawSurface = new blocks.elements.Surface(t, b, l, r);
            },

            updateSurface: function () {
                if (this.leftColumn == null) {
                    this.calculateSurface(this.rightColumn.top, this.rightColumn.bottom, this.rightColumn.left, this.rightColumn.left);
                }
                else if (this.rightColumn == null) {
                    this.calculateSurface(this.leftColumn.top, this.leftColumn.bottom, this.leftColumn.right, this.leftColumn.right);
                }
                else {

                    this.calculateSurface(Math.min(this.leftColumn.top, this.rightColumn.top), Math.max(this.leftColumn.bottom, this.rightColumn.bottom), this.leftColumn.calculateRight(this.leftColumn.element), this.leftColumn.calculateLeft(this.rightColumn.element));
                }
            }

        });
    }]);