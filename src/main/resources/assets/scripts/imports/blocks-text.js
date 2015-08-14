base.plugin("blocks.imports.Text", ["base.core.Class", "blocks.imports.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.MediumEditor", "blocks.core.Sidebar", function (Class, Widget, BlocksConstants, BlocksMessages, Broadcaster, Editor, Sidebar)
{
    var BlocksText = this;
    this.TAGS = ["blocks-text div", "blocks-text span"];

    (this.Class = Class.create(Widget.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksText.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            BlocksText.Class.Super.prototype.focus.call(this, block, element, hotspot, event);

            // Preparation
            element.attr("contenteditable", true);
            // last argument means inline (no enter allowed) or not
            var editor = Editor.getEditor(element, element.prop('tagName') == 'SPAN');
            this._setCursor(hotspot.left, hotspot.top);

            // Add toolbar to sidebar
            var toolbar = $(Editor.getToolbarElement());
            toolbar.addClass(BlocksConstants.PREVENT_BLUR_CLASS);
            //make sure, if we click the toolbar, the block-window doesn't pop up
            toolbar.attr(BlocksConstants.CLICK_ROLE_ATTR, BlocksConstants.FORCE_CLICK_ATTR_VALUE);
        },
        blur: function (block, element)
        {
            BlocksText.Class.Super.prototype.blur.call(this, block, element);

            Editor.removeEditor(element);
            element.removeAttr("contenteditable");
        },
        getOptionConfigs: function (block, element)
        {
            return [];
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetTextTitle;
        },

        //-----PRIVATE METHODS-----
        /*
         * Puts the cursor for given coordinates
         * */
        _setCursor: function (x, y)
        {
            var caretPosition = this._getRangeFromPosition(x, y);
            if (caretPosition != null) {
                var sel = window.getSelection();
                sel.removeAllRanges();
                sel.addRange(caretPosition);
            }
        },
        _getRangeFromPosition: function (x, y)
        {
            var range = null;
            if (document.caretPositionFromPoint) {
                var pos = document.caretPositionFromPoint(x, y);
                range = document.createRange();
//            range.selectNodeContents(pos.offsetNode);
                range.setStart(pos.offsetNode, pos.offset);
//            range.setEnd(pos.offsetNode, pos.offset);

            } else if (document.caretRangeFromPoint) {
                range = document.caretRangeFromPoint(x, y);
            } else {
                Logger.debug("Field editing is not supported ...");
            }

            return range;
        },

    })).register(this.TAGS);

}]);