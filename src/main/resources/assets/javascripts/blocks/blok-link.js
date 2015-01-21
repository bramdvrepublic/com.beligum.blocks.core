/**
 * Created by wouter on 19/12/14.
 */

blocks.plugin("blocks.core.bloklink", ["blocks.core.Admin", function(Admin) {
    var dialogContent = $('<div class="form" role="form"><div class="form-group">' +
        '<label for="colorselect">Kleur</label>' +
        '<select class="form-control" id="colorselect">' +
        '<option value="">Roze</option>'+
        '<option value="bgblue">Blauw</option>'+
        '<option value="bgbrown">Bruin</option>'+
        '<option value="bgdarkblue">Donker blauw</option>'+
        '<option value="bggreen">Groen</option>'+
        '<option value="bgorange">Oranje</option>'+
        '<option value="bgred">Rood</option>'+
        '</select></div>' +
        '' +
        '<div class="form-group">' +
        '<label for="colorselect">Url</label>' +
        '<input type="text" class="form-control"  id="linkurl" value="" />' +
        '</div>' +
        '</div>');



    Admin.register(
        {
            enabled: function(block) {
                var retVal = (block.element.attr("typeof") == "exhibition") || (block.element.attr("typeof") == "experience") || (block.element.attr("typeof") == "bordered-link");
                if (retVal) {
                    var a = block.element.children("a").first();
                    var link = a.attr("href");
                    var input = dialogContent.find("#linkurl");
                    input.val(link);
                }
                return retVal;
            },

            callback: function(block, element, content) {
                var color = content.find("#colorselect").val();
                var el = block.element.find(".square-inner");
                el.removeClass("bgblue");
                el.removeClass("bgbrown");
                el.removeClass("bgdarkblue");
                el.removeClass("bggreen");
                el.removeClass("bgorange");
                el.removeClass("bgred");
                el.addClass(color);

                var a = block.element.children("a").first();
                var url = content.find("#linkurl").val();
                a.attr("href", url);

            },
            element: function() {
                return dialogContent
            },
            title: "Kies de kleur van de blok"
        }
    );

}]);
