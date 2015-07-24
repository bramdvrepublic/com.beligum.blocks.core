/**
 * Created by wouter on 17/07/15.
 */
base.plugin("blocks.edit.Link", ["constants.blocks.common", "blocks.core.Edit", "blocks.core.Sidebar", function (Constants, Edit, Sidebar)
{
    this.focus = function (windowID, element, blockEvent)
    {
        Sidebar.addValueAttribute(windowID, element, "Link", "Paste or type a link", "href", false, true);
    };

    this.blur = function ()
    {
    };

    Edit.registerByTag("A", this);

}]);