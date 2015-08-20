base.plugin("blocks.imports.Text", ["base.core.Class", "blocks.imports.Property", "constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.MediumEditor", "blocks.core.Sidebar", function (Class, Property, BlocksConstants, BlocksMessages, Broadcaster, Editor, Sidebar)
{
    var Text = this;
    this.TAGS = ["div[property]", "span[property]"];
    //this.TAGS = ["blocks-text div", "blocks-text span", "div[property]", "span[property]"];

    (this.Class = Class.create(Property.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            Text.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            Text.Class.Super.prototype.focus.call(this, block, element, hotspot, event);

            // Preparation
            element.attr("contenteditable", true);

            //this allows us to set some specific additional options to the elements to control how the editor behaves
            var options = {};
            var optionsAttr = element.attr(BlocksConstants.TEXT_EDITOR_OPTIONS_ATTR);
            if (optionsAttr) {
                //this converts and array to an object
                var optionsAttrValues = optionsAttr.split(" ");
                for (var i=0;i<optionsAttrValues.length;i++) {
                    //for now, we don't have values, so just set to true
                    //note that code (eg the constuctor in mediumModule.js) depends on this to be true
                    options[optionsAttrValues[i]] = true;
                }
            }

            var inlineEditor = element.prop('tagName') == 'SPAN';

            // last argument means inline (no enter allowed) or not
            var editor = Editor.getEditor(element, inlineEditor, options[BlocksConstants.TEXT_EDITOR_OPTIONS_NO_TOOLBAR]);
            this._setCursor(hotspot.left, hotspot.top);

            // Add toolbar to sidebar
            var toolbar = $(Editor.getToolbarElement());
            if (toolbar) {
                toolbar.addClass(BlocksConstants.PREVENT_BLUR_CLASS);
                //make sure, if we click the toolbar, the block-window doesn't pop up
                toolbar.attr(BlocksConstants.CLICK_ROLE_ATTR, BlocksConstants.FORCE_CLICK_ATTR_VALUE);
            }
        },
        blur: function (block, element)
        {
            Text.Class.Super.prototype.blur.call(this, block, element);

            Editor.removeEditor(element);
            element.removeAttr("contenteditable");
        },
        getConfigs: function (block, element)
        {
            return Text.Class.Super.prototype.getConfigs.call(this, block, element);
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