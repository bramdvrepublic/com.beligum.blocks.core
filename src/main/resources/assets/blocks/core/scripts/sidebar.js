/**
 * Created by wouter on 15/06/15.
 */

base.plugin("blocks.core.Sidebar", ["blocks.core.Layouter", "blocks.media.Finder", "blocks.core.Notification", "base.core.Commons", "blocks.imports.Widget", "constants.blocks.core", "messages.blocks.core", function (Layouter, Finder, Notification, Commons, Widget, BlocksConstants, BlocksMessages) {

    var SideBar = this;
    var configPanels = {};
    var currentProperty = null;

    var activeBlocks = [];

    /**
     * @param block the block that should get focus (not null)
     * @param element one of these:
     *                - the first property element on the way up of the element that got clicked (inside the block)
     *                - the template element (then element==block.element) that got clicked
     *                - the page element
     * @param hotspot the (possibly changed) mouse coordinates that function as the 'hotspot' for this event (object with top and left like offset())
     * @param event the original event that triggered this all
     */
    this.focusBlock = function (block, element, hotspot, event) {

        this.reset();

        var currBlock = block;
        var currElement = element;
        activeBlocks = [];

        //little helper function to refactor things
        var pushActiveBlock = function (currBlock, currElement) {
            activeBlocks.push({
                block: currBlock,
                element: currElement
            });

            //we also push the properties inside a block
            //note: this is evened out after the first cycle
            if (!currElement.is(currBlock.element)) {
                activeBlocks.push({
                    block: currBlock,
                    element: currBlock.element
                });
            }
        };

        //allows us to select only the last row and column in the tree
        var firstRow = null;
        var lastRow = null;
        var firstColumn = null;
        var lastColumn = null;
        //we'll cycle through the parents until we hit the page, then reversing the order and creating windows, starting with the page
        while (currBlock != null) {
            if (currBlock instanceof blocks.elements.Property || currBlock instanceof blocks.elements.Block) {
                pushActiveBlock(currBlock, currElement);
            }
            else if (currBlock instanceof blocks.elements.Page) {
                //we select the _first_ column, (instead of the last, see row below) because it's what
                // we naturally expect in the GUI (the column closest around the block we're focusing)
                if (firstColumn != null) {
                    pushActiveBlock(firstColumn, firstColumn.element);
                }
                //if we have a row, push that one first before closing with the page
                if (lastRow != null) {
                    pushActiveBlock(lastRow, lastRow.element);
                }
                pushActiveBlock(currBlock, currElement);
            }
            else if (currBlock instanceof blocks.elements.Row) {
                if (firstRow == null) {
                    firstRow = currBlock;
                }
                lastRow = currBlock;
            }
            else if (currBlock instanceof blocks.elements.Column) {
                if (firstColumn == null) {
                    firstColumn = currBlock;
                }
                lastColumn = currBlock;
            }

            currBlock = currBlock.parent;
            // if the element is not the same as block.element, the first loop will be different, but
            // after one time, it will ease out
            currElement = currBlock == null ? null : currBlock.element;
        }

        var title = null;
        for (var i = activeBlocks.length - 1; i >= 0; i--) {

            var e = activeBlocks[i];

            var widget = Widget.Class.create(e.element);
            //save it for blur()
            if (widget) {
                activeBlocks[i].widget = widget;
            }

            //don't make windows for (real) properties, only blocks and pages
            var isPropertyInBlock = !e.block.element.is(e.element);
            var blockTitle = isPropertyInBlock ? 'property' : 'block';
            if (widget) {
                blockTitle = widget.getWindowName();
            }

            var windowTitle = title;
            if (title == null) {
                windowTitle = blockTitle;
            }
            else {
                windowTitle = title + '<i class="fa fa-fw fa-angle-right"/>' + blockTitle;
            }

            //we'll expand all windows by default, except the row and column
            var collapsed = false;
            if (e.block instanceof blocks.elements.Row || e.block instanceof blocks.elements.Column) {
                collapsed = true;
            }
            //if we're showing the controls for a block, close the window panel
            else if (block instanceof blocks.elements.Block && e.block instanceof blocks.elements.Page) {
                collapsed = true;
            }

            //we'll iterate the array in reverse order, but when focusing a block,
            //we don't want users to be able to save the page (it causes all kinds of problems),
            //so we disable the 'page-entry' if a block is focused
            var disabled = !(block instanceof blocks.elements.Page) && e.block instanceof blocks.elements.Page;

            // if a parent stopped the creation of sub-windows, keep executing the focus() method,
            // but without a window ID (allowing for logic without UI consequences)
            var windowID = SideBar.createWindow(e.element, windowTitle, collapsed, disabled);
            var addedOptions = false;

            // don't render the remove button for properties: only blocks can be deleted
            if (!isPropertyInBlock && e.block instanceof blocks.elements.Block && e.block.canDrag && windowID) {
                //this.addRemoveBlockButton(windowID, e.block);
                //addedOptions = true;
            }

            if (widget) {
                // the focus method can return a list of UI widgets it needs to add to the window
                // this way, we have control over that (where we have all the information to decide; eg. what property in which block, etc)
                widget.focus(e.block, e.element, hotspot, event);
                var optionsToAdd = widget.getConfigs(e.block, e.element);
                if (optionsToAdd) {
                    if (addedOptions && optionsToAdd.length > 0) {
                        this.addUIForProperty(windowID, '<hr>');
                        addedOptions = true;
                    }

                    //since we have a 'weight' setting, make sure we sort the array first
                    optionsToAdd.sort(function (a, b) {

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
                        this.addUIForProperty(windowID, optionsToAdd[w]);
                        addedOptions = true;
                    }
                }

                if (widget.getPropertyConfigs) {
                    $.merge(optionsToAdd, widget.getPropertyConfigs(e.block, e.element));
                }
            }

            //don't add an empty panel
            if (addedOptions) {
                this.appendWindowToSidebar(BlocksConstants.SIDEBAR_CONTEXT_ID, windowID);
                title = windowTitle;
            }
        }
    };

    this.reset = function () {
        this.unloadFinder();

        for (var i = 0; i < activeBlocks.length; i++) {
            var e = activeBlocks[i];
            if (e.widget) {
                e.widget.blur(e.block, e.element);
            }
        }
        configPanels = {};
        activeBlocks = [];

        //reset the sidebar and prepare for adding
        var sidebar = $("." + BlocksConstants.PAGE_SIDEBAR_CLASS);
        sidebar.removeClass(BlocksConstants.OPACITY_CLASS);
        sidebar.addClass(BlocksConstants.PREVENT_BLUR_CLASS);

        var sidebarContext = $("#" + BlocksConstants.SIDEBAR_CONTEXT_ID);
        sidebarContext.empty();
    };

    /*
     * Drill down and add functionality for each block
     * */
    var update = function (property) {
        // property: add div
        currentProperty = property;
        setBlockFocus(property);
        SideBar.refresh();
    };

    this.animateSidebarWidth = function (width, callback) {
        var windowWidth = $(window).width();

        var sidebarElement = $("." + BlocksConstants.PAGE_SIDEBAR_CLASS);
        sidebarElement.addClass(BlocksConstants.SIDEBAR_ANIMATED_CLASS);
        sidebarElement.css("width", (width) + "px");
        //one() = on() but only once
        sidebarElement.one('webkitTransitionEnd otransitionend oTransitionEnd msTransitionEnd transitionend', function (event) {
            if ($(event.target).hasClass(BlocksConstants.PAGE_SIDEBAR_CLASS)) {
                sidebarElement.removeClass(BlocksConstants.SIDEBAR_ANIMATED_CLASS);
                $("." + BlocksConstants.PAGE_CONTENT_CLASS).css("width", (windowWidth - width) + "px");

                if (callback) {
                    callback(event);
                }
            }
        });
    };

    this.addRemoveBlockButton = function (windowID, property) {
        //var remove = $("<div class='panel panel-default "+ Constants.REMOVE_BLOCK_CLASS +"'/>");
        var blockActions = $("<ul/>").addClass(BlocksConstants.BLOCK_ACTIONS_CLASS);
        var removeAction = $("<li><label>" + BlocksMessages.deleteBlockLabel + "</label></li>");
        var removeButton = $("<a class='btn btn-danger btn-sm pull-right'><i class='fa fa-fw fa-trash-o'></i></a>");
        blockActions.append(removeAction);
        removeAction.append(removeButton);

        removeButton.click(function () {
            //TODO let's not ask for a confirmation but implement an undo-function later on...
            //confirm.removeClass("hidden");
            //text.addClass("hidden");

            //resetOld();
            $("." + BlocksConstants.OPACITY_CLASS).removeClass(BlocksConstants.OPACITY_CLASS);
            Layouter.removeBlock(property);
        });

        this.addUIForProperty(windowID, blockActions);
    };

    this.addUIForProperty = function (windowId, html) {

        var config = SideBar.getWindowForId(windowId);
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
            Logger.error("Couldn't find window with ID " + windowId);
        }
    };

    this.createWindow = function (element, title, collapsed, disabled) {

        if (configPanels == null) {
            configPanels = {};
        }

        var windowId = Commons.generateId();
        if (configPanels[windowId] == null) {

            var panelId = windowId + '-panel';
            var bodyId = windowId + '-panel-body';
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
                var bodyAdvancedBodyId = windowId + '-advanced';
                var bodyAdvancedCollapsed = true;
                var bodyAdvancedHeader = $('<div class="collapser' + (bodyAdvancedCollapsed ? ' collapsed' : '') + '" data-toggle="collapse" data-target="#' + bodyAdvancedBodyId + '" aria-expanded="' + (bodyAdvancedCollapsed ? 'false' : 'true') + '" aria-controls="' + bodyAdvancedBodyId + '">' + BlocksMessages.sidebarPanelAdvancedTitle + '</div>').appendTo(bodyAdvanced);
                var bodyAdvancedBodyWrapper = $('<div id="' + bodyAdvancedBodyId + '" class="collapse' + (bodyAdvancedCollapsed ? '' : ' in') + '">').appendTo(bodyAdvanced);
                //same remark as above: better to have a wrapper around the content
                var bodyAdvancedBody = $('<div>').appendTo(bodyAdvancedBodyWrapper);

                if (element) {
                    div.mouseenter(function () {
                        highlight(element);
                    });

                    div.mouseleave(function () {
                        unhighlight(element);
                    });
                }
            }

            configPanels[windowId] = div;

            //note: real adding is done manually in appendWindowToSidebar()
        }

        return windowId
    };

    this.getWindowForId = function (id) {
        return configPanels[id];
    };

    this.appendWindowToSidebar = function (type, id) {

        var div = this.getWindowForId(id);

        if (type == BlocksConstants.SIDEBAR_CONTEXT_ID) {
            $("#" + BlocksConstants.SIDEBAR_CONTEXT_ID).append(div);
        }
        else if (type == BlocksConstants.SIDEBAR_FILES_ID) {
            $("#" + BlocksConstants.SIDEBAR_FILES_ID).append(div);
        }

        return div;
    };

    //TODO factor this away because the finder is no dependency of this project
    this.loadFinder = function (options) {
        //general test if we have the media plugin available
        var MediaConstants = base.getPlugin("constants.blocks.media.core");
        if (MediaConstants) {
            var contextTab = $("#" + BlocksConstants.SIDEBAR_CONTEXT_ID);
            var finderTab = $("#" + BlocksConstants.SIDEBAR_FILES_ID);
            contextTab.addClass(BlocksConstants.LOADING_CLASS);
            finderTab.removeClass(BlocksConstants.LOADING_CLASS);
            //we'll start off with an empty container and let createWindow() fill it
            finderTab.empty();

            //'switch' to the finder tab
            $("#" + BlocksConstants.SIDEBAR_FILES_TAB_ID).tab('show');

            //now create and add a new frame
            var windowID = SideBar.createWindow(null, BlocksMessages.finderTabTitle);
            SideBar.appendWindowToSidebar(BlocksConstants.SIDEBAR_FILES_ID, windowID);
            //let's us do perform some css tweaks
            var frame = SideBar.getWindowForId(windowID);
            frame.addClass(BlocksConstants.SIDEBAR_FINDER_PANEL_CLASS);

            //TODO maybe not necessary to reload this every time, but it allows us to always present a fresh uptodate view of the server content
            var finder = frame.find(".panel-body");
            finder.load(MediaConstants.FINDER_INLINE_ENDPOINT, function (response, status, xhr) {
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
    this.unloadFinder = function () {
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

    // -----PRIVATE-----
    var highlight = function (element) {
        //don't highlight the entire page
        if (!element.hasClass(BlocksConstants.PAGE_CONTENT_CLASS)) {
            element.addClass(BlocksConstants.HIGHLIGHT_CLASS);
        }
    };
    var unhighlight = function (element) {
        element.removeClass(BlocksConstants.HIGHLIGHT_CLASS);
    };

}]);