blocks.plugin("blocks.core.menu", ["blocks.core.Broadcaster", "blocks.core.Notification", function(Broadcaster, Notification) {
    this.MainMenu = this;

    var menuBtn = $('<div class="blocks-main-edit-button"><i class="glyphicon glyphicon-cog"></i></div>');
    var menuBar = $('<div class="blocks-main-menu"><div class="main-menu-items"></div></div>');
    var btnList = menuBar;

    //TODO BAS SH 3: add a modal to the delete-button (sketch is given here)
//    var templateBtn = $('<a class="btn  btn-default" href="#">Change template</a>');
//    btnList.append(templateBtn);
    var saveBtn = $('<a class="btn  btn-default" href="#">Save</a>');
    btnList.append(saveBtn);
    var deleteBtn = $('<a class="btn  btn-default" href="#">Delete</a>');
    btnList.append(deleteBtn);
    var modalBtn = $('<button type="button" class="btn btn-primary btn-lg" data-toggle="modal" data-target="#myModal">Launch demo modal </button>');
    btnList.append(modalBtn);

    var deleteModal = $('<div class="modal fade" id="myModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true"> ' +
    '<div class="modal-dialog"> ' +
    '<div class="modal-content"> ' +
    '<div class="modal-header"> ' +
    '<button type="button" class="close" data-dismiss="modal" aria-label="Close">' +
    '<span aria-hidden="true">&times;</span>' +
    '</button> ' +
    '<h4 class="modal-title" id="myModalLabel">Modal title</h4> ' +
    '</div> ' +
    '<div class="modal-body">... </div> ' +
    '<div class="modal-footer"> ' +
    '<button type="button" class="btn btn-default" data-dismiss="modal">Close</button> ' +
    '<button type="button" class="btn btn-primary">Save changes</button> ' +
    '</div> ' +
    '</div> ' +
    '</div> ' +
    '</div>');
    $("body").append(deleteModal);


    menuBtn.on("click", function(event) {
        if (menuBar.hasClass("open")) {
            menuBar.removeClass("open");
            Broadcaster.send(Broadcaster.EVENTS.STOP_BLOCKS);
        } else {
            menuBar.addClass("open");
            Broadcaster.send(Broadcaster.EVENTS.START_BLOCKS);
        }
    });

    saveBtn.on("click", function() {
        menuBar.removeClass("open");
        var page = $("html")[0].outerHTML;
        var o = JSON.stringify({"url": document.URL, "page": page});
        var test = JSON.stringify({t: [{x: 1, y:2}, {x: 1}]});
        $.ajax({type: 'POST',
                url: "/entities/save",
                data: o,
                contentType: 'application/json; charset=UTF-8',
                complete: function(data) {
                    if(data.responseText){
                        //an url has been returned
                        window.location = data.responseText;
                    }else {
                        Logger.debug("Saved data!");
                        menuBar.addClass("open");
                    }
                },
                dataType: 'json'}
        )
    });

    deleteBtn.on("click", function() {
        menuBar.removeClass("open");
        $.ajax({type: 'POST',
                url: "/entities/delete",
                data: document.URL,
                success: function(url) {
                    if(url){
                        window.location = url;
                    }else{
                        location.reload();
                    }
                }
            }
        )
    });


    var create = function() {
        $("body").prepend(menuBar);
        $("body").append(menuBtn);
    };

    var remove = function() {
        menuBar.remove();
        menuBtn.remove();
    };

    create();



    var modalText = '<div class="form-inline" role="form"><div class="form-group"></div></div>';
//
//    templateBtn.on("click", function() {
//        // show dialog with all templates
//        event.stopPropagation();
//        $.getJSON("/entities/template").success(function(data) {
//            var optionList = $('<select class="form-control" id="blocktypeselect"></div>');
//            var label = '<label for="inputPassword2" class="sr-only">Type block : </label>';
//            for(var i=0; i< data.length; i++) {
//                optionList.append('<option value="'+data[i]+'">'+data[i]+'</option>');
//            }
//            var list = $(modalText);
//            list.find(".form-group").empty().append(label).append(optionList);
//            Notification.alert("Set template", list.html(), function(content) {
//                var value = content.find("#blocktypeselect").val();
//                if (value != null && value != "") {
//                    $.ajax({
//                        url: "/entities/template",
//                        type: "PUT",
//                        contentType: "application/json",
//                        data: {template: value, id: location.href}
//                    }).success(function(data) {
//                        //var newBlock = blocks[value];
//                        Logger.debug("Template changed")
//                    });
//                }
//
//            });
//        });
//        // choose and click
//    });

//    templateBtn.on("click", function() {
//        // show dialog with all templates
//        event.stopPropagation();
//        $.getJSON("/entities/template").success(function(data) {
//            var optionList = $('<select class="form-control" id="blocktypeselect"></div>');
//            var label = '<label for="inputPassword2" class="sr-only">Type block : </label>';
//            for(var i=0; i< data.length; i++) {
//                optionList.append('<option value="'+data[i]+'">'+data[i]+'</option>');
//            }
//            var list = $(modalText);
//            list.find(".form-group").empty().append(label).append(optionList);
//            Notification.alert("Set template", list.html(), function(content) {
//                var value = content.find("#blocktypeselect").val();
//                if (value != null && value != "") {
//                    $.ajax({
//                        url: "/entities/template",
//                        type: "PUT",
//                        contentType: "application/json",
//                        data: {template: value, id: location.href}
//                    }).success(function(data) {
//                        //var newBlock = blocks[value];
//                        Logger.debug("Template changed")
//                    });
//                }
//
//            });
//        });
//        // choose and click
//    });


}]);