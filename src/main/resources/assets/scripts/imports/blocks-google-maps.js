/**
 * Created by wouter on 1/09/15.
 */
base.plugin("blocks.imports.GoogleMaps", ["base.core.Class", "blocks.imports.Block", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", function (Class, Block, BlocksConstants, BlocksMessages, Sidebar)
{
    var BlocksMaps = this;
    this.TAGS = ["blocks-google-maps"];

    (this.Class = Class.create(Block.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksMaps.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            BlocksMaps.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            BlocksMaps.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element)
        {
            var retVal = BlocksMaps.Class.Super.prototype.getConfigs.call(this, block, element);

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetSpacerTitle;
        }

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);
}]);