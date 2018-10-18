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
base.plugin("blocks.core.Undo", ["base.core.Class", "constants.blocks.core", "blocks.core.Broadcaster", function (Class, Constants, Broadcaster)
{
    var Undo = this;

    //-----INNER CLASSES-----
    this.Stack = Class.create({

        //-----STATICS-----
        STATIC: {},

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
        },
        undo: function ()
        {
            this.commands[this.stackPosition].undo();
            this.stackPosition--;
            this.changed();
        },
        canUndo: function ()
        {
            return this.stackPosition >= 0;
        },
        redo: function ()
        {
            this.stackPosition++;
            this.commands[this.stackPosition].redo();
            this.changed();
        },
        canRedo: function ()
        {
            return this.stackPosition < this.commands.length - 1;
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
            // do nothing, override
        },

        //-----PRIVATE METHODS-----
        _clearRedo: function ()
        {
            // TODO there's probably a more efficient way for this
            this.commands = this.commands.slice(0, this.stackPosition + 1);
        },

    });
    this.Command = Class.create({

        //-----STATICS-----
        STATIC: {},

        //-----VARIABLES-----
        overrideException: null,

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            this.overrideException = new Error("Please override me");
        },

        //-----ABSTRACT METHODS-----
        execute: function ()
        {
            throw this.overrideException;
        },
        undo: function ()
        {
            throw this.overrideException;
        },
        redo: function ()
        {
            throw this.overrideException;
        },

        //-----PUBLIC METHODS-----

    });

    this.UpdateHtmlCommand = Class.create(Undo.Command, {

        //-----VARIABLES-----
        element: null,
        oldHtml: null,
        newHtml: null,

        //-----CONSTRUCTORS-----
        constructor: function (element, oldHtml, newHtml)
        {
            Undo.UpdateHtmlCommand.Super.call(this, element, oldHtml, newHtml);

            this.element = element;
            this.oldHtml = oldHtml;
            this.newHtml = newHtml;
        },

        //-----IMPLEMENTED METHODS-----
        execute: function ()
        {
        },
        undo: function ()
        {
            this.element.html(this.oldHtml);
        },
        redo: function ()
        {
            this.element.html(this.newHtml);
        },
    });

    //-----MAIN CODE-----
    this.stack = new Undo.Stack();
    this.observer = null;

    //-----PUBLIC METHODS-----
    this.recordHtmlChange = function (element, oldHtml, newHtml)
    {
        this.stack.execute(new Undo.UpdateHtmlCommand(element, oldHtml, typeof newHtml === 'undefined' ? element.html() : newHtml));
    };

    //-----EVENTS-----
    $(document).bind("keyup", function KeyPress(e)
    {
        var evtobj = window.event ? event : e;

        if (evtobj.ctrlKey) {
            switch (evtobj.keyCode) {
                case 90: //z
                    e.preventDefault();
                    if (Undo.stack.canUndo()) {
                        Undo.stack.undo();
                    }
                    else {
                        Logger.info("Can't undo anymore");
                    }
                    break;
                case 89: //y
                    e.preventDefault();
                    if (Undo.stack.canRedo()) {
                        Undo.stack.redo();
                    }
                    else {
                        Logger.info("Can't redo anymore");
                    }
                    break;
            }
        }
    });

    //main entry point for blocks after all the GUI events are handled
    $(document).on(Broadcaster.EVENTS.START_BLOCKS, function (event)
    {
        // Undo.observer = new MutationObserver(function (mutations) {
        //     // Whether you iterate over mutations..
        //     mutations.forEach(function (mutation) {
        //         // or use all mutation records is entirely up to you
        //         var entry = {
        //             //mutation: mutation,
        //             // Returns "attributes" if the mutation was an attribute mutation,
        //             // "characterData" if it was a mutation to a CharacterData node,
        //             // and "childList" if it was a mutation to the tree of nodes.
        //             type: mutation.type,
        //             // For attributes, it is the element whose attribute changed.
        //             // For characterData, it is the CharacterData node.
        //             // For childList, it is the node whose children changed.
        //             target: mutation.target,
        //             value: mutation.target.textContent,
        //             oldValue: mutation.oldValue
        //         };
        //         console.log('Recording mutation:', mutation);
        //     });
        // });
        //
        // Undo.observer.observe($('blocks-layout'/*BlocksConstants.PAGE_CONTENT_CLASS*//*'.blocks-page-content'*/)[0], {
        //     subtree: true,
        //     attributes: true,
        //     childList: true,
        //     characterData: true,
        //     attributeOldValue: true,
        //     characterDataOldValue: true
        // });
    });

    $(document).on(Broadcaster.EVENTS.STOP_BLOCKS, function (event)
    {
        if (Undo.observer) {
            Undo.observer.disconnect();
        }
    });

    //-----PRIVATE METHODS-----

}]);
