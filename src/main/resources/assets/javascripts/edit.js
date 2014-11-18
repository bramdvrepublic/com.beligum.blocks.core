blocks.plugin("blocks.core.Edit", ["blocks.core.Broadcaster", "blocks.core.BlockMenu", function(Broadcaster, Menu) {

    var registeredBlocks = {};
    /*
    * registerblock
    *  - admin-page-url
    *  - can edit
    *  - class-name
    *
    * */
    var registerBlock = function (event) {
        if (event.blockClassName != null && event.adminUrl != null) {
            if (registeredBlocks[event.blockClassName] == null) {
                registeredBlocks[event.blockClassName] = event.adminUrl;
            } else {
                // ALREADY EXISTS: notification
            }
        } else {

        }
    };
    Broadcaster.on(Broadcaster.EVENTS.REGISTER_EDITABLE_BLOCK, "blocks.core.Edit", function(event) {
        registerBlock(event);
    })

    var button = $('<div type="button" class="btn btn-small btn-default"><i class="glyphicon glyphicon-pencil"></i></div>')
    Menu.addButton({
        element: button,
        priority: 110
    });



}]);