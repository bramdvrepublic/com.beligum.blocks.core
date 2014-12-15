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
    var blocks = {
        exhibition : $('<article typeof="exhibition" class="can-edit column exhibition square col-md-4"> <a href="" title=""> <span class="square-inner bgblue"> <header> <h3 property="exhibition-title">Molen en malen</h3> </header> <img property="exhibition-image" alt="" src=""/> </span> </a> </article>'),
        button: $('<article typeof="what-button" class="column can-edit what-button square col-md-4"> <a href="" title=""> <span class="square-inner"> <header can-edit> <h3>Een titel hier</h3> </header> <img alt="" src=""/> </span> </a> </article>'),
        video : $('<div typeof="video"><div class="embed-responsive embed-responsive-16by9"><iframe  class="iframe" class="embed-responsive-item" src="//player.vimeo.com/video/58880979"></iframe></div></div>'),
        image : $('<div class="block" typeof="image"><div class="image-container"><img class="img-responsive" src="/assets/images/navigo/wetenschapper.jpg" /><div class="clear"></div></div></div>'),
        text : $("<div class='block can-edit' typeof='text'><h1>Enter here your text.</h1></div>"),
        layout: $("<div class='block can-layout' ></div>")
    }

    button.on("click", function(event) {
        event.stopPropagation();
        var currentBlock = Menu.currentBlock();
        $.getJSON("/entities/list").success(function(data) {
            var optionList = $('<select class="form-control" id="blocktypeselect"></div>');
            var label = '<label for="inputPassword2" class="sr-only">Type block : </label>';
            for(var key in blocks) {
                optionList.append('<option value="'+key+'">'+key+'</option>');
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
