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
    var hoverOverMenu = false;
    var menuElement = $('<div class="block-menu"></div>');
    var menuHandle = $('<div class="block-menu-handle"><i class="glyphicon glyphicon-cog"></div>')
    menuElement.append(menuHandle);

    $(document).on("mousedown", ".block-menu-handle", function(event) {
        if (!menuElement.hasClass("open")) {
            menuElement.addClass("open");
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);

            // Handler to close the manu on click
            $(document).on("mousedown.remove_block_menu", function (event) {
                var target = $(event.target);
                if (!target.hasClass("block-menu-item") && !target.closest("block-menu-item").length > 0) {

                    menuElement.removeClass("open");
                    $(document).off("mousedown.remove_block_menu");
                    BlockMenu.hideMenu();
                    Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
                }




            });
        }

        event.preventDefault();
    });



    menuElement.on("mouseenter", function() {
        hoverOverMenu = true;
    });

    menuElement.on("mouseleave", function() {
        hoverOverMenu = false;
        if (activeBlock == null) {
            Broadcaster.resetHover();
        }
    });

    this.mouseOverMenu = function() {
        return hoverOverMenu;
    };



    var buttons = [];
    var activeBlock = null;


    this.createMenu = function() {
        $("body").append(menuElement);
        BlockMenu.hideMenu();
    }

    this.removeMenu = function() {
        menuElement.remove();
    }

    this.hideMenu = function() {
        activeBlock = null;
        menuElement.removeClass("open");
        menuElement.hide();
    };


    this.showMenu = function(block) {
        if (block != null) {
            visible = true;
            menuElement.hide();
            // sets the block this menu is currently bound to
            activeBlock = block;

            for (var i = 0; i < buttons.length; i++) {
                if (buttons[i].enabled != null && !buttons[i].enabled(activeBlock)) {
                    $(buttons[i].element).addClass("disabled");
                } else {
                    $(buttons[i].element).removeClass("disabled");
                }
            }
            menuElement.css("position", "absolute");
            menuElement.css("top", activeBlock.element.offset().top + "px");
            // put menu in upper left corner of block
            var menuWidth = menuElement.width();
            menuElement.css("left", (activeBlock.right - menuWidth) + "px");
            menuElement.css("z-index", Overlay.maxIndex() + 1);
            menuElement.show();
        } else {
            menuElement.hide();
        }
    };


    /*
    * Add button to menu
    *
    * */
    this.addButton = function(button) {
        button.element.addClass("block-menu-item");
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
    };


}]);