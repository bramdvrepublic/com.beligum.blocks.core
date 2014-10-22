/**
 * Created by wouter on 7/10/14.
 */


blocks.plugin("blocks.core.Mouse", ["blocks.core.Broadcaster", "blocks.core.Elements", "blocks.core.Constants", function (Broadcaster, Elements, Constants) {
    
    var active = true;

    var draggingStatus = Constants.DRAGGING.NO;
    var drag = {startEvent: null, surface: null};
    var currentEvent = null;
    var resizeHandle = {current: null, previous: null};
    var block = {current: null, previous: null};

    var _lastPoints = [];
    var direction = Constants.DIRECTION.NONE;

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

    var getActiveElementsForEvent = function (event) {
        currentEvent = event;
        resizeHandle.previous = resizeHandle.current;
        block.previous = block.current;

        block.current = null;
        resizeHandle.current = null;

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

        // Find the first parent row and check if a resizeHandle is triggered
        var parentRow = block.current;
        while (parentRow != null && !(parentRow instanceof Elements.Row && !(parentRow instanceof Elements.Block))) {
            parentRow = parentRow.parent;
        }
        if (parentRow != null && parentRow instanceof Elements.Row) {
            resizeHandle.current = parentRow.findTriggeredResizeHandle(event, Elements.ResizeHandle);
        }

    };

    var createEvent = function (event) {
        var blocksEvent = {
            event: event,
            resizeHandle: resizeHandle,
            block: block,
            direction: calculateDirection(event),
            dragging: false
        };
        if (draggingStatus == Constants.DRAGGING.YES) {
            blocksEvent.drag = drag;
            blocksEvent.dragging = true;
        }
        return blocksEvent;
    };

    var mouseDown = function (event) {
        if (active) {
            // send down event: resizermousedown, blockmousedown, dropspotmousedown
            // Blockevent = name, jquery Event, Surface
            getActiveElementsForEvent(event);
            if (draggingStatus != Constants.DRAGGING.YES && draggingStatus != Constants.DRAGGING.WAITING) {
                draggingStatus = Constants.DRAGGING.WAITING;
                drag.startEvent = event;
                if (resizeHandle.current != null) {
                    drag.surface = resizeHandle.current;
                } else if (block.current != null && block.current instanceof Elements.Block) {
                    drag.surface = block.current;
                } else {
                    var x = 0;
                }
            }
        } else {
            draggingStatus = Constants.DRAGGING.NO;
            drag.startEvent = null;
        }
    }

    var mouseUp = function (event) {
        getActiveElementsForEvent(event);
        var oldDragStatus = draggingStatus;

        if (oldDragStatus == Constants.DRAGGING.YES) {
            if (drag.surface instanceof Elements.ResizeHandle) {
                Broadcaster.send(config.EVENT.END_DRAG_RESIZE_HANDLE, createEvent(event));
            } else if (drag.surface instanceof Elements.Block) {
                Broadcaster.send(config.EVENT.END_DRAG_BLOCK, createEvent(event));
            }
        }

        draggingStatus = Constants.DRAGGING.NO;
        drag.startEvent = null;
    };

    var enableDragAfterTreshold = function (event) {
        Logger.debug("Wait for drag: " + Math.abs(drag.startEvent.pageX - event.pageX) + " " + Math.abs(drag.startEvent.pageY - event.pageY));
        if (Math.abs(drag.startEvent.pageX - event.pageX) > config.DRAGGING_THRESHOLD ||
            Math.abs(drag.startEvent.pageY - event.pageY) > config.DRAGGING_THRESHOLD) {
            Logger.debug("Start dragging.");

            if (drag.surface != null) {
                draggingStatus = Constants.DRAGGING.YES;
                getActiveElementsForEvent(event);
                if (drag.surface instanceof Elements.ResizeHandle) {
                    Broadcaster.send(config.EVENT.START_DRAG_RESIZE_HANDLE, createEvent(event));
                } else if (block.current != null && block.current instanceof Elements.Block) {
                    Broadcaster.send(config.EVENT.START_DRAG_BLOCK, createEvent(event));
                }
            }
        }
    };

    var mouseMove = function (event) {

        if (active) {

            getActiveElementsForEvent(event);
            if (draggingStatus == Constants.DRAGGING.NO || draggingStatus == Constants.DRAGGING.WAITING) {
                if (block.current != block.previous) {
                    if (block.current == null) {
                        Broadcaster.send(config.EVENT.HOOVER_LEAVE_BLOCK, createEvent(event));
                    } else if (block.previous == null) {
                        Broadcaster.send(config.EVENT.HOOVER_ENTER_BLOCK, createEvent(event));
                    } else {
                        Broadcaster.send(config.EVENT.HOOVER_ENTER_BLOCK, createEvent(event));
                        Broadcaster.send(config.EVENT.HOOVER_LEAVE_BLOCK, createEvent(event));
                    }
                }

                if (resizeHandle.current != resizeHandle.previous) {
                    if (resizeHandle.current == null) {
                        Broadcaster.send(config.EVENT.HOOVER_LEAVE_RESIZE_HANDLE, createEvent(event));
                    } else if (resizeHandle.previous == null) {
                        Broadcaster.send(config.EVENT.HOOVER_ENTER_RESIZE_HANLDE, createEvent(event));
                    } else {
                        Broadcaster.send(config.EVENT.HOOVER_ENTER_RESIZE_HANLDE, createEvent(event));
                        Broadcaster.send(config.EVENT.HOOVER_LEAVE_RESIZE_HANDLE, createEvent(event));
                    }
                }

                if (draggingStatus == Constants.DRAGGING.WAITING) {
                    enableDragAfterTreshold(event)
                }
            }

            if (draggingStatus == Constants.DRAGGING.YES) {
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

                if (resizeHandle.current != resizeHandle.previous) {
                    if (resizeHandle.current == null) {
                        Broadcaster.send(config.EVENT.DRAG_LEAVE_RESIZE_HANDLE, createEvent(event));
                    } else if (resizeHandle.previous == null) {
                        Broadcaster.send(config.EVENT.DRAG_ENTER_RESIZE_HANLDE, createEvent(event));
                    } else {
                        Broadcaster.send(config.EVENT.DRAG_ENTER_RESIZE_HANLDE, createEvent(event));
                        Broadcaster.send(config.EVENT.DRAG_LEAVE_RESIZE_HANDLE, createEvent(event));
                    }
                }

            }

        }
    };

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
    $(document).ready(function () {
        disableSelection();
    })


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
        $(window).on("resize", function () {
            onResize(event)
        });
        // Moving out of window, stop dragging
        $(document).on("mouseout", function (event) {
            var from = event.relatedTarget || event.toElement;
            if (!from || from.nodeName == "HTML") {
                mouseUp(event);
            }
        });
    }

    var unregisterMouseEvent = function () {
        $(document).on("mousedown.blocks_core");
        $(document).on("mouseup.blocks_core");
        $(document).off("mousemove.blocks_core");
    }

    registerMouseEvents();
    Broadcaster.on(this.config.EVENT.REFRESH_LAYOUT, function (event) {
        refreshLayout()
    });
    $(document).ready(function () {
        refreshLayout();
    });


}])
    .config("blocks.core.Mouse", {

        DRAGGING_THRESHOLD: 10,
        EVENT: {
            START_DRAG_RESIZE_HANDLE: "startDragResizeHandle",
            START_DRAG_BLOCK: "startDragBlock",
            END_DRAG_RESIZE_HANDLE: "endDragResizeHandle",
            END_DRAG_BLOCK: "endDragBlock",
            HOOVER_LEAVE_BLOCK: "hooverLeaveLayoutElement",
            HOOVER_ENTER_BLOCK: "hooverEnterLayoutElement",
            HOOVER_LEAVE_RESIZE_HANDLE: "hooverLeaveResizeHandle",
            HOOVER_ENTER_RESIZE_HANLDE: "hooverEnterResizeHandle",
            DRAG_LEAVE_BLOCK: "dragLeaveLayoutElement",
            DRAG_ENTER_BLOCK: "dragEnterLayoutElement",
            DRAG_OVER_BLOCK: "dragOverLayoutElement",
            DRAG_LEAVE_RESIZE_HANDLE: "dragLeaveResizeHandle",
            DRAG_ENTER_RESIZE_HANLDE: "dragEnterResizeHandle",
            REFRESH_LAYOUT: "refreshLayout"
        }

    });

