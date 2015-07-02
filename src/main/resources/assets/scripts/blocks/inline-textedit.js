base.plugin("blocks.core.edit.InlineText", ["constants.blocks.common", "blocks.core.Broadcaster", "blocks.core.Edit", "blocks.core.Editor", "blocks.core.Sidebar",  function (Constants, Broadcaster, Edit, Editor, Sidebar)
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
        var editor = Editor.getEditor(element, false);

        // Add toolbar to sidebar
        var windowID = Sidebar.createWindow(Constants.STYLE, element, "Tekst");
        Sidebar.addUIForProperty(windowID, element, Editor.toolbarElement);

        setCursor(blockEvent.clientX, blockEvent.clientY);

    };

    this.blur = function(element) {
        element.removeAttr("contenteditable");
        element.removeClass(Constants.PROPERTY_EDIT_CLASS);
    };

    Edit.registerByTag("SPAN", this);
    Edit.registerByTag("A", this);

}]);