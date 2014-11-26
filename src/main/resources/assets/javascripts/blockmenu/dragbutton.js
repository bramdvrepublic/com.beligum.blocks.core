blocks.plugin("blocks.core.Dragbutton", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Overlay", "blocks.core.Broadcaster", function(Menu, Layouter, Overlay, Broadcaster) {

    var button = $('<div type="button" class="btn btn-default"><i class="glyphicon glyphicon-move"></i></div>')
    Menu.addButton({
        element: button,
        priority: 5
    })


    button.click(function() {
        var currentBlock = Menu.currentBlock();
        Broadcaster.send(new Broadcaster.EVENTS.DEACTIVATE_MOUSE());
        Layouter.setLayoutParent($(currentBlock.element));
        Broadcaster.send(new Broadcaster.EVENTS.ACTIVATE_MOUSE());
        Broadcaster.send(new Broadcaster.EVENTS.DO_ALLOW_DRAG());
        Overlay.createForBlock(currentBlock, function() {
            Layouter.setLayoutParent(null);
        });
    })

}]);