/*
 * Copyright 2017 Republic of Reinvention bvba. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

base.plugin("blocks.core.Notification", [function ()
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