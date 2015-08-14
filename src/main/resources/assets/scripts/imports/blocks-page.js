/**
 * Created by wouter on 17/06/15.
 */
base.plugin("blocks.imports.Page", ["base.core.Class", "blocks.imports.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.Broadcaster", "blocks.core.SidebarUtils", "blocks.core.UI", function (Class, Widget, BlocksConstants, BlocksMessages, Sidebar, Broadcaster, SidebarUtils, UI)
{
    var BlocksPage = this;
    this.TAGS = ['.'+BlocksConstants.PAGE_CONTENT_CLASS];

    (this.Class = Class.create(Widget.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            BlocksPage.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {

        },
        focus: function (block, element, hotspot, event)
        {
            BlocksPage.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            BlocksPage.Class.Super.prototype.blur.call(this, block, element);
        },
        getOptionConfigs: function (block, element)
        {
            var retVal = [];

            var pageActions = $('<ul class="' + BlocksConstants.BLOCK_ACTIONS_CLASS + '"/>');
            var savePage = $('<li><span>Save changes</span></li>').append($('<a class="' + BlocksConstants.SAVE_PAGE_BUTTON + ' btn btn-primary btn-sm pull-right"><i class="fa fa-fw fa-floppy-o"></i></a>')).appendTo(pageActions);
            var deletePage = $('<li><span>Delete page</span></li>').append($('<a class="' + BlocksConstants.DELETE_PAGE_BUTTON + ' btn btn-default btn-sm pull-right"><i class="fa fa-fw fa-trash-o"></i></a>')).appendTo(pageActions);
            var newBlock = $('<li><span>New block</span></li>').append($('<a class="' + BlocksConstants.CREATE_BLOCK_CLASS + ' btn btn-default btn-sm pull-right" data-toggle="popover" data-trigger="click" data-placement="bottom" data-content="Drag this button to your page to create a new block."><i class="fa fa-fw fa-magic"></i></a>')).appendTo(pageActions);

            //activation is done in menu.js
            pageActions = pageActions.wrap('<div/>');
            pageActions.append('<hr>');

            //initialize the newBlock popover
            $(document).ready(function ()
            {
                var popover = newBlock.find('[data-toggle="popover"]').popover({
                    container: 'body'
                });
                popover.on('shown.bs.popover', function ()
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
                title = $("<title property='page-title' />").html(BlocksMessages.defaultPageTitle);
                $("head").append(title);
            }

            if (title.hasAttribute("property")) {
                retVal.push(SidebarUtils.addValueHtml(Sidebar, title, "Page title", "Enter a title for this page", false));
            }

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetPageTitle;
        },

    })).register(this.TAGS);

}]);