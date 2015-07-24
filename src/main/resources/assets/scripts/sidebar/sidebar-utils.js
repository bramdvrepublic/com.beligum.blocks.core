/**
 * Created by wouter on 18/06/15.
 */
base.plugin("blocks.core.SidebarUtils", ["constants.blocks.common", "blocks.finder", "blocks.core.Frame", function (Constants, Finder, Frame)
{

    var Plugin = this;

    /*
     * element: element to change
     * label: name to show as label
     * values = array of objects {value: 'a value to change', name: 'name of the value}
     * */
    this.addUniqueClass = function (element, label, values)
    {
        // Create selectbox to add to sidebar
        var id = Plugin.makeid();
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
        var id = Plugin.makeid();
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
        var id = Plugin.makeid();
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
        var id = Plugin.makeid();
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
     * textSelect: user can manipulate the edit field
     * serverSelect: user can only select file from server
     * url: user can select a local url from tree
     * */
    this.addValueAttribute = function (element, label, name, confirm, disabled, serverSelect, url, SideBar)
    {
        var id = Plugin.makeid();
        var container = $("<div />");
        var form = $("<div class='form-inline' />");
        container.append(form);
        var content = $('<div class="form-group" />');

        //var group = $('<div class="input-group" />');
        content.append($('<label for="' + id + '">' + label + ' </label>'));
        var input = $('<input "' + id + '" type="text" class="form-control" />');
        if (!disabled) {
            input.attr("disabled", "");
        }

        if (element.hasAttribute(name) != null) {
            input.val(element.attr(name));
        }
        content.append(input);

        var oldvalue = input.val();
        if (confirm == true) {
            var cancel = $('<a class="input-btn-clear"><i class="fa fa-times"></i></a>');
            var ok = $('<button class="btn btn-primary"><i class="fa fa-check"></i></button>');
            content.append(cancel)/*.append(ok)*/;

            var form2 = $("<div />");
            container.append(form2);
            form2.append(ok);

            input.on("change keyup", function (e)
            {
                if (input.val()==null || input.val()=='') {
                    cancel.removeClass("show");
                }
                else {
                    cancel.addClass("show");
                }
            });
            cancel.click(function (e)
            {
                input.val(oldvalue);
                input.change();
                input.focus();
            });

            ok.click(function (e)
            {
                oldvalue = input.val();
                input.val(oldvalue);
                element.attr(name, oldvalue);
            });
        }

        if (!confirm) {
            input.keyup(function (e)
            {
                element.attr(name, input.val());
                oldvalue = input.val();
            });
        }

        if (serverSelect) {
            var fileButton = $('<button class="btn btn-primary"><i class="fa fa-file-o"></i></button>');
            form.append(fileButton);

            //var close = function() {
            //    $("." + Constants.PAGE_CONTENT_CLASS).show();
            //    $("." + Constants.BLOCKS_START_BUTTON).show();
            //    $('.' + Constants.PAGE_SIDEBAR_CLASS + ' a[href="#' + Constants.SIDEBAR_CONTEXT_ID +'"]').tab('show')
            //};

            // Define variable so we can access it after of or cancel
            var sidebarWidth = $("." + Constants.PAGE_SIDEBAR_CLASS).outerWidth();

            var finderOptions = {};
            finderOptions.onSelect = function(files) {
                if (files.length > 0) {
                    var file = files[0];
                    if (file.charAt(0) !== "/") {
                        file = "/" + file;
                    }
                    file = Constants.ASSETS_FOLDER + file;
                    input.val(file);
                    element.attr(name, file);
                }
                SideBar.refresh();
                // restore sidebar width
                Frame.setSidebarWidth(sidebarWidth);
            };

            finderOptions.onCancel = function() {
                SideBar.refresh();
                // restore sidebar width
                Frame.setSidebarWidth(sidebarWidth);
            };

            fileButton.click(function ()
            {
                loadFinder(finderOptions);
                // save sidebar width
                sidebarWidth = $("." + Constants.PAGE_SIDEBAR_CLASS).outerWidth();
                var windowWidth = $(window).width();

                if(windowWidth / 2 > sidebarWidth) {
                    Frame.setSidebarWidth(windowWidth / 2);
                }
            });

            form.prepend(content);

        }

        return container;
    };

    /*
     * element: element to change
     * label: name to show as label
     * confirm: value only changes when user confirms
     * */
    this.addValueHtml = function (element, label, confirm)
    {
        var id = Plugin.makeid();
        var content = $("<div class='form-group' />");
        content.append($('<label for="' + id + '">' + label + '</label>'));
        var group = $('<div class="input-group" />');
        var input = $('<input id="' + id + '" type="text" class="form-control" />');

        input.val(element.html());
        content.append(group.append(input));

        var oldvalue = input.val();
        if (confirm == true) {
            var cancel = $('<div class="input-group-addon"><i class="fa fa-times" style="color:red"></i></div>');
            var ok = $('<div class="input-group-addon"><i class="fa fa-check" style="color:green"></i></div>');
            group.append(cancel).append(ok);

            cancel.click(function (e)
            {
                input.val(oldvalue);
            });

            ok.click(function (e)
            {
                oldvalue = input.val();
                input.val(oldvalue);
                element.html(oldvalue);
            });
        }

        if (!confirm) {
            input.keyup(function (e)
            {
                element.html(input.val());
                oldvalue = input.val();

            });
        }

        return content;
    };

    this.makeid = function ()
    {
        var text = "";
        var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (var i = 0; i < 5; i++)
            text += possible.charAt(Math.floor(Math.random() * possible.length));

        return text;
    };

    var loadFinder = function(options) {
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
