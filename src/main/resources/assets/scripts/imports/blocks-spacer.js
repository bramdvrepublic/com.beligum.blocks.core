/**
 * Created by wouter on 17/07/15.
 */
base.plugin("blocks.imports.BlocksSpacer", ["base.core.Class", "blocks.imports.Block", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", function (Class, Block, BlocksConstants, BlocksMessages, Sidebar)
{
    var BlocksSpacer = this;
    this.TAGS = ["blocks-spacer"];

    (this.Class = Class.create(Block.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksSpacer.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        getConfigs: function (block, element)
        {
            var retVal = BlocksSpacer.Class.Super.prototype.getConfigs.call(this, block, element);

            retVal.push(this.addSliderClass(Sidebar, block.element, "Hoogte", [
                {value: "xs", name: "Extra small"},
                {value: "sm", name: "Small"},
                {value: "md", name: "Medium"},
                {value: "lg", name: "Large"},
                {value: "xl", name: "Extra large"},
            ]));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetSpacerTitle;
        },

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);
}]);