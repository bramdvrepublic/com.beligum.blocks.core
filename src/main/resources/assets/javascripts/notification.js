blocks.plugin("blocks.core.notification", [function() {
    // Todo show/hide overlay
    this.dialog = function(title, message, okFunction, cancelFunction) {
        var modal = $("<div class='modal'></div>").css("background-color", "#ffffff");
        var modalDialog = $("<div class='modal-dialog'></div>");
        var modalHeader = $("<div class='modal-header'></div>");
        var closeIcon = $('<button type="button" class="close" data-dismiss="modal"><span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>');
        var modalTitle = $("<h4 class='modal-title'>" + title + "</h4>");
        var modalBody = $("<div class='modal-body'>" + message + "</div>");
        var modalFooter = $("<div class='modal-footer'></div>");
        var closeButton = $('<button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>');
        var okButton = $('<button type="button" class="btn btn-primary">Ok</button>');

        var hideDialog = function(callback) {
            modal.fadeOut(200, function() {
                modal.remove();
                callback();
            });
        };

        if (cancelFunction != null) {
            closeButton.on("click", function() {hideDialog(cancelFunction)});
            closeIcon.on("click", function() {hideDialog(cancelFunction)});
            modalFooter.append(closeButton)
        } else {
            okButton.on("click", function() {hideDialog(okFunction)});
        }

        modalFooter.append(okButton);
        modal.append(modalDialog.append(modalHeader.append(closeIcon).append(modalTitle)).append(modalBody).append(modalFooter));
        modal.hide();
        $("body").append(modal);
        modal.fadeIn(200);
    };

    this.alert = function(title, message, okFunction) {
        this.dialog(title, message, okFunction);
    };


}]);