/*
 * Allows editiong of an embedded youtube video when you click on it
 * */
base.plugin("blocks.edit.Youtube", ["constants.blocks.core", "blocks.core.Edit", "blocks.core.Sidebar",  function (Constants, Edit, Sidebar)
{
    this.focus = function(windowID, property, blockEvent) {
        var embedded = blockEvent.block.current.element.children(".embed-responsive");
        var iframe = embedded.children("iframe");
        iframe.addClass("edit");
        Sidebar.addValueAttribute(windowID, iframe, "video url", "Paste or type a Youtube link", "src", false, true, false);
        Sidebar.addUniqueClass(windowID, embedded, "formaat", [{"name": "16 x 9", value: "embed-responsive-16by9"}, {"name": "4 x 3", value: "embed-responsive-4by3"}]);
    };

    this.blur = function(property, block) {
        var embedded = block.element.children(".embed-responsive");
        var iframe = embedded.children("iframe");
        iframe.removeClass("edit");
    };

    Edit.registerByTag("BLOCKS-YOUTUBE", this);

}]);