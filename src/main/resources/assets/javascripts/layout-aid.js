blocks.plugin("blocks.core.layout-aid", ["blocks.core.Mouse", "blocks.core.Broadcaster", "blocks.core.Elements", function(Mouse, Broadcaster, elements) {

    var layoutFrame = $("<div />");
    var createLayoutFrame = function() {
        layoutFrame.children().remove();
        for(var i=0; i < Elements.trees.length; i++) {
            var tree = Elements.trees[i];
            createFrame(tree);
        }
    };

    var createFrame = function(element) {
          if (element instanceof Elements.Block) {
              var box = $("<div />");
              box.css("position", "absolute");
              box.css("top", element.top + "px");
              box.css("top", element.left + "px");
              box.css("width", (element.right - element.left) + "px");
              box.css("height", (element.bottom - element.top) + "px");
              box.css("border", "solid black 1px");
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
        showLayoutFrame();
    };

    var leaveBlockHoover = function(blockEvent) {
        hideLayoutFrame();
    };

    Broadcaster.on(Mouse.config.EVENT.HOOVER_ENTER_BLOCK, function (event) {
        enterBlockHoover(event)
    });
    Broadcaster.on(Mouse.config.EVENT.HOOVER_LEAVE_BLOCK, function (event) {
        allowDrag(event)
    });


    Broadcaster.on(Mouse.config.EVENT.START_DRAG, function (event) {
        dragStarted(event)
    });
    Broadcaster.on(Mouse.config.EVENT.END_DRAG, function (event) {
        dragEnded(event)
    });
    Broadcaster.on(Mouse.config.EVENT.DRAG_LEAVE_BLOCK, function (event) {
        dragLeaveBlock(event)
    });
    Broadcaster.on(Mouse.config.EVENT.DRAG_OVER_BLOCK, function (event) {
        dragOverBlock(event)
    });

}]);