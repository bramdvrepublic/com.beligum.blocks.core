/**
 * Created by wouter on 18/06/15.
 */
base.plugin("blocks.core.SidebarUtils", ["constants.blocks.common", "blocks.finder", function (Constants, Finder)
{
    var SidebarUtils = this;

    this.oldValAttr = 'data-reset';

    /*
     * element: element to change
     * label: name to show as label
     * values = array of objects {value: 'a value to change', name: 'name of the value}
     * */
    this.addUniqueClass = function (element, label, values)
    {
        // Create selectbox to add to sidebar
        var id = SidebarUtils.makeid();
        var content = $("<div class='form-group' />");
        content.append($('<label for="' + id + '">' + label + '</label>'));
        var select = $("<select id='" + id + "' class='form-control'/>");
        content.append(select);

        // Create values inside selectbox and see which one to select
        var classFound = false;
        var selected = null;
        for (var i = 0; i < values.length; i++) {
            var c = values[i];
            var option = $("<option />").attr("value", c.value).html(c.name);
            if (element.hasClass(c.value) && !classFound) {
                option.attr("selected", "selected");
                selected = c.value;
                classFound = true;
            } else if (element.hasClass(c.value) && classFound) {
                // If more then 1 value is selected, only keep the first value
                element.removeClass(c.value);
            }
            select.append(option);
        }

        // Callback for when the select box changes

        select.change(function (e)
        {
            if (selected != null) {
                element.removeClass(selected);
            }
            selected = select.val()
            element.addClass(selected);
        })

        return content;
    };

    /*
     * element: element to change
     * label: name to show as label
     * values = array of objects {value: 'a value to change', name: 'name of the value}
     * */
    this.addOptionalClass = function (element, label, values)
    {
// Create selectbox to add to sidebar
        var id = SidebarUtils.makeid();
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
     * label: name to show as label
     * values = array of objects {value: 'a value to change', name: 'name of the value}
     * */
    this.addUniqueAttributeValue = function (element, label, name, values)
    {
        // Create selectbox to add to sidebar
        var id = SidebarUtils.makeid();
        var content = $("<div class='form-group' />");
        content.append($('<label for="' + id + '">' + label + '</label>'));
        var select = $("<select class='form-control'/>");
        content.append(select);

        // Create values inside selectbox and see which one to select
        var attrFound = null;
        var selected = null;
        for (var i = 0; i < values.length; i++) {
            var c = values[i];
            var option = $("<option />").attr("value", c.value).html(c.name);
            if (element.attr(name) == c.value && !attrFound) {
                option.attr("selected", "selected");
                selected = c.value;
                attrFound = true;
            }
            select.append(option);
        }

        // Callback for when the select box changes

        select.change(function (e)
        {
            selected = select.val();
            if (selected != "") {
                element.attr(name, selected);
            } else {
                element.removeAttribute(name);
            }
        })

        return content;
    };

    /*
     * element: element to change
     * label: name to show as label
     * values = array of objects {value: 'a value to change', name: 'name of the value}
     * */
    this.addUniqueAttribute = function (element, label, values)
    {
        // Create selectbox to add to sidebar
        var id = SidebarUtils.makeid();
        var content = $("<div class='form-group' />");
        content.append($('<label for="' + id + '">' + label + '</label>'));
        var select = $("<select class='form-control'/>");
        content.append(select);

        // Create values inside selectbox and see which one to select
        var attrFound = null;
        var selected = null;
        for (var i = 0; i < values.length; i++) {
            var c = values[i];
            var option = $("<option />").attr("value", c.value).html(c.name);
            if (element.hasAttribute(c.value)) {
                if (!attrFound) {
                    option.attr("selected", "selected");
                    attrFound = true;
                    selected = c.value;
                } else {
                    element.removeAttribute(c.value)
                }
            }
            select.append(option);
        }

        // Callback for when the select box changes

        select.change(function (e)
        {
            element.removeAttr(selected);
            selected = select.val();
            if (selected != "") {
                element.attr(selected, "");
            }
        })

        return content;
    };

    /*
     * element: element to change
     * label: name to show as label
     * name: name of the attriubute the value changes
     * confirm: value only changes when user confirms
     * serverSelect: user can only select file from server
     * url: user can select a local url from tree
     * */
    this.addValueAttribute = function (element, label, placeholderText, name, confirm, serverSelect, url, SideBar)
    {
        //TODO pass the default value (for reset)
        var content = this.addValueHtml(null, label, placeholderText, confirm);

        //TODO a little bit hacky, but does the job
        var input = content.find('input');
        input.on("change keyup focus", function (event)
        {
            element.attr(name, input.val());
            //input.attr(SidebarUtils.oldValAttr, input.val());
        });

        //initialize the input with the value
        input.val(element.attr(name));

        //var oldvalue = input.val();
        //if (confirm == true) {
        //    var cancel = $('<a class="input-btn-clear"><i class="fa fa-times"></i></a>');
        //    var ok = $('<button class="btn btn-primary"><i class="fa fa-check"></i></button>');
        //    container.append(cancel)/*.append(ok)*/;
        //
        //    var form2 = $("<div />");
        //    container.append(form2);
        //    form2.append(ok);
        //
        //    input.on("change keyup", function (e)
        //    {
        //        if (input.val()==null || input.val()=='') {
        //            cancel.removeClass("show");
        //        }
        //        else {
        //            cancel.addClass("show");
        //        }
        //    });
        //    cancel.click(function (e)
        //    {
        //        input.val(oldvalue);
        //        input.change();
        //        input.focus();
        //    });
        //
        //    ok.click(function (e)
        //    {
        //        oldvalue = input.val();
        //        input.val(oldvalue);
        //        element.attr(name, oldvalue);
        //    });
        //}
        //
        //if (!confirm) {
        //    input.keyup(function (e)
        //    {
        //        element.attr(name, input.val());
        //        oldvalue = input.val();
        //    });
        //}
        //
        //if (serverSelect) {
        //    var fileButton = $('<button class="btn btn-primary"><i class="fa fa-file-o"></i></button>');
        //    form.append(fileButton);
        //
        //    //var close = function() {
        //    //    $("." + Constants.PAGE_CONTENT_CLASS).show();
        //    //    $("." + Constants.BLOCKS_START_BUTTON).show();
        //    //    $('.' + Constants.PAGE_SIDEBAR_CLASS + ' a[href="#' + Constants.SIDEBAR_CONTEXT_ID +'"]').tab('show')
        //    //};
        //
        //    // Define variable so we can access it after of or cancel
        //    var sidebarWidth = $("." + Constants.PAGE_SIDEBAR_CLASS).outerWidth();
        //
        //    var finderOptions = {};
        //    finderOptions.onSelect = function(files) {
        //        if (files.length > 0) {
        //            var file = files[0];
        //            if (file.charAt(0) !== "/") {
        //                file = "/" + file;
        //            }
        //            file = Constants.ASSETS_FOLDER + file;
        //            input.val(file);
        //            element.attr(name, file);
        //        }
        //        SideBar.refresh();
        //        // restore sidebar width
        //        Frame.setSidebarWidth(sidebarWidth);
        //    };
        //
        //    finderOptions.onCancel = function() {
        //        SideBar.refresh();
        //        // restore sidebar width
        //        Frame.setSidebarWidth(sidebarWidth);
        //    };
        //
        //    fileButton.click(function ()
        //    {
        //        loadFinder(finderOptions);
        //        // save sidebar width
        //        sidebarWidth = $("." + Constants.PAGE_SIDEBAR_CLASS).outerWidth();
        //        var windowWidth = $(window).width();
        //
        //        if(windowWidth / 2 > sidebarWidth) {
        //            Frame.setSidebarWidth(windowWidth / 2);
        //        }
        //    });
        //
        //    form.prepend(container);
        //
        //}

        return content;
    };

    /*
     * element: element to change
     * label: name to show as label
     * confirm: value only changes when user confirms
     * */
    this.addValueHtml = function (element, labelText, placeholderText, confirm)
    {
        var id = SidebarUtils.makeid();

        var resetBtn = null;

        var formGroup = $('<div class="form-group"></div>');
        var label = ($('<label for="' + id + '">' + labelText + '</label>')).appendTo(formGroup);
        var inputGroup = $('<div class="input-group"></div>').appendTo(formGroup);
        var input = $('<input ' + id + ' type="text" class="form-control" placeholder="' + placeholderText + '">').appendTo(inputGroup);

        input.attr(SidebarUtils.oldValAttr, '');
        if (element) {
            var oldVal = element.html();
            input.attr(SidebarUtils.oldValAttr, oldVal);
            input.val(oldVal);
        }

        var inputActions = $('<div class="input-actions"/>').appendTo(inputGroup);
        var clearBtn = $('<span class="input-btn-clear"><i class="fa fa-times"></span>').appendTo(inputActions);
        input.on("change keyup focus", function (event)
        {
            if (input.val() == null || input.val() == '') {
                clearBtn.removeClass("show");
            }
            else {
                clearBtn.addClass("show");
            }

            if (resetBtn) {
                if (input.val() === input.attr(SidebarUtils.oldValAttr)) {
                    resetBtn.removeClass("show");
                }
                else {
                    resetBtn.addClass("show");
                }
            }

            if (event.type !== "focus") {
                if (!confirm) {
                    if (element) {
                        element.html(input.val());
                    }
                }
            }
        });
        clearBtn.click(function (e)
        {
            input.val('');
            input.change();
            input.focus();
        });
        if (input.attr(SidebarUtils.oldValAttr) !== '') {
            resetBtn = $('<span class="input-btn-reset"><i class="fa fa-rotate-left"></span>').appendTo(inputActions);
            resetBtn.click(function (e)
            {
                input.val(input.attr(SidebarUtils.oldValAttr));
                input.change();
                input.focus();
            });
        }

        if (confirm) {
            var actionsGroup = $('<div class="input-group actions"></div>').appendTo(formGroup);
            var applyBtn = $('<a class="btn btn-sm btn-primary"><i class="fa fa-check"></i> Apply</a>').appendTo(actionsGroup);
            applyBtn.click(function (event)
            {
                if (element) {
                    element.html(input.val());
                }
            });
        }
        else {
            //implemented in the input event handler
        }

        return formGroup;
    };

    this.makeid = function ()
    {
        var text = "";
        var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (var i = 0; i < 5; i++)
            text += possible.charAt(Math.floor(Math.random() * possible.length));

        return text;
    };

    var loadFinder = function (options)
    {
        var filesContainer = $("#" + Constants.SIDEBAR_CONTEXT_ID);
        filesContainer.empty().addClass(Constants.LOADING_CLASS);

        //TODO maybe not necessary to reload this every time, but it allows us to always present a fresh uptodate view of the server content
        filesContainer.load("/media/finder-inline", function (response, status, xhr)
        {
            if (status == "error") {
                var msg = "Error while loading the finder; ";
                filesContainer.removeClass(Constants.LOADING_CLASS);
                Notification.error(msg + xhr.status + " " + xhr.statusText, xhr);
            }
            else {
                Finder.init(options);
                filesContainer.removeClass(Constants.LOADING_CLASS);
            }
        });
    };

}]);
