base.plugin("blocks.core.Notification", ["blocks.core.Broadcaster", function (Broadcaster)
{
    this.info = function (message, object)
    {
        Logger.info(message, object);

        BootstrapDialog.show({
            title: "Info",
            message: message,
            type: BootstrapDialog.TYPE_INFO,
            buttons: [
                {
                    label: 'Information',
                    cssClass: 'btn-info',
                    action: function (dialogRef)
                    {
                        dialogRef.close();
                    }
                }
            ]
        })
    };
    this.warn = function (message, object)
    {
        Logger.warn(message, object);

        BootstrapDialog.show({
            title: "Warning",
            message: message,
            type: BootstrapDialog.TYPE_WARNING,
            buttons: [
                {
                    label: 'Warning',
                    cssClass: 'btn-warning',
                    action: function (dialogRef)
                    {
                        dialogRef.close();
                    }
                }
            ]
        })
    };
    this.error = function (message, object)
    {
        Logger.error(message, object);

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