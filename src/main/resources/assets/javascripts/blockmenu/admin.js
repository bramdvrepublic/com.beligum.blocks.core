/**
 * Created by wouter on 26/11/14.
 */

blocks.plugin("blocks.core.Admin", ["blocks.core.BlockMenu", "blocks.core.DomManipulation",  "blocks.core.Notification", "blocks.core.Broadcaster", function(Menu, DOM, Notification, Broadcaster) {

    var registeredPlugins = [];
    var selectedPlugin = null;

    var enabled = function(block) {

        var retVal = false;
        for (var i=0; i < registeredPlugins.length; i++) {
            var plugin = registeredPlugins[i];
            retVal = plugin.enabled(block);
            if (retVal) {
                selectedPlugin = plugin;
                break;
            }
        }
        if(!retVal) selectedPlugin = null;
        return retVal;
    };


    var startAdmin = function(block, el) {
        enabled(block);
        var content = $("<div/>");
        if (typeof(selectedPlugin.element) == "function") {
            content.append(selectedPlugin.element());
        } else {
            content.append(selectedPlugin.element);
        }

        content.addClass("admin-dialog-content");

        BootstrapDialog.show({
            title: selectedPlugin.title,
            message: content.clone(),
            type: BootstrapDialog.TYPE_INFO, // <-- Default value is BootstrapDialog.TYPE_PRIMARY
            buttons: [
                {id: 'btn-close',
                    label: 'Cancel',
                    action: function(dialogRef){
                        dialogRef.close();
                        Broadcaster.send(Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE));
                    }},
                {
                    id: 'btn-ok',
                    icon: 'glyphicon glyphicon-check',
                    label: 'Ok',
                    cssClass: 'btn-primary',
                    action: function(dialogRef){
                        selectedPlugin.callback(block, el, dialogRef.$modalBody);
                        dialogRef.close();
                        Broadcaster.send(Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE));
                    }

                }]
        });

        //Notification.dialog(selectedPlugin.title, content.clone(),
        //    function (body) {
        //        selectedPlugin.callback(block, el, body);
        //
        //    },
        //    function () {
        //        Broadcaster.send(Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE));
        //
        //    });
    };


    /*
    * Register with object with properties:
    *   - enabled(block) = function that returns true when backend is enabled for this block
    *   - callback(block) = function that is called when the button is pressed
    *   - html = element to insert as backend
    *   When 2 plugins register for the same block, the first will win.
    * */
    this.register = function(plugin) {
        if (plugin.enabled == null) plugin.enabled = function() {return false;};
        if (plugin.callback == null) plugin.callback = function() {};
        // TODO check for string
        if (plugin.element == null) plugin.element = "";
        registeredPlugins.push(plugin);
    };


    var button = $('<div ><i class="glyphicon glyphicon-pencil"></i> Edit block</div>');


    Menu.addButton({
        element: button,
        priority: 100,
        enabled: enabled,
        action: function(event) {
            startAdmin(Menu.currentBlock(), Menu.currentBlock().element);
        }
    });

}]);