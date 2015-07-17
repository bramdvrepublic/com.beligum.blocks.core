base.plugin("blocks.core.edit.InlineText", ["constants.blocks.common", "blocks.core.Broadcaster", "blocks.core.Edit", "blocks.core.MediumEditor", "blocks.core.Sidebar",  function (Constants, Broadcaster, Edit, Editor, Sidebar)
{

    var getRangeFromPosition = function (x, y)
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
    };

    /*
     * Puts the cursor for given coordinates
     * */
    var setCursor = function (x, y)
    {
        var caretPosition = getRangeFromPosition(x, y);
        if (caretPosition != null) {
            var sel = window.getSelection();
            sel.removeAllRanges();
            sel.addRange(caretPosition);
        }
    };

    /*
     * Start full text editing on a block (start Scribe)
     *
     * */
    this.focus = function (element, blockEvent)
    {
        // Preparation
        element.attr("contenteditable", true);
        var editor = Editor.getEditor(element, true);

        // Add toolbar to sidebar
        setCursor(blockEvent.clientX, blockEvent.clientY);
        var toolbar = $(Editor.getToolbarElement());
        toolbar.addClass(Constants.PREVENT_BLUR_CLASS);

    };

    this.blur = function(element) {
        Editor.removeEditor(element);
        element.removeAttr("contenteditable");
    };

    Edit.registerByTag("SPAN", this);

}]);