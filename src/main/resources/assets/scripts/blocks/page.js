/**
 * Created by wouter on 17/06/15.
 */
base.plugin("blocks.edit.Page", ["constants.blocks.common", "blocks.core.Edit", "blocks.core.Sidebar", "blocks.core.Broadcaster",  function (Constants, Edit, Sidebar, Broadcaster)
{
    var Page = this;
    var draggingAllowed = false;


    var newBlockButton = $('<a class="btn btn-default btn-sm pull-right '+Constants.CREATE_BLOCK_CLASS+'" data-toggle="popover" data-trigger="click" data-placement="bottom" data-content="Drag this button to your page to create a new block."><i class="fa fa-magic"></i></a>');
    var newBlock = $('<li class=""><span>New block</span></li>');
    newBlock.append(newBlockButton);

    this.focus = function(windowID, element, blockEvent) {
        var title = $("title");
        if (title.length == 0) {
            title = $("<title />");
            $("head").append(title);
        }
        var pageActions = $('<ul class="'+Constants.BLOCK_ACTIONS_CLASS+'">');

        var savePage = $('<li><span>Save changes</span></li>');
        var savePageBtn = $('<a class="btn btn-primary btn-sm pull-right '+Constants.SAVE_PAGE_BUTTON+'"><i class="fa fa-floppy-o"></i></a>');
        savePage.append(savePageBtn);

        var deletePage = $('<li><span>Delete page</span></li>');
        var deletePageBtn = $('<a class="btn btn-danger btn-sm pull-right '+Constants.DELETE_PAGE_BUTTON+'"><i class="fa fa-trash-o"></i></a>');
        deletePage.append(deletePageBtn);

        //activation is done in mouse.js
        pageActions.append(savePage).append(deletePage).append(newBlock);

        Sidebar.addUIForProperty(windowID, pageActions);
        pageActions.after('<hr>');
        //initialize the newBlock popover
        $(document).ready(function()
        {
            newBlock.find('[data-toggle="popover"]').popover({
                container: 'body'
            }).on('shown.bs.popover', function ()
            {
                var $pop = $(this);
                setTimeout(function ()
                {
                    $pop.popover('hide');
                }, 2000);
            });
        });

        Sidebar.addValueHtml(windowID, title, "Page title", "Enter a title for this page", false);
    };

    this.blur = function() {

    };

    this.getWindowName = function() {
        return "Page";
    }

    $(document).on(Broadcaster.EVENTS.DO_ALLOW_DRAG, function ()
    {
        draggingAllowed = true;
        newBlockButton.removeAttr("disabled");
        newBlock.removeAttr("data-toggle");
        newBlock.removeAttr("title");
    ;   newBlock.tooltip('destroy');
    });

    $(document).on(Broadcaster.EVENTS.DO_NOT_ALLOW_DRAG, function ()
    {
        draggingAllowed = false;
        newBlockButton.attr("disabled", "");
        newBlock.attr("data-toggle", "tooltip");
        newBlock.attr("data-placement", "bottom");
        newBlock.attr("title", "Your window is not wide enough to drag and drop new blocks.");
        newBlock.tooltip();
    });

    Edit.registerByTag(Constants.PAGE_CONTENT_CLASS, this);

}]);