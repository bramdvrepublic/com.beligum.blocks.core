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
base.plugin("blocks.core.Edit", ["blocks.core.Broadcaster", "constants.blocks.common", "blocks.core.Overlay", "blocks.core.DomManipulation", "blocks.core.Editor", function (Broadcaster, Constants, Overlay, DOM, Editor)
{
    var Edit = this;
    var currentlyEditedElement = null;


    //CKEDITOR.disableAutoInline = true;

    var isIframe = function (currentBlock)
    {
        return currentBlock.element.find("iframe").length == 1;
    };


    /*
     * This function is called when we want to enable editing on a property
     * */
    Edit.makeEditable = function (property)
    {
        return editFunction(property);
    };

    /*
     * Clean up after edit. Called on END_EDIT_FIELD_EVENT;
     * */
    Edit.endEdit = function ()
    {

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
            element.off("blur");
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
        if (property instanceof blocks.elements.Page) {
            retVal = registeredByTag[Constants.PAGE_CONTENT_CLASS.toUpperCase()];
        } else {
            retVal = registeredByTag[element.prop("tagName").toUpperCase()];
        }

        return retVal;
    };


//  Edit.registerByTag("IFRAME", doEditIframe);
//    Edit.registerByTag("DIV", doEditTextInline);
//    Edit.registerByTag("P", doEditTextInline);
//
//    Edit.registerByTag("H1", doEditTextInline);
//    Edit.registerByTag("H2", doEditTextInline);
//    Edit.registerByTag("H3", doEditTextInline);
//    Edit.registerByTag("H4", doEditTextInline);
//    Edit.registerByTag("H5", doEditTextInline);
//    Edit.registerByTag("H6", doEditTextInline);
//    Edit.registerByTag("A", doEditTextInline);
//    Edit.registerByTag("SPAN", doEditTextInline);


}]);