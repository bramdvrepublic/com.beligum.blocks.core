blocks.plugin("blocks.core.image", ["blocks.core.Edit", "blocks.core.Notification", "blocks.core.Broadcaster", function(Edit, Notification, Broadcaster) {
    var dialogContent = $('<div class="form-inline" role="form"><div class="form-group">' +
        '<label for="imagelabel" class="sr-only">Geef de url van een afbeelding: </label>'  +
        '<input type="text"  class="form-control"  placeholder="Geef een url" id="imageselect" />'+

        '</select></div></div>');



    var editImage = function(blockEvent) {
        var element = blockEvent.property.current.element;
        if (element.prop("tagName") == "IMG") {
            Notification.dialog("Change image", dialogContent.html(), function(body) {
                var imageSrc= $(body).find("#imageselect").val();
                element.attr("src", imageSrc);
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            }, function() {
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            })
        }


    };


    Edit.registerByTag("IMG", editImage);


}]);