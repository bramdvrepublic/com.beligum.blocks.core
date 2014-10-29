/**
 * Created by wouter on 7/10/14.
 */


blocks.plugin("blocks.core.Mouse", ["blocks.core.Broadcaster", "blocks.core.Elements", "blocks.core.Constants", function (Broadcaster, Elements, Constants) {
    
    var active = false;
    var draggingStatus = Constants.DRAGGING.CAN_NOT_START_DRAG;
    var draggingOptions = {startEvent: null, surface: null};
    var currentEvent = null;
    var block = {current: null, previous: null};
    var _lastPoints = [];
    var config = this.config;

    var refreshLayout = function () {
        Elements.generateHotspots();
        resizeHandle = {current: null, previous: null};
        block = {current: null, previous: null};
    };

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

    var getHooveredBlockForEvent = function (event) {
        currentEvent = event;
        block.previous = block.current;
        block.current = null;

        // First search for active element
        // If an element is active, we have a big chance the next event is in the same element, so we start our search here
        if (block.previous != null) {
            block.current = block.previous.findActiveElement(event);
        }
        // Our shortcut failed so search the full page
        // we loop the trees of elements to find the smallest active element
        if (block.current == null) {
            var i = 0;
            while (i < Elements.trees.length && block.current == null) {
                block.current = Elements.trees[i].findActiveElement(event);
                i++;
            }
        }

    };

    var createEvent = function (event) {
        var blocksEvent = {
            event: event,
            block: block,
            direction: calculateDirection(event),
            dragging: draggingStatus
        };

        if (draggingStatus == Constants.DRAGGING.YES) {
            blocksEvent.drag = draggingOptions;
            blocksEvent.dragging = draggingStatus;
        }
        return blocksEvent;
    };

    var resetDrag = function() {
        draggingStatus = Constants.DRAGGING.CAN_NOT_START_DRAG;
        draggingOptions.startEvent = null;
        draggingOptions.surface = null;
    }

    var mouseDown = function (event) {
        if (active && draggingStatus != Constants.DRAGGING.NOT_ALLOWED) {
            getHooveredBlockForEvent(event);
            if (draggingStatus == Constants.DRAGGING.CAN_START_DRAG) {
                    draggingStatus = Constants.DRAGGING.WAITING;
                    draggingOptions.startEvent = event;
                    disableSelection();
            } else {
                resetDrag();
                Logger.debug("We can not start drag because dragging is already in place or not allowed. " + draggingStatus);
            }
        }
    }

    var mouseUp = function (event) {
        enableSelection();
        if (active && draggingStatus != Constants.DRAGGING.NOT_ALLOWED) {
            getHooveredBlockForEvent(event);
            var oldDragStatus = draggingStatus;
            if (oldDragStatus == Constants.DRAGGING.YES) {
                Broadcaster.send(config.EVENT.END_DRAG, createEvent(event));
            }
            resetDrag();
        }
    };


    var enableDragAfterTreshold = function (event) {
        if (Math.abs(draggingOptions.startEvent.pageX - event.pageX) > config.DRAGGING_THRESHOLD ||
            Math.abs(draggingOptions.startEvent.pageY - event.pageY) > config.DRAGGING_THRESHOLD) {
            if (draggingOptions.surface != null) {
                draggingStatus = Constants.DRAGGING.YES;
                getHooveredBlockForEvent(event);
                Broadcaster.send(config.EVENT.START_DRAG, createEvent(event));
            }
        }
    };

    var mouseMove = function (event) {

        if (active) {

            getHooveredBlockForEvent(event);
            if (draggingStatus != Constants.DRAGGING.YES) {
                if (block.current != block.previous) {
                    if (block.current == null) {
                        Broadcaster.send(config.EVENT.HOOVER_LEAVE_BLOCK, createEvent(event));
                    } else if (block.previous == null) {
                        Broadcaster.send(config.EVENT.HOOVER_ENTER_BLOCK, createEvent(event));
                    } else {
                        Broadcaster.send(config.EVENT.HOOVER_ENTER_BLOCK, createEvent(event));
                        Broadcaster.send(config.EVENT.HOOVER_LEAVE_BLOCK, createEvent(event));
                    }
                } else {
                    Broadcaster.send(config.EVENT.HOOVER_OVER_BLOCK, createEvent(event));
                }

                if (draggingStatus == Constants.DRAGGING.WAITING) {
                    enableDragAfterTreshold(event)
                }
            } else {
                if (block.current != block.previous) {
                    if (block.current == null) {
                        Broadcaster.send(config.EVENT.DRAG_LEAVE_BLOCK, createEvent(event));
                    } else if (block.previous == null) {
                        Broadcaster.send(config.EVENT.DRAG_ENTER_BLOCK, createEvent(event));
                    } else {
                        Broadcaster.send(config.EVENT.DRAG_ENTER_BLOCK, createEvent(event));
                        Broadcaster.send(config.EVENT.DRAG_LEAVE_BLOCK, createEvent(event));
                    }
                } else if (block.current != null) {
                    Broadcaster.send(config.EVENT.DRAG_OVER_BLOCK, createEvent(event));
                }

            }
        }
    };

    // Todo: canNotstartDrag for surface with hiogher priority received after canStartDrag. What to do?
    var canStartDrag = function (options) {
        if (draggingStatus == Constants.DRAGGING.NOT_ALLOWED) return;
        if (options.surface != null && options.surface instanceof  Elements.Surface) {
            Logger.debug("Can start drag")
            if (draggingOptions.surface != null && draggingOptions.priority >= options.priority) {
                draggingOptions.surface = options.surface;
                draggingOptions.priority = options.priority;
                draggingStatus = Constants.DRAGGING.CAN_START_DRAG;
            } else if (draggingOptions.surface == null) {
                draggingOptions.surface = options.surface;
                draggingOptions.priority = options.priority;
                draggingStatus = Constants.DRAGGING.CAN_START_DRAG;
            } else {
                Logger.debug("We can not drag for unknown reason type of element");

            }
        } else {
            Logger.debug("We can not drag this type of element")
        }
    };

    var canNotStartDrag = function (options) {
        if (draggingStatus == Constants.DRAGGING.NOT_ALLOWED) return;
        if (options == null || draggingOptions.surface == options.surface) {
            Logger.debug("Can not start drag")
            resetDrag();
            block.previous = null;
            block.current = null;
        }
    };

    var disallowDrag = function() {
        Logger.debug("Dragging not allowed");
        draggingOptions.surface = null;
        draggingStatus = Constants.DRAGGING.NOT_ALLOWED;
    };

    var allowDrag = function() {
        Logger.debug("Dragging allowed")
        resetDrag();
        block.previous = null;
        block.current = null;
    }

    var onResize = function (event) {
        Broadcaster.send("refreshLayout", event);

    };

    var disableSelection = function () {
        // http://stackoverflow.com/questions/826782/css-rule-to-disable-text-selection-highlighting#4407335
        $("body").css("-webkit-touch-callout", "none");
        $("body").css("-khtml-user-select", "none");
        $("body").css("-moz-user-select", "none");
        $("body").css("-ms-user-select", "none");
        $("body").css("user-select", "none");
        // IE < 10
        $("body").attr("onselectstart", "return false;");

    };






    var enableSelection = function () {
        //http://stackoverflow.com/questions/826782/css-rule-to-disable-text-selection-highlighting#4407335
        $("body").css("-webkit-touch-callout", "text");
        $("body").css("-khtml-user-select", "text");
        $("body").css("-moz-user-select", "text");
        $("body").css("-ms-user-select", "text");
        $("body").css("user-select", "text");
        $("body").attr("onselectstart", "return true;");
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
            $(window).on("resize.blocks_core", function () {
                onResize(event)
            });
            // Moving out of window, stop dragging
            $(document).on("mouseout.blocks_core", function (event) {
                var from = event.relatedTarget || event.toElement;
                if (!from || from.nodeName == "HTML") {
                    mouseUp(event);
                }
            });
            refreshLayout();
        }
    };

    var unregisterMouseEvents = function () {
        if (active) {
            active = false;
            $(document).off("mousedown.blocks_core");
            $(document).off("mouseup.blocks_core");
            $(document).off("mousemove.blocks_core");
            $(document).off("resize.blocks_core");
            $(document).off("mouseout.blocks_core");
        }
    };

    Broadcaster.on(this.config.EVENT.REFRESH_LAYOUT, function (event) {refreshLayout()});
    Broadcaster.on(this.config.EVENT.CAN_START_DRAG, function (surface) {canStartDrag(surface);});
    Broadcaster.on(this.config.EVENT.CAN_NOT_START_DRAG, function (surface) {canNotStartDrag(surface);});
    Broadcaster.on(this.config.EVENT.ACTIVATE_MOUSE, function (surface) {registerMouseEvents();});
    Broadcaster.on(this.config.EVENT.DEACTIVATE_MOUSE, function (surface) {unregisterMouseEvents();});
    Broadcaster.on(this.config.EVENT.ALLOW_DRAG, function (surface) {allowDrag();});
    Broadcaster.on(this.config.EVENT.DO_NOT_ALLOW_DRAG, function (surface) {disallowDrag();});

    $(document).ready(function () {
        registerMouseEvents();
    });


}])
    .config("blocks.core.Mouse", {

        DRAGGING_THRESHOLD: 0,
        EVENT: {
            CAN_START_DRAG: "canStartDrag",
            CAN_NOT_START_DRAG: "canNotStartDrag",
            START_DRAG: "startDrag",
            END_DRAG: "endDrag",
            ALLOW_DRAG: "allowDrag",
            DO_NOT_ALLOW_DRAG: "disallowDrag",
            HOOVER_LEAVE_BLOCK: "hooverLeaveLayoutElement",
            HOOVER_ENTER_BLOCK: "hooverEnterLayoutElement",
            HOOVER_OVER_BLOCK: "hooverEnterLayoutElement",
            DRAG_LEAVE_BLOCK: "dragLeaveLayoutElement",
            DRAG_ENTER_BLOCK: "dragEnterLayoutElement",
            DRAG_OVER_BLOCK: "dragOverLayoutElement",
            ACTIVATE_MOUSE: "activateMouse",
            DEACTIVATE_MOUSE: "deactivateMouse",
            REFRESH_LAYOUT: "refreshLayout"
        }

    });

