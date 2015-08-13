/*
 * Allows editiong of an embedded youtube video when you click on it
 * */
base.plugin("blocks.edit.Youtube", ["base.core.Class", "blocks.edit.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.SidebarUtils", function (Class, Widget, BlocksConstants, BlocksMessages, Sidebar, SidebarUtils)
{
    var YoutubeWidget = this;
    var TAGS = ["BLOCKS-YOUTUBE"];

    (this.Class = Class.create(Widget.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            YoutubeWidget.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            YoutubeWidget.Class.Super.prototype.focus.call(this);
        },
        blur: function (block, element)
        {
            YoutubeWidget.Class.Super.prototype.blur.call(this);
        },
        getOptionConfigs: function (block, element)
        {
            var retVal = [];

            var embedded = block.element.children(".embed-responsive");
            var iframe = embedded.children("iframe");
            iframe.addClass("edit");

            retVal.push(SidebarUtils.addValueAttribute(Sidebar, iframe, "video url", "Paste or type a Youtube link", "src", false, true, false));
            retVal.push(SidebarUtils.addUniqueClass(Sidebar, embedded, "formaat", [{"name": "16 x 9", value: "embed-responsive-16by9"}, {"name": "4 x 3", value: "embed-responsive-4by3"}]));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetYoutubeTitle;
        },

        //-----PRIVATE METHODS-----

    })).register(TAGS);

}]);