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
 * Created by wouter on 15/06/15.
 */
base.plugin("blocks.core.Sidebar", ["blocks.core.Layouter", "blocks.media.Finder", "blocks.core.Notification", "base.core.Commons", "blocks.imports.Widget", "constants.blocks.core", "messages.blocks.core", function (Layouter, Finder, Notification, Commons, Widget, BlocksConstants, BlocksMessages)
{
    var Sidebar = this;

    //-----CONSTANTS-----

    //-----VARIABLES-----
    //this will map IDs to config panels
    var configPanelMap = {};

    //this will hold data structures for the currently showing config panels
    var activePanels = [];

    //-----PUBLIC METHODS-----
    /**
     * Reset and initialize the sidebar's config panels for the supplied (focused) surface.
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

        //keep track of the current surface and element (inside the surface.element)
        var currSurface = focusedSurface;
        var currElement = clickedElement;
        var firstColumn = null;

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

                    widget.focus(currSurface, currElement, null, mousedownEvent);
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
        }
    };

    /**
     * Resets the sidebar to a neutral state
     */
    this.reset = function ()
    {
        this.unloadFinder();

        // clean shutdown: call blur on all active surfaces
        for (var i = 0; i < activePanels.length; i++) {
            var e = activePanels[i];
            if (e.widget) {
                e.widget.blur(e.surface, e.element);
            }
        }

        //reset variables
        configPanelMap = {};
        activePanels = [];

        //reset the sidebar and prepare for adding
        var sidebar = $("." + BlocksConstants.PAGE_SIDEBAR_CLASS);
        sidebar.removeClass(BlocksConstants.OPACITY_CLASS);
        sidebar.addClass(BlocksConstants.PREVENT_BLUR_CLASS);

        var sidebarContext = $("#" + BlocksConstants.SIDEBAR_CONTEXT_ID);
        sidebarContext.empty();
    };

    /**
     * Loads the finder into the sidebar, passing the options to the finder init method
     */
    //TODO factor this away because the finder is no dependency of this project
    this.loadFinder = function (options)
    {
        //general test if we have the media plugin available
        var MediaConstants = base.getPlugin("constants.blocks.media.core");
        if (MediaConstants) {
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

            //TODO maybe not necessary to reload this every time, but it allows us to always present a fresh uptodate view of the server content
            var finder = frame.find(".panel-body");
            finder.load(MediaConstants.FINDER_INLINE_ENDPOINT, function (response, status, xhr)
            {
                if (status == "error") {
                    var msg = "Error while loading the finder; ";
                    Notification.error(msg + xhr.status + " " + xhr.statusText, xhr);
                    finder.removeClass(BlocksConstants.LOADING_CLASS);
                }
                else {
                    Finder.init(options);
                    //don't show the warning when clicking something in the finder
                    finder.attr(BlocksConstants.CLICK_ROLE_ATTR, BlocksConstants.FORCE_CLICK_ATTR_VALUE);
                    finder.removeClass(BlocksConstants.LOADING_CLASS);
                }
            });
        }
    };

    /**
     * If the finder tab is active in the sidebar, unload it.
     */
    this.unloadFinder = function ()
    {
        var MediaConstants = base.getPlugin("constants.blocks.media.core");
        if (MediaConstants) {
            //'switch' back to the context tab
            $("#" + BlocksConstants.SIDEBAR_CONTEXT_TAB_ID).tab('show');

            var finderTab = $("#" + BlocksConstants.SIDEBAR_FILES_ID);
            if (!finderTab.is(':empty')) {
                var contextTab = $("#" + BlocksConstants.SIDEBAR_CONTEXT_ID);
                contextTab.removeClass(BlocksConstants.LOADING_CLASS);
                finderTab.addClass(BlocksConstants.LOADING_CLASS);
                finderTab.html('');
            }
        }
    };

    /**
     * Sets the width of the sidebar to the specified value is pixels, animated.
     */
    this.setWidth = function (width, callback)
    {
        var sidebarElement = $("." + BlocksConstants.PAGE_SIDEBAR_CLASS);
        sidebarElement.addClass(BlocksConstants.SIDEBAR_ANIMATED_CLASS);
        sidebarElement.css("width", (width) + "px");
        //one() = on() but only once
        sidebarElement.one('webkitTransitionEnd otransitionend oTransitionEnd msTransitionEnd transitionend', function (event)
        {
            if ($(event.target).hasClass(BlocksConstants.PAGE_SIDEBAR_CLASS)) {
                sidebarElement.removeClass(BlocksConstants.SIDEBAR_ANIMATED_CLASS);
                $("." + BlocksConstants.PAGE_CONTENT_CLASS).css("width", ($(window).width() - width) + "px");

                if (callback) {
                    callback(event);
                }
            }
        });
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

}
]);