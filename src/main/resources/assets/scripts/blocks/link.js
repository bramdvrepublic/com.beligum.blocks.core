/**
 * Created by wouter on 17/07/15.
 */
base.plugin("blocks.edit.Link", ["constants.blocks.common", "blocks.core.Edit", "blocks.core.Sidebar",  function (Constants, Edit, Sidebar)
{

    this.focus = function(element, blockEvent) {
        var windowID = Sidebar.createWindow(Constants.CONTENT, blockEvent.block.current.element, "Link");
        Sidebar.addValueAttribute(windowID, element, "Link", "href", true, true, true);
    };

    this.blur = function() {

    };

    Edit.registerByTag("A", this);

}]);