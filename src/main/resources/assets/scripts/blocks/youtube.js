/*
 * Allows editiong of an embedded youtube video when you click on it
 * */
base.plugin("blocks.edit.Youtube", ["constants.blocks.common", "blocks.core.Edit", "blocks.core.Sidebar",  function (Constants, Edit, Sidebar)
{

    this.focus = function(property) {
        var embedded = property.element.children(".embed-responsive");
        var iframe = embedded.children("iframe");
        iframe.addClass("edit");
        var windowID = Sidebar.createWindow(Constants.CONTENT, property, "Youtube video");
        Sidebar.addValueAttribute(windowID, iframe, "video url", "src", true);
        Sidebar.addUniqueClass(windowID, embedded, "formaat", [{"name": "16 x 9", value: "embed-responsive-16by9"}, {"name": "4 x 3", value: "embed-responsive-4by3"}]);
    };

    this.blur = function(property) {
        var embedded = property.element.children(".embed-responsive");
        var iframe = embedded.children("iframe");
        iframe.addClass("edit");
    };

    Edit.registerByTag("BLOCKS-YOUTUBE", this);


}]);