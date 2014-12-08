/*
* The menu that is shown in each block.
* with addButton you can add a button to the menu
* addButton takes an object with:
*   element: jquery element (the button). See bootstrap for now (<button class="btn">test</button>)
*   priority: sets the order for the buttons. smallest is most left
*   visible: if null, button is always shown, otherwise function that takes
*           block as argument and returns true/false if this button should
*           be shown (for this block)
*
*   Menu is shwon when hoovering over a block
*   Menu is hidden while dragging
* */
// TODO refactor, put some things in config (element classes etc)
// TODO when showing menu set height block double of menu
blocks.plugin("blocks.core.BlockMenu", ["blocks.core.Broadcaster", "blocks.core.Overlay", function(Broadcaster, Overlay) {
    var BlockMenu = this;
    // on hoover block show menu
    var menuElement = $('<div id="blocks-core-block-menu" class="block-menu btn-group-xs btn-group" style="z-index: 600"></div>');
    var buttons = [];
    var activeBlock = null;
    var hoverOverMenu = false;

    menuElement.on("mouseenter", function() {
        hoverOverMenu = true;
    });

    menuElement.on("mouseleave", function() {
        hoverOverMenu = false;
    });

    this.mouseOverMenu = function() {
        return hoverOverMenu;
    }

    this.showMenuElement = function(blockEvent) {
        menuElement.hide();
        activeBlock = blockEvent.block.current;
        for (var i = 0; i < buttons.length; i++) {
            if (buttons[i].enabled != null && !buttons[i].enabled(activeBlock)) {
                $(buttons[i].element).addClass("disabled");
            } else {
                $(buttons[i].element).removeClass("disabled");
            }
        }
        menuElement.css("position", "absolute");
        menuElement.css("top", activeBlock.top + "px");
        // center menu in block
        var menuWidth = menuElement.width();
        var menuLeft = activeBlock.left + ((activeBlock.right - activeBlock.left) / 2) - (menuWidth / 2);
        if (menuLeft < 0) menuLeft = 0;
        menuElement.css("left", menuLeft + "px");
        menuElement.css("z-index", Overlay.maxIndex() + 1);
        menuElement.show();

    };

    this.hideMenuElement = function() {
        activeBlock = null;
        menuElement.hide();
    };

    $(document).ready(function() {
        $("body").append(menuElement);
        BlockMenu.hideMenuElement();
    })
//
//    var mouseOverMenu = function(event) {
//        var retVal = false;
//        var offset = menuElement.offset();
//        if (offset.top < event.pageY && offset.top + menuElement.height() > event.pageY && offset.left < event.pageX && offset.left + menuElement.width() > event.pageX) {
//            if (!dragdropPrevented) {
//                dragdropPrevented = true;
//                Broadcaster.send(new Broadcaster.EVENTS.DO_NOT_ALLOW_DRAG())
//            }
//            retVal = true;
//        } else {
//            if (dragdropPrevented) {
//                dragdropPrevented = false;
//                Broadcaster.send(new Broadcaster.EVENTS.DO_ALLOW_DRAG())
//            }
//        }
//        return retVal;
//    };
//
//    var showMenu = function(blockEvent) {
//        if (blockEvent.block.current != activeBlock) {
//            // We are in a different block
//            // No block = remove menu
//            if (blockEvent.block.current == null) {
//                // if mouse is still in current menu, then do not remove it.
//                // TODO: does not yet work. Or does it?
//                if (!mouseOverMenu(blockEvent.event)) {
//                    hideMenuElement();
//                }
//            } else {
//                // show menu at correct place
//                showMenuElement(blockEvent);
//            }
//        } else {
//            // nothing changed, check if hoover over button
//            if (mouseOverMenu(blockEvent.event)) {
//                // prevent draggingOptions drop
//            }
//        }
//    };


    /*
    * Add button to menu
    *
    * */
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



//    var activate= function(blockEvent) {
//       active = true;
//        showMenuElement(blockEvent)
//    };
//
//    var deactivate = function(blockEvent) {
//       active = false;
//       hideMenuElement(blockEvent);
//    };
//
//
//    var active = true;
//    var activeBlock = null;
    var dragdropPrevented = false;

//    Broadcaster.on(Broadcaster.EVENTS.HOOVER_ENTER_BLOCK, "blocks.core.BlockMenu", function (event) {
//        showMenu(event.blockEvent)
//    });
//    Broadcaster.on(Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK, "blocks.core.BlockMenu", function (event) {
//        // check if not active block
//        showMenu(event.blockEvent)
//    });
//    Broadcaster.on(Broadcaster.EVENTS.HOOVER_OVER_BLOCK, "blocks.core.BlockMenu", function (event) {
//        showMenu(event.blockEvent)
//    });
//
//    Broadcaster.on(Broadcaster.EVENTS.START_DRAG, "blocks.core.BlockMenu", function (event) {
//        deactivate(event.blockEvent);
//    });
//
//    Broadcaster.on(Broadcaster.EVENTS.END_DRAG, "blocks.core.BlockMenu", function (event) {
//        activate(event.blockEvent)
//    });

}]);