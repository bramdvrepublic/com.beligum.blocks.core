/**
 * Created by wouter on 27/11/14.
 */
base.plugin("blocks.core.BlockMenu.new", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification", function (Menu, Layouter, Notification)
{
    var button = $('<div ><i class="glyphicon glyphicon-asterisk"></i> Add custom block</div>');




    Menu.addButton({
        element: button,
        priority: 100,
        action: function(event) {
            event.stopPropagation();


        }
    });

}]);

base.plugin("blocks.core.BlockMenu.newText", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification", function (Menu, Layouter, Notification)
{
    var button = $('<div ><i class="glyphicon glyphicon-align-justify"></i> Add basic text block</div>')
    Menu.addButton({
        element: button,
        priority: 100,
        action: function(event) {
            var currentBlock = Menu.currentBlock();
            // copy block and add to body
            Layouter.addNewBlockAtLocation($(newBlock[0].outerHTML), Menu.currentBlock());
        }
    });

    var newBlock = $("<div typeof='text' ><div property='content' can-edit><h1>Enter some text here.<h1></h1></div></div>")




}]);
