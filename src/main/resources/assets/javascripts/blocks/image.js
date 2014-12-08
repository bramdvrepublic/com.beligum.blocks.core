blocks.plugin("blocks.core.image", ["blocks.core.Admin", function(Admin) {
    var dialogContent = $('<h2>Select an image from the list</h2><div class="form-inline" role="form"><div class="form-group">' +
        '<label for="imagelabel" class="sr-only">Image: </label>'  +
        '<select class="form-control" id="imageselect">' +
        '<option value="/assets/images/navigo/activist-of-visser.jpg">activist 1</option>'+
        '<option value="/assets/images/navigo/activist_edit.jpg">activist 2</option>'+
        '<option value="/assets/images/navigo/kok.jpg">kok</option>'+
        '<option value="/assets/images/navigo/minister.jpg">minister</option>'+
        '<option value="/assets/images/navigo/wetenschapper.jpg">wetenschapper</option>'+
        '<option value="/assets/images/navigo/visser.jpg">visser</option>'+
        '</select></div></div>');


    Admin.register(
        {
            enabled: function(block) {
                return block.element.attr("typeof") == "image";
            },
            callback: function(block, content) {
                var imageElement = block.element.find("img");
                var imageSrc = content.find("#imageselect").val();
                imageElement.attr("src", imageSrc);
            },
            element: dialogContent
        }
    );

}]);