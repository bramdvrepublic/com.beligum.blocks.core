/**
 * Created by wouter on 18/06/15.
 */
base.plugin("blocks.core.Plugin-Utils", [ function () {

    var Plugin = this;

    this.addUniqueClass = function(element, label, values) {
        // Create selectbox to add to sidebar
        var id = Plugin.makeid();
        var content = $("<div class='form-group' />");
        content.append($('<label for="'+ id +'">' + label + '</label>'));
        var select = $("<select id='"+id+"' class='form-control'/>");
        content.append(select);

        // Create values inside selectbox and see which one to select
        var classFound = false;
        var selected = null;
        for (var i=0; i < values.length; i++) {
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

        select.change(function(e) {
            if (selected != null) {
                element.removeClass(selected);
            }
            selected = select.val()
            element.addClass(selected);
        })

        return content;
    };

    this.addOptionalClass = function(element, label, values) {
// Create selectbox to add to sidebar
        var id = Plugin.makeid();
        var content = $("<div class='form-group' />");
        content.append("<label>"+label+"</label>");

        // Create checkboxes for each value
        var classFound = null;
        for (var i=0; i < values.length; i++) {
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
            input.change(function(e) {
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

    this.addUniqueAttributeValue = function(element, label, name, values) {
        // Create selectbox to add to sidebar
        var id = Plugin.makeid();
        var content = $("<div class='form-group' />");
        content.append($('<label for="'+ id +'">' + label + '</label>'));
        var select = $("<select class='form-control'/>");
        content.append(select);

        // Create values inside selectbox and see which one to select
        var attrFound = null;
        var selected = null;
        for (var i=0; i < values.length; i++) {
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

        select.change(function(e) {
            selected = select.val();
            if (selected != "") {
                element.attr(name, selected);
            } else {
                element.removeAttribute(name);
            }
        })

        return content;
    };

    this.addUniqueAttribute = function(element, label, values) {
        // Create selectbox to add to sidebar
        var id = Plugin.makeid();
        var content = $("<div class='form-group' />");
        content.append($('<label for="'+ id +'">' + label + '</label>'));
        var select = $("<select class='form-control'/>");
        content.append(select);

        // Create values inside selectbox and see which one to select
        var attrFound = null;
        var selected = null;
        for (var i=0; i < values.length; i++) {
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

        select.change(function(e) {
            element.removeAttr(selected);
            selected = select.val();
            if (selected != "") {
                element.attr(selected, "");
            }
        })

        return content;
    };

    this.addValueAttribute = function(element, label, name, confirm) {
        var id = Plugin.makeid();
        var content = $("<div class='form-group' />");
        content.append($('<label for="'+ id +'">' + label + '</label>'));
        var group = $('<div class="input-group" />');
        var input = $('<input "'+id+'" type="text" class="form-control" />');

        if (element.hasAttribute(name) != null) {
            input.val(element.attr(name));
        }
        content.append(group.append(input));

        var oldvalue = input.val();
        if (confirm == true) {
            var cancel = $('<div class="input-group-addon"><i class="fa fa-times" style="color:red"></i></div>');
            var ok = $('<div class="input-group-addon"><i class="fa fa-check" style="color:green"></i></div>');
            group.append(cancel).append(ok);

            cancel.click(function(e) {
                input.val(oldvalue);
            });

            ok.click(function(e) {
                oldvalue = input.val();
                input.val(oldvalue);
                element.attr(name, oldvalue);
            });
        }

        if (!confirm) {
            input.keyup(function(e) {
                element.attr(name, input.val());
                oldvalue = input.val();

            });
        }

        return content;
    };

    this.addValueHtml = function(element, label, confirm) {
        var id = Plugin.makeid();
        var content = $("<div class='form-group' />");
        content.append($('<label for="'+ id +'">' + label + '</label>'));
        var group = $('<div class="input-group" />');
        var input = $('<input "'+id+'" type="text" class="form-control" />');

        input.val(element.html());
        content.append(group.append(input));

        var oldvalue = input.val();
        if (confirm == true) {
            var cancel = $('<div class="input-group-addon"><i class="fa fa-times" style="color:red"></i></div>');
            var ok = $('<div class="input-group-addon"><i class="fa fa-check" style="color:green"></i></div>');
            group.append(cancel).append(ok);

            cancel.click(function(e) {
                input.val(oldvalue);
            });

            ok.click(function(e) {
                oldvalue = input.val();
                input.val(oldvalue);
                element.html(oldvalue);
            });
        }

        if (!confirm) {
            input.keyup(function(e) {
                element.html(input.val());
                oldvalue = input.val();

            });
        }

        return content;
    };

    this.makeid = function()
    {
        var text = "";
        var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for( var i=0; i < 5; i++ )
            text += possible.charAt(Math.floor(Math.random() * possible.length));

        return text;
    }

}]);
