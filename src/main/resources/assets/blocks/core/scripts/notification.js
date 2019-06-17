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

/**
 * Wrapper around the BootstrapDialog plugin to centralize a number of frequently-used notifications modal dialogs.
 */
base.plugin("blocks.core.Notification", ["base.core.Commons", "constants.blocks.core", "messages.blocks.core", function (Commons, BlocksConstants, BlocksMessages)
{
    this.info = function (message, object)
    {
        //this will blindly pass all other arguments to the Logger method and prepend it with the message again (which is actually unnecesary)
        var logArgs = Array.prototype.slice.call(arguments, 1);
        logArgs.unshift(message);
        Logger.info.apply(Logger, logArgs);

        BootstrapDialog.show({
            title: BlocksMessages.info,
            message: message,
            type: BootstrapDialog.TYPE_PRIMARY,
            buttons: [
                {
                    label: BlocksMessages.ok,
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
            title: BlocksMessages.warning,
            message: message,
            type: BootstrapDialog.TYPE_WARNING,
            buttons: [
                {
                    label: BlocksMessages.close,
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
            title: BlocksMessages.error,
            message: message,
            type: BootstrapDialog.TYPE_DANGER,
            buttons: [
                {
                    label: BlocksMessages.close,
                    cssClass: '',
                    action: function (dialogRef)
                    {
                        dialogRef.close();
                    }
                }
            ]
        })
    };

    this.jsonError = function (message, xhr, textStatus, exception)
    {
        // if we have a public error message, use it (see the JSON error class in base-core for details)
        if (xhr.responseJSON && xhr.responseJSON.error) {
            this.error(xhr.responseJSON.error.message, xhr);
        }
        else {
            this.error(message, xhr, exception);
        }
    };

    this.confirm = function (question, callback, highlightTrueBtn, trueLabel, falseLabel)
    {
        if (!trueLabel) {
            trueLabel = BlocksMessages.yes;
        }
        if (!falseLabel) {
            falseLabel = BlocksMessages.no;
        }

        var trueClass = '';
        var falseClass = '';
        if (!Commons.isUnset(highlightTrueBtn)) {
            if (highlightTrueBtn) {
                trueClass = 'btn-primary';
            }
            else {
                falseClass = 'btn-primary';
            }
        }

        BootstrapDialog.show({
            title: BlocksMessages.confirm,
            message: question,
            type: BootstrapDialog.TYPE_DEFAULT,
            closable: false,
            draggable: false,
            data: {
                callback: callback
            },
            buttons: [
                {
                    label: falseLabel,
                    cssClass: falseClass,
                    action: function(dialog) {
                        typeof dialog.getData('callback') === 'function' && dialog.getData('callback')(false);
                        dialog.close();
                    }
                }, {
                    label: trueLabel,
                    cssClass: trueClass,
                    action: function(dialog) {
                        typeof dialog.getData('callback') === 'function' && dialog.getData('callback')(true);
                        dialog.close();
                    }
                }]
        })
    };

}]);