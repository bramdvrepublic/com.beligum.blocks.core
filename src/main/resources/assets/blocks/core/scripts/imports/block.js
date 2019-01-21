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
 * Please note we don't want to move this file to the blocks-imports-block project because that would mean the standard "delete" button (etc.)
 * may be made optional. We chose to 'extend' this config with the blocks-imports-block project instead (contrary to the blocks-imports-row/column projects).
 *
 * Created by wouter on 17/07/15.
 */
base.plugin("blocks.imports.Block", ["base.core.Class", "blocks.imports.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Manager", "base.core.Commons", "blocks.imports.All", function (Class, Widget, BlocksConstants, BlocksMessages, Manager, Commons, All)
{
    var Block = this;
    this.TAGS = All.IMPORTS;

    (this.Class = Class.create(Widget.Class, {

        STATIC: {},

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            Block.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            Block.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            Block.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element, addCreateLink)
        {
            var retVal = [];

            var blockActions = $("<ul/>").addClass(BlocksConstants.BLOCK_ACTIONS_CLASS);

            var removeAction = $("<li><label>"+BlocksMessages.deleteBlockLabel+"</label></li>").appendTo(blockActions);
            var removeButton = $("<a class='btn btn-danger btn-sm pull-right'><i class='fa fa-fw fa-trash-o'></i></a>").appendTo(removeAction);
            removeButton.click(function (event)
            {
                //TODO let's not ask for a confirmation but implement an undo-function later on...
                //confirm.removeClass("hidden");
                //text.addClass("hidden");

                //$("." + BlocksConstants.OPACITY_CLASS).removeClass(BlocksConstants.OPACITY_CLASS);
                Manager.remove(event, block);
            });

            retVal.push(blockActions);

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetBlockTitle;
        },

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);
}]);