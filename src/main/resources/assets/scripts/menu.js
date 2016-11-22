base.plugin("blocks.core.Frame", ["blocks.core.Broadcaster", "blocks.core.Notification", "blocks.core.Hover", "blocks.core.DomManipulation", "constants.blocks.core", "blocks.core.Sidebar", "messages.blocks.core", "blocks.core.UI", function (Broadcaster, Notification, Hover, DOM, BlocksConstants, Sidebar, BlocksMessages, UI)
{
    var Frame = this;

    var SIDEBAR_STATE_NULL = "";
    var SIDEBAR_STATE_SHOW = "show";
    var SIDEBAR_STATE_HIDE = "hide";
    //Note: an empty paths means: take the path of the current page
    var DEFAULT_COOKIE_OPTIONS = {path: '/'};

    var MIN_SIDEBAR_WIDTH = 200;

    //note that because we set a container width on the blocks-layout in some styles (eg. sticky footers and full background-colors),
    //we need to scale it along with the container inside it
    var CONTAINERS_SELECTOR = ".container, blocks-layout";

    this.KEY_CODE_SHIFT = 16;

    //-----VARIABLES-----
    var keysPressed = [];

    //----MORE OR LESS THE START OF EVERYTHING----
    //note: the icon is set in blocks.less
    var menuStartButton = $('<a class="' + BlocksConstants.BLOCKS_START_BUTTON + '"></a>');
    menuStartButton.attr(BlocksConstants.CLICK_ROLE_ATTR, BlocksConstants.FORCE_CLICK_ATTR_VALUE);
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
    var sidebarState = Cookies.get(BlocksConstants.COOKIE_SIDEBAR_STATE);
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

            //about to start up the side bar and modify the HTML
            Broadcaster.send(Broadcaster.EVENTS.PRE_START_BLOCKS);

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
                //this is needed (instead of $(this)) to detect the [contenteditable]
                var control = $(e.target);

                //this attribute allows us to let some components pass through after all
                var pierceThrough = false;

                //also check all the parents for that attribute to allow for easy management and grouping
                if (!pierceThrough) {
                    pierceThrough = control.is('[' + BlocksConstants.FORCE_CLICK_ATTR + ']') || control.parents('[' + BlocksConstants.FORCE_CLICK_ATTR + ']').length > 0;
                }

                //allow all the buttons in modal dialogs to work as usual
                if (!pierceThrough) {
                    pierceThrough = control.parents('.modal-dialog').length > 0;
                }

                //disable the popup when we're editing text
                if (!pierceThrough) {
                    pierceThrough = control.is('[contenteditable=true]') || control.parents('[contenteditable=true]').length > 0;
                }

                if (!pierceThrough) {
                    //controls in the sidebar are enabled by default
                    if (UI.sidebar) {
                        pierceThrough = UI.sidebar.find(control).length > 0;
                    }
                }

                //check if we clicked on the link, or on something inside a link
                //and pass through if we didn't click on a link itself
                if (!pierceThrough) {
                    if (!control.is($(this))) {
                        pierceThrough = true;
                    }
                }

                //if shift is pressed, allow parse through (allow for easy navigation when you know what you're doing)
                if (!pierceThrough) {
                    pierceThrough = Frame.isKeyPressed(Frame.KEY_CODE_SHIFT);
                }

                if (pierceThrough) {
                    //NOOP
                }
                else {
                    e.preventDefault();
                    Notification.warn(BlocksMessages.clicksDisabledWhileEditing);
                }

                return !pierceThrough;
            });

            // Get old sidebar width from cookie
            var cookieSidebarWidth = Cookies.get(BlocksConstants.COOKIE_SIDEBAR_WIDTH);
            //make sure the value is OK and cleanup if not
            if (!$.isNumeric(cookieSidebarWidth)) {
                cookieSidebarWidth = null;
                Cookies.remove(BlocksConstants.COOKIE_SIDEBAR_WIDTH, DEFAULT_COOKIE_OPTIONS);
            }
            else {
                cookieSidebarWidth = parseInt(cookieSidebarWidth);
            }

            var windowWidth = $(window).width();
            var INIT_SIDEBAR_WIDTH = windowWidth * 0.2; // default width of sidebar is 20% of window
            if (cookieSidebarWidth != null && cookieSidebarWidth > 0) {
                INIT_SIDEBAR_WIDTH = cookieSidebarWidth;
            }

            //control the bounds, even if the cookie says otherwise
            if (INIT_SIDEBAR_WIDTH < MIN_SIDEBAR_WIDTH) {
                INIT_SIDEBAR_WIDTH = MIN_SIDEBAR_WIDTH;
            }

            menuStartButton.addClass("open");

            //slide open the sidebar and activate the callback when finished
            Sidebar.animateSidebarWidth(INIT_SIDEBAR_WIDTH, function (event)
            {
                //re-add the button (but with a changed icon)
                $("body").append(menuStartButton);
                enableSidebarDrag();

                //give ourself and the animation some time to settle before sending out the event
                //Note: not anymore, fixed it
                setTimeout(function ()
                {
                    Broadcaster.send(Broadcaster.EVENTS.START_BLOCKS, event);
                }, 0);
            });

        } else {
            //about to stop up the side bar and modify the HTML
            Broadcaster.send(Broadcaster.EVENTS.PRE_STOP_BLOCKS);

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
                $("body").append(menuStartButton);
                $("body").removeClass(BlocksConstants.BODY_EDIT_MODE_CLASS);

                clearContainerWidth();

                Broadcaster.send(Broadcaster.EVENTS.STOP_BLOCKS, event);
            });
        }

        //Note: by default, the cookie is deleted when the browser is closed:
        Cookies.set(BlocksConstants.COOKIE_SIDEBAR_STATE, cookieState, DEFAULT_COOKIE_OPTIONS);
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
                Cookies.set(BlocksConstants.COOKIE_SIDEBAR_WIDTH, sidebarElement.width(), DEFAULT_COOKIE_OPTIONS);
            });
        });
    };

    var disableSidebarDrag = function ()
    {
        $(document).off("mousedown.sidebar_resize");
    };

    /*
     * in bootstrap the containerwidth is fixed. to prevent the container from bleeding
     * into our sidebar, we set the width fixed with a new width, smaller than our page content wrapper.
     * Sync with method below.
     */
    var updateContainerWidth = function ()
    {
        var wrapper = $("." + BlocksConstants.PAGE_CONTENT_CLASS);
        var containers = $(CONTAINERS_SELECTOR);

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
    //method to clear the manual container width from above; sync them
    var clearContainerWidth = function ()
    {
        $(CONTAINERS_SELECTOR).css("width", "");
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

    $(document).on(Broadcaster.EVENTS.DOM_CHANGED, function (event)
    {
        var wrapper = $("." + BlocksConstants.PAGE_CONTENT_CLASS);

        //it's possible the content of the sidebar made it grow/shrink;
        //this will alter the content wrapper class if it did (and fire a re-layout)
        var contentWidth = $(window).width() - sidebarElement.outerWidth();
        if (wrapper.outerWidth() != contentWidth) {
            wrapper.css("width", contentWidth + "px");
            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
        }
    });

    /*
     * Save button: saves the page
     * */
    $(document).on("click", "." + BlocksConstants.SAVE_PAGE_BUTTON, function (event)
    {
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE, event);

        //the idea is to send the entire page to the server and let it only save the correct tags (eg. with property and data-property attributes)
        // remove the widths from the containers
        $(CONTAINERS_SELECTOR).removeAttr("style");

        //the sidebar is open now. We used to send everything to the server, letting it to handle the sidebar HTML code on its own,
        // but it's too much hassle and too simple for us to 'close' the sidebar now. So let's just take the html in the wrapper and create
        // a virtual html page by combining the content of the wrapper with the <head> in the html

        //clear the manual container width (we'll re-set it back later)
        clearContainerWidth();

        //clear special classes for disabling selection of text when the sidebar is open (will be reset later)
        DOM.enableTextSelection();

        //create a new node out of the full page html
        var savePage = $("html").clone();

        //this extracts the real body (without the sidebar code) we need to save
        //see toggle close for more or less the same code
        //TODO ideally, we should make this uniform (virtually close the sidebar?)
        var container = savePage.find("." + BlocksConstants.PAGE_CONTENT_CLASS);
        //we modify the width property of the body while resizing the sidebar; make sure it doesn't get saved
        container.css("width", "");
        var content = container.html();
        var body = savePage.find("body");
        body.empty();
        body.append(content);
        body.removeClass(BlocksConstants.BODY_EDIT_MODE_CLASS);

        //convert from jQuery to html string
        savePage = savePage[0].outerHTML;

        //reset what we cleared cleared it above
        updateContainerWidth();
        DOM.disableTextSelection();

        var dialog = new BootstrapDialog({
            type: BootstrapDialog.TYPE_PRIMARY,
            title: 'Saving ...',
            message: 'Please wait while we save the page. This can take a few seconds.',
            buttons: []
        });

        dialog.open();

        $.ajax({
            type: 'POST',
            url: "/blocks/admin/page/save?url=" + encodeURIComponent(document.URL),
            data: savePage,
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
            var dialog = new BootstrapDialog({
                type: BootstrapDialog.TYPE_PRIMARY,
                title: 'Deleting ...',
                message: 'Please wait while we delete the page. This can take a few seconds.',
                buttons: []
            });

            dialog.open();

            $.ajax({
                type: 'DELETE',
                url: "/blocks/admin/page/delete",
                data: document.URL,
                contentType: 'application/json; charset=UTF-8',
            })
                .done(function (url, textStatus, response)
                {
                    if (url) {
                        window.location = url;
                    } else {
                        location.reload();
                    }
                })
                .fail(function (xhr, textStatus, exception)
                {
                    var message = response.status == 400 ? response.responseText : "An error occurred while deleting the page.";
                    Notification.dialog("Error", "<div>" + message + "</div>", function ()
                    {
                    });
                })
                .always(function ()
                {
                    dialog.close();
                });
        };

        BootstrapDialog.show({
            title: "Delete page",
            type: BootstrapDialog.TYPE_DANGER,
            message: "<div>Are you sure you want to delete this page?</div>",
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
            onhide: function ()
            {
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            }
        });

    });

    $(document).on("keyup keydown", function (e)
    {
        switch (e.type) {
            case "keydown" :
                keysPressed.push(e.keyCode);
                break;
            case "keyup" :
                var idx = keysPressed.indexOf(e.keyCode);
                if (idx >= 0) {
                    keysPressed.splice(idx, 1);
                }
                break;
        }
    });

    this.isKeyPressed = function (code)
    {
        return keysPressed.indexOf(code) >= 0;
    };

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