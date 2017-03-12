/**
 * Created by bram on 11/30/16.
 */
base.plugin("blocks.imports.Row", ["base.core.Class", "blocks.imports.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", function (Class, Widget, BlocksConstants, BlocksMessages, Sidebar)
{
    var BlocksRow = this;
    this.TAGS = [".row"];

    (this.Class = Class.create(Widget.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksRow.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            BlocksRow.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            BlocksRow.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element)
        {
            var retVal = BlocksRow.Class.Super.prototype.getConfigs.call(this, block, element);

            retVal.push(this.addOptionalClass(Sidebar, block.element, BlocksMessages.rowSeamlessLabel, BlocksConstants.SEAMLESS_CLASS));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetRowTitle;
        }

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);
}]);