/*
* Allows editiong of an image when youy click on it
* */
blocks.plugin("blocks.core.image", ["blocks.finder", "blocks.core.Edit", "blocks.core.Notification", "blocks.core.Broadcaster", function(Finder, Edit, Notification, Broadcaster) {
    var dialogContent = $('<div class="form-inline" role="form"><div class="form-group">' +
        '<label for="imagelabel" class="sr-only">Geef de url van een afbeelding: </label>'  +
        '<input type="text"  class="form-control"  placeholder="Geef een url" id="imageselect" />'+

        '</select></div></div>');



    var editImage = function(blockEvent) {
        var element = blockEvent.property.current.element;
        if (element.prop("tagName") == "IMG") {

            Finder.open({
                onSelect: function(file) {
                    element.attr("src", "/media/public/" + file);
                },
                onClose: function() {
                    Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);
                }
            });
        }
    };


    Edit.registerByTag("IMG", editImage);


}]);