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
 * The Mouse plugin converts jQuery events to blocks Events
 *
 * A block Event has the following parameters
 *      event: jQuery event
 *      block: the current BLOCK (See Elements) where the event happenend
 *      direction: the direction the mouse pointer is moving (based on the last 10 mouseevents).
 *          Options are:
 *          - Constants.DIRECTION.UP
 *          - Constants.DIRECTION.DOWN
 *          - Constants.DIRECTION.LEFT
 *          - Constants.DIRECTION.RIGHT
 *      draggingStatus: current dragging status:
 *          Constants.DRAGGING.NOT_ALLOWED: dragging is not allowed and can and will not happen
 *                             CAN_START_DRAG: dragging could be started, an item that can be
 *                                             dragged is 'selected'
 *                             CAN_NOT_START_DRAG: dragging can not be started because
 *                                                 there is no draggable item 'selected'
 *                             WAITING: dragging is started for an element but the threshold, set
 *                                      to really start the drag is not yet reached
 *                             YES: currently dragging an object
 *
 *      draggingOptions: only available when draggingStatus == YES
 *          surface: the current surface that is dragged (See Elements)
 *          startEvent: the jquery event on the start of the drag
 *
 *  the mouse plugin sends the following EVENTS. They all send blockEvents as parameter:
 *
 *  HOVER_ENTER_BLOCK: Mouse hovers over a block (not dragging)
 *  HOVER_LEAVE_BLOCK: Mouse leaves a block (not dragging)
 *  HOVER_OVER_BLOCK: Mouse hovers ovr a block (not dragging)
 *
 *  START_DRAG: dragging started
 *  END_DRAG: dragging ended
 *  ABORT_DRAG: dragging aborts (e.g. mouse is outside window)
 *  DRAG_ENTER_BLOCK: a new block was entered while dragging
 *  DRAG_LEAVE_BLOCK: a block was left while dragging
 *  DRAG_OVER_BLOCK: dragging over a block
 *
 *  The mouse plugin listens for the following events:
 *  CAN_START_DRAG: a surface is selected so user could start dragging
 *                  takes options object parameter:
 *                      options.surface: the selected surface
 *                      options.priority: priority of the surface. If an other unknown surface is
 *                                        already selected, then this selected surface will be replaced
 *                                        with the new surface if the new priority is lower then the
 *                                        current priority or if the new surface is of the same kind
 *  CAN_NOT_START_DRAG: a surface is no longer selected so the user can no longer start drag
 *                  takes options object parameter:
 *                      options.surface: the surface that was selected. If this event is received with a different surface
 *                                       then the selected surface, this event will be ignored
 *                      if options == null, then the selected surface will be removed.
 *
 *  ACTIVATE_MOUSE: activate this module
 *  DEACTIVATE_MOUSE: deactivate this module, no events will be send
 *  ALLOW_DRAG: allow dragging
 *  DISABLE_DND: do not allow dragging, even if a surface is selected
 *
 *
 */
base.plugin("blocks.core.Mouse", ["blocks.core.Broadcaster", "blocks.core.Layouter", "constants.base.core.internal", "constants.blocks.core", "blocks.core.Sidebar", "blocks.core.Hover", "blocks.core.UI", function (Broadcaster, Layouter, BaseConstantsInternal, BlocksConstants, SideBar, Hover, UI)
{
    var Mouse = this;

    //-----CONSTANTS-----
    // The minimum number of pixels we need to move before real dragging starts.
    // Note: watch out with this value: it should be smaller than the smallest possible object in the layout system (width or height)
    // but when clicking near the edge of such an object, even smaller; so maybe TODO: activate the DnD when entering a new block?
    var DRAGGING_THRESHOLD = 3;
    var SHOW_DEBUG_LINES = true;
    //the size of the history window to keep during dragging
    var WINDOW_SIZE = 20;
    var DIRECTION_MULTIPLIER = 10000;

    //-----VARIABLES-----
    // flag to enable/disable this entire module (both clicking and dragging)
    var active = false;

    // flag to enable/disable layout functionality (create, resize, move and delete)
    var enableLayout = false;

    // flag to enable/disable editing of existing blocks (giving them focus to start editing)
    var enableEdit = false;

    // if we're really dragging, clicking or nothing at all
    var draggingStatus = BaseConstantsInternal.DRAGGING.NO;

    // the surface that's currently being dragged around
    var draggingSurface = null;

    // the original event that started the drag
    var draggingStartEvent = null;

    //this keeps aggregated values and an array of history entries of maximum size WINDOW_SIZE
    //that will keep track of the last mousemove event statistics
    var stats = {

        //the index of the last (current) event in the events array
        idx: 0,
        //a floating window of (max) size WINDOW_SIZE
        events: [],

        //the floating sum of the angle sines
        sinSum: 0,
        //the floating sum of the angle cosines
        cosSum: 0,
        //the floating sum of the total lengths
        totalLength: 0,
        //the floating sum of the timeDiffs
        totalTime: 0,

        //the circular variance of all the angles
        variance: 0,
        //the mean angle of all the angles
        direction: 0,
        //the current speed in pixels per second
        speed: 0,
    };

    //the current general (mean) direction vector we're dragging in
    var dragVector = {
        x1: 0, y1: 0,
        x2: 0, y2: 0
    };

    var debugCanvas = null;

    //-----PUBLIC METHODS-----
    /**
     * Activate this module and start listening for mouse click events (and DnD)
     */
    this.activate = function ()
    {
        if (!active) {

            //put this as fast as possible to eliminate double event registering
            active = true;

            //make sure we start with a clean slate
            _resetMouse();

            $(document).on("mousedown.blocks_core", function (event)
            {
                if (active) {
                    if (event.which == 1) {
                        _mouseDown(event);
                    }
                    else {
                        _mouseCancel(event);
                    }
                }
            });

            $(document).on("mouseup.blocks_core", function (event)
            {
                if (active) {
                    if (event.which == 1) {
                        _mouseUp(event);
                    }
                    else {
                        _mouseCancel(event);
                    }
                }
            });

            //this is important:
            // - if we're dragging, we need a way to cancel and moving outside the window feels like a good escape-gesture
            // - if we're dragging and we're switching context to another window (alt-tab), we should probably abort
            $(document).on("mouseleave.blocks_core", function (event)
            {
                _mouseCancel(event);
            });
        }
    };

    /**
     * Deactivate this module and stop listening for mouse click events (and DnD)
     */
    this.deactivate = function ()
    {
        if (active) {
            active = false;

            //reset all tracking variables
            _resetMouse();

            //remove all event listeners
            $(document).off("mousedown.blocks_core");
            $(document).off("mouseup.blocks_core");
            $(document).off("mousemove.blocks_core");
            $(document).off("mouseleave.blocks_core");
        }
    };

    /**
     * enable/disable layout functionality (create, resize, move and delete)
     */
    this.enableLayout = function (enable)
    {
        enableLayout = enable;
    };

    /**
     * enable/disable editing of existing blocks (giving them focus to start editing)
     */
    this.enableEdit = function (enable)
    {
        enableEdit = enable;
    };

    //-----PRIVATE METHODS-----
    /**
     * Starts a mouse session that will result in one of:
     *  - click a block (if the move-threshold is not exceeded before release)
     *  - start dragging a block (if the move-threshold is exceeded)
     *  - the create-block-button is dragged (and the move-threshold is exceeded)
     * @param event
     * @private
     */
    var _mouseDown = function (event)
    {
        //if both edit and layout are disabled, we have nothing to do
        if (enableEdit || enableLayout) {
            // this variable more or less controls if we're dragging a new block or not,
            // so make sure it's null when we're dragging the new-block button around
            var surface = null;
            var creatingNew = false;
            var element = $(event.target);
            Logger.info('mousedown on ', event);
            if (element.hasClass(BlocksConstants.CREATE_BLOCK_CLASS) || element.parents("." + BlocksConstants.CREATE_BLOCK_CLASS).length > 0) {
                creatingNew = true;
            }
            // If the element we click on has a registered ID attribute and we can find it in our surface model,
            // we clicked on a valid surface. Note that we only have block and resizer surfaces, so we'll always
            // click on one of them (or nothing at all), but this is good time to dig a little deeper and see
            // if we didn't click on a property inside a block instead. Because those are the 'real' hotspots
            // (the block overlays are just easier for the end-user to understand) and the only parts
            // that will get saved after changing them, so it makes sense to work with them instead of the blocks.
            else if (element.hasAttribute(blocks.elements.Surface.INDEX_ATTR)) {
                surface = blocks.elements.Surface.INDEX[element.attr(blocks.elements.Surface.INDEX_ATTR)];

                // //dig deeper
                // if (surface) {
                //
                //     var propertySurface = surface.childAt(event.pageX, event.pageY);
                //     if (propertySurface) {
                //         surface = propertySurface;
                //     }
                //     else {
                //         //It's possible we didn't find a sub-property
                //         // eg. when it's a resize handle or we just
                //         // want to edit some attributes of the block itself
                //         // and it's saved by a parent property-element
                //     }
                // }
            }

            //we need this to enable sidebar.js to know on which element we really clicked (instead of click-events on the overlay)
            //TODO note: there's an error here and we should refactor this: eg. try to click on a video's play button and because of this class being activated, the mouseUp event is never received...
            //UI.overlayWrapper.addClass(BlocksConstants.BLOCK_OVERLAY_NO_EVENTS_CLASS);

            //four options:
            // 1) we clicked on a valid surface -> block holds a surface object and creatingNew is false
            // 2) we clicked on a resize handle -> block holds a resizer surface object and creatingNew is false
            // 3) we clicked on the create new button -> block is null and creatingNew is true
            // 4) we clicked outside any surface -> block is undefined and creatingNew is false

            //before setting the tracking variables, we make sure we start with a clean slate because mousedown starts everything
            _resetMouse();

            // options 1-3
            if (surface || creatingNew) {
                draggingStatus = BaseConstantsInternal.DRAGGING.WAITING;
                draggingSurface = surface;
                draggingStartEvent = event;

                //start listening for mouse movement
                $(document).on("mousemove.blocks_core", function (event)
                {
                    if (active) {
                        // note: we don't check which mouse button is pressed
                        // because docs say not all browsers set it and we already
                        // set it during mousedown.
                        // It should be safe for incoming drags (that weren't initiated by us),
                        // because we abort on mouseleave and this mousemove is only booted here.
                        // See https://stackoverflow.com/questions/322378/javascript-check-if-mouse-button-down
                        _mouseMove(event);
                    }
                });
            }
            // option 4
            else {
                draggingStatus = BaseConstantsInternal.DRAGGING.NO;
            }
        }
    };

    /**
     * Tracks an active dragging session
     *
     * @param event
     * @private
     */
    var _mouseMove = function (event)
    {
        Logger.info('mouse move');

        //this should always be true since the mousemove is only installed on a correct mousedown
        //but let's check it anyway
        if (draggingStatus != BaseConstantsInternal.DRAGGING.NO) {

            //first, update the statistics and direction and speed vectors
            updateVector(event);

            //we're waiting for the threshold to be exceeded, but only if we're allowed to layout, otherwise, we just allow a click
            if (draggingStatus == BaseConstantsInternal.DRAGGING.WAITING && enableLayout && stats.totalLength > DRAGGING_THRESHOLD) {

                draggingStatus = BaseConstantsInternal.DRAGGING.YES;

                //var overlays = $('.' + BlocksConstants.BLOCK_OVERLAY_CLASS);
                //we need this to enable sidebar.js to know on which element we really clicked (instead of click-events on the overlay)
                //overlays.removeClass(BlocksConstants.BLOCK_OVERLAY_NO_EVENTS_CLASS);

                //don't show the hover effects while dragging; it blocks the visibility of the lines in between
                //overlays.addClass(BlocksConstants.BLOCK_OVERLAY_BLOCK_HOVER);

                //UI.showOverlays(false);

                // //pass this along with the custom event data object
                // Broadcaster.send(Broadcaster.EVENTS.START_DRAG, event, {
                //     //we'll pass the block we initially had our cursor over (even before the wait threshold)
                //     block: startBlock
                // });
            }
            //we're past the threshold and dragging a block around
            else if (draggingStatus == BaseConstantsInternal.DRAGGING.YES) {

                // var block = Hover.getHoveredBlock();
                //
                // Broadcaster.send(Broadcaster.EVENTS.DRAG_OVER_BLOCK, event, {
                //     block: block
                // });
            }
        }
    };

    /**
     * Stops an active and successful mouse session and determine what to do.
     *
     * @param event
     * @private
     */
    var _mouseUp = function (event)
    {
        if (SHOW_DEBUG_LINES && debugCanvas) {
            debugCanvas.remove();
            debugCanvas = null;
        }

        if (draggingStatus != BaseConstantsInternal.DRAGGING.NO) {

            //the low level html element we clicked on
            var element = $(event.target);
            Logger.info('mouseup on ', element[0]);

            if (draggingStatus == BaseConstantsInternal.DRAGGING.YES) {
                //Broadcaster.send(Broadcaster.EVENTS.END_DRAG, event);
            }
            // this means we were dragging, but haven't exceeded the threshold yet
            // so, instead of starting a drag, we did a click
            else if (draggingStatus == BaseConstantsInternal.DRAGGING.WAITING) {

                //Note: when we click the new block button, draggingSurface will be null
                // and the popover will do it's work, so we have no else clause
                // Also, if we can't edit, we just disable the focus of a block
                if (enableEdit && draggingSurface) {

                    Broadcaster.send(Broadcaster.EVENTS.FOCUS_BLOCK, event, {
                        surface: draggingSurface,
                        event: event,
                    });

                    //Broadcaster.send(Broadcaster.EVENTS.FOCUS_BLOCK, event, draggingSurface);

                    //var hoverObj = Hover.createHoverClickObject(draggingSurface, element, event);
                    // if (hoverObj) {
                    //     //this will mainly end up in sidebar.js
                    //     Broadcaster.send(Broadcaster.EVENTS.FOCUS_BLOCK, event, hoverObj);
                    //
                    //     //we'll hiding the overlays during hover, so we can't be on any hovered object after focus
                    //     Hover.setHoveredBlock(null);
                    // }
                    // else {
                    //     Logger.error("Got null object while creating a hover object; this shouldn't happen");
                    // }
                }
            }
            // // this means the mousedown happened outside of any block or other kind of hotspot
            // // eg. on the page itself, so we're focusing the page (and it should blur any active focus down the line)
            // else if (draggingStatus == BaseConstantsInternal.DRAGGING.TEXT_SELECTION) {
            //     if (Hover.getFocusedBlock() != null) {
            //         //this will mainly end up in sidebar.js
            //         Broadcaster.send(Broadcaster.EVENTS.FOCUS_BLOCK, event, {
            //             //this is an alternative for launching a blur
            //             block: Hover.getPageBlock(),
            //             //this is the specific 'deep' html element at this mouse position that was clicked (possible because we disabled the events of the overlays during mousedown)
            //             element: element,
            //             //there is no property element for the pageBlock
            //             propertyElement: null
            //         });
            //     }
            // }
            // else {
            //     // do nothing
            // }
        }

        //let's always reset the mouse when done
        _resetMouse();
    };

    /**
     * Cancels and resets an active mouse session
     *
     * @param event
     */
    var _mouseCancel = function (event)
    {
        if (draggingStatus == BaseConstantsInternal.DRAGGING.YES) {
            Broadcaster.send(Broadcaster.EVENTS.ABORT_DRAG, event);
        }

        _resetMouse();
    };

    /**
     * Reset all mouse-tracking variables and event listeners.
     *
     * @private
     */
    var _resetMouse = function ()
    {
        draggingStatus = BaseConstantsInternal.DRAGGING.NO;
        draggingSurface = null;
        draggingStartEvent = null;
        stats = {
            idx: 0,
            events: [],
            sinSum: 0,
            cosSum: 0,
            totalLength: 0,
            totalTime: 0,
            variance: 0,
            direction: 0,
            speed: 0,
        };

        dragVector = {
            x1: 0, y1: 0,
            x2: 0, y2: 0
        };

        //since we're only listening for move events after clicking now, deregister this by default
        $(document).off("mousemove.blocks_core");

        // windowFrame = {
        //     width: document.innerWidth,
        //     height: document.innerHeight
        // };
        // draggingStartEvent = null;
        //
        // startBlock = null;
        // draggingStatus = BaseConstantsInternal.DRAGGING.NO;
        //
        // //re-enable (or reset) the events of the overlays to work
        // var overlays = $('.' + BlocksConstants.BLOCK_OVERLAY_CLASS);
        // overlays.removeClass(BlocksConstants.BLOCK_OVERLAY_NO_EVENTS_CLASS);
        //
        // overlays.removeClass("invisible");
    };

    /*
     * Gives the current direction of the mouse in degrees
     * */
    var updateVector = function (event)
    {
        updateStats(event.pageX, event.pageY);

        //draws the absolute current direction, including speed or not
        if (SHOW_DEBUG_LINES) {
            if (!debugCanvas) {
                debugCanvas = $('<canvas style="position: absolute; top: 0; left: 0;" width="' + UI.body.width() + '" height="' + UI.body.height() + '" />').appendTo(UI.body);
            }

            //whether you want to visualize the speed or not
            var showSpeed = true;
            var multiplier = showSpeed ? stats.speed : DIRECTION_MULTIPLIER;
            var x1 = event.pageX;
            var y1 = event.pageY;
            var x2 = x1 - (Math.cos(stats.direction) * multiplier);
            var y2 = y1 - (Math.sin(stats.direction) * multiplier);

            var ctx = debugCanvas[0].getContext("2d");
            ctx.clearRect(0, 0, debugCanvas[0].width, debugCanvas[0].height);
            ctx.beginPath();
            ctx.moveTo(x1, y1);
            ctx.lineTo(x2, y2);
            ctx.lineWidth = 1;
            ctx.strokeStyle = '#0000ff';
            ctx.stroke();
        }

        dragVector.x1 = event.pageX;
        dragVector.y1 = event.pageY;

        //Note: it makes sense to only update the target direction of the vector if the variance of
        //the angles is below a certain threshold, because only then we are 'really moving' in a certain
        //direction. Otherwise, there are too many angles pointing in other directions
        if (stats.variance < 0.2) {
            //by using a large value (larger than the largest possible page), we're sure the resulting
            //line will extend the page borders and thus intersecting will all possible block edges
            //on that page (see later)
            dragVector.x2 = dragVector.x1 - (Math.cos(stats.direction) * DIRECTION_MULTIPLIER);
            dragVector.y2 = dragVector.y1 - (Math.sin(stats.direction) * DIRECTION_MULTIPLIER);

            if (SHOW_DEBUG_LINES) {
                var ctx = debugCanvas[0].getContext("2d");
                ctx.clearRect(0, 0, debugCanvas[0].width, debugCanvas[0].height);
                ctx.beginPath();
                ctx.moveTo(dragVector.x1, dragVector.y1);
                ctx.lineTo(dragVector.x2, dragVector.y2);
                ctx.lineWidth = 1;
                ctx.strokeStyle = '#00ff00';
                ctx.stroke();
            }
        }
    };

    var updateStats = function (curX, curY)
    {
        var stat = {
            //Note: these are milliseconds
            time: new Date().getTime(),

            x: curX,
            y: curY,

            //time difference between this entry and the previous
            timeDiff: 0,
            //length between this entry and the previous
            length: 0,
            //sin of the angle between this entry and the previous
            sin: 0,
            //cos of the angle between this entry and the previous
            cos: 0,
        };

        //skip the very first event to fill the difference variables
        if (stats.events.length > 0) {
            //calculate the index of the previous entry in the window
            var prevIdx = (stats.idx - 1) % WINDOW_SIZE;
            prevIdx = prevIdx < 0 ? stats.events.length - 1 : prevIdx;

            var prevStat = stats.events[prevIdx];

            stat.timeDiff = stat.time - prevStat.time;

            var xDiff = prevStat.x - stat.x;
            var yDiff = prevStat.y - stat.y;
            stat.length = Math.sqrt(xDiff * xDiff + yDiff * yDiff);

            var angleWithPrev = Math.atan2(yDiff, xDiff);
            stat.sin = Math.sin(angleWithPrev);
            stat.cos = Math.cos(angleWithPrev);

            //calculate the index of the oldest entry in the window
            var oldestIdx = stats.events.length < WINDOW_SIZE ? 0 : ((stats.idx + 1) % WINDOW_SIZE);
            var oldestStat = stats.events[oldestIdx];

            // Instead of re-summing all the the entries, we just calc a moving sum
            // by substracting the last and adding the new value
            stats.sinSum = stats.sinSum - oldestStat.sin + stat.sin;
            stats.cosSum = stats.cosSum - oldestStat.cos + stat.cos;
            stats.totalLength = stats.totalLength - oldestStat.length + stat.length;
            stats.totalTime = stats.totalTime - oldestStat.timeDiff + stat.timeDiff;
        }

        //store the statistic and move to the next
        stats.events[stats.idx] = stat;
        stats.idx = (stats.idx + 1) % WINDOW_SIZE;

        //calculate the mean and the variance of the all the angles in the window;
        // see https://en.wikipedia.org/wiki/Mean_of_circular_quantities
        //     https://en.wikipedia.org/wiki/Directional_statistics
        stats.direction = Math.atan2(stats.sinSum / stats.events.length, stats.cosSum / stats.events.length);
        stats.variance = 1.0 - Math.sqrt(stats.sinSum * stats.sinSum + stats.cosSum * stats.cosSum) / stats.events.length;
        //note: the resulting speed will be expressed as pixels per second
        stats.speed = stats.totalLength / (stats.totalTime / 1000);
    };

    //-----UNCHECKED-----

    /**
     * Calculates the direction of the current mouse vector for the supplied block.
     * Eg. the side of thet that block should be 'highlighted' based on the current mouse movements
     * @param block The block for which you want to calulate it
     * @returns BaseConstantsInternal.DIRECTION
     */
    this.directionForBlock = function (block)
    {
        if (intersects(dragVector.x1, dragVector.y1, dragVector.x2, dragVector.y2, block.left, block.top, block.right, block.top)) {
            return BaseConstantsInternal.DIRECTION.UP;
        }
        else if (intersects(dragVector.x1, dragVector.y1, dragVector.x2, dragVector.y2, block.left, block.bottom, block.right, block.bottom)) {
            return BaseConstantsInternal.DIRECTION.DOWN;
        }
        else if (intersects(dragVector.x1, dragVector.y1, dragVector.x2, dragVector.y2, block.left, block.top, block.left, block.bottom)) {
            return BaseConstantsInternal.DIRECTION.LEFT;
        }
        else if (intersects(dragVector.x1, dragVector.y1, dragVector.x2, dragVector.y2, block.right, block.top, block.right, block.bottom)) {
            return BaseConstantsInternal.DIRECTION.RIGHT;
        }
        else {
            return BaseConstantsInternal.DIRECTION.NONE;
        }
    };

    // https://gist.github.com/Joncom/e8e8d18ebe7fe55c3894
    var intersects = function (p0_x, p0_y, p1_x, p1_y, p2_x, p2_y, p3_x, p3_y)
    {
        var s1_x, s1_y, s2_x, s2_y;
        s1_x = p1_x - p0_x;
        s1_y = p1_y - p0_y;
        s2_x = p3_x - p2_x;
        s2_y = p3_y - p2_y;

        var s, t;
        s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / (-s2_x * s1_y + s1_x * s2_y);
        t = (s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            // Collision detected
            return 1;
        }

        return 0; // No collision
    };

}]);

