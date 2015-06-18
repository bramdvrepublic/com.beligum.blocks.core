/*
 * Allows editiong of an image when youy click on it
 * */
base.plugin("blocks.edit.Image", ["constants.blocks.common", "blocks.core.Edit", "blocks.core.Sidebar",  function (Constants, Edit, Sidebar)
{

    this.focus = function(property) {
        var element = property.element.children("img");
        var windowID = Sidebar.createWindow(Constants.CONTENT, property, "Afbeelding");
        Sidebar.addValueAttribute(windowID, element, "image url", "src", true);
    };

    this.blur = function() {

    };

    Edit.registerByTag("BLOCKS-IMAGE", this);


}]);