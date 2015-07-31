/**
 * Created by wouter on 18/06/15.
 */
base.plugin("blocks.edit.Carousel", ["constants.blocks.common", "blocks.core.Edit", "blocks.core.Sidebar", "blocks.core.SidebarUtils",  function (Constants, Edit, Sidebar, SidebarUtils)
{
    var listGroup = null;

    this.focus = function(windowId, propertyElement, blockEvent) {
        var element = blockEvent.block.current.element.find(".carousel");

        listGroup = $('<div class="list-group" />');

        redraw(element);

        Sidebar.addUIForProperty(windowId, addImageButton(element));
        Sidebar.addUIForProperty(windowId, listGroup);
    };

    this.blur = function() {
        listGroup = null;
    };

    this.getWindowName = function() {
        return "Caroussel";
    };

    var createImageEditBox = function(item, items, carousel) {

        var image = item.children("img");
        var caption = item.children(".carousel-caption");

        var listGroupItem = $('<div class="list-group-item" />');
        var label = $("<div>Carousel image</div>");
        var editButton = $('<span class="label btn btn-primary pull-right"><i class="fa fa-fw fa-pencil"></i></span>');
        var deleteButton = $('<span class="label btn btn-danger pull-right"><i class="fa fa-fw fa-trash-o"></i></span>');
        label.append(editButton)

        var labelEdit = $("<div></div>");
        var closeButton = $('<span class="label btn pull-right"><i class="fa fa-times"></i></span>');
        labelEdit.append(closeButton);
        var editBox = $('<div class="hidden" />');

        editBox.append(labelEdit);
        editBox.append(SidebarUtils.addValueAttribute(Sidebar, image, "Image url", "Paste or type a link", "src", false, true, false));
        editBox.append(SidebarUtils.addValueHtml(Sidebar, caption, "Image caption", "Paste or type an image caption", false));

        closeButton.click(function() {
            editBox.addClass("hidden");
            label.removeClass("hidden");
        });

        editButton.click(function() {
            label.addClass("hidden");
            editBox.removeClass("hidden");
        });

        listGroupItem.append(label);

        // Do not add remove when there is only one image
        if (items.length > 1) {
            label.append(deleteButton);
            var labelRemove = $('<div class="hidden">Are you sure you want to remove the image</div>');
            var yesRemoveButton = $('<span class="label btn btn-success pull-right"><i class="fa fa-check"></i></span>');
            var noRemoveButton = $('<span class="label btn btn-danger pull-right"><i class="fa fa-times"></i></span>');
            labelRemove.append(yesRemoveButton).append(noRemoveButton);

            deleteButton.click(function () {
                labelRemove.removeClass("hidden");
                label.addClass("hidden");
            });

            noRemoveButton.click(function () {
                label.removeClass("hidden");
                labelRemove.addClass("hidden");
            });

            yesRemoveButton.click(function () {
                label.removeClass("hidden");
                labelRemove.addClass("hidden");
                listGroupItem.remove();
                item.remove();
                redrawIndicators(carousel);
                redraw(carousel);
            });

            listGroupItem.append(labelRemove);

        }

        listGroupItem.append(editBox);

        return listGroupItem

    };

    var addImageButton = function(carousel) {
        var items = carousel.find(".carousel-inner");
        var indicators = carousel.find(".carousel-indicators");
        var button = $('<button class="btn btn-primary">Add image</button>');

        button.click(function() {
            var image = $('<img property="image" src="http://cdn.banquenationale.ca/cdnbnc/2013/06/ruisseau.jpg" >');
            var caption = $('<div property="image-caption" class="carousel-caption" />');
            var item = $('<div class="item" />');

            item.append(image).append(caption);
            items.append(item);
            redrawIndicators(carousel);
            redraw(carousel);

        });

        return button;
    };

    var redrawIndicators = function(carousel) {
        var id = carousel.attr("id");
        var indicators = carousel.find(".carousel-indicators");
        var items = carousel.find(".carousel-inner").children();
        indicators.empty();

        for (var i=0; i < items.length; i++) {
            var indicator = $('<li data-target="#'+id+'" data-slide-to="'+ i +'"></li>');
            if (i==0) indicator.addClass("active");
            indicators.append(indicator);
        }
        carousel.find(".carousel-inner").children().removeClass("active");
        carousel.find(".carousel-inner").children().first().addClass("active");
    };

    var redraw = function(carousel) {
        var items = carousel.find(".item");
        listGroup.empty();
        for (var i=0; i<items.length; i++) {
            var item = $(items[i]);
            listGroup.append(createImageEditBox(item, items, carousel));
        }
    };

    Edit.registerByTag("BOOTSTRAP-CAROUSEL", this);

}]);