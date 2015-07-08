base.plugin("blocks.core.menu", ["blocks.core.Broadcaster", "blocks.core.Notification", "constants.blocks.common", "blocks.finder", function (Broadcaster, Notification, Constants, Finder)
{
    var MainMenu = this;

    /*
    * Create the html for top bar
    * */
    this.menuStartButton = $('<div class="'+ Constants.BLOCKS_START_BUTTON +'"><i class="glyphicon glyphicon-cog"></i></div>');

    this.menuBar = $("<div class='" + Constants.PAGE_MENU_CLASS + "'></div>");
    this.menuBar.load("/templates/menu");

    this.sideBar = $("<div class='" + Constants.PAGE_SIDEBAR_CLASS + " " + Constants.PREVENT_EDIT_BLUR_CLASS +"'></div>");
    this.sideBar.load("/templates/sidebar");

     /*
     * Hide show bar on click of menu button
     * */
    var oldBodyMargin = parseInt($("body").css("padding-top"));
    var menuAnimationSpeed = 300;

    $(document).on("click", "."+ Constants.BLOCKS_START_BUTTON, function (event)
    {
        if ($("body").children("." + Constants.PAGE_MENU_CLASS).length == 0) {

            var body = $("body").html();
            $("body").empty();
            $("body").append(MainMenu.menuBar);
            $("body").append($("<div class='" + Constants.PAGE_CONTENT_CLASS + "' />").append(body));
            $("body").append(MainMenu.sideBar);
            if (MainMenu.sideBar.children().length > 0) {
                Finder.init();
            } else {
                //TODO: Sidebar is not yet loaded so we can not initialize finder
            }

            Broadcaster.send(Broadcaster.EVENTS.START_BLOCKS);

        } else {
            var content = $("." + Constants.PAGE_CONTENT_CLASS).html();
            $("body").empty();
            $("body").append(content);

            Broadcaster.send(Broadcaster.EVENTS.STOP_BLOCKS);
        }
    });

    function queryParam(paramName)
    {
        var query = window.location.search.split("?");
        if (query.length > 1) {
            var paramPairs = query[1].split("&");
            var i;
            for (i = 0; i < paramPairs.length; i++) {
                var paramPair = paramPairs[i];
                var pair = paramPair.split("=");
                if (pair[0] == paramName) {
                    return pair[1];
                }
            }
        }
        return null;
    }

    /*
     * Save button: saves the page
     * */
    $(document).on("click", "."+Constants.SAVE_PAGE_BUTTON, function ()
    {
        var page = $("html")[0].outerHTML;
        var deleted = queryParam("deleted")
        if (!deleted) {
            deleted = false;
        }
        $.ajax({
                type: 'POST',
                url: "/blocks/admin/page/save/" + window.location.href,
                data: page,
                contentType: 'application/json; charset=UTF-8',
                success: function (url, textStatus, response)
                {
                    if (url) {
                        window.location = url;
                    } else {
                        location.reload();
                    }
                },
                error: function (response, textStatus, errorThrown)
                {
                    var message = response.status == 400 ? response.responseText : "An error occurred while saving the page";
                    Notification.dialog("Error", "<div>" + message + "</div>", function ()
                    {
                    });
                }
            }
        )
    });

    /*
     * Delete button: deletes the page
     * */
    $(document).on("click", "."+Constants.DELETE_PAGE_BUTTON, function ()
    {
        var onConfirm = function ()
        {
            $.ajax({
                    type: 'DELETE',
                    url: "/blocks/admin/page/delete",
                    data: document.URL,
                    contentType: 'application/json; charset=UTF-8',
                    success: function (url, textStatus, response)
                    {
                        if (url) {
                            window.location = url;
                        } else {
                            location.reload();
                        }
                    },
                    error: function (response, textStatus, errorThrown)
                    {
                        var message = response.status == 400 ? response.responseText : "An error occurred while deleting the page.";
                        Notification.dialog("Error", "<div>" + message + "</div>", function ()
                        {
                        });
                    }
                }
            )
        };

        BootstrapDialog.show({
            title: "Delete page",
            message: "<div>Do you want to delete this page and all it's translations?</div>",
            buttons: [
                {
                    id: 'btn-close',
                    label: 'Cancel',
                    action: function (dialogRef)
                    {
                        dialogRef.close();
                        Broadcaster.send(Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE));
                    }
                },
                {
                    id: 'btn-ok',
                    icon: 'glyphicon glyphicon-check',
                    label: 'Ok',
                    cssClass: 'btn-primary',
                    action: function (dialogRef)
                    {
                        onConfirm();
                        dialogRef.close();
                        Broadcaster.send(Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE));
                    }

                }
            ]
        });

    });




    var create = function ()
    {
        $("body").append(MainMenu.menuStartButton);
    };


    create();


}]);