/**
 * Created by wouter on 15/06/15.
 */

base.plugin("blocks.core.Sidebar", ["blocks.core.Broadcaster", "constants.blocks.common", "blocks.core.DomManipulation", "blocks.core.Layouter", "blocks.core.SidebarUtils", "blocks.core.Edit", "blocks.finder", "blocks.core.Notification", function (Broadcaster, Constants, DOM, Layouter, SidebarUtils, Edit, Finder, Notification)
{
    var SideBar = this;
    var configPanels = {};
    var currentProperty = null;
    var currentBlockEvent = null;



    /*
     * When clicking a property disable drag drop
     * */
    var setBlockFocus = function (property, block)
    {
        // Defines the element outside which to click to blur
        // is block if block is available
        var borderingElement = null;

        // Blur everything visually
        if (block != null) {
            block.element.parents().siblings().addClass(Constants.OPACITY_CLASS);
            block.element.siblings().addClass(Constants.OPACITY_CLASS);
            block.element.addClass(Constants.PROPERTY_EDIT_CLASS);
            borderingElement = block.element;
        } else {
            property.parents().siblings().addClass(Constants.OPACITY_CLASS);
            property.siblings().addClass(Constants.OPACITY_CLASS);
            property.addClass(Constants.PROPERTY_EDIT_CLASS);
            borderingElement = property;
        }

        var sidebarElement = $("." + Constants.PAGE_SIDEBAR_CLASS);
        sidebarElement.removeClass(Constants.OPACITY_CLASS);

        // prevent blur when clicking on following elements
        sidebarElement.addClass(Constants.PREVENT_BLUR_CLASS);
        borderingElement.addClass(Constants.PREVENT_BLUR_CLASS);

        Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD);

        $(document).on("mousedown.sidebar_edit_end", function (e)
        {

            var newProperty = null;
            var element = $(e.target);

            // if we clicked inside the sidebar -> ignore
            if (element[0] == sidebarElement[0] || sidebarElement.has(element).length > 0) {
                return;
            }

            while (newProperty == null && element[0] != borderingElement[0] && element.parent().length > 0) {
                if (element.hasAttribute("property") || element.hasAttribute("data-property")) {
                    newProperty = element;
                } else {
                    element = element.parent();
                }
            }

            var preventBlurElements = $("." + Constants.PREVENT_BLUR_CLASS);
            // check if we clicked outside this block
            if (!preventBlurElements.is(e.target) && preventBlurElements.has(e.target).length === 0 && preventBlurElements != newProperty && preventBlurElements.has(newProperty).length === 0) {
                // we clicked outside the property
                blurCurrentSelection(property, block);

                // Only send edit_end on mouse up. Otherwise the other clicked property will start editing immediately
                $(document).on("mouseup.sidebar_edit_end", function ()
                {
                    $(document).off("mouseup.sidebar_edit_end");
                    Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);
                });

            }
            // We didn't change block but we did change property
            else if (block != null && newProperty != null && (property == null || newProperty[0] != property[0])) {
                blurCurrentSelection(property, block);
                update(newProperty, Broadcaster.createEvent(e));
            } else {
                // nothing changed
            }
        });
    };

    var blurCurrentSelection = function (property, block)
    {
        // remove this trigger
        $(document).off("mousedown.sidebar_edit_end");

        // blur this block
        var editFunction = Edit.makeEditable(block.element);
        if (editFunction != null && editFunction.blur != null) {
            editFunction.blur(property, block);
        }
        if (property != null) {
            var editFunction = Edit.makeEditable(property);
            if (editFunction != null && editFunction.blur != null) {
                editFunction.blur(property, block);
            }
        }
        reset();
        block = null;
    }

    var reset = function ()
    {
        $("." + Constants.OPACITY_CLASS).removeClass(Constants.OPACITY_CLASS);
        $("." + Constants.PREVENT_BLUR_CLASS).removeClass(Constants.PREVENT_BLUR_CLASS);
        $("." + Constants.PROPERTY_EDIT_CLASS).removeClass(Constants.PROPERTY_EDIT_CLASS);
        $("." + Constants.BLOCK_EDIT_CLASS).removeClass(Constants.BLOCK_EDIT_CLASS);

        currentProperty = null;
        currentBlockEvent = null;
        configPanels = {};
        SideBar.refresh();
    };

    /*
     * Drill down and add functionality for each block
     * */
    var update = function (property, blockEvent)
    {
        // property: add div
        currentProperty = property;
        currentBlockEvent = blockEvent;
        var block = currentBlockEvent.property.current;
        setBlockFocus(property, block);
        SideBar.refresh();
    };

    /*
     * Drill down and add functionality for each block
     * */
    this.refresh = function ()
    {
        var filesContainer = $("#" + Constants.SIDEBAR_CONTEXT_ID);
        filesContainer.empty();

        var block = Broadcaster.getContainer();
        //this allows us to also use this function outside of a DnD context
        if (block) {
            var editFunction = Edit.makeEditable(block.element);
            if (editFunction != null && editFunction.focus != null) {
                var windowID = SideBar.createWindow(Constants.CONTEXT, block.element, "Page");
                editFunction.focus(windowID, block.element, null);
            }
        }

        if (currentBlockEvent != null) {
            block = currentBlockEvent.property.current;
        }
        //var editFunction = Edit.makeEditable(currentProperty);

        var activeBlocks = [];

        // Add editfunctionality for blocks
        while (block != null) {
            if (block instanceof blocks.elements.Block) {
                var blockTitle = 'Block';
                var editFunction = Edit.makeEditable(block.element);
                if (editFunction != null && editFunction.getWindowName != null) {
                    blockTitle = editFunction.getWindowName();
                }
                var windowID = SideBar.createWindow(Constants.CONTEXT, block.element, blockTitle);
                if (block.canDrag) {
                    SideBar.addRemoveBlockButton(windowID, block);
                }

                activeBlocks.push({element: block.element, id: windowID});

                if (editFunction != null && editFunction.focus != null) {
                    editFunction.focus(windowID, block.element, currentBlockEvent);
                }
            }
            block = block.parent;
        }

        // Add edit functionality for properties
        // Do not check blocks
        var property = currentProperty;
        while (property != null && !property.hasClass(Constants.PAGE_CONTENT) && property[0].tagName.indexOf("-") < 0) {
            var editFunction = Edit.makeEditable(property);
            if (editFunction != null && editFunction.focus != null) {
                var windowId = null;
                for(var i=0; i < activeBlocks.length; i++) {
                    if (activeBlocks[i].element.has(property)) {
                        windowId = activeBlocks[i].id;
                        break;
                    }
                }
                editFunction.focus(windowId, currentProperty, currentBlockEvent);
            }
            property = property.parent();
        }


    };

    this.addRemoveBlockButton = function (windowID, property)
    {

        //var remove = $("<div class='panel panel-default "+ Constants.REMOVE_BLOCK_CLASS +"'/>");
        var blockActions = $("<ul/>").addClass(Constants.BLOCK_ACTIONS_CLASS);
        var removeAction = $("<li><span>Remove block</span></li>");
        var removeButton = $("<a class='btn btn-danger btn-sm pull-right'><i class='fa fa-trash-o'></i></a>");
        blockActions.append(removeAction);
        removeAction.append(removeButton);

        removeButton.click(function ()
        {
            //TODO let's not ask for a confirmation but implement an undo-function later on...
            //confirm.removeClass("hidden");
            //text.addClass("hidden");

            reset();
            $("." + Constants.OPACITY_CLASS).removeClass(Constants.OPACITY_CLASS);
            Layouter.removeBlock(property);
        });

        this.addUIForProperty(windowID, blockActions);
        blockActions.after($("<hr/>"));
    };

    /**
     * Empties the sidebar to start adding UI with the functions below
     */
    this.clear = function ()
    {
        reset();
    };

    this.addUIForProperty = function (windowId, html)
    {
        var config = SideBar.getWindowForId(windowId);
        var content = config.children(".panel-body");
        content.append(html);
    };

    this.createWindow = function (type, element, title)
    {
        var windowId = SidebarUtils.makeid();
        if (configPanels[Constants.CONTEXT] == null) configPanels[Constants.CONTEXT] = {};
        var panels = configPanels[Constants.CONTEXT];

        if (panels[windowId] == null) {

            var div = $("<div class='panel panel-default'/>");
            var header = $("<div class='panel-heading'>" + title + "</div>");
            var content = $("<div class='panel-body'/>");
            div.append(header).append(content);

            panels[windowId] = div;
            if (type == Constants.CONTEXT) {
                $("#" + Constants.SIDEBAR_CONTEXT_ID).append(div);
            }

            div.mouseenter(function ()
            {
                highlight(element);
            });

            div.mouseleave(function ()
            {
                unhighlight(element);
            });
        }

        return windowId
    }

    this.getWindowForId = function (id)
    {
        var retVal = null;

        var panels = configPanels[Constants.CONTEXT];
        retVal = panels[id];

        return retVal;
    }

    this.addUniqueClass = function (windowId, element, label, values)
    {
        SideBar.addUIForProperty(windowId, SidebarUtils.addUniqueClass(element, label, values));
    };

    this.addOptionalClass = function (windowId, element, label, values)
    {
        SideBar.addUIForProperty(windowId, SidebarUtils.addOptionalClass(element, label, values));
    };

    this.addUniqueAttributeValue = function (windowId, element, label, name, values)
    {
        SideBar.addUIForProperty(windowId, SidebarUtils.addUniqueAttributeValue(element, label, name, values));
    };

    this.addUniqueAttribute = function (windowId, element, label, values)
    {
        SideBar.addUIForProperty(windowId, SidebarUtils.addUniqueAttribute(element, label, values));
    };

    this.addValueAttribute = function (windowId, element, label, placeholderText, name, confirm, serverSelect, urlSelect)
    {
        SideBar.addUIForProperty(windowId, SidebarUtils.addValueAttribute(element, label, placeholderText, name, confirm, serverSelect, urlSelect, this));
    };

    this.addValueHtml = function (windowId, element, label, placeholderText, confirm)
    {
        SideBar.addUIForProperty(windowId, SidebarUtils.addValueHtml(element, label, placeholderText, confirm));
    };

    // Called when editing is enabled. Catch mouseup and check if a block is editable
    this.enableEditing = function ()
    {
        $(document).on("mouseup.sidebar_edit_start", "." + Constants.PAGE_CONTENT_CLASS, function (event)
        {
            // find parents until parent is <body> or until parent has property attribute
            // first property enable editing
            var element = $(event.target);
            if (DOM.isContainer(element)) {
                element = element.children().last();
            }
            if (DOM.isRow(element)) {
                element = element.children().last();
            }
            if (DOM.isColumn(element)) {
                element = element.children().last();
            }

            var property = null;

            while (property == null && element[0].tagName.indexOf("-") == -1 && element[0].tagName != "BODY") {
                if (element.hasAttribute("property") || element.hasAttribute("data-property")) {
                    property = element;
                    break;
                } else {
                    element = element.parent();
                }
            }
            var blockEvent = Broadcaster.createEvent(event);

            if (blockEvent.block.current != null || property != null) {
                Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD);
                update(property, blockEvent);
            }
        });
    };

    this.disableEditing = function ()
    {
        $(document).off("mouseup.sidebar_edit_start");
    };


    // PRIVATE

    var highlight = function (element)
    {
        element.addClass(Constants.HIGHLIGHT_ANIMATION_CLASS);
        element.addClass(Constants.HIGHLIGHT_CLASS);
        setTimeout(function ()
        {
            element.removeClass(Constants.HIGHLIGHT_ANIMATION_CLASS);
        }, 200);
    };

    var unhighlight = function (element)
    {
        element.removeClass(Constants.HIGHLIGHT_ANIMATION_CLASS);
        element.removeClass(Constants.HIGHLIGHT_CLASS);
        setTimeout(function ()
        {
            element.removeClass(Constants.HIGHLIGHT_ANIMATION_CLASS);
        }, 200);
    };

}]);