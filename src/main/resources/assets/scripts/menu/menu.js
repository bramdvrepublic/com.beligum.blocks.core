base.plugin("blocks.core.frame", ["blocks.core.Broadcaster", "blocks.core.Notification", "blocks.core.Overlay", "blocks.core.DomManipulation", "constants.blocks.common", function (Broadcaster, Notification, Overlay, DOM, Constants)
{
    var MainMenu = this;

    /*
    * Create the html for top bar
    * */
    this.menuStartButton = $('<div class="'+ Constants.BLOCKS_START_BUTTON +' '+ Constants.PREVENT_BLUR_CLASS +'"><i class="fa fa-cog"></i></div>');

    this.sideBar = $("<div class='" + Constants.PAGE_SIDEBAR_CLASS + " " + Constants.PREVENT_BLUR_CLASS +"'></div>");
    this.sideBar.load("/templates/sidebar");

     /*
     * Hide show bar on click of menu button
     * */
    var oldBodyMargin = parseInt($("body").css("padding-top"));
    var menuAnimationSpeed = 300;

    $(document).on("click", "."+ Constants.BLOCKS_START_BUTTON, function (event)
    {
        if ($("body").children("." + Constants.PAGE_CONTENT_CLASS).length == 0) {
            MainMenu.menuStartButton.remove();
            var body = $("body").html();
            $("body").empty();
            $("body").append($("<div class='" + Constants.PAGE_CONTENT_CLASS + "' />").append(body));
            $("body").append(MainMenu.sideBar);

            $(document).on("click.prevent_click_editing", "a", function(e) {
                e.preventDefault();
            });

            var windowWidth = $(window).width();
            MainMenu.sideBar.css("width", (windowWidth*0.2) + "px");
            $("." + Constants.PAGE_CONTENT_CLASS).css("width", (windowWidth*0.8) + "px");
            $("body").append(MainMenu.menuStartButton);
            updateContainerWidth();
            enableSidebarDrag();

            Broadcaster.send(Broadcaster.EVENTS.START_BLOCKS);
        } else {
            var content = $("." + Constants.PAGE_CONTENT_CLASS).html();
            $("body").empty();
            $("body").append(content);
            $(document).off("click.prevent_click_editing");
            Broadcaster.send(Broadcaster.EVENTS.STOP_BLOCKS);
            $("body").append(MainMenu.menuStartButton);
            disableSidebarDrag();
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
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        var page = $("html")[0].outerHTML;
        var dialog = new BootstrapDialog({
            type: BootstrapDialog.TYPE_WARNING,
            title: 'Saving ...',
            message: 'Please wait while we save the page. This can take a few seconds',
            buttons: []
        });

        dialog.open();

        $.ajax({
                type: 'POST',
                url: "/blocks/admin/page/save/" + window.location.href,
                data: page,
                contentType: 'application/json; charset=UTF-8',
                success: function (url, textStatus, response)
                {
                    dialog.close();
                    dialog = new BootstrapDialog({
                        type: BootstrapDialog.TYPE_SUCCESS,
                        title: 'Page saved',
                        message: '<p>The page was succesfully saved.</p>',
                        buttons: [
                            {
                                id: 'btn-close',
                                label: 'Ok',
                                action: function (dialogRef)
                                {
                                    dialogRef.close();
                                    Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
                                }
                            }
                        ]
                    });
                    dialog.open();
                },
                error: function (response, textStatus, errorThrown)
                {
                    var message = response.status == 400 ? response.responseText : "An error occurred while saving the page";
                    dialog.close();
                    dialog = new BootstrapDialog({
                        type: BootstrapDialog.TYPE_SUCCESS,
                        title: 'Page saved',
                        message: "<p>An error occurred while saving the page:</p><p>" + message + "</p>",
                        buttons: [
                            {
                                id: 'btn-close',
                                label: 'Ok',
                                action: function (dialogRef)
                                {
                                    dialogRef.close();
                                    Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
                                }
                            }
                        ]
                    });
                    dialog.open();
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


    var enableSidebarDrag = function() {
        $(document).on("mousedown.sidebar_resize", "."+Constants.PAGE_SIDEBAR_RESIZE_CLASS, function() {
            // On mousedown start resizing
            Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD);
            DOM.disableSelection();
            DOM.disableContextMenu();
            $("body").addClass(Constants.FORCE_RESIZE_CURSOR_CLASS);

            var windowWidth = $(window).width();
            var pageContent = $("." + Constants.PAGE_CONTENT_CLASS);
            $(document).on("mousemove.sidebar_resize", function(event) {
                var X = event.pageX;
                var ratioSide = windowWidth - X;
                var ratioPage = windowWidth - ratioSide;
                MainMenu.sideBar.css("width", ratioSide + "px");
                pageContent.css("width", ratioPage + "px");

            });

            $(document).on("mouseup.sidebar_resize", function() {

                // check size page content
                // find containers and get width
                // if container widgt is greater then page content width
                // set container width to pagecontent width - 20
                updateContainerWidth();


                $(document).off("mousemove.sidebar_resize");
                $(document).off("mouseup.sidebar_resize");
                DOM.enableSelection();
                DOM.enableContextMenu();
                $("body").removeClass(Constants.FORCE_RESIZE_CURSOR_CLASS);
                Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);
            });
        });
    };

    var disableSidebarDrag = function() {
        $(document).off("mousedown.sidebar_resize");
    };

    /*
    * in bootstrap the containerwidth is fixed. to prevent the container from bleeding
    * into our sidebar, we set the fixed with smaller then our page content wrapper
    * */
    var updateContainerWidth = function() {
        var wrapper = $("." + Constants.PAGE_CONTENT_CLASS);
        var containers = $(".container");
        containers.removeAttr("style");
        if (wrapper.length > 0) {
            var wrapperWidth = wrapper.outerWidth();
            var containerWidth = containers.width();
            if (containerWidth > wrapperWidth) {
                containers.css("width", (wrapperWidth - 50) + "px");
            }
        }
    };

    // On Window resize
    var resizing = false;
    $(window).on("smartresize", function() {
        if (resizing) {
            updateContainerWidth();
            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, layoutContainer);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            resizing = true;
        }
    });

    $(window).on("resize.blocks_broadcaster", function ()
    {
        if (resizing == false) {
            Overlay.removeOverlays();
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
            resizing = true;
        }
    });


    // Add the start button as only notice of our presence
    $("body").append(MainMenu.menuStartButton);


}]);