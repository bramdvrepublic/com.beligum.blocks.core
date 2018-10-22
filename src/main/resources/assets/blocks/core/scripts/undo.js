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
 * Created by wouter on 19/01/15.
 *
 * The manager is the central point. here we catch all the events to keep an overview
 */
base.plugin("blocks.core.Undo", ["base.core.Class", "constants.blocks.core", "blocks.core.Broadcaster", "blocks.core.Hover", function (Class, Constants, Broadcaster, Hover)
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
        listener: null,
        sizeof: -1,

        //-----CONSTRUCTORS-----
        constructor: function (context, listener)
        {
            this.context = context;
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
        _notifyUndoListeners: function (value)
        {
            if (this.listener) {
                this.listener(value, 'undo', this);
            }
        },
        _notifyRedoListeners: function (value)
        {
            if (this.listener) {
                this.listener(value, 'redo', this);
            }
        }
    });

    this.UpdateHtmlCommand = Class.create(Undo.Command, {

        //-----STATICS-----
        STATIC: {
            NAME: 'updateHtml',
        },

        //-----VARIABLES-----
        element: null,
        oldHtml: null,
        newHtml: null,

        //-----CONSTRUCTORS-----
        constructor: function (context, element, oldHtml, listener)
        {
            Undo.UpdateHtmlCommand.Super.call(this, context, listener);

            this.element = element;
            this.newHtml = LZString.compress(element.html());
            this.oldHtml = LZString.compress(oldHtml);
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
            this.element.data(Undo.Command.UNDO_DATA, '');
            var value = LZString.decompress(this.oldHtml);
            this.element.html(value);
            this._notifyUndoListeners(value);
            this.element.removeData(Undo.Command.UNDO_DATA);
        },
        redo: function ()
        {
            this.element.data(Undo.Command.REDO_DATA, '');
            var value = LZString.decompress(this.newHtml);
            this.element.html(value);
            this._notifyRedoListeners(value);
            this.element.removeData(Undo.Command.REDO_DATA);
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
        constructor: function (context, element, attribute, oldValue, listener)
        {
            Undo.UpdateHtmlCommand.Super.call(this, context, listener);

            this.element = element;
            this.attribute = attribute;
            this.newValue = element.attr(attribute);
            this.oldValue = oldValue;
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
            this.element.data(Undo.Command.UNDO_DATA, '');
            var value = this.oldValue;
            this.element.attr(this.attribute, value);
            this._notifyUndoListeners(value);
            this.element.removeData(Undo.Command.UNDO_DATA);
        },
        redo: function ()
        {
            this.element.data(Undo.Command.REDO_DATA, '');
            var value = this.newValue;
            this.element.attr(this.attribute, value);
            this._notifyRedoListeners(value);
            this.element.removeData(Undo.Command.REDO_DATA);
        },
    });

    //-----MAIN CODE-----
    this.stack = new Undo.Stack();
    this.enabled = false;

    //-----PUBLIC METHODS-----
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
    this.recordHtmlChange = function (element, oldHtml, listener)
    {
        _executeDelayedCommand(Undo.UpdateHtmlCommand.NAME, element, oldHtml, function (oldVal)
        {
            return new Undo.UpdateHtmlCommand(Hover.getFocusedBlock().element[0], element, oldVal, listener);
        });
    };
    this.recordAttributeChange = function (element, attribute, oldValue, listener)
    {
        _executeDelayedCommand(Undo.UpdateAttributeCommand.NAME, element, oldValue, function (oldVal)
        {
            return new Undo.UpdateAttributeCommand(Hover.getFocusedBlock().element[0], element, attribute, oldVal, listener);
        });
    };

    //-----EVENTS-----
    //Note: it makes sense to disable undo when the sidebar is closed;
    //otherwise, a ctrl-z keystroke on any page would try to do an undo,
    //but the user doesn't have any visual indication she's editing that page.
    $(document).on(Broadcaster.EVENTS.START_BLOCKS, function (event)
    {
        Undo.enabled = true;
    });
    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function (event)
    {
        Undo.enabled = false;
    });

    //note that we need to use keydown in order to override the builtin shortcut (eg. contenteditable)
    $(document).bind("keydown", function KeyPress(e)
    {
        if (Undo.enabled) {
            var evtobj = window.event ? event : e;

            if (evtobj.ctrlKey) {
                switch (evtobj.keyCode) {
                    case 90: //z
                        e.preventDefault();

                        if (Undo.stack.canUndo(Hover.getFocusedBlock().element[0], Hover.getPageBlock().element[0])) {
                            Undo.stack.undo();
                        }
                        else {
                            Logger.info("Can't undo anymore");
                        }

                        break;
                    case 89: //y
                        e.preventDefault();

                        if (Undo.stack.canRedo(Hover.getFocusedBlock().element[0], Hover.getPageBlock().element[0])) {
                            Undo.stack.redo();
                        }
                        else {
                            Logger.info("Can't redo anymore");
                        }

                        break;
                }
            }
        }
    });

    //-----PRIVATE METHODS-----
    var _executeDelayedCommand = function (cmdName, element, oldValue, commandCallback)
    {
        //don't record changes that happen _inside_ the undo/redo events
        if (!Undo.isInsideUndoRedo(element)) {

            // a simple algorithm to amortize frequent changes
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
                    Logger.info('Executing undo/redo command: ' + command.name());
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
                } catch (ex) {
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
