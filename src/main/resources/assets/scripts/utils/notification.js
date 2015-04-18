base.plugin("blocks.core.Notification", ["blocks.core.Broadcaster", function (Broadcaster)
{
    //// Todo show/hide overlay
    //var maxIndex = function() {
    //    return Math.max.apply(null,$.map($('body  *'), function(e,n){
    //            if($(e).css('position')=='absolute' || $(e).css('position')=='relative')
    //                return parseInt($(e).css('z-index'))||1 ;
    //        })
    //    );
    //};
    //
    //this.dialog = function(title, message, okFunction, cancelFunction) {
    //    var modal = $("<div class='modal'></div>")/*.css("background-color", "#ffffff")*/;
    //    var modalDialog = $("<div class='modal-dialog'></div>");
    //    var modalContent = $("<div class='modal-content'></div>");
    //    var modalHeader = $("<div class='modal-header'></div>");
    //    var closeIcon = $('<button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>');
    //    var modalTitle = $("<h4 class='modal-title'>" + title + "</h4>");
    //    var modalBody = $("<div class='modal-body'></div>").append($(message));
    //    var modalFooter = $("<div class='modal-footer'></div>");
    //    var closeButton = $('<button type="button" class="btn btn-default" >Cancel</button>');
    //    var okButton = $('<button type="button" class="btn btn-primary confirm">Ok</button>');
    //    modal.css("z-index", (maxIndex() + 2));
    //    modalDialog.css("height", "80%");
    //    modal.css("max-height", "60%");
    //
    //    modalBody.css("overflow", "auto");
    //    modalBody.css("min-height", "10%");
    //    modalBody.css("max-height", "50%");
    //    var hideDialog = function(callback) {
    //        modal.fadeOut(200, function() {
    //            modal.remove();
    //            Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
    //            callback(modalBody);
    //        });
    //    };
    //
    //    if (cancelFunction != null) {
    //        closeButton.on("click", function(event) {
    //            event.stopPropagation();
    //            hideDialog(cancelFunction)});
    //        closeIcon.on("click", function(event) {
    //            event.stopPropagation();
    //            hideDialog(cancelFunction)});
    //        okButton.on("click", function(event) {
    //            event.stopPropagation();
    //            hideDialog(okFunction)}
    //        );
    //        modalFooter.append(closeButton)
    //    } else {
    //        okButton.on("click", function() {
    //            event.stopPropagation();
    //            hideDialog(okFunction)});
    //        closeIcon.on("click", function() {
    //            event.stopPropagation();
    //            hideDialog(function(){})}
    //        );
    //    }
    //
    //    Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
    //    modalFooter.append(okButton);
    //    modal.append(modalDialog.append(modalContent.append(modalHeader.append(closeIcon).append(modalTitle)).append(modalBody).append(modalFooter)));
    //    modal.hide();
    //    modal.addClass("modal-centered");
    //    $("body").append(modal);
    //    modal.css("margin-left", -modal.width()/2)
    //    modal.css("margin-top", -modal.height()/2);
    //    modal.fadeIn(200);
    //
    //};
    //
    //this.alert = function(title, message, okFunction) {
    //    this.dialog(title, message, okFunction);
    //};


    this.error = function(message){
        BootstrapDialog.show({
            title: "Error",
            message: message,
            type: BootstrapDialog.TYPE_DANGER,
            buttons: [
                {
                    label: 'Close',
                    cssClass: 'btn-danger',
                    action: function(dialogRef){
                        dialogRef.close();
                    }
                }
            ]
        })
    }



}]);