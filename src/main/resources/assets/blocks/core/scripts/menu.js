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

base.plugin("blocks.core.Menu", ["constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.Notification", "blocks.core.Sidebar", "blocks.core.UI", function (BlocksConstants, BlocksMessages, Broadcaster, Notification, Sidebar, UI)
{
    var Menu = this;

    //-----CONSTANTS-----

    // note that because we set a container width on the blocks-layout in some styles
    // (eg. sticky footers and full background-colors),
    // we need to scale it along with the container inside it
    var CONTAINERS_SELECTOR = ".container, blocks-layout";

    //-----VARIABLES-----

    /*
     * In bootstrap the container width is fixed. to prevent the container from bleeding
     * into our sidebar, we set the width fixed with a new width, smaller than our page content wrapper.
     * Sync with method below.
     */
    var updateContainerWidth = function ()
    {
        var wrapper = $("." + BlocksConstants.PAGE_CONTENT_CLASS);
        var containers = $(CONTAINERS_SELECTOR);

        //TODO this is dangerous to blindly do this
        containers.removeAttr("style");

        if (wrapper.length > 0) {
            var wrapperWidth = wrapper.outerWidth();
            var containerWidth = containers.outerWidth();
            if (containerWidth > wrapperWidth) {
                //let's keep a small margin between the website and our sidebar
                containers.css("width", (wrapperWidth - BlocksConstants.SIDEBAR_MARGIN_LEFT_PX) + "px");
            }
        }
    };
    //method to clear the manual container width from above; sync them
    var clearContainerWidth = function ()
    {
        $(CONTAINERS_SELECTOR).css("width", "");
    };

    // On window resize (smart means the events are debounced, see dom.js)
    var sidebarWidth = UI.sidebar.outerWidth();
    var resizing = false;
    $(window).smartresize(function (event)
    {
        //boots the resizing
        if (!resizing) {
            sidebarWidth = UI.sidebar.outerWidth();
            //Hover.removeHoverOverlays();
            Broadcaster.send(Broadcaster.EVENTS.PAUSE_BLOCKS, event);
            resizing = true;
        }
        else {
            var windowWidth = $(window).width();
            $("." + BlocksConstants.PAGE_CONTENT_CLASS).css("width", (windowWidth - sidebarWidth) + "px");
            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
            Broadcaster.send(Broadcaster.EVENTS.RESUME_BLOCKS, event);
            resizing = false;
        }
    });

    //before updating the layout, make sure the container width is set properly
    $(document).on(Broadcaster.EVENTS.WILL_REFRESH_LAYOUT, function (event)
    {
        // check size page content
        // find containers and get width
        // if container width is greater then page content width
        // set container width to pagecontent width - 20
        updateContainerWidth();
    });

    $(document).on(Broadcaster.EVENTS.DOM_CHANGED, function (event)
    {
        var wrapper = $("." + BlocksConstants.PAGE_CONTENT_CLASS);

        //it's possible the content of the sidebar made it grow/shrink;
        //this will alter the content wrapper class if it did (and fire a re-layout)
        var contentWidth = $(window).width() - UI.sidebar.outerWidth();
        if (wrapper.outerWidth() != contentWidth) {
            wrapper.css("width", contentWidth + "px");
            Broadcaster.send(Broadcaster.EVENTS.DO_REFRESH_LAYOUT, event);
        }
    });

}]);