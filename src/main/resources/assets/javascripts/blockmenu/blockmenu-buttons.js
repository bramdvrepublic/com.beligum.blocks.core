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
    var button = $('<div class="block-menu-item"><i class="glyphicon glyphicon-trash"></i> Delete</div>')
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

blocks.plugin("blocks.core.BlockMenu.zoom", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification", "blocks.core.DomManipulation", "blocks.core.Broadcaster", function(Menu, Layouter, Notification, DOM, Broadcaster) {
    var button = $('<div><i class="glyphicon glyphicon-trash"></i> Zoom</div>')
    Menu.addButton({
        element: button,
        priority: 105,
        enabled : function(block) {
            var retVal = false
            if (DOM.canLayout(block.element)) retVal = true;
            return retVal;
        }


    });



    button.on("click", function(event) {
        event.stopPropagation();
        var currentBlock = Menu.currentBlock();
        var lastParent = Broadcaster.layoutParentElement;


        var ol = $("<div />");
        ol.attr("style", "position: absolute; top: 0px; left: 0px; position: fixed; top: 0px; bottom: 0px; left: 0px; right: 0px; background-color: rgba(0,0,0,0.8); z-index: 9000");

        var oldElement = currentBlock.element;

        var oldStyle = null;
        if (oldElement.hasAttribute('style')) oldStyle = oldElement.attr('style');
        oldElement.css("visibility", "hidden");
        var clonedElement = oldElement.clone();
        clonedElement.attr("style", "position: absolute; z-index: 9001; left: " + currentBlock.left +"px; top: "+ currentBlock.top+"px; width:" + (currentBlock.right - currentBlock.left) + "px");

        $("body").append(ol).append(clonedElement);
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        Broadcaster.setLayoutParent(clonedElement);
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);

        ol.on("click", function () {
            ol.remove();
            clonedElement.remove()
            oldElement.css("visibility", "visible");
            if (oldStyle != null) {
                clonedElement.attr("style", oldStyle);
            } else {
                clonedElement.removeAttr("style");
            }
            oldElement.replaceWith(clonedElement);
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
            Broadcaster.setLayoutParent(lastParent);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);


        });
    })
}]);
