blocks.plugin("blocks.core.menu", ["blocks.core.Broadcaster", "blocks.core.Notification", function(Broadcaster, Notification) {
    this.MainMenu = this;

    var menuBtn = $('<div class="blocks-main-edit-button"><i class="glyphicon glyphicon-cog"></i></div>');
    var menuBar = $('<div class="blocks-main-menu"><div class="main-menu-items"></div></div>');
    var btnList = menuBar;

//    var templateBtn = $('<a class="btn  btn-default" href="#">Change template</a>');
//    btnList.append(templateBtn);
    var saveBtn = $('<a class="btn  btn-default" href="#">Save</a>');
    btnList.append(saveBtn);
    var deleteBtn = $('<a class="btn  btn-default" href="#">Delete</a>');
    btnList.append(deleteBtn);
    var changeUrlBtn = $('<a class="btn  btn-default" href="#">Change url</a>');
    btnList.append(changeUrlBtn);


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
        $.ajax({type: 'PUT',
                url: "/entities/" + window.location.pathname,
                data: page,
                contentType: 'application/json; charset=UTF-8',
                success: function(url, textStatus, response) {
                    if(url){
                        window.location = url;
                    }else{
                        location.reload();
                    }
                },
                error: function(response, textStatus, errorThrown) {
                    var message = response.status == 400 ? response.responseText : "An error occurred while saving the page";
                    Notification.dialog("Error", "<div>" + message + "</div>", function(){});
                }}
        )
    });

    deleteBtn.on("click", function() {
        var onConfirm = function(){
            $.ajax({type: 'POST',
                    url: "/entities/delete",
                    data: document.URL,
                    success: function(url, textStatus, response) {
                        if(url){
                            window.location = url;
                        }else{
                            location.reload();
                        }
                    },
                    error: function(response, textStatus, errorThrown) {
                        var message = response.status == 400 ? response.responseText : "An error occurred while deleting the page.";
                        Notification.dialog("Error", "<div>" + message + "</div>", function(){});
                    }
                }
            )
        };

        BootstrapDialog.show({
            title: "Delete page",
            message: "<div>Do you want to delete this page and all it's translations?</div>",
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
                        onConfirm();
                        dialogRef.close();
                        Broadcaster.send(Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE));
                    }

                }
            ]
        });
       
    });

    changeUrlBtn.on("click", function() {
        var translateDialog = new BootstrapDialog()
            .setTitle('Change url')
            .setMessage($('<div></div>').load('/modals/changeurl?original=' + window.location.href))
            .setType(BootstrapDialog.TYPE_INFO)
            .setButtons([
                {
                    label: 'Cancel',
                    action: function (changeUrlDialog) {
                        changeUrlDialog.close();
                    }
                },
                {
                    label: 'Change',
                    cssClass: 'btn-info',
                    action: function (changeUrlDialog) {
                        changeUrlDialog.close();
                        $.ajax({
                            type: 'POST',
                            url: "/urls?original=" + window.location.href + "&newpath=" + $("#new").val(),
                            success: function(url, textStatus, response){
                                if(url){
                                    window.location = url;
                                }else{
                                    location.reload();
                                }
                            },
                            error: function(response, textStatus, errorThrown){
                                var message = response.status == 400 ? response.responseText : "An error occurred while changing the url.";
                                Notification.dialog("Error", "<div>" + message + "</div>", function(){});
                            }
                        })
                    }
                }])
            .open();
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