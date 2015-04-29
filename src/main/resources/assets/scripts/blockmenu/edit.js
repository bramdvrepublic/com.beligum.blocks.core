/*
 * This is the dispatcher for the edit functionality
 * When the START_EDIT_FIELD_EVENT is send, we check here which field has to be edited,
 * and we start the correct editing functionality.
 *
 * This plugins only provides text editing. Inline (medium editor) and full text (ckeditor)
 * Other plugins can register here by calling:
 *  - registerByTag(TAG_IN_UPPERCASE, function to call)
 *  - registerByType(type, function to call)
 *
 *  functions are called with the current blockevent (START_EDIT_FIELD EVENT) as parameter
 *
 * */
base.plugin("blocks.core.Edit", ["blocks.core.Broadcaster", "blocks.core.Constants", "blocks.core.Overlay", "blocks.core.DomManipulation", function (Broadcaster, Constants, Overlay, DOM)
{
    var Edit = this;
    var currentlyEditedElement = null;


    CKEDITOR.disableAutoInline = true;

    var isIframe = function (currentBlock)
    {
        return currentBlock.element.find("iframe").length == 1;
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
    }

    /*
     * This function is called when we want to enable editing on a property
     * */
    Edit.makeEditable = function (property)
    {
        var element = property.element;
        var doEdit = null;
        if (DOM.canEdit(element)) {
            doEdit = editFunction(property);
        }

        var retVal = null;
        if (doEdit == null) {
            retVal = {editFunction: null, editType: Constants.EDIT_NONE}

        } else {
            retVal = {
                editFunction: function (blockEvent)
                {
                    Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
                    element.addClass(Constants.PROPERTY_EDIT_CLASS);
                    currentlyEditedElement = element;
                    doEdit(blockEvent)
                },
                editType: isTextEdit(property) ? Constants.EDIT_TEXT : Constants.EDIT_OTHER
            }
        }
        return retVal;
    };

    /*
     * Clean up after edit. Called on END_EDIT_FIELD_EVENT;
     * */
    Edit.endEdit = function ()
    {

    };


    /*
     * Start full text editing on a block (start ckeditor)
     * */
    var doEditText = function (blockEvent)
    {
        var element = blockEvent.property.current.element;
        // Preparation
        element.attr("contenteditable", true);
        element.addClass(Constants.PROPERTY_EDIT_CLASS);
        var oldInlineStyle = element.attr("style");
        var editor = element.ckeditor().editor;

        var setCursor = function ()
        {
            var caretPosition = getRangeFromPosition(blockEvent.clientX, blockEvent.clientY);
            if (caretPosition != null) {
                var sel = window.getSelection();
                sel.removeAllRanges();
                sel.addRange(caretPosition);
            }
        };

        if (editor.status == "unloaded") {
            editor.on("instanceReady", function ()
            {
                setCursor();
            })
        } else {
            setCursor();
        }

        editor.on("blur", function ()
        {
            editor.destroy();
            element.removeAttr("contenteditable");
            element.removeClass(Constants.PROPERTY_EDIT_CLASS);
            if (oldInlineStyle == undefined) {
                element.removeAttr("style");
            } else {
                element.attr("style", oldInlineStyle);
            }
            Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);
        })

    };

    /*
     * Start full inline editing on a block (start medium editor)
     * */
    var doEditTextInline = function (blockEvent)
    {
        var element = blockEvent.property.current.element;

        element.attr("contenteditable", true);
        element.addClass(Constants.PROPERTY_EDIT_CLASS);
        element.focus();
        var editor = new Medium({element: element[0], mode: Medium.inlineMode});

        var setCursor = function ()
        {
            var caretPosition = getRangeFromPosition(blockEvent.clientX, blockEvent.clientY);
            if (caretPosition != null) {
                var sel = window.getSelection();
                sel.removeAllRanges();
                sel.addRange(caretPosition);
            }
        };
        setCursor();

        element.on("blur", function ()
        {
            editor.destroy();
            element.removeAttr("contenteditable");
            element.removeClass(Constants.PROPERTY_EDIT_CLASS);
            Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);
        })
    };

    var isTextEdit = function (property)
    {
        retVal = false;
        var f = editFunction(property);
        if (f != null && (f == doEditTextInline || f == doEditText)) retVal = true;
        return retVal;
    };

    var registeredByClass = {};
    this.registerByClass = function (clazz, callback)
    {
        registeredByClass[clazz.toUpperCase()] = callback;
    };

    var registeredByType = {};
    this.registerByType = function (type, callback)
    {
        registeredByType[type.toUpperCase()] = callback;
    };

    var registeredByTag = {};
    this.registerByTag = function (tag, callback)
    {
        registeredByTag[tag.toUpperCase()] = callback;
    };

    var editFunction = function (property)
    {
        var element = property.element;
        var retVal = null;
        if (DOM.isEntity(element)) {
            var t = element.attr(Constants.IS_ENTITY).toUpperCase();
            retVal = registeredByType[t];
        } else {
            retVal = registeredByTag[element.prop("tagName").toUpperCase()];
        }
        return retVal;
    };

//    Edit.registerByTag("IFRAME", doEditIframe);
    Edit.registerByTag("DIV", doEditText);
    Edit.registerByTag("P", doEditText);

    Edit.registerByTag("H1", doEditTextInline);
    Edit.registerByTag("H2", doEditTextInline);
    Edit.registerByTag("H3", doEditTextInline);
    Edit.registerByTag("H4", doEditTextInline);
    Edit.registerByTag("H5", doEditTextInline);
    Edit.registerByTag("H6", doEditTextInline);
    Edit.registerByTag("A", doEditTextInline);
    Edit.registerByTag("SPAN", doEditTextInline);


}]);