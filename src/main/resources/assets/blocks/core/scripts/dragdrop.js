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
 * Plugin that handles the dragging of blocks
 *
 * While dragging we create 2 dropPointers, "anchor" and "other".
 * Droppointers are Dom elements that overlay the block that takes the drop. (anchor) If we
 * drop between 2 blocks, we also overlay the other block (other)
 * We show arrows in the overlay to indicate the direction the block will move.
 */
base.plugin("blocks.core.DragDrop", ["blocks.core.Broadcaster", "blocks.core.Layouter", "constants.base.core.internal", "constants.blocks.core", "blocks.core.Hover", "messages.blocks.core", "blocks.core.Notification", "blocks.core.Mouse", "blocks.core.DOM", function (Broadcaster, Layouter, BaseConstantsInternal, BlocksConstants, Hover, BlocksMessages, Notification, Mouse, DOM)
{
    var DragDrop = this;
    var draggingEnabled = false;
    var dragging = false;
    var dropPointerElements = null;
    var lastDropLocation = null;
    var currentDraggedBlock = null;
    var sidebarLeft = null;
    var oldDirection = {
        dir: BaseConstantsInternal.DIRECTION.NONE,
        stamp: $.now()
    };

    // Simple object to translate SIDE in css border side
    var cssSide = {};
    cssSide[BaseConstantsInternal.SIDE.TOP] = "top";
    cssSide[BaseConstantsInternal.SIDE.BOTTOM] = "bottom";
    cssSide[BaseConstantsInternal.SIDE.LEFT] = "left";
    cssSide[BaseConstantsInternal.SIDE.RIGHT] = "right";

    var cssBorderSide = {};
    cssBorderSide[BaseConstantsInternal.SIDE.TOP] = "border-" + cssSide[BaseConstantsInternal.SIDE.TOP];
    cssBorderSide[BaseConstantsInternal.SIDE.BOTTOM] = "border-" + cssSide[BaseConstantsInternal.SIDE.BOTTOM];
    cssBorderSide[BaseConstantsInternal.SIDE.LEFT] = "border-" + cssSide[BaseConstantsInternal.SIDE.LEFT];
    cssBorderSide[BaseConstantsInternal.SIDE.RIGHT] = "border-" + cssSide[BaseConstantsInternal.SIDE.RIGHT];

    /*
     * METHODS CALLED WHILE DRAGGING
     **/

    this.setActive = function (value)
    {
        draggingEnabled = value;
        currentDraggedBlock = null;
    };

    this.isDragging = function ()
    {
        return draggingEnabled && dragging;
    };

    this.dragStarted = function (blockEvent, eventData)
    {
        sidebarLeft = $("." + BlocksConstants.PAGE_SIDEBAR_CLASS).offset().left;

        //Broadcaster.zoom();
        oldDirection = {
            dir: BaseConstantsInternal.DIRECTION.NONE,
            stamp: $.now()
        };

        //passed in from mouse.js (the original block we were over when starting to drag)
        currentDraggedBlock = eventData.block;

        //we're dragging an existing block
        if (draggingEnabled && currentDraggedBlock != null && currentDraggedBlock.canDrag && currentDraggedBlock.getTotalBlocks() > 1) {
            //currentDraggedBlock.getContainer().createAllDropspots();

            //mark the current overlay as being dragged
            currentDraggedBlock.overlay.addClass(BlocksConstants.OVERLAY_DRAGGING_CLASS);

            Hover.getFocusedBlock().createAllDropspots();
            createDropPointerElement();
            dragging = true;
            //Hover.removeResizeHandles();
            // we have to set both
            // html for undefined area and baody to override default cursor of body.

            $("body").addClass(BlocksConstants.FORCE_DRAG_CURSOR_CLASS);
        }
        //we're dragging a new block
        else if (currentDraggedBlock == null) {
            dragging = true;
            Hover.getFocusedBlock().createAllDropspots();
            //Hover.removeResizeHandles();
            createDropPointerElement();
            $("body").addClass(BlocksConstants.FORCE_DRAG_CURSOR_CLASS);
        }
    };

    this.dragOverBlock = function (blockEvent, eventData)
    {
        var pageX = blockEvent.originalEvent.pageX;
        var pageY = blockEvent.originalEvent.pageY;

        //Logger.debug("Dragging block at "+pageX+","+pageY);

        var dropBlock = eventData.block;

        //Logger.debug("Dropblock: "+dropBlock);

        if (dragging) {

            // find the triggered dropspot
            // dropspot has an "anchor" block and sometimes "other" (when dropping between 2 columns, 2 rows, 2 templates)
            var dropSpot = null;

            if (dropBlock != null && dropBlock.canDrag) {
                var direction = Mouse.intersectingSide(dropBlock);

                // when outside container block of last droplocation is null
                var now = $.now();
                //will prevent the dropspots from flickering when changing fast
                if (now - oldDirection.stamp > 100) {
                    if (direction != oldDirection.dir && !(lastDropLocation != null && lastDropLocation.block == null)) {
                        //Logger.debug("Direction changed " + direction + " from " + oldDirection.dir);
                        dropBlock.recalculateTriggers(direction, pageX, pageY, lastDropLocation);
                    }
                    oldDirection = {
                        dir: direction,
                        stamp: now
                    };
                }

                dropSpot = dropBlock.getTriggeredDropspot(direction, pageX, pageY);
                if (dropSpot == null && dropBlock != null) {
                    dropBlock.recalculateTriggers(direction, pageX, pageY, null);
                    dropSpot = dropBlock.getTriggeredDropspot(direction, pageX, pageY);
                }
            }
            //check if we're dragging the create block from the sidebar
            else if (sidebarLeft == null || sidebarLeft > pageX) {
                var container = Hover.getFocusedBlock().getLayoutContainer();

                if (pageY > container.top && pageY < container.bottom) {
                    if (pageX < container.left) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstantsInternal.SIDE.LEFT, container, 0);
                    } else if (pageX > container.right) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstantsInternal.SIDE.RIGHT, container, 0);
                    }
                } else if (pageX > container.left && pageX < container.right) {
                    if (pageY < container.top) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstantsInternal.SIDE.TOP, container, 0);
                    } else if (pageY > container.bottom) {
                        dropSpot = new blocks.elements.Dropspot(BaseConstantsInternal.SIDE.BOTTOM, container, 0);
                    }
                }
                if (dropSpot != null) {
                    dropSpot.block = container;
                }
            }

            // check if something changed
            if (lastDropLocation != dropSpot) {
                //check if we're in between dropspots
                if (dropSpot != null) {
                    if (insideWindow(blockEvent.clientX, blockEvent.clientY)) {
                        // We can not drop on ourselves so skip
                        if (!dropSpotInDraggedBlock(dropSpot)) {
                            // only update the last location when all is well;
                            // this will enable us to drop outside a block (see later)
                            // and make the UI more stable
                            lastDropLocation = dropSpot;

                            //Logger.debug("Dropspot changed with min: " + lastDropLocation.min + " - " + lastDropLocation.max);
                            // show overlays for our droplocation(s)
                            drawDropPointerElement(lastDropLocation.anchor, lastDropLocation.side);
                        }
                        else {
                            // this will make sure we can abort the DnD by releasing
                            // the mouse in the dragged block
                            lastDropLocation = null;
                            hideDropPointerElements();
                        }
                    }
                    else {
                        // this will make sure we can abort the DnD by releasing
                        // the mouse outside of the window
                        lastDropLocation = null;
                        hideDropPointerElements();
                    }
                } else {
                    // don't hide the dropspot or update the lastDropLocation when we're not above a block
                    // this will show the last shown dropspot when dragging away,
                    // but will make the UI a lot more stable
                    //lastDropLocation = dropSpot;
                    //hideDropPointerElements();

                    //Logger.debug("Dropspot is null");
                }
            } else {
                // Do nothing because nothing changed
                //Logger.debug("dropspot not changed");
                //Logger.debug(lastDropLocation);
            }
        }
    };

    this.dragEnded = function (blockEvent)
    {
        if (dragging) {
            var dropX = blockEvent.originalEvent.clientX;
            var dropY = blockEvent.originalEvent.clientY;

            // check for null (e.g during abort_drag)
            // If we did not drop on ourself, change location
            if (currentDraggedBlock != null && lastDropLocation != null && !dropSpotInDraggedBlock(lastDropLocation) && insideWindow(dropX, dropY)) {
                //Logger.debug("Drop block");
                //Hover.removeHoverOverlays();
                resetDragDrop();
                Layouter.changeBlockLocation(currentDraggedBlock, lastDropLocation.anchor, lastDropLocation.side);
            }
            //we added a new block
            else if (currentDraggedBlock == null && lastDropLocation != null && insideWindow(dropX, dropY)) {

                // We added a new block
                Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, blockEvent);
                //Hover.removeHoverOverlays();

                // show normal cursor during dialog
                $("body").removeClass(BlocksConstants.FORCE_DRAG_CURSOR_CLASS);
                // Remove all pointer elements
                removeDropPointerElements();
                //removeDraggedOverlay();

                // show select box with all blocks
                var boxDialog;
                //Note: the inner div will be replaced when the new load() content comes in
                var box = $('<div><div style="padding: 20px;">' + BlocksMessages.newBlockLoading + '</div></div>');

                var html = $('html');
                var endpointUrlParams = '';
                var currentTypeof = html.attr('typeof');
                if (currentTypeof) {
                    endpointUrlParams += endpointUrlParams == '' ? '?' : '&';
                    endpointUrlParams += BlocksConstants.GET_BLOCKS_TYPEOF_PARAM + '=' + encodeURIComponent(currentTypeof);
                }
                var pageTemplate = html.attr(BlocksConstants.HTML_ROOT_TEMPLATE_ATTR);
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
                                    addHeadResources(data[BlocksConstants.BLOCK_DATA_PROPERTY_INLINE_STYLES], name + "-in-style", BlocksConstants.BLOCK_DATA_PROPERTY_INLINE_STYLES);
                                    addHeadResources(data[BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_STYLES], name + "-ex-style", BlocksConstants.BLOCK_DATA_PROPERTY_EXTERNAL_STYLES);

                                    if (data[BlocksConstants.BLOCK_DATA_PROPERTY_HTML] && data[BlocksConstants.BLOCK_DATA_PROPERTY_HTML] !== "") {
                                        // whow, this is weird stuff!
                                        // Originally just $(data.html), but docs say the current version is safer.
                                        // Problem was it failed with certains custom elements:
                                        // th-search didn't work, where div-search did work.
                                        // Seems to be a bug in JQuery: https://github.com/jquery/jquery/issues/1987
                                        // Fixed with a patched version (see pom.xml)
                                        var block = $($.parseHTML($.trim(data[BlocksConstants.BLOCK_DATA_PROPERTY_HTML])));

                                        //Hover.removeHoverOverlays();
                                        resetDragDrop();
                                        cancelled = false;
                                        Layouter.addNewBlockAtLocation(block, lastDropLocation.anchor, lastDropLocation.side, function onComplete()
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

                var cancelled = true;
                boxDialog = BootstrapDialog.show({
                    title: BlocksMessages.selectFromTheListBelow,
                    cssClass: BlocksConstants.NEW_BLOCK_MODAL_CLASS,
                    message: function ()
                    {
                        return box
                    },
                    buttons: [],
                    onhidden: function ()
                    {
                        if (cancelled) {
                            DragDrop.dragAborted();
                            Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS, blockEvent);
                        }
                    }
                });

            }
            else {
                //Logger.debug("No drop for block");
                this.dragAborted();
                Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS, blockEvent);
            }
        }

        sidebarLeft = null;
        currentDraggedBlock = null;
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

    var resetDragDrop = function ()
    {
        $("body").removeClass(BlocksConstants.FORCE_DRAG_CURSOR_CLASS);
        removeDropPointerElements();
        $('.' + BlocksConstants.OVERLAY_CLASS + '.' + BlocksConstants.OVERLAY_DRAGGING_CLASS).removeClass(BlocksConstants.OVERLAY_DRAGGING_CLASS);
        //removeDraggedOverlay();
        draggingEnabled = false;
        dragging = false;
    };

    this.dragAborted = function ()
    {
        resetDragDrop();
    };

    /*
     * checks if the droplocation (or dropspot) equals the block we are dragging
     */
    var dropSpotInDraggedBlock = function (dropSpot)
    {
        var retVal = false;

        if (dropSpot == null) {
            retVal = true;
        }
        else if (currentDraggedBlock == null) {
            retVal = false;
        }
        else {
            // dragged block equals anchor or other
            if ((currentDraggedBlock === dropSpot.anchor || currentDraggedBlock === dropSpot.other)) {
                retVal = true;
            }
            else if (dropSpot.anchor != null && dropSpot.anchor.children.length == 1 && dropSpot.anchor.children[0] === currentDraggedBlock) {
                retVal = true;
            }
            else if (dropSpot.other != null && dropSpot.other.children.length == 1 && dropSpot.other.children[0] === currentDraggedBlock) {
                retVal = true;
            }
        }

        //Logger.debug("Dropspot is dragged block? "+retVal);

        return retVal;
    };

    var insideWindow = function (x, y)
    {
        //Logger.debug("Checking bounds for "+x+","+y+" inside "+sidebarLeft+" and "+window.innerWidth+"x"+window.innerHeight);

        if (x < 0 || x > window.innerWidth || x > sidebarLeft || y < 0 || y > window.innerHeight) {
            return false;
        } else {
            return true;
        }
    };

    // create drop pointer as element in DOM
    var createDropPointerElement = function ()
    {
        //Logger.debug("create droppointer ");
        if (dropPointerElements == null) {
            dropPointerElements = $("<div class='" + BlocksConstants.BLOCKS_DROPSPOT_CLASS + "' />");
            dropPointerElements.css("position", "absolute");
            $("body").append(dropPointerElements);
        }
        hideDropPointerElements();
    };

    // remove droppointer as element in dom
    var removeDropPointerElements = function ()
    {
        if (dropPointerElements != null) {
            dropPointerElements.remove();
            dropPointerElements = null;
        }
    };

    // update droppointer in dom
    // name = anchor or other
    var drawDropPointerElement = function (surface, side)
    {
        if (surface != null) {
            var top = surface.top;
            var left = surface.left;
            var width = surface.right - surface.left;
            var height = surface.bottom - surface.top;

            // Instead of showing the drop surface with the border of a bounding box, we'll just draw the border
            // as a box itself, so we can use the background css to style it
            var offset = BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH / 2.0;
            if (side == BaseConstantsInternal.SIDE.TOP) {
                top = surface.top - offset;
                height = BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH;
            }
            else if (side == BaseConstantsInternal.SIDE.RIGHT) {
                left = surface.right - offset;
                width = BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH;
            }
            else if (side == BaseConstantsInternal.SIDE.BOTTOM) {
                top = surface.bottom - offset;
                height = BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH;
            }
            else if (side == BaseConstantsInternal.SIDE.LEFT) {
                left = surface.left - offset;
                width = BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH;
            }

            dropPointerElements.css("top", top + "px");
            dropPointerElements.css("left", left + "px");
            dropPointerElements.css("width", width + "px");
            dropPointerElements.css("height", height + "px");
            for (var i = 1; i < 5; i++) {
                dropPointerElements.removeClass(BlocksConstants.BLOCKS_DROPSPOT_CLASS + "-" + cssSide[i]);
            }
            dropPointerElements.addClass(BlocksConstants.BLOCKS_DROPSPOT_CLASS + "-" + cssSide[side]);

            dropPointerElements.show();

        } else {
            hideDropPointerElements();
        }
    };

    function isElementInViewport(el)
    {
        //special bonus for those using jQuery
        if (typeof jQuery === "function" && el instanceof jQuery) {
            el = el[0];
        }

        var rect = el.getBoundingClientRect();

        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) && /*or $(window).height() */
            rect.right <= (window.innerWidth || document.documentElement.clientWidth) /*or $(window).width() */
        );
    }

    var hideDropPointerElements = function ()
    {
        //Logger.debug("hide droppointer element");
        if (dropPointerElements != null) {
            dropPointerElements.hide();
        }
    };

}]);



