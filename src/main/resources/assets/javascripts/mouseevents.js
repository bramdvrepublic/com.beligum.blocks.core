/**
 * Created by wouter on 7/10/14.
 */


blocks.module("blocks.mouseEvent", ["blocks.broadcaster", "blocks.elements" ])
    .service("MouseEvent", ["Broadcaster", "LayoutAnalyzer", "BlockMouseEvents", "Elements", function(Broadcaster, LayoutAnalyzer, BlockMouseEvents, Elements) {
        var DRAGGING = {
            WAITING: 1,
                YES: 2,
                NO: 3,
                TEXT_SELECTION: 4
        }
        var DRAGGING_THRESHOLD = 10;
        var active = true;
        var draggingStatus = DRAGGING.NO;
        var drag = {startEvent: null, surface: null};
        var currentEvent = null;
        var dropSpot = {current: null, previous: null};
        var resizeHandle = {current: null, previous: null};
        var layoutElement = {current: null, previous: null};

        var refreshLayout = function() {
            LayoutAnalyzer.generateHotspots();
            dropSpot = {current: null, previous: null};
            resizeHandle = {current: null, previous: null};
            layoutElement = {current: null, previous: null};
        };

        var getActiveElementsForEvent = function(event) {
            currentEvent = event;
            dropSpot.previous = dropSpot.current;
            resizeHandle.previous = resizeHandle.current;
            layoutElement.previous = layoutElement.current;

            layoutElement.current = null;
            dropSpot.current = null;
            resizeHandle.current = null;

            // First search for active element
            // If an element is active, we have a big chance the next event is in the same element, so we start our search here
            if (layoutElement.previous != null) {
                layoutElement.current = layoutElement.previous.findActiveElement(event);
            }
            // Our shortcut failed so search the full page
            // we loop the trees of elements to find the smallest active element
            if (layoutElement.current == null) {
                var i = 0;
                while (i < LayoutAnalyzer.trees.length && layoutElement.current == null) {
                    layoutElement.current = LayoutAnalyzer.trees[i].findActiveElement(event);
                    i++;
                }
            }

            // Find the first parent row and check if a resizeHandle is triggered
            var parentRow = layoutElement.current;
            while (parentRow != null && !(parentRow instanceof Elements.Row)) {
                parentRow = parentRow.parent;
            }
            if (parentRow != null && parentRow instanceof Elements.Row) {
                resizeHandle.current = parentRow.findActiveTrigger(event, Elements.ResizeHandle);
            }

            // Check if a dropSpot is triggered
            if (dropSpot.previous == null || !dropSpot.previous.isTriggered(event)) {
                if (layoutElement.current != null) {
                    dropSpot.current = layoutElement.current.findActiveTrigger(event, Elements.DropSpot);
                } else {
                    dropSpot.current = null;
                }
            } else {
                dropSpot.current = dropSpot.previous;
            }

        };

        var createEvent = function(event) {
            var blocksEvent = {
                event: event,
                dropSpot: dropSpot,
                resizeHandle: resizeHandle,
                trigger: this.trigger,
                dragging: false
            };
            if (draggingStatus == DRAGGING.YES) {
                blocksEvent.drag = drag;
                blocksEvent.dragging = true;
            }
            return blocksEvent;

        };

        var mouseDown = function(event) {
            if (active) {
                // send down event: resizermousedown, blockmousedown, dropspotmousedown
                // Blockevent = name, jquery Event, Surface
                getActiveElementsForEvent(event);
                if (draggingStatus != DRAGGING.YES && draggingStatus != DRAGGING.WAITING) {
                    draggingStatus = DRAGGING.WAITING;
                    drag.startEvent = event;
                    if (resizeHandle.current != null) {
                        drag.surface = resizeHandle.current;
                    } else if (layoutElement.current != null && layoutElement.current instanceof Elements.Block) {
                        drag.surface = layoutElement.current;
                    } else {
                        var x = 0;
                    }
                }

            } else {
                draggingStatus = DRAGGING.NO;
                drag.startEvent = null;
            }
        }

        var mouseUp = function(event) {

            getActiveElementsForEvent(event);
            var oldDragStatus = draggingStatus;

            if (oldDragStatus == DRAGGING.YES) {
                if (drag.surface instanceof Elements.ResizeHandle) {
                    Broadcaster.send(BlockMouseEvents.END_DRAG_RESIZE_HANDLE, createEvent(event));
                } else if (drag.surface instanceof Elements.Block) {
                    Broadcaster.send(BlockMouseEvents.END_DRAG_BLOCK, createEvent(event));
                }
            }

            draggingStatus = DRAGGING.NO;
            drag.startEvent = null;
        };

        var enableDragAfterTreshold = function(event) {
            Logger.debug("Wait for drag: " + Math.abs(drag.startEvent.pageX - event.pageX) + " " + Math.abs(drag.startEvent.pageY - event.pageY));
            if (Math.abs(drag.startEvent.pageX - event.pageX) > DRAGGING_THRESHOLD ||
                Math.abs(drag.startEvent.pageY - event.pageY) > DRAGGING_THRESHOLD) {
                Logger.debug("Start dragging.");

                if (drag.surface != null) {
                    draggingStatus = DRAGGING.YES;
                    getActiveElementsForEvent(event);
                    if (drag.surface instanceof Elements.ResizeHandle) {
                        Broadcaster.send(BlockMouseEvents.START_DRAG_RESIZE_HANDLE, createEvent(event));
                    } else if (layoutElement.current != null && layoutElement.current instanceof Elements.Block) {
                        Broadcaster.send(BlockMouseEvents.START_DRAG_BLOCK, createEvent(event));
                    }
                }
            }
        };

        var mouseMove = function(event) {

            if (active) {

                getActiveElementsForEvent(event);
                if (draggingStatus == DRAGGING.NO || draggingStatus == DRAGGING.WAITING) {
                    if (layoutElement.current != layoutElement.previous) {
                        if (layoutElement.current == null) {
                            Broadcaster.send(BlockMouseEvents.HOOVER_LEAVE_LAYOUT_ELEMENT, createEvent(event));
                        } else if (layoutElement.previous == null) {
                            Broadcaster.send(BlockMouseEvents.HOOVER_ENTER_LAYOUT_ELEMENT, createEvent(event));
                        } else {
                            Broadcaster.send(BlockMouseEvents.HOOVER_ENTER_LAYOUT_ELEMENT, createEvent(event));
                            Broadcaster.send(BlockMouseEvents.HOOVER_LEAVE_LAYOUT_ELEMENT, createEvent(event));
                        }
                    }

                    if (resizeHandle.current != resizeHandle.previous) {
                        if (resizeHandle.current == null) {
                            Broadcaster.send(BlockMouseEvents.HOOVER_LEAVE_RESIZE_HANDLE, createEvent(event));
                        } else if (resizeHandle.previous == null) {
                            Broadcaster.send(BlockMouseEvents.HOOVER_ENTER_RESIZE_HANLDE, createEvent(event));
                        } else {
                            Broadcaster.send(BlockMouseEvents.HOOVER_ENTER_RESIZE_HANLDE, createEvent(event));
                            Broadcaster.send(BlockMouseEvents.HOOVER_LEAVE_RESIZE_HANDLE, createEvent(event));
                        }
                    }

                    if (dropSpot.current != dropSpot.previous) {
                        if (dropSpot.current == null) {
                            Broadcaster.send(BlockMouseEvents.HOOVER_LEAVE_DROPSPOT, createEvent(event));
                        } else if (dropSpot.previous == null) {
                            Broadcaster.send(BlockMouseEvents.HOOVER_ENTER_DROPSPOT, createEvent(event));
                        } else {
                            Broadcaster.send(BlockMouseEvents.HOOVER_ENTER_DROPSPOT, createEvent(event));
                            Broadcaster.send(BlockMouseEvents.HOOVER_LEAVE_DROPSPOT, createEvent(event));
                        }
                    }

                    if (draggingStatus == DRAGGING.WAITING) {
                        enableDragAfterTreshold(event)
                    }
                }

                if (draggingStatus == DRAGGING.YES) {
                    if (layoutElement.current != layoutElement.previous) {
                        if (layoutElement.current == null) {
                            Broadcaster.send(BlockMouseEvents.DRAG_LEAVE_LAYOUT_ELEMENT, createEvent(event));
                        } else if (layoutElement.previous == null) {
                            Broadcaster.send(BlockMouseEvents.DRAG_ENTER_LAYOUT_ELEMENT, createEvent(event));
                        } else {
                            Broadcaster.send(BlockMouseEvents.DRAG_ENTER_LAYOUT_ELEMENT, createEvent(event));
                            Broadcaster.send(BlockMouseEvents.DRAG_LEAVE_LAYOUT_ELEMENT, createEvent(event));
                        }
                    }

                    if (resizeHandle.current != resizeHandle.previous) {
                        if (resizeHandle.current == null) {
                            Broadcaster.send(BlockMouseEvents.DRAG_LEAVE_RESIZE_HANDLE, createEvent(event));
                        } else if (resizeHandle.previous == null) {
                            Broadcaster.send(BlockMouseEvents.DRAG_ENTER_RESIZE_HANLDE, createEvent(event));
                        } else {
                            Broadcaster.send(BlockMouseEvents.DRAG_ENTER_RESIZE_HANLDE, createEvent(event));
                            Broadcaster.send(BlockMouseEvents.DRAG_LEAVE_RESIZE_HANDLE, createEvent(event));
                        }
                    }

                    if (dropSpot.current != dropSpot.previous) {
                        if (dropSpot.current == null) {

                            Broadcaster.send(BlockMouseEvents.DRAG_LEAVE_DROPSPOT, createEvent(event));
                        } else if (dropSpot.previous == null) {
                            Broadcaster.send(BlockMouseEvents.DRAG_ENTER_DROPSPOT, createEvent(event));
                        } else {
                            Broadcaster.send(BlockMouseEvents.DRAG_ENTER_DROPSPOT, createEvent(event));
                            Broadcaster.send(BlockMouseEvents.DRAG_LEAVE_DROPSPOT, createEvent(event));
                        }
                    }
                }

            }
        };

        var onResize = function(event) {
            Broadcaster.send("refreshLayout", event);

        };

        var disableSelection = function() {
            // http://stackoverflow.com/questions/826782/css-rule-to-disable-text-selection-highlighting#4407335
            $("body").css("-webkit-touch-callout", "none");
            $("body").css("-khtml-user-select", "none");
            $("body").css("-moz-user-select", "none");
            $("body").css("-ms-user-select", "none");
            $("body").css("user-select", "none");
            // IE < 10
            $("body").attr("onselectstart", "return false;");

        };
        $(document).ready(function() {
            disableSelection();
        })


        var enableSelection = function() {
            //http://stackoverflow.com/questions/826782/css-rule-to-disable-text-selection-highlighting#4407335
            $("body").css("-webkit-touch-callout", "text");
            $("body").css("-khtml-user-select", "text");
            $("body").css("-moz-user-select", "text");
            $("body").css("-ms-user-select", "text");
            $("body").css("user-select", "text");
            $("body").attr("onselectstart", "return true;");
        }

        var registerMouseEvents = function() {
            $(document).on("mousedown.blocks_core", function (event) {mouseDown(event)});
            $(document).on("mouseup.blocks_core", function (event) {mouseUp(event)});
            $(document).on("mousemove.blocks_core", function (event) {
                mouseMove(event)}
            );
            $(window).on("resize", function () {onResize(event)});
            // Moving out of window, stop dragging
            $(document).on("mouseout", function(event) {
                var from = event.relatedTarget || event.toElement;
                if (!from || from.nodeName == "HTML") {
                    mouseUp(event);
                }
            });
        }

        var unregisterMouseEvent = function() {
            $(document).on("mousedown.blocks_core");
            $(document).on("mouseup.blocks_core");
            $(document).off("mousemove.blocks_core");
        }

        registerMouseEvents();
        Broadcaster.on(BlockMouseEvents.REFRESH_LAYOUT, function(event) {refreshLayout()});
        $(document).ready(function() {
            refreshLayout();
        });


    }])
    .factory("BlockMouseEvents", function() {
        return {
            START_DRAG_RESIZE_HANDLE: "startDragResizeHandle",
            START_DRAG_BLOCK: "startDragBlock",
            END_DRAG_RESIZE_HANDLE: "endDragResizeHandle",
            END_DRAG_BLOCK: "endDragBlock",
            HOOVER_LEAVE_LAYOUT_ELEMENT: "hooverLeaveLayoutElement",
            HOOVER_ENTER_LAYOUT_ELEMENT: "hooverEnterLayoutElement",
            HOOVER_LEAVE_RESIZE_HANDLE: "hooverLeaveResizeHandle",
            HOOVER_ENTER_RESIZE_HANLDE: "hooverEnterResizeHandle",
            HOOVER_LEAVE_DROPSPOT: "hooverLeaveDropspot",
            HOOVER_ENTER_DROPSPOT: "hooverEnterDropspot",
            DRAG_LEAVE_LAYOUT_ELEMENT: "dragLeaveLayoutElement",
            DRAG_ENTER_LAYOUT_ELEMENT: "dragEnterLayoutElement",
            DRAG_LEAVE_RESIZE_HANDLE: "dragLeaveResizeHandle",
            DRAG_ENTER_RESIZE_HANLDE: "dragEnterResizeHandle",
            DRAG_LEAVE_DROPSPOT: "dragLeaveDropspot",
            DRAG_ENTER_DROPSPOT: "dragEnterDropspot",
            REFRESH_LAYOUT: "refreshLayout"
        }
    });

