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

base.plugin("blocks.core.Frame", ["blocks.core.Broadcaster", "blocks.core.Notification", "blocks.core.Hover", "blocks.core.DOM", "constants.blocks.core", "blocks.core.Sidebar", "messages.blocks.core", "blocks.core.UI", function (Broadcaster, Notification, Hover, DOM, BlocksConstants, Sidebar, BlocksMessages, UI)
{
    var Frame = this;

    var SIDEBAR_STATE_NULL = "";
    var SIDEBAR_STATE_SHOW = BlocksConstants.PAGE_SIDEBAR_COOKIE_SHOW;
    var SIDEBAR_STATE_HIDE = BlocksConstants.PAGE_SIDEBAR_COOKIE_HIDE;
    //Note: an empty paths means: take the path of the current page
    var DEFAULT_COOKIE_OPTIONS = BlocksConstants.PAGE_SIDEBAR_COOKIE_OPTIONS;

    var MIN_SIDEBAR_WIDTH = 200;

    //note that because we set a container width on the blocks-layout in some styles (eg. sticky footers and full background-colors),
    //we need to scale it along with the container inside it
    var CONTAINERS_SELECTOR = ".container, blocks-layout";

    this.KEY_CODE_SHIFT = 16;

    //-----VARIABLES-----
    var keysPressed = [];

    //----MORE OR LESS THE START OF EVERYTHING----
    // Add the start button as only notice of our presence
    //note: the icon is set in blocks.less
    UI.startButton = $('<a class="' + BlocksConstants.BLOCKS_START_BUTTON + '"></a>')
        .attr(BlocksConstants.CLICK_ROLE_ATTR, BlocksConstants.FORCE_CLICK_ATTR_VALUE)
        .appendTo(UI.body);

    // Hide/show sidebar when menu button is clicked
    UI.startButton.on("click", function (event)
    {
        toggleSidebar(UI.body.find("." + BlocksConstants.PAGE_CONTENT_CLASS).length == 0);
    });

    UI.sidebar = $("<div class='" + BlocksConstants.PAGE_SIDEBAR_CLASS + " " + BlocksConstants.PREVENT_BLUR_CLASS + "'></div>");
    UI.sidebar.load(BlocksConstants.SIDEBAR_ENDPOINT, function (response, status, xhr)
    {
        if (status == 'success') {
            //check for a cookie and auto-open when the sidebar was active
            if (Cookies.get(BlocksConstants.COOKIE_SIDEBAR_STATE) === SIDEBAR_STATE_SHOW) {
                $(document).ready(function ()
                {
                    toggleSidebar(true);
                });
            }
        }
        else {
            Notification.error(msg + xhr.status + " " + xhr.statusText, xhr);
        }
    });

    var toggleSidebar = function (show)
    {
        var cookieState = SIDEBAR_STATE_NULL;

        if (show) {

            // Remove the menu button while animating sidebar
            //note that we remove it because we'll call body.empty() below
            UI.startButton.detach();

            // Needs some explaining:
            // If we wrap the body with our blocks-page-content element,
            // the scripts at the bottom of the page seem to cause errors.
            // So we will pull them out of the body and insert them as siblings of the body.
            // Note that this is designed to wrap scripts that don't render anything out, so watch out
            // if you use it for other purposes; it will break the design/behaviour of the sidebar.
            var ignoredBody = UI.body.find('.' + BlocksConstants.PAGE_IGNORE_CLASS);
            // We'll create a placeholder for the ignored body, just after it (while keeping the reference to the original in the variable)
            // (note that we're re-using the same class for the placeholder) so we can look it up in the body and put it back
            // in the same spot when the sidebar is closed
            ignoredBody.after('<div class="' + BlocksConstants.PAGE_IGNORE_CLASS + '" />');
            //detach is like remove() but without the releasing of memory structures
            ignoredBody.detach();

            //wrap the contents of the body in a separate wrapper element, so we can add the surfaces and sidebar too
            UI.pageContent = $('<div class="' + BlocksConstants.PAGE_CONTENT_CLASS + '" />');
            UI.pageContent.append(UI.body.children().detach());
            UI.body.append(UI.pageContent);
            UI.body.addClass(BlocksConstants.BODY_EDIT_MODE_CLASS);
            //temporarily put them here
            UI.body.append(ignoredBody);
            UI.body.append(UI.sidebar);

            UI.overlayWrapper = $('<div class="' + BlocksConstants.BLOCK_OVERLAY_WRAPPER_CLASS + '"/>').appendTo(UI.body);
            UI.surfaceWrapper = $('<div class="' + BlocksConstants.BLOCK_SURFACE_WRAPPER_CLASS + '"/>').appendTo(UI.overlayWrapper);
            UI.handleWrapper = $('<div class="' + BlocksConstants.BLOCK_HANDLE_WRAPPER_CLASS + '"/>').appendTo(UI.overlayWrapper);
            UI.dropspotWrapper = $('<div class="' + BlocksConstants.BLOCK_DROPSPOT_WRAPPER_CLASS + '"/>').appendTo(UI.overlayWrapper);

            //set up perfect-scrollbar.js
            if (jQuery().perfectScrollbar) {
                //only scroll from the tab content so the header doesn't scroll away
                UI.sidebar.find('.' + BlocksConstants.SIDEBAR_CONTAINER_CLASS).perfectScrollbar();
            }

            // Prevent clicking on links while in editing mode
            // Note: after trying mousedown or mouseup to prevent vanishing links from triggering the modal,
            // it's quite important this is effectively the click event, because it overloads a lot of necessary other clicks
            // Use the pierce trough class to work around it
            var preventEditNamespace = 'prevent_click_editing';
            $(document).on("click." + preventEditNamespace, "a, button", function (e)
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

                //TODO unchecked
                //check if we clicked on the link, or on something inside a link
                //and pass through if we didn't click on a link itself
                // if (!pierceThrough) {
                //     if (!control.is($(this))) {
                //         pierceThrough = true;
                //     }
                // }

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

            cookieState = SIDEBAR_STATE_SHOW;
            //transform the button to a closing cross
            //slide open the sidebar and activate the callback when finished
            Sidebar.setWidth(INIT_SIDEBAR_WIDTH, function (event)
            {
                //re-add the button (but with a changed icon)
                UI.startButton.addClass("open").appendTo(UI.body);

                //allow the sidebar to be resized
                enableSidebarDrag();

                // should we warn the user if she navigates away (possibly without saving)?
                if (BlocksConstants.ENABLE_LEAVE_EDIT_CONFIRM_CONFIG == 'true') {
                    window.onbeforeunload = function (e)
                    {
                        // Cancel the event as stated by the standard.
                        e.preventDefault();
                        // Chrome requires returnValue to be set.
                        e.returnValue = '';
                        //most browsers will ignore this message
                        return BlocksMessages.leavePageConfirmation;
                    };
                }

                //Make sure to initialize all the elements before firing the event
                //because a lot of boot-code will use them
                UI.init();

                Broadcaster.send(Broadcaster.EVENTS.START_BLOCKS, event);
            });

        }
        //hide the sidebar
        else {

            //make sure all focused blocks are blurred in a clean manner
            Sidebar.reset();

            cookieState = SIDEBAR_STATE_HIDE;
            var CLOSE_SIDEBAR_WIDTH = 0.0;
            //hide the button while animating
            UI.startButton.removeClass("open").detach();
            Sidebar.setWidth(CLOSE_SIDEBAR_WIDTH, function (event)
            {
                //don't allow the sidebar to be resized
                disableSidebarDrag();

                //disable navigate-away warning
                if (BlocksConstants.ENABLE_LEAVE_EDIT_CONFIRM_CONFIG == 'true') {
                    window.onbeforeunload = undefined;
                }

                var content = $('.' + BlocksConstants.PAGE_CONTENT_CLASS);

                //this will select all (original) ignored content tags, excluding the placeholders
                var ignoredContent = UI.body.find('.' + BlocksConstants.PAGE_IGNORE_CLASS + ':not(.' + BlocksConstants.PAGE_CONTENT_CLASS + ' .' + BlocksConstants.PAGE_IGNORE_CLASS + ')');
                ignoredContent.detach();

                var content = content.html();
                UI.body.empty();
                UI.body.append(content);

                //this will loop the ignored content and put them back in the placeholders in-order
                UI.body.find('.' + BlocksConstants.PAGE_IGNORE_CLASS).each(function (idx)
                {
                    $(this).replaceWith(ignoredContent[idx]);
                });

                $(document).off("click." + preventEditNamespace);
                UI.body.append(UI.startButton);
                UI.body.removeClass(BlocksConstants.BODY_EDIT_MODE_CLASS);

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
            //needed because sometimes we hover out of the dragger while moving the sidebar (because of some lag)
            UI.body.addClass(BlocksConstants.FORCE_RESIZE_CURSOR_CLASS);

            var windowWidth = $(window).width();
            var pageContent = $("." + BlocksConstants.PAGE_CONTENT_CLASS);
            $(document).on("mousemove.sidebar_resize", function (event)
            {
                var x = event.pageX;
                var sideWidth = windowWidth - x;
                var pageWidth = windowWidth - sideWidth;
                if (sideWidth > MIN_SIDEBAR_WIDTH && pageWidth > MIN_SIDEBAR_WIDTH) {
                    UI.sidebar.css("width", sideWidth + "px");
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

                UI.body.removeClass(BlocksConstants.FORCE_RESIZE_CURSOR_CLASS);

                //Note: by default, the cookie is deleted when the browser is closed:
                Cookies.set(BlocksConstants.COOKIE_SIDEBAR_WIDTH, UI.sidebar.width(), DEFAULT_COOKIE_OPTIONS);
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

    // On window resize (smart means the events are debounced, see dom.js)
    var sidebarWidth = UI.sidebar.outerWidth();
    var resizing = false;
    $(window).smartresize(function (event)
    {
        //boots the resizing
        if (!resizing) {
            sidebarWidth = UI.sidebar.outerWidth();
            //Hover.removeHoverOverlays();
            Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, event);
            resizing = true;
        }
        else {
            var windowWidth = $(window).width();
            $("." + BlocksConstants.PAGE_CONTENT_CLASS).css("width", (windowWidth - sidebarWidth) + "px");
            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
            Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS, event);
            resizing = false;
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
        var contentWidth = $(window).width() - UI.sidebar.outerWidth();
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
        Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, event);

        //the idea is to send the entire page to the server and let it only save the correct tags (eg. with property and data-property attributes)
        // remove the widths from the containers
        $(CONTAINERS_SELECTOR).removeAttr("style");

        //the sidebar is open now. We used to send everything to the server, letting it to handle the sidebar HTML code on its own,
        // but it's too much hassle and too simple for us to 'close' the sidebar now. So let's just take the html in the wrapper and create
        // a virtual html page by combining the content of the wrapper with the <head> in the html

        //clear the manual container width (we'll re-set it back later)
        clearContainerWidth();

        //clear special classes for disabling selection of text when the sidebar is open (will be reset later)
        DOM.enableTextSelection(true);
        //same reason as above; make sure this doesn't get saved accidentally
        DOM.enableContextMenu(true);

        //create a new node out of the full page html
        var savePage = $("html").clone();

        //this extracts the real body (without the sidebar code) we need to save
        //see toggle close for more or less the same code
        //TODO ideally, we should make this uniform (virtually close the sidebar?)
        var container = savePage.find("." + BlocksConstants.PAGE_CONTENT_CLASS);
        //we modify the width property of the body while resizing the sidebar; make sure it doesn't get saved
        container.css("width", "");
        var content = container.html();
        var bodyCopy = savePage.find("body");
        bodyCopy.empty();
        bodyCopy.append(content);
        bodyCopy.removeClass(BlocksConstants.BODY_EDIT_MODE_CLASS);

        //convert from jQuery to html string
        savePage = savePage[0].outerHTML;

        //reset what we cleared above
        updateContainerWidth();
        DOM.enableTextSelection(false);

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
            .done(function (data, textStatus, response)
            {
            })
            .fail(function (xhr, textStatus, exception)
            {
                Notification.error(BlocksMessages.savePageError + (exception ? "; " + exception : ""), xhr);
            })
            .always(function ()
            {
                dialog.close();
                Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS);
            });
    });

    /*
     * Delete button: deletes the page
     * */
    $(document).on("click", "." + BlocksConstants.DELETE_PAGE_BUTTON, function (event)
    {
        Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, event);
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
                    if (BlocksConstants.ENABLE_LEAVE_EDIT_CONFIRM_CONFIG == 'true') {
                        window.onbeforeunload = undefined;
                    }

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
                Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS);
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