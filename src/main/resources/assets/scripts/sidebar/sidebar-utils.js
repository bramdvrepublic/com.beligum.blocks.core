/**
 * Created by wouter on 18/06/15.
 */
base.plugin("blocks.core.SidebarUtils", ["constants.blocks.core", "blocks.media.Finder", "base.core.Commons", function (Constants, Finder, Commons)
{
    var SidebarUtils = this;

    this.OLD_VAL_ATTR = 'data-reset';
    this.TEXT_INPUT_ACTION_OPTION_DISABLE = 'disable';
    this.TEXT_INPUT_ACTION_OPTION_ONSELECT = 'onSelect';

    /*
     * element: element to change
     * labelText: name to show as label
     * values = array of objects {value: 'a value to change', name: 'name of the value}
     * */
    this.addUniqueClass = function (Sidebar, element, labelText, values)
    {
        var classFound = false;

        var retVal = this.createCombobox(Sidebar, element, labelText, values,
            function initCallback(testValue)
            {
                var retVal = false;

                if (element.hasClass(testValue)) {
                    if (!classFound) {
                        classFound = true;
                        retVal = true;
                    }
                    else {
                        // If more then 1 value is selected, only keep the first value
                        element.removeClass(c.value);
                    }
                }

                return retVal;
            },
            function changeCallback(oldValue, newValue)
            {
                if (oldValue) {
                    element.removeClass(oldValue);
                }
                element.addClass(newValue);
            });

        return retVal;
    };

    /*
     * element: element to change
     * label: name to show as label
     * values = array of objects {value: 'a value to change', name: 'name of the value}
     * */
    this.addOptionalClass = function (Sidebar, element, label, values)
    {
        // Create selectbox to add to sidebar
        var id = Commons.generateId();
        var content = $("<div class='form-group' />");
        content.append("<label>" + label + "</label>");

        // Create checkboxes for each value
        var classFound = null;
        for (var i = 0; i < values.length; i++) {
            var c = values[i];

            var wrapper = $('<div class="checkbox" />');
            var label = $('<label />');
            var input = $('<input type="checkbox" >');
            input.attr("value", c.value);
            if (element.hasClass(c.value)) {
                input.attr("checked", "checked");
            }
            wrapper.append(label.append(input).append(c.name));
            content.append(wrapper);
            input.change(function (e)
            {
                var box = $(this);
                if (box[0].checked) {
                    element.addClass(box.val());
                } else {
                    element.removeClass(box.val());
                }
            })
        }


        return content;
    };

    /*
     * element: element to change
     * labelText: name to show as label
     * values = array of objects {value: 'a value to change', name: 'name of the value}
     * */
    this.addUniqueAttributeValue = function (Sidebar, element, labelText, name, values)
    {
        var attrFound = false;

        var retVal = this.createCombobox(Sidebar, element, labelText, values,
            function initCallback(testValue)
            {
                var retVal = false;

                if (element.attr(name) == testValue && !attrFound) {
                    attrFound = true;
                    retVal = true;
                }

                return retVal;
            },
            function changeCallback(oldValue, newValue)
            {
                if (newValue != "") {
                    element.attr(name, newValue);
                } else {
                    element.removeAttribute(name);
                }
            });

        return retVal;
    };

    /*
     * element: element to change
     * labelText: name to show as label
     * values = array of objects {value: 'a value to change', name: 'name of the value}
     * */
    this.addUniqueAttribute = function (Sidebar, element, labelText, values)
    {
        var attrFound = false;

        var retVal = this.createCombobox(Sidebar, element, labelText, values,
            function initCallback(testValue)
            {
                var retVal = false;

                if (element.hasAttribute(testValue)) {
                    if (!attrFound) {
                        attrFound = true;
                        retVal = true;
                    }
                    else {
                        element.removeAttribute(testValue)
                    }
                }

                return retVal;
            },
            function changeCallback(oldValue, newValue)
            {
                element.removeAttr(oldValue);
                if (newValue != "") {
                    element.attr(newValue, "");
                }
            });

        return retVal;
    };

    /*
     * element: element to change
     * labelText: name to show as label
     * attribute: name of the attribute the value changes
     * confirm: value only changes when user confirms
     * fileSelect: user can only select file from server
     * pageSelect: user can select a page url from the sitemap
     * */
    this.addValueAttribute = function (Sidebar, element, labelText, placeholderText, attribute, confirm, fileSelect, pageSelect)
    {
        var inputActions = {};

        if (fileSelect) {
            var fileSelectOptions = {};
            fileSelectOptions[SidebarUtils.TEXT_INPUT_ACTION_OPTION_ONSELECT] = function (event, input)
            {
                // Define variable so we can access it after of or cancel
                var sidebarWidth = $("." + Constants.PAGE_SIDEBAR_CLASS).outerWidth();

                var finderOptions = {};
                finderOptions.onSelect = function (selectedFileUrls)
                {
                    if (selectedFileUrls.length > 0) {
                        var fileUrl = selectedFileUrls[0];
                        if (fileUrl.charAt(0) !== "/") {
                            fileUrl = "/" + fileUrl;
                        }

                        input.val(fileUrl);
                        input.change();
                        input.focus();
                    }
                    Sidebar.unloadFinder();
                    // restore sidebar width
                    Sidebar.setSidebarWidth(sidebarWidth);
                };

                finderOptions.onCancel = function ()
                {
                    Sidebar.unloadFinder();
                    // restore sidebar width
                    Sidebar.setSidebarWidth(sidebarWidth);
                };

                Sidebar.loadFinder(finderOptions);
                // save sidebar width
                sidebarWidth = $("." + Constants.PAGE_SIDEBAR_CLASS).outerWidth();
                var windowWidth = $(window).width();

                if (windowWidth / 2 > sidebarWidth) {
                    Sidebar.setSidebarWidth(windowWidth / 2);
                }
            };

            inputActions["Select file from server..."] = fileSelectOptions;
        }

        if (pageSelect) {
            var pageSelectOptions = {};
            pageSelectOptions[SidebarUtils.TEXT_INPUT_ACTION_OPTION_DISABLE] = true;
            pageSelectOptions[SidebarUtils.TEXT_INPUT_ACTION_OPTION_ONSELECT] = function (event, input)
            {
                alert("onSelect");
            };

            inputActions["Lookup page address (coming soon)"] = pageSelectOptions;
        }

        var content = this.createTextInput(Sidebar, element, function ()
            {
                return element.attr(attribute);
            }, function (val)
            {
                return element.attr(attribute, val);
            },
            labelText, placeholderText, confirm, inputActions
        );

        return content;
    };

    /**
     * element: element to change
     * labelText: name to show as label
     * placeholderText: string to show as placeholder
     * confirm: value only changes when user clicks apply button
     **/
    this.addValueHtml = function (Sidebar, element, labelText, placeholderText, confirm)
    {
        return this.createTextInput(Sidebar, element, function ()
        {
            return element.html();
        }, function (val)
        {
            return element.html(val);
        }, labelText, placeholderText, confirm);
    };

    /**
     * element: element to change
     * getterFunction: the function to use to get the value we're changing
     * setterFunction: the function to use to set the value we're changing
     * labelText: name to show as label
     * placeholderText: string to show as placeholder
     * confirm: value only changes when user clicks apply button
     **/
    this.createTextInput = function (Sidebar, element, getterFunction, setterFunction, labelText, placeholderText, confirm, dropdownActions)
    {
        var id = Commons.generateId();

        var resetBtn = null;
        var selectBtn = null;

        var formGroup = $('<div class="form-group"></div>');
        var label = ($('<label for="' + id + '">' + labelText + '</label>')).appendTo(formGroup);
        var inputGroup = $('<div class="input-group"></div>').appendTo(formGroup);
        var input = $('<input ' + id + ' type="text" class="form-control" placeholder="' + placeholderText + '">').appendTo(inputGroup);

        var oldVal = '';
        if (getterFunction) {
            oldVal = getterFunction();
            if (!oldVal) {
                oldVal = '';
            }
        }
        input.attr(SidebarUtils.OLD_VAL_ATTR, oldVal);
        input.val(oldVal);
        input.change();

        var inputActions = $('<div class="input-actions right"/>').appendTo(inputGroup);

        //check if we need to show the reset button
        if (input.attr(SidebarUtils.OLD_VAL_ATTR) !== '') {
            resetBtn = $('<a title="Reset value" class="input-btn input-btn-reset"><i class="fa fa-rotate-left"></a>').appendTo(inputActions);
            resetBtn.click(function (e)
            {
                input.val(input.attr(SidebarUtils.OLD_VAL_ATTR));
                input.change();
                input.focus();
            });
            //don't let input lose focus when the button is clicked
            resetBtn.mousedown(function (e)
            {
                return false;
            });
        }

        //append the clear button
        var clearBtn = $('<a title="Clear value" class="input-btn input-btn-clear"><i class="fa fa-times"></a>').appendTo(inputActions);
        input.on("change keyup focus", function (event)
        {
            if (input.val() == null || input.val() == '') {
                clearBtn.removeClass("show");
            }
            else {
                clearBtn.addClass("show");
            }

            if (resetBtn) {
                if (input.val() === input.attr(SidebarUtils.OLD_VAL_ATTR)) {
                    resetBtn.removeClass("show");
                }
                else {
                    resetBtn.addClass("show");
                }
            }

            if (selectBtn) {
                selectBtn.addClass("show");
            }

            if (event.type !== "focus" && !confirm && setterFunction) {
                setterFunction(input.val());
            }
        });
        input.on("blur", function (event)
        {
            if (clearBtn) {
                clearBtn.removeClass("show");
            }
            if (resetBtn) {
                resetBtn.removeClass("show");
            }
            if (selectBtn) {
                selectBtn.removeClass("show");
            }
        });

        //don't let input lose focus when the button is clicked
        clearBtn.mousedown(function (e)
        {
            return false;
        });
        clearBtn.click(function (e)
        {
            input.val('');
            input.change();
            input.focus();
        });

        //check if there are extra actions (next to reset and clear)
        if (dropdownActions) {
            var dropdownOptions = $('<ul class="dropdown-menu dropdown-menu-right"/>');
            $.each(dropdownActions, function (key, value)
            {
                var option = $('<li />').appendTo(dropdownOptions);
                var link = $('<a href="javascript:void(0)">' + key + '</a>').appendTo(option);
                if (value[SidebarUtils.TEXT_INPUT_ACTION_OPTION_DISABLE] === true) {
                    option.addClass("disabled");
                }
                //don't add the event handler when the link is disabled
                else {
                    if (value[SidebarUtils.TEXT_INPUT_ACTION_OPTION_ONSELECT]) {
                        link.click(function (event)
                        {
                            //let's pass the input field so the function knows where to put the result
                            value[SidebarUtils.TEXT_INPUT_ACTION_OPTION_ONSELECT](event, input);
                        });
                    }
                }
            });

            if (dropdownOptions.children().length) {
                selectBtn = $('<a title="More actions" class="input-btn input-btn-actions" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false"><i class="fa fa-search"></a>').appendTo(inputActions);
                //don't let input lose focus when the button is clicked
                selectBtn.mousedown(function (e)
                {
                    return false;
                });

                inputActions.append(dropdownOptions);
            }
        }

        if (confirm) {
            var actionsGroup = $('<div class="input-group actions"></div>').appendTo(formGroup);
            var applyBtn = $('<a class="btn btn-sm btn-primary"><i class="fa fa-check"></i> Apply</a>').appendTo(actionsGroup);
            applyBtn.click(function (event)
            {
                if (setterFunction) {
                    setterFunction(input.val());
                }
            });
        }
        else {
            //implemented in the input event handler
        }

        return formGroup;
    };
    this.createCombobox = function (Sidebar, element, labelText, values, initCallback, changeCallback)
    {
        // Create selectbox to add to sidebar
        var id = Commons.generateId();
        var content = $('<div class="form-group" />');
        content.append($('<label for="' + id + '">' + labelText + '</label>'));
        var dropdown = $('<div class="dropdown"/>').appendTo(content);
        var button = $('<button class="btn btn-default dropdown-toggle" type="button" id="' + id + '" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true"><span class="text">-----</span>&#160;<span class="caret"></span></button>').appendTo(dropdown);

        // Create values inside selectbox and see which one to select
        var classFound = false;
        var dropdownMenu = $('<ul class="dropdown-menu" aria-labelledby="' + id + '"/>').appendTo(dropdown);
        for (var i = 0; i < values.length; i++) {
            var c = values[i];
            var li = $('<li />').appendTo(dropdownMenu);
            var a = $('<a data-value="' + c.value + '">' + c.name + '</a>').appendTo(li);

            a.click(function (event)
            {
                var combo = $(this).parents(".dropdown").find('.btn');
                var text = $(this).text();
                var newValue = $(this).data('value');
                var oldValue = combo.val();

                //make bootstrap dropdown behave like a regular <select>
                combo.find('.text').text(text);
                combo.val(newValue);
                //save the selection to the dropdown menu
                $(this).parents(".dropdown").find('li').removeClass("active");
                $(this).parents("li").addClass("active");

                if (changeCallback) {
                    changeCallback(oldValue, newValue);
                }
            });

            if (initCallback) {
                var activate = initCallback(c.value);
                if (activate === true) {
                    a.click();
                }
            }
        }

        return content;
    };

}]);
