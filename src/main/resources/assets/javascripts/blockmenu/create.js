/**
 * Created by wouter on 27/11/14.
 */
blocks.plugin("blocks.core.BlockMenu.new", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification",  function(Menu, Layouter, Notification) {
    var button = $('<div ><i class="glyphicon glyphicon-asterisk"></i> Add custom block</div>');


    var modalText = '<div class="form-inline" role="form"><div class="form-group"></div></div>';

    Menu.addButton({
        element: button,
        priority: 100,
        action: function(event) {
            event.stopPropagation();
            var currentBlock = Menu.currentBlock();
            $.getJSON("/entities/list").success(function(data) {
                var optionList = $('<select class="form-control" id="blocktypeselect"></div>');
                var label = '<label for="inputPassword2" class="sr-only">Type block : </label>';
                for(var i=0; i< data.length; i++) {
                    optionList.append('<option value="'+data[i]+'">'+data[i]+'</option>');
                }
                var list = $(modalText);
                list.find(".form-group").empty().append(label).append(optionList);

                BootstrapDialog.show({
                    title: 'Add a custom block',
                    message: list,
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
                                var value = dialogRef.$modalBody.find("#blocktypeselect").val();
                                if (value != null && value != "") {
                                    $.getJSON("/entities/class/" + value).success(function(data) {
                                        var x= 0;
                                        Layouter.addNewBlockAtLocation($(data.template), currentBlock);
                                    });
                                }
                                dialogRef.close();
                            }

                        }]

                })
                //
                //Notification.alert("Add new block", list.html(), function(content) {
                //    var value = content.find("#blocktypeselect").val();
                //    if (value != null && value != "") {
                //        $.getJSON("/entities/class/" + value).success(function(data) {
                //            //var newBlock = blocks[value];
                //            var x= 0;
                //            Layouter.addNewBlockAtLocation($(data.template), currentBlock);
                //        });
                //    }
                //
                //});
            });

        }
    });

}]);

blocks.plugin("blocks.core.BlockMenu.newText", ["blocks.core.BlockMenu", "blocks.core.Layouter", "blocks.core.Notification",  function(Menu, Layouter, Notification) {
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
