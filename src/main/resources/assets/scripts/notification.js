base.plugin("blocks.core.Notification", ["blocks.core.Broadcaster", function (Broadcaster)
{
    this.info = function (message, object)
    {
        //this will blindly pass all other arguments to the Logger method and prepend it with the message again (which is actually unnecesary)
        var logArgs = Array.prototype.slice.call(arguments, 1);
        logArgs.unshift(message);
        Logger.info.apply(Logger, logArgs);

        BootstrapDialog.show({
            title: "Info",
            message: message,
            type: BootstrapDialog.TYPE_INFO,
            buttons: [
                {
                    label: 'Ok',
                    cssClass: '',
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
        //this will blindly pass all other arguments to the Logger method and prepend it with the message again (which is actually unnecesary)
        var logArgs = Array.prototype.slice.call(arguments, 1);
        logArgs.unshift(message);
        Logger.warn.apply(Logger, logArgs);

        BootstrapDialog.show({
            title: "Warning",
            message: message,
            type: BootstrapDialog.TYPE_WARNING,
            buttons: [
                {
                    label: 'Close',
                    cssClass: '',
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
        //this will blindly pass all other arguments to the Logger method and prepend it with the message again (which is actually unnecesary)
        var logArgs = Array.prototype.slice.call(arguments, 1);
        logArgs.unshift(message);
        Logger.error.apply(Logger, logArgs);

        BootstrapDialog.show({
            title: "Error",
            message: message,
            type: BootstrapDialog.TYPE_DANGER,
            buttons: [
                {
                    label: 'Close',
                    cssClass: '',
                    action: function (dialogRef)
                    {
                        dialogRef.close();
                    }
                }
            ]
        })
    };
}]);