base.plugin("blocks.imports.BlocksCarouselText", ["base.core.Class", "blocks.imports.Text", "constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.MediumEditor", "blocks.core.Sidebar", function (Class, Text, BlocksConstants, BlocksMessages, Broadcaster, Editor, Sidebar)
{
    var BlocksCarouselText = this;
    this.TAGS = ["blocks-carousel div[property]", "blocks-carousel span[property]"];

    (this.Class = Class.create(Text.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksCarouselText.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        getConfigs: function (block, element)
        {
            return BlocksCarouselText.Class.Super.prototype.getConfigs.call(this, block, element);
        },

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);

}]);