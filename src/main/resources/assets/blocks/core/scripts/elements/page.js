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
 * All content in a page is wrapped in a <div> marked with the BlocksConstants.PAGE_CONTENT_CLASS.
 * This div basically contains all elements that can be edited and layouted by the user.
 */
base.plugin("blocks.core.elements.Page", ["base.core.Class", "constants.blocks.core", "messages.blocks.core", "blocks.core.UI", function (Class, BlocksConstants, BlocksMessages, UI)
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
        constructor: function (element)
        {
            //before we build a new page, make sure all old dom elements are cleared
            blocks.elements.Surface.clearAllOverlays();

            blocks.elements.Page.Super.call(this, null, element);

            //Note: it makes sense to reset the ID lookup table before booting up the submodel
            blocks.elements.Surface.INDEX = {};

            //We manually call this for the top-level structure (the page) to bootstrap the subtree-generation
            this._buildSubmodel();
        },

        //-----PUBLIC METHODS-----

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
            // this (together with _isAcceptableChild()) decides the root element
            // of the page to build our hierarchy from. Everything outside of this type
            // will be ignored.
            if (blocks.elements.Surface.isLayout(element)) {
                return new blocks.elements.Layout(this, element);
            }
            else {
                return new blocks.elements.Container(this, element);
            }
        },
        _isAcceptableChild: function(element)
        {
            return blocks.elements.Surface.isLayout(element) || blocks.elements.Surface.isContainer(element);
        },
        /**
         * Overloads the parent surface function to keep simplifying until nothing is changing anymore
         *
         * @param deep
         * @private
         */
        _simplify: function (deep)
        {
            var retVal = false;

            var keepSimplifying = true;
            while (keepSimplifying) {
                // as long as the super method returns true (something changed), we continue simplifying
                keepSimplifying = blocks.elements.Page.Super.prototype._simplify.call(this, deep);
                retVal = retVal || keepSimplifying;
            }

            return retVal;
        },
    });
}]);