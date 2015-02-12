/**
 * Created by bas on 11.02.15.
 * Customization of modals can be done using data-... attributes
 * Needs jQuery.js
 */
var MODALS = {
    MODAL_ON_CONFIRM : "on-confirm",
    MODAL_BODY_TEXT : "body-text",
    MODAL_CONFIRM_TEXT : "confirm-text",
    MODAL_TITLE : "title",
    ERROR : "#errorModal",
    SUCCESS : "#successModal",
    DANGER : "#dangerModal",
    setModalData: function (modal, data) {
        if (data) {
            if (data.onConfirm) {
                modal.find(".confirm").attr("onclick", data.onConfirm);
            }
            if (data.bodyText) {
                modal.find(".modal-body").text(data.bodyText);
            }
            if (data.confirmText) {
                modal.find(".confirm").text(data.confirmText);
            }
            if (data.title) {
                modal.find(".modal-title").text(data.title);
            }
        }
    },
    customize: function (modal) {
        modal.on('show.bs.modal', function (event) {
            var causeElement = $(event.relatedTarget);
            var data = {};
            data.onConfirm = causeElement.data(MODALS.MODAL_ON_CONFIRM);
            data.bodyText = causeElement.data(MODALS.MODAL_BODY_TEXT);
            data.confirmText = causeElement.data(MODALS.MODAL_CONFIRM_TEXT);
            data.title = causeElement.data(MODALS.MODAL_TITLE);
            var modal = $(this);
            MODALS.setModalData(modal, data);
        });
    },
    show: function(modalName, data){
        var modal = $(modalName).modal();
        MODALS.setModalData(modal, data);
        $(modalName).modal('show');
    }
};

jQuery(document).ready(function($) {
    MODALS.customize($(MODALS.DANGER));
    MODALS.customize($(MODALS.ERROR));
});
