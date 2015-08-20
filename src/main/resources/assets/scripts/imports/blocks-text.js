base.plugin("blocks.imports.BlocksText", ["base.core.Class", "blocks.imports.Block", "constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.MediumEditor", "blocks.core.Sidebar", function (Class, Block, BlocksConstants, BlocksMessages, Broadcaster, Editor, Sidebar)
{
    var BlocksText = this;
    this.TAGS = ["blocks-text"];

    (this.Class = Class.create(Block.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksText.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        getConfigs: function (block, element)
        {
            return BlocksText.Class.Super.prototype.getConfigs.call(this, block, element);
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetTextTitle;
        },

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);

}]);