/**
 * Created by wouter on 17/07/15.
 */
base.plugin("blocks.imports.Block", ["base.core.Class", "blocks.imports.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Layouter", "base.core.Commons", "blocks.imports.All", function (Class, Widget, BlocksConstants, BlocksMessages, Layouter, Commons, All)
{
    var Block = this;
    this.TAGS = All.IMPORTS;

    (this.Class = Class.create(Widget.Class, {

        STATIC: {},

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            Block.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            Block.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            Block.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element, addCreateLink)
        {
            var retVal = [];

            var blockActions = $("<ul/>").addClass(BlocksConstants.BLOCK_ACTIONS_CLASS);

            var removeAction = $("<li><label>Remove block</label></li>").appendTo(blockActions);
            var removeButton = $("<a class='btn btn-danger btn-sm pull-right'><i class='fa fa-fw fa-trash-o'></i></a>").appendTo(removeAction);
            removeButton.click(function ()
            {
                //TODO let's not ask for a confirmation but implement an undo-function later on...
                //confirm.removeClass("hidden");
                //text.addClass("hidden");

                $("." + BlocksConstants.OPACITY_CLASS).removeClass(BlocksConstants.OPACITY_CLASS);
                Layouter.removeBlock(block);
            });

            retVal.push(blockActions);

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetBlockTitle;
        },

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);
}]);