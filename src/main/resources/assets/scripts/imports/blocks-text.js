base.plugin("blocks.core.edit.Text", ["constants.blocks.core", "blocks.core.Broadcaster", "blocks.core.Edit", "blocks.core.MediumEditor", "blocks.core.Sidebar", function (Constants, Broadcaster, Edit, Editor, Sidebar)
{
    /*
     * Start full text editing on a block (start Scribe)
     */
    this.focus = function (block, element, event)
    {
        var retVal = [];

        // Preparation
        element.attr("contenteditable", true);
        // last argument means inline (no enter allowed) or not
        var editor = Editor.getEditor(element, element.prop('tagName')!='SPAN');

        // Add toolbar to sidebar
        setCursor(event.originalEvent.clientX, event.originalEvent.clientY);
        var toolbar = $(Editor.getToolbarElement());
        toolbar.addClass(Constants.PREVENT_BLUR_CLASS);

        return retVal;
    };

    this.blur = function (block, element)
    {
        Editor.removeEditor(element);
        element.removeAttr("contenteditable");
    };

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

    Edit.registerByTag("DIV", this);
    Edit.registerByTag("SPAN", this);

}]);