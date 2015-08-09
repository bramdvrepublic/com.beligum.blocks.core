/**
 * The Mouse plugin converts jQuery events to blocks Events
 *
 * A block Event has the following parameters
 *      event: jQuery event
 *      block.current: the current BLOCK (See Elements) where the event happenend
 *      block.previous: the last previous BLOCK where an event happened
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

base.plugin("blocks.core.Mouse", ["blocks.core.Broadcaster", "blocks.core.Layouter", "base.core.Constants", "constants.blocks.core", "blocks.core.Sidebar", function (Broadcaster, Layouter, BaseConstants, BlocksConstants, SideBar)
{
    // watch out with this value: it should be smaller than the smallest possible object in the layout system (width or height)
    // but when clicking near the edge of such an object, even smaller; so maybe TODO: activate the DnD when entering a new block?
    var DRAGGING_THRESHOLD = 3;

    // flag if this module is active
    var Mouse = this;
    var active = false;
    // dragging options, kept here for parsedContent while waiting for drag
    var draggingStatus = BaseConstants.DRAGGING.NO;
    var draggingStart = null;
    var dblClickFound = false;
    var currentBlock = null;
    var currentProperty = null;
    var windowFrame = {width: 0, height: 0};
    var lastMoveEvent = $.Event("mousemove", {pageX: 0, pageY: 0});

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
        draggingStart = null;

        currentBlock = null;
        currentProperty = null;
        draggingStatus = BaseConstants.DRAGGING.NO;

        mouseMove(lastMoveEvent);

        //since we're only listening for move events after clicking now, deregister this by default
        $(document).off("mousemove.blocks_core");
    };

    this.disallowDrag = function ()
    {
        draggingStatus = BaseConstants.DRAGGING.NOT_ALLOWED;
    };

    this.allowDrag = function ()
    {
        Mouse.resetMouse(true);
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
        //$(document).on("mousemove.blocks_core", function (event)
        //{
        //    mouseMove(event);
        //});

        $(document).on("mouseleave.blocks_core", function (event)
        {
//                mouseUp(event);
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

    this.getLastMoveEvent = function()
    {
        return lastMoveEvent;
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
                //var block = Broadcaster.getHoveredBlockForPosition(event.pageX, event.pageY);
                var block = Broadcaster.getHoveredBlock();
                if (draggingStatus == BaseConstants.DRAGGING.NO && block.current != null && block.current.canDrag) {
                    draggingStatus = BaseConstants.DRAGGING.WAITING;
                    draggingStart = event;

                    $(document).on("mousemove.blocks_core", function (event)
                    {
                        mouseMove(event);
                    });
                }
                else if (draggingStatus == BaseConstants.DRAGGING.NO && event.target != null && ($(event.target).hasClass(BlocksConstants.CREATE_BLOCK_CLASS) || $(event.target).parents("." + BlocksConstants.CREATE_BLOCK_CLASS).length > 0)) {
                    draggingStatus = BaseConstants.DRAGGING.WAITING;
                    draggingStart = event;
                    Logger.debug("Start new drag");
                }
                else if ($(event.target).hasClass(BlocksConstants.BLOCKS_START_BUTTON) || $(event.target).parents("." + BlocksConstants.BLOCKS_START_BUTTON).length > 0) {
                    //FIXME right that nothing is in here?
                }
                else {
                    Logger.debug("We can not start because dragging is already in place or not allowed. " + draggingStatus);
                    Mouse.resetMouse();
                    draggingStatus = BaseConstants.DRAGGING.TEXT_SELECTION;

                    //TODO doens't exist anymore, safely omit?
                    //Broadcaster.send(Broadcaster.EVENTS.END_HOVER);
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
            Logger.debug("MOUSE UP");
            if (draggingStatus != BaseConstants.DRAGGING.NOT_ALLOWED) {
                var oldDragStatus = draggingStatus;
                if (oldDragStatus == BaseConstants.DRAGGING.YES) {
                    Broadcaster.send(Broadcaster.EVENTS.END_DRAG, event);
                    //} else if (Broadcaster.property().current != null && Broadcaster.property().current.editType != BlocksConstants.EDIT_NONE) {
                    //    Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD);

                } else if (oldDragStatus == BaseConstants.DRAGGING.WAITING) {
                    if ($(event.target).hasClass(BlocksConstants.CREATE_BLOCK_CLASS) || $(event.target).parents("." + BlocksConstants.CREATE_BLOCK_CLASS).length > 0) {
                        //implemented with a popover in page.js instead
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
        Logger.debug("Calculate wait for drag");
        if (Math.abs(draggingStart.pageX - event.pageX) > DRAGGING_THRESHOLD ||
            Math.abs(draggingStart.pageY - event.pageY) > DRAGGING_THRESHOLD) {
            draggingStatus = BaseConstants.DRAGGING.YES;
            Logger.debug("Start drag");

            //pass this along with the custom event data object
            Broadcaster.send(Broadcaster.EVENTS.START_DRAG, event, {
                draggingStart: draggingStart
            });
        }
    };

    var mouseMove = function (event)
    {
        if (active) {
            calculateDirection(event);

            var changedBlock = false;
            var block = Broadcaster.block();

            // check if block changed since last mouse move
            if (block.current !== currentBlock) {
                changedBlock = true;
                currentBlock = block.current;
            }

            var changedProperty = false;
            var property = Broadcaster.property();
            // check if property changed since last mouse move
            if (property.current !== currentProperty) {
                changedProperty = true;
                currentProperty = property.current;
            }

            if (draggingStatus == BaseConstants.DRAGGING.WAITING) {
                enableDragAfterTreshold(event);
            }
            //we're not dragging, just moving the mouse
            else if (draggingStatus != BaseConstants.DRAGGING.YES) {
                //if (changedProperty) {
                //    if (property.current == null) {
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_LEAVE_PROPERTY);
                //    } else if (property.previous == null) {
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_ENTER_PROPERTY);
                //    } else {
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_LEAVE_PROPERTY);
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_ENTER_PROPERTY);
                //    }
                //} else if (property.current != null) {
                //    Broadcaster.send(Broadcaster.EVENTS.HOVER_OVER_PROPERTY);
                //}
                //
                //if (changedBlock) {
                //    if (block.current == null) {
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_LEAVE_BLOCK);
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_LEAVE_PROPERTY);
                //    } else if (block.previous == null) {
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_ENTER_BLOCK);
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_ENTER_PROPERTY);
                //    } else {
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_LEAVE_BLOCK);
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_LEAVE_PROPERTY);
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_ENTER_BLOCK);
                //        Broadcaster.send(Broadcaster.EVENTS.HOVER_ENTER_PROPERTY);
                //
                //        //Logger.debug("changed templates");
                //    }
                //} else if (block.current != null) {
                //    Broadcaster.send(Broadcaster.EVENTS.HOVER_OVER_BLOCK);
                //}
            }
            //we're dragging a block around
            else if (draggingStatus == BaseConstants.DRAGGING.YES) {
                //if (changedBlock) {
                //    if (block.current == null) {
                //        Broadcaster.send(Broadcaster.EVENTS.DRAG_LEAVE_BLOCK);
                //    } else if (block.previous == null) {
                //        Broadcaster.send(Broadcaster.EVENTS.DRAG_ENTER_BLOCK);
                //    } else {
                //        Broadcaster.send(Broadcaster.EVENTS.DRAG_ENTER_BLOCK);
                //        Broadcaster.send(Broadcaster.EVENTS.DRAG_LEAVE_BLOCK);
                //    }
                //}
                //else {// if (block.current != null)
                    Broadcaster.send(Broadcaster.EVENTS.DRAG_OVER_BLOCK, event);
                //}
            }

            lastMoveEvent = event;

            //Legacy code, needed?
            //lastMoveEvent.block = Broadcaster.getHoveredBlockForPosition(lastMoveEvent.pageX, lastMoveEvent.pageY);
            //lastMoveEvent.direction = direction;
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
        //var angle = direction * (180 / Math.PI);
        //Logger.debug("Hoek: " + angle + " - variance: " + variance);

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

