blocks.plugin("blocks.core.Notification", ["blocks.core.Broadcaster", function(Broadcaster) {
    // Todo show/hide overlay
    this.dialog = function(title, message, okFunction, cancelFunction) {
        var modal = $("<div class='modal'></div>").css("background-color", "#ffffff");
        var modalDialog = $("<div class='modal-dialog'></div>");
        var modalHeader = $("<div class='modal-header'></div>");
        var closeIcon = $('<button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>');
        var modalTitle = $("<h4 class='modal-title'>" + title + "</h4>");
        var modalBody = $("<div class='modal-body'>" + message + "</div>");
        var modalFooter = $("<div class='modal-footer'></div>");
        var closeButton = $('<button type="button" class="btn btn-default" >Cancel</button>');
        var okButton = $('<button type="button" class="btn btn-primary">Ok</button>');

        var hideDialog = function(callback) {
            modal.fadeOut(200, function() {
                modal.remove();
                Broadcaster.send(Broadcaster.EVENTS.ACTIVATE_MOUSE);
                callback(modalBody);
            });
        };

        if (cancelFunction != null) {
            closeButton.on("click", function() {hideDialog(cancelFunction)});
            closeIcon.on("click", function() {hideDialog(cancelFunction)});
            modalFooter.append(closeButton)
        } else {
            okButton.on("click", function() {hideDialog(okFunction)});
            closeIcon.on("click", function() {hideDialog(okFunction)});
        }

        Broadcaster.send(Broadcaster.EVENTS.DEACTIVATE_MOUSE);
        modalFooter.append(okButton);
        modal.append(modalDialog.append(modalHeader.append(closeIcon).append(modalTitle)).append(modalBody).append(modalFooter));
        modal.hide();
        modal.addClass("modal-centered");
        $("body").append(modal);
        modal.css("margin-left", -modal.width()/2)
        modal.css("margin-top", -modal.height()/2);
        modal.fadeIn(200);

    };

    this.alert = function(title, message, okFunction) {
        this.dialog(title, message, okFunction);
    };


}]);