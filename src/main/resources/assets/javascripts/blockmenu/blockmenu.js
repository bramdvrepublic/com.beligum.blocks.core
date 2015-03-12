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
/*
* This is the menu that is showed for every block
*
* */

blocks.plugin("blocks.core.BlockMenu", ["blocks.core.Broadcaster", "blocks.core.Constants", function(Broadcaster, Constants) {

    /*
    * Init Menu in html
    * */
    var BlockMenu = this;
    var hoverOverMenu = false;
    var menuElement = $('<div class="block-menu"></div>');
    var menuHandle = $('<div class="block-menu-handle"><i class="glyphicon glyphicon-cog"></div>')
    menuElement.append(menuHandle);

    /*
    * Catch mouse down on the menu. This opens and initializes the menu
    * */
    $(document).on("mousedown", ".block-menu-handle", function(event) {
        if (!menuElement.hasClass("open")) {
            // Check which buttons to show
            $.each(buttons, function(index, button) {
                if (button.enabled != null && !button.enabled(activeBlock)) {
                    $(button.element).addClass("disabled");
                    $(button.element).removeClass("enabled");
                    $(button.element).off("mouseup.block-menu-action")
                } else {
                    $(button.element).removeClass("disabled");
                    $(button.element).addClass("enabled");
                    $(button.element).off("mouseup.block-menu-action")

                    $(button.element).on("mouseup.block-menu-action", function(event) {
                        button.action(event);
                        BlockMenu.hideMenu();
                    })
                }

            });

            menuElement.addClass("open");
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);

            // Close the menu if we click outside the menu
            // This handler is deleted when the menu hides
            $(document).on("mousedown.remove_block_menu", function (event) {
                var target = $(event.target);
                if (!target.hasClass("block-menu-item") && !target.closest("block-menu-item").length > 0) {
                    menuElement.removeClass("open");
                    BlockMenu.hideMenu();
                    Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
                }

            });
        }

        event.preventDefault();
    });


    /*
    * This allows us to check if we are hovering over the menu or not
    * can be useful...
    * */
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


    /*
    * Show hide remove menu
    * */


    this.createMenu = function() {
        $("body").append(menuElement);
        BlockMenu.hideMenu();
    }

    this.removeMenu = function() {
        menuElement.remove();
    }

    this.hideMenu = function() {
        $(document).off("mousedown.remove_block_menu");
        activeBlock = null;
        menuElement.removeClass("open");
        menuElement.hide();
    };

    /*
     * Add button to menu: priority
     *
     * button is a json object with fields:
     * - priority: sets the place in the menu. Higher priority is higher in the menu
     * - enabled: a function that returns true or false if this button should be shown for this block
     * - action: function called when button is clicked
     * */

    var buttons = [];
    var activeBlock = null;
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

    // The current block that the menu is activated on
    // This way, when a plugin registers at the menu, it always has acces to the current block and element
    this.currentBlock = function() {
        return activeBlock;
    };



    this.showMenu = function(block) {
        if (block != null) {
            visible = true;
            menuElement.hide();
            // sets the block this menu is currently bound to
            activeBlock = block;


            menuElement.css("position", "absolute");
            var menuHeight = menuElement.height();
            menuElement.css("top", (activeBlock.top - menuHeight) + "px");
            // put menu in upper left corner of block
            var menuWidth = menuElement.width();
            menuElement.css("left", (activeBlock.right - menuWidth) + "px");
            menuElement.css("z-index", Constants.maxIndex + 1);
            menuElement.show();
        } else {
            menuElement.hide();
        }
    };




}]);