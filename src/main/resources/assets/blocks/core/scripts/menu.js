/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    sidebarElement.load(BlocksConstants.SIDEBAR_ENDPOINT, function (response, status, xhr)
    {
        if (status == "error") {
            Notification.error(msg + xhr.status + " " + xhr.statusText, xhr);
        }
        else {
            //check for a cookie and auto-open when the sidebar was active
            var sidebarState = Cookies.get(BlocksConstants.COOKIE_SIDEBAR_STATE);
            if (sidebarState === SIDEBAR_STATE_SHOW) {
                $(document).ready(function ()
                {
                    toggleSidebar(true);
                });
            }
        }
    });

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
            var body = $("body");

            // Needs some explaining:
            // If we wrap the body with our blocks-page-content element,
            // the scripts at the bottom of the page seem to cause errors.
            // So we will pull them out of the body and insert them as siblings of the body.
            // Note that this is designed to wrap scripts that don't render anything out, so watch out
            // if you use it for other purposes; it will break the design/behaviour of the sidebar.
            var ignoredBody = body.find('.' + BlocksConstants.PAGE_IGNORE_CLASS);
            // When closing the sidebar, we need in-order insert-placeholders, so insert those here.
            ignoredBody.after('<div class="' + BlocksConstants.PAGE_IGNORE_CLASS + '" />');
            //detach is like remove() but without the releasing of memory structures
            ignoredBody.detach();

            //Note: this means all previously installed event handlers will be lost!
            //We should actually migrate to "body.clone(true).unwrap()" but the JS of the modules are not ready for that yet...
            var bodyHtml = body.html();
            body.empty();
            //wrap the content of the body in the class and add that again to the body
            body.append($('<div class="' + BlocksConstants.PAGE_CONTENT_CLASS + '" />').append(bodyHtml));
            //temporarily put them here
            body.append(ignoredBody);
            body.append(sidebarElement);
            body.addClass(BlocksConstants.BODY_EDIT_MODE_CLASS);

            //set up perfect-scrollbar.js
            if (jQuery().perfectScrollbar) {
                //only scroll from the tab content so the header doesn't scroll away
                sidebarElement.find('.' + BlocksConstants.SIDEBAR_CONTAINER_CLASS).perfectScrollbar();
            }

            // Prevent clicking on links while in editing mode
            // Note: after trying mousedown or mouseup to prevent vanishing links from triggering the modal,
            // it's quite important this is effectively the click event, because it overloads a lot of necessary other clicks
            // Use the pierce trough class to work around it
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

                //since the selector of this handler only manages <a> and <button>, we only have two options
                var newLocation = null;

                //if shift is pressed, allow parse through (allow for easy navigation when you know what you're doing)
                if (!pierceThrough) {
                    pierceThrough = Frame.isKeyPressed(Frame.KEY_CODE_SHIFT);
                    if (pierceThrough) {
                        var tag = control.prop("tagName").toLowerCase();
                        //for now, we only support regular links, because buttons are harder to implement...
                        if (tag == "a") {
                            newLocation = control.attr("href");
                        }
                        else {
                            pierceThrough = false;
                            Logger.warn("Unsupported tag name encountered while handling a force-click; " + tag);
                        }
                    }
                }

                if (pierceThrough) {
                    //cut developers some slack, can't count the times I had to debug a
                    //link and ended up here...
                    Logger.info("Clicked on a link that had pierce-through set", e);

                    if (newLocation) {
                        window.location = newLocation;
                    }
                }
                else {
                    Notification.warn(BlocksMessages.clicksDisabledWhileEditing);
                }

                //See http://api.jquery.com/on/
                //Returning false from an event handler will automatically call
                // event.stopPropagation() and event.preventDefault().
                return pierceThrough;
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

        }
        //hide the sidebar
        else {
            //about to stop up the side bar and modify the HTML
            Broadcaster.send(Broadcaster.EVENTS.PRE_STOP_BLOCKS);

            //make sure all focused blocks are blurred in a clean manner
            Sidebar.reset();

            cookieState = SIDEBAR_STATE_HIDE;
            var CLOSE_SIDEBAR_WIDTH = 0.0;
            menuStartButton.hide().removeClass("open");
            Sidebar.animateSidebarWidth(CLOSE_SIDEBAR_WIDTH, function (event)
            {
                disableSidebarDrag();
                menuStartButton.show();

                var body = $("body");
                var content = $('.' + BlocksConstants.PAGE_CONTENT_CLASS);

                //this will select all (original) ignored content tags, excluding the placeholders
                var ignoredContent = body.find('.' + BlocksConstants.PAGE_IGNORE_CLASS + ':not(.' + BlocksConstants.PAGE_CONTENT_CLASS + ' .' + BlocksConstants.PAGE_IGNORE_CLASS + ')');
                ignoredContent.detach();

                var content = content.html();
                body.empty();
                body.append(content);

                //this will loop the ignored content and put them back in the placeholders in-order
                body.find('.' + BlocksConstants.PAGE_IGNORE_CLASS).each(function (idx)
                {
                    $(this).replaceWith(ignoredContent[idx]);
                });

                $(document).off("click.prevent_click_editing");
                body.append(menuStartButton);
                body.removeClass(BlocksConstants.BODY_EDIT_MODE_CLASS);

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
        //same reason as above; make sure this doesn't get saved accidentally
        DOM.enableContextMenu();

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
            title: BlocksMessages.savePageDialogTitle,
            message: BlocksMessages.savePageDialogMessage,
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
        var onConfirm = function (deleteAllTranslations)
        {
            var dialog = new BootstrapDialog({
                type: BootstrapDialog.TYPE_DANGER,
                title: BlocksMessages.deletingPageDialogTitle,
                message: BlocksMessages.deletingPageDialogMessage,
                buttons: []
            });

            dialog.open();

            $.ajax({
                type: 'DELETE',
                url: deleteAllTranslations ? BlocksConstants.DELETE_PAGE_ALL_ENDPOINT : BlocksConstants.DELETE_PAGE_ENDPOINT,
                data: document.URL,
                contentType: 'application/json; charset=UTF-8',
            })
                .done(function (url, textStatus, response)
                {
                    if (url) {
                        window.location = url;
                    }
                    else {
                        location.reload();
                    }
                })
                .fail(function (xhr, textStatus, exception)
                {
                    dialog.close();
                    Notification.error(BlocksMessages.deletingPageErrorMessage + (exception ? "; " + exception : ""), xhr);
                })
                .always(function ()
                {
                    //Note: we don't close it here, but in the fail() instead,
                    // because the done() does a redirect and thus displays the message all
                    // the way to the end
                    //dialog.close();
                });
        };

        BootstrapDialog.show({
            title: BlocksMessages.deletePageDialogTitle,
            type: BootstrapDialog.TYPE_DANGER,
            message: BlocksMessages.deletePageDialogMessage,
            buttons: [
                {
                    id: 'btn-ok-single',
                    label: BlocksMessages.deletePageDialogConfirmSingle,
                    cssClass: 'btn-danger',
                    action: function (dialogRef)
                    {
                        onConfirm(false);
                        dialogRef.close();
                    }

                },
                {
                    id: 'btn-ok-all',
                    label: BlocksMessages.deletePageDialogConfirmAll,
                    cssClass: 'btn-danger',
                    action: function (dialogRef)
                    {
                        onConfirm(true);
                        dialogRef.close();
                    }

                },
                {
                    id: 'btn-close',
                    label: BlocksMessages.cancel,
                    action: function (dialogRef)
                    {
                        dialogRef.close();
                    }
                },
            ],
            onhide: function ()
            {
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
            }
        });

    });

    this.isKeyPressed = function (code)
    {
        return keysPressed[code] === true;
    };

    $(document).on("keyup keydown", function (e)
    {
        var KEYCODE_SHIFT = 16;
        var KEYCODE_CTRL = 17;
        var KEYCODE_TAB = 9;
        var KEYCODE_DELETE = 46;
        var KEYCODE_ALT = 18;
        var KEYCODE_SPACE = 32;
        var KEYCODE_BACKSPACE = 8;
        var KEYCODE_ESC = 27;
        var KEYCODE_LEFT = 37;
        var KEYCODE_UP = 38;
        var KEYCODE_RIGHT = 39;
        var KEYCODE_DOWN = 40;

        var KEYCODE_S = 83;

        switch (e.type) {
            case "keydown" :
                keysPressed[e.keyCode] = true;
                break;
            case "keyup" :
                //Logger.info("key up: "+e.keyCode);
                keysPressed[e.keyCode] = false;
                break;
        }

        var btn;
        //disabled for now
        // if (Frame.isKeyPressed(KEYCODE_CTRL) && Frame.isKeyPressed(KEYCODE_S)) {
        //     btn = $("." + BlocksConstants.SAVE_PAGE_BUTTON);
        // }

        if (btn) {
            if (btn.is(":visible")) {
                btn.click();
                e.preventDefault();
            }
        }
    });

}]);