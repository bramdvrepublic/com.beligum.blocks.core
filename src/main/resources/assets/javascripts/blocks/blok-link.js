/**
 * Created by wouter on 19/12/14.
 */

blocks.plugin("blocks.core.bloklink", ["blocks.core.Admin", function(Admin) {
    var dialogContent = $('<div class="form-inline" role="form"><div class="form-group">' +
        '<select class="form-control" id="colorselect">' +
        '<option value="">Roze</option>'+
        '<option value="bgblue">Blauw</option>'+
        '<option value="bgbrown">Bruin</option>'+
        '<option value="bgdarkblue">Donker blauw</option>'+
        '<option value="bggreen">Groen</option>'+
        '<option value="bgorange">Oranje</option>'+
        '<option value="bgred">Rood</option>'+
        '</select></div></div>');


    Admin.register(
        {
            enabled: function(element) {
                return element.attr("typeof") == "exhibition";
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


            },
            element: dialogContent,
            title: "Kies de kleur van de blok"
        }
    );

}]);
