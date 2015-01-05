blocks.plugin("blocks.core.video", ["blocks.core.Admin", function(Admin) {
    var dialogContent = $('<h3>Select a video: </h3><div class="form" >' +
        '<div class="form-group">' +
        '<label for="videoselect" class="sr-only">Video: </label>'  +
        '<select class="form-control" name="videoselect" id="videoselect">' +
        '<option value="None"> - </option>'+
        '<option value="//player.vimeo.com/video/58880979">Activisme 1</option>'+
        '<option value="//player.vimeo.com/video/97414641">Activisme 2</option>'+
        '</select>' +
        '</div>' +
        '<div class="form-group">' +
        '<label for="videourl" class="sr-only">Type embedded url: </label>'  +
        '<input type="text" placeholder="Type url for a video" id="videourl" name="videourl" />'  +
        '</div>' +
        '</div>');


    Admin.register(
        {
            enabled: function(element) {
                return element.attr("typeof") == "video";
            },
            callback: function(block, content) {
                var imageElement = block.element.find("iframe");
                var imageSrc = content.find("#videoselect").val();
                if (imageSrc == "None") {
                    imageSrc = content.find("#videourl").val();
                }
                if (imageSrc != null) {
                    imageElement.attr("src", imageSrc);

                }
            },
            element: dialogContent
        }
    );

}]);