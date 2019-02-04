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
 * The undo manager.
 *
 * Created by bram on 19/10/18.
 */
base.plugin("blocks.core.Undo", ["base.core.Class", "constants.blocks.core", "blocks.core.Broadcaster", "blocks.core.UI", function (Class, Constants, Broadcaster, UI)
{
    var Undo = this;

    //-----INNER CLASSES-----
    this.Stack = Class.create({

        //-----STATICS-----
        STATIC: {
            //by default, we'll use a stack of 1M
            MAX_STACK_SIZE: 1024 * 1024,
        },

        //-----VARIABLES-----
        commands: [],
        stackPosition: -1,
        savePosition: -1,

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
        },

        //-----PUBLIC METHODS-----
        execute: function (command)
        {
            this._clearRedo();
            command.execute();
            this.commands.push(command);
            this.stackPosition++;
            this.changed();
            Broadcaster.send(Broadcaster.EVENTS.UNDO.RECORDED);

            //the calc seems to be quite heavy, so we put it somewhere in the future
            //to improve the user experience
            var _this = this;
            setTimeout(function ()
            {
                _this._checkSize(_this);
            }, 10);
        },
        undo: function ()
        {
            Logger.info("Performing undo");

            this.commands[this.stackPosition].undo();
            this.stackPosition--;
            this.changed();
        },
        canUndo: function (context, generalContext)
        {
            var retVal = false;

            retVal = this.stackPosition >= 0;
            if (context && retVal) {
                //note that we can only undo commands that are in our own context,
                //except if we're in the general page context, where we can undo everything
                retVal = context == generalContext || context == this.commands[this.stackPosition].getContext();
            }

            return retVal;
        },
        redo: function ()
        {
            Logger.info("Performing redo");

            this.stackPosition++;
            this.commands[this.stackPosition].redo();
            this.changed();
        },
        canRedo: function (context, generalContext)
        {
            var retVal = false;

            retVal = this.stackPosition < this.commands.length - 1;
            if (context && retVal) {
                //note that we can only redo commands that are in our own context,
                //except if we're in the general page context, where we can undo everything
                retVal = context == generalContext || context == this.commands[this.stackPosition + 1].getContext();
            }

            return retVal;
        },
        save: function ()
        {
            this.savePosition = this.stackPosition;
            this.changed();
        },
        dirty: function ()
        {
            return this.stackPosition != this.savePosition;
        },
        changed: function ()
        {
        },

        //-----PRIVATE METHODS-----
        _clearRedo: function ()
        {
            // TODO there's probably a more efficient way for this
            this.commands = this.commands.slice(0, this.stackPosition + 1);
        },
        _checkSize: function (_this)
        {
            //This method is called after a command was added to the stack and it's size was calculated
            //We'll use it to wipe old entries that fill this structure's size past a certain threshold.
            var totalSize = 0;

            if (_this.commands.length > 0) {
                // Count back from the end of the stack until we exceed the threshold,
                // then cut off the lower part
                for (var i = _this.commands.length - 1; i >= 0; i--) {
                    totalSize += _this.commands[i].size();

                    //if this stack size exceeds the limit, we'll cut right after
                    //this position (because this is the one that pushed us over the edge)
                    if (totalSize > Undo.Stack.MAX_STACK_SIZE) {
                        var cutIdx = i + 1;
                        _this.commands = _this.commands.slice(cutIdx, _this.commands.length);
                        _this.stackPosition = _this.stackPosition - cutIdx;
                        Logger.info('Cutting off undo stack at position ' + cutIdx);

                        break;
                    }
                }
            }
        },
    });
    this.Command = Class.create({

        //-----STATICS-----
        STATIC: {
            UNDO_DATA: 'undo',
            REDO_DATA: 'redo',
        },

        //-----VARIABLES-----
        context: null,
        element: null,
        elementSelector: null,
        oldValue: null,
        newValue: null,
        configSelector: null,
        configOldValue: null,
        configNewValue: null,
        listener: null,
        sizeof: -1,

        //-----CONSTRUCTORS-----
        constructor: function (context, element, oldValue, newValue, configElement, configOldValue, configNewValue, listener)
        {
            this.context = context;
            this.element = element;
            this.elementSelector = this._buildSelector(element);
            this.oldValue = oldValue;
            this.newValue = newValue;
            //Instead of saving the config element (that is dynamically created in the sidebar),
            //we keep a selector reference and hope it will match on the next undo/redo.
            //Note that this solution is not optimal because the elements in the sidebar can change order,
            //get added to, etc. We should probably move to a situation where each element has it's own fixed id.
            this.configSelector = this._buildSelector(configElement);
            //let's make everything uniform and set it to undefined if null was passed (see check in _doPostprocessing())
            this.configOldValue = configOldValue == null ? undefined : configOldValue;
            this.configNewValue = configNewValue == null ? undefined : configNewValue;
            this.listener = listener;
        },

        //-----ABSTRACT METHODS-----
        name: function ()
        {
            return null;
        },
        execute: function ()
        {
        },
        undo: function ()
        {
        },
        redo: function ()
        {
        },
        size: function ()
        {
            if (this.sizeof < 0) {
                this.sizeof = _calcBytesize(this);
            }

            return this.sizeof;
        },
        getContext: function ()
        {
            return this.context;
        },

        //-----PUBLIC METHODS-----

        //-----PRIVATE METHODS-----
        _doPostprocessing: function (action, value, configValue)
        {
            if (this.listener) {
                this.listener(value, action, this);
            }

            if (this.configSelector) {
                var configEl = $(this.configSelector);
                if (configEl.length > 0) {
                    var configVal = value;
                    if (typeof configValue !== 'undefined') {
                        //this allows us to pass a dynamic callback function as the config value
                        if (typeof configValue == 'function') {
                            configVal = configValue(value, action, this);
                        }
                        else {
                            configVal = configValue;
                        }
                    }

                    //setting the value of a checkbox doesn't happen with val()
                    if (configEl.is('input[type="checkbox"]')) {
                        configEl.prop('checked', configVal).change();
                    }
                    else {
                        configEl.val(configVal).change();
                    }
                }
            }

            if (action == 'undo') {
                Broadcaster.send(Broadcaster.EVENTS.UNDO.PERFORMED);
            }
            else {
                Broadcaster.send(Broadcaster.EVENTS.UNDO.REDO);
            }
        },
        _getElement: function ()
        {
            // Note that we get in trouble if we store a reference to an elemnt
            // and later modify the html (eg. of it's container element).
            // The stored element reference will still be valid (apparently)
            // but changes to it won't happen.
            // This method works around that by also storing a selector and checking
            // if the element exists and re-searching it using the selector if it doesn't.
            // Note that the real cause is more problematic: by using .html(), we lose all
            // references to elements in that html (eg. event handlers, data, etc.) and
            // things might break.
            // It's a work in progress, but we should update the code of UpdateHtmlCommand with
            // something better.
            var retVal = this.element;

            if (!$.contains(document, retVal[0])) {
                retVal = $(this.elementSelector);
            }

            return retVal;
        },
        _buildSelector: function (element)
        {
            //Note: there's a problem when we try to sync the config widget in the sidebar
            //with an undo/redo action because that widget might have been deleted/rebuilt in
            //the mean time (eg. focus was lost and regained).
            //To work around that, we generate a css selector for the config widget and try to find it
            //again when the undo/redo is happening.
            return element && element.length > 0 ? Undo.cssSelectorGenerator.getSelector(element[0]) : null;
        }
    });

    this.UpdateHtmlCommand = Class.create(Undo.Command, {

        //-----STATICS-----
        STATIC: {
            NAME: 'updateHtml',
        },

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function (context, element, oldValue, configElement, configOldValue, configNewValue, listener)
        {
            Undo.UpdateHtmlCommand.Super.call(this, context, element, LZString.compress(oldValue), LZString.compress(element.html()),
                configElement, configOldValue, configNewValue, listener);
        },

        //-----IMPLEMENTED METHODS-----
        name: function ()
        {
            return Undo.UpdateHtmlCommand.NAME;
        },
        execute: function ()
        {
        },
        undo: function ()
        {
            var element = this._getElement();

            element.data(Undo.Command.UNDO_DATA, '');
            var value = LZString.decompress(this.oldValue);
            element.html(value);
            this._doPostprocessing('undo', value, this.configOldValue);
            element.removeData(Undo.Command.UNDO_DATA);
        },
        redo: function ()
        {
            var element = this._getElement();

            element.data(Undo.Command.REDO_DATA, '');
            var value = LZString.decompress(this.newValue);
            element.html(value);
            this._doPostprocessing('redo', value, this.configNewValue);
            element.removeData(Undo.Command.REDO_DATA);
        },
    });

    this.UpdateAttributeCommand = Class.create(Undo.Command, {

        //-----STATICS-----
        STATIC: {
            NAME: 'updateAttribute',
        },

        //-----VARIABLES-----
        element: null,
        attribute: null,
        oldValue: null,
        newValue: null,

        //-----CONSTRUCTORS-----
        constructor: function (context, element, attribute, oldValue, configElement, configOldValue, configNewValue, listener)
        {
            //from the docs:
            // - attr(name): returns undefined for attributes that have not been set
            // - attr(name, value): if value is null, the attribute will be removed (note: doesn't work with undefined!)
            var newValue = element.attr(attribute);
            if (typeof newValue == 'undefined') {
                newValue = null;
            }
            if (typeof oldValue == 'undefined') {
                oldValue = null;
            }

            Undo.UpdateHtmlCommand.Super.call(this, context, element, oldValue, newValue,
                configElement, configOldValue, configNewValue, listener);

            this.attribute = attribute;
        },

        //-----IMPLEMENTED METHODS-----
        name: function ()
        {
            return Undo.UpdateHtmlCommand.NAME;
        },
        execute: function ()
        {
        },
        undo: function ()
        {
            var element = this._getElement();

            element.data(Undo.Command.UNDO_DATA, '');
            var value = this.oldValue;
            //note: from the docs: if value is null, the attribute will be removed
            element.attr(this.attribute, value);
            this._doPostprocessing('undo', value, this.configOldValue);
            element.removeData(Undo.Command.UNDO_DATA);
        },
        redo: function ()
        {
            var element = this._getElement();

            element.data(Undo.Command.REDO_DATA, '');
            var value = this.newValue;
            //note: from the docs: if value is null, the attribute will be removed
            element.attr(this.attribute, value);
            this._doPostprocessing('redo', value, this.configNewValue);
            element.removeData(Undo.Command.REDO_DATA);
        },
    });

    //-----MAIN CODE-----
    this.stack = new Undo.Stack();
    //Note that by default, this id's of the elements are taken into account,
    //but they're generated on-the-fly, so we explicitly removed them from the option list below.
    this.cssSelectorGenerator = new CssSelectorGenerator({selectors: ['class', 'tag', 'nthchild']});
    this.enabled = false;
    this.oldPageHtml = '';

    UI.registerKeystrokeAction(UI.KEYCODE.Z, UI.KEYCODE.MODIFIER.CTRL, function ()
    {
        if (Undo.stack.canUndo(UI.focusedSurface.element[0], UI.pageSurface.element[0])) {
            Undo.stack.undo();
        }
        else {
            Logger.info("Can't undo anymore");
        }
    });

    UI.registerKeystrokeAction(UI.KEYCODE.Y, UI.KEYCODE.MODIFIER.CTRL, function ()
    {
        if (Undo.stack.canRedo(UI.focusedSurface.element[0], UI.pageSurface.element[0])) {
            Undo.stack.redo();
        }
        else {
            Logger.info("Can't redo anymore");
        }
    });

    //-----PUBLIC METHODS-----
    this.enable = function (enable)
    {
        if (enable) {
            this.enabled = true;
            //initialize the html of the page now it's inside it's wrapper element
            this.oldPageHtml = UI.pageContent.html();
        }
        else {
            this.enabled = false;
            //note: it makes sense to 'forget' this when the sidebar is closed
            this.oldPageHtml = '';
        }
    };
    this.isInsideUndoRedo = function (element)
    {
        return this.isInsideUndo(element) || this.isInsideRedo(element);
    };
    this.isInsideUndo = function (element)
    {
        return typeof element.data(Undo.Command.UNDO_DATA) !== 'undefined';
    };
    this.isInsideRedo = function (element)
    {
        return typeof element.data(Undo.Command.REDO_DATA) !== 'undefined';
    };
    /**
     * Records an inner HTML change that can later be undone.
     *
     * @param element The jQuery element on which the attribute was changed
     * @param oldValue The old value of the inner html
     * @param configElement The jQuery sidebar widget to call .val().change() on if we undo/redo this action
     * @param configOldValue A value, a callback function or null (to use the oldValue) that will be supplied to the config's .val() callback on undo
     * @param configNewValue A value, a callback function or null (to use the newValue) that will be supplied to the config's .val() callback on redo
     * @param listener The listener callback that is called on each undo and redo
     */
    this.recordHtmlChange = function (element, oldValue, configElement, configOldValue, configNewValue, listener)
    {
        _executeDelayedCommand(Undo.UpdateHtmlCommand.NAME, element, oldValue, function (oldVal)
        {
            return new Undo.UpdateHtmlCommand(UI.focusedSurface.element[0], element, oldVal, configElement, configOldValue, configNewValue, listener);
        });
    };
    /**
     * Records an attribute change that can later be undone.
     *
     * @param element The jQuery element on which the attribute was changed
     * @param attribute The name of the attribute
     * @param oldValue The old value of the attribute or null if it didn't exist before this call
     * @param configElement The jQuery sidebar widget to call .val().change() on if we undo/redo this action
     * @param configOldValue A value, a callback function or null (to use the oldValue) that will be supplied to the config's .val() callback on undo
     * @param configNewValue A value, a callback function or null (to use the newValue) that will be supplied to the config's .val() callback on redo
     * @param listener The listener callback that is called on each undo and redo
     */
    this.recordAttributeChange = function (element, attribute, oldValue, configElement, configOldValue, configNewValue, listener)
    {
        _executeDelayedCommand(Undo.UpdateAttributeCommand.NAME, element, oldValue, function (oldVal)
        {
            return new Undo.UpdateAttributeCommand(UI.focusedSurface.element[0], element, attribute, oldVal, configElement, configOldValue, configNewValue, listener);
        });
    };

    //-----EVENTS-----
    //note: if another block executes a change, we need to make sure
    //the old html is updated or we'll bypass that change and jump further back in time
    $(document).on(Broadcaster.EVENTS.UNDO.RECORDED, function (event)
    {
        Undo.oldPageHtml = UI.pageContent.html();
    });
    // This handles the undo/redo of the general page layout (adding/moving/deleting blocks & resizing of columns).
    $(document).on(Broadcaster.EVENTS.PAGE.CHANGED, function (event, extraData)
    {
        //Note: blocks-layout is moved around when the sidebar opens,
        //so we need to re-fetch it every time the DOM changes to avoid stale references
        if (UI.pageContent.html() !== Undo.oldPageHtml) {
            //note: executing this will trigger the update of oldBlocksHtml, see listener above
            Undo.recordHtmlChange(UI.pageContent, Undo.oldPageHtml, null, null, null, function ()
            {
                //Rebuild the page model when an undo/redo was executed so everything is in sync.
                Broadcaster.send(Broadcaster.EVENTS.PAGE.RELOAD, null);

                // every time the page is undone/redone, we need to make sure the stored html is updated
                Undo.oldPageHtml = UI.pageContent.html();
            });
        }
    });

    //-----PRIVATE METHODS-----
    var _executeDelayedCommand = function (cmdName, element, oldValue, commandCallback)
    {
        //don't record changes that happen _inside_ the undo/redo events
        if (!Undo.isInsideUndoRedo(element)) {

            // a simple algorithm to amortize frequent changes
            //Note: by using the cmdName as key, we only group the same type commands together
            var prevEvent = element.data(cmdName);
            if (prevEvent) {
                //if we're about to merge two updates, we need to take the first oldHtml value
                oldValue = prevEvent.oldValue;
                clearTimeout(prevEvent.timer);
            }

            element.data(cmdName, {
                oldValue: oldValue,
                timer: setTimeout(function ()
                {
                    var command = commandCallback(oldValue);
                    Logger.info('Storing undo/redo command: ' + command.name());
                    Undo.stack.execute(command);
                    element.removeData(cmdName);

                }, 500)
            });
        }
    };
    /**
     * Calculates the byte size of an object.
     * See https://github.com/miktam/sizeof/blob/master/index.js
     */
    var _calcBytesize = function (object, objectList)
    {
        objectList = objectList || [];

        if (object !== null && typeof (object) === 'object' && objectList.indexOf(object) === -1) {

            //avoid infinite recursion
            objectList.push(object);

            var bytes = 0;
            for (var key in object) {

                if (!Object.hasOwnProperty.call(object, key)) {
                    continue;
                }

                bytes += _calcBytesize(key, objectList);
                try {
                    bytes += _calcBytesize(object[key], objectList);
                }
                catch (ex) {
                    if (ex instanceof RangeError) {
                        // circular reference detected, final result might be incorrect
                        // let's be nice and not throw an exception
                        bytes = 0;
                    }
                }
            }
            return bytes;
        }
        else if (typeof (object) === 'string') {
            return object.length * 2;
        }
        else if (typeof (object) === 'boolean') {
            return 4;
        }
        else if (typeof (object) === 'number') {
            return 8;
        }
        else {
            return 0;
        }
    };

}]);
