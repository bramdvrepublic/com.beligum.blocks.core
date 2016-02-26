/*
 * Allows editiong of an embedded youtube video when you click on it
 * */
base.plugin("blocks.imports.BlocksYoutube", ["base.core.Class", "blocks.imports.Block", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", function (Class, Block, BlocksConstants, BlocksMessages, Sidebar)
{
    var BlocksYoutube = this;
    this.TAGS = ["blocks-youtube"];

    (this.Class = Class.create(Block.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksYoutube.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            BlocksYoutube.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            BlocksYoutube.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element)
        {
            var retVal = BlocksYoutube.Class.Super.prototype.getConfigs.call(this, block, element);

            var embedded = block.element.children(".embed-responsive");
            var iframe = embedded.children("iframe");
            iframe.addClass("edit");

            retVal.push(this.addValueAttribute(Sidebar, iframe, "video url", "Paste or type a Youtube link", "src", false, true, false));
            retVal.push(this.addUniqueClass(Sidebar, embedded, "formaat", [{"name": "16 x 9", value: "embed-responsive-16by9"}, {"name": "4 x 3", value: "embed-responsive-4by3"}]));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetYoutubeTitle;
        },

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);

}]);