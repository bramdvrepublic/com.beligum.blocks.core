/**
 * Created by wouter on 15/06/15.
 */

base.plugin("blocks.core.Sidebar", ["blocks.core.Broadcaster", "constants.blocks.core", "blocks.core.DomManipulation", "blocks.core.Layouter", "blocks.core.SidebarUtils", "blocks.core.Edit", "blocks.media.Finder", "blocks.core.Notification", "base.core.Commons", "blocks.core.Hover", function (Broadcaster, Constants, DOM, Layouter, SidebarUtils, Edit, Finder, Notification, Commons, Hover)
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
        while (currBlock != null) {
            if (currBlock instanceof blocks.elements.Property || currBlock instanceof blocks.elements.Block || currBlock instanceof blocks.elements.Page) {

                activeBlocks.push({
                    block: currBlock,
                    element: currElement
                });

                if (currElement!=currBlock.element) {
                    activeBlocks.push({
                        block: currBlock,
                        element: currBlock.element
                    });
                }
            }
            currBlock = currBlock.parent;
            // if the element is not the same as block.element, the first loop will be different, but
            // after one time, it will ease out
            currElement = currBlock==null?null:currBlock.element;
        }

        var title = null;
        for (var i=activeBlocks.length-1;i>=0;i--) {
            var e = activeBlocks[i];

            var editFunction = Edit.makeEditable(e.element);

            //don't make windows for (real) properties, only blocks and pages
            var isRealProperty = e.block.element != e.element;
            var blockTitle = isRealProperty ? 'Property' : 'Block';
            if (editFunction != null && editFunction.getWindowName != null) {
                blockTitle = editFunction.getWindowName();
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
            var addedWidgets = false;

            // don't render the remove button for properties: only blocks can be deleted
            if (!isRealProperty && windowID) {
                if (e.block.canDrag) {
                    this.addRemoveBlockButton(windowID, e.block);
                    addedWidgets = true;
                }
            }

            if (editFunction != null && editFunction.focus != null) {
                // the focus method can return a list of UI widgets it needs to add to the window
                // this way, we have control over that (where we have all the information to decide; eg. what property in which block, etc)
                var widgetsToAdd = editFunction.focus(e.block, e.element, hotspot, event);
                if (widgetsToAdd) {
                    if (addedWidgets && widgetsToAdd.length>0) {
                        this.addUIForProperty(windowID, '<hr>');
                        addedWidgets = true;
                    }
                    for (var w=0;w<widgetsToAdd.length;w++) {
                        this.addUIForProperty(windowID, widgetsToAdd[w]);
                        addedWidgets = true;
                    }
                }
            }

            //don't add an empty panel
            if (addedWidgets) {
                this.appendWindowToSidebar(Constants.CONTEXT, windowID);
            }
        }
    };

    this.reset = function()
    {
        for (var i=0;i<activeBlocks.length;i++) {
            var e = activeBlocks[i];
            var editFunction = Edit.makeEditable(e.element);
            if (editFunction != null && editFunction.blur != null) {
                editFunction.blur(e.block, e.element);
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

    ///*
    // * When clicking a property disable drag drop
    // * */
    //var setBlockFocus = function (property)
    //{
    //    currentProperty = property;
    //
    //    // Defines the element outside which to click to blur
    //    // is block if block is available
    //    var borderingElement = null;
    //
    //    // Blur everything visually
    //    property.element.parents().siblings().addClass(Constants.OPACITY_CLASS);
    //    property.element.siblings().addClass(Constants.OPACITY_CLASS);
    //    property.element.addClass(Constants.PROPERTY_EDIT_CLASS);
    //    borderingElement = property.element;
    //
    //    var sidebarElement = $("." + Constants.PAGE_SIDEBAR_CLASS);
    //    sidebarElement.removeClass(Constants.OPACITY_CLASS);
    //
    //    // prevent blur when clicking on following elements
    //    sidebarElement.addClass(Constants.PREVENT_BLUR_CLASS);
    //    borderingElement.addClass(Constants.PREVENT_BLUR_CLASS);
    //
    //    Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD);
    //
    //    $(document).on("mousedown.sidebar_edit_end", function (e)
    //    {
    //        var newProperty = null;
    //        var element = $(e.target);
    //
    //        // if we clicked inside the sidebar -> ignore
    //        if (element[0] == sidebarElement[0] || sidebarElement.has(element).length > 0) {
    //            return;
    //        }
    //
    //        while (newProperty == null && element[0] != borderingElement[0] && element.parent().length > 0) {
    //            if (element.hasAttribute("property") || element.hasAttribute("data-property")) {
    //                newProperty = element;
    //            } else {
    //                element = element.parent();
    //            }
    //        }
    //
    //        var preventBlurElements = $("." + Constants.PREVENT_BLUR_CLASS);
    //        // check if we clicked outside this block
    //        if (!preventBlurElements.is(e.target) && preventBlurElements.has(e.target).length === 0 && preventBlurElements != newProperty && preventBlurElements.has(newProperty).length === 0) {
    //            // we clicked outside the property
    //            blurCurrentSelection(property, currentBlock);
    //
    //            // Only send edit_end on mouse up. Otherwise the other clicked property will start editing immediately
    //            $(document).on("mouseup.sidebar_edit_end", function (event)
    //            {
    //                $(document).off("mouseup.sidebar_edit_end");
    //                Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD, event);
    //            });
    //
    //        }
    //        // We didn't change block but we did change property
    //        else if (currentBlock != null && newProperty != null && (property == null || newProperty[0] != property[0])) {
    //            blurCurrentSelection(property, currentBlock);
    //            update(newProperty);
    //        } else {
    //            // nothing changed
    //        }
    //    });
    //};
    //
    //var blurCurrentSelection = function (property)
    //{
    //    // remove this trigger
    //    $(document).off("mousedown.sidebar_edit_end");
    //
    //    // blur this block
    //    if (property != null) {
    //        var editFunction = Edit.makeEditable(property);
    //        if (editFunction != null && editFunction.blur != null) {
    //            editFunction.blur(property, currentBlock);
    //        }
    //    }
    //    resetOld();
    //    currentBlock = null;
    //}
    //
    //var resetOld = function ()
    //{
    //    //blurCurrentSelection(currentProperty);
    //    $("." + Constants.OPACITY_CLASS).removeClass(Constants.OPACITY_CLASS);
    //    $("." + Constants.PREVENT_BLUR_CLASS).removeClass(Constants.PREVENT_BLUR_CLASS);
    //    $("." + Constants.PROPERTY_EDIT_CLASS).removeClass(Constants.PROPERTY_EDIT_CLASS);
    //    $("." + Constants.BLOCK_EDIT_CLASS).removeClass(Constants.BLOCK_EDIT_CLASS);
    //
    //    currentProperty = null;
    //    configPanels = {};
    //    SideBar.refresh();
    //};
    //
    //this.resetOld = function ()
    //{
    //    blurCurrentSelection(currentProperty);
    //};
    //
    //
    ///*
    // * Drill down and add functionality for each block
    // * */
    //this.refresh = function ()
    //{
    //    //start with an empty sidebar and add frames where needed
    //    var sidebar = $("#" + Constants.SIDEBAR_CONTEXT_ID);
    //    sidebar.empty();
    //
    //    var block = currentProperty;
    //    //this allows us to also use this function outside of a DnD context
    //    if (block) {
    //        var editFunction = Edit.makeEditable(block.element);
    //        if (editFunction != null && editFunction.focus != null) {
    //            var windowID = SideBar.createWindow(block.element, "Page");
    //            SideBar.appendWindowToSidebar(Constants.CONTEXT, windowID);
    //            editFunction.focus(windowID, block.element, null);
    //        }
    //    }
    //
    //    var activeBlocks = [];
    //
    //    // Add editfunctionality for blocks
    //    while (block != null) {
    //        if (block instanceof blocks.elements.Block) {
    //            var blockTitle = 'Block';
    //            var editFunction = Edit.makeEditable(block.element);
    //            if (editFunction != null && editFunction.getWindowName != null) {
    //                blockTitle = editFunction.getWindowName();
    //            }
    //            var windowID = SideBar.createWindow(block.element, blockTitle);
    //            SideBar.appendWindowToSidebar(Constants.CONTEXT, windowID);
    //            if (block.canDrag) {
    //                SideBar.addRemoveBlockButton(windowID, block);
    //            }
    //
    //            activeBlocks.push({element: block.element, id: windowID});
    //
    //            if (editFunction != null && editFunction.focus != null) {
    //                editFunction.focus(windowID, block);
    //            }
    //        }
    //        block = block.parent;
    //    }
    //
    //    // Add edit functionality for properties
    //    // Do not check blocks
    //    var property = currentProperty;
    //    while (property != null && property.element != null && !property.element.hasClass(Constants.PAGE_CONTENT) && property.element[0].tagName.indexOf("-") < 0) {
    //        var editFunction = Edit.makeEditable(property);
    //        if (editFunction != null && editFunction.focus != null) {
    //            var windowId = null;
    //            for (var i = 0; i < activeBlocks.length; i++) {
    //                if (activeBlocks[i].element.has(property)) {
    //                    windowId = activeBlocks[i].id;
    //                    break;
    //                }
    //            }
    //            editFunction.focus(windowId, currentProperty);
    //        }
    //        property = property.parent();
    //    }
    //};

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

    ///**
    // * Empties the sidebar to start adding UI with the functions below
    // */
    //this.clear = function ()
    //{
    //    resetOld();
    //};

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

    //this.addUniqueClass = function (windowId, element, label, values)
    //{
    //    SideBar.addUIForProperty(windowId, SidebarUtils.addUniqueClass(this, element, label, values));
    //};
    //
    //this.addOptionalClass = function (windowId, element, label, values)
    //{
    //    SideBar.addUIForProperty(windowId, SidebarUtils.addOptionalClass(this, element, label, values));
    //};
    //
    //this.addSliderClass = function (windowId, element, label, values)
    //{
    //    SideBar.addUIForProperty(windowId, SidebarUtils.addSliderClass(this, element, label, values));
    //};
    //
    //this.addUniqueAttributeValue = function (windowId, element, label, name, values)
    //{
    //    SideBar.addUIForProperty(windowId, SidebarUtils.addUniqueAttributeValue(this, element, label, name, values));
    //};
    //
    //this.addUniqueAttribute = function (windowId, element, label, values)
    //{
    //    SideBar.addUIForProperty(windowId, SidebarUtils.addUniqueAttribute(this, element, label, values));
    //};
    //
    //this.addValueAttribute = function (windowId, element, label, placeholderText, name, confirm, serverSelect, pageSelect)
    //{
    //    SideBar.addUIForProperty(windowId, SidebarUtils.addValueAttribute(this, element, label, placeholderText, name, confirm, serverSelect, pageSelect));
    //};
    //
    //this.addValueHtml = function (windowId, element, label, placeholderText, confirm)
    //{
    //    SideBar.addUIForProperty(windowId, SidebarUtils.addValueHtml(this, element, label, placeholderText, confirm));
    //};

    //// Called when editing is enabled. Catch mouseup and check if a block is editable
    //this.enableEditing = function ()
    //{
    //    // don't filter on overlay classes here, because we deactivated events on the overlay during mousedown
    //    // (see mouse.js and it's BLOCK_OVERLAY_NO_EVENTS_CLASS class with pointer-events: none;)
    //    $(document).on("mouseup.sidebar_edit_start", function (event)
    //    {
    //        var block = Hover.getHoveredBlock();
    //        var propertyElement = $(event.currentTarget);
    //
    //        while (!(propertyElement.hasAttribute("property") || propertyElement.hasAttribute("data-property")) && el[0].tagName.indexOf("-") == -1 && el[0].tagName != "BODY") {
    //            propertyElement = propertyElement.parent();
    //        }
    //
    //        //update(firstPropEl);
    //        //
    //        //return;
    //        //
    //        //
    //        ////TODO look up the property of this overlay with the reverse map
    //        ////var block = null;
    //        ////if (propertyElement.length>0) {
    //        ////    var firstPropEl = null;
    //        ////    var el = property.element;
    //        ////
    //        ////    while (firstPropEl == null && el[0].tagName.indexOf("-") == -1 && el[0].tagName != "BODY") {
    //        ////        if (el.hasAttribute("property") || el.hasAttribute("data-property")) {
    //        ////            firstPropEl = el;
    //        ////            break;
    //        ////        } else {
    //        ////            el = el.parent();
    //        ////        }
    //        ////    }
    //        ////
    //        ////    if (firstPropEl != null) {
    //        ////        Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD, event);
    //        ////        update(firstPropEl);
    //        ////    }
    //        ////
    //        ////}
    //        ////else {
    //        ////    Logger.error("Encountered overlay without proper element attached, this shouldn't happen");
    //        ////}
    //        //
    //        ////
    //        ////var block = blocks.elements.Property.INDEX[element.attr(blocks.elements.Property.OVERLAY_INDEX_ATTR)];
    //        //
    //        ////THIS IS WHERE I LEFT OFF: how do we do this? Find the first property inside, or on tag name, or just pass this tag?
    //        ////last one is my favorite
    //        //var editFunction = Edit.makeEditable(propertyElement);
    //        //if (editFunction != null && editFunction.focus != null) {
    //        //    var windowID = SideBar.createWindow(propertyElement, "BLAH");
    //        //    SideBar.appendWindowToSidebar(Constants.CONTEXT, windowID);
    //        //    editFunction.focus(windowID, propertyElement, null);
    //        //}
    //        //
    //        ////DEBUGGIGN
    //        //return;
    //        //
    //        //
    //        //if (property.element.length > 0) {
    //        //    var firstPropEl = null;
    //        //    var el = property.element;
    //        //
    //        //    while (firstPropEl == null && el[0].tagName.indexOf("-") == -1 && el[0].tagName != "BODY") {
    //        //        if (el.hasAttribute("property") || el.hasAttribute("data-property")) {
    //        //            firstPropEl = el;
    //        //            break;
    //        //        } else {
    //        //            el = el.parent();
    //        //        }
    //        //    }
    //        //    if (firstPropEl != null) {
    //        //        Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD, event);
    //        //        update(firstPropEl);
    //        //    }
    //        //}
    //        //else {
    //        //    Logger.error("Encountered overlay without proper element attached, this shouldn't happen");
    //        //}
    //        //
    //        ////DEBUGGING
    //        //return;
    //        ////
    //        ////// find parents until parent is <body> or until parent has property attribute
    //        ////// first property enable editing
    //        ////if (element.length>0 && DOM.isContainer(element)) {
    //        ////    element = element.children().last();
    //        ////}
    //        ////if (element.length>0 && DOM.isRow(element)) {
    //        ////    element = element.children().last();
    //        ////}
    //        ////if (element.length>0 && DOM.isColumn(element)) {
    //        ////    element = element.children().last();
    //        ////}
    //        ////
    //        ////if (element.length>0) {
    //        ////    var property = null;
    //        ////
    //        ////    while (property == null && element[0].tagName.indexOf("-") == -1 && element[0].tagName != "BODY") {
    //        ////        if (element.hasAttribute("property") || element.hasAttribute("data-property")) {
    //        ////            property = element;
    //        ////            break;
    //        ////        } else {
    //        ////            element = element.parent();
    //        ////        }
    //        ////    }
    //        ////
    //        ////    //TODO work on this
    //        ////    var blockEvent = Broadcaster.createEvent(event);
    //        ////
    //        ////    if (blockEvent.block != null || property != null) {
    //        ////        Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD, event);
    //        ////        update(property, blockEvent);
    //        ////    }
    //        ////}
    //        ////else {
    //        ////    Logger.warn("Empty element found (possibly after drilling down), is this ok?");
    //        ////}
    //    });
    //};
    //
    //this.disableEditing = function ()
    //{
    //    $(document).off("mouseup.sidebar_edit_start");
    //};

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