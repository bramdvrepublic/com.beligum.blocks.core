/**
 * Created by wouter on 15/06/15.
 */


base.plugin("blocks.core.Sidebar", ["blocks.core.Broadcaster", "constants.blocks.common", "blocks.core.DomManipulation", "blocks.core.Layouter", "blocks.core.Plugin-Utils", "blocks.core.Edit",  function (Broadcaster, Constants, DOM, Layouter, Plugin, Edit)
{
    var SideBar = this;
    var configPanels = {};
    var currentProperty = null;
    var currentBlock = null;


    // This is called when we click the files tab
    // So onselect = null
    // TODO set this id correct in constants file
    $(document).on('click', "#ttt", function (e) {
        Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD);
        $("." + Constants.PAGE_CONTENT_CLASS).hide();
        $("." + Constants.BLOCKS_START_BUTTON).hide();
        // add close button
        var closeBtn = $('<div class="btn '+ Constants.CLOSE_FINDER_BUTTON +'">X</div>');
        $("#" + Constants.SIDEBAR_FILES_ID).append(closeBtn);

        closeBtn.click(function() {
            $("." + Constants.PAGE_CONTENT_CLASS).show();
            $("." + Constants.BLOCKS_START_BUTTON).show();
            $('.' + Constants.PAGE_SIDEBAR_CLASS + ' a[href="#' + Constants.SIDEBAR_STYLE_ID +'"]').tab('show')
            Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);
        });
    });


    /*
     * When clicking a property disable drag drop
     * */
    var setBlockFocus = function(property, block) {
        // Defines the element outside which to click to blur
        // is block if block is available
        var borderingElement = null;

        // Blur everything visually
        if (block != null) {
            block.element.parents().siblings().addClass(Constants.OPACITY_CLASS);
            block.element.siblings().addClass(Constants.OPACITY_CLASS);
            borderingElement = block.element;
        } else {
            property.parents().siblings().addClass(Constants.OPACITY_CLASS);
            property.siblings().addClass(Constants.OPACITY_CLASS);
            borderingElement = property;
        }

        var sidebarElement = $("." + Constants.PAGE_SIDEBAR_CLASS);
        sidebarElement.removeClass(Constants.OPACITY_CLASS);
        Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD);

        $(document).on("mousedown.sidebar", function(e) {


            var newProperty = null;
            var element = $(e.target);

            // if we clicked inside the sidebar -> ignore
            if (element[0] == sidebarElement[0] || sidebarElement.has(element).length > 0) {
                return;
            }

            while (newProperty == null && element[0] != borderingElement[0] && element.parent().length > 0) {
                if (element.hasAttribute("property") || element.hasAttribute("data-property")) {
                    newProperty=element;
                } else {
                    element = element.parent();
                }
            }

            // check if we clicked outside this block
            if (!borderingElement.is(e.target) && borderingElement.has(e.target).length === 0 && borderingElement != newProperty && borderingElement.has(newProperty).length === 0) {
                // we clicked outside the property
                if (block == null || !block.isTriggered(e.pageX, e.pageY)) {
                    // remove this trigger
                    $(document).off("mousedown.sidebar");
                    // blur this block
                    $("." + Constants.OPACITY_CLASS).removeClass(Constants.OPACITY_CLASS);

                    if (block != null) {
                        var editFunction = Edit.makeEditable(block.element);
                        if (editFunction != null && editFunction.blur != null) {
                            editFunction.blur(property, block);
                        }
                    }

                    if (property != null) {
                        var editFunction = Edit.makeEditable(property);
                        if (editFunction != null && editFunction.blur != null) {
                            editFunction.blur(property, block);
                        }
                    }

                    block = null;
                    Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);
                    reset();

                }
            }

            // We didn't change block but we did change property
            else if (newProperty != null && (property == null || newProperty[0] != property[0])) {
                // remove this trigger
                $(document).off("mousedown.sidebar");
                // blur this block
                $("." + Constants.OPACITY_CLASS).removeClass(Constants.OPACITY_CLASS);
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
                update(newProperty, Broadcaster.createEvent(e));
            }


        });


    };

    var reset = function() {
        configPanels = {};
        $("#" + Constants.SIDEBAR_STYLE_ID).empty();
        $("#" + Constants.SIDEBAR_CONTENT_ID).empty();
        $("#inbetween").empty();
        var block = Broadcaster.getContainer();
        var editFunction = Edit.makeEditable(block.element);
        if (editFunction != null && editFunction.focus != null) {
            editFunction.focus(block.element, null);
        }
    };

    /*
     * Drill down and add functionality for each block
     * */
    var update = function(property, blockEvent) {
        // property: add div

        var editFunction = null;

        if (property != null) {
            editFunction = Edit.makeEditable(property);
        }

        var block = blockEvent.property.current;
        //if (block != null) {
            setBlockFocus(property, block);
            if (editFunction != null) {
                editFunction.focus(property, blockEvent);
            }

            while (block != null) {
                if (block instanceof blocks.elements.Block) {
                    if (block.editFunction != null && block.editFunction.focus != null) {
                        block.editFunction.focus(property, blockEvent);
                    }
                    if (block.canDrag) SideBar.addRemoveBlockButton(block);
                }
                block = block.parent;
            }
        //}

    };


    this.addRemoveBlockButton = function(property) {
        var remove = $("<div class='panel panel-default "+ Constants.REMOVE_BLOCK_CLASS +"'/>");
        var body = $("<div class='panel-body'><div>");
        var text = $("<div class='text'><span>Remove block</span></div>");
        var button = $("<span class='btn btn-danger btn-sm pull-right'><i class='fa fa-trash-o'></i></span></div>");
        var confirm = $("<div class='confirm hidden'><div class='confirm-text'>Are you sure you want to remove this block?</div><div><span class='btn btn-sm btn-success'><i class='fa fa-check'></i></span><span class='btn btn-sm btn-danger'><i class='fa fa-times'></i></span></div></div>")
        var yes = confirm.find(".btn-success");
        var no = confirm.find(".btn-danger");

        button.click(function() {
            confirm.removeClass("hidden");
            text.addClass("hidden");
        });

        no.click(function() {
            text.removeClass("hidden");
            confirm.addClass("hidden");
        });

        yes.click(function() {
            reset();
            $("." + Constants.OPACITY_CLASS).removeClass(Constants.OPACITY_CLASS);
            Layouter.removeBlock(property);
        })

        remove.append(body.append(text.append(button)).append(confirm));

        remove.mouseenter(function() {
            highlight(property.element);
        });

        remove.mouseleave(function() {
            unhighlight(property.element);
        });

        $("#inbetween").prepend(remove);
    };

    this.addUIForProperty = function(windowId, element, html) {
        var config = SideBar.getWindowForId(windowId, element, null);
        var content = config.children(".panel-body");
        content.append(html);
    };

    this.createWindow = function(type, element, title)
    {
        var windowId = Plugin.makeid();
        if (configPanels[type] == null) configPanels[type] = {};
        var panels = configPanels[type];

        if (panels[windowId] == null) {

            var div = $("<div class='panel panel-default'/>");
            var header = $("<div class='panel-heading'>" + title + "</div>");
            var content = $("<div class='panel-body'/>");
            div.append(header).append(content);

            panels[windowId] = div;
            if (type == Constants.STYLE) {
                $("#" + Constants.SIDEBAR_STYLE_ID).append(div);
            } else if (type == Constants.CONTENT) {
                $("#" + Constants.SIDEBAR_CONTENT_ID).append(div);
            }

            div.mouseenter(function() {
                highlight(element);
            });

            div.mouseleave(function() {
                unhighlight(element);
            });
        }
        return windowId
    }

    this.getWindowForId = function(id) {
        var retVal = null;

        if (configPanels[Constants.STYLE] != null) {
            var panels = configPanels[Constants.STYLE];
            retVal = panels[id];
        }

        if (retVal == null && configPanels[Constants.CONTENT] != null) {
            var panels = configPanels[Constants.CONTENT];
            retVal = panels[id];
        }

        return retVal;
    }

    this.addUniqueClass = function(windowId, element, label, values) {
        SideBar.addUIForProperty(windowId, element, Plugin.addUniqueClass(element, label, values));
    };

    this.addOptionalClass = function(windowId, element, label, values) {
        SideBar.addUIForProperty(windowId, element, Plugin.addOptionalClass(element, label, values));
    };

    this.addUniqueAttributeValue = function(windowId, element, label, name, values) {
        SideBar.addUIForProperty(windowId, element, Plugin.addUniqueAttributeValue(element, label, name, values));
    };

    this.addUniqueAttribute = function(windowId, element, label, values) {
        SideBar.addUIForProperty(windowId, element, Plugin.addUniqueAttribute( element, label, values));
    };

    this.addValueAttribute = function(windowId, element, label, name, confirm, textSelect, serverSelect, urlSelect) {
        SideBar.addUIForProperty(windowId, element, Plugin.addValueAttribute(element, label, name, confirm, textSelect, serverSelect, urlSelect));
    };

    this.addValueHtml = function(windowId, element, label, confirm) {
        SideBar.addUIForProperty(windowId, element, Plugin.addValueHtml(element, label, confirm));
    };

    this.enableEditing = function() {

        $(document).on("mouseup.sidebar_start", "." + Constants.PAGE_CONTENT_CLASS, function(event) {
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
                    property=element;
                } else {
                    element = element.parent();
                }
            }
            var blockEvent = Broadcaster.createEvent(event);

            if (blockEvent.block.current != null || property != null) {
                Broadcaster.send(Broadcaster.EVENTS.START_EDIT_FIELD);
                update(property, blockEvent);
                Logger.debug("test 1st click");
            }
            event.stopPropagation();
        });
    };

    this.disableEditing = function() {
        $(document).off("mouseup.sidebar_start");
    };

    // PRIVATE



    var highlight = function(element) {
        element.addClass(Constants.HIGHLIGHT_ANIMATION_CLASS);
        element.addClass(Constants.HIGHLIGHT_CLASS);
        setTimeout(function(){
            element.removeClass(Constants.HIGHLIGHT_ANIMATION_CLASS);
        }, 200);
    }

    var unhighlight = function(element) {
        element.removeClass(Constants.HIGHLIGHT_ANIMATION_CLASS);
        element.removeClass(Constants.HIGHLIGHT_CLASS);
        setTimeout(function(){
            element.removeClass(Constants.HIGHLIGHT_ANIMATION_CLASS);
        }, 200);
    }

}]);