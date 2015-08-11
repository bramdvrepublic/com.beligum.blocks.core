/*
 * Allows editiong of an image when youy click on it
 * */
base.plugin("blocks.edit.Image", ["constants.blocks.core", "blocks.core.Edit", "blocks.core.Sidebar", "blocks.core.SidebarUtils",  function (Constants, Edit, Sidebar, SidebarUtils)
{

    this.focus = function(block, element, event)
    {
        var retVal = [];

        retVal.push(SidebarUtils.addUniqueClass(Sidebar, element, "Rand", [
            {value: "bordered", name: "Met rand"},
            {value: "", name: "Zonder rand"}
        ]));
        retVal.push(SidebarUtils.addValueAttribute(Sidebar, element, "Image url", "Paste or type an image link", "src", false, true, false));

        return retVal;
    };

    this.blur = function(block, element)
    {
    };

    this.getWindowName = function ()
    {
        return "Image";
    };

    Edit.registerByTag("IMG", this);


}]);