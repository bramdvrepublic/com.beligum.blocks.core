/**
 * Created by bas on 11.02.15.
 * Customization of modals can be done using data-... attributes
 * Needs jQuery.js
 */

function customize(modal){
    modal.on('show.bs.modal', function (event) {
        var causeElement = $(event.relatedTarget);
        var onConfirm = causeElement.data("on-confirm");
        var bodyText = causeElement.data("body-text");
        var confirmText = causeElement.data("confirm-text");
        var title = causeElement.data("title");
        var modal = $(this);
        if(onConfirm) {
            modal.find("#confirm").attr("onclick", onConfirm);
        }
        if(bodyText){
            modal.find(".modal-body").text(bodyText);
        }
        if(confirmText){
            modal.find("#confirm").text(confirmText);
        }
        if(title){
            modal.find("#confirmModalLabel").text(title);
        }
    });
}

jQuery(document).ready(function($) {
    customize($('#confirmModal'));
    customize($('#successModal'));
});
