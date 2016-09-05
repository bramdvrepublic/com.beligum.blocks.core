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

            var VAL_PROP = "curieName";
            retVal.push(this.addUniqueAttributeValueAsync(Sidebar, block.element, BlocksMessages.searchBoxType, BlocksConstants.SEARCH_BOX_TYPE_ARG, "/blocks/admin/rdf/search/classes/", "title", VAL_PROP,
                null
                // function changeListener(oldValueTerm, newValueTerm)
                // {
                //     if (oldValueTerm) Logger.info(oldValueTerm[VAL_PROP]);
                //     if (newValueTerm) Logger.info(newValueTerm[VAL_PROP]);
                //
                //     //for now, we don't allow the combobox to switch to an "empty" value, so ignore if that happens (probably during initialization)
                //     if (!newValueTerm) {
                //         return;
                //     }
                //
                //     // don't change anything if they're both the same
                //     if (oldValueTerm && oldValueTerm[VAL_PROP] == newValueTerm[VAL_PROP]) {
                //         return;
                //     }
                // }
            ));
            retVal.push(this.addOptionalClass(Sidebar, block.element, BlocksMessages.searchBoxInline, BlocksConstants.SEARCH_BOX_CLASS_INLINE));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.searchBoxTitle;
        },

        //-----PRIVATE METHODS-----

    })).register(TAGS);

}]);