/**
 * Created by wouter on 17/07/15.
 */
base.plugin("blocks.imports.Property", ["base.core.Class", "blocks.imports.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", function (Class, Widget, BlocksConstants, BlocksMessages, Sidebar)
{
    var Property = this;

    (this.Class = Class.create(Widget.Class, {

        STATIC: {},

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            Property.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            Property.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            Property.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element)
        {
            var retVal = [];

            return retVal;
        },
        getWindowName: function ()
        {
            return Property.Class.Super.prototype.getWindowName.call(this);
        },

        //-----PRIVATE METHODS-----

    }));
}]);