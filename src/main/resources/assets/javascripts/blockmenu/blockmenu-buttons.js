/*
*
* Buttons for a blockMenu.
* Add a button by calling addButton (of a blockMenu) with an object () the button
*
* Button object:
*   - element: jQuery element that is the button, normally a bootstrap button (with class btn)
*   - priority: sets the priority of the button. Button with higher priority will be placed more to the left (front)
*
* */
// TODO add button to layout inside a parsedContent block
// block-parsedContent class


blocks.plugin("blocks.core.BlockMenu.delete", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification",  function(Menu, Layouter, Notification) {
    var button = $('<div class="block-menu-item"><i class="glyphicon glyphicon-trash"></i> Delete</div>')
    Menu.addButton({
        element: button,
        priority: 105,
        enabled : function(block) {
            var total = block.getTotalBlocks();
            Logger.debug("Total: " + total);
            if (total == 1) {
                return false;
            } else {
                return true;
            }
        },
        action: function(event) {
            event.stopPropagation();
            var currentBlock = Menu.currentBlock();
            BootstrapDialog.show({
                title: 'WARNING',
                message: 'Are you sure you want to delete this block?',
                type: BootstrapDialog.TYPE_DANGER, // <-- Default value is BootstrapDialog.TYPE_PRIMARY
                buttons: [
                    {id: 'btn-close',
                    label: 'Cancel',
                    action: function(dialogRef){
                        dialogRef.close();
                    }},
                    {
                        id: 'btn-ok',
                        icon: 'glyphicon glyphicon-check',
                        label: 'Ok',
                        cssClass: 'btn-primary',
                        action: function(dialogRef){
                            Layouter.removeBlock(currentBlock);
                            dialogRef.close();
                        }

                }]

            })
        }
    });

}]);

