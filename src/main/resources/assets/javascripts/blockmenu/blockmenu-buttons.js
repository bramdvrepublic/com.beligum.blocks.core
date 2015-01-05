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
        priority: 105,
        enabled : function(block) {
            var total = block.getTotalBlocks();
            Logger.debug("Total: " + total);
            if (total == 1) {
                return false;
            } else {
                return true;
            }
        }
    });



    button.on("click", function(event) {
        event.stopPropagation();
        var currentBlock = Menu.currentBlock();
        Notification.alert("Delete", "<p>You will now delete this block!</p>", function() {
            Layouter.removeBlock(currentBlock);
        });
    })
}]);
