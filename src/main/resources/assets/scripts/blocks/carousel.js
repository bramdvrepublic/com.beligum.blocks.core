/**
 * Created by wouter on 18/06/15.
 */
base.plugin("blocks.edit.Carousel", ["constants.blocks.common", "blocks.core.Edit", "blocks.core.Sidebar", "blocks.core.Plugin-Utils",  function (Constants, Edit, Sidebar, Plugin)
{

    this.focus = function(propertyElement, blockEvent) {
        var element = blockEvent.block.current.element.find(".carousel");
        var contentID = Sidebar.createWindow(Constants.CONTENT, blockEvent.block.current.element, "Carousel");
        var styleID = Sidebar.createWindow(Constants.STYLE, blockEvent.block.current.element, "Carousel");

        var items = element.find(".item");
        var listGroup = $('<div class="list-group" />')
        var index = 0;
        items.each(function() {
            var item = $(this);
            var image = item.children("img");
            var caption = item.children(".carousel-caption");
            listGroup.append(createImageEditBox(index, element, image, caption));
        });
        Sidebar.addUIForProperty(contentID, element, addImageButton(listGroup, element));
        Sidebar.addUIForProperty(contentID, element, listGroup);

        Sidebar.addValueAttribute(styleID, element, "Slide interval in ms:", "data-interval", false, true);
        Sidebar.addUniqueAttributeValue(styleID, element, "Pause on hoover:", "data-pause", [{name: "Yes", value: "true"}, {name: "No", value: "false"}]);
        Sidebar.addUniqueAttributeValue(styleID, element, "Auto cycle:", "data-wrap", [{name: "Yes", value: "true"}, {name: "No", value: "false"}]);

    };

    this.blur = function() {

    };


    var createImageEditBox = function(index, carousel, image, caption) {

        var listGroupItem = $('<div class="list-group-item" />');
        var label = $("<div>Carousel image</div>");
        var editButton = $('<span class="label btn btn-primary pull-right"><i class="fa fa-pencil"></i></span>');
        var deleteButton = $('<span class="label btn btn-danger pull-right"><i class="fa fa-trash-o"></i></span>');
        label.append(editButton).append(deleteButton);

        var labelEdit = $("<div></div>");
        var closeButton = $('<span class="label btn pull-right"><i class="fa fa-times"></i></span>');
        labelEdit.append(closeButton);
        var editBox = $('<div class="hidden" />');

        var labelRemove = $('<div class="hidden">Are you sure you want to remove the image</div>');
        var yesButton = $('<span class="label btn btn-success pull-right"><i class="fa fa-check"></i></span>');
        var noButton = $('<span class="label btn btn-danger pull-right"><i class="fa fa-times"></i></span>');
        labelRemove.append(yesButton).append(noButton);

        editBox.append(labelEdit);
        editBox.append(Plugin.addValueAttribute(image, "image url", "src", true));
        editBox.append(Plugin.addValueHtml(caption, "image caption", false));

        closeButton.click(function() {
            editBox.addClass("hidden");
            label.removeClass("hidden");
        });

        editButton.click(function() {
            label.addClass("hidden");
            editBox.removeClass("hidden");
        });

        deleteButton.click(function() {
            labelRemove.removeClass("hidden");
            label.addClass("hidden");
        });

        noButton.click(function() {
            label.removeClass("hidden");
            labelRemove.addClass("hidden");
        });

        yesButton.click(function() {
            label.removeClass("hidden");
            labelRemove.addClass("hidden");
            listGroupItem.remove();
            var items = carousel.find(".carousel-inner").children();
            var indicators = carousel.find(".carousel-indicators").children();
            $(items[index]).remove();
            $(indicators[index]).remove();

        });

        listGroupItem.append(label);
        listGroupItem.append(labelRemove);
        listGroupItem.append(editBox);

        return listGroupItem

    };

    var addImageButton = function(listGroup, carousel) {
        var items = carousel.find(".carousel-inner");
        var indicators = carousel.find(".carousel-indicators");
        var button = $('<button class="btn btn-primary">Add image</button>');

        button.click(function() {
            var image = $('<img property="image" src="http://cdn.banquenationale.ca/cdnbnc/2013/06/ruisseau.jpg" >');
            var caption = $('<div property="image-caption" class="carousel-caption" />');
            var item = $('<div class="item" />');
            item.append(image).append(caption);
            items.append(item);
            var id = carousel.attr("id");
            var nr = 0;
            var last = indicators.children().last();
            if (last.length == 1) {
                nr = parseInt(last.attr("data-slide-to"));
            }
            indicators.append($('<li data-target="#'+id+'" data-slide-to="'+ nr +'"></li>'));

            listGroup.append(createImageEditBox(indicators.children().length, carousel, image, caption));

        });



        return button;
    };

    Edit.registerByTag("BOOTSTRAP-CAROUSEL", this);


}]);