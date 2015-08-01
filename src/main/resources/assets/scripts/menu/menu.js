base.plugin("blocks.core.Frame", ["blocks.core.Broadcaster", "blocks.core.Notification", "blocks.core.Overlay", "blocks.core.DomManipulation", "constants.blocks.core", "blocks.core.Sidebar", "messages.blocks.core", function (Broadcaster, Notification, Overlay, DOM, BlocksConstants, Sidebar, BlocksMessages)
{
    var Frame = this;

    var SIDEBAR_STATE_NULL = "";
    var SIDEBAR_STATE_SHOW = "show";
    var SIDEBAR_STATE_HIDE = "hide";

    //note: the icon of the <i> is set in blocks.less
    var menuStartButton = $('<a class="' + BlocksConstants.BLOCKS_START_BUTTON + '"></a>');

    var sidebarElement = $("<div class='" + BlocksConstants.PAGE_SIDEBAR_CLASS + " " + BlocksConstants.PREVENT_BLUR_CLASS + "'></div>");
    sidebarElement.load("/templates/sidebar");

    /*
     * Hide show bar on click of menu button
     * */
    $(document).on("click", "." + BlocksConstants.BLOCKS_START_BUTTON, function (event)
    {
        toggleSidebar($("body").children("." + BlocksConstants.PAGE_CONTENT_CLASS).length == 0);
    });

    var toggleSidebar = function (show)
    {

        var cookieState = SIDEBAR_STATE_NULL;

        if (show) {
            cookieState = SIDEBAR_STATE_SHOW;

            // Remove the menu button while animating sidebar
            menuStartButton.remove();

            // put body content in wrapper
            var body = $("body").html();
            $("body").empty();
            //wrap the content of the body in the class and add that again to the body
            $("body").append($("<div class='" + BlocksConstants.PAGE_CONTENT_CLASS + "' />").append(body));
            $("body").append(sidebarElement);
            $("body").addClass(BlocksConstants.BODY_EDIT_MODE_CLASS);

            // Prevent clicking on links while in editing mode
            $(document).on("click.prevent_click_editing", "a", function (e)
            {
                e.preventDefault();
            });

            // Get old sidebar width from cookie
            var cookieSidebarWidth = $.cookie(BlocksConstants.COOKIE_SIDEBAR_WIDTH);
            var windowWidth = $(window).width();
            var INIT_SIDEBAR_WIDTH = windowWidth * 0.2; // default width of sidebar is 20% of window
            if (cookieSidebarWidth != null) {
                INIT_SIDEBAR_WIDTH = cookieSidebarWidth;
            }

            menuStartButton.addClass("open");

            Sidebar.setSidebarWidth(INIT_SIDEBAR_WIDTH, function ()
            {
                $("body").append(menuStartButton);
                enableSidebarDrag();
                Broadcaster.send(Broadcaster.EVENTS.START_BLOCKS);
            });

        } else {
            cookieState = SIDEBAR_STATE_HIDE;
            var CLOSE_SIDEBAR_WIDTH = 0.0;
            menuStartButton.hide().removeClass("open");
            Sidebar.setSidebarWidth(CLOSE_SIDEBAR_WIDTH, function ()
            {
                disableSidebarDrag();
                menuStartButton.show();

                var content = $("." + BlocksConstants.PAGE_CONTENT_CLASS).html();
                $("body").empty();
                $("body").append(content);
                $(document).off("click.prevent_click_editing");
                Broadcaster.send(Broadcaster.EVENTS.STOP_BLOCKS);
                $("body").append(menuStartButton);

                $("body").removeClass(BlocksConstants.BODY_EDIT_MODE_CLASS);
            });
        }

        //Note: by default, the cookie is deleted when the browser is closed:
        $.cookie(BlocksConstants.COOKIE_SIDEBAR_STATE, cookieState);
    };

    //check for a cookie and auto-open when the sidebar was active
    var sidebarState = $.cookie(BlocksConstants.COOKIE_SIDEBAR_STATE);

    if (sidebarState === SIDEBAR_STATE_SHOW) {
        $(document).ready(function ()
        {
            toggleSidebar(true);
        });
    }

    var enableSidebarDrag = function ()
    {
        $(document).on("mousedown.sidebar_resize", "." + BlocksConstants.PAGE_SIDEBAR_RESIZE_CLASS, function ()
        {
            // On mousedown start resizing
            // Make sure we are no longer in edit mode
            Sidebar.reset();

            Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD);
            DOM.disableSelection();
            DOM.disableContextMenu();
            $("body").addClass(BlocksConstants.FORCE_RESIZE_CURSOR_CLASS);

            var windowWidth = $(window).width();
            var pageContent = $("." + BlocksConstants.PAGE_CONTENT_CLASS);
            $(document).on("mousemove.sidebar_resize", function (event)
            {
                var X = event.pageX;
                var sideWidth = windowWidth - X;
                var pageWidth = windowWidth - sideWidth;
                if (sideWidth > 200 && pageWidth > 200) {
                    sidebarElement.css("width", sideWidth + "px");
                    pageContent.css("width", pageWidth + "px");
                }
            });

            $(document).on("mouseup.sidebar_resize", function ()
            {

                // check size page content
                // find containers and get width
                // if container width is greater then page content width
                // set container width to pagecontent width - 20
                updateContainerWidth();

                $(document).off("mousemove.sidebar_resize");
                $(document).off("mouseup.sidebar_resize");
                DOM.enableSelection();
                DOM.enableContextMenu();
                $("body").removeClass(BlocksConstants.FORCE_RESIZE_CURSOR_CLASS);
                Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);

                //Note: by default, the cookie is deleted when the browser is closed:
                $.cookie(BlocksConstants.COOKIE_SIDEBAR_WIDTH, sidebarElement.width());
            });
        });
    };

    var disableSidebarDrag = function ()
    {
        $(document).off("mousedown.sidebar_resize");
    };

    /*
     * in bootstrap the containerwidth is fixed. to prevent the container from bleeding
     * into our sidebar, we set the width fixed with a new width, smaller than our page content wrapper
     * */
    var updateContainerWidth = function ()
    {
        var wrapper = $("." + BlocksConstants.PAGE_CONTENT_CLASS);
        var containers = $(".container");
        containers.removeAttr("style");
        if (wrapper.length > 0) {
            var wrapperWidth = wrapper.outerWidth();
            var containerWidth = containers.outerWidth();
            if (containerWidth > wrapperWidth) {
                containers.css("width", (wrapperWidth - 50) + "px");
            }
        }
    };

    var sidebarWidth = sidebarElement.outerWidth();
    // On Window resize
    var resizing = false;
    $(window).smartresize(function ()
    {
        if (resizing) {
            var windowWidth = $(window).width();
            $("." + BlocksConstants.PAGE_CONTENT_CLASS).css("width", (windowWidth - sidebarWidth) + "px");
            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            resizing = false;
        }
    });

    $(window).on("resize.blocks_broadcaster", function ()
    {
        if (resizing == false) {
            // Leave edit mode
            Sidebar.reset();
            sidebarWidth = sidebarElement.outerWidth();
            Overlay.removeOverlays();
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
            resizing = true;
        }
    });

    $(document).on(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, function (event)
    {
        updateContainerWidth();
    });

    /*
     * Save button: saves the page
     * */
    $(document).on("click", "." + BlocksConstants.SAVE_PAGE_BUTTON, function ()
    {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        Sidebar.reset();
        // remove the widths from the containers
        $(".container").removeAttr("style");
        var page = $("html")[0].outerHTML;
        //var page = $("body").html();
        updateContainerWidth();

        var dialog = new BootstrapDialog({
            type: BootstrapDialog.TYPE_PRIMARY,
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
        })
            .done(function (url, textStatus, response)
            {
            })
            .fail(function (xhr, textStatus, exception)
            {
                Notification.error(BlocksMessages.savePageError + (exception ? "; " + exception : ""), xhr);
            })
            .always(function ()
            {
                dialog.close();
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            });
    });

    /*
     * Delete button: deletes the page
     * */
    $(document).on("click", "." + BlocksConstants.DELETE_PAGE_BUTTON, function ()
    {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
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

    // Add the start button as only notice of our presence
    $("body").append(menuStartButton);

    //SETUP THE KEYBOARD SHORTCUTS
    $(document).keydown(function (e)
    {
        var retVal = true;

        //$.ui.keyCode.S
        var action;
        if (e) {
            if (e.ctrlKey) {
                switch (e.which) {
                    //Ctrl+S
                    case 83:
                        action = $("." + BlocksConstants.SAVE_PAGE_BUTTON).click();
                        break;
                }
            }
            else {
                switch (e.which) {
                    //DELETE
                    case 46:
                        action = $("." + BlocksConstants.DELETE_PAGE_BUTTON).click();
                        break;
                }
            }
        }

        if (action) {
            event.preventDefault();
            retVal = false;
        }

        return retVal;
    });

}]);