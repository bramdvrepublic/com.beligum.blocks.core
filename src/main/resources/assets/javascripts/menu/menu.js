blocks.plugin("blocks.core.menu", ["blocks.core.Broadcaster", "blocks.core.Notification", function(Broadcaster, Notification) {

    var menuElement = $('<div class="btn-group" role="group"><button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-expanded="false">Dropdown<span class="caret"></span></button><ul class="dropdown-menu" role="menu"></ul></div>')
    var btnList = menuElement.find(".dropdown-menu");

    var templateBtn = $('<li><a href="#">Change template</a></li>');
    btnList.append(templateBtn);
    var saveBtn = $('<li><a href="#">Save</a></li>');
    btnList.append(saveBtn);

    menuElement.addClass("blocks-main-menu");

    $("body").append(menuElement);

    menuElement.on("mouseenter", function(event) {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        menuElement.addClass("open");
    })

    menuElement.on("mouseleave", function(event) {
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
        menuElement.removeClass("open");
    })

    menuElement.on("click", function(event) {
        if (menuElement.hasClass("open")) {
            menuElement.removeClass("open");
        } else {
            menuElement.addClass("open");
        }
    })

    saveBtn.on("click", function() {
        menuElement.removeClass("open");
        var page = $("html").html();

        $.ajax({
            url: "/entities/test",
            type: "PUT",
            contentType: "application/json",
            data: {html: page},
            success: function() {
                Logger.debug("Saved data!");
            }
        })
    });

    var modalText = '<div class="form-inline" role="form"><div class="form-group"></div></div>';

    templateBtn.on("click", function() {
        // show dialog with all templates
        event.stopPropagation();
        $.getJSON("/entities/template").success(function(data) {
            var optionList = $('<select class="form-control" id="blocktypeselect"></div>');
            var label = '<label for="inputPassword2" class="sr-only">Type block : </label>';
            for(var i=0; i< data.length; i++) {
                optionList.append('<option value="'+data[i]+'">'+data[i]+'</option>');
            }
            var list = $(modalText);
            list.find(".form-group").empty().append(label).append(optionList);
            Notification.alert("Set template", list.html(), function(content) {
                var value = content.find("#blocktypeselect").val();
                if (value != null && value != "") {
                    $.ajax({
                        url: "/entities/template",
                        type: "PUT",
                        contentType: "application/json",
                        data: {template: value, id: location.href}
                    }).success(function(data) {
                        //var newBlock = blocks[value];
                        Logger.debug("Template changed")
                    });
                }

            });
        });
        // choose and click
    });


}]);