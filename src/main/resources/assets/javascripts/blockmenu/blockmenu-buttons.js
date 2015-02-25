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


blocks.plugin("blocks.core.BlockMenu.delete", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification",  function(Menu, Layouter, Notification) {
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
        },
        action: function(event) {
            event.stopPropagation();
            var currentBlock = Menu.currentBlock();
            BootstrapDialog.confirm({
                title: 'WARNING',
                message: 'Warning! Drop your banana?',
                type: BootstrapDialog.TYPE_WARNING, // <-- Default value is BootstrapDialog.TYPE_PRIMARY
                closable: true, // <-- Default value is false
                draggable: true, // <-- Default value is false
                btnCancelLabel: 'Do not drop it!', // <-- Default value is 'Cancel',
                btnOKLabel: 'Drop it!', // <-- Default value is 'OK',
                btnOKClass: 'btn-warning', // <-- If you didn't specify it, dialog type will be used,
                callback: function(result) {
                    // result will be true if button was click, while it will be false if users close the dialog directly.
                    if(result) {
                        alert('Yup.');
                    }else {
                        alert('Nope.');
                    }
                }
            })
        }
    });

}]);



//blocks.plugin("blocks.core.BlockMenu.zoom", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification", "blocks.core.DomManipulation", "blocks.core.Broadcaster", "blocks.core.Overlay", function(Menu, Layouter, Notification, DOM, Broadcaster, Overlay) {
//    var button = $('<div><i class="glyphicon glyphicon-trash"></i> Zoom</div>')
//    Menu.addButton({
//        element: button,
//        priority: 105,
//        enabled : function(block) {
//            var retVal = false
//            if (DOM.canLayout(block.element)) retVal = true;
//            return retVal;
//        }
//    });
//
//
//
//    button.on("click", function(event) {
//        event.stopPropagation();
//        var currentBlock = Menu.currentBlock();
//
//        var clonedElement = Overlay.overlayForElement(currentBlock.element, function() {
//            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
//            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, lastParent);
//            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
//        });
//
//        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
//        Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, clonedElement);
//        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
//
//
//    })
//}]);
