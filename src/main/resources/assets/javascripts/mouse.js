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



blocks.plugin("blocks.core.Mouse", ["blocks.core.Broadcaster", "blocks.core.Elements", "blocks.core.Layouter", "blocks.core.Constants", function (Broadcaster, Elements, Layouter, Constants) {
    // flag if this module is active
    var active = false;
    // dragging options, kept here for parsedContent while waiting for drag
    var draggingStatus = Constants.DRAGGING.DISABLED;
    var draggingOptions = {startEvent: null, owner: null, priority: null};
    // the active block for the last 2 mouseEvents
    var block = {current: null, previous: null};
    // array of coordinates {x, y} from the last mouseEvents, used to calculate the direction
    var _lastPoints = [];
    var config = this.config;

    var windowFrame = {width: 0, height: 0};

    var resetMouse = function (force) {
        windowFrame = {width: window.innerWidth, height: window.innerHeight}
        block = {current: null, previous: null};
        if (draggingStatus != Constants.DRAGGING.NOT_ALLOWED) {
            draggingStatus = Constants.DRAGGING.DISABLED;
            Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.DRAG_DISABLED(draggingOptions.priority, draggingOptions.owner));
        }
        draggingOptions.startEvent = null;
        draggingOptions.owner = null;
        draggingOptions.priority = null;
    };


    // returns the current mouse direction
    var calculateDirection = function(event) {
        var REMEMBER_NR_OF_POINTS = 10;
        var newPoint = {x: event.pageX, y: event.pageY}
        var lastPoint;
        _lastPoints.push(newPoint);
        if (_lastPoints.length < REMEMBER_NR_OF_POINTS) {
            lastPoint = _lastPoints[0];
        } else {
            lastPoint = _lastPoints.shift();
        }
        var deltaX = newPoint.x - lastPoint.x;
        var deltaY = newPoint.y - lastPoint.y;
        var retVal;
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            if (deltaX < 0) {
                retVal =  Constants.DIRECTION.LEFT;
            } else {
                retVal = Constants.DIRECTION.RIGHT;
            }
        } else if (Math.abs(deltaX) < Math.abs(deltaY)) {
            if (deltaY < 0) {
                retVal = Constants.DIRECTION.UP;
            } else {
                retVal = Constants.DIRECTION.DOWN;
            }
        } else {
            retVal = Constants.DIRECTION.NONE;
        }
        return retVal;
    };

    // sets the current active block
    var getHooveredBlockForEvent = function (event) {
        block.previous = block.current;
        block.current = null;
        if (draggingStatus != Constants.DRAGGING.TEXT_SELECTION && active) {
            // First search for active element
            // If an element is active, we have a big chance the next event is in the same element, so we start our search here
            if (block.previous != null) {
                block.current = block.previous.findActiveElement(event.pageX, event.pageY);
            }
            // Our shortcut failed so search the full page
            // we loop the trees of elements to find the smallest active element
            if (block.current == null) {
                var i = 0;
                while (i < Layouter.getLayoutTree().length && block.current == null) {
                    block.current = Layouter.getLayoutTree()[i].findActiveElement(event.pageX, event.pageY);
                    i++;
                }
            }
        }

    };

    // create a block event from a jQuery mouseEvent
    var createBlockEvent = function (event) {
        getHooveredBlockForEvent(event);
        var blocksEvent = {
            event: event,
            block: block,
            direction: calculateDirection(event),
            draggingStatus: draggingStatus
        };

        if (draggingStatus == Constants.DRAGGING.YES) {
            blocksEvent.draggingOptions = draggingOptions;
            blocksEvent.draggingStatus = draggingStatus;
        }
        return blocksEvent;
    };

    /*
     * on mousedown and dragging allowed and surface to drag selected:
     *   - wait for drag
     *   - disable selection
     *   - keep current jQuery event as startevent
     * */

    var mouseDown = function (event) {
        if (active) {
//            if (draggingStatus == Constants.DRAGGING.YES) {
//                Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.ABORT_DRAG(createBlockEvent(event)));
//                resetMouse();
//                return;
//            }
            // check for left mouse click
            if (event.which == 1) {
                var currentEvent = createBlockEvent(event);
                if (draggingStatus == Constants.DRAGGING.ENABLED &&
                    currentEvent.block.current != null &&
                    currentEvent.block.current.isLayoutable) {
                    draggingStatus = Constants.DRAGGING.WAITING;
                    draggingOptions.startEvent = event;
                    disableSelection();
                } else {
                    resetMouse();
                    draggingStatus = Constants.DRAGGING.TEXT_SELECTION;
                    Broadcaster.send(new Broadcaster.EVENTS.END_HOOVER(createBlockEvent(event)));
                    Logger.debug("We can not start draggingOptions because dragging is already in place or not allowed. " + draggingStatus);
                }
            } else {
                // middle or right mouse button presses
                //TODO
                if (draggingStatus == Constants.DRAGGING.YES) {
                    Broadcaster.send(new Broadcaster.EVENTS.ABORT_DRAG(createBlockEvent(event)));
                }
                resetMouse();
            }
        }
    };

    /*
     * If dragging send END_OF_DRAG and reset draggingOptions
     * */
    var mouseUp = function (event) {
        enableSelection();
        if (active) {
            if (draggingStatus != Constants.DRAGGING.NOT_ALLOWED) {
                var oldDragStatus = draggingStatus;
                if (oldDragStatus == Constants.DRAGGING.YES) {
                    Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.END_DRAG(createBlockEvent(event)));

                }
            }
            resetMouse();
        }
};

    /*
     * While waiting for drag, check if threshold is activated to really start drag
     * */
    var enableDragAfterTreshold = function (event) {
        if (Math.abs(draggingOptions.startEvent.pageX - event.pageX) > config.DRAGGING_THRESHOLD ||
            Math.abs(draggingOptions.startEvent.pageY - event.pageY) > config.DRAGGING_THRESHOLD) {
            draggingStatus = Constants.DRAGGING.YES;
            Broadcaster.send(new Broadcaster.EVENTS.START_DRAG(createBlockEvent(event)));
        }
    };

    /*
     *
     * */
    var mouseMove = function (event) {
        if (active) {
            createBlockEvent(event);
            // Abort outside window
            if (event.pageX < 0 || event.pageY > windowFrame.width || event.pageY < 0 || event.pageY > windowFrame.height) {
                mouseUp(event);
            } else if (draggingStatus == Constants.DRAGGING.WAITING) {
                enableDragAfterTreshold(event);
            } else if (draggingStatus != Constants.DRAGGING.YES) {
                if (block.current != block.previous) {
                    if (block.current == null) {
                        Broadcaster.send(new Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK(createBlockEvent(event)));
                    } else if (block.previous == null) {
                        Broadcaster.send(new Broadcaster.EVENTS.HOOVER_ENTER_BLOCK(createBlockEvent(event)));
                    } else {
                        Broadcaster.send(new Broadcaster.EVENTS.HOOVER_ENTER_BLOCK(createBlockEvent(event)));
                        Broadcaster.send(new Broadcaster.EVENTS.HOOVER_LEAVE_BLOCK(createBlockEvent(event)));
                    }
                } else if (block.current != null) {
                    Broadcaster.send(new Broadcaster.EVENTS.HOOVER_OVER_BLOCK(createBlockEvent(event)));
                }

            } else if (draggingStatus == Constants.DRAGGING.YES) {
                if (block.current != block.previous) {
                    if (block.current == null) {
                        Broadcaster.send(new Broadcaster.EVENTS.DRAG_LEAVE_BLOCK(createBlockEvent(event)));
                    } else if (block.previous == null) {
                        Broadcaster.send(new Broadcaster.EVENTS.DRAG_ENTER_BLOCK(createBlockEvent(event)));
                    } else {
                        Broadcaster.send(new Broadcaster.EVENTS.DRAG_ENTER_BLOCK(createBlockEvent(event)));
                        Broadcaster.send(new Broadcaster.EVENTS.DRAG_LEAVE_BLOCK(createBlockEvent(event)));
                    }
                } else if (block.current != null) {
                    Broadcaster.send(new Broadcaster.EVENTS.DRAG_OVER_BLOCK(createBlockEvent(event)));
                }

            }
        }
    };


    var enableDrag = function (event) {
        Logger.debug("Request to enable drag for " + event.owner);

        if ((draggingOptions.priority <= event.priority
            || draggingStatus == Constants.DRAGGING.DISABLED)
            && draggingStatus != Constants.DRAGGING.NOT_ALLOWED) {
                Broadcaster.sendNoTimeout(new Broadcaster.EVENTS.DRAG_DISABLED(draggingOptions.priority, draggingOptions.owner));
                event.callback();
                draggingOptions.owner = event.owner;
                draggingOptions.priority = event.priority;
                draggingOptions.callback = event.callback;
                draggingStatus = Constants.DRAGGING.ENABLED;
                Logger.debug("Dragging enabled for " + event.owner);
        } else {
            // Another element with a higher priority is already selected
            Logger.debug("Dragging is already reserved for namespace " + draggingOptions.owner);
        }

    };

    var disableDrag = function (event) {
        // Do nothing if dragging is not allowed or not allowed status could be overwritten
        if (draggingStatus == Constants.DRAGGING.NOT_ALLOWED) return;
        Logger.debug("Request to disable drag for " + event.owner);

        // TODO: check if someone with higher priority can disable drag from lower priority namespace
        if (draggingStatus == Constants.DRAGGING.ENABLED && event.owner == draggingOptions.owner) {
            resetMouse();
            Logger.debug("Dragging for " + event.owner + " disabled")
        } else {
            Logger.debug("Dragging is reserved for other owner " + draggingOptions.owner);
        }
    };

    var disallowDrag = function() {
        Logger.debug("Dragging not allowed");
        draggingOptions.surface = null;
        draggingStatus = Constants.DRAGGING.NOT_ALLOWED;
    };

    var allowDrag = function() {
        Logger.debug("Dragging allowed");
        draggingStatus = Constants.DRAGGING.DISABLED;
        resetMouse(true);
    };

    // disable cross-browser text selection
    var disableSelection = function () {
        // http://stackoverflow.com/questions/826782/css-rule-to-disable-text-selection-highlighting#4407335
        $("html").css("-webkit-touch-callout", "none");
        $("html").css("-khtml-user-select", "none");
        $("html").css("-moz-user-select", "none");
        $("html").css("-ms-user-select", "none");
        $("html").css("user-select", "none");
        $("html").attr("oncontextmenu", "return false;");
        // IE < 10
        $("html").attr("onselectstart", "return false;");

    };

    // enable cross-browser text selection
    var enableSelection = function () {
        //http://stackoverflow.com/questions/826782/css-rule-to-disable-text-selection-highlighting#4407335
        $("html").css("-webkit-touch-callout", "text");
        $("html").css("-khtml-user-select", "text");
        $("html").css("-moz-user-select", "text");
        $("html").css("-ms-user-select", "text");
        $("html").css("user-select", "text");
        $("html").attr("oncontextmenu", "");
        $("html").attr("onselectstart", "return true;");
    }

    var registerMouseEvents = function () {
        if (!active) {
            active = true;
            $(document).on("mousedown.blocks_core", function (event) {
                mouseDown(event)
            });
            $(document).on("mouseup.blocks_core", function (event) {
                mouseUp(event)
            });
            $(document).on("mousemove.blocks_core", function (event) {
                    mouseMove(event)
                }
            );
            $(document).on("dblclick.blocks_core", function(event) {
                    Broadcaster.send(new Broadcaster.EVENTS.DOUBLE_CLICK_BLOCK(createBlockEvent(event)));
            });

            resetMouse(true);
        }
    };


    var unregisterMouseEvents = function () {
        if (active) {
            active = false;
            $(document).off("mousedown.blocks_core");
            $(document).off("mouseup.blocks_core");
            $(document).off("mousemove.blocks_core");
        }
    };

    Broadcaster.on(Broadcaster.EVENTS.DID_REFRESH_LAYOUT, function () {
        // TODO: What if layout refreshes while we are dragging
        resetMouse();
    });
    Broadcaster.on(Broadcaster.EVENTS.ENABLE_DRAG, "blocks.core.Mouse", function (event) {enableDrag(event);});
    Broadcaster.on(Broadcaster.EVENTS.DISABLE_DRAG, "blocks.core.Mouse", function (event) {disableDrag(event);});
    Broadcaster.on(Broadcaster.EVENTS.ACTIVATE_MOUSE, "blocks.core.Mouse", function () {registerMouseEvents();});
    Broadcaster.on(Broadcaster.EVENTS.DEACTIVATE_MOUSE, "blocks.core.Mouse", function () {unregisterMouseEvents();});
    Broadcaster.on(Broadcaster.EVENTS.DO_ALLOW_DRAG, "blocks.core.Mouse", function () {allowDrag();});
    Broadcaster.on(Broadcaster.EVENTS.DO_NOT_ALLOW_DRAG, "blocks.core.Mouse", function () {disallowDrag();});


    if (this.config.ACTIVATE_AT_BOOT) {
        $(document).ready(function () {
            registerMouseEvents();
        });
    }


}])
    .config("blocks.core.Mouse", {
        DRAGGING_THRESHOLD: 0,
        ACTIVATE_AT_BOOT: true
    });

