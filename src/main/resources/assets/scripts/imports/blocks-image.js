/*
 * Allows editing of an image when youy click on it
 * */
base.plugin("blocks.edit.Image", ["base.core.Class", "blocks.edit.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.SidebarUtils",  function (Class, Widget, BlocksConstants, BlocksMessages, Sidebar, SidebarUtils)
{
    var ImageWidget = this;
    var TAGS = ["IMG"];

    (this.Class = Class.create(Widget.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            ImageWidget.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            ImageWidget.Class.Super.prototype.focus.call(this);
        },
        blur: function (block, element)
        {
            ImageWidget.Class.Super.prototype.blur.call(this);
        },
        getOptionConfigs: function (block, element)
        {
            var retVal = [];

            retVal.push(SidebarUtils.addUniqueClass(Sidebar, element, "Rand", [
                {value: "bordered", name: "Met rand"},
                {value: "", name: "Zonder rand"}
            ]));
            retVal.push(SidebarUtils.addValueAttribute(Sidebar, element, "Image url", "Paste or type an image link", "src", false, true, false));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetImageTitle;
        },

    })).register(TAGS);

}]);