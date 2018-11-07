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
    var configPanels = {};

    //this will hold data structures for the currently showing config panels
    var activePanels = [];

    //-----PUBLIC METHODS-----
    /**
     * Reset and initialize the sidebar's config panels for the supplied (focused) surface
     */
    this.init = function (focusedSurface, event)//(block, element, hotspot, event)
    {
        this.reset();

        //allows us to select only the last row and column in the tree (on the way up)
        var firstRow = null;
        var lastRow = null;
        var firstColumn = null;
        var lastColumn = null;

        // We'll cycle through the parents until we hit the page, then reverse the order and create panels
        // for all of them, starting with the page
        var currSurface = focusedSurface;
        while (currSurface != null) {

            if (currSurface instanceof blocks.elements.Page) {
                //we select the _first_ column, (instead of the last, see row below) because it's what
                // we naturally expect in the GUI (the column closest around the block we're focusing)
                if (firstColumn != null) {
                    activePanels.push({
                        surface: firstColumn,
                    });
                }
                //if we have a row, push that one first before closing with the page
                if (lastRow != null) {
                    activePanels.push({
                        surface: lastRow
                    });
                }
                activePanels.push({
                    surface: currSurface
                });
            }
            else if (currSurface instanceof blocks.elements.Container) {
                //NOOP
            }
            else if (currSurface instanceof blocks.elements.Row) {
                if (firstRow == null) {
                    firstRow = currSurface;
                }
                lastRow = currSurface;
            }
            else if (currSurface instanceof blocks.elements.Column) {
                if (firstColumn == null) {
                    firstColumn = currSurface;
                }
                lastColumn = currSurface;
            }
            else if (currSurface instanceof blocks.elements.Block) {
                activePanels.push({
                    surface: currSurface
                });
            }
            else if (currSurface instanceof blocks.elements.Property) {
                activePanels.push({
                    surface: currSurface
                });
            }
            else {
                Logger.error("Encountered unimplemented surface type; this shouldn't happen", currSurface);
            }

            currSurface = currSurface.parent;
        }

        var title = null;
        for (var i = activePanels.length - 1; i >= 0; i--) {

            var panel = activePanels[i];

            var surface = panel.surface;
            var element = surface.element;
            // if (surface instanceof blocks.elements.Property) {
            //     element = surface.parent.element;
            // }

            //Widget.create() is a statis factory method that iterates all registered
            //tag selectors and returns the correct instance for the supplied element
            var widget = Widget.Class.create(element);
            //save it for blur()
            if (widget) {
                activePanels[i].widget = widget;
            }

            //don't make panels for (real) properties, only blocks and pages
            var blockTitle = 'TODO: change-this';
            if (widget) {
                //all the rest is already lower case, make it uniform no matter what
                blockTitle = widget.getWindowName() ? widget.getWindowName().toLowerCase() : widget.getWindowName();
            }

            var panelTitle = title;
            if (title == null) {
                panelTitle = blockTitle;
            }
            else {
                panelTitle = title + '<i class="fa fa-fw fa-angle-right"/>' + blockTitle;
            }

            //we'll expand all panels by default, except the row and column
            var collapsed = false;
            if (surface instanceof blocks.elements.Row || surface instanceof blocks.elements.Column) {
                collapsed = true;
            }
            //if we're showing the controls for a block, close the panel
            else if (focusedSurface instanceof blocks.elements.Block && surface instanceof blocks.elements.Page) {
                collapsed = true;
            }

            //we'll iterate the array in reverse order, but when focusing a block,
            //we don't want users to be able to save the page (it causes all kinds of problems),
            //so we disable the 'page-entry' if a block is focused
            var disabled = !(focusedSurface instanceof blocks.elements.Page) && surface instanceof blocks.elements.Page;

            // if a parent stopped the creation of sub-panels, keep executing the focus() method,
            // but without a panel ID (allowing for logic without UI consequences)
            var panelID = createConfigPanel(panelTitle, collapsed, disabled);
            var addedOptions = false;

            if (widget) {
                // the focus method can return a list of UI widgets it needs to add to the panel
                // this way, we have control over that (where we have all the information to decide; eg. what property in which block, etc)
                //TODO refactor the last two (three?) away
                if (surface instanceof blocks.elements.Property) {
                    surface = surface.parent;
                    element = surface.element;
                }
                widget.focus(surface, element, null, event);
                var optionsToAdd = widget.getConfigs(surface, element);
                if (optionsToAdd) {
                    if (addedOptions && optionsToAdd.length > 0) {
                        addUIForProperty(panelID, '<hr>');
                        addedOptions = true;
                    }

                    //since we have a 'weight' setting, make sure we sort the array first
                    optionsToAdd.sort(function (a, b)
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
                    });

                    for (var w = 0; w < optionsToAdd.length; w++) {
                        addUIForProperty(panelID, optionsToAdd[w]);
                        addedOptions = true;
                    }
                }
            }

            //don't add empty panels
            if (addedOptions) {
                appendConfigPanelToSidebar(panelID, BlocksConstants.SIDEBAR_CONTEXT_ID);
                title = panelTitle;
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
                e.widget.blur(e.surface, e.surface.element);
            }
        }

        //reset variables
        configPanels = {};
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
    var addUIForProperty = function (panelId, html)
    {
        var config = getConfigPanelForId(panelId);
        if (config) {

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
                config.find(".panel-body ." + BlocksConstants.PANEL_BODY_ADVANCED_CLASS + ' .collapse > div').append(html);
                //the advanced panel is hidden when it has no members
                config.find(".panel-body ." + BlocksConstants.PANEL_BODY_ADVANCED_CLASS).show();
            }
            else {
                config.find('.panel-body .' + BlocksConstants.PANEL_BODY_SIMPLE_CLASS).append(html);
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
        return configPanels[id];
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
        if (configPanels == null) {
            configPanels = {};
        }

        var panelId = Commons.generateId();
        if (configPanels[panelId] == null) {

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

            configPanels[panelId] = div;

            //note: real adding is done manually in appendConfigPanelToSidebar()
        }

        return panelId
    };

    /**
     * Looks up the config panel with the supplied ID and adds it to the sidebar
     * in the tab with the right ID.
     */
    var appendConfigPanelToSidebar = function (id, tabId)
    {
        var configPanel = getConfigPanelForId(id);

        if (configPanel) {
            if (tabId == BlocksConstants.SIDEBAR_CONTEXT_ID) {
                $("#" + BlocksConstants.SIDEBAR_CONTEXT_ID).append(configPanel);
            }
            else if (tabId == BlocksConstants.SIDEBAR_FILES_ID) {
                $("#" + BlocksConstants.SIDEBAR_FILES_ID).append(configPanel);
            }
        }
        else {
            Logger.error('Couldn\'t find a config panel with this id', id);
        }
    };

}]);