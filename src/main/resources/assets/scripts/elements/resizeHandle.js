/*
 * defines a resizehandle. The surface of the resizeHandle is the are that triggers when you hover over it
 * the draw-surface is the surface that will be drawn in the dom (can be bigger or smaller).
 * left and rightcolumn are the columns that this handle will resize when dragged
 */
base.plugin("blocks.core.Elements.ResizeHandle", ["base.core.Class", "constants.blocks.core", "blocks.core.Resizer", function (Class, BlocksConstants, Resizer)
{
    var body = $("body");
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};
    blocks.elements.ResizeHandle = Class.create(blocks.elements.Surface, {
        STATIC: {
            DRAW_WIDTH: 30,
            TRIGGER_WIDTH: 6
        },

        constructor: function (leftColumn, rightColumn)
        {
            this.leftColumn = leftColumn;
            this.rightColumn = rightColumn;

            this.overlay = $("<div />").addClass(BlocksConstants.COLUMN_RESIZER_CLASS);

        },

        update: function ()
        {
            var left = Math.floor((this.calculateLeft(this.rightColumn.element) + this.calculateRight(this.leftColumn.element)) / 2) - Math.floor(blocks.elements.ResizeHandle.TRIGGER_WIDTH / 2)
            this.overlay.css("left", left);
            var siblings = this.leftColumn.parent.resizeHandles;
            var height = this.calculateBottom(this.leftColumn.parent.element) - this.calculateTop(this.leftColumn.parent.element);
            for (var i = 0; i < siblings.length; i++) {
                siblings[i].overlay.css("height", height);
            }
        },

        showOverlay: function ()
        {
            var _this = this;
            this.overlay.on("mousedown.resizehandle", function (event)
            {
                // only start drag on left click
                if (event.which == 1) {
                    Resizer.startDrag(_this);
                    $(document).on("mouseup.resizehandle", function (event)
                    {
                        $(document).off("mouseup.resizehandle");
                        Resizer.endDrag(null);

                    });
                }
            });

            var left = 0;
            var width = blocks.elements.ResizeHandle.TRIGGER_WIDTH;
            var top = 0;
            var height = 0;
            var half_width = Math.floor(blocks.elements.ResizeHandle.TRIGGER_WIDTH / 2);

            var left = this.leftColumn.right - half_width;
            var top = this.leftColumn.top;
            var width = blocks.elements.ResizeHandle.TRIGGER_WIDTH;
            var height = this.leftColumn.bottom - this.leftColumn.top;

            this.overlay.css("top", top);
            this.overlay.css("height", height);
            this.overlay.css("left", left);
            this.overlay.css("width", width);
            this.overlay.css("z-index", base.utils.maxIndex + 2);

            //TODO
            //var wrapper = $('.'+BlocksConstants.BLOCK_OVERLAYS_WRAPPER_CLASS);
            //if (wrapper.length==0) {
            //    wrapper = $("<div class='" + BlocksConstants.BLOCK_OVERLAYS_WRAPPER_CLASS + "' />").appendTo($('.'+BlocksConstants.PAGE_CONTENT_CLASS));
            //}
            //wrapper.append(this.overlay);
            body.append(this.overlay);
        },

        updateHeight: function() {
            var height = this.leftColumn.parent.element.height();
            this.overlay.css("height", height);
        },

        removeOverlay: function ()
        {
            this.overlay.off("mousedown.resizehandle");
            this.overlay.remove();
        }
    });
}]);