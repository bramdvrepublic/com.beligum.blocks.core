/**
 * Created by wouter on 17/06/15.
 */
base.plugin("blocks.edit.Page", ["constants.blocks.common", "blocks.core.Edit", "blocks.core.Sidebar", "blocks.core.Broadcaster",  function (Constants, Edit, Sidebar, Broadcaster)
{
    var Page = this;

    this.focus = function(windowID, element, blockEvent) {
        var title = $("title");
        if (title.length == 0) {
            title = $("<title />");
            $("head").append(title);
        }

        var pageActions = $('<ul class="'+Constants.BLOCK_ACTIONS_CLASS+'">');

        var savePage = $('<li class="'+Constants.SAVE_PAGE_BUTTON+'"><span>Save changes</span></li>').append($('<a class="btn btn-primary btn-sm pull-right"><i class="fa fa-floppy-o"></i></a>'));
        var deletePage = $('<li class="'+Constants.DELETE_PAGE_BUTTON+'"><span>Delete page</span></li>').append($('<a class="btn btn-danger btn-sm pull-right"><i class="fa fa-trash-o"></i></a>'));
        //activation is done in mouse.js
        var newBlock = $('<li class="'+Constants.CREATE_BLOCK_CLASS+'"><span>New block</span></li>').append($('<a class="btn btn-default btn-sm pull-right" data-toggle="popover" data-trigger="click" data-placement="bottom" data-content="Drag this button to your page to create a new block."><i class="fa fa-magic"></i></a>'));
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

        Sidebar.addValueHtml(windowID, title, "Page title", false);
    };

    this.blur = function() {

    };

    this.getWindowName = function() {
        return "Page";
    }

    Edit.registerByTag(Constants.PAGE_CONTENT_CLASS, this);

}]);