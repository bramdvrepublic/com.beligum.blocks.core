blocks.plugin("blocks.core.image", ["blocks.core.Admin", function(Admin) {
    var dialogContent = $('<div class="form-inline" role="form"><div class="form-group">' +
        '<label for="imagelabel" class="sr-only">Geef de url van een afbeelding: </label>'  +
        '<input type="text" placeholder="Geef een url" id="imageselect" />'+

        '</select></div></div>');


    Admin.register(
        {
            enabled: function(element) {
                return element.prop("tagName") == "IMG";
            },
            callback: function(block, element, content) {

                var imageSrc = content.find("#imageselect").val();

                element.attr("src", imageSrc);
            },
            element: dialogContent,
            title: "Select an image"
        }
    );

}]);