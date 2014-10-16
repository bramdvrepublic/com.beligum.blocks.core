/**
 * Created by wouter on 13/10/14.
 */

blocks.module("App", ["blocks.mouseEvent", "blocks.broadcaster", "blocks.elements", "blocks.resizer", "blocks.dragdrop"])
var injector = createInjector(["App"], true);

var app = injector.invoke(["MouseEvent", "Resizer", "DragDropBlocks", function(MouseEvent, Resizer, DragDropBlocks) {
    this.ok = function() {
        return "ok";
    }
}]);


