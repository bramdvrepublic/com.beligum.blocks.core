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
base.plugin("blocks.core.Manager", ["constants.base.core.internal", "constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.DragDrop", "blocks.core.Resizer", "blocks.core.Hover", "blocks.core.DOM", "blocks.core.Sidebar", "blocks.core.UI", "blocks.core.Notification", function (Constants, BlocksConstants, BlocksMessages, Broadcaster, Mouse, DragDrop, Resizer, Hover, DOM, Sidebar, UI, Notification)
{
    var Manager = this;

    //-----CONSTANTS-----

    //-----VARIABLES-----
    // flag to enable/disable layout functionality (create, resize, move and delete)
    var allowLayout = true;

    // flag to enable/disable editing of existing blocks (giving them focus to start editing)
    var allowEdit = true;

    //the surface of the page
    var pageSurface = null;

    //the currently focused surface
    var focusedSurface = null;

    //-----EVENT LISTENERS-----
    /**
     * Sent out by menu.js when the sidebar was opened and the editor needs to boot up
     */
    $(document).on(Broadcaster.EVENTS.START_BLOCKS, function (event)
    {
        //note that this encapsulates DO_REFRESH_LAYOUT, but initializes a few other things first
        // Broadcaster.send(Broadcaster.EVENTS.DOM_CHANGED, event);

        //start off by showing the layouter
        // Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE, event);

        //create the page model
        pageSurface = new blocks.elements.Page();

        //use the generalized method to put focus on the newly created page
        switchFocus(pageSurface, pageSurface.element, event);

        //start listening for clicks
        Mouse.activate();
    });

    /**
     * Sent out by menu.js when the sidebar was closed and the editor needs to shut down
     */
    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function (event)
    {
        //some cleanup: helps bugs when closing the bar during focus
        focusSwitch(Hover.getPageBlock());

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
                && (!focusedSurface || focusedSurface === pageSurface)
                //make sure the block we received is a valid block
                && eventData.surface.isBlock()
                //if we click on (instead of dragging) the new block button, do nothing and let the popover do it's thing
                && !eventData.surface.isNewBlock()) {

                switchFocus(eventData.surface, eventData.element, eventData.originalEvent);

                switchToPage = false;
            }
            //option 2) we re-clicked on the focused surface (this should be impossible because pointer-events are disabled on focus)
            else if (focusedSurface && eventData.surface === focusedSurface) {
                switchToPage = false;
            }
        }
        //we didn't click on a surface
        else if (!eventData.surface) {
            // there's currently focus on a block
            if (focusedSurface.isBlock()) {
                //option 3) since we disable pointer-events for the focused surface, additional clicks inside that surface will be tied to regular DOM elements inside the block;
                //          make sure we don't switch back to the page so the user can interact with the content of the block
                //          Note: $.closest() begins with the current element, so the block itself is also included.
                if (eventData.element.closest(focusedSurface.element).length > 0) {
                    switchToPage = false;
                }
                //option 4) if the click was not on a real surface-overlay, but also not in the page (but eg. in the sidebar, a dialog, etc), ignore it
                else if (eventData.element.closest(UI.pageContent).length == 0) {
                    switchToPage = false;
                }
            }
        }

        // in all other cases, we switch back to the page
        if (switchToPage && !focusedSurface.isPage()) {
            switchFocus(pageSurface, pageSurface.element, eventData.originalEvent);
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
            if (!draggedBlock.isNewBlock()) {
                draggedBlock.moveTo(activeDropspot.anchor, activeDropspot.side);
            }
            else {
                createNewBlock(function callback(newBlockEl, onComplete)
                {
                    var parentSurface = activeDropspot.anchor;
                    //Create a new block with an element, but without a parent
                    //and immediately move it to the final location
                    var newBlock = new blocks.elements.Block(null, newBlockEl);
                    newBlock.moveTo(parentSurface, activeDropspot.side);

                    if (onComplete) {
                        onComplete();
                    }
                });
            }
        }

        //this clears all previous dropspot indicators (for all surfaces)
        blocks.elements.Surface.clearDropspots();
    });

    //-----PRIVATE METHODS-----
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

        focusedSurface = surface;
    };
    /**
     * @param block the block that should get focus (not null)
     * @param element the low-level element that we clicked on (may be null, if we didn't click on anything)
     * @param propertyElement the first property element or template element on the way up from element (may be null)
     * @param hotspot the (possibly changed) mouse coordinates that function as the 'hotspot' for this event (object with top and left like offset())
     */
    var focusSwitch = function (block, element, propertyElement, hotspot, event)
    {
        //this will make sure we always 'go back' to the page first, instead of directly focussing the next clicked block
        // except when we click on an element that's inside the currently focused block
        var previousFocusedBlock = Hover.getFocusedBlock();

        if (previousFocusedBlock == null || (previousFocusedBlock != Hover.getPageBlock() && previousFocusedBlock.element.find(propertyElement).length == 0)) {
            Sidebar.init(Hover.getPageBlock(), Hover.getPageBlock().element, Hover.getPageBlock().element.offset(), event);
            Hover.removeFocusOverlays();
            Hover.setFocusedBlock(Hover.getPageBlock());
            Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS, event);
        }
        else {
            //if we got a property, use it, otherwise focus the entire block
            var selectedElement = propertyElement == null ? block.element : propertyElement;

            Sidebar.init(block, selectedElement, hotspot, event);
            Hover.showFocusOverlays(block.element);
            Hover.setFocusedBlock(block);
            //TODO revise
            Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, event);

            enableFocusBlurDetection(block, selectedElement);
        }
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

    //-----TODO UNCHECKED-----
    //TODO revise (RESUME_BLOCKS)
    $(document).on(Broadcaster.EVENTS.ACTIVATE_MOUSE, function (event)
    {
        //start listening for mousedown, mouseup, mouseleave
        Mouse.activate();
        //collapse selection, prevent text selection, disable ondragstart
        DOM.enableTextSelection(false);
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
        if (enable) {
            //Mouse.enableLayout(true);
            DragDrop.setActive(true);
            Resizer.activate(true);

            if (UI.newBlockBtn) {
                UI.newBlockBtn.removeAttr("disabled");
            }
        }
        else {
            //Mouse.enableLayout(false);
            DragDrop.setActive(false);
            Resizer.activate(false);

            if (UI.newBlockBtn) {
                UI.newBlockBtn.attr("disabled", "");
                UI.newBlockBtn.attr("title", BlocksMessages.pageTooSmallToLayout);
            }
        }
    };

}]);
