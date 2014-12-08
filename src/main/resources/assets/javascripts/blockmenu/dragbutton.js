blocks.plugin("blocks.core.Dragbutton", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.DomManipulation", "blocks.core.Overlay", "blocks.core.Broadcaster", function(Menu, Layouter, DOM, Overlay, Broadcaster) {

    var button = $('<div type="button" class="btn btn-default"><i class="glyphicon glyphicon-zoom-in"></i></div>')

    var isEnabled = function(currentBlock) {
        if (DOM.canLayout(currentBlock.element)) {
            return true;
        } else {
            return false;
        }
    }

    Menu.addButton({
        element: button,
        priority: 500,
        enabled: isEnabled
    })

    var layoutParent = function() {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        Broadcaster.setLayoutParent(null);
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
    };

    button.click(function() {
        var currentBlock = Menu.currentBlock();
        if (isEnabled(currentBlock)) {
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
            Broadcaster.setLayoutParent($(currentBlock.element));
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            Overlay.createForBlock(currentBlock, function () {
                layoutParent();
            });
        }
    })

}]);