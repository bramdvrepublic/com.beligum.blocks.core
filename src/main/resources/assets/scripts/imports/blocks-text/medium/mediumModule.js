/**
 * Created by wouter on 12/06/15.
 */
base.plugin("blocks.core.MediumEditor", ["blocks.core.MediumEditorExtensions", function (Extensions)
{
    var MediumModule = this;

    var Editor = null;

    //default values, overridable
    var toolbarButtons = [Extensions.StylesPicker.NAME, 'bold', 'italic', 'underline', 'strike-through', 'superscript', Extensions.LinkInput.NAME, 'orderedlist', 'unorderedlist', 'justifyLeft', 'justifyCenter', 'justifyRight', 'removeFormat'];
    var toolbarButtonsInline = ['bold', 'italic', 'underline', 'superscript', 'removeFormat'];

    var stylePickerStyles = [];
    var toolbarOptions = {};

    this.getToolbarElement = function ()
    {
        var retVal = null;
        if (Editor != null) {
            var toolbarExt = Editor.getExtensionByName("toolbar");
            if (toolbarExt) {
                retVal = toolbarExt.getToolbarElement();
            }
        }
        return retVal;
    };

    this.getEditor = function (element, inline, hideToolbar)
    {
        if (Editor != null) {
            MediumModule.removeEditor();
        }

        var options = {
            buttonLabels: 'fontawesome'
        };

        var stylePicker = new Extensions.StylesPicker({});
        stylePicker.setStyles(stylePickerStyles);

        options.extensions = {};
        options.extensions[Extensions.StylesPicker.NAME] = stylePicker;
        options.extensions[Extensions.LinkInput.NAME] = new Extensions.LinkInput({});

        if (!hideToolbar) {
            var toolbarOptions = {};

            //enable the toolbar always displaying in the same location relative to the medium-editor element.
            toolbarOptions.static = true;
            //this enables updating the state of the toolbar buttons even when the selection is collapsed (there is no selection, just a cursor)
            toolbarOptions.updateOnEmptySelection = true;
            if (!inline) {
                toolbarOptions.buttons = toolbarButtons;
            } else {
                toolbarOptions.buttons = toolbarButtonsInline;
            }
            toolbarOptions.align = 'left';

            options.toolbar = toolbarOptions;
        }
        else {
            options.toolbar = false;
            //hmmm, this doesn't seem to work very well...
            options.keyboardCommands = false;
        }

        options.disableReturn = inline;

        Editor = new MediumEditor(element[0], options);

        return Editor;
    };

    this.removeEditor = function (element)
    {
        if (Editor != null) {
            Editor.destroy();
        }
        Editor = null;
    };

    this.setToolbarButtons = function (buttonArray)
    {
        toolbarOptions = buttonArray;
    };

    this.setStylePickerStyles = function (newStyles)
    {
        stylePickerStyles = newStyles;
    };

}]);
