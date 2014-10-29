blocks.plugin("blocks.core.BlockMenu", ["blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.Constants", function(Broadcaster, Mouse, Constants) {

    // on hoover block show menu
    var menuElement = $('<div id="blocks-core-block-menu" class="block-menu btn-group"></div>');
    var buttons = [];
    var showMenuElement = function(blockEvent) {
        activeBlock = blockEvent.block.current;
        menuElement.css("position", "absolute");
        menuElement.css("top", activeBlock.top + "px");
        menuElement.css("left", activeBlock.left + "px");
        menuElement.show();
    };

    var hideMenuElement = function() {
        activeBlock = null;
        menuElement.hide();
    };

    $(document).ready(function() {
        $("body").append(menuElement);
        hideMenuElement();
    })

    var mouseOverMenu = function(event) {
        var retVal = false;
        var offset = menuElement.offset();
        if (offset.top < event.pageY && offset.top + menuElement.height() > event.pageY && offset.left < event.pageX && offset.left + menuElement.width() > event.pageX) {
            if (!dragdropPrevented) {
                dragdropPrevented = true;
                Broadcaster.send(Mouse.config.EVENT.DO_NOT_ALLOW_DRAG)
            }
            retVal = true;
        } else {
            if (dragdropPrevented) {
                dragdropPrevented = false;
                Broadcaster.send(Mouse.config.EVENT.ALLOW_DRAG)
            }
        }
        return retVal;
    };

    var showMenu = function(blockEvent) {
        if (blockEvent.block.current != activeBlock) {
            // We are in a different block
            // No block = remove menu
            if (blockEvent.block.current == null) {
                // if mouse is still in current menu, then do not remove it.
                if (!mouseOverMenu(blockEvent.event)) {
                    hideMenuElement();
                }
            } else {
                // show menu at correct place
                showMenuElement(blockEvent);
            }
        } else {
            // nothing changed, check if hoover over button
            if (mouseOverMenu(blockEvent.event)) {
                // prevent drag drop
            }
        }
    };

    this.addButton = function(button) {
        if (button.priority == null) button.priority = 0;
        var added = false;
        for (var i=0; i < buttons.length; i++) {
            if (buttons[i].priority < button.priority) {
                    buttons.splice(i,0, button);
                    menuElement.append(button.element);
                    menuElement.append(buttons[i]);
            }
        }
        if (!added) {
            menuElement.append(button.element);
            buttons.push(button);
        }
    };

    this.currentBlock = function() {
        return activeBlock;
    }



    var activate= function(blockEvent) {
       active = true;
    };

    var deactivate = function(blockEvent) {
       active = false;
       hideMenuElement(blockEvent);
    };


    var active = true;
    var activeBlock = null;
    var dragdropPrevented = false;

    Broadcaster.on(Mouse.config.EVENT.HOOVER_ENTER_BLOCK, function (event) {
        showMenu(event)
    });
    Broadcaster.on(Mouse.config.EVENT.HOOVER_LEAVE_BLOCK, function (event) {
        // check if not active block
        showMenu(event)
    });
    Broadcaster.on(Mouse.config.EVENT.HOOVER_OVER_BLOCK, function (event) {
        showMenu(event)
    });

    Broadcaster.on(Mouse.config.EVENT.START_DRAG, function (event) {
        deactivate(event);
    });

    Broadcaster.on(Mouse.config.EVENT.END_DRAG, function (event) {
        activate(event)
    });

}]);