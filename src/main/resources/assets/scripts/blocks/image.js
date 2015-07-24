/*
 * Allows editiong of an image when youy click on it
 * */
base.plugin("blocks.edit.Image", ["constants.blocks.common", "blocks.core.Edit", "blocks.core.Sidebar",  function (Constants, Edit, Sidebar)
{

    this.focus = function(windowID, element, blockEvent) {

        var values = [
            {value: "bordered", name: "Met rand"},
            {value: "", name: "Zonder rand"}
        ];

        Sidebar.addUniqueClass(windowID, element, "Rand", values);
        Sidebar.addValueAttribute(windowID, element, "Image url", "Paste or type an image link", "src", false, true);
    };

    this.blur = function() {

    };

    Edit.registerByTag("IMG", this);


}]);