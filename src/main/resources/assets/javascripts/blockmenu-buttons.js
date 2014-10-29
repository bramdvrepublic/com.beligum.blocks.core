blocks.plugin("blocks.core.BlockMenu.new", ["blocks.core.BlockMenu", "blocks.core.Layouter",  function(Menu, Layouter) {
    var button = $('<div type="button" class="btn btn-default"><i class="glyphicon glyphicon-asterisk"></i></div>')
    Menu.addButton({
        element: button,
        priority: 100
    })

    var newBlock = $('<div class="block"></div>');

    button.on("click", function(event) {
        Layouter.addNewBlockAtLocation($('<div class="block green"></div>'), Menu.currentBlock().element);
    })
}]);


blocks.plugin("blocks.core.BlockMenu.delete", ["blocks.core.BlockMenu", "blocks.core.Layouter", function(Menu, Layouter) {
    var button = $('<div type="button" class="btn btn-default"><i class="glyphicon glyphicon-trash"></i></div>')
    Menu.addButton({
        element: button,
        priority: 100
    })

    var newBlock = $('<div class="block"></div>');

    button.on("click", function(event) {
        Layouter.removeBlock(Menu.currentBlock());
    })
}]);

blocks.plugin("blocks.core.BlockMenu.edit", ["blocks.core.BlockMenu", "blocks.core.Broadcaster", function(Menu, Broadcaster) {
    var button = $('<div type="button" class="btn btn-default"><i class="glyphicon glyphicon-pencil"></i></div>')
    Menu.addButton({
        element: button,
        priority: 100
    })


    button.on("click", function(event) {
        // go into edit mode
        var blockElement = Menu.currentBlock().element;
        var overlayElement = $("<div />");
        overlayElement.addClass("blocks-edit-overlay");
        overlayElement.css("position", "absolute");
        overlayElement.css("opacity", "0.9");
        overlayElement.css("background-color", "#424242");
        overlayElement.css("top", "0px");
        overlayElement.css("left", "0px");
        overlayElement.css("width", $(document).width() + "px");
        overlayElement.css("height", $(document).height() + "px");
        overlayElement.css("z-index", "1000");
        overlayElement.click(function() {
            overlayElement.remove();
            blockElement.css("position", "");
            blockElement.css("z-index", "");
            blockElement.removeAttr("contenteditable", "");
            Broadcaster.send("activateMouse");
        });

        blockElement.css("position", "relative");
        blockElement.css("z-index", "2001");
        blockElement.attr("contenteditable", "true");
        Broadcaster.send("deactivateMouse");


        $("body").append(overlayElement);

    })
}]);