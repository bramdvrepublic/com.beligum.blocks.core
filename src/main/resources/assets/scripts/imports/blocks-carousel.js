/**
 * Created by wouter on 18/06/15.
 */
base.plugin("blocks.edit.Carousel", ["base.core.Class", "blocks.edit.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.SidebarUtils", "constants.blocks.core", function (Class, Widget, BlocksConstants, BlocksMessages, Sidebar, SidebarUtils, Constants)
{
    var CarouselWidget = this;
    var TAGS = ["BLOCKS-CAROUSEL"];

    (this.Class = Class.create(Widget.Class, {

        //-----VARIABLES-----
        _listGroup: null,

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            CarouselWidget.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
            //init the carousels on the page
            $(document).ready(function ()
            {
                $('.carousel').carousel({
                    interval: BlocksConstants.CAROUSEL_DEFAULT_INTERVAL_MS,
                    keyboard: false //messes with the UI
                });
            });
        },
        focus: function (block, element, hotspot, event)
        {
            CarouselWidget.Class.Super.prototype.focus.call(this);
        },
        blur: function (block, element)
        {
            CarouselWidget.Class.Super.prototype.blur.call(this);

            var element = element.find(".carousel");

            //restart the cycling
            element.carousel('cycle');

            this._listGroup = null;
        },
        getOptionConfigs: function (block, element)
        {
            var retVal = [];

            var element = element.find(".carousel");

            this._listGroup = $('<div class="list-group" />');

            this._redraw(element);

            retVal.push(this._listGroup);
            retVal.push(this._addImageButton(element));

            //pauses all carousels while editing
            $('.carousel').each(function ()
            {
                //this only works when data-interval="false" (otherwise the default timeout is activated when you click an arrow button)
                $(this).carousel('pause');
            });

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetCarouselTitle;
        },

        //-----PRIVATE METHODS-----
        _redraw: function (carousel)
        {
            var items = carousel.find(".item");
            this._listGroup.empty();
            for (var i = 0; i < items.length; i++) {
                var item = $(items[i]);
                this._listGroup.append(this._createImageEditBox(i, item, items, carousel));
            }
        },

        _createImageEditBox: function (index, item, items, carousel)
        {
            var image = item.children("img");
            var caption = item.children(".carousel-caption");

            var listGroupItem = $('<div class="list-group-item clearfix" />');
            var wrapper = $('<div></div>').appendTo(listGroupItem);
            var label = $('<div class="pull-left">' + (item.find('.title p').html()) + '</div>').appendTo(wrapper);
            var buttons = $('<div class="btn-group pull-right" role="group"></div>').appendTo(wrapper);

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
                    this._redrawIndicators(carousel);
                    this._redraw(carousel);
                });
            }

            return listGroupItem
        },

        _addImageButton: function (carousel)
        {
            var items = carousel.find(".carousel-inner");
            var indicators = carousel.find(".carousel-indicators");
            var button = $('<button class="btn btn-primary btn-sm">Add slide</button>');

            button.click(function ()
            {
                //Sync this with carousel.html
                var item = $('<div class="item" />').appendTo(items);
                var image = $('<img property="image" src="/assets/images/blocks/placeholder_slide_1.jpg">').appendTo(item);
                var caption = $('<div class="carousel-caption"></div>').appendTo(item);
                var title = $('<div property="title" class="title"><p>Enter a title</p></div>').appendTo(caption);
                var description = $('<div property="description" class="description"><p>Enter a description</p></div>').appendTo(caption);

                this._redrawIndicators(carousel);
                this._redraw(carousel);
            });

            return button;
        },

        _redrawIndicators: function (carousel)
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
        }

    })).register(TAGS);

}]);