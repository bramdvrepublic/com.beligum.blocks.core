base.plugin("blocks.imports.BlocksFicheEntryText", ["base.core.Class", "blocks.imports.Text", "constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.MediumEditor", "blocks.core.Sidebar", function (Class, Text, BlocksConstants, BlocksMessages, Broadcaster, Editor, Sidebar)
{
    var BlocksFicheEntryText = this;
    this.TAGS = ["blocks-fiche-entry [data-property='label']", "blocks-fiche-entry .value"];

    (this.Class = Class.create(Text.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksFicheEntryText.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        getConfigs: function (block, element)
        {
            return BlocksFicheEntryText.Class.Super.prototype.getConfigs.call(this, block, element);
        },

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);

}]);