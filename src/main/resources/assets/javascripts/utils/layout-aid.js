/*
* For simple layout helpers
*
* - show frame of all blocks on hoover.
*
*
* */

blocks.plugin("blocks.core.LayoutAid", ["blocks.core.Layouter", "blocks.core.Broadcaster", "blocks.core.Elements", "blocks.core.Constants", "blocks.core.Overlay", "blocks.core.BlockMenu", function(Layouter, Broadcaster, Elements, Constants, Overlay, BlockMenu) {

    var layoutFrame = $('<div style="position: absolute; top: 0px; left: 0px; z-index: 500;" />');
    var createLayoutFrame = function() {
        layoutFrame.children().remove();
        if (Broadcaster.getContainer() != null) {
            createFrame(Broadcaster.getContainer());
        }
    };

    var createFrame = function(element) {
          if (element instanceof Elements.Block) {
              var box = $("<div />");
              box.css("position", "absolute");
              box.css("top", element.top + "px");
              box.css("left", element.left + "px");
              box.css("width", (element.right - element.left) + "px");
              box.css("height", (element.bottom - element.top) + "px");
              box.css("border", "1px dotted grey");
              layoutFrame.append(box);
          } else {
              for (var i=0; i < element.children.length; i++) {
                  createFrame(element.children[i]);
              }
          }
    };
//
//    var showLayoutFrame = function() {
//        $("body").append(layoutFrame);
//    };
//
//    var hideLayoutFrame = function() {
//        layoutFrame.remove();
//    };

    var currentBlock = null;

    var enterBlockHoover = function(blockEvent) {
        if (blockEvent.block.current != null && blockEvent.block.current.canDrag && !BlockMenu.mouseOverMenu()) {
//            showLayoutFrame();
            Overlay.highlightBlock(blockEvent.block.current);
            currentBlock = blockEvent.block.current;
            BlockMenu.showMenuElement(blockEvent);
        }
    };

    var leaveBlockHoover = function(blockEvent) {
        if (currentBlock != null) {
//            hideLayoutFrame();
            Overlay.unhighlightBlock(currentBlock);
            BlockMenu.hideMenuElement(blockEvent);
            currentBlock = null;
        }
    };

    var currentProperty = null;
    var enterPropertyHoover = function(blockEvent) {
        if (blockEvent.property.current != null) {
//            showLayoutFrame();
            Overlay.highlightProperty(blockEvent.property.current);
            currentProperty = blockEvent.property.current;
        }
    };

    var leavePropertyHoover = function(blockEvent) {
        if (currentProperty != null) {
//            hideLayoutFrame();
            Overlay.unhighlightProperty(currentProperty);
            currentProperty = null;
        }
    };

    $(document).on(Broadcaster.EVENTS.HOOVER_ENTER_BLOCK, function (event) {
        Logger.debug("changed blocks enter");
        enterBlockHoover(event);

    });
    $(document).on(Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK, function (event) {
        Logger.debug("changed blocks leave");
        if(!BlockMenu.mouseOverMenu()) {
            leaveBlockHoover(event);
        }
    });

    $(document).on(Broadcaster.EVENTS.HOOVER_ENTER_PROPERTY, function (event) {
        Logger.debug("changed property highlight enter");
        enterPropertyHoover(event);

    });
    $(document).on(Broadcaster.EVENTS.HOOVER_LEAVE_PROPERTY, function (event) {
        Logger.debug("changed property highlight leave");
        leavePropertyHoover(event);

    });

    $(document).on(Broadcaster.EVENTS.START_DRAG, function (event) {
        leaveBlockHoover(event);
        leavePropertyHoover(event);
    });
    $(document).on(Broadcaster.EVENTS.DEACTIVATE_MOUSE, function (event) {
        leaveBlockHoover(event);
        leavePropertyHoover(event);
    });

    $(document).on(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, function() {

    })



}]);