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

base.plugin("blocks.core.Mouse", ["blocks.core.Broadcaster", "blocks.core.Layouter", "base.core.Constants", "constants.blocks.core", "blocks.core.Sidebar", "blocks.core.Hover", function (Broadcaster, Layouter, BaseConstants, BlocksConstants, SideBar, Hover)
{
    // watch out with this value: it should be smaller than the smallest possible object in the layout system (width or height)
    // but when clicking near the edge of such an object, even smaller; so maybe TODO: activate the DnD when entering a new block?
    var DRAGGING_THRESHOLD = 3;

    // flag if this module is active
    var Mouse = this;
    var active = false;
    // dragging options, kept here for parsedContent while waiting for drag
    var draggingStatus = BaseConstants.DRAGGING.NO;
    var draggingStartEvent = null;
    var dblClickFound = false;
    var startBlock = null;
    var windowFrame = {width: 0, height: 0};

    //for direction calculations
    var directionVector = {x1: 0, y1: 0, x2: 0, y2: 0};
    var sins = [];
    var coss = [];
    var prevX = -1;
    var prevY = -1;
    var distance = 0;
    var direction = 0;
    var lengths = [];
    var times = [];
    var index = 0;
    var limit = 20;
    var variance = 0;
    var prevTime = new Date().getTime();

    this.resetMouse = function ()
    {
        windowFrame = {width: document.innerWidth, height: document.innerHeight};
        dblClickFound = false;
        draggingStartEvent = null;

        startBlock = null;
        draggingStatus = BaseConstants.DRAGGING.NO;

        //re-enable (or reset) the events of the overlays to work
        var overlays = $('.'+BlocksConstants.BLOCK_OVERLAY_CLASS);
        overlays.removeClass(BlocksConstants.BLOCK_OVERLAY_NO_EVENTS_CLASS);

        overlays.removeClass("invisible");

        //since we're only listening for move events after clicking now, deregister this by default
        $(document).off("mousemove.blocks_core");
    };

    this.disallowDrag = function ()
    {
        draggingStatus = BaseConstants.DRAGGING.NOT_ALLOWED;
    };

    this.allowDrag = function ()
    {
        Mouse.resetMouse();
    };

    this.activate = function ()
    {
        Mouse.deactivate();
        active = true;
        $(document).on("mousedown.blocks_core", function (event)
        {
            if (event.which == 1) {
                mouseDown(event);
            }
        });
        $(document).on("mouseup.blocks_core", function (event)
        {
            if (event.which == 1) {
                mouseUp(event);
            }
        });

        $(document).on("mouseleave.blocks_core", function (event)
        {
            if (draggingStatus == BaseConstants.DRAGGING.YES) {
                Broadcaster.send(Broadcaster.EVENTS.ABORT_DRAG, event);
            }
            Mouse.resetMouse();

            Logger.debug("Mouse out of window. Cancel!");
        });

        Mouse.resetMouse();
    };

    this.deactivate = function ()
    {
        active = false;
        $(document).off("mousedown.blocks_core");
        $(document).off("mouseup.blocks_core");
        $(document).off("mousemove.blocks_core");
        $(document).off("mouseleave.blocks_core");
    };

    /**
     * Calculates the direction of the current mouse vector for the supplied block.
     * Eg. the side of thet that block should be 'highlighted' based on the current mouse movements
     * @param block The block for which you want to calulate it
     * @returns BaseConstants.DIRECTION
     */
    this.directionForBlock = function (block)
    {
        if (intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.left, block.top, block.right, block.top)) {
            return BaseConstants.DIRECTION.UP;
        } else if (intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.left, block.bottom, block.right, block.bottom)) {
            return BaseConstants.DIRECTION.DOWN;
        } else if (intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.left, block.top, block.left, block.bottom)) {
            return BaseConstants.DIRECTION.LEFT;
        } else if (intersects(directionVector.x1, directionVector.y1, directionVector.x2, directionVector.y2, block.right, block.top, block.right, block.bottom)) {
            return BaseConstants.DIRECTION.RIGHT;
        } else {
            return BaseConstants.DIRECTION.NONE;
        }
    };

    /*
     * on mousedown and dragging allowed and surface to drag selected:
     *   - wait for drag
     *   - disable selection
     *   - keep current jQuery event as startevent
     * */
    var mouseDown = function (event)
    {
        if (active) {
            // check for left mouse click
            if (event.which == 1) {
                var block = Hover.getHoveredBlock();

                //we need this to enable sidebar.js to know on which element we really clicked (instead of click-events on the overlay)
                $('.'+BlocksConstants.BLOCK_OVERLAY_CLASS).addClass(BlocksConstants.BLOCK_OVERLAY_NO_EVENTS_CLASS);

                //we're attempting to dnd an existing block
                if (draggingStatus == BaseConstants.DRAGGING.NO) {
                    if (block != null && block.canDrag) {
                        // save the block we started on for future reference
                        // (because we're removing the events from the overlay classes for now)
                        startBlock = block;
                    }
                    else {
                        // Signalfor dragging new block
                        startBlock = null;
                    }

                    //if we don't have a startblock, we must be dragging from the new-block button
                    if (startBlock!=null || event.target != null && ($(event.target).hasClass(BlocksConstants.CREATE_BLOCK_CLASS) || $(event.target).parents("." + BlocksConstants.CREATE_BLOCK_CLASS).length > 0)) {
                        draggingStatus = BaseConstants.DRAGGING.WAITING;
                        draggingStartEvent = event;

                        //put the mousemove on the document instead of the overlay so we get the events even though BLOCK_OVERLAY_NO_EVENTS_CLASS
                        $(document).on("mousemove.blocks_core", function (event)
                        {
                            mouseMove(event);
                        });
                    }
                }
                //this will happen when we eg. click on the page (or nothing at all, like outside the page)
                else {
                    //Logger.debug("We can not start because dragging is already in place or not allowed. " + draggingStatus);
                    Mouse.resetMouse();

                    // this overload the resetMouse above and actually makes sense:
                    // it means we clicked down outside of any block hotspot (eg. just on the page) and if we start dragging now,
                    // (if that really does that, that's another question)
                    draggingStatus = BaseConstants.DRAGGING.TEXT_SELECTION;
                }
            }
            else {
                // middle or right mouse button presses
                //TODO ??
                if (draggingStatus == BaseConstants.DRAGGING.YES) {
                    Broadcaster.send(Broadcaster.EVENTS.ABORT_DRAG, event);
                }
                Mouse.resetMouse();
            }
        }
    };

    /*
     * If dragging send END_OF_DRAG and reset draggingOptions
     * */
    var mouseUp = function (event)
    {
        if (active && event.which == 1) {
            if (draggingStatus != BaseConstants.DRAGGING.NOT_ALLOWED) {

                //the low level html element we clicked on
                var element = $(event.target);

                if (draggingStatus == BaseConstants.DRAGGING.YES) {
                    Broadcaster.send(Broadcaster.EVENTS.END_DRAG, event);
                }
                // this means we were dragging, but haven't exceeded the threshold yet
                // so, instead of starting to drag a block, we clicked one
                else if (draggingStatus == BaseConstants.DRAGGING.WAITING) {

                    var hoverObj = Hover.createHoverClickObject(startBlock, element, event);
                    if (hoverObj) {
                        //this will mainly end up in sidebar.js
                        Broadcaster.send(Broadcaster.EVENTS.FOCUS_BLOCK, event, hoverObj);
                    }
                    else {
                        Logger.error("Got null object while creating a hover object; this shouldn't happen");
                    }
                }
                // this means the mousedown happened outside of any block or other kind of hotspot
                // eg. on the page itself, so we're focusing the page (and it should blur any active focus down the line)
                else if (draggingStatus == BaseConstants.DRAGGING.TEXT_SELECTION) {
                    if (Hover.getFocusedBlock()!=null) {
                        //this will mainly end up in sidebar.js
                        Broadcaster.send(Broadcaster.EVENTS.FOCUS_BLOCK, event, {
                            //this is an alternative for launching a blur
                            block: Hover.getPageBlock(),
                            //this is the specific 'deep' html element at this mouse position that was clicked (possible because we disabled the events of the overlays during mousedown)
                            element: element,
                            //there is no property element for the pageBlock
                            propertyElement: null
                        });
                    }
                }
                else {
                    // do nothing
                }
            }
        }

        Mouse.resetMouse();
    };

    /*
     * While waiting for drag, check if threshold is activated to really start drag
     * */
    var enableDragAfterTreshold = function (event)
    {
        //Logger.debug("Calculate wait for drag");
        if (Math.abs(draggingStartEvent.pageX - event.pageX) > DRAGGING_THRESHOLD ||
            Math.abs(draggingStartEvent.pageY - event.pageY) > DRAGGING_THRESHOLD) {
            draggingStatus = BaseConstants.DRAGGING.YES;
            //Logger.debug("Start drag");

            var overlays = $('.'+BlocksConstants.BLOCK_OVERLAY_CLASS);
            //we need this to enable sidebar.js to know on which element we really clicked (instead of click-events on the overlay)
            overlays.removeClass(BlocksConstants.BLOCK_OVERLAY_NO_EVENTS_CLASS);

            //don't show the hover effects while dragging; it blocks the visibility of the lines in between
            overlays.addClass(BlocksConstants.BLOCK_OVERLAY_BLOCK_HOVER);

            //pass this along with the custom event data object
            Broadcaster.send(Broadcaster.EVENTS.START_DRAG, event, {
                //we'll pass the block we initially had our cursor over (even before the wait threshold)
                block: startBlock
            });
        }
    };

    var mouseMove = function (event)
    {
        if (active) {
            //first, update the direction and speed vectors
            calculateDirection(event);

            //we're waiting for the threshold to be exceeded
            if (draggingStatus == BaseConstants.DRAGGING.WAITING) {
                enableDragAfterTreshold(event);
            }
            //we're dragging a block around
            else if (draggingStatus == BaseConstants.DRAGGING.YES) {
                var block = Hover.getHoveredBlock();

                Broadcaster.send(Broadcaster.EVENTS.DRAG_OVER_BLOCK, event, {
                    block: block
                });
            }
        }
    };

    //-----METHODS THAT CALCULATE THE SPEED AND DIRECTION VECTORS-----

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
        t = ( s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / (-s2_x * s1_y + s1_x * s2_y);

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            // Collision detected
            return 1;
        }

        return 0; // No collision
    };

    /*
     * Gives the current direction of the mouse in degrees
     * */
    var calculateDirection = function (event)
    {
        updateDistanceAndDirection(event.pageX, event.pageY);

        directionVector.x1 = event.pageX;
        directionVector.y1 = event.pageY;
        if (variance < 0.2) {
            var cos = (Math.cos(direction) * 10000);
            var sin = (Math.sin(direction) * 10000);
            directionVector.x2 = directionVector.x1 - cos;
            directionVector.y2 = directionVector.y1 - sin;
        }
        //Logger.debug(directionVector.x1 + ", "+directionVector.y1+ " - " +directionVector.x2 +", "+ directionVector.y2);
        var angle = direction * (180 / Math.PI);
        //Logger.debug("Angle: " + (angle).toFixed(0) + "° - variance: " + (variance).toFixed(0));

        return direction;
    };

    var updateDistanceAndDirection = function (curX, curY)
    {
        var angle = Math.atan2(prevY - curY, prevX - curX);
        sins[index] = Math.sin(angle);
        coss[index] = Math.cos(angle);
        lengths[index] = Math.sqrt((curX - prevX) * (curX - prevX) + (curY - prevY) * (curY - prevY));
        var time = new Date().getTime();
        times[index] = time - prevTime;

        variance = 1.0 - Math.sqrt(sum(coss) * sum(coss) + sum(sins) * sum(sins)) / sins.length;

        direction = Math.atan2(1 / sins.length * sum(sins), 1 / coss.length * sum(coss));
        var speed = sum(lengths) / (sum(times) / 200);
        distance = Math.min(Math.max(40, speed), 100);
        prevTime = time;
        index = (index + 1) % limit;
        prevX = curX;
        prevY = curY;
    };

    var sum = function (array)
    {
        var s = 0.0;
        for (var i = 0; i < array.length; i++) {
            s += array[i];
        }
        return s;
    };
}]);

