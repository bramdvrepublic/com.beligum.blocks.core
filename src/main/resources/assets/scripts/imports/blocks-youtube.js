/*
 * Allows editiong of an embedded youtube video when you click on it
 * */
base.plugin("blocks.edit.Youtube", ["constants.blocks.core", "blocks.core.Edit", "blocks.core.Sidebar", "blocks.core.SidebarUtils", function (Constants, Edit, Sidebar, SidebarUtils)
{
    this.focus = function (block, element, hotspot, event)
    {
        var retVal = [];

        var embedded = block.element.children(".embed-responsive");
        var iframe = embedded.children("iframe");
        iframe.addClass("edit");

        retVal.push(SidebarUtils.addValueAttribute(Sidebar, iframe, "video url", "Paste or type a Youtube link", "src", false, true, false));
        retVal.push(SidebarUtils.addUniqueClass(Sidebar, embedded, "formaat", [{"name": "16 x 9", value: "embed-responsive-16by9"}, {"name": "4 x 3", value: "embed-responsive-4by3"}]));

        return retVal;
    };

    this.blur = function (block, element)
    {
        var embedded = block.element.children(".embed-responsive");
        var iframe = embedded.children("iframe");
        iframe.removeClass("edit");
    };

    Edit.registerByTag("BLOCKS-YOUTUBE", this);

}]);