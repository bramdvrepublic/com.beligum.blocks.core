/**
 * Created by wouter on 15/06/15.
 */

base.plugin("blocks.core.Sidebar", ["blocks.core.Broadcaster", "constants.blocks.core", "blocks.core.DomManipulation", "blocks.core.Layouter", "blocks.core.SidebarUtils", "blocks.media.Finder", "blocks.core.Notification", "base.core.Commons", "blocks.core.Hover", "blocks.imports.Widget", function (Broadcaster, Constants, DOM, Layouter, SidebarUtils, Finder, Notification, Commons, Hover, Widget)
{
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
    this.focusBlock = function (block, element, hotspot, event)
    {
        this.reset();

        var currBlock = block;
        var currElement = element;
        activeBlocks = [];

        //we'll cycle through the parents until we hit the page, then reversing the order and creating windows, starting with the page
        var lastRow = null;

        //little helper function to refactor things
        var pushActiveBlock = function(currBlock, currElement)
        {
            activeBlocks.push({
                block: currBlock,
                element: currElement
            });

            //we also push the properties inside a block
            //note: this is evened out after the first cycle
            if (currElement!=currBlock.element) {
                activeBlocks.push({
                    block: currBlock,
                    element: currBlock.element
                });
            }
        };
        while (currBlock != null) {
            if (currBlock instanceof blocks.elements.Property || currBlock instanceof blocks.elements.Block) {
                pushActiveBlock(currBlock, currElement);
            }
            else if (currBlock instanceof blocks.elements.Page) {
                //TODO not yet, but might be interesting to keep for later..
                //if we have a row, push that one first before closing with the page
                //if (lastRow!=null) {
                //    pushActiveBlock(lastRow.element, lastRow.element);
                //}
                pushActiveBlock(currBlock, currElement);
            }
            else if (currBlock instanceof blocks.elements.Row) {
                lastRow = currBlock;
            }
            currBlock = currBlock.parent;
            // if the element is not the same as block.element, the first loop will be different, but
            // after one time, it will ease out
            currElement = currBlock==null?null:currBlock.element;
        }

        var title = null;
        for (var i=activeBlocks.length-1;i>=0;i--) {
            var e = activeBlocks[i];

            var widget = Widget.Class.create(e.element);

            //don't make windows for (real) properties, only blocks and pages
            var isRealProperty = e.block.element != e.element;
            var blockTitle = isRealProperty ? 'property' : 'block';
            if (widget) {
                blockTitle = widget.getWindowName();
            }

            if (title == null) {
                title = blockTitle;
            }
            else {
                title = title + '<i class="fa fa-fw fa-angle-right"/>' + blockTitle;
            }

            // if a parent stopped the creation of sub-windows, keep executing the focus() method,
            // but without a window ID (allowing for logic without UI consequences)
            var windowID = SideBar.createWindow(e.element, title);
            var addedOptions = false;

            // don't render the remove button for properties: only blocks can be deleted
            if (!isRealProperty && windowID) {
                if (e.block.canDrag) {
                    this.addRemoveBlockButton(windowID, e.block);
                    addedOptions = true;
                }
            }

            if (widget) {
                // the focus method can return a list of UI widgets it needs to add to the window
                // this way, we have control over that (where we have all the information to decide; eg. what property in which block, etc)
                widget.focus(e.block, e.element, hotspot, event);
                var optionsToAdd = widget.getOptionConfigs(e.block, e.element);
                if (optionsToAdd) {
                    if (addedOptions && optionsToAdd.length>0) {
                        this.addUIForProperty(windowID, '<hr>');
                        addedOptions = true;
                    }
                    for (var w=0;w<optionsToAdd.length;w++) {
                        this.addUIForProperty(windowID, optionsToAdd[w]);
                        addedOptions = true;
                    }
                }
            }

            //don't add an empty panel
            if (addedOptions) {
                this.appendWindowToSidebar(Constants.CONTEXT, windowID);
            }
        }
    };

    this.reset = function()
    {
        for (var i=0;i<activeBlocks.length;i++) {
            var e = activeBlocks[i];
            var widget = Widget.Class.create(e.element);
            if (widget) {
                widget.blur(e.block, e.element);
            }
        }
        configPanels = {};
        activeBlocks = [];

        //reset the sidebar and prepare for adding
        var sidebar = $("." + Constants.PAGE_SIDEBAR_CLASS);
        sidebar.removeClass(Constants.OPACITY_CLASS);
        sidebar.addClass(Constants.PREVENT_BLUR_CLASS);

        var sidebarContext = $("#" + Constants.SIDEBAR_CONTEXT_ID);
        sidebarContext.empty();
    };

    /*
     * Drill down and add functionality for each block
     * */
    var update = function (property)
    {
        // property: add div
        currentProperty = property;
        setBlockFocus(property);
        SideBar.refresh();
    };

    this.animateSidebarWidth = function (width, callback)
    {
        var windowWidth = $(window).width();

        var sidebarElement = $("." + Constants.PAGE_SIDEBAR_CLASS);
        sidebarElement.addClass(Constants.SIDEBAR_ANIMATED_CLASS);
        sidebarElement.css("width", (width) + "px");
        //one() = on() but only once
        sidebarElement.one('webkitTransitionEnd otransitionend oTransitionEnd msTransitionEnd transitionend', function (event)
        {
            if ($(event.target).hasClass(Constants.PAGE_SIDEBAR_CLASS)) {
                sidebarElement.removeClass(Constants.SIDEBAR_ANIMATED_CLASS);
                $("." + Constants.PAGE_CONTENT_CLASS).css("width", (windowWidth - width) + "px");

                if (callback) {
                    callback(event);
                }
            }
        });
    };

    this.addRemoveBlockButton = function (windowID, property)
    {
        //var remove = $("<div class='panel panel-default "+ Constants.REMOVE_BLOCK_CLASS +"'/>");
        var blockActions = $("<ul/>").addClass(Constants.BLOCK_ACTIONS_CLASS);
        var removeAction = $("<li><span>Remove block</span></li>");
        var removeButton = $("<a class='btn btn-danger btn-sm pull-right'><i class='fa fa-fw fa-trash-o'></i></a>");
        blockActions.append(removeAction);
        removeAction.append(removeButton);

        removeButton.click(function ()
        {
            //TODO let's not ask for a confirmation but implement an undo-function later on...
            //confirm.removeClass("hidden");
            //text.addClass("hidden");

            //resetOld();
            $("." + Constants.OPACITY_CLASS).removeClass(Constants.OPACITY_CLASS);
            Layouter.removeBlock(property);
        });

        this.addUIForProperty(windowID, blockActions);
    };

    this.addUIForProperty = function (windowId, html)
    {
        var config = SideBar.getWindowForId(windowId);
        if (config) {
            var content = config.children(".panel-body");
            content.append(html);
        }
        else {
            Logger.error("Couldn't find window with ID " + windowId);
        }
    };

    this.createWindow = function (element, title)
    {
        var windowId = Commons.generateId();
        if (configPanels == null) {
            configPanels = {};
        }

        if (configPanels[windowId] == null) {

            var div = $("<div class='panel panel-default'/>");
            var header = $("<div class='panel-heading'>" + title + "</div>");
            var content = $("<div class='panel-body'/>");
            div.append(header).append(content);

            configPanels[windowId] = div;

            if (element) {
                div.mouseenter(function ()
                {
                    highlight(element);
                });

                div.mouseleave(function ()
                {
                    unhighlight(element);
                });
            }

            //note: real adding is done manually in appendWindowToSidebar()
        }

        return windowId
    };

    this.getWindowForId = function (id)
    {
        return configPanels[id];
    };

    this.appendWindowToSidebar = function (type, id)
    {
        var div = this.getWindowForId(id);

        if (type == Constants.CONTEXT) {
            $("#" + Constants.SIDEBAR_CONTEXT_ID).append(div);
        }
        else if (type == Constants.FINDER) {
            $("#" + Constants.SIDEBAR_FILES_ID).append(div);
        }

        return div;
    };

    //TODO factor this away because the finder is no dependency of this project
    this.loadFinder = function (options)
    {
        var contextTab = $("#" + Constants.SIDEBAR_CONTEXT_ID);
        var finderTab = $("#" + Constants.SIDEBAR_FILES_ID);
        contextTab.addClass(Constants.LOADING_CLASS);
        finderTab.removeClass(Constants.LOADING_CLASS);
        //we'll start off with an empty container and let createWindow() fill it
        finderTab.empty();

        //'switch' to the finder tab
        $("#" + Constants.SIDEBAR_FILES_TAB_ID).tab('show');

        //now create and add a new frame
        var windowID = SideBar.createWindow(null, "Files on server");
        SideBar.appendWindowToSidebar(Constants.FINDER, windowID);
        //let's us do perform some css tweaks
        var frame = SideBar.getWindowForId(windowID);
        frame.addClass(Constants.SIDEBAR_FINDER_PANEL_CLASS);

        //TODO maybe not necessary to reload this every time, but it allows us to always present a fresh uptodate view of the server content
        var finder = frame.children(".panel-body");
        finder.load("/media/finder-inline", function (response, status, xhr)
        {
            if (status == "error") {
                var msg = "Error while loading the finder; ";
                Notification.error(msg + xhr.status + " " + xhr.statusText, xhr);
                finder.removeClass(Constants.LOADING_CLASS);
            }
            else {
                Finder.init(options);
                //don't show the warning when clicking something in the finder
                finder.attr(Constants.CLICK_ROLE_ATTR, Constants.FORCE_CLICK_ATTR_VALUE);
                finder.removeClass(Constants.LOADING_CLASS);
            }
        });
    };
    this.unloadFinder = function ()
    {
        //'switch' back to the context tab
        $("#" + Constants.SIDEBAR_CONTEXT_TAB_ID).tab('show');

        var contextTab = $("#" + Constants.SIDEBAR_CONTEXT_ID);
        var finderTab = $("#" + Constants.SIDEBAR_FILES_ID);
        contextTab.removeClass(Constants.LOADING_CLASS);
        finderTab.addClass(Constants.LOADING_CLASS);
        finderTab.empty();
    };

    // -----PRIVATE-----
    var highlight = function (element)
    {
        //don't highlight the entire page
        if (!element.hasClass(Constants.PAGE_CONTENT_CLASS)) {
            element.addClass(Constants.HIGHLIGHT_CLASS);
        }
    };
    var unhighlight = function (element)
    {
        element.removeClass(Constants.HIGHLIGHT_CLASS);
    };

}]);