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
 * Region where templates can be dragged
 *
 * Created by wouter on 10/06/15.
 */
base.plugin("blocks.core.elements.Page", ["base.core.Class", "constants.blocks.core", "messages.blocks.core", "blocks.core.DOM", function (Class, BlocksConstants, BlocksMessages, DOM)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Page = Class.create(blocks.elements.Surface, {

        //-----STATICS-----

        //-----CONSTANTS-----

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            //before we build a new page, make sure all old dom elements are cleared
            blocks.elements.Surface.clearAllOverlays();

            blocks.elements.Page.Super.call(this, null, $("." + BlocksConstants.PAGE_CONTENT_CLASS));

            //Note: it makes sense to reset the ID lookup table before booting up the submodel
            blocks.elements.Surface.INDEX = {};

            //We manually call this for the top-level structure (the page) to bootstrap the subtree-generation
            this._buildSubmodel();

            //this._refresh(true);
        },

        //-----PUBLIC METHODS-----

        //-----TODO UNCHECKED-----
        // getLayoutContainer: function ()
        // {
        //     var retVal = null;
        //     for (var i = 0; i < this.children.length; i++) {
        //         var child = this.children[i];
        //         if (child.isContainer()) {
        //             retVal = child;
        //             break;
        //         }
        //     }
        //     return retVal;
        // },

        //-----PRIVATE METHODS-----
        _getType: function()
        {
            return 'page';
        },
        _getName: function()
        {
            return BlocksMessages.surfacePageName;
        },
        _newChildInstance: function(element)
        {
            return new blocks.elements.Container(this, element);
        },
        _isAcceptableChild: function(element)
        {
            return blocks.elements.Surface.isContainer(element);
        },
    });
}]);