/**
 * Created by bram on 24/02/16.
 */
base.plugin("blocks.imports.BlocksFicheEntryText", ["base.core.Class", "blocks.imports.Text", "constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.MediumEditor", "blocks.core.Sidebar", function (Class, Text, BlocksConstants, BlocksMessages, Broadcaster, Editor, Sidebar)
{
    var BlocksFicheEntryText = this;
    this.TAGS = [
        //Note: we don't allow the user to edit the label of the property (but it works)
        //"blocks-fiche-entry ."+BlocksConstants.FICHE_ENTRY_NAME_CLASS,
        "blocks-fiche-entry [data-property="+BlocksConstants.FICHE_ENTRY_VALUE_PROPERTY+"] ."+BlocksConstants.INPUT_TYPE_EDITOR,
        "blocks-fiche-entry [data-property="+BlocksConstants.FICHE_ENTRY_VALUE_PROPERTY+"] ."+BlocksConstants.INPUT_TYPE_INLINE_EDITOR
    ];

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