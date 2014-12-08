/**
 * Created by wouter on 27/11/14.
 */
blocks.plugin("blocks.core.BlockMenu.new", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification",  function(Menu, Layouter, Notification) {
    var button = $('<div type="button" class="btn btn-default"><i class="glyphicon glyphicon-asterisk"></i></div>');
    Menu.addButton({
        element: button,
        priority: 100
    });

    var modalText = '<div class="form-inline" role="form"><div class="form-group"></div></div>';

    button.on("click", function(event) {
        var currentBlock = Menu.currentBlock();
        $.getJSON("/entities/list").success(function(data) {
            var optionList = $('<select class="form-control" id="blocktypeselect"></div>');
            var label = '<label for="inputPassword2" class="sr-only">Type block : </label>';
            for(var i = 0; i < data.length; i++) {
                optionList.append('<option value="'+data[i]+'">'+data[i]+'</option>');
            }
            var list = $(modalText);
            list.find(".form-group").empty().append(label).append(optionList);
            Notification.alert("Add new block", list.html(), function(content) {
                var value = content.find("#blocktypeselect").val();

                var newBlock = blocks[value];
                Layouter.addNewBlockAtLocation($(newBlock[0].outerHTML), currentBlock);
            });
        });

    });



    var blocks = {
        video : $('<div typeof="video"><div class="embed-responsive embed-responsive-16by9"><iframe  class="iframe" class="embed-responsive-item" src="//player.vimeo.com/video/58880979"></iframe></div></div>'),
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


}]);
