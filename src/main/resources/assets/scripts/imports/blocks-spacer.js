/**
 * Created by wouter on 17/07/15.
 */
base.plugin("blocks.imports.Spacer", ["base.core.Class", "blocks.imports.Block", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.SidebarUtils", function (Class, Block, BlocksConstants, BlocksMessages, Sidebar, SidebarUtils)
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
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            BlocksSpacer.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            BlocksSpacer.Class.Super.prototype.blur.call(this, block, element);
        },
        getOptionConfigs: function (block, element)
        {
            var retVal = BlocksSpacer.Class.Super.prototype.getOptionConfigs.call(this, block, element);

            retVal.push(SidebarUtils.addSliderClass(Sidebar, block.element, "Hoogte", [
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