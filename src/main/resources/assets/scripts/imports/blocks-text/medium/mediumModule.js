/**
 * Created by wouter on 12/06/15.
 */
base.plugin("blocks.core.MediumEditor", ["blocks.core.MediumEditorExtensions", function (Extensions)
{
    var MediumModule = this;

    var Editor = null;
    //default values, overridable
    var toolbarButtons = [Extensions.StylesPicker.NAME, 'bold', 'italic', 'underline', 'strike-through', 'superscript', Extensions.LinkInput.NAME, 'orderedlist', 'unorderedlist', 'justifyLeft', 'justifyCenter', 'justifyRight'];
    var stylePickerStyles = [];
    var toolbarOptions = {};

    this.getToolbarElement = function ()
    {
        var retVal = null;
        if (Editor != null) {
            retVal = Editor.getExtensionByName("toolbar").getToolbarElement();
        }
        return retVal;
    };

    this.getEditor = function (element, inline)
    {
        if (Editor != null) {
            MediumModule.removeEditor();
        }

        var options = {};
        toolbarOptions = {};

        options.buttonLabels = 'fontawesome';
        var stylePicker = new Extensions.StylesPicker({});
        stylePicker.setStyles(stylePickerStyles);

        options.extensions = {};
        options.extensions[Extensions.StylesPicker.NAME] = stylePicker;
        options.extensions[Extensions.LinkInput.NAME] = new Extensions.LinkInput({});

        // Always show toolbar
        toolbarOptions.static = true;
        toolbarOptions.updateOnEmptySelection = true;
        toolbarOptions.buttons = toolbarButtons;
        toolbarOptions.align = 'left';

        options.disableReturn = inline;
        options.toolbar = toolbarOptions;
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
