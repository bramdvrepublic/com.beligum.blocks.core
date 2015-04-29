base.plugin("blocks.core.Notification", ["blocks.core.Broadcaster", function (Broadcaster)
{
    this.error = function (message)
    {
        BootstrapDialog.show({
            title: "Error",
            message: message,
            type: BootstrapDialog.TYPE_DANGER,
            buttons: [
                {
                    label: 'Close',
                    cssClass: 'btn-danger',
                    action: function (dialogRef)
                    {
                        dialogRef.close();
                    }
                }
            ]
        })
    }
}]);