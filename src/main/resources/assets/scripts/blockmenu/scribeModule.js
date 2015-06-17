/**
 * Created by wouter on 12/06/15.
 */

base.plugin("blocks.core.Editor", [function() {
    var Editor = this;

    var editors = {};
    var Scribe = this;

    this.scribe = null;
    this.toolbar = null;

    this.toolbarElement = $("<div/>").load("/debug/toolbar");

    require({
        paths: {
            'scribe': '/assets/scripts/scribe/scribe',
            'scribe-plugin-toolbar': '/assets/scripts/scribe/scribe-plugin-toolbar'
        }
    }, ['scribe', 'scribe-plugin-toolbar'], function (ScribeClass, toolbar) {
            //var scribeElement = document.querySelector('.scribe');
            // Create an instance of Scribe
            Scribe.scribe = ScribeClass;
            Scribe.toolbar = toolbar;

        });

    this.getEditor = function(element) {
        var editor = editors[element];
        if (editor == null) {
            editor = new Scribe.scribe(element.first()[0]);
            editors[element] = editor;
            editor.use(Scribe.toolbar(Editor.toolbarElement[0]));
        }

        return editor;
    }
}]);