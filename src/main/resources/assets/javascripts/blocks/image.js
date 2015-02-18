blocks.plugin("blocks.core.image", ["blocks.finder", "blocks.core.Edit", "blocks.core.Notification", "blocks.core.Broadcaster", function(Finder, Edit, Notification, Broadcaster) {
    var dialogContent = $('<div class="form-inline" role="form"><div class="form-group">' +
        '<label for="imagelabel" class="sr-only">Geef de url van een afbeelding: </label>'  +
        '<input type="text"  class="form-control"  placeholder="Geef een url" id="imageselect" />'+

        '</select></div></div>');



    var editImage = function(blockEvent) {
        var element = blockEvent.property.current.element;
        if (element.prop("tagName") == "IMG") {

            Finder.open({callback: function(file) {
                element.attr("src", "/media/public/" + file);
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            }});
        }


    };


    Edit.registerByTag("IMG", editImage);


}]);