base.plugin("blocks.core.Frame", ["blocks.core.Broadcaster", "blocks.core.Notification", "blocks.core.Hover", "blocks.core.DomManipulation", "constants.blocks.core", "blocks.core.Sidebar", "messages.blocks.core", "blocks.core.UI", function (Broadcaster, Notification, Hover, DOM, BlocksConstants, Sidebar, BlocksMessages, UI)
{
    var Frame = this;

    var SIDEBAR_STATE_NULL = "";
    var SIDEBAR_STATE_SHOW = "show";
    var SIDEBAR_STATE_HIDE = "hide";

    var MIN_SIDEBAR_WIDTH = 200;

    //----MORE OR LESS THE START OF EVERYTHING----
    //note: the icon is set in blocks.less
    var menuStartButton = $('<a class="' + BlocksConstants.BLOCKS_START_BUTTON + '"></a>');
    // Hide show bar on click of menu button
    $(document).on("click", "." + BlocksConstants.BLOCKS_START_BUTTON, function (event)
    {
        toggleSidebar($("body").children("." + BlocksConstants.PAGE_CONTENT_CLASS).length == 0);
    });

    // Add the start button as only notice of our presence
    $("body").append(menuStartButton);

    var sidebarElement = $("<div class='" + BlocksConstants.PAGE_SIDEBAR_CLASS + " " + BlocksConstants.PREVENT_BLUR_CLASS + "'></div>");
    sidebarElement.load("/templates/sidebar");

    //check for a cookie and auto-open when the sidebar was active
    var sidebarState = $.cookie(BlocksConstants.COOKIE_SIDEBAR_STATE);
    if (sidebarState === SIDEBAR_STATE_SHOW) {
        $(document).ready(function ()
        {
            toggleSidebar(true);
        });
    }

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
            $(document).on("click.prevent_click_editing", "a, button", function (e)
            {
                var control = $(this);

                //this attribute allows us to let some components pass through after all
                var controlRole = control.attr(BlocksConstants.CLICK_ROLE_ATTR);
                var pierceThrough = controlRole!=null && controlRole==BlocksConstants.FORCE_CLICK_ATTR_VALUE;

                //also check all the parents for that attribute to allow for easy management and grouping
                if (!pierceThrough) {
                    pierceThrough = control.parents('[' + BlocksConstants.FORCE_CLICK_ATTR + ']').length > 0;
                }

                //allow all the buttons in modal dialogs to work as usual
                if (!pierceThrough) {
                    pierceThrough = control.parents('.modal-dialog').length > 0;
                }

                //disable the popup when we're editing text
                if (!pierceThrough) {
                    pierceThrough = control.parents('[contenteditable=true]').length > 0;
                }

                if (pierceThrough) {
                    //NOOP
                }
                //controls in the sidebar are enabled by default
                else if (UI.sidebar.find(control).length>0) {
                    //NOOP
                }
                else {
                    e.preventDefault();
                    Notification.warn(BlocksMessages.clicksDisabledWhileEditing);
                }
            });

            // Get old sidebar width from cookie
            var cookieSidebarWidth = $.cookie(BlocksConstants.COOKIE_SIDEBAR_WIDTH);
            var windowWidth = $(window).width();
            var INIT_SIDEBAR_WIDTH = windowWidth * 0.2; // default width of sidebar is 20% of window
            if (cookieSidebarWidth != null) {
                INIT_SIDEBAR_WIDTH = cookieSidebarWidth;
            }

            //control the bounds, even if the cookie says otherwise
            if (INIT_SIDEBAR_WIDTH<MIN_SIDEBAR_WIDTH) {
                INIT_SIDEBAR_WIDTH = MIN_SIDEBAR_WIDTH;
            }

            menuStartButton.addClass("open");

            //slide open the sidebar and activate the callback when finished
            Sidebar.animateSidebarWidth(INIT_SIDEBAR_WIDTH, function (event)
            {
                //re-add the button (but with a changed icon)
                $("body").append(menuStartButton);
                enableSidebarDrag();
                Broadcaster.send(Broadcaster.EVENTS.START_BLOCKS, event);
            });

        } else {
            cookieState = SIDEBAR_STATE_HIDE;
            var CLOSE_SIDEBAR_WIDTH = 0.0;
            menuStartButton.hide().removeClass("open");
            Sidebar.animateSidebarWidth(CLOSE_SIDEBAR_WIDTH, function (event)
            {
                disableSidebarDrag();
                menuStartButton.show();

                var content = $("." + BlocksConstants.PAGE_CONTENT_CLASS).html();
                $("body").empty();
                $("body").append(content);
                $(document).off("click.prevent_click_editing");
                Broadcaster.send(Broadcaster.EVENTS.STOP_BLOCKS, event);
                $("body").append(menuStartButton);

                $("body").removeClass(BlocksConstants.BODY_EDIT_MODE_CLASS);
            });
        }

        //Note: by default, the cookie is deleted when the browser is closed:
        $.cookie(BlocksConstants.COOKIE_SIDEBAR_STATE, cookieState);
    };

    var enableSidebarDrag = function ()
    {
        $(document).on("mousedown.sidebar_resize", "." + BlocksConstants.PAGE_SIDEBAR_RESIZE_CLASS, function (event)
        {
            //TODO IS THIS NECESSARY?
            //// On mousedown start resizing
            //// Make sure we are no longer in edit mode
            //Sidebar.reset();
            //
            //DOM.disableTextSelection();
            //DOM.disableContextMenu();

            //needed because sometimes we hover out of the dragger while moving the sidebar (because of some lag)
            $("body").addClass(BlocksConstants.FORCE_RESIZE_CURSOR_CLASS);

            var windowWidth = $(window).width();
            var pageContent = $("." + BlocksConstants.PAGE_CONTENT_CLASS);
            $(document).on("mousemove.sidebar_resize", function (event)
            {
                var X = event.pageX;
                var sideWidth = windowWidth - X;
                var pageWidth = windowWidth - sideWidth;
                if (sideWidth > MIN_SIDEBAR_WIDTH && pageWidth > MIN_SIDEBAR_WIDTH) {
                    sidebarElement.css("width", sideWidth + "px");
                    pageContent.css("width", pageWidth + "px");

                    //tried to alter the viewport dynamically, but it didn't work (yet?) as expected...
                    //var viewportSuffix = ', initial-scale=1.0, maximum-scale=1.0, user-scalable=0';
                    //$('head meta[name=viewport]').attr('content', 'width='+pageWidth+viewportSuffix);
                    ////Logger.debug($('meta[name=viewport]').attr('content'));

                    //to be caught by eg. the finder layouter
                    Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
                }
            });

            $(document).on("mouseup.sidebar_resize", function (event)
            {
                $(document).off("mousemove.sidebar_resize");
                $(document).off("mouseup.sidebar_resize");

                $("body").removeClass(BlocksConstants.FORCE_RESIZE_CURSOR_CLASS);

                //TODO IS THIS NECESSARY?
                //DOM.enableTextSelection();
                //DOM.enableContextMenu();
                //Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD, event);

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
        //TODO this is dangerous to blindly do this
        containers.removeAttr("style");
        if (wrapper.length > 0) {
            var wrapperWidth = wrapper.outerWidth();
            var containerWidth = containers.outerWidth();
            if (containerWidth > wrapperWidth) {
                //let's keep a small margin between the website and our sidebar
                containers.css("width", (wrapperWidth - BlocksConstants.SIDEBAR_MARGIN_LEFT_PX) + "px");
            }
        }
    };

    // On Window resize
    var sidebarWidth = sidebarElement.outerWidth();
    var resizing = false;
    $(window).smartresize(function (event)
    {
        if (resizing) {
            var windowWidth = $(window).width();
            $("." + BlocksConstants.PAGE_CONTENT_CLASS).css("width", (windowWidth - sidebarWidth) + "px");
            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE, event);
            resizing = false;
        }
    });

    $(window).on("resize.blocks_broadcaster", function (event)
    {
        if (resizing == false) {
            // Leave edit mode
            //Sidebar.resetOld();
            sidebarWidth = sidebarElement.outerWidth();
            Hover.removeHoverOverlays();
            Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE, event);
            resizing = true;
        }
    });

    //before updating the layout, make sure the container width is set properly
    $(document).on(Broadcaster.EVENTS.WILL_REFRESH_LAYOUT, function (event)
    {
        // check size page content
        // find containers and get width
        // if container width is greater then page content width
        // set container width to pagecontent width - 20
        updateContainerWidth();
    });

    /*
     * Save button: saves the page
     * */
    $(document).on("click", "." + BlocksConstants.SAVE_PAGE_BUTTON, function (event)
    {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE, event);
        //Sidebar.resetOld();
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
    $(document).on("click", "." + BlocksConstants.DELETE_PAGE_BUTTON, function (event)
    {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE, event);
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
            type: BootstrapDialog.TYPE_DANGER,
            message: "<div>Do you want to delete this page and all it's translations?</div>",
            buttons: [
                {
                    id: 'btn-close',
                    label: 'Cancel',
                    action: function (dialogRef)
                    {
                        dialogRef.close();
                    }
                },
                {
                    id: 'btn-ok',
                    label: 'Ok',
                    cssClass: 'btn-danger',
                    action: function (dialogRef)
                    {
                        onConfirm();
                        dialogRef.close();
                    }

                }
            ],
            onhide: function()
            {
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            }
        });

    });

    //TODO SETUP THE KEYBOARD SHORTCUTS (messed up the editor)
    //$(document).keydown(function (e)
    //{
    //    var retVal = true;
    //
    //    //$.ui.keyCode.S
    //    var btn;
    //    if (e) {
    //        if (e.ctrlKey) {
    //            switch (e.which) {
    //                //Ctrl+S
    //                case 83:
    //                    btn = $("." + BlocksConstants.SAVE_PAGE_BUTTON);
    //                    break;
    //            }
    //        }
    //        else {
    //            switch (e.which) {
    //                //DELETE
    //                case 46:
    //                    btn = $("." + BlocksConstants.DELETE_PAGE_BUTTON);
    //                    break;
    //            }
    //        }
    //    }
    //
    //    if (btn) {
    //        if (btn.is(":visible")) {
    //            btn.click();
    //            event.preventDefault();
    //            retVal = false;
    //        }
    //    }
    //
    //    return retVal;
    //});

}]);