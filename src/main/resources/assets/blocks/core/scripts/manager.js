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

/**
 * The manager is the central point of the editor and solves the circular dependency issue.
 * It's basically a main dispatcher where all events arrive (and acted upon), so we can reach it from
 * all other submodules by letting them send out events without having to inject this manager
 * into them (and create a circular dependency).
 *
 * Rule of thumb: this manager will probably inject all submodules (to fire up their methods as a reaction
 * on events), but this manager will never be injected as a dependency of a submodule, only be letting
 * them send out events that are listened to in the manager.
 *
 * Created by wouter on 19/01/15.
 */
base.plugin("blocks.core.Manager", ["constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.Sidebar", "blocks.core.UI", "blocks.core.Notification", function (BlocksConstants, BlocksMessages, Broadcaster, Mouse, Sidebar, UI, Notification)
{
    var Manager = this;

    //-----CONSTANTS-----
    var AUTO_REFRESH_TIMEOUT = 500;

    //-----VARIABLES-----
    // flag to enable/disable layout functionality (create, resize, move and delete)
    var allowLayout = true;

    // flag to enable/disable editing of existing blocks (giving them focus to start editing)
    var allowEdit = true;

    // timer that checks the body dimension at regular intervals
    // and refreshes the page model when needed
    var dimensionTimer = null;
    var dimension;

    //-----MAIN ENTRY POINT: THIS BOOTSTRAPS THE BLOCKS SYSTEM-----
    Sidebar.create();

    //-----EVENT LISTENERS-----
    /**
     * Sent out when the sidebar was opened and the editor needs to boot up
     */
    $(document).on(Broadcaster.EVENTS.BLOCKS.START, function (event)
    {
        //create the page model
        UI.pageSurface = new blocks.elements.Page();

        //use the generalized method to put focus on the newly created page
        switchFocus(UI.pageSurface, UI.pageSurface.element, event);

        //disable navigating away without saving the page
        disableNavigation(true);

        //display a notification when the user navigates away (possibly without saving)
        if (BlocksConstants.ENABLE_LEAVE_EDIT_CONFIRM_CONFIG === 'true') {
            enableLeaveConfirmation(true);
        }

        //start listening for custom click events
        Mouse.activate();

        //start watching the DOM dimensions independently of any events
        //and refresh the page model when a change is detected
        enableResizeDetector(true);

    });

    /**
     * Sent out when the sidebar was closed and the editor needs to shut down
     */
    $(document).on(Broadcaster.EVENTS.BLOCKS.STOP, function (event)
    {
        //some cleanup: helps bugs when closing the bar during focus
        switchFocus(UI.pageSurface, UI.pageSurface.element, event);

        disableNavigation(false);
        enableLeaveConfirmation(false);
        enableResizeDetector(false);

        //TODO revise this (needs to be pause?)
        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE, event);
    });

    /**
     * Sent out by numerous modules when the editing of the page needs to be
     * temporarily halted, eg. during saving, dialogs, resizing, etc.
     * so the page is frozen while we wait for something to complete.
     */
    $(document).on(Broadcaster.EVENTS.PAUSE_BLOCKS, function (event)
    {
        Mouse.deactivate();

        //TODO revise
        //Hover.removeHoverOverlays();
        // DragDrop.setActive(false);
        // DOM.enableTextSelection(true);
    });

    /**
     * Un-pause the editor
     */
    $(document).on(Broadcaster.EVENTS.RESUME_BLOCKS, function (event)
    {
        Mouse.activate();
    });

    /**
     * Sent out by mouse.js when a click is registered.
     *
     * @param eventData has this structure:
     *   eventData.surface: the surface of the block
     *   eventData.element: the low-level DOM element (in the block element) we clicked
     *   eventData.originalEvent: the mousedown event that originated the click
     */
    $(document).on(Broadcaster.EVENTS.MOUSE.CLICK, function (event, eventData)
    {
        var switchToPage = true;

        //we clicked on a surface
        if (eventData.surface) {

            //option 1) check if we need to switch focus to a block
            if (
                //clicking on a surface is the first event to edit it, so block it if we don't have permission
                allowEdit
                //we don't allow to switch focus from one block to the next, let's always go back to the page first
                && (!UI.focusedSurface || UI.focusedSurface === UI.pageSurface)
                //make sure the block we received is a valid block
                && eventData.surface.isBlock()
                //if we click on (instead of dragging) the new block button, do nothing and let the popover do it's thing
                && !eventData.surface.isNew()) {

                switchFocus(eventData.surface, eventData.element, eventData.originalEvent);

                switchToPage = false;
            }
            //option 2) we re-clicked on the focused surface (this should be impossible because pointer-events are disabled on focus)
            else if (UI.focusedSurface && eventData.surface === UI.focusedSurface) {
                switchToPage = false;
            }
        }
        //we didn't click on a surface
        else if (!eventData.surface) {
            // there's currently focus on a block
            if (UI.focusedSurface.isBlock()) {
                //option 3) since we disable pointer-events for the focused surface, additional clicks inside that surface will be tied to regular DOM elements inside the block;
                //          make sure we don't switch back to the page so the user can interact with the content of the block
                //          Note: $.closest() begins with the current element, so the block itself is also included.
                if (eventData.element.closest(UI.focusedSurface.element).length > 0) {
                    switchToPage = false;
                }
                //option 4) if the click was not on a real surface-overlay, but also not in the page (but eg. in the sidebar, a dialog, etc), ignore it
                else if (eventData.element.closest(UI.pageContent).length == 0) {
                    switchToPage = false;
                }
            }
        }

        // in all other cases, we switch back to the page
        if (switchToPage && !UI.focusedSurface.isPage()) {
            switchFocus(UI.pageSurface, UI.pageSurface.element, eventData.originalEvent);
        }
    });

    $(document).on(Broadcaster.EVENTS.MOUSE.DRAG_START, function (event, eventData)
    {
        //add a general and a typed dragging class to the overlay wrapper
        UI.overlayWrapper.addClass(BlocksConstants.OVERLAY_DRAG_CLASS);
        UI.overlayWrapper.addClass(BlocksConstants.OVERLAY_DRAG_CLASS + '-' + eventData.surface.type);

        //also add a class to the block we're dragging around (except when creating a new block)
        if (eventData.surface.overlay) {
            eventData.surface.overlay.addClass(BlocksConstants.OVERLAY_DRAG_CLASS);
        }
    });
    $(document).on(Broadcaster.EVENTS.MOUSE.DRAG_MOVE, function (event, eventData)
    {
        //this clears all previous dropspot indicators (for all surfaces)
        blocks.elements.Surface.clearDropspots();

        //offer the user a preview of what would happen when the active surface would be moved
        //to the surface we're currently hovering over (in the direction indicated by the vector)
        eventData.surface.previewMoveTo(eventData.hoveredSurface, eventData.dragVector);
    });
    $(document).on(Broadcaster.EVENTS.MOUSE.DRAG_STOP, function (event, eventData)
    {
        //Remove the classes that were set during DRAG_START
        //removeClass() with function allows for a prefix-remove;
        // eg. it will remove both the 'drag' and typed 'drag-block' classes
        UI.overlayWrapper.removeClass(function (index, className)
        {
            //note: \s matches whitespace (spaces, tabs and new lines). \S is negated \s
            return (className.match(new RegExp('\\S*' + BlocksConstants.OVERLAY_DRAG_CLASS + '\\S*', 'g')) || []).join(' ');
        });

        var draggedBlock = eventData.surface;

        //reset hover information that was stored during previewing
        draggedBlock.resetPreviewMoveTo();

        //reset the drag class on the dragged surface (except when creating a new block)
        if (draggedBlock.overlay) {
            draggedBlock.overlay.removeClass(BlocksConstants.OVERLAY_DRAG_CLASS);
        }

        var activeDropspot = blocks.elements.Surface.getActiveDropspot();
        //note that eg. resizers don't have dropspots, their preview is immediate
        if (activeDropspot) {

            //save a reference to the parent before it's removed
            var oldParent = draggedBlock.parent;

            if (!draggedBlock.isNew()) {
                draggedBlock.moveTo(activeDropspot.anchor, activeDropspot.side);
            }
            else {
                createNewBlock(function callback(newBlockEl, onComplete)
                {
                    var parentSurface = activeDropspot.anchor;
                    // Create a new block and immediately move it to the final location.
                    // Note that the block will not be added to the parent until moveTo()
                    // is called, but the parent is needed for the constructor to create
                    // the overlay.
                    var newBlock = new blocks.elements.Block(parentSurface, newBlockEl);
                    newBlock.moveTo(parentSurface, activeDropspot.side);

                    if (onComplete) {
                        onComplete();
                    }
                });
            }

            postChangeBlock(oldParent);
        }

        //this clears all previous dropspot indicators (for all surfaces)
        blocks.elements.Surface.clearDropspots();
    });

    $(document).on(Broadcaster.EVENTS.PAGE.SAVE, function (event, eventData)
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

        //create a new node out of the full page html
        var savePage = UI.html.clone();

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

    $(document).on(Broadcaster.EVENTS.PAGE.DELETE, function (event, eventData)
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

    $(document).on(Broadcaster.EVENTS.BLOCK.DELETE, function (event, eventData)
    {
        var surface = eventData.surface;

        //save a reference to the parent before it's removed
        var oldParent = surface.parent;

        surface.parent._removeChild(surface);

        postChangeBlock(oldParent);

        //avoid the pierce through popup because the sidebar button is already gone
        switchFocus(UI.pageSurface, UI.pageSurface.element, event);
    });

    $(document).on("keyup keydown", function (e)
    {
        switch (e.type) {
            case "keydown" :
                UI.keysPressed[e.keyCode] = true;
                break;
            case "keyup" :
                //Logger.info("key up: "+e.keyCode);
                UI.keysPressed[e.keyCode] = false;
                break;
        }

        var btn;
        //disabled for now
        // if (UI.isKeyPressed(KEYCODE_CTRL) && UI.isKeyPressed(KEYCODE_S)) {
        //     btn = $("." + BlocksConstants.SAVE_PAGE_BUTTON);
        // }

        if (btn) {
            if (btn.is(":visible")) {
                btn.click();
                e.preventDefault();
            }
        }
    });

    //-----PRIVATE METHODS-----
    /**
     * Do a context switch to the clicked block
     *
     * @param surface
     * @param clickedElement
     * @param clickEvent
     */
    var switchFocus = function (surface, clickedElement, clickEvent)
    {
        //since the css below is animated, the sidebar will be filled by the time
        //the animation is finished, so boot it first
        Sidebar.init(surface, clickedElement, clickEvent);

        if (surface.isBlock()) {
            UI.overlayWrapper.addClass(BlocksConstants.BLOCK_FOCUSED_CLASS);
            surface.overlay.addClass(BlocksConstants.BLOCK_FOCUSED_CLASS);
            Mouse.enableDragging(false);
        }
        else {
            UI.overlayWrapper.removeClass(BlocksConstants.BLOCK_FOCUSED_CLASS);
            UI.surfaceWrapper.children().removeClass(BlocksConstants.BLOCK_FOCUSED_CLASS);
            Mouse.enableDragging(true);
        }

        UI.focusedSurface = surface;
    };

    /**
     * These are the common actions to be done when we added/moved/removed a block.
     *
     * @param oldParent
     */
    var postChangeBlock = function (oldParent)
    {
        //check if we need to cleanup the old parents because they're empty
        var toClean = oldParent;
        while (toClean) {
            //keep a reference to the parent (because we'll be detaching it below)
            var toCleanParent = toClean.parent;

            //if the surface we want to clean is empty and it has a parent,
            //we'll remove it from that parent
            if (toClean.children.length === 0 && toCleanParent) {
                toCleanParent._removeChild(toClean);
            }

            toClean = toCleanParent;
        }

        //Once all is done, we need to force a deep refresh of the entire page
        //note that we need to call refresh after the simplify,
        //because simplify can modify the dom slightly
        UI.pageSurface._simplify(true);
        UI.pageSurface._refresh(true);
    };

    var createNewBlock = function (callback)
    {
        // show select box with all blocks
        var boxDialog;
        //Note: the inner div will be replaced when the new load() content comes in
        var box = $('<div><div style="padding: 20px;">' + BlocksMessages.newBlockLoading + '</div></div>');

        var endpointUrlParams = '';
        var currentTypeof = UI.html.attr('typeof');
        if (currentTypeof) {
            endpointUrlParams += endpointUrlParams == '' ? '?' : '&';
            endpointUrlParams += BlocksConstants.GET_BLOCKS_TYPEOF_PARAM + '=' + encodeURIComponent(currentTypeof);
        }
        var pageTemplate = UI.html.attr(BlocksConstants.HTML_ROOT_TEMPLATE_ATTR);
        if (pageTemplate) {
            endpointUrlParams += endpointUrlParams == '' ? '?' : '&';
            endpointUrlParams += BlocksConstants.GET_BLOCKS_TEMPLATE_PARAM + '=' + encodeURIComponent(pageTemplate);
        }
        box.load(BlocksConstants.GET_BLOCKS_ENDPOINT + endpointUrlParams, function (response, status, xhr)
        {
            if (status == "error") {
                Notification.error(BlocksMessages.newBlockError + (response ? "; " + response : ""), xhr);
            }
            else {
                box.find("a").click(function (event)
                {
                    var name = $(this).attr("data-value");

                    //not always very fast, so show the wait dialog
                    //var waitingDialog;
                    var waitingDialog = new BootstrapDialog({
                        message: BlocksMessages.newBlockLoadingResources
                    });

                    boxDialog.close();
                    if (waitingDialog) {
                        waitingDialog.open();
                    }

                    $.getJSON(BlocksConstants.GET_BLOCK_ENDPOINT + name)
                        .done(function (data)
                        {
                            if (data[BlocksConstants.BLOCK_DATA_PROPERTY_HTML] && data[BlocksConstants.BLOCK_DATA_PROPERTY_HTML] !== "") {

                                addHeadResources(data[BlocksConstants.BLOCK_DATA_PROPERTY_INLINE_STYLES], name + "-in-style", BlocksConstants.BLOCK_DATA_PROPERTY_INLINE_STYLES);
                                addHeadResources(data[BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_STYLES], name + "-ex-style", BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_STYLES);

                                // Whow, this is weird stuff!
                                // Originally just $(data.html), but docs say the current version is safer.
                                // Problem was it failed with certains custom elements:
                                // th-search didn't work, where div-search did work.
                                // Seems to be a bug in JQuery: https://github.com/jquery/jquery/issues/1987
                                // Fixed with a patched version (see pom.xml)
                                // Note: fixed in JQuery 1.12.0 & 2.2.0 & 3.0 so we should probably get rid of the patched JQuery
                                var block = $($.parseHTML($.trim(data[BlocksConstants.BLOCK_DATA_PROPERTY_HTML])));

                                //resetDragDrop();
                                //cancelled = false;
                                // Layouter.addNewBlockAtLocation(block, lastDropLocation.anchor, lastDropLocation.side, function onComplete()
                                // {
                                //     addHeadResources(data[BlocksConstants.BLOCK_DATA_PROPERTY_INLINE_SCRIPTS], name + "-in-script", BlocksConstants.BLOCK_DATA_PROPERTY_INLINE_SCRIPTS, true);
                                //     addHeadResources(data[BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_SCRIPTS], name + "-ex-script", BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_SCRIPTS, true);
                                // });

                                callback(block, function onComplete()
                                {
                                    addHeadResources(data[BlocksConstants.BLOCK_DATA_PROPERTY_INLINE_SCRIPTS], name + "-in-script", BlocksConstants.BLOCK_DATA_PROPERTY_INLINE_SCRIPTS, true);
                                    addHeadResources(data[BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_SCRIPTS], name + "-ex-script", BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_SCRIPTS, true);
                                });
                            }
                            else {
                                Notification.error(BlocksMessages.newBlockError, data);
                            }
                        })
                        .fail(function (xhr, textStatus, exception)
                        {
                            Notification.error(BlocksMessages.newBlockError + (exception ? "; " + exception : ""), xhr);
                        })
                        .always(function ()
                        {
                            if (waitingDialog) {
                                waitingDialog.close();
                            }
                        });
                });
            }
        });

        //var cancelled = true;
        boxDialog = BootstrapDialog.show({
            title: BlocksMessages.selectFromTheListBelow,
            cssClass: BlocksConstants.NEW_BLOCK_MODAL_CLASS,
            message: function ()
            {
                return box
            },
            buttons: [],
            // onhidden: function ()
            // {
            //     if (cancelled) {
            //         DragDrop.dragAborted();
            //         Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS, blockEvent);
            //     }
            // }
        });
    };
    var addHeadResources = function (resourceArray, className, resourceType, isScript)
    {
        if (resourceArray != null && resourceArray.length > 0) {
            loadRecursiveHeadResources(resourceArray, 0, className, resourceType, isScript);
        }
    };
    var loadRecursiveHeadResources = function (resourceArray, idx, className, resourceType, isScript)
    {
        if (idx < resourceArray.length) {

            //parse the raw html to a jquery object
            var resourceEl = $(resourceArray[idx]);

            //Note: we don't so this anymore because we implemented the 'is present' check, see below
            //start off by removing existing ones
            //$("head ." + className).remove();
            //resourceEl.addClass(className);

            var srcAttr = resourceEl.attr("src");
            if (isScript && srcAttr) {

                //Note 1: this also adds the script to the head of the page (in case of a crossdomain URL, not in case of a local one, is that ok?),
                //        so we don't execute a $("head").append(resourceEl); here, hope that's ok
                //Note 2: we can't just append the script to the head element, cause we need to catch the callback and wire-in the plugin...
                //Note 3: By default, $.getScript() sets the cache setting to false by appending a timestamp to the script, so it's re-requested every time.
                //        Since we won't want that, we replace $.getScript by it's $.ajax counterpart and set the cache to true
                $.ajax({
                    type: "GET",
                    url: srcAttr,
                    dataType: "script",
                    cache: true
                })
                //$.getScript(srcAttr)
                    .done(function (data, textStatus, jqxhr)
                    {
                        //this is needed to auto-wire the plugins (was a quick fix, hope it's ok)
                        base.run();

                        //recursive call to make sure the resources are loaded synchronously
                        loadRecursiveHeadResources(resourceArray, idx + 1, className, resourceType, isScript);
                    })
                    .fail(function (xhr, textStatus, exception)
                    {
                        Notification.error(BlocksMessages.loadResourcesError + (exception ? "; " + exception : ""), xhr, textStatus, exception);
                    });
            }
            else {

                var head = $('head');
                var resourceElRaw = resourceEl[0];
                var resourceElInnerHtml = resourceElRaw.innerHTML;

                //since scripts will probably be in the footer, make sure we search the entire DOM,
                //then sub-filter on full html string
                var isPresent = $('html').find(resourceElRaw.tagName).filter(function ()
                {

                    if (resourceType == BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_STYLES) {
                        return $(this).attr('href') === resourceEl.attr('href');
                    }
                    else if (resourceType == BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_SCRIPTS) {
                        return $(this).attr('src') === resourceEl.attr('src');
                    }
                    else {
                        return this.innerHTML === resourceElInnerHtml;
                    }
                });

                if (isPresent.length == 0) {
                    head.append(resourceEl);
                }
                else {
                    //Logger.info('Skipped resource append for ' + resourceElRaw.outerHTML);
                }

                loadRecursiveHeadResources(resourceArray, idx + 1, className, resourceType, isScript);
            }
        }
    };

    /**
     * Prevent clicking on links while in editing mode
     * Note: after trying mousedown or mouseup to prevent vanishing links from triggering the modal,
     * it's quite important this is effectively the click event, because it overloads a lot of necessary other clicks
     * Use the pierce trough class to work around it
     *
     * @param enable
     */
    var disableNavigation = function (enable)
    {
        var NAMESPACE = 'prevent_click';

        if (enable) {
            // This will capture all clicks on <a> or <button> elements,
            // regardless of their location and/or if they're moved, added, deleted, ...
            $(document).on("click." + NAMESPACE, "a, button", function (e)
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

                //controls in the sidebar are enabled by default
                if (!pierceThrough && UI.sidebar) {
                    pierceThrough = UI.sidebar.find(control).length > 0;
                }

                //allow the dev to set a specific flag in the event to pierce through, both in the direct event and in the originalEvent
                //However, since all pierce-through should be set using UI.setPierceThrough(), it should always be set on the originalEvent object
                if (!pierceThrough && ((e.data && e.data[UI.PIERCE_THROUGH_DATA] === true) || (e.originalEvent.data && e.originalEvent.data[UI.PIERCE_THROUGH_DATA] === true))) {
                    pierceThrough = true;
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
                    pierceThrough = UI.isKeyPressed(UI.KEYCODE.SHIFT);
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
        }
        else {
            //removes all events in this namespace
            $(document).off("." + NAMESPACE);
        }
    };

    /**
     * Enable functionality where we warn the user if she navigates away (possibly without saving)?
     *
     * @param enable
     */
    var enableLeaveConfirmation = function (enable)
    {
        if (enable) {
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
        else {
            window.onbeforeunload = undefined;
        }
    };

    /**
     * This is unelegant, but needed: when adding a new block,
     * (eg. a blocks-image), external resources (styles/scripts/images/...)
     * sometimes change that block's dimensions long after it was created
     * (eg. an image over a slow link). Because of this (and other reasons),
     * we can't really only refresh the page model (+ surface dimensions) in a
     * controlled manner. So we decided to create this loop that does that from time to time.
     *
     * @param enable
     */
    var enableResizeDetector = function(enable)
    {
        if (enable) {
            dimension = {
                width: UI.body.width(),
                height: UI.body.height(),
            };
            dimensionTimer = setInterval(function ()
            {
                var width = UI.body.width();
                var height = UI.body.height();

                if (dimension.width !== width || dimension.height !== height) {
                    dimension.width = width;
                    dimension.height = height;

                    UI.pageSurface._refresh(true);
                }

            }, AUTO_REFRESH_TIMEOUT);
        }
        else {
            if (dimensionTimer) {
                clearInterval(dimensionTimer);
            }

            dimensionTimer = undefined;
        }
    };

    //-----TODO UNCHECKED-----
    //TODO revise (RESUME_BLOCKS)
    $(document).on(Broadcaster.EVENTS.ACTIVATE_MOUSE, function (event)
    {
        //start listening for mousedown, mouseup, mouseleave
        Mouse.activate();
        //collapse selection, prevent text selection, disable ondragstart
        //TODO revise
        //DOM.enableTextSelection(false);
        //show surfaces
        //Hover.showHoverOverlays();
        //enable dragging
        DragDrop.setActive(true);

        //TODO move this to DO_REFRESH_LAYOUT?
        //fire up drag-and-drop subsystem if we have enough room
        var windowWidth = $(window).width();
        //TODO make constant
        var MIN_SCREEN_DND_THRESHOLD = 1030;
        if (windowWidth >= MIN_SCREEN_DND_THRESHOLD) {
            enableLayout(true);
        }
        else {
            Logger.debug("Available page screen size is less than " + MIN_SCREEN_DND_THRESHOLD + " (" + windowWidth + "), disabling drag-and-drop.");
            enableLayout(false);
        }
    });

    $(document).on(Broadcaster.EVENTS.DOM_CHANGED, function (event)
    {
        Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
    });

    $(document).on(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, function (event)
    {
        //this will end up in menu.js triggering updateContainerWidth()
        Broadcaster.send(Broadcaster.EVENTS.WILL_REFRESH_LAYOUT, event);

        //hide the overlays while redrawing
        //Hover.removeHoverOverlays();

        //make sure the page content wrapper block is at least the height of the body (to support good, natural blur, see comments below)
        updatePageContentHeight();

        //we always start off with a focused page
        var pageBlock = Hover.createPageBlock();
        //note: we can't fill in the last argument because it's not a property or a template tag
        focusSwitch(pageBlock, pageBlock.element, null);

        //redrawing done
        //Hover.showHoverOverlays();

        Broadcaster.send(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, event);
    });

    $(document).on(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, function (event)
    {
        Mouse.resetMouse();
    });

    //-----EVENTS FOR DRAGGING-----
    /**
     * Called when a user starts dragging a block (fired after a minimum threshold is exceeded)
     */
    $(document).on(Broadcaster.EVENTS.START_DRAG, function (event, eventData)
    {
        //Broadcaster.zoom();
        DOM.enableContextMenu(false);
        DragDrop.dragStarted(event, eventData);
    });

    /**
     * Called when a user aborted dragging a block
     */
    $(document).on(Broadcaster.EVENTS.ABORT_DRAG, function (event)
    {
        //Broadcaster.unzoom();
        DOM.enableContextMenu(true);
        DragDrop.dragAborted(event);
    });

    /**
     * Called when a user ended dragging a block
     */
    $(document).on(Broadcaster.EVENTS.END_DRAG, function (event)
    {
        //Broadcaster.unzoom();

        DOM.enableContextMenu(true);
        DragDrop.dragEnded(event);
    });

    /**
     * Called all the time when a user is dragging one block over another block
     */
    $(document).on(Broadcaster.EVENTS.DRAG_OVER_BLOCK, function (event, eventData)
    {
        if (DragDrop.isDragging()) {
            DragDrop.dragOverBlock(event, eventData);
        }
    });

    //-----EVENTS FOR DRAGGING-----
    var updatePageContentHeight = function ()
    {
        var body = $('html');
        var bodyBottom = body.position().top + body.outerHeight(true);

        var page = $('.' + BlocksConstants.PAGE_CONTENT_CLASS);
        var pageBottom = page.position().top + page.outerHeight(true);
        //Note: we must always set the out height to the body height (that's why it's commented out)
        // because we want the page content to scroll independently from the sidebar (css is set to overflow-y auto)
        //if (pageBottom<bodyBottom) {
        page.outerHeight(bodyBottom - page.position().top);
        //}
    };

    var enableFocusBlurDetection = function (block, focusedElement)
    {
        // this basically comes down to this:
        // we'll make the entire block a hotspot, not just the focusedElement
        // and all clicks inside that block will get a new focus (if it's not again the same element we clicked on)
        // as soon as we click outside of the block, it is blurred and focus goes to the page (always first the page)
        block.element.on("mousedown.manager_focus_end", function (event)
        {
            var element = $(event.target);
            if (element.length) {
                var hoverObj = Hover.createHoverClickObject(block, element, event);
                if (hoverObj && !hoverObj.propertyElement.is(focusedElement)) {
                    //unregister first, because the next run will add it again
                    block.element.off("mousedown.manager_focus_end");
                    //this will mainly end up in sidebar.js
                    Broadcaster.send(Broadcaster.EVENTS.FOCUS_BLOCK, event, hoverObj);
                }
            }

            //don't prevent default or the text editor won't function
            //e.preventDefault();
            event.stopPropagation();
        });

        //Note: everything focus/blur related is more or less depending on clicking on the  PAGE_CONTENT_CLASS block,
        // so make sure it at least occupies the entire page (left of the sidebar) to create the illusion we can click
        // outside any block (even when we're not clicking on another block) to have a block lose focus.
        // Because setting the height of the PAGE_CONTENT_CLASS element using css was too error prone, we decided to set it using scripting,
        // see above in updatePageContentHeight()
        //Note 2: we can't eg; make this $(document), because it would blur too fast (eg. when clicking on the sidebar or alert dialogs)
        var page = $('.' + BlocksConstants.PAGE_CONTENT_CLASS);
        page.on("mousedown.manager_focus_end", function (event)
        {
            event.preventDefault();
            event.stopPropagation();

            block.element.off("mousedown.manager_focus_end");
            page.off("mousedown.manager_focus_end");

            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
        });
    };
    /**
     * Called when we want to enable/disable the layouting of the page
     * (eg. the whole drag-and-drop system to create, move and/or resize surfaces)
     */
    var enableLayout = function (enable)
    {
        var newBlockBtn = $('.' + BlocksConstants.CREATE_BLOCK_CLASS);

        if (enable) {
            //Mouse.enableLayout(true);
            DragDrop.setActive(true);
            Resizer.activate(true);

            if (newBlockBtn) {
                newBlockBtn.removeAttr("disabled");
            }
        }
        else {
            //Mouse.enableLayout(false);
            DragDrop.setActive(false);
            Resizer.activate(false);

            if (newBlockBtn) {
                newBlockBtn.attr("disabled", "");
                newBlockBtn.attr("title", BlocksMessages.pageTooSmallToLayout);
            }
        }
    };

}]);
