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
 *  HOOVER_ENTER_BLOCK: Mouse hoovers over a block (not dragging)
 *  HOOVER_LEAVE_BLOCK: Mouse leaves a block (not dragging)
 *  HOOVER_OVER_BLOCK: Mouse hoovers ovr a block (not dragging)
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
 *  DO_NOT_ALLOW_DRAG: do not allow dragging, even if a surface is selected
 *
 *
 */

base.plugin("blocks.core.Mouse", ["blocks.core.Broadcaster", "blocks.core.Layouter", "base.core.Constants", "constants.blocks.common", "blocks.core.Sidebar", function (Broadcaster, Layouter, BaseConstants, BlocksConstants, SideBar)
{
    // flag if this module is active
    var Mouse = this;
    var active = false;
    // dragging options, kept here for parsedContent while waiting for drag
    var draggingStatus = BaseConstants.DRAGGING.NO;
    var draggingStart = null;
    var dblClickFound = false;
    var currentBlock = null;
    var currentProperty = null;
    // array of coordinates {x, y} from the last mouseEvents, used to calculate the direction
    var config = this.config;
    var windowFrame = {width: 0, height: 0};

    this.resetMouse = function ()
    {
        windowFrame = {width: document.innerWidth, height: document.innerHeight};
        dblClickFound = false;
        draggingStart = null;

        currentBlock = null;
        currentProperty = null;
        //var docWidth = $(document).width();
        var docWidth = 1000;
        if (docWidth > 920) {
            Broadcaster.send(Broadcaster.EVENTS.ENABLE_BLOCK_DRAG);
            draggingStatus = BaseConstants.DRAGGING.NO;
        } else {
            draggingStatus = BaseConstants.DRAGGING.NOT_ALLOWED;
        }

        mouseMove(Broadcaster.getLastMove());
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
                var block = Broadcaster.getHooveredBlockForPosition(event.pageX, event.pageY);
                if (draggingStatus == BaseConstants.DRAGGING.NO && block.current != null && block.current.canDrag) {
                    draggingStatus = BaseConstants.DRAGGING.WAITING;
                    draggingStart = event;
                } else if (draggingStatus == BaseConstants.DRAGGING.NO && event.target != null && ($(event.target).hasClass(BlocksConstants.CREATE_BLOCK_CLASS) || $(event.target).parents(BlocksConstants.CREATE_BLOCK_CLASS).length > 0)) {
                    draggingStatus = BaseConstants.DRAGGING.WAITING;
                    draggingStart = event;
                    Logger.debug("Start new drag");
                } else if ($(event.target).hasClass(BlocksConstants.BLOCKS_START_BUTTON) || $(event.target).parents("." + BlocksConstants.BLOCKS_START_BUTTON).length > 0) {

                } else {
                    Logger.debug("We can not start because dragging is already in place or not allowed. " + draggingStatus);
                    Mouse.resetMouse();
                    draggingStatus = BaseConstants.DRAGGING.TEXT_SELECTION;
                    Broadcaster.send(Broadcaster.EVENTS.END_HOOVER);

                }
            } else {
                // middle or right mouse button presses
                //TODO ??
                if (draggingStatus == BaseConstants.DRAGGING.YES) {
                    Broadcaster.send(Broadcaster.EVENTS.ABORT_DRAG);
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
                    Broadcaster.send(Broadcaster.EVENTS.END_DRAG);
                //} else if (Broadcaster.property().current != null && Broadcaster.property().current.editType != BlocksConstants.EDIT_NONE) {
                //    Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD);

                } else {
                    SideBar.update();
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
        if (Math.abs(draggingStart.pageX - event.pageX) > config.DRAGGING_THRESHOLD ||
            Math.abs(draggingStart.pageY - event.pageY) > config.DRAGGING_THRESHOLD) {
            draggingStatus = BaseConstants.DRAGGING.YES;
            Logger.debug("Start drag");
            Broadcaster.send(Broadcaster.EVENTS.START_DRAG, {draggingStart: draggingStart});

        }
    };

    /*
     *
     * */
    var mouseMove = function (event)
    {
        if (active) {
            var changedBlock = false;
            var block = Broadcaster.block();
            // check if block changed since last mouse move
            if (block.current !== currentBlock) {
                Logger.debug("New block");
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
            } else if (draggingStatus != BaseConstants.DRAGGING.YES) {
                if (changedProperty) {

                    if (property.current == null) {
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_LEAVE_PROPERTY);
                    } else if (property.previous == null) {
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_ENTER_PROPERTY);
                    } else {
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_LEAVE_PROPERTY);
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_ENTER_PROPERTY);
                    }
                } else if (property.current != null) {
                    Broadcaster.send(Broadcaster.EVENTS.HOOVER_OVER_PROPERTY);
                }

                if (changedBlock) {

                    if (block.current == null) {
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK);
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_LEAVE_PROPERTY);
                    } else if (block.previous == null) {
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_ENTER_BLOCK);
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_ENTER_PROPERTY);
                    } else {
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK);
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_LEAVE_PROPERTY);
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_ENTER_BLOCK);
                        Broadcaster.send(Broadcaster.EVENTS.HOOVER_ENTER_PROPERTY);

                        //Logger.debug("changed templates");
                    }
                } else if (block.current != null) {
                    Broadcaster.send(Broadcaster.EVENTS.HOOVER_OVER_BLOCK);
                }

            } else if (draggingStatus == BaseConstants.DRAGGING.YES) {
                if (changedBlock) {
                    if (block.current == null) {
                        Broadcaster.send(Broadcaster.EVENTS.DRAG_LEAVE_BLOCK);
                    } else if (block.previous == null) {
                        Broadcaster.send(Broadcaster.EVENTS.DRAG_ENTER_BLOCK);
                    } else {
                        Broadcaster.send(Broadcaster.EVENTS.DRAG_ENTER_BLOCK);
                        Broadcaster.send(Broadcaster.EVENTS.DRAG_LEAVE_BLOCK);
                    }
                } else // if (block.current != null)
                {
                    Broadcaster.send(Broadcaster.EVENTS.DRAG_OVER_BLOCK);
                }

            }
        }
    };

    this.disallowDrag = function ()
    {
        Logger.debug("Dragging not allowed");
        draggingStatus = BaseConstants.DRAGGING.NOT_ALLOWED;
    };

    this.allowDrag = function ()
    {
        Logger.debug("Dragging allowed");
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
        $(document).on("mousemove.blocks_core", function (event)
        {
            mouseMove(event);
        });


        $(document).on("mouseleave.blocks_core", function ()
        {
//                mouseUp(event);
            if (draggingStatus == BaseConstants.DRAGGING.YES) {
                Broadcaster.send(Broadcaster.EVENTS.ABORT_DRAG);
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
}])
    .config("blocks.core.Mouse", {
        DRAGGING_THRESHOLD: 10,
        CLICK_TIMEOUT: 500,
        ACTIVATE_AT_BOOT: true
    });

