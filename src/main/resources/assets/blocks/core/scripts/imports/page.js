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
 * Created by wouter on 17/06/15.
 */
base.plugin("blocks.imports.Page.core", ["base.core.Class", "constants.blocks.core", "messages.blocks.core", "blocks.core.Broadcaster", "blocks.core.UI", "blocks.imports.Widget", function (Class, BlocksConstants, BlocksMessages, Broadcaster, UI, Widget)
{
    var Page = this;
    this.TAGS = ['.' + BlocksConstants.PAGE_CONTENT_CLASS];

    (this.Class = Class.create(Widget.Class, {

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            Page.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        getConfigs: function (block, element)
        {
            var retVal = [];

            var pageActions = $('<ul class="' + BlocksConstants.BLOCK_ACTIONS_CLASS + '"/>');

            var saveAction = $('<li><label>' + BlocksMessages.savePageLabel + '</label></li>').appendTo(pageActions);
            var saveBtn = $('<a class="' + BlocksConstants.SAVE_PAGE_BUTTON + ' btn btn-primary btn-sm pull-right"><i class="fa fa-fw fa-floppy-o"></i></a>').appendTo(saveAction);
            saveBtn.click(function (event)
            {
                Broadcaster.send(Broadcaster.EVENTS.PAGE.SAVE, event);

                // Note: this makes sure this event pierces through
                return false;
            });
            UI.registerKeystrokeSelector(UI.KEYCODE.S, UI.KEYCODE.MODIFIER.CTRL, '.' + BlocksConstants.SAVE_PAGE_BUTTON);

            var deleteAction = $('<li><label>' + BlocksMessages.deletePageLabel + '</label></li>').appendTo(pageActions);
            var deleteBtn = $('<a class="' + BlocksConstants.DELETE_PAGE_BUTTON + ' btn btn-default btn-sm pull-right"><i class="fa fa-fw fa-trash-o"></i></a>').appendTo(deleteAction);
            deleteBtn.click(function (event)
            {
                Broadcaster.send(Broadcaster.EVENTS.PAGE.DELETE, event);

                // Note: this makes sure this event pierces through
                return false;
            });
            UI.registerKeystrokeSelector(UI.KEYCODE.DELETE, UI.KEYCODE.MODIFIER.NONE, '.' + BlocksConstants.DELETE_PAGE_BUTTON);

            var newBlock = $('<li><label>' + BlocksMessages.newBlockLabel + '</label></li>').append($('<a class="' + BlocksConstants.CREATE_BLOCK_CLASS + ' btn btn-default btn-sm pull-right" data-toggle="popover" data-trigger="click" data-placement="bottom" data-content="' + BlocksMessages.newBlockTooltip + '"><i class="fa fa-fw fa-magic"></i></a>')).appendTo(pageActions);

            //initialize the newBlock popover
            //Note that the popover might not be added to the DOM yet if things load fast (hence the timeout)
            $(document).ready(function ()
            {
                setTimeout(function ()
                {
                    var pops = newBlock.find('[data-toggle="popover"]');
                    if (pops) {
                        pops.popover({
                            container: 'body'
                        });
                        pops.on('shown.bs.popover', function ()
                        {
                            var _this = $(this);
                            setTimeout(function ()
                            {
                                _this.popover('hide');
                            }, 2000);
                        });
                    }
                }, 1000);
            });

            retVal.push(pageActions);

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetPageTitle;
        },

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);

}]);