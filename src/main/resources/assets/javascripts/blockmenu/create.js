/**
 * Created by wouter on 27/11/14.
 */
blocks.plugin("blocks.core.BlockMenu.new", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification",  function(Menu, Layouter, Notification) {
    var button = $('<div type="button" class="btn btn-default"><i class="glyphicon glyphicon-asterisk"></i></div>')
    Menu.addButton({
        element: button,
        priority: 100
    })

    var modalText = '<div class="form-inline" role="form"><div class="form-group">' +
        '<label for="inputPassword2" class="sr-only">Type block : </label>'  +
        '<select class="form-control" id="blocktypeselect"><option value="video">video</option><option value="image">image</option>' +
        '<option value="text">text</option><option value="layout">layout</option>' +
            '</div></div>';

    button.on("click", function(event) {
        var currentBlock = Menu.currentBlock();
        Notification.alert("Add new block", modalText, function(content) {
            var value = content.find("#blocktypeselect").val();
            var newBlock = blocks[value];
            Layouter.addNewBlockAtLocation($(newBlock[0].outerHTML), Menu.currentBlock());
        });
    })



    var blocks = {
        video : $('<div typeof="video"><div class="embed-responsive embed-responsive-16by9"><iframe  class="iframe" class="embed-responsive-item" src="//player.vimeo.com/video/26196053?color=94127a"></iframe></div></div>'),
        image : $('<div class="block" typeof="image"><div class="image-container"><img class="img-responsive" src="/assets/images/navigo/wetenschapper.jpg" /><div class="clear"></div></div></div>'),
        text : $("<div class='block can-edit' typeof='text'><h1>Enter here your text.</h1></div>"),
        layout: $("<div class='block can-layout' ></div>")
    }


}]);

blocks.plugin("blocks.core.BlockMenu.newText", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification",  function(Menu, Layouter, Notification) {
    var button = $('<div type="button" class="btn btn-default"><i class="glyphicon glyphicon-align-justify"></i></div>')
    Menu.addButton({
        element: button,
        priority: 100
    });

    var newBlock = $("<div class='block can-edit' typeof='text'><h1>Enter here your text.</h1></div>")

    button.on("click", function(event) {
        var currentBlock = Menu.currentBlock();
        // copy block and add to body
        Layouter.addNewBlockAtLocation($(newBlock[0].outerHTML), Menu.currentBlock());
    })

//    button.on("click", function(event) {
//        var currentBlock = Menu.currentBlock();
//        Notification.alert("Add new block", modalText, function(content) {
//            var value = content.find("#blocktypeselect").val();
//            var newBlock = blocks[value];
//            Layouter.addNewBlockAtLocation($(newBlock[0].outerHTML), Menu.currentBlock());
//        });
//    })




}]);
