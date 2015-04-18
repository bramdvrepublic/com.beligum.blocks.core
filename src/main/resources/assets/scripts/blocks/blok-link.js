/**
 * Created by wouter on 19/12/14.
 *
 * Test for plugin. Edit a MOT block link
 */

base.plugin("blocks.core.bloklink", ["blocks.core.Edit", "blocks.core.Broadcaster", "blocks.core.Sitemap", function (Edit, Broadcaster, Sitemap)
{

    /*
    * The content for the dialog: for testing written in jquery
    * */
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
        '<div class="input-group">' +
        '<input type="text" class="form-control"  id="linkurl" value="" />' +
        '<span class="input-group-btn"><button class="btn btn-default select-url" type="button" >Select</button></span>' +
        '</div>' +
        '</div>' +
        '</div>');



    $(document).on("click", ".form-group .select-url", function() {
        Sitemap.urlsModal();
    });



    var doEditBlockLink = function(blockEvent) {
        BootstrapDialog.show({
            title: "Change Link Block",
            message: dialogContent,
            buttons: [
                {id: 'btn-close',
                    label: 'Cancel',
                    action: function(dialogRef){
                        Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);
                        dialogRef.close();
                    }},
                {
                    id: 'btn-ok',
                    icon: 'glyphicon glyphicon-check',
                    label: 'Ok',
                    cssClass: 'btn-primary',
                    action: function(dialogRef){
                        var block = blockEvent.block.current;
                        var color = dialogRef.$modalBody.find("#colorselect").val();
                        var el = block.element.find(".square-inner");
                        el.removeClass("bgblue");
                        el.removeClass("bgbrown");
                        el.removeClass("bgdarkblue");
                        el.removeClass("bggreen");
                        el.removeClass("bgorange");
                        el.removeClass("bgred");
                        el.addClass(color);


                        var a = block.element.children("a").first();
                        var url = dialogRef.$modalBody.find("#linkurl").val();
                        a.attr("href", url);
                        Broadcaster.send(Broadcaster.EVENTS.END_EDIT_FIELD);
                        dialogRef.close();
                    }

                }]
        })
    };

    /*
    * We register for editing. When clicked in the blocksmenu our dialog will be shown (element),
    * when clicked ok in the dialog our callback is called.
    * enabled checks if we want to edit this block. This is used by the dispatcher to call our plugin for the right block
    * */
    Edit.registerByType("exhibition", doEditBlockLink);


}]);
