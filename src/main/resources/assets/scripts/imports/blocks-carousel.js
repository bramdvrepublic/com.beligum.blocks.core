/**
 * Created by wouter on 18/06/15.
 */
base.plugin("blocks.edit.Carousel", ["constants.blocks.core", "blocks.core.Edit", "blocks.core.Sidebar", "blocks.core.SidebarUtils", "constants.blocks.core", function (Constants, Edit, Sidebar, SidebarUtils, Constants)
{
    var listGroup = null;

    this.focus = function (windowId, element, blockEvent)
    {
        var element = element.find(".carousel");

        listGroup = $('<div class="list-group" />');

        redraw(element);

        Sidebar.addUIForProperty(windowId, listGroup);
        Sidebar.addUIForProperty(windowId, addImageButton(element));

        //pauses all carousels while editing
        $('.carousel').each(function(){
            //this only works when data-interval="false" (otherwise the default timeout is activated when you click an arrow button)
            $(this).carousel('pause');
        });

        //this means we don't want children properties to create windows (but execute their logic, yes)
        return false;
    };

    this.blur = function (element)
    {
        var element = element.find(".carousel");

        //restart the cycling
        element.carousel('cycle');

        listGroup = null;
    };

    this.getWindowName = function ()
    {
        return "Caroussel";
    };

    var createImageEditBox = function (index, item, items, carousel)
    {
        var image = item.children("img");
        var caption = item.children(".carousel-caption");

        var listGroupItem = $('<div class="list-group-item clearfix" />');
        //don't start from zero
        var wrapper = $('<div></div>').appendTo(listGroupItem);
        var label = $('<div class="pull-left">'+(item.find('.title p').html())+'</div>').appendTo(wrapper);
        var buttons = $('<div class="btn-group pull-right" role="group"></div>').appendTo(wrapper);
        var editButton = $('<span class="btn btn-primary btn-xs"><i class="fa fa-fw fa-pencil"></i></span>').appendTo(buttons);

        var editBox = $('<div class="hidden" />').appendTo(listGroupItem);
        var labelEdit = $("<div></div>").appendTo(editBox);
        var closeButton = $('<span class="close pull-right"><i class="fa fa-times"></i></span>').appendTo(labelEdit);

        editBox.append(SidebarUtils.addValueAttribute(Sidebar, image, "Image", "Paste or type a link", "src", false, true, false));

        closeButton.click(function ()
        {
            editBox.addClass("hidden");
            wrapper.removeClass("hidden");
        });
        editButton.click(function ()
        {
            wrapper.addClass("hidden");
            editBox.removeClass("hidden");
        });

        // Do not add remove when there is only one image
        if (items.length > 1) {
            var deleteButton = $('<span class="btn btn-danger btn-xs"><i class="fa fa-fw fa-trash-o"></i></span>').appendTo(buttons);

            var deleteWrapper = $('<div class="hidden"></div>').appendTo(listGroupItem);
            var deleteLabel = $('<div class="pull-left">Are you sure?</div>').appendTo(deleteWrapper);
            var deleteGroup = $('<div class="btn-group pull-right" role="group"></div>').appendTo(deleteWrapper);
            var yesRemoveButton = $('<span class="btn btn-success btn-xs"><i class="fa fa-check"></i></span>').appendTo(deleteGroup);
            var noRemoveButton = $('<span class="btn btn-danger btn-xs"><i class="fa fa-times"></i></span>').appendTo(deleteGroup);

            deleteButton.click(function ()
            {
                deleteWrapper.removeClass("hidden");
                wrapper.addClass("hidden");
            });

            noRemoveButton.click(function ()
            {
                wrapper.removeClass("hidden");
                deleteWrapper.addClass("hidden");
            });

            yesRemoveButton.click(function ()
            {
                buttons.removeClass("hidden");
                deleteWrapper.addClass("hidden");
                listGroupItem.remove();
                item.remove();
                redrawIndicators(carousel);
                redraw(carousel);
            });
        }

        return listGroupItem
    };

    var addImageButton = function (carousel)
    {
        var items = carousel.find(".carousel-inner");
        var indicators = carousel.find(".carousel-indicators");
        var button = $('<button class="btn btn-primary btn-sm">Add image</button>');

        button.click(function ()
        {
            //Sync this with carousel.html
            var item = $('<div class="item" />').appendTo(items);
            var image = $('<img property="image" src="http://cdn.banquenationale.ca/cdnbnc/2013/06/ruisseau.jpg">').appendTo(item);
            var caption = $('<div class="carousel-caption"></div>').appendTo(item);
            var title = $('<div property="title" class="title"><p>Enter a title</p></div>').appendTo(caption);
            var description = $('<div property="description" class="description"><p>Enter a description</p></div>').appendTo(caption);

            redrawIndicators(carousel);
            redraw(carousel);
        });

        return button;
    };

    var redrawIndicators = function (carousel)
    {
        var id = carousel.attr("id");
        var indicators = carousel.find(".carousel-indicators");
        var items = carousel.find(".carousel-inner").children();
        indicators.empty();

        for (var i = 0; i < items.length; i++) {
            var indicator = $('<li data-target="#' + id + '" data-slide-to="' + i + '"></li>');
            if (i == 0) indicator.addClass("active");
            indicators.append(indicator);
        }
        carousel.find(".carousel-inner").children().removeClass("active");
        carousel.find(".carousel-inner").children().first().addClass("active");
    };

    var redraw = function (carousel)
    {
        var items = carousel.find(".item");
        listGroup.empty();
        for (var i = 0; i < items.length; i++) {
            var item = $(items[i]);
            listGroup.append(createImageEditBox(i, item, items, carousel));
        }
    };

    //init the carousels on the page
    $(document).ready(function() {
        $('.carousel').carousel({
            interval: Constants.CAROUSEL_DEFAULT_INTERVAL_MS,
            keyboard: false //messes with the UI
        });
    });

    Edit.registerByTag("BLOCKS-CAROUSEL", this);

}]);