/**
 * Created by wouter on 17/06/15.
 */
base.plugin("blocks.edit.Page", ["constants.blocks.common", "blocks.core.Edit", "blocks.core.Sidebar",  function (Constants, Edit, Sidebar)
{
    this.focus = function(element, blockEvent) {
        var title = $("title");
        if (title.length == 0) {
            title = $("<title />");
            $("head").append(title);
        }
        var windowID = Sidebar.createWindow(Constants.CONTENT, $("." + Constants.PAGE_CONTENT_CLASS), "Pagina");
        Sidebar.addValueHtml(windowID, title, "Page title", false);
    };

    this.blur = function() {


    };

    Edit.registerByTag(Constants.PAGE_CONTENT_CLASS, this);

}]);