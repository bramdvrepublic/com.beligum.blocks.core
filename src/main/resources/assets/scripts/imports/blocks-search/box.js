/**
 * Created by wouter on 25/08/15.
 */
base.plugin("mot.blocks.search", ["base.core.Class", "blocks.imports.Block", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", function (Class, Block, BlocksConstants, BlocksMessages, Sidebar)
{
    var SearchBox = this;
    var TAGS = ["blocks-search-box"];

    (this.Class = Class.create(Block.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            SearchBox.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            SearchBox.Class.Super.prototype.focus.call(this);
        },
        blur: function (block, element)
        {
            SearchBox.Class.Super.prototype.blur.call(this);
        },
        getConfigs: function (block, element)
        {
            var retVal = SearchBox.Class.Super.prototype.getConfigs.call(this, block, element);

            retVal.push(this.addUniqueAttributeValueAsync(Sidebar, block.element, BlocksMessages.searchBoxType, BlocksConstants.SEARCH_BOX_TYPE_ARG, "/blocks/admin/rdf/classes/", "title", "curieName", null));
            retVal.push(this.addUniqueAttributeValue(Sidebar, block.element, BlocksMessages.searchBoxResultsFormat, BlocksConstants.SEARCH_BOX_RESULTS_FORMAT_ARG, [{name: BlocksMessages.searchResultsFormatList, value: BlocksConstants.SEARCH_RESULTS_FORMAT_LIST}, {name: BlocksMessages.searchResultsFormatLetters, value: BlocksConstants.SEARCH_RESULTS_FORMAT_LETTERS}], null));
            retVal.push(this.addOptionalClass(Sidebar, block.element, BlocksMessages.searchBoxInline, BlocksConstants.SEARCH_BOX_CLASS_INLINE));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetSearchTitle;
        },

        //-----PRIVATE METHODS-----

    })).register(TAGS);

}]);