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
        for(var i=0; i < Broadcaster.getLayoutTree().length; i++) {
            var tree = Broadcaster.getLayoutTree()[i];
            createFrame(tree);
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
        if (blockEvent.block.current != null && !BlockMenu.mouseOverMenu()) {
//            showLayoutFrame();
            Overlay.highlightBlock(blockEvent.block.current);Overlay.highlightBlock(blockEvent.block.current);
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
    $(document).on(Broadcaster.EVENTS.START_DRAG, function (event) {
        leaveBlockHoover(event);
    });
    $(document).on(Broadcaster.EVENTS.DEACTIVATE_MOUSE, function (event) {
        leaveBlockHoover(event);
    });

    $(document).on(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, function() {
        leaveBlockHoover(event);
    })
//    $(document).on(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, function() {
//        createLayoutFrame();
//    })


}]);