/*
 * defines a resizehandle. The surface of the resizeHandle is the are that triggers when you hoover over it
 * the draw-surface is the surface that will be drawn in the dom (can be bigger or smaller).
 * left and rightcolumn are the columns that this handle will resize when dragged
 */
blocks
    .plugin("blocks.core.Elements.ResizeHandle", ["blocks.core.Class", "blocks.core.Constants", "blocks.core.Resizer", function (Class, Constants, Resizer) {

        var body = $("body");
        blocks.elements = blocks.elements || {};
        blocks.elements.ResizeHandle = Class.create(blocks.elements.Surface, {
            STATIC: {
                DRAW_WIDTH: 30,
                TRIGGER_WIDTH: 10
            },

            constructor: function (leftColumn, rightColumn) {
                this.leftColumn = leftColumn;
                this.rightColumn = rightColumn;

                this.overlay = $("<div />").addClass(Constants.COLUMN_RESIZER_CLASS);







            },

            update: function() {
                var left = Math.floor((this.calculateLeft(this.rightColumn.element) + this.calculateRight(this.leftColumn.element)) / 2) - Math.floor(blocks.elements.ResizeHandle.TRIGGER_WIDTH / 2)
                this.overlay.css("left", left);
                var siblings = this.leftColumn.parent.resizeHandles;
                var height = this.leftColumn.parent.bottom - this.leftColumn.parent.top;
                for (var i=0; i < siblings; i++ ) {
                    siblings[i].overlay.css("height", height);
                }
            },

            showOverlay: function() {
                var _this = this;
                this.overlay.on("mousedown.resizehandle", function () {
                    Resizer.startDrag(_this);
                    $(document).on("mouseup.resizehandle", function () {
                        $(document).off("mouseup.resizehandledrag");
                        Resizer.endDrag(null);

                    });
                });

                var left = 0; var width = blocks.elements.ResizeHandle.TRIGGER_WIDTH; var top = 0; var height = 0;
                var half_width = Math.floor(blocks.elements.ResizeHandle.TRIGGER_WIDTH / 2);

                var left = this.leftColumn.right - half_width;
                var top = this.leftColumn.top;
                var width = blocks.elements.ResizeHandle.TRIGGER_WIDTH;
                var height = this.leftColumn.bottom - this.leftColumn.top;

                this.overlay.css("top", top);
                this.overlay.css("height", height);
                this.overlay.css("left", left);
                this.overlay.css("width", width);
                this.overlay.css("z-index", Constants.maxIndex + 2);
                body.append(this.overlay);
            },

            removeOverlay: function() {
                this.overlay.off("mousedown.resizehandle");
                this.overlay.remove();
            }


        });
    }]);