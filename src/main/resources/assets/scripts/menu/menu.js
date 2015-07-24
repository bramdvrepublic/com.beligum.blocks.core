base.plugin("blocks.core.Frame", ["blocks.core.Broadcaster", "blocks.core.Notification", "blocks.core.Overlay", "blocks.core.DomManipulation", "constants.blocks.common", "blocks.core.Sidebar", function (Broadcaster, Notification, Overlay, DOM, Constants, Sidebar)
{
    var Frame = this;

    var SIDEBAR_STATE_NULL = "";
    var SIDEBAR_STATE_SHOW = "show";
    var SIDEBAR_STATE_HIDE = "hide";

    //note: the icon of the <i> is set in blocks.less
    var menuStartButton = $('<a class="'+ Constants.BLOCKS_START_BUTTON +'"></a>');

    var sidebarElement = $("<div class='" + Constants.PAGE_SIDEBAR_CLASS + " " + Constants.PREVENT_BLUR_CLASS +"'></div>");
    sidebarElement.load("/templates/sidebar");

    /*
     * Hide show bar on click of menu button
     * */

    $(document).on("click", "."+ Constants.BLOCKS_START_BUTTON, function (event)
    {
        toggleSidebar($("body").children("." + Constants.PAGE_CONTENT_CLASS).length == 0);
    });

    var toggleSidebar = function(show) {

        var cookieState = SIDEBAR_STATE_NULL;

        if (show) {
            cookieState = SIDEBAR_STATE_SHOW;

            // Remove the menu button while animating sidebar
            menuStartButton.remove();

            // put body content in wrapper
            var body = $("body").html();
            $("body").empty();
            $("body").append($("<div class='" + Constants.PAGE_CONTENT_CLASS + "' />").append(body));
            $("body").append(sidebarElement);

            // Prevent clicking on links while in editing mode
            $(document).on("click.prevent_click_editing", "a", function(e) {
                e.preventDefault();
            });

            // Get old sidebar width from cookie
            var cookieSidebarWidth = $.cookie(Constants.COOKIE_SIDEBAR_WIDTH);
            var windowWidth = $(window).width();
            var INIT_SIDEBAR_WIDTH = windowWidth * 0.2; // default width of sidebar is 20% of window
            if (cookieSidebarWidth != null) {
                INIT_SIDEBAR_WIDTH = cookieSidebarWidth;
            }

            menuStartButton.addClass("open");

            setSidebarWidth(INIT_SIDEBAR_WIDTH, function() {
                $("body").append(menuStartButton);
                enableSidebarDrag();
                Broadcaster.send(Broadcaster.EVENTS.START_BLOCKS);
            });

        } else {
            cookieState = SIDEBAR_STATE_HIDE;
            var CLOSE_SIDEBAR_WIDTH = 0.0;
            menuStartButton.hide().removeClass("open");
            setSidebarWidth(CLOSE_SIDEBAR_WIDTH, function() {
                disableSidebarDrag();
                menuStartButton.show();

                var content = $("." + Constants.PAGE_CONTENT_CLASS).html();
                $("body").empty();
                $("body").append(content);
                $(document).off("click.prevent_click_editing");
                Broadcaster.send(Broadcaster.EVENTS.STOP_BLOCKS);
                $("body").append(menuStartButton);

            });
        }

        //Note: by default, the cookie is deleted when the browser is closed:
        $.cookie(Constants.COOKIE_SIDEBAR_STATE, cookieState);
    };

    //check for a cookie and auto-open when the sidebar was active
    var sidebarState = $.cookie(Constants.COOKIE_SIDEBAR_STATE);

    if (sidebarState===SIDEBAR_STATE_SHOW) {
        $(document).ready(function () {
            toggleSidebar(true);
        });
    }


    var setSidebarWidth = function(width, callback) {
        var windowWidth = $(window).width();
        sidebarElement.addClass(Constants.SIDEBAR_ANIMATED_CLASS);
        sidebarElement.css("width", (width) + "px");
        //one() = on() but only once
        sidebarElement.one('webkitTransitionEnd otransitionend oTransitionEnd msTransitionEnd transitionend', function(event) {
            if ($(event.target).hasClass(Constants.PAGE_SIDEBAR_CLASS)) {
                sidebarElement.removeClass(Constants.SIDEBAR_ANIMATED_CLASS);
                $("." + Constants.PAGE_CONTENT_CLASS).css("width", (windowWidth - width) + "px");
                updateContainerWidth();

                if (callback) callback();

            }
        });
    };

    this.setSidebarWidth = function(width, callback) {
        setSidebarWidth(width, callback);
    };



    var enableSidebarDrag = function() {
        $(document).on("mousedown.sidebar_resize", "."+Constants.PAGE_SIDEBAR_RESIZE_CLASS, function() {
            // On mousedown start resizing
            // Make sure we are no longer in edit mode
            Sidebar.reset();

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
                sidebarElement.css("width", ratioSide + "px");
                pageContent.css("width", ratioPage + "px");
            });

            $(document).on("mouseup.sidebar_resize", function() {

                // check size page content
                // find containers and get width
                // if container width is greater then page content width
                // set container width to pagecontent width - 20
                updateContainerWidth();

                $(document).off("mousemove.sidebar_resize");
                $(document).off("mouseup.sidebar_resize");
                DOM.enableSelection();
                DOM.enableContextMenu();
                $("body").removeClass(Constants.FORCE_RESIZE_CURSOR_CLASS);
                Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);

                //Note: by default, the cookie is deleted when the browser is closed:
                $.cookie(Constants.COOKIE_SIDEBAR_WIDTH, sidebarElement.width());
            });
        });
    };

    var disableSidebarDrag = function() {
        $(document).off("mousedown.sidebar_resize");
    };

    /*
     * in bootstrap the containerwidth is fixed. to prevent the container from bleeding
     * into our sidebar, we set the width fixed with a new width, smaller than our page content wrapper
     * */
    var updateContainerWidth = function() {
        var wrapper = $("." + Constants.PAGE_CONTENT_CLASS);
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
    $(window).smartresize(function() {
        if (resizing) {
            var windowWidth = $(window).width();
            $("." + Constants.PAGE_CONTENT_CLASS).css("width", (windowWidth - sidebarWidth) + "px");
            updateContainerWidth();
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



    /*
     * Save button: saves the page
     * */
    $(document).on("click", "."+Constants.SAVE_PAGE_BUTTON, function ()
    {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        Sidebar.reset();
        // remove the widths from the containers
        $(".container").removeAttr("style");
        var page = $("html")[0].outerHTML;
        updateContainerWidth();

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

                    //Annoying when saving a lot...
                    //dialog = new BootstrapDialog({
                    //    type: BootstrapDialog.TYPE_SUCCESS,
                    //    title: 'Page saved',
                    //    message: '<p>The page was succesfully saved.</p>',
                    //    buttons: [
                    //        {
                    //            id: 'btn-close',
                    //            label: 'Ok',
                    //            action: function (dialogRef)
                    //            {
                    //                dialogRef.close();
                    //                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
                    //            }
                    //        }
                    //    ]
                    //});
                    //dialog.open();
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

}]);