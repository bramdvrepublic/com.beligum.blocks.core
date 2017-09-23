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
base.plugin("blocks.imports.Page", ["base.core.Class", "blocks.imports.Widget", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.Broadcaster", "blocks.core.UI", function (Class, Widget, BlocksConstants, BlocksMessages, Sidebar, Broadcaster, UI)
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
        init: function ()
        {

        },
        focus: function (block, element, hotspot, event)
        {
            Page.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            Page.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element)
        {
            var retVal = [];

            var pageActions = $('<ul class="' + BlocksConstants.BLOCK_ACTIONS_CLASS + '"/>');
            var savePage = $('<li><label>' + BlocksMessages.savePageLabel + '</label></li>').append($('<a class="' + BlocksConstants.SAVE_PAGE_BUTTON + ' btn btn-primary btn-sm pull-right"><i class="fa fa-fw fa-floppy-o"></i></a>')).appendTo(pageActions);
            var deletePage = $('<li><label>' + BlocksMessages.deletePageLabel + '</label></li>').append($('<a class="' + BlocksConstants.DELETE_PAGE_BUTTON + ' btn btn-default btn-sm pull-right"><i class="fa fa-fw fa-trash-o"></i></a>')).appendTo(pageActions);
            var newBlock = $('<li><label>' + BlocksMessages.newBlockLabel + '</label></li>').append($('<a class="' + BlocksConstants.CREATE_BLOCK_CLASS + ' btn btn-default btn-sm pull-right" data-toggle="popover" data-trigger="click" data-placement="bottom" data-content="' + BlocksMessages.newBlockTooltip + '"><i class="fa fa-fw fa-magic"></i></a>')).appendTo(pageActions);

            //activation is done in menu.js (we need one element)
            pageActions = pageActions.wrap('<div/>');
            pageActions.append('<hr>');

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

            var title = $("title");
            if (title.length == 0) {
                title = $("<title property='title' />").html(BlocksMessages.defaultPageTitle);
                $("head").append(title);
            }

            if (title.hasAttribute("property") || title.hasAttribute("data-property")) {
                retVal.push(this.addValueHtml(Sidebar, title, BlocksMessages.pageTitleLabel, BlocksMessages.pageTitlePlaceholder, false));
            }

            retVal.push(this.addUniqueAttributeValueAsync(Sidebar, $("html"), BlocksMessages.pageSubjectLabel, "typeof", BlocksConstants.RDF_CLASSES_ENDPOINT, "title", "curieName", null));

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetPageTitle;
        },

        //-----PRIVATE METHODS-----

    })).register(this.TAGS);

}]);