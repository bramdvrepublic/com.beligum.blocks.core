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

    /*
     * This function is called when we want to enable editing on a property
     * */
    Edit.makeEditable = function (element)
    {
        var retVal = null;
        if (element.hasClass(Constants.PAGE_CONTENT_CLASS)) {
            retVal = registeredByTag[Constants.PAGE_CONTENT_CLASS.toUpperCase()];
        } else {
            retVal = registeredByTag[element.prop("tagName").toUpperCase()];
        }

        return retVal;
    };


}]);