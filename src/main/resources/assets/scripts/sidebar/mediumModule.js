/**
 * Created by wouter on 12/06/15.
 */
base.plugin("blocks.core.MediumEditor", ["blocks.core.MediumEditorExtensions", function (Extensions)
{
    var MediumModule = this;

    var Editor = null;
    var defaultToolbarOptions = [Extensions.StylesPickerButton.NAME, 'bold', 'italic', 'underline', 'strike-through', 'superscript', 'anchor', 'orderedlist', 'unorderedlist', 'justifyLeft', 'justifyCenter', 'justifyRight'];

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
        var toolbarOptions = {};

        options.buttonLabels = 'fontawesome';
        options.extensions = {};
        options.extensions[Extensions.StylesPickerButton.NAME] = new Extensions.StylesPickerButton({
            parentModule: MediumModule
        });

        // Always show toolbar
        toolbarOptions.static = true;
        toolbarOptions.updateOnEmptySelection = true;
        toolbarOptions.buttons = defaultToolbarOptions;
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
        defaultToolbarOptions = buttonArray;
    };

}]);
