/**
 * Created by wouter on 25/08/15.
 */
base.plugin("mot.blocks.results", ["base.core.Class", "blocks.imports.Block", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", function (Class, Block, BlocksConstants, BlocksMessages, Sidebar)
{
    var SearchResults = this;
    var TAGS = ["blocks-search-results"];

    (this.Class = Class.create(Block.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            SearchResults.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            SearchResults.Class.Super.prototype.focus.call(this);
        },
        blur: function (block, element)
        {
            SearchResults.Class.Super.prototype.blur.call(this);
        },
        getConfigs: function (block, element)
        {
            var retVal = SearchResults.Class.Super.prototype.getConfigs.call(this, block, element);

            retVal.push(this.addUniqueAttributeValue(Sidebar, block.element, BlocksMessages.searchBoxResultsFormat, BlocksConstants.SEARCH_RESULTS_FORMAT_ARG,
                [
                    {
                        name: BlocksMessages.searchResultsFormatList,
                        value: BlocksConstants.SEARCH_RESULTS_FORMAT_LIST
                    },
                    {
                        name: BlocksMessages.searchResultsFormatLetters,
                        value: BlocksConstants.SEARCH_RESULTS_FORMAT_LETTERS
                    },
                    {
                        name: BlocksMessages.searchResultsFormatImages,
                        value: BlocksConstants.SEARCH_RESULTS_FORMAT_IMAGES
                    }
                ], null));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.searchBoxResultsTitle;
        },

        //-----PRIVATE METHODS-----

    })).register(TAGS);

}]);