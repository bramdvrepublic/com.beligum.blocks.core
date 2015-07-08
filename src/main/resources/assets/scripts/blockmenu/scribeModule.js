/**
 * Created by wouter on 12/06/15.
 */

base.plugin("blocks.core.ScribeEditor", [function() {
    var Editor = this;

    var editors = null;
    var toolbarInstance = null;
    var Scribe = this;

    this.scribe = null;
    this.toolbar = null;
    this.link = null;

    this.toolbarElement = $("<div/>").load("/templates/editor/toolbar");

    require({
        paths: {
            'scribe': '/assets/scripts/scribe/scribe',
            'scribe-plugin-toolbar': '/assets/scripts/scribe/scribe-plugin-toolbar',
            'scribe-plugin-link': '/assets/scripts/scribe/scribe-plugin-link'
        }
    }, ['scribe', 'scribe-plugin-toolbar', 'scribe-plugin-link'], function (ScribeClass, toolbar, link) {
        //var scribeElement = document.querySelector('.scribe');
        // Create an instance of Scribe
        Scribe.scribe = ScribeClass;
        Scribe.toolbar = toolbar;
        Scribe.link = link;
        var i = 0;
    });

    this.getEditor = function(element, inline) {
        if (editors != null) {
            Scribe.removeEditor(element);
        }
        editors = new Scribe.scribe(element.first()[0], {allowBlockElements: inline});
        toolbarInstance = Scribe.toolbar(Editor.toolbarElement[0]);
        editors.use(toolbarInstance);

        editors.use(Scribe.link());


        return editors;
    };

    this.removeEditor = function(element) {
        // Remove all eventlisteners of this element with a trick
        if (scribetoolbar.el != null && scribetoolbar.el != undefined) {
            var el = scribetoolbar.el,
                elClone = el.cloneNode(true);
            if (el.parentNode != null) {
                el.parentNode.replaceChild(elClone, el);
            }
        }
        editors.remove(element);
        //el = element[0],
        //    elClone = el.cloneNode(true);
        //if (el != null && el.parentNode != null) {
        //    el.parentNode.replaceChild(elClone, el);
        //}
    };


}]);