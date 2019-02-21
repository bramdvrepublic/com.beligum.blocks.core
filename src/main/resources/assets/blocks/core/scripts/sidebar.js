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
 * Plugin that centralizes all functionality of the folding page sidebar.
 */
base.plugin("blocks.core.Sidebar", ["base.core.Commons", "constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.Notification", "blocks.core.UI", "blocks.imports.Widget", function (Commons, BlocksConstants, BlocksMessages, Broadcaster, Notification, UI, Widget)
{
    var Sidebar = this;

    //-----CONSTANTS-----
    var SIDEBAR_STATE_NULL = "";
    var SIDEBAR_STATE_SHOW = BlocksConstants.PAGE_SIDEBAR_COOKIE_SHOW;
    var SIDEBAR_STATE_HIDE = BlocksConstants.PAGE_SIDEBAR_COOKIE_HIDE;
    //Note: an empty paths means: take the path of the current page
    var DEFAULT_COOKIE_OPTIONS = BlocksConstants.PAGE_SIDEBAR_COOKIE_OPTIONS;

    //the sidebar can't grow smaller than this
    var MIN_SIDEBAR_WIDTH = 200;

    //-----VARIABLES-----
    //this will map IDs to config panels
    var configPanelMap = {};

    //this will hold data structures for the currently showing config panels
    var activePanels = [];

    //-----PUBLIC METHODS-----
    /**
     * Load the server html and create the sidebar element (but don't add it to the DOM yet)
     */
    this.load = function ()
    {
        UI.sidebar = $("<div class='" + BlocksConstants.PAGE_SIDEBAR_CLASS + "'></div>");
        UI.sidebar.load(BlocksConstants.SIDEBAR_ENDPOINT, function (response, status, xhr)
        {
            if (status == 'success') {

                // When everything is preloaded correctly,
                // notify the user we're ready by creating a start button,
                // Note: the icon is set in blocks.less
                UI.startButton = $('<a class="' + BlocksConstants.BLOCKS_START_BUTTON + '"></a>')
                    .attr(BlocksConstants.CLICK_ROLE_ATTR, BlocksConstants.FORCE_CLICK_ATTR_VALUE)
                    // Hide/show sidebar when menu button is clicked
                    .on("click", function (event)
                    {
                        // Since we reuse this button as the close button, we need to detect the state,
                        // that way we can leave the event handler attached.
                        // No page wrapper means we need to boot the system, otherwise stop it
                        if (Commons.isUnset(UI.pageContent)) {
                            Broadcaster.send(Broadcaster.EVENTS.BLOCKS.START, event);
                        }
                        else {
                            Broadcaster.send(Broadcaster.EVENTS.BLOCKS.STOP, event);
                        }
                    })
                    .appendTo(UI.body);

                //check for a cookie and auto-open when the sidebar was active
                if (Cookies.get(BlocksConstants.COOKIE_SIDEBAR_STATE) === SIDEBAR_STATE_SHOW) {
                    $(document).ready(function ()
                    {
                        //when all sidebar and DOM initialization is done, we can start the blocks system
                        Broadcaster.send(Broadcaster.EVENTS.BLOCKS.START, event);
                    });
                }
            }
            else {
                Notification.error(msg + xhr.status + " " + xhr.statusText, xhr);
            }
        });
    };

    /**
     * Open or close the sidebar panel, (un)loading all required code to boot it's content.
     *
     * @param show
     */
    this.toggle = function (show, callback)
    {
        if (show) {

            // Remove the menu button while animating sidebar
            // Note that we remove it because we'll call body.empty() below
            UI.startButton.detach();

            //set up perfect-scrollbar.js
            if (jQuery().perfectScrollbar) {
                //only scroll from the tab content so the header doesn't scroll away
                UI.sidebar.find('.' + BlocksConstants.SIDEBAR_CONTAINER_CLASS).perfectScrollbar();
            }

            //transform the button to a closing cross
            //slide open the sidebar and activate the callback when finished
            Sidebar.setWidth(getInitialWidth(), function (event)
            {
                //re-add the button (but with a changed icon)
                UI.startButton.addClass("open").appendTo(UI.body);

                //Note: by default, the cookie is deleted when the browser is closed, override that.
                Cookies.set(BlocksConstants.COOKIE_SIDEBAR_STATE, SIDEBAR_STATE_SHOW, DEFAULT_COOKIE_OPTIONS);

                //allow this sidebar to be resized
                enableResizing(true);

                if (callback) {
                    callback();
                }
            });

        }
        //hide the sidebar
        else {

            //make sure all focused blocks are blurred in a clean manner
            this.reset();

            //hide the button while animating
            UI.startButton.detach();

            Sidebar.setWidth(0.0, function (event)
            {
                //Note: by default, the cookie is deleted when the browser is closed, override that.
                Cookies.set(BlocksConstants.COOKIE_SIDEBAR_STATE, SIDEBAR_STATE_HIDE, DEFAULT_COOKIE_OPTIONS);

                //don't allow the sidebar to be resized
                enableResizing(false);

                //re-add the button (but with a changed icon)
                UI.startButton.removeClass("open").appendTo(UI.body);

                if (callback) {
                    callback();
                }
            });
        }
    };

    /**
     * Reset and initialize the sidebar's config panels for the supplied (focused) surface.
     *
     * @param focusedSurface The currently focused surface
     * @param clickedElement The specific element (inside the surface) we clicked on
     * @param mousedownEvent The original mousedown event that cause the focus switch
     */
    this.init = function (focusedSurface, clickedElement, mousedownEvent)
    {
        this.reset();

        // First, we build a data structure that allows for easy iteration:
        // Starting at the clicked element, we 'go up' and search for registered widget selectors.
        // Once we reach the focused surface (note that this means the clicked element should always
        // be inside the element of the focused surface), we 'go up' faster by taking the parent of the
        // surface (instead of iterating each DOM element) until we hit the page surface.
        // This is done in reversed order so we have the page at the top.
        // Note that we select the column closest around the block we're focusing,
        // because it's what we naturally expect in the GUI.
        // Similarly, we select the row furthest from the focused block; the last row before we switch to the page.

        // if we click on a block, but we actually clicked on the free room around that block
        // (the 'stretched' space of a block to make it align with it's parent row), we'll click
        // on the column-element instead of the block-element and we need to fix this because the
        // clicked element will be a level 'too high' and out of sync with the surface we want to focus
        if (clickedElement.closest(focusedSurface.element).length === 0) {
            // Additional tweaking:
            // when we click in the margin of a block and that block
            // has only one property, we'll simulate the click on that property
            // instead. This is needed eg. for blocks-text that registers itself
            // on the first property inside the block, not on the block itself.
            if (focusedSurface.children.length === 1) {
                clickedElement = focusedSurface.children[0].element;
            }
            else {
                clickedElement = focusedSurface.element;
            }
        }

        //keep track of the current surface and element (inside the surface.element)
        var currSurface = focusedSurface;
        var currElement = clickedElement;
        var firstColumn = null;

        var runawayCounter = 0;
        while (currSurface != null) {

            //note that we don't let properties be surfaces for config widgets; we start at block-level
            var validSurface = currSurface.isPage()
                || (currSurface.isRow() && currSurface.parent.isContainer())
                || (currSurface.isColumn() && !firstColumn && (firstColumn = currSurface))
                || (currSurface.isBlock());

            if (validSurface) {

                //check if we have a widget registered for the current element
                var widget = Widget.Class.create(currElement);

                if (widget) {

                    //save it for blur()
                    activePanels.push({
                        widget: widget,
                        surface: currSurface,
                        element: currElement,
                        hotspot: clickedElement,
                    });

                    //we'll iterate the array in reverse order, but when focusing a block,
                    //we don't want users to be able to save the page (it causes all kinds of problems),
                    //so we disable the 'page-entry' if a block is focused
                    var disabled = currSurface.isPage() && !focusedSurface.isPage();

                    //we'll expand all panels by default, except the page, row and column (but the page will be disabled)
                    var collapsed = currSurface.isRow() || currSurface.isColumn() || disabled;

                    //if we can find a specialized title, use it, otherwise just use the name of the surface
                    var panelTitle = widget.getWindowName() ? widget.getWindowName().toLowerCase() : currSurface.name;

                    var panelID = createConfigPanel(panelTitle, collapsed, disabled);

                    widget.focus(currSurface, currElement, clickedElement, mousedownEvent);

                    var optionsToAdd = widget.getConfigs(currSurface, currElement);
                    if (optionsToAdd && optionsToAdd.length > 0) {

                        //since we have a 'weight' setting, make sure we sort the array first
                        optionsToAdd.sort(configOptionsSorter);

                        for (var w = 0; w < optionsToAdd.length; w++) {
                            appendToConfigPanel(panelID, optionsToAdd[w]);
                        }

                        //we use prepend because we're reversing the order
                        appendConfigPanelToSidebar(panelID, BlocksConstants.SIDEBAR_CONTEXT_ID, true);
                    }
                }
            }

            // we 'go up' element by element until we reach the element of the surface,
            // then we go up surface by surface
            if (currElement.is(currSurface.element)) {
                currSurface = currSurface.parent;
                currElement = currSurface ? currSurface.element : null;
            }
            else {
                //otherwise, we leave the surface be and iterate the elements first
                //(this should only happen at the block-level)
                currElement = currElement.parent();
            }

            if (++runawayCounter > 1000) {
                Logger.error('Infinite loop detected, breaking forcefully. This shouldn\'t happen.');
                break;
            }
        }
    };

    /**
     * Resets the sidebar to a neutral state
     */
    this.reset = function ()
    {
        this.unloadFinder();

        // clean shutdown: call blur on all active widgets
        for (var i = 0; i < activePanels.length; i++) {
            var e = activePanels[i];
            if (e.widget) {
                e.widget.blur(e.surface, e.element, e.hotspot);
            }
        }

        //reset variables
        configPanelMap = {};
        activePanels = [];

        var sidebarContext = $("#" + BlocksConstants.SIDEBAR_CONTEXT_ID);
        sidebarContext.empty();
    };

    /**
     * Loads the finder into the sidebar, passing the options to the finder init method
     */
    this.loadFinder = function (options)
    {
        var contextTab = $("#" + BlocksConstants.SIDEBAR_CONTEXT_ID);
        var finderTab = $("#" + BlocksConstants.SIDEBAR_FILES_ID);
        contextTab.addClass(BlocksConstants.LOADING_CLASS);
        finderTab.removeClass(BlocksConstants.LOADING_CLASS);
        //we'll start off with an empty container and let createConfigPanel() fill it
        finderTab.empty();

        //'switch' to the finder tab
        $("#" + BlocksConstants.SIDEBAR_FILES_TAB_ID).tab('show');

        //now create and add a new frame
        var panelId = createConfigPanel(BlocksMessages.finderTabTitle);
        appendConfigPanelToSidebar(panelId, BlocksConstants.SIDEBAR_FILES_ID);
        //let's us do perform some css tweaks
        var frame = getConfigPanelForId(panelId);
        if (frame) {
            frame.addClass(BlocksConstants.SIDEBAR_FINDER_PANEL_CLASS);
        }
        else {
            Logger.error('Couldn\'t find a config panel with this id', panelId);
        }

        // Using a broadcast event, we cut the ties with the blocks.media project
        // Note: maybe it's not necessary to reload this every time,
        // but it allows us to always present a fresh up-to-date view of the server content
        var container = frame.find(".panel-body");
        Broadcaster.send(Broadcaster.EVENTS.FINDER.LOAD, null, {
            container: container,
            options: options,
            callback: function (success)
            {
                if (success) {
                    //don't show the warning when clicking something in the finder
                    container.attr(BlocksConstants.CLICK_ROLE_ATTR, BlocksConstants.FORCE_CLICK_ATTR_VALUE);
                    container.removeClass(BlocksConstants.LOADING_CLASS);
                }
                else {
                    Notification.error('Error while loading the finder; ' + xhr.status + " " + xhr.statusText, xhr);
                    container.removeClass(BlocksConstants.LOADING_CLASS);
                }
            }
        });
    };

    /**
     * If the finder tab is active in the sidebar, unload it.
     */
    this.unloadFinder = function ()
    {
        //'switch' back to the context tab
        $("#" + BlocksConstants.SIDEBAR_CONTEXT_TAB_ID).tab('show');

        var finderTab = $("#" + BlocksConstants.SIDEBAR_FILES_ID);
        if (!finderTab.is(':empty')) {
            var contextTab = $("#" + BlocksConstants.SIDEBAR_CONTEXT_ID);
            contextTab.removeClass(BlocksConstants.LOADING_CLASS);
            finderTab.addClass(BlocksConstants.LOADING_CLASS);
            finderTab.html('');

            //auto-close the sidebar to the previous width
            Sidebar.setWidth(getInitialWidth());
        }
    };

    /**
     * Sets the width of the sidebar to the specified value is pixels, animated.
     */
    this.setWidth = function (width, callback, skipAnimation)
    {
        //note: closing the sidebar is setting the width to zero, so allow it too
        if (width === 0 || width > MIN_SIDEBAR_WIDTH) {

            if (!skipAnimation) {

                // there is no such thing as a transition callback, we need to simulate it
                // by augmenting the speed at which we refresh the page
                Broadcaster.send(Broadcaster.EVENTS.PAGE.REFRESH_SPEED, null, {speed: 40});

                UI.sidebar.addClass(BlocksConstants.SIDEBAR_ANIMATED_CLASS);
            }

            //this will trigger the animation if the class above is set
            UI.sidebar.css("width", width + "px");

            if (!skipAnimation) {

                //one() = on() but only once
                UI.sidebar.one('webkitTransitionEnd otransitionend oTransitionEnd msTransitionEnd transitionend', function (event)
                {
                    //set the refresh rate back to normal
                    Broadcaster.send(Broadcaster.EVENTS.PAGE.REFRESH_SPEED, null, {speed: -1});

                    //don't really remember why this was needed
                    if ($(event.target).is(UI.sidebar)) {

                        UI.sidebar.removeClass(BlocksConstants.SIDEBAR_ANIMATED_CLASS);

                        // the refreshing of the overlays happens in the dimension monitor,
                        // but calling it explicitly improves the user experience while dragging
                        Broadcaster.send(Broadcaster.EVENTS.PAGE.REFRESH, event);

                        if (callback) {
                            callback(event);
                        }
                    }
                });
            }
            else {
                // the refreshing of the overlays happens in the dimension monitor,
                // but calling it explicitly improves the user experience while dragging
                Broadcaster.send(Broadcaster.EVENTS.PAGE.REFRESH);

                if (callback) {
                    callback();
                }
            }
        }
    };

    //-----PRIVATE METHODS-----
    var appendToConfigPanel = function (panelId, html)
    {
        var configPanel = getConfigPanelForId(panelId);
        if (configPanel) {

            //these are the defaults
            var htmlConfig = {};
            htmlConfig[BlocksConstants.SIDEBAR_CONFIG_ADVANCED_KEY] = false;

            //copy in the passed configs, overwriting the default
            if (html instanceof jQuery) {
                var c = html.data(BlocksConstants.SIDEBAR_CONFIG_KEY);
                if (c) {
                    $.extend(htmlConfig, c);
                }
            }

            if (htmlConfig[BlocksConstants.SIDEBAR_CONFIG_ADVANCED_KEY]) {
                //the body of the advanced panel is collapsable, so make sure we add them to a sub-container
                configPanel.find(".panel-body ." + BlocksConstants.PANEL_BODY_ADVANCED_CLASS + ' .collapse > div').append(html);
                //the advanced panel is hidden when it has no members
                configPanel.find(".panel-body ." + BlocksConstants.PANEL_BODY_ADVANCED_CLASS).show();
            }
            else {
                configPanel.find('.panel-body .' + BlocksConstants.PANEL_BODY_SIMPLE_CLASS).append(html);
            }
        }
        else {
            Logger.error('Couldn\'t find a config panel with this id', panelId);
        }
    };

    /**
     * Lookup the config panel for the supplied id
     */
    var getConfigPanelForId = function (id)
    {
        return configPanelMap[id];
    };

    /**
     * Creates a new, uniform config panel
     *
     * @param title
     * @param collapsed
     * @param disabled
     * @returns {*}
     */
    var createConfigPanel = function (title, collapsed, disabled)
    {
        if (configPanelMap == null) {
            configPanelMap = {};
        }

        var panelId = Commons.generateId();
        if (configPanelMap[panelId] == null) {

            var panelId = panelId + '-panel';
            var bodyId = panelId + '-panel-body';
            var div = $('<div id="' + panelId + '" class="panel panel-default' + (disabled ? ' disabled' : '') + '"/>');
            var header = $('<div class="panel-heading collapser' + (collapsed ? ' collapsed' : '') + '" data-toggle="collapse" data-target="#' + bodyId + '" aria-expanded="' + (collapsed ? 'false' : 'true') + '" aria-controls="' + bodyId + '">' + title + '</div>').appendTo(div);

            if (!disabled) {
                // note: the "in" makes it start unfolded
                var collapse = $('<div id="' + bodyId + '" class="collapse' + (collapsed ? '' : ' in') + '">').appendTo(div);
                // creating the extra wrapper above seems to speed up/smoothen the animation a lot
                var body = $('<div class="panel-body"/>').appendTo(collapse);
                //these container will split up the elements added to the sidebar and fold the advanced ones
                var bodySimple = $('<div class="' + BlocksConstants.PANEL_BODY_SIMPLE_CLASS + '"/>').appendTo(body);
                //note: the advanced container contains a control and a collapser
                var bodyAdvanced = $('<div class="' + BlocksConstants.PANEL_BODY_ADVANCED_CLASS + '"/>').appendTo(body);
                var bodyAdvancedBodyId = panelId + '-advanced';
                var bodyAdvancedCollapsed = true;
                var bodyAdvancedHeader = $('<div class="collapser' + (bodyAdvancedCollapsed ? ' collapsed' : '') + '" data-toggle="collapse" data-target="#' + bodyAdvancedBodyId + '" aria-expanded="' + (bodyAdvancedCollapsed ? 'false' : 'true') + '" aria-controls="' + bodyAdvancedBodyId + '">' + BlocksMessages.sidebarPanelAdvancedTitle + '</div>').appendTo(bodyAdvanced);
                var bodyAdvancedBodyWrapper = $('<div id="' + bodyAdvancedBodyId + '" class="collapse' + (bodyAdvancedCollapsed ? '' : ' in') + '">').appendTo(bodyAdvanced);
                //same remark as above: better to have a wrapper around the content
                var bodyAdvancedBody = $('<div>').appendTo(bodyAdvancedBodyWrapper);
            }

            configPanelMap[panelId] = div;

            //note: real adding is done manually in appendConfigPanelToSidebar()
        }

        return panelId
    };

    /**
     * Sorts two config options based on their assigned weight value
     * where higher weights will bubble to the top config panel
     */
    var configOptionsSorter = function (a, b)
    {
        // 1: a is greater than b
        //-1: a is less than b
        // 0: a must be equal to b

        var aConfig = a.data(BlocksConstants.SIDEBAR_CONFIG_KEY);
        var bConfig = b.data(BlocksConstants.SIDEBAR_CONFIG_KEY);

        if (typeof aConfig == 'undefined' && typeof bConfig == 'undefined') {
            return 0;
        }
        else if (typeof aConfig == 'undefined') {
            return 1;
        }
        else if (typeof bConfig == 'undefined') {
            return -1;
        }
        else {
            var aWeight = aConfig[BlocksConstants.SIDEBAR_CONFIG_WEIGHT_KEY];
            var bWeight = bConfig[BlocksConstants.SIDEBAR_CONFIG_WEIGHT_KEY];

            if (typeof aWeight == 'undefined' && typeof bWeight == 'undefined') {
                return 0;
            }
            else if (typeof aWeight == 'undefined') {
                return 1;
            }
            else if (typeof bWeight == 'undefined') {
                return -1;
            }
            else {
                //this means: higher weights will bubble to the top of the array
                return bWeight - aWeight;
            }
        }
    };

    /**
     * Looks up the config panel with the supplied ID and adds it to the sidebar
     * in the tab with the right ID.
     */
    var appendConfigPanelToSidebar = function (id, tabId, prepend)
    {
        var configPanel = getConfigPanelForId(id);

        if (configPanel) {
            var tab = null;
            if (tabId == BlocksConstants.SIDEBAR_CONTEXT_ID) {
                tab = $("#" + BlocksConstants.SIDEBAR_CONTEXT_ID);
            }
            else if (tabId == BlocksConstants.SIDEBAR_FILES_ID) {
                tab = $("#" + BlocksConstants.SIDEBAR_FILES_ID);
            }

            if (tab) {
                if (prepend) {
                    tab.prepend(configPanel);
                }
                else {
                    tab.append(configPanel);
                }
            }
        }
        else {
            Logger.error('Couldn\'t find a config panel with this id', id);
        }
    };

    /**
     * Enable or disable the events that handle the resizing of the sidebar
     *
     * @param enable
     */
    var enableResizing = function (enable)
    {
        var NAMESPACE = 'sidebar_resize';

        if (enable) {
            $(document).on("mousedown." + NAMESPACE, "." + BlocksConstants.PAGE_SIDEBAR_RESIZE_CLASS, function (event)
            {
                //needed because sometimes we hover out of the dragger while moving the sidebar (because of some lag)
                UI.body.addClass(BlocksConstants.FORCE_RESIZE_CURSOR_CLASS);

                $(document).on("mousemove." + NAMESPACE, function (event)
                {
                    Sidebar.setWidth(UI.window.width() - event.pageX, null, true);
                });

                $(document).on("mouseup." + NAMESPACE, function (event)
                {
                    $(document).off("mousemove." + NAMESPACE);
                    $(document).off("mouseup." + NAMESPACE);

                    UI.body.removeClass(BlocksConstants.FORCE_RESIZE_CURSOR_CLASS);

                    //Note: by default, the cookie is deleted when the browser is closed:
                    Cookies.set(BlocksConstants.COOKIE_SIDEBAR_WIDTH, UI.sidebar.width(), DEFAULT_COOKIE_OPTIONS);
                });
            });
        }
        else {
            //removes all events in this namespace
            $(document).off("." + NAMESPACE);
        }
    };

    /**
     * Query the cookie state and return a good/valid initial width for the sidebar.
     *
     * @returns {number}
     */
    var getInitialWidth = function ()
    {
        // default width of sidebar is 20% of window
        var retVal = $(window).width() * 0.2;

        // Get old sidebar width from cookie
        var cookieSidebarWidth = Cookies.get(BlocksConstants.COOKIE_SIDEBAR_WIDTH);
        //make sure the value is OK and cleanup if not
        if (!Commons.isUnset(cookieSidebarWidth) && $.isNumeric(cookieSidebarWidth)) {
            cookieSidebarWidth = parseInt(cookieSidebarWidth);
        }
        else {
            cookieSidebarWidth = null;
            Cookies.remove(BlocksConstants.COOKIE_SIDEBAR_WIDTH, DEFAULT_COOKIE_OPTIONS);
        }

        if (cookieSidebarWidth != null && cookieSidebarWidth > 0) {
            retVal = cookieSidebarWidth;
        }
        //control the bounds, even if the cookie says otherwise
        if (retVal < MIN_SIDEBAR_WIDTH) {
            retVal = MIN_SIDEBAR_WIDTH;
        }

        return retVal;
    };
}
]);