/**
 * Created by wouter on 17/06/15.
 */
base.plugin("blocks.edit.Page", ["constants.blocks.core", "messages.blocks.core", "blocks.core.Edit", "blocks.core.Sidebar", "blocks.core.Broadcaster", "blocks.core.SidebarUtils", function (Constants, Messages, Edit, Sidebar, Broadcaster, SidebarUtils)
{
    var Page = this;
    var draggingAllowed = false;

    //TODO: refactor these away
    var newBlockButton = $('<a class="btn btn-default btn-sm pull-right" data-toggle="popover" data-trigger="click" data-placement="bottom" data-content="Drag this button to your page to create a new block."><i class="fa fa-fw fa-magic"></i></a>');
    var newBlock = $('<li class="' + Constants.CREATE_BLOCK_CLASS + '"><span>New block</span></li>');
    newBlock.append(newBlockButton);

    $(document).on(Broadcaster.EVENTS.ENABLE_DND, function ()
    {
        draggingAllowed = true;
        newBlockButton.removeAttr("disabled");
        newBlock.removeAttr("data-toggle");
        newBlock.removeAttr("title");
        newBlock.tooltip('destroy');
    });

    $(document).on(Broadcaster.EVENTS.DISABLE_DND, function ()
    {
        draggingAllowed = false;
        newBlockButton.attr("disabled", "");
        newBlock.attr("data-toggle", "tooltip");
        newBlock.attr("data-placement", "bottom");
        newBlock.attr("title", "Your window is not wide enough to drag and drop new blocks.");
        newBlock.tooltip();
    });

    this.focus = function (block, element, hotspot, event)
    {
        var retVal = [];

        var pageActions = $('<ul class="' + Constants.BLOCK_ACTIONS_CLASS + '"/>');
        var savePage = $('<li><span>Save changes</span></li>').append($('<a class="' + Constants.SAVE_PAGE_BUTTON + ' btn btn-primary btn-sm pull-right"><i class="fa fa-fw fa-floppy-o"></i></a>'));
        var deletePage = $('<li><span>Delete page</span></li>').append($('<a class="' + Constants.DELETE_PAGE_BUTTON + ' btn btn-danger btn-sm pull-right"><i class="fa fa-fw fa-trash-o"></i></a>'));
        //activation is done in mouse.js
        pageActions.append(savePage).append(deletePage).append(newBlock);
        pageActions = pageActions.wrap('<div/>');
        pageActions.append('<hr>');

        //initialize the newBlock popover
        $(document).ready(function ()
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

        retVal.push(pageActions);

        var title = $("title");
        if (title.length == 0) {
            title = $("<title property='page-title' />").html(Messages.defaultPageTitle);
            $("head").append(title);
        }

        if (title.hasAttribute("property")) {
            retVal.push(SidebarUtils.addValueHtml(Sidebar, title, "Page title", "Enter a title for this page", false));
        }

        return retVal;
    };

    this.blur = function (block, element)
    {
    };

    this.getWindowName = function ()
    {
        return "Page";
    };

    Edit.registerByTag(Constants.PAGE_CONTENT_CLASS, this);

}])