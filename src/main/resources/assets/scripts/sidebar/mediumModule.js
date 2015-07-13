/**
 * Created by wouter on 7/07/15.
 */

/**
 * Created by wouter on 12/06/15.
 */

base.plugin("blocks.core.MediumEditor", ["blocks.core.MediumEditorExtensions", function(Extensions) {
    var Editor = this;
    var defaultToolbarOptions = ['bold', 'italic', 'underline', 'strike-through', 'superscript', 'anchor', 'orderedlist', 'unorderedlist', 'justifyLeft', 'justifyCenter', 'justifyRight', 'styles-picker'];

    this.getToolbarElement = function() {
        var retVal = null;
        if (Editor != null) {
            retVal = Editor.getExtensionByName("toolbar").getToolbarElement();
        }
        return retVal;
    };

    this.getEditor = function(element, inline) {
        var options = {};
        var toolbarOptions = {};

        options.buttonLabels = 'fontawesome';
        options.extensions = {
            "styles-picker": new Extensions.styleExtension()
        };

        // Always show toolbar
        toolbarOptions.static = true;
        toolbarOptions.updateOnEmptySelection = true;

        toolbarOptions.buttons = defaultToolbarOptions;

        options.disableReturn = inline;
        options.toolbar = toolbarOptions;
        Editor = new MediumEditor(element[0], options);
        return Editor;
    };

    this.removeEditor = function(element) {
        Editor.destroy();
        Editor = null;
    };

    this.setToolbarButtons = function(buttonArray) {
        defaultToolbarOptions = buttonArray;
    }


}]);
