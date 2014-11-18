/*
* The menu that is shown in each block.
* with addButton you can add a button to the menu
* addButton takes an object with:
*   element: jquery element (the button). See bootstrap for now (<button class="btn">test</button>)
*   priority: sets the order for the buttons. smallest is most left
*
*   Menu is shwon when hoovering over a block
*   Menu is hidden while dragging
* */
// TODO refactor, put some things in config (element classes etc)
// TODO when showing menu set height block double of menu
blocks.plugin("blocks.core.BlockMenu", ["blocks.core.Broadcaster", function(Broadcaster) {

    // on hoover block show menu
    var menuElement = $('<div id="blocks-core-block-menu" class="block-menu btn-group-xs btn-group" style="z-index: 600"></div>');
    var buttons = [];
    var timeOutHandler = null;

    var showMenuElement = function(blockEvent) {
        clearTimeout(timeOutHandler);
        menuElement.hide();
        activeBlock = blockEvent.block.current;
        timeOutHandler = setTimeout(function() {
            menuElement.css("position", "absolute");
            menuElement.css("top", activeBlock.top + "px");
            // center menu in block
            var menuWidth = menuElement.width();
            var menuLeft = activeBlock.left + ((activeBlock.right - activeBlock.left) / 2) - (menuWidth / 2);
            if (menuLeft < 0) menuLeft = 0;
            menuElement.css("left", menuLeft + "px");
            menuElement.show();
        }, 400);
    };

    var hideMenuElement = function() {
        activeBlock = null;
        clearTimeout(timeOutHandler);
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
                Broadcaster.send(new Broadcaster.EVENTS.DO_NOT_ALLOW_DRAG())
            }
            retVal = true;
        } else {
            if (dragdropPrevented) {
                dragdropPrevented = false;
                Broadcaster.send(new Broadcaster.EVENTS.DO_ALLOW_DRAG())
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
                // TODO: does not yet work :)
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
                // prevent draggingOptions drop
            }
        }
    };

    this.addButton = function(button) {
        if (button.priority == null) button.priority = 0;
        var added = false;
        for (var i=0; i < buttons.length; i++) {
            var b = buttons[i];
            if (b.priority <= button.priority) {
                buttons.splice(i+1 ,0, button);
                b.element.before(button.element);
                added = true;
                break;
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

    Broadcaster.on(Broadcaster.EVENTS.HOOVER_ENTER_BLOCK, "blocks.core.BlockMenu", function (event) {
        showMenu(event.blockEvent)
    });
    Broadcaster.on(Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK, "blocks.core.BlockMenu", function (event) {
        // check if not active block
        showMenu(event.blockEvent)
    });
    Broadcaster.on(Broadcaster.EVENTS.HOOVER_OVER_BLOCK, "blocks.core.BlockMenu", function (event) {
        showMenu(event.blockEvent)
    });

    Broadcaster.on(Broadcaster.EVENTS.START_DRAG, "blocks.core.BlockMenu", function (event) {
        deactivate(event.blockEvent);
    });

    Broadcaster.on(Broadcaster.EVENTS.END_DRAG, "blocks.core.BlockMenu", function (event) {
        activate(event.blockEvent)
    });

}]);