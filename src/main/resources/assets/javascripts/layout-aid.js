/*
* For simple layout helpers
*
* - show frame of all blocks on hoover.
*
*
* */

blocks.plugin("blocks.core.layout-aid", ["blocks.core.Layouter", "blocks.core.Broadcaster", "blocks.core.Elements", function(Layouter, Broadcaster, Elements) {

    var layoutFrame = $('<div style="position: absolute; top: 0px; left: 0px; z-index: 500;" />');
    var createLayoutFrame = function() {
        layoutFrame.children().remove();
        for(var i=0; i < Layouter.getLayoutTree().length; i++) {
            var tree = Layouter.getLayoutTree()[i];
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
              box.css("border", "1px solid black");
              layoutFrame.append(box);
          } else {
              for (var i=0; i < element.children.length; i++) {
                  createFrame(element.children[i]);
              }
          }
    };

    var showLayoutFrame = function() {
        $("body").append(layoutFrame);
    };

    var hideLayoutFrame = function() {
        layoutFrame.remove();
    };

    var enterBlockHoover = function(blockEvent) {
        if (blockEvent.block.current != null) {
            showLayoutFrame();
        }
    };

    var leaveBlockHoover = function(blockEvent) {
        if (blockEvent.block.current == null) {
            hideLayoutFrame();
        }
    };

    Broadcaster.on(Broadcaster.EVENTS.HOOVER_ENTER_BLOCK, function (event) {
        //enterBlockHoover(event)
    });
    Broadcaster.on(Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK, function (event) {
        //leaveBlockHoover(event)
    });

    Broadcaster.on("layoutChanged", function() {
        //createLayoutFrame();
    })


}]);