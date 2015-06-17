base.plugin("blocks.core.TextEdit", ["constants.blocks.common", "blocks.core.Broadcaster", "blocks.core.Edit", "blocks.core.Editor", "blocks.core.Sidebar",  function (Constants, Broadcaster, Edit, Editor, Sidebar)
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
     this.focus = function (property, blockEvent)
    {
        var element = blockEvent.property.current.element;
        // Preparation
        element.attr("contenteditable", true);
        element.addClass(Constants.PROPERTY_EDIT_CLASS);
        var editor = Editor.getEditor(element);

        var oldValue = element.attr("data-old-value");
        if (oldValue == null) {
            element.attr("data-old-value", element.html());
        }

        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        element.addClass(Constants.PROPERTY_EDIT_CLASS);

        // Add toolbar to sidebar
        Sidebar.addUIForProperty(Constants.STYLE, property, Editor.toolbarElement);
        setCursor(blockEvent.clientX, blockEvent.clientY);

    };

    this.blur = function(property) {
        var element = property.element;
        element.removeAttr("contenteditable");
        element.removeClass(Constants.PROPERTY_EDIT_CLASS);
    };


    Edit.registerByTag("DIV", this);

}]);