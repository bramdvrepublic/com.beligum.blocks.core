/*
*
* Buttons for a blockMenu.
* Add a button by calling addButton (of a blockMenu) with an object () the button
*
* Button object:
*   - element: jQuery element that is the button, normally a bootstrap button (with class btn)
*   - priority: sets the priority of the button. Button with higher priority will be placed more to the left (front)
*
* */
// TODO add button to layout inside a parsedContent block
// block-parsedContent class


blocks.plugin("blocks.core.BlockMenu.delete", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification", function(Menu, Layouter, Notification) {
    var button = $('<div type="button" class="btn btn-default"><i class="glyphicon glyphicon-trash"></i></div>')
    Menu.addButton({
        element: button,
        priority: 105
    })

    button.on("click", function(event) {
        var currentBlock = Menu.currentBlock();
        Notification.alert("Delete", "<p>You will now delete this block!</p>", function() {
            Layouter.removeBlock(currentBlock);
        });
    })
}]);

//blocks.plugin("blocks.core.BlockMenu.edit", ["blocks.core.BlockMenu", "blocks.core.Broadcaster", function(Menu, Broadcaster) {


    // TODO: On resize we should stop the editing
//    button.on("click", function(event) {
//        // go into edit mode
//        var blockElement = Menu.currentBlock().element;
//        var blockBackground =  $("<div />");
//        blockBackground.css("background-color", "#FFFFFF");
//        blockBackground.css("position", "absolute");
//        blockBackground.css("top", Menu.currentBlock().top +"px");
//        blockBackground.css("left", Menu.currentBlock().left +"px");
//        blockBackground.css("width", (Menu.currentBlock().right - Menu.currentBlock().left) +"px");
//        blockBackground.css("height", (Menu.currentBlock().bottom - Menu.currentBlock().top) +"px");
//        blockBackground.css("z-index", 2000);
//
//
//        var overlayElement = $("<div />");
//        overlayElement.addClass("blocks-edit-overlay");
//        overlayElement.css("position", "absolute");
//        overlayElement.css("opacity", "0.9");
//        overlayElement.css("background-color", "#424242");
//        overlayElement.css("top", "0px");
//        overlayElement.css("left", "0px");
//        overlayElement.css("width", $(document).width() + "px");
//        overlayElement.css("height", $(document).height() + "px");
//        overlayElement.css("z-index", "1000");
//
//        overlayElement.click(function() {
//            blockBackground.remove();
//            overlayElement.remove();
//            blockElement.css("position", "");
//            blockElement.css("z-index", "");
//            if (blockElement.children().length > 0) {
//                var p = blockElement.children().first();
//                p.removeAttr("contenteditable", "");
//                // use native javascript focus. otherwise focus
//                // won't work on contenteditable element
//                // TODO: try http://stackoverflow.com/questions/2388164/set-focus-on-div-contenteditable-element#16863913
//                p.get(0).focus();
//            } else {
//                blockElement.removeAttr("contenteditable", "");
//            }
//            Broadcaster.send(new Broadcaster.EVENTS.ACTIVATE_MOUSE());
//        });
//
//        blockElement.css("position", "relative");
//        blockElement.css("z-index", "2001");
//        if (blockElement.children().length > 0) {
//            var p = blockElement.children().first();
//            p.attr("contenteditable", "true");
//            //p.focus();
//        } else {
//            blockElement.attr("contenteditable", "true");
//        }
//        Broadcaster.send(new Broadcaster.EVENTS.DEACTIVATE_MOUSE());
//
//
//        $("body").append(overlayElement);
//        $("body").append(blockBackground);

//    })
//}]);