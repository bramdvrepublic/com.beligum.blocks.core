blocks.plugin("blocks.core.menu", ["blocks.core.Broadcaster", function(Broadcaster) {

    var menuElement = $('<div class="btn-group" role="group"><button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-expanded="false">Dropdown<span class="caret"></span></button><ul class="dropdown-menu" role="menu"></ul></div>')
    var btnList = menuElement.find(".dropdown-menu");

    var saveBtn = $('<li><a href="#">Save</a></li>');
    btnList.append(saveBtn);

    menuElement.addClass("blocks-main-menu");


    $("body").append(menuElement);

    menuElement.on("mouseenter", function(event) {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
    })

    menuElement.on("mouseleave", function(event) {
        Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
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
                Logger.debug("SAved data!");
            }
        })
    })




}]);