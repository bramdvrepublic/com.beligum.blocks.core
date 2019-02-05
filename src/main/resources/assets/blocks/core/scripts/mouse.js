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

base.plugin("blocks.core.Mouse", ["base.core.Commons", "blocks.core.Broadcaster", "constants.blocks.core", "blocks.core.Sidebar", "blocks.core.UI", function (Commons, Broadcaster, BlocksConstants, SideBar, UI)
{
    var Mouse = this;

    //-----CONSTANTS-----
    this.DRAGGING = {
        NO: 0,
        YES: 1,
        DISABLED: 2,
    };

    // instead of checking every element that's clicked and filter out the ones we're interested in,
    // we set a filter on the parent dom containers for which to process events
    var DOM_FILTER = "." + BlocksConstants.PAGE_CONTENT_CLASS + ", ." + BlocksConstants.BLOCK_OVERLAY_WRAPPER_CLASS + ", ." + BlocksConstants.CREATE_BLOCK_CLASS;

    // The minimum number of pixels we need to move before real dragging starts.
    var DRAG_PX_THRESHOLD = 3;
    // The minimum amount of time we need to be clicking before real dragging starts.
    var DRAG_MILLIS_THRESHOLD = 1000;

    //show dragging direction
    var SHOW_DEBUG_LINES = false;

    //the size of the history window to keep during dragging
    var WINDOW_SIZE = 20;

    //evict stats from the window that are older than this time diff
    //set to -1 to disable evicting
    var MAX_TIMEDIFF_MILLIS = 1000;

    //multiplier for the DOM direction line to calculate intersections will all possible block edges
    var DIRECTION_MULTIPLIER = 10000;

    //this allows all variances to pass and enabling it seems to result in a more natural
    //experience because you can always see 'where we go'. Smoothing the resulting direction
    //vector is a better solution than filtering out high variances
    //For a smooth experience, either set this to false or set a relatively high threshold below
    var IGNORE_VARIANCE = false;

    //this is the maximum variance (region [0..1]) that's tolerated
    //during mouse vector updates. All updates with larger variances
    //won't result in a vector recalculation
    var VARIANCE_THRESHOLD = 0.75;

    //-----VARIABLES-----
    // flag to enable/disable this entire module (both clicking and dragging)
    var active = false;

    // flag to enable/disable click events (otherwise they are simulated by tracking mouse down/up)
    var enableClickEvents = true;

    // true if we really started dragging, past the threshold
    var draggingStatus = Mouse.DRAGGING.NO;

    //the current general (mean) direction vector we're dragging in
    var dragVector = {};

    // the surface that's currently being dragged around
    var mousedownSurface = null;

    //the low-level element we really clicked on
    var clickedElement = null;

    //the surface we're currently hovering on during dragging
    var hoveredSurface = null;

    // the original mousedown event that started the drag
    var mousedownEvent = null;

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
        //the absolute sum of the total lengths (disregarding the window)
        totalLengthAbs: 0,
        //the floating sum of the timeDiffs
        totalTimeDiff: 0,
        //the absolute sum of the timeDiffs (disregarding the window)
        totalTimeDiffAbs: 0,

        //the circular variance of all the angles
        variance: 0,
        //the mean angle of all the angles
        direction: 0,
        //the current speed in pixels per second
        speed: 0,
    };

    var debugCanvas = null;

    //-----GENERAL JQUERY FUNCTIONS-----
    /**
     * JQuery event listener for more performant window resizing.
     * Use like this: $(window).smartresize(function (event) {});
     */
    $.fn["smartresize"] = function (fn)
    {
        return fn ? this.bind('resize', debounce(fn)) : this.trigger("smartresize");
    };

    /**
     * JQuery event listener for more performant window resizing.
     * Use like this: $(document).on("smartmousemove", function (event) {});
     */
    $.fn["smartmousemove"] = function (fn)
    {
        return fn ? this.bind('mousemove', debounce(fn)) : this.trigger("smartmousemove");
    };

    //-----PUBLIC METHODS-----
    /**
     * Activate this module and start listening for mouse click events (and DnD)
     */
    this.enable = function (enable)
    {
        if (enable) {
            if (!active) {

                //put this as fast as possible to eliminate double event registering
                active = true;

                //make sure we start with a clean slate
                _resetMouse();

                $(document).on("mousedown.blocks_core", DOM_FILTER, function (event)
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

                $(document).on("mouseup.blocks_core", DOM_FILTER, function (event)
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
        }
        //disable
        else {
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
        }
    };

    this.enableClickEvents = function(enable)
    {
        enableClickEvents = enable;
    };

    this.enableDragging = function (enable)
    {
        draggingStatus = enable ? Mouse.DRAGGING.NO : Mouse.DRAGGING.DISABLED;
    };

    /**
     * Enables/disables the selection of text in the page
     * See http://stackoverflow.com/questions/826782/css-rule-to-disable-text-selection-highlighting#4407335
     */
    this.enableNativeDnd = function (enable)
    {
        if (enable) {

            UI.html.removeClass(BlocksConstants.PREVENT_SELECTION_CLASS);

            window.ondragstart = function ()
            {
                return true;
            };
        }
        else {
            //de-select existing selection
            var sel = window.getSelection().removeAllRanges();

            //add user-select: none; to the root html
            UI.html.addClass(BlocksConstants.PREVENT_SELECTION_CLASS);

            //disable dragging
            window.ondragstart = function ()
            {
                return false;
            };
        }
    };

    /**
     * Enables/disables the context menu
     */
    this.enableContextMenu = function (enable)
    {
        if (enable) {
            $("html").removeAttr("oncontextmenu", "");
            // IE < 10
            $("html").removeAttr("onselectstart");
        }
        else {
            $("html").attr("oncontextmenu", "return false;");
            // IE < 10
            $("html").attr("onselectstart", "return false;");
        }
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
        //TODO can't decide if we need this
        // event.preventDefault();
        // event.stopPropagation();

        //before setting the tracking variables, we make sure we start with a clean slate because mousedown starts everything
        _resetMouse();

        //four options:
        // 1) we clicked on a valid surface
        // 2) we clicked on a resize handle
        // 3) we clicked on the create new button
        // 4) we clicked outside any surface

        //note that we are a low-level mouse event handler, so we track the mouse
        //as long as we're activated, even if we didn't click on any particular element,
        //because that will probably mean we need to reset some stuff in the manager
        //that's why we always implement 'option 4':
        mousedownEvent = event;
        mousedownSurface = null;

        //this will be one of these:
        // - the overlay element if mousedown is on an overlay
        // - the new button element if mousedown is on the new block button
        // - a random other element if mousedown is not on one of two above
        var element = $(event.target);

        //check if we're pressing on the new-block-button and create a new block if we are
        //this will allow us to handle creating new blocks a lot like moving existing blocks
        if (element.hasClass(BlocksConstants.CREATE_BLOCK_CLASS) || element.parents("." + BlocksConstants.CREATE_BLOCK_CLASS).length > 0) {
            mousedownSurface = new blocks.elements.Block();
        }
        else {
            // If the element we click on has a registered ID attribute and we can find it in our surface model,
            // we clicked on a valid surface. Note that we only have block and resizer surfaces, so we'll always
            // click on one of them (or nothing at all).
            mousedownSurface = blocks.elements.Surface.lookup(element);
        }

        // if we clicked on a valid surface, we need to activate a few things extra
        if (mousedownSurface) {

            // Important note:
            // Clicking on a surface is not enough: we need to know on which low-level element inside
            // (the element of) that surface we clicked (this can be any element, not just properties;
            // for instance blocks-image registeres all <img> tags).
            // Because the overlay is 'blocking' the event on the low-level element, we use a little trick
            // by temporarily disable pointer events on the overlay by setting OVERLAY_NO_EVENTS_CLASS.
            // This will allow next events to pass through the lower-level elements.
            // Note that the first event that will use this is either the next move or the next mouseup.
            // That low-level element will be tracked by setting clickedElement on the next event.
            // In the mouseup event, we remove the event blocking on the overlay again.
            // Also note that this will disable the hover events on the overlays while dragging,
            // which is exactly what we want.
            UI.overlayWrapper.addClass(BlocksConstants.OVERLAY_NO_EVENTS_CLASS);

            //start listening for mouse movement (even if dragging is disabled, to fill the clickedElement asap)
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
                //if we encounter the situation where we're still listening while the module
                // is disabled, immediately reset ourself (unregistering while doing so)
                else {
                    _resetMouse();
                }
            });
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
        var targetElement = $(event.target);

        // Save the first element we encounter after mousedown,
        // either here or in mouseup. This way, we catch the closest
        // element to the mousedown coordinate
        if (!clickedElement) {
            clickedElement = targetElement;
        }

        // if dnd was actually disabled, we'll unregister ourself immediately,
        // only using this to fill the clickedElement above
        if (draggingStatus === Mouse.DRAGGING.DISABLED) {
            $(document).off("mousemove.blocks_core");
        }
        //if not, do our normal dnd tracking
        else {

            //first, update the statistics and vectors
            updateStats(event);
            updateVector(event);

            if (draggingStatus === Mouse.DRAGGING.NO) {

                //this is something we always do
                // (actually, this only needs to be disabled once, on first move, but hey)
                // to disable the dragging of images and selection of text when we're
                // operating below the dragging threshold
                Mouse.enableNativeDnd(false);

                //first, check if we need to activate dragging
                //note that we only start dragging after a certain pixel threshold, except for the resizers because
                //sometimes they need very fine dragging (col in row in col)
                if (mousedownSurface.isResizer() || stats.totalLengthAbs > DRAG_PX_THRESHOLD || stats.totalTimeDiffAbs > DRAG_MILLIS_THRESHOLD) {

                    draggingStatus = Mouse.DRAGGING.YES;

                    //this will re-activate the overlays because if we're dragging, we need them to
                    //figure out which surface we're hovering on
                    UI.overlayWrapper.removeClass(BlocksConstants.OVERLAY_NO_EVENTS_CLASS);

                    //note: we'll also directly trigger a move, see below
                    Broadcaster.send(Broadcaster.EVENTS.MOUSE.DRAG.START, event, {
                        //this is the surface we're dragging around
                        surface: mousedownSurface,
                        //this is the DOM element we started our drag on
                        element: clickedElement,
                        //this is the original mousedown event that started the drag
                        event: mousedownEvent,
                    });
                }
            }
            //we're past the threshold and are dragging a block around
            else if (draggingStatus === Mouse.DRAGGING.YES) {

                //keep track of the surfaces we're hovering on
                var prevHoveredSurface = hoveredSurface;

                //Note: this will be null for the very first move, because the OVERLAY_NO_EVENTS_CLASS was still
                //active when the dragging status changed to yes and causing the targetElement to be the low-level
                //DOM element causing the event instead of the overlay.
                //We choose to skip this first event (probably just one pixel) and only fire when we have a valid surface,
                //which is what we expect anyway
                hoveredSurface = blocks.elements.Surface.lookup(targetElement);

                if (hoveredSurface) {
                    Broadcaster.send(Broadcaster.EVENTS.MOUSE.DRAG.MOVE, event, {
                        //this is the surface we started the drag on
                        surface: mousedownSurface,
                        //this is the DOM element we started our drag on
                        element: clickedElement,
                        //this is the original mousedown event that started the drag
                        originalEvent: mousedownEvent,

                        //this is the surface we previously hovered on
                        prevHoveredSurface: prevHoveredSurface,
                        //this is the surface we're currently hovering on
                        hoveredSurface: hoveredSurface,

                        //the current dragvector (x1,y1,x2,y2)
                        dragVector: dragVector,
                        //the statistics of the dragvector (variance, direction, speed)
                        dragStats: stats,
                    });
                }
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
        //TODO can't decide if we need this
        // event.preventDefault();
        // event.stopPropagation();

        if (SHOW_DEBUG_LINES && debugCanvas) {
            debugCanvas.remove();
            debugCanvas = null;
        }

        // if the clicked element wasn't set by now,
        // we just use the target of the event because this
        // means we clicked without moving at little bit
        // (otherwise it's set during mousemove)
        if (!clickedElement) {
            clickedElement = $(event.target);
        }

        //if we didn't drag (or we weren't allowed to), we clicked
        if (draggingStatus === Mouse.DRAGGING.NO || draggingStatus === Mouse.DRAGGING.DISABLED) {

            // This will prevent a 'click' event from happening when
            // we have the blocks system in place and we do a mousedown/mouseup
            // on the same element.
            // This blocking is needed to prevent click listeners from firing automatically
            // when we focus a block (eg. medium editor in blocks-text)
            // Note the last 'useCapture' argument means we want to use 'event capturing' instead of
            // standard jquery event bubbling.
            // See https://developer.mozilla.org/en-US/docs/Web/API/EventTarget/addEventListener
            if (!enableClickEvents) {
                // we don't want to leave this on permanently, because it blocks
                // literally everthing (eg. all sidebar controls too). So let's fire it
                // and reset it after a short amount of time, when we're sure the click
                // event should have happened.
                window.addEventListener('click', _blockClick, true);
                setTimeout(function ()
                {
                    window.removeEventListener('click', _blockClick, true);
                }, 100);
            }

            //note that we use the mousedown event as the parent event,
            // it're more intuitive when sending out a 'click' event
            Broadcaster.send(Broadcaster.EVENTS.MOUSE.CLICK, event, {
                //the surface we clicked on
                surface: mousedownSurface,
                //the low-level DOM element we clicked on
                element: clickedElement,
                //the original mouse down event
                originalEvent: mousedownEvent,
            });
        }
        else if (draggingStatus === Mouse.DRAGGING.YES) {
            Broadcaster.send(Broadcaster.EVENTS.MOUSE.DRAG.STOP, event, {
                //this is the surface we dragged around
                surface: mousedownSurface,
                //the low-level DOM element we ended our drag on
                element: clickedElement,
                //the original mouse down event
                originalEvent: mousedownEvent,
                //the last surface we were hovering on
                hoveredSurface: hoveredSurface,
            });
        }

        //we always reset the mouse when done
        _resetMouse();
    };

    /**
     * Cancels and resets an active mouse session
     *
     * @param event
     */
    var _mouseCancel = function (event)
    {
        if (draggingStatus === Mouse.DRAGGING.YES) {
            // Note that the eventData object is basically the same as the drag.stop one
            // because they'll end up in the same handler in the manager
            Broadcaster.send(Broadcaster.EVENTS.MOUSE.DRAG.ABORT, event, {
                //this is the surface we dragged around (possibly undefined)
                surface: mousedownSurface,
                //the low-level DOM element we ended our drag on
                element: clickedElement,
                //the original mouse down event
                originalEvent: mousedownEvent,
                //the last surface we were hovering on
                hoveredSurface: hoveredSurface,
            });
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
        //don't reset the status of dnd is disabled, we need to do that explicitly
        if (draggingStatus !== Mouse.DRAGGING.DISABLED) {
            draggingStatus = Mouse.DRAGGING.NO;
        }
        mousedownSurface = null;
        clickedElement = null;
        hoveredSurface = null;
        mousedownEvent = null;
        resetStats();
        dragVector = {};

        //since we're only listening for move events after clicking now, deregister this by default
        $(document).off("mousemove.blocks_core");

        //this will make sure the overlays always regain their events on reset
        UI.overlayWrapper.removeClass(BlocksConstants.OVERLAY_NO_EVENTS_CLASS);

        //re-enable the text selection that was disabled during drag
        Mouse.enableNativeDnd(true);
    };

    /**
     * Blocks all click events on everything
     */
    var _blockClick = function(event)
    {
        event.stopImmediatePropagation();
        event.stopPropagation();
    };

    /*
     * Gives the current direction of the mouse in degrees
     * */
    var updateVector = function (event)
    {
        //we're updating the vector on every mouse move,
        //so it makes sense to always sync the start point of the vector
        //to the current location of the mouse
        dragVector.x1 = event.pageX;
        dragVector.y1 = event.pageY;

        // Note: it makes sense to only update the target direction of the vector if the variance of
        // the angles is below a certain threshold, because only then we are 'really moving' in a certain
        // direction. A high variance means there are too many angles pointing in different directions.
        // Variances in the [0.5 - 1.0] range typically mean we abruptly changed direction, making the dragvector
        // do a 180 degree turn, bouncing the intersecting edge around. You may want to hide this.
        // Note though that during initial movement (just after dragging started), after pausing for longer than
        // MAX_TIMEDIFF_MILLIS (and restarting) or while moving very slowly (typically speeds < 5 or < 10),
        // the variance will also by high, so it's a bit of a tradeoff, hence the ignore flag...
        if (Commons.isUnset(dragVector.x2) || IGNORE_VARIANCE || stats.variance < VARIANCE_THRESHOLD) {

            // by using a large multiplier (larger than the largest possible page), we're sure the resulting
            // line will extend the page borders and thus intersecting will all possible block edges
            // on that page (see later)
            var x2 = dragVector.x1 - (Math.cos(stats.direction) * DIRECTION_MULTIPLIER);
            var y2 = dragVector.y1 - (Math.sin(stats.direction) * DIRECTION_MULTIPLIER);

            //initialize the 2nd coordinate if it doesn't exist
            dragVector.x2 = dragVector.x2 ? dragVector.x2 : 0;
            dragVector.y2 = dragVector.y2 ? dragVector.y2 : 0;

            //note: averaging out the new value eases the signal down a lot
            // dragVector.x2 = (dragVector.x2 + x2) / 2;
            // dragVector.y2 = (dragVector.y2 + y2) / 2;

            dragVector.x2 = x2;
            dragVector.y2 = y2;
        }

        //always show the debug line, even if the dragVector isn't recalculated
        if (SHOW_DEBUG_LINES) {

            //draws the absolute current direction, including speed or not
            if (!debugCanvas) {
                debugCanvas = $('<canvas style="position: absolute; top: 0; left: 0; pointer-events: none;" width="' + UI.body.width() + '" height="' + UI.body.height() + '" />').appendTo(UI.body);
            }

            var ctx = debugCanvas[0].getContext("2d");

            //start over
            ctx.clearRect(0, 0, debugCanvas[0].width, debugCanvas[0].height);

            //draw the resulting direction vector in green
            ctx.beginPath();
            ctx.moveTo(dragVector.x1, dragVector.y1);
            ctx.lineTo(dragVector.x2, dragVector.y2);
            ctx.lineWidth = 1;
            ctx.strokeStyle = '#00ff00';
            ctx.stroke();

            //draw the raw speed vector on top of the direction vector in blue
            ctx.beginPath();
            ctx.moveTo(dragVector.x1, dragVector.y1);
            ctx.lineTo(dragVector.x1 - (Math.cos(stats.direction) * stats.speed),
                dragVector.y1 - (Math.sin(stats.direction) * stats.speed));
            ctx.lineWidth = 1;
            ctx.strokeStyle = '#0000ff';
            ctx.stroke();
        }
    };

    var resetStats = function ()
    {
        stats.idx = 0;
        stats.events = [];
        stats.sinSum = 0;
        stats.cosSum = 0;
        stats.totalLength = 0;
        stats.totalLengthAbs = 0;
        stats.totalTimeDiff = 0;
        stats.totalTimeDiffAbs = 0;
        stats.variance = 0;
        stats.direction = 0;
        stats.speed = 0;
    };

    var updateStats = function (event)
    {
        var newStat = {

            //Note: these are milliseconds
            time: new Date().getTime(),

            x: event.pageX,
            y: event.pageY,

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

            // calculate the index of the previous entry in the window
            // note: stats.idx is the index where we will place the new stat
            var prevIdx = stats.idx > 0 ? stats.idx - 1 : stats.events.length - 1;

            // clean up the old entries (if the mouse stands still, it feels more
            // natural to 'forget' what we've done before after a certain time)
            if (MAX_TIMEDIFF_MILLIS > 0) {

                //note: if prevIdx equals stats.idx, we deleted all others
                while (prevIdx !== stats.idx && stats.events[prevIdx] && newStat.time - stats.events[prevIdx].time > MAX_TIMEDIFF_MILLIS) {

                    var p = stats.events[prevIdx];
                    stats.sinSum -= p.sin;
                    stats.cosSum -= p.cos;
                    stats.totalLength -= p.length;
                    stats.totalTimeDiff -= p.timeDiff;

                    //remove the element from the window and move down the current
                    //index if the index is before the current index
                    stats.events.splice(prevIdx, 1);
                    if (prevIdx < stats.idx) {
                        stats.idx--;
                    }

                    prevIdx = prevIdx > 0 ? prevIdx - 1 : stats.events.length - 1;
                }
            }

            //make sure we haven't cleaned up the full array in the previous step
            var prevStat = stats.events[prevIdx];
            if (prevIdx !== stats.idx && prevStat) {

                newStat.timeDiff = newStat.time - prevStat.time;

                var xDiff = prevStat.x - newStat.x;
                var yDiff = prevStat.y - newStat.y;
                newStat.length = Math.sqrt(xDiff * xDiff + yDiff * yDiff);

                var angleWithPrev = Math.atan2(yDiff, xDiff);
                newStat.sin = Math.sin(angleWithPrev);
                newStat.cos = Math.cos(angleWithPrev);

                //calculate the index of the oldest entry in the window
                var oldestIdx = stats.events.length < WINDOW_SIZE ? 0 : ((stats.idx + 1) % WINDOW_SIZE);
                var oldestStat = stats.events[oldestIdx];

                // Instead of re-summing all the the entries, we just calc a moving sum
                // by substracting the last and adding the new value
                stats.sinSum = stats.sinSum - oldestStat.sin + newStat.sin;
                stats.cosSum = stats.cosSum - oldestStat.cos + newStat.cos;
                stats.totalLength = stats.totalLength - oldestStat.length + newStat.length;
                stats.totalLengthAbs = stats.totalLengthAbs + newStat.length;
                stats.totalTimeDiff = stats.totalTimeDiff - oldestStat.timeDiff + newStat.timeDiff;
                stats.totalTimeDiffAbs = stats.totalTimeDiffAbs + newStat.timeDiff;
            }
            else {
                //if the cleanup for some reason resulted in a funky invalid prevStat,
                //we make sure the index numbering is reset
                stats.idx = 0;
            }
        }

        //store the statistic and move to the next
        stats.events[stats.idx] = newStat;
        //advance the index for the next stat
        stats.idx = (stats.idx + 1) % WINDOW_SIZE;

        //calculate the mean and the variance of the all the angles in the window;
        // see https://en.wikipedia.org/wiki/Mean_of_circular_quantities
        //     https://en.wikipedia.org/wiki/Directional_statistics
        stats.direction = Math.atan2(stats.sinSum / stats.events.length, stats.cosSum / stats.events.length);
        stats.variance = 1.0 - Math.sqrt(stats.sinSum * stats.sinSum + stats.cosSum * stats.cosSum) / stats.events.length;
        //note: the resulting speed will be expressed as pixels per second (avoiding division by zero)
        stats.speed = stats.totalTimeDiff === 0 ? 0 : stats.totalLength / (stats.totalTimeDiff / 1000);
    };

    /**
     * Debouncing function to make eventing more performant
     * by amortizing quick successions together.
     * See http://unscriptable.com/index.php/2009/03/20/debouncing-javascript-methods/
     * @param func
     * @param threshold
     * @param execAsap
     * @returns {debounced}
     */
    var debounce = function (func, threshold, execAsap)
    {
        var timeout;

        return function debounced()
        {
            var obj = this, args = arguments;

            function delayed()
            {
                if (!execAsap) {
                    func.apply(obj, args);
                }
                timeout = null;
            }

            if (timeout) {
                clearTimeout(timeout);
            }
            else if (execAsap) {
                func.apply(obj, args);
            }

            timeout = setTimeout(delayed, threshold || 50);
        };
    };

}]);

