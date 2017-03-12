/**
 * Created by bram on 11/30/16.
 */
base.plugin("blocks.imports.Column", ["base.core.Class", "blocks.imports.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", function (Class, Widget, BlocksConstants, BlocksMessages, Sidebar)
{
    var BlocksColumn = this;
    this.TAGS = [
        ".col-xs-1", ".col-xs-2", ".col-xs-3", ".col-xs-4", ".col-xs-5", ".col-xs-6", ".col-xs-7", ".col-xs-8", ".col-xs-9", ".col-xs-10", ".col-xs-11", ".col-xs-12",
        ".col-sm-1", ".col-sm-2", ".col-sm-3", ".col-sm-4", ".col-sm-5", ".col-sm-6", ".col-sm-7", ".col-sm-8", ".col-sm-9", ".col-sm-10", ".col-sm-11", ".col-sm-12",
        ".col-md-1", ".col-md-2", ".col-md-3", ".col-md-4", ".col-md-5", ".col-md-6", ".col-md-7", ".col-md-8", ".col-md-9", ".col-md-10", ".col-md-11", ".col-md-12",
        ".col-lg-1", ".col-lg-2", ".col-lg-3", ".col-lg-4", ".col-lg-5", ".col-lg-6", ".col-lg-7", ".col-lg-8", ".col-lg-9", ".col-lg-10", ".col-lg-11", ".col-lg-12",
    ];

    (this.Class = Class.create(Widget.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksColumn.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            BlocksColumn.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            BlocksColumn.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element)
        {
            var retVal = BlocksColumn.Class.Super.prototype.getConfigs.call(this, block, element);

            retVal.push(this.addOptionalClass(Sidebar, block.element, BlocksMessages.columnSeamlessLabel, BlocksConstants.SEAMLESS_CLASS));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetColumnTitle;
        }

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);
}]);