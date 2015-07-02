define('scribe-plugin-toolbar',[],function () {

    scribetoolbar = {bttons: null, el: null};
    scribetoolbar.buttons = null
    var el = null;



    return function (toolbarNode) {
        return function (scribe) {
            scribetoolbar.buttons = toolbarNode.querySelectorAll('[data-command-name]');

            Array.prototype.forEach.call(scribetoolbar.buttons, function (button) {
                button.addEventListener('click', function () {
                    // Look for a predefined command.
                    var command = scribe.getCommand(button.dataset.commandName);

                    /**
                     * Focus will have been taken away from the Scribe instance when
                     * clicking on a button (Chrome will return the focus automatically
                     * but only if the selection is not collapsed. As per: http://jsbin.com/tupaj/1/edit?html,js,output).
                     * It is important that we focus the instance again before executing
                     * the command, because it might rely on selection data.
                     */
                    if (scribe.el != null) {
                        scribe.el.focus();
                        command.execute(button.dataset.commandValue);
                    }
                    /**
                     * Chrome has a bit of magic to re-focus the `contenteditable` when a
                     * command is executed.
                     * As per: http://jsbin.com/papi/1/edit?html,js,output
                     */
                });

                // Keep the state of toolbar buttons in sync with the current selection.
                // Unfortunately, there is no `selectionchange` event.
                scribe.el.addEventListener('keyup', updateUi);
                scribe.el.addEventListener('mouseup', updateUi);

                scribe.el.addEventListener('focus', updateUi);
                scribe.el.addEventListener('blur', updateUi);

                scribetoolbar.el = scribe.el;
                // We also want to update the UI whenever the content changes. This
                // could be when one of the toolbar buttons is actioned.
                scribe.on('content-changed', updateUi);

                function updateUi(e) {
                    if (scribe.api != null) {
                    // Look for a predefined command.
                    var command = scribe.getCommand(button.dataset.commandName);


                        var selection = new scribe.api.Selection();


                        // TODO: Do we need to check for the selection?
                        if (selection.range && command.queryState(button.dataset.commandValue)) {
                            button.classList.add('active');
                        } else {
                            button.classList.remove('active');
                        }

                        if (selection.range && command.queryEnabled()) {
                            button.removeAttribute('disabled');
                        } else {
                            button.setAttribute('disabled', 'disabled');
                        }
                    }
                }
            });
        };
    };

});


//# sourceMappingURL=scribe-plugin-toolbar.js.map