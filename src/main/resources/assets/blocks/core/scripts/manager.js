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
 */
base.plugin("blocks.core.Manager", ["base.core.Commons", "constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.Mouse", "blocks.core.Sidebar", "blocks.core.UI", "blocks.core.Notification", "blocks.core.Undo", function (Commons, BlocksConstants, BlocksMessages, Broadcaster, Mouse, Sidebar, UI, Notification, Undo)
{
    var Manager = this;

    //-----CONSTANTS-----
    // Checks DOM dimension changes every x millis
    var DEFAULT_DIMENSION_TIMEOUT = 1000;

    // Because we set a container width on the <blocks-layout> element in some styles
    // (eg. sticky footers and full background-colors), we need to scale it along with the container inside it
    var CONTAINERS_SELECTOR = '.' + BlocksConstants.CONTAINER_CLASS + ', blocks-layout';

    // The minimum width we need to enable the blocks editor
    // Note: this value is the bootstrap threshold for large devices (normal desktops and up)
    var MIN_WINDOW_WIDTH = 992;

    //-----VARIABLES-----
    // timer that checks the body dimension at regular intervals
    // and refreshes the page model when needed
    var dimensionTimer = null;
    var dimensionTimeout = DEFAULT_DIMENSION_TIMEOUT;
    var dimensions = undefined;

    //general flag to keep track when the system is booted or not
    var booted = false;

    //temp variable to store page html during DnD
    var pageContentHtml = undefined;

    //-----MAIN ENTRY POINT: LOAD THE SIDEBAR HTML AND BOOTSTRAP THE BLOCKS SYSTEM-----
    Sidebar.load();

    //-----EVENT LISTENERS-----
    /**
     * Sent out when the sidebar was opened and the editor needs to boot up
     */
    $(document).on(Broadcaster.EVENTS.BLOCKS.START, function (event)
    {
        // This needs to come before the sidebar toggle,
        // otherwise the sidebar isn't attached to the DOM yet
        createEditLayout();

        // Create the overlay containers
        // Note: these need to exist before the page model is created
        UI.overlayWrapper = $('<div class="' + BlocksConstants.BLOCK_OVERLAY_WRAPPER_CLASS + '"/>').appendTo(UI.body);
        UI.surfaceWrapper = $('<div class="' + BlocksConstants.SURFACE_WRAPPER_CLASS + '"/>').appendTo(UI.overlayWrapper);
        UI.resizerWrapper = $('<div class="' + BlocksConstants.RESIZER_WRAPPER_CLASS + '"/>').appendTo(UI.overlayWrapper);
        UI.dropspotWrapper = $('<div class="' + BlocksConstants.DROPSPOT_WRAPPER_CLASS + '"/>').appendTo(UI.overlayWrapper);

        // These needs to be outside the toggle callback too, so we can
        // animate the width of the body while opening the sidebar
        UI.containers = $(CONTAINERS_SELECTOR);
        UI.pageSurface = new blocks.elements.Page(UI.pageContent);

        //blur the focused block when we hit ESC
        UI.registerKeystrokeAction(UI.KEYCODE.ESC, UI.KEYCODE.MODIFIER.NONE, function (event)
        {
            if (UI.focusedSurface && UI.focusedSurface !== UI.pageSurface) {
                switchFocus(UI.pageSurface, UI.pageSurface.element, event);
            }
        });

        UI.registerKeystrokeAction(UI.KEYCODE.Z, UI.KEYCODE.MODIFIER.CTRL, function ()
        {
            Undo.undo();
        });

        UI.registerKeystrokeAction(UI.KEYCODE.Y, UI.KEYCODE.MODIFIER.CTRL, function ()
        {
            Undo.redo();
        });

        //open (animated) the sidebar
        Sidebar.toggle(true, function ()
        {
            //use the generalized method to put focus on the newly created page
            switchFocus(UI.pageSurface, UI.pageSurface.element, event);

            //disable navigating away without saving the page
            disableNavigation(true);

            //display a notification when the user navigates away (possibly without saving)
            if (BlocksConstants.ENABLE_LEAVE_EDIT_CONFIRM_CONFIG === 'true') {
                enableLeaveConfirmation(true);
            }

            //start watching the DOM dimensions independently of any events
            //and refresh the page model when a change is detected
            enableChangeDetector(true);

            //start listening for custom click events
            Mouse.enable(true);

            //Note: it makes sense to disable undo when the sidebar is closed;
            //otherwise, a ctrl-z keystroke on any page would try to do an undo,
            //but the user doesn't have any visual indication she's editing that page.
            Undo.enable(true);

            booted = true;

            //notify others we're done booting
            Broadcaster.send(Broadcaster.EVENTS.BLOCKS.STARTED);
        });

    });

    /**
     * Sent out when the sidebar was closed and the editor needs to shut down
     */
    $(document).on(Broadcaster.EVENTS.BLOCKS.STOP, function (event)
    {
        booted = false;

        //some cleanup: helps bugs when closing the bar during focus
        switchFocus(UI.pageSurface, UI.pageSurface.element, event);

        //toggle the sidebar before removing it from the DOM
        Sidebar.toggle(false, function ()
        {
            //remove any remaining manual dimension attributes
            clearBodyDimensions();

            //unset these to signal we're down
            UI.pageSurface = undefined;
            UI.focusedSurface = undefined;

            Undo.enable(false);
            Mouse.enable(false);
            UI.resetKeystrokes();
            enableLeaveConfirmation(false);
            disableNavigation(false);
            enableChangeDetector(false);

            //we need to properly detach the start button before clearing the layout
            //or the attached events will be gone
            UI.startButton.detach();
            destroyEditLayout();
            UI.startButton.appendTo(UI.body);

            //reset the variables to signal the system is down
            UI.pageContent = undefined;
            UI.containers = undefined;
            UI.overlayWrapper = undefined;
            UI.surfaceWrapper = undefined;
            UI.resizerWrapper = undefined;
            UI.dropspotWrapper = undefined;

            //notify others we've finished shutting down
            Broadcaster.send(Broadcaster.EVENTS.BLOCKS.STOPPED);
        });
    });

    /**
     * Sent out by numerous modules when the editing of the page needs to be
     * temporarily halted, eg. during saving, dialogs, resizing, etc.
     * so the page is frozen while we wait for something to complete.
     */
    $(document).on(Broadcaster.EVENTS.BLOCKS.PAUSE, function (event)
    {
        pauseSystem(true);
    });

    /**
     * Un-pause the blocks system
     */
    $(document).on(Broadcaster.EVENTS.BLOCKS.RESUME, function (event)
    {
        pauseSystem(false);
    });

    /**
     * Sent out by mouse.js when a click is registered.
     */
    $(document).on(Broadcaster.EVENTS.MOUSE.CLICK, function (event, eventData)
    {
        var switchToPage = true;

        //we clicked on a surface
        if (eventData.surface) {

            //option 1) check if we need to switch focus to a block
            if (
                //clicking on a surface is the first event to edit it, so block it if we don't have permission
                UI.allowEdit
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
                else if (eventData.element.closest(UI.pageContent).length === 0) {
                    switchToPage = false;
                }
                //option 5) the mousedown event of this click started in the focused block, but the user dragged outside the block and let go
                // Eg. this happens when you select text and move outside the block. In this case, don't blur, since we started ON the block.
                else if ($(eventData.originalEvent.target).closest(UI.focusedSurface.element).length > 0) {
                    switchToPage = false;
                }
            }
        }

        // in all other cases, we switch back to the page
        if (switchToPage && !UI.focusedSurface.isPage()) {
            switchFocus(UI.pageSurface, UI.pageSurface.element, eventData.originalEvent);
        }
    });

    $(document).on(Broadcaster.EVENTS.MOUSE.DRAG.START, function (event, eventData)
    {
        if (eventData.surface.isNew() ? UI.allowCreate : UI.allowLayout) {

            //if we're dragging a resizer, we need to store a snapshot of the page
            //because the DOM will be updated during preview (see drag stop)
            if (eventData.surface.isResizer()) {
                pageContentHtml = UI.pageSurface.element.html();
            }

            //add a general and a typed dragging class to the overlay wrapper
            UI.overlayWrapper.addClass(BlocksConstants.OVERLAY_DRAG_CLASS);
            UI.overlayWrapper.addClass(BlocksConstants.OVERLAY_DRAG_CLASS + '-' + eventData.surface.type);

            //also add a class to the block we're dragging around (except when creating a new block)
            if (eventData.surface.overlay) {
                eventData.surface.overlay.addClass(BlocksConstants.OVERLAY_DRAG_CLASS);
            }
        }
    });

    $(document).on(Broadcaster.EVENTS.MOUSE.DRAG.MOVE, function (event, eventData)
    {
        if (eventData.surface.isNew() ? UI.allowCreate : UI.allowLayout) {

            //offer the user a preview of what would happen when the active surface would be moved
            //to the surface we're currently hovering over (in the direction indicated by the vector)
            eventData.surface.previewMoveTo(eventData.hoveredSurface, eventData.dragVector);
        }
    });

    $(document).on(Broadcaster.EVENTS.MOUSE.DRAG.STOP, function (event, eventData)
    {
        if (eventData.surface.isNew() ? UI.allowCreate : UI.allowLayout) {

            //TODO Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, event);

            //Remove the classes that were set during DRAG_START
            //removeClass() with function allows for a prefix-remove;
            // eg. it will remove both the 'drag' and typed 'drag-block' classes
            UI.overlayWrapper.removeClass(function (index, className)
            {
                //note: \s matches whitespace (spaces, tabs and new lines). \S is negated \s
                return (className.match(new RegExp('\\S*' + BlocksConstants.OVERLAY_DRAG_CLASS + '\\S*', 'g')) || []).join(' ');
            });

            var draggedSurface = eventData.surface;

            // we call this method from the abort handler as well...
            if (draggedSurface) {
                //reset hover information that was stored during previewing
                draggedSurface.resetPreviewMoveTo();

                //reset the drag class on the dragged surface (except when creating a new block)
                if (draggedSurface.overlay) {
                    draggedSurface.overlay.removeClass(BlocksConstants.OVERLAY_DRAG_CLASS);
                }

                var oldHtml = UI.pageSurface.element.html();
                var activeDropspot = blocks.elements.Surface.getActiveDropspot();
                // note that eg. resizers don't have dropspots, their preview is immediate
                // also, this is called on drag abort, with explicitly no active dropspot
                if (activeDropspot) {

                    //save a reference to the parent before it's removed
                    var oldParent = draggedSurface.parent;

                    if (!draggedSurface.isNew()) {

                        if (UI.allowLayout) {

                            draggedSurface.moveTo(activeDropspot.anchor, activeDropspot.side);

                            postChangeBlock(oldParent);

                            Broadcaster.send(Broadcaster.EVENTS.BLOCK.MOVED, event, {
                                surface: draggedSurface
                            });
                            //when the block was moved, the entire page changed
                            Broadcaster.send(Broadcaster.EVENTS.PAGE.CHANGED.HTML, event, {
                                surface: UI.pageSurface,
                                oldValue: oldHtml,
                            });
                        }
                    }
                    else {
                        if (UI.allowCreate) {

                            loadNewBlockList(function callback(newBlockEl, onComplete)
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

                                postChangeBlock(oldParent);

                                Broadcaster.send(Broadcaster.EVENTS.BLOCK.CREATED, event, {
                                    surface: newBlock
                                });
                                //when the block was added, the entire page changed
                                Broadcaster.send(Broadcaster.EVENTS.PAGE.CHANGED.HTML, event, {
                                    surface: UI.pageSurface,
                                    oldValue: oldHtml,
                                });
                            });
                        }
                    }
                }
                else {
                    //note that resizers don't have dropspots, but they do change the page,
                    //so make sure this is sent out (eg. needed by Undo)
                    //Also note that this will also be called on drag abort, which is
                    //exactly what we want (to have immediate resizer updated, but with proper undo)
                    if (draggedSurface.isResizer()) {
                        //note that, for now, we don't implement a 'column changed' event
                        //because it's not consistent: if we move around blocks, sometimes
                        //columns get changed as well and those events would need to be
                        //implemented as well. For now, only firing a page change is enough, I guess.
                        Broadcaster.send(Broadcaster.EVENTS.PAGE.CHANGED.HTML, event, {
                            surface: UI.pageSurface,
                            oldValue: pageContentHtml,
                        });

                        pageContentHtml = undefined;
                    }
                }
            }

            //this clears all previous dropspot indicators (for all surfaces)
            blocks.elements.Surface.clearDropspots();
        }
    });

    $(document).on(Broadcaster.EVENTS.MOUSE.DRAG.ABORT, function (event, eventData)
    {
        Logger.warn("Aborting active dragging session");

        //make sure we don't have an active dropspot (so we can re-use the stop handler)
        blocks.elements.Surface.clearDropspots();

        // after clearing the dropspot, we'll re-use the stop handler above
        Broadcaster.send(Broadcaster.EVENTS.MOUSE.DRAG.STOP, event, eventData);
    });

    /**
     * Update the page model and it's dimensions, once, right now
     * (instead of waiting for the refresh loop to fire it)
     */
    $(document).on(Broadcaster.EVENTS.PAGE.REFRESH, function (event, eventData)
    {
        //manually call the refresh method
        //by default, we force a refresh, but that can be overridden
        refreshPage(!(eventData && eventData.force === false));
    });

    /**
     * Change the speed at which the page is updated in the refresh-loop.
     * If speed is unset (or negative) in the eventData, it's reset to the default speed.
     */
    $(document).on(Broadcaster.EVENTS.PAGE.REFRESH_SPEED, function (event, eventData)
    {
        if (eventData.speed && eventData.speed > 0) {
            setDimensionTimeout(eventData.speed);
        }
        //if nothing is supplied, we assume a reset is requested
        else {
            setDimensionTimeout(DEFAULT_DIMENSION_TIMEOUT);
        }
    });

    /**
     * Completely rebuild the page model and it's overlays
     */
    $(document).on(Broadcaster.EVENTS.PAGE.RELOAD, function (event, eventData)
    {
        //make sure we are in a reset state before we actually clear stuff
        switchFocus(UI.pageSurface, UI.pageSurface.element, event);

        //note: this will also signal the refresh method to ignore refresh events while rebuilding
        UI.pageSurface = null;

        blocks.elements.Surface.clearAllOverlays();

        UI.pageSurface = new blocks.elements.Page(UI.pageContent);

        //the pointer changed, so make sure to call this
        switchFocus(UI.pageSurface, UI.pageSurface.element, event);
    });

    /**
     * Send the current page html to the server for saving
     */
    $(document).on(Broadcaster.EVENTS.PAGE.SAVE, function (event, eventData)
    {
        //TODO Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, event);

        //note: get the page html before we open the modal dialog
        // or it will end up in there
        var pageHtml = getBodyHtml();

        var dialog = new BootstrapDialog({
            type: BootstrapDialog.TYPE_DEFAULT,
            title: BlocksMessages.savePageDialogTitle,
            message: BlocksMessages.savePageDialogMessage,
            buttons: []
        });

        dialog.open();

        var params = {};
        params[BlocksConstants.PAGE_URL_PARAM] = document.URL;
        $.ajax({
            type: 'POST',
            url: BlocksConstants.SAVE_PAGE_ENDPOINT + '?' + $.param(params),
            data: pageHtml,
            // we're sending json
            contentType: 'application/json; charset=UTF-8',
            // we're expecting json back (explicitly needed for validation error handling)
            dataType: 'json',
        })
            .done(function (data, textStatus, response)
            {
            })
            .fail(function (xhr, textStatus, exception)
            {
                Notification.jsonError(BlocksMessages.savePageError, xhr, textStatus, exception);
            })
            .always(function ()
            {
                dialog.close();
                //TODO Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS);
            });
    });

    /**
     * Delete the current page
     */
    $(document).on(Broadcaster.EVENTS.PAGE.DELETE, function (event, eventData)
    {
        //TODO Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, event);

        var dialog = undefined;

        var onConfirm = function (deleteAllTranslations)
        {
            dialog.setTitle(BlocksMessages.deletingPageDialogTitle);
            dialog.setMessage(BlocksMessages.deletingPageDialogMessage);
            dialog.getModalFooter().find('button').addClass('hidden');

            $.ajax({
                type: 'DELETE',
                url: deleteAllTranslations ? BlocksConstants.DELETE_PAGE_ALL_ENDPOINT : BlocksConstants.DELETE_PAGE_ENDPOINT,
                data: document.URL,
                contentType: 'application/json; charset=UTF-8',
            })
                .done(function (url, textStatus, response)
                {
                    // We'll be reloading the page, so temporarily disable the notification when the user navigates away
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
                    // first close the dialog, then show the notification
                    dialog.close();
                    Notification.jsonError(BlocksMessages.deletingPageErrorMessage, xhr, textStatus, exception);
                })
                .always(function ()
                {
                    //Note: we don't close it here, but in the fail() instead,
                    // because the done() does a redirect and thus displays the message all
                    // the way to the end
                    //dialog.close();
                });
        };

        var btnIdSingle = 'btn-ok-single';
        var btnIdAll = 'btn-ok-all';
        var btnIdCancel = 'btn-close';
        dialog = new BootstrapDialog({
            type: BootstrapDialog.TYPE_DEFAULT,
            title: BlocksMessages.loading,
            message: BlocksMessages.pleaseWait + '...',

            // note that these start out hidden...
            buttons: [
                {
                    id: btnIdSingle,
                    label: BlocksMessages.deletePageDialogConfirmSingle,
                    cssClass: 'btn-danger hidden',
                    action: function (dialogRef)
                    {
                        onConfirm(false);
                    }

                },
                {
                    id: btnIdAll,
                    label: BlocksMessages.deletePageDialogConfirmAll,
                    cssClass: 'btn-danger hidden',
                    action: function (dialogRef)
                    {
                        onConfirm(true);
                    }

                },
                {
                    id: btnIdCancel,
                    label: BlocksMessages.cancel,
                    cssClass: 'btn-default hidden',
                    action: function (dialogRef)
                    {
                        dialog.close();
                    }
                }
            ],
            onhide: function ()
            {
                //TODO Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS);
            }
        });

        dialog.open();

        // do a little round trip to check if this page has translations so we can ease up the dialog if it doesn't have any
        var data = {};
        data[BlocksConstants.PAGE_URL_PARAM] = window.location.href;
        Logger.info(data);
        $.getJSON(BlocksConstants.GET_PAGE_META_ENDPOINT, data)
            .done(function (data)
            {
                dialog.setTitle(BlocksMessages.deletePageDialogTitle);

                // un-hide the single and cancel button, and selectively un-hide the delete all if we have translations
                dialog.getModalFooter().find('#'+btnIdSingle).removeClass('hidden');
                dialog.getModalFooter().find('#'+btnIdCancel).removeClass('hidden');

                // the 'translations' property will hold a map with language->URI pairs of translations of this page
                if (data && data.translations && !$.isEmptyObject(data.translations)) {
                    var num = 0;
                    $.each(data.translations, function(key, value) { num++ });
                    dialog.setMessage(Commons.format(BlocksMessages.deletePageAllDialogMessage, num));

                    dialog.getModalFooter().find('#'+btnIdAll).removeClass('hidden');
                }
                else {
                    dialog.setMessage(BlocksMessages.deletePageSingleDialogMessage);
                }
            })
            .fail(function (xhr, textStatus, exception)
            {
                // first close the dialog, then show the notification
                dialog.close();
                Notification.jsonError(BlocksMessages.deletingPageErrorMessage, xhr, textStatus, exception);
            });
    });

    $(document).on(Broadcaster.EVENTS.PAGE.CHANGED.HTML, function (event, eventData)
    {
        //don't record changes that happen _inside_ the undo event
        if (!Undo.isInsideUndoRedo(eventData.surface.element)) {
            //note: executing this will trigger the update of oldBlocksHtml, see listener above
            Undo.recordHtmlChange(eventData.surface.element, eventData.oldValue, null, null, null, function ()
            {
                //we wrapped the listener callback to add a refresh, but the sender could have passed a listener too
                if (eventData.listener) {
                    eventData.listener(value, action, cmd);
                }

                //Rebuild the page model when an undo/redo was executed
                //note that we need to reload, not refresh because undo will replace the page html entirely
                Broadcaster.send(Broadcaster.EVENTS.PAGE.RELOAD);
            });
        }

        Broadcaster.send(Broadcaster.EVENTS.PAGE.REFRESH);
    });

    $(document).on(Broadcaster.EVENTS.BLOCK.CREATED, function (event, eventData)
    {
        //note: this is fired in the middle of block creation (as soon as it exists),
        // but style/scripts resources are still being loaded, so fire it after a small delay;
        // it looks cleaner and is more noticeable
        setTimeout(function ()
        {

            eventData.surface.overlay.addClass(BlocksConstants.BLOCK_HIGHLIGHT_CLASS);

            //note: the css animation will fadeout the color of the hightlight background,
            // so make sure to cleanup and sync and wipe the class when the css animation is done
            setTimeout(function ()
            {
                eventData.surface.overlay.removeClass(BlocksConstants.BLOCK_HIGHLIGHT_CLASS);
            }, parseInt(BlocksConstants.BLOCK_HIGHLIGHT_DURATION_MILLIS));

        }, parseInt(BlocksConstants.BLOCK_HIGHLIGHT_DELAY_MILLIS));
    });

    $(document).on(Broadcaster.EVENTS.BLOCK.FOCUS, function (event, eventData)
    {
        //if the surface in the data is empty, we'll assume the page should be focused
        if (eventData && eventData.surface && eventData.element) {
            //note: it doesn't make sense to pass the EVENTS.BLOCK.FOCUS event, pass the original one instead
            switchFocus(eventData.surface, eventData.element, event.originalEvent);
        }
        else if (UI.pageSurface) {
            switchFocus(UI.pageSurface, UI.pageSurface.element, event.originalEvent);
        }
    });

    $(document).on(Broadcaster.EVENTS.BLOCK.DELETE, function (event, eventData)
    {
        if (UI.allowDelete) {
            //TODO Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, event);

            var oldHtml = UI.pageSurface.element.html();

            var surface = eventData.surface;

            //save a reference to the parent before it's removed
            var oldParent = surface.parent;
            surface.parent._removeChild(surface);
            if (surface.overlay) {
                surface.overlay.remove();
            }
            postChangeBlock(oldParent);

            Broadcaster.send(Broadcaster.EVENTS.BLOCK.DELETED, event, {
                surface: surface
            });
            //when the block was deleted, the entire page changed
            Broadcaster.send(Broadcaster.EVENTS.PAGE.CHANGED.HTML, event, {
                surface: UI.pageSurface,
                oldValue: oldHtml,
            });

            switchFocus(UI.pageSurface, UI.pageSurface.element, event);
        }
    });

    $(document).on(Broadcaster.EVENTS.BLOCK.CHANGED.HTML, function (event, eventData)
    {
        //don't record changes that happen _inside_ the undo event
        if (!eventData.element || !Undo.isInsideUndoRedo(eventData.element)) {
            Undo.recordHtmlChange(eventData.element, eventData.oldValue, eventData.configElement, eventData.configOldValue, eventData.configNewValue, function (value, action, cmd)
            {
                //we wrapped the listener callback to add a refresh, but the sender could have passed a listener too
                if (eventData.listener) {
                    eventData.listener(value, action, cmd);
                }

                Broadcaster.send(Broadcaster.EVENTS.PAGE.REFRESH);
            });
        }

        Broadcaster.send(Broadcaster.EVENTS.PAGE.REFRESH);
    });

    $(document).on(Broadcaster.EVENTS.BLOCK.CHANGED.ATTRIBUTE, function (event, eventData)
    {
        //don't record changes that happen _inside_ the undo event
        if (!eventData.element || !Undo.isInsideUndoRedo(eventData.element)) {
            Undo.recordAttributeChange(eventData.element, eventData.attribute, eventData.oldValue, eventData.configElement, eventData.configOldValue, eventData.configNewValue, function (value, action, cmd)
            {
                //we wrapped the listener callback to add a refresh, but the sender could have passed a listener too
                if (eventData.listener) {
                    eventData.listener(value, action, cmd);
                }

                Broadcaster.send(Broadcaster.EVENTS.PAGE.REFRESH);
            });
        }

        Broadcaster.send(Broadcaster.EVENTS.PAGE.REFRESH);
    });

    /**
     * Monitor all keystrokes and save the currently pressed ones to UI.keysPressed.
     * To be checked with eg. UI.isKeyPressed(KEYCODE_CTRL)
     */
    $(document).on("keyup keydown", function (e)
    {
        switch (e.type) {
            case "keydown" :
                UI.keysPressed[e.keyCode] = true;

                //it makes more sense to fire actions on key down, not key up
                UI.fireKeystroke(e);

                break;
            case "keyup" :
                delete UI.keysPressed[e.keyCode];
                break;
        }

    });

    //-----PRIVATE METHODS-----
    /**
     * Reorder the DOM so the current html is prepared to add a sidebar element and whatnot.
     */
    var createEditLayout = function ()
    {
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

        // wrap the contents of the body in a separate wrapper element,
        // so we can add the surfaces and sidebar too
        UI.pageContent = $('<div class="' + BlocksConstants.PAGE_CONTENT_CLASS + '" />');
        UI.pageContent.append(UI.body.children().detach());
        UI.body.append(UI.pageContent);
        UI.body.addClass(BlocksConstants.BODY_EDIT_MODE_CLASS);
        //temporarily put them here
        UI.body.append(ignoredBody);
        UI.body.append(UI.sidebar);
    };

    /**
     * The inverse method of createEditLayout()
     * If a html element is specified, we'll destroy the layout on that element instead of the live page
     */
    var destroyEditLayout = function (htmlEl)
    {
        var html = htmlEl ? htmlEl : UI.html;
        var body = htmlEl ? htmlEl.find('body') : UI.body;
        var pageContent = htmlEl ? htmlEl.find('.' + BlocksConstants.PAGE_CONTENT_CLASS) : UI.pageContent;

        // clear the manual container widths
        clearBodyDimensions(html);

        // This is used by a number of import modules to detect edit mode,
        // make sure it's cleared
        body.removeClass(BlocksConstants.BODY_EDIT_MODE_CLASS);

        // While creating the layout, we add/set a lot of styling and classes
        // to help the layout. When these are cleared, an empty attribute is
        // left most of the time, so it makes sense to clean up after ourself
        Commons.removeEmptyAttr(html, 'style', true);
        Commons.removeEmptyAttr(html, 'class', true);

        //this will select all (original) ignored content tags, excluding the placeholders
        //find the ignored content outside the wrapper and detach them, to re-add them later on
        var ignoredContent = body.find('.' + BlocksConstants.PAGE_IGNORE_CLASS + ':not(.' + BlocksConstants.PAGE_CONTENT_CLASS + ' .' + BlocksConstants.PAGE_IGNORE_CLASS + ')').detach();

        //save the inner html of the content wrapper before clearing the body
        //note that this clears the sidebar and overlay containers too
        pageContent = pageContent.children().detach();
        body.empty().append(pageContent);

        //this will loop the ignored content and put them back in the placeholders in-order
        body.find('.' + BlocksConstants.PAGE_IGNORE_CLASS).each(function (idx)
        {
            $(this).replaceWith(ignoredContent[idx]);
        });
    };

    /**
     * Extract and return the body html when we're in edit mode.
     * The idea is to send the entire page html to the server and let it
     * decide what to save server-side.
     *
     * @returns {*|string}
     */
    var getBodyHtml = function ()
    {
        //create a new node out of the full page html
        var html = UI.html.clone();

        //clear the wrappers that were created by the blocks system on the clone
        destroyEditLayout(html);

        //convert from jQuery to html string
        return html[0].outerHTML;
    };

    /**
     * Do a context switch to the clicked block
     *
     * @param surface
     * @param clickedElement
     * @param clickEvent
     */
    var switchFocus = function (surface, clickedElement, clickEvent)
    {
        // if the surface is already in focus, what we really want to do is
        // refresh it and not tear down all sidebar widgets and rebuild them,
        // because it has a lot of side effects (like possible dangling events
        // going to widgets that don't exist anymore)
        if (UI.focusedSurface && UI.focusedSurface === surface) {
            Sidebar.refresh(surface, clickedElement, clickEvent);
        }
        else {
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

            // we need to enable/disable the click events after a little timeout
            // because enabling them right away will let them slip through
            setTimeout(function ()
            {
                Mouse.enableClickEvents(surface.isBlock());
            }, 100);

            UI.focusedSurface = surface;
        }
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

    var loadNewBlockList = function (callback)
    {
        //TODO Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, event);

        var boxDialog = BootstrapDialog.show({
            type: BootstrapDialog.TYPE_DEFAULT,
            title: BlocksMessages.selectFromTheListBelow,
            cssClass: BlocksConstants.NEW_BLOCK_MODAL_CLASS,
            message: BlocksMessages.newBlockLoading,
            buttons: [],
        });

        var data = {};
        var currentTypeof = UI.html.attr('typeof');
        if (currentTypeof) {
            data[BlocksConstants.GET_BLOCKS_TYPEOF_PARAM] = currentTypeof;
        }
        var pageTemplate = UI.html.attr(BlocksConstants.HTML_ROOT_TEMPLATE_ATTR);
        if (pageTemplate) {
            data[BlocksConstants.GET_BLOCKS_TEMPLATE_PARAM] = pageTemplate;
        }

        // show select box with all blocks
        $.getJSON(BlocksConstants.GET_BLOCKS_ENDPOINT, data)
            .done(function (data)
            {
                if (data.length === 0) {
                    Notification.error(BlocksMessages.newBlockEmptyError);

                    if (boxDialog) {
                        boxDialog.close();
                    }
                }
                else if (data.length === 1) {
                    //just take the first one
                    createNewBlock(data[0].name, boxDialog, callback);
                }
                else {
                    var formGroup = $('<div class="form-group" ' + BlocksConstants.FORCE_CLICK_ATTR + ' />');
                    var listGroup = $('<div class="list-group"/>').appendTo(formGroup);
                    for (var i = 0; i < data.length; i++) {

                        var block = data[i];

                        var link = $("<a/>", {
                            "href": "javascript:void(0)",
                            "class": "list-group-item",
                            "data-value": block.name,

                            //when we click on the block, we need to load it's resources
                            //and create a new block at the active dropspot
                            click: function ()
                            {
                                createNewBlock($(this).attr("data-value"), boxDialog, callback);
                            }
                        }).appendTo(listGroup);

                        var preview = $('<div class="preview">' +
                            '<i class="fa ' + (block.icon ? block.icon : 'fa-square-o') + '"></i>' +
                            '</div>').appendTo(link);
                        var caption = $('<div class="caption">' +
                            '<span class="title">' + block.title + '</span>' +
                            '<span class="description">' + block.description + '</span>' +
                            '</div>').appendTo(link);
                    }

                    if (jQuery().perfectScrollbar) {
                        boxDialog.getModalBody().perfectScrollbar();
                    }

                    boxDialog.setMessage(formGroup);
                }
            })
            .fail(function (xhr, textStatus, exception)
            {
                Notification.jsonError(BlocksMessages.newBlockError, xhr, textStatus, exception);

                if (boxDialog) {
                    boxDialog.close();
                }
            })
    };

    var createNewBlock = function (name, dialog, callback)
    {
        if (dialog) {
            //not always very fast, so show the wait dialog
            dialog.setMessage(BlocksMessages.newBlockLoadingResources);
        }

        var data = {};
        data[BlocksConstants.GET_BLOCK_NAME_PARAM] = name;
        $.getJSON(BlocksConstants.GET_BLOCK_ENDPOINT, data)
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

                    if (callback) {
                        callback(block, function onComplete()
                        {
                            addHeadResources(data[BlocksConstants.BLOCK_DATA_PROPERTY_INLINE_SCRIPTS], name + "-in-script", BlocksConstants.BLOCK_DATA_PROPERTY_INLINE_SCRIPTS, true);
                            addHeadResources(data[BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_SCRIPTS], name + "-ex-script", BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_SCRIPTS, true);
                        });
                    }
                }
                else {
                    Notification.error(BlocksMessages.newBlockError, data);
                }
            })
            .fail(function (xhr, textStatus, exception)
            {
                Notification.jsonError(BlocksMessages.newBlockError, xhr, textStatus, exception);
            })
            .always(function ()
            {
                if (dialog) {
                    dialog.close();
                }
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
                        Notification.jsonError(BlocksMessages.loadResourcesError, xhr, textStatus, exception);
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

    var pauseSystem = function (pause)
    {
        //we can only pause/resume a booted system
        if (booted) {
            if (pause) {
                //don't pause an already paused system
                if (Mouse.isEnabled()) {

                    Logger.info('Pausing blocks system');

                    Mouse.enable(false);

                    //make sure we are in a reset state before we actually clear stuff
                    switchFocus(UI.pageSurface, UI.pageSurface.element, event);

                    //note: this will also signal the refresh method to ignore refresh events while rebuilding
                    UI.pageSurface = null;

                    blocks.elements.Surface.clearAllOverlays();

                    Broadcaster.send(Broadcaster.EVENTS.BLOCKS.PAUSED);
                }
            }
            else {
                //only resume a paused system
                if (!Mouse.isEnabled()) {

                    Logger.info('Resuming blocks system');

                    UI.pageSurface = new blocks.elements.Page(UI.pageContent);

                    //the pointer changed, so make sure to call this
                    switchFocus(UI.pageSurface, UI.pageSurface.element, event);

                    Mouse.enable(true);

                    Broadcaster.send(Broadcaster.EVENTS.BLOCKS.RESUMED);
                }
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
                if (!pierceThrough && ((e.data && e.data[UI.FORCE_CLICK_DATA] === true) || (e.originalEvent.data && e.originalEvent.data[UI.FORCE_CLICK_DATA] === true))) {
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
    var enableChangeDetector = function (enable)
    {
        if (enable) {
            //only fire one up if none is running
            if (!dimensionTimer) {
                dimensionTimer = setInterval(refreshPage, dimensionTimeout);
            }

            // Big note here: we used to boot a MutationObserver here (see code around 14/2/19)
            // but it seems like jQuery does some behind-the-scenes attribute-tricks when calculating
            // the dimensions of hidden elements (it temporarily makes them visible), causing attribute-changes
            // inside the refresh, effectively creating an endless event loop that's not easily solvable.
            // Instead of blindly following the mutation observer events, we decided to implement a more
            // controlled refresh-policy (eg. active calling of refresh when the DOM changes instead of
            // watching the DOM with an observer). As a plus, we have a lot more control over the DOM (eg. undo)
            // but on the other side, we need to implement the calls for all actions.
        }
        else {
            if (dimensionTimer) {
                clearInterval(dimensionTimer);
            }
            dimensionTimer = null;
        }
    };

    /**
     * If you want to change the check-rate of the dimension timer
     *
     * @param newTimeout
     */
    var setDimensionTimeout = function (newTimeout)
    {
        dimensionTimeout = newTimeout;

        //only reboot if it's currently running
        if (dimensionTimer) {
            enableChangeDetector(false);
            enableChangeDetector(true);
        }
    };

    /**
     * The callback function that will be called by the dimension timer, see above.
     * If force is true, an update is forced, even though the surrounding dimensions didn't change.
     */
    var refreshPage = function (force)
    {
        var windowWidth = UI.window.width();
        var windowHeight = UI.window.height();

        //no need to do any refreshes if we have nothing to refresh
        if (!Commons.isUnset(UI.pageSurface)) {

            //note how these cascade as one depends on the other from outer structure to inner
            var refreshNeeded = !dimensions || force === true;
            refreshNeeded = refreshNeeded || !dimensions.window || dimensions.window.width !== windowWidth || dimensions.window.height !== windowHeight;
            refreshNeeded = refreshNeeded || !dimensions.body || dimensions.body.width !== UI.body.width() || dimensions.body.height !== UI.body.height();
            refreshNeeded = refreshNeeded || !dimensions.pageContent || dimensions.pageContent.width !== UI.pageContent.width() || dimensions.pageContent.height !== UI.pageContent.height();
            refreshNeeded = refreshNeeded || !dimensions.sidebar || dimensions.sidebar.width !== UI.sidebar.width();

            if (refreshNeeded) {

                Logger.info('Refreshing page');

                dimensions = dimensions || {
                    window: {},
                    body: {},
                    pageContent: {},
                    sidebar: {},
                };

                //sync this with the buildup of refreshNeeded above
                var sidebarChanged = !dimensions.sidebar || dimensions.sidebar.width !== UI.sidebar.width();

                //update the stored dimensions
                dimensions.window.width = windowWidth;
                dimensions.window.height = windowHeight;
                dimensions.body.width = UI.body.width();
                dimensions.body.height = UI.body.height();
                dimensions.pageContent.width = UI.pageContent.width();
                dimensions.pageContent.height = UI.pageContent.height();
                dimensions.sidebar.width = UI.sidebar.width();

                // If we have a sidebar, we need to sync the width of the content to the width of the sidebar
                // because when resizing the sidebar, only its width is set and a refresh is called
                if (sidebarChanged && UI.sidebar) {
                    // Let's keep a small margin between the website and our sidebar
                    updateBodyDimensions(windowWidth - dimensions.sidebar.width - BlocksConstants.SIDEBAR_MARGIN_LEFT_PX);
                }

                //update the model and it's dimensions
                //note: it must come before the pause call because that can possibly
                // make the pageSurface null
                UI.pageSurface._refresh(true);
            }
        }

        // deactivate the system when page is too small
        // note: this won't do anything if it's already in the right state
        // note: keep this outside the pageSurface check or resume won't work
        pauseSystem(windowWidth < MIN_WINDOW_WIDTH);
    };

    var updateBodyDimensions = function (newWidth)
    {
        // This resizing needs some explaining. We can basically resize three parent elements:
        //
        // 1) the window/viewport
        //    Altering the viewport dynamically would be our best bet, because all media-queries in the stylesheets would
        //    be activated and the page would scale natively, just like when the window is resized. However, it doesn't work as expected.
        //    Turns out that overriding the viewport is possible on mobile to emulate a desktop page,
        //    but not the other way around (shrinking a desktop page to it's mobile variant).
        //    We could simulate this behaviour by adding virtual classes to the body and setting them in the css media queries
        //    as well, but that would mean every developer needs to do this: unacceptable.
        //
        // 2) the 'virtual body' element: our pageContent wrapper element that holds the main html elements
        //    By scaling the wrapper together with the sidebar, the body scales nicely up until the new width of the wrapper
        //    'hits' the width of the container, because in Bootstrap, the containers have explicit widths set:
        //
        //    @media (min-width: 768px) {
        //      .container {
        //        width: 750px;
        //      }
        //    }
        //    @media (min-width: 992px) {
        //      .container {
        //        width: 970px;
        //      }
        //    }
        //    @media (min-width: 1200px) {
        //      .container {
        //        width: 1170px;
        //      }
        //    }
        //
        //    When the wrapper gets smaller than this container width (eg. especially on small screens), the sidebar starts floating over the body
        //    and parts of the content get hidden by the sidebar. We could argue this is not that bad (keeping the WYSIWYG paradigm in mind),
        //    but on small screens, this happens fairly quickly and it's pretty annoying.
        //
        // 3) the Bootstrap .container element (because it's supposed to be the single containing element that holds all layout elements)
        //    Since this container has an explicit width set in css, it makes sense to change it, by overriding it in the style attribute.
        //    Note that this means the container is squeezed together, without the media queries of the css being activated. Eg. the container
        //    will be layout just like in regular desktop mode (assuming we're on a desktop), but only in a smaller width. Meaning, no columns
        //    will jump underneath other columns like on mobile.
        //    This is actually a good thing, because it means we can keep layouting the page in desktop mode, without having the sidebar obfuscating
        //    the content, even on smaller screens.
        //
        // 4) a combination of 2 & 3
        //    Actually, we don't want to set the width on the Bootstrap container explicitly, only if it's width is larger
        //    than the remaining space next to the sidebar. However, checking if the container 'fits' into that space is hard.
        //    Therefore, we use a little trick and set the width of the wrapper first, then clear the explicit widths on the containers
        //    only to re-add them if the largest container dimension would be larger than the width of the wrapper.
        //    Note that setting the width of the wrapper to the remaining space is always a good idea (to preserve container alignment, etc)
        //
        // ----> Don't forget to sync the chosen implementation with the clearBodyDimensions() method!

        // Option 1, disabled, doesn't work
        //$('head meta[name=viewport]').attr('content', 'width=' + pageWidth + ',initial-scale=1.0,maximum-scale=1.0,user-scalable=0');

        // Option 2, disabled
        // UI.pageContent.css("width", newWidth + "px");

        // Option 3, disabled
        // UI.containers.css("width", newWidth + "px");

        // Option 4
        // 4a) set the width on the wrapper
        UI.pageContent.css("width", newWidth + "px");
        // 4b) clear the explicit widths
        UI.containers.css("width", "");
        // 4c) find the largest container
        var maxContainerWidth = Math.max.apply(Math, UI.containers.map(function ()
        {
            return $(this).outerWidth();
        }).get());
        // 4d) alter the container only if it's too large
        if (maxContainerWidth > newWidth) {
            UI.containers.css("width", newWidth + "px");
        }

        //Update: now solved in css, but if needed, make sure to clear the height in clearBodyDimensions()
        // // Also sync the page _height_ to the body to make sure the page content wrapper
        // // is at least (not any more, see note below) the height of the body,
        // // so eg. sticky footers also work the same way when the sidebar is open.
        // // Note: we must always set the height to the body height (not only if the content is larger)
        // // because we want the page content to scroll independently from the sidebar (css is set to overflow-y auto)
        // var bodyBottom = UI.html.position().top + UI.html.outerHeight(true);
        // UI.pageContent.outerHeight(bodyBottom - UI.pageContent.position().top);
    };

    /**
     * This is more or less the inverse of updateBodyDimensions() and clears all manual attributes
     * as a result of setting them explicitly when resizing the sidebar.
     * If a html element is specified, we'll clear the dimensions on that element instead of the live page
     */
    var clearBodyDimensions = function (htmlEl)
    {
        var pageContent = htmlEl ? htmlEl.find('.' + BlocksConstants.PAGE_CONTENT_CLASS) : UI.pageContent;
        var containers = htmlEl ? htmlEl.find(CONTAINERS_SELECTOR) : UI.containers;

        if (pageContent) {
            pageContent.css("width", "");
            Commons.removeEmptyStyle(pageContent);
        }

        if (containers) {
            containers.css("width", "");
            Commons.removeEmptyStyle(containers);
        }
    };

}]);
