blocks.plugin("blocks.core.menu", ["blocks.core.Broadcaster", "blocks.core.Notification", function(Broadcaster, Notification) {

    var menuElement = $('<div class="blocks-main-menu"><div class"main-menu-button"><i class="glyphicon glyphicon-cog"></i></div><div class="main-menu-items"></div></div>')
    var btnList = menuElement.find(".main-menu-items");

    var templateBtn = $('<div class="main-menu-item"><a href="#">Change template</a></div>');
    btnList.append(templateBtn);
    var saveBtn = $('<div class="main-menu-item"><a href="#">Save</a></div>');
    btnList.append(saveBtn);


    $("body").append(menuElement);

    menuElement.on("mouseenter", function(event) {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        menuElement.addClass("open");
    });

    menuElement.on("mouseleave", function(event) {
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
        menuElement.removeClass("open");
    });

    menuElement.on("click", function(event) {
        if (menuElement.hasClass("open")) {
            menuElement.removeClass("open");
        } else {
            menuElement.addClass("open");
        }
    });

    saveBtn.on("click", function() {
        menuElement.removeClass("open");
        var page = $("html")[0].outerHTML;
        var o = JSON.stringify({"url": document.URL, "page": page});
        var test = JSON.stringify({t: [{x: 1, y:2}, {x: 1}]});
        $.ajax({type: 'POST',
                url: "/entities/save",
                data: o,
                contentType: 'application/json; charset=UTF-8',
                success: function() {
                    Logger.debug("Saved data!");
                },
                dataType: 'json'}
        )
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