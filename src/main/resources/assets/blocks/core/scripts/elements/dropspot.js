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
 * Special element that indicates a trigger where a block could be dropped
 * on the side of another surface. It's independent of other surfaces so it can be placed
 * absolutely and over the overlays of other surfaces.
 */
base.plugin("blocks.core.elements.Dropspot", ["base.core.Class", "constants.blocks.core", "messages.blocks.core", "blocks.core.UI", function (Class, BlocksConstants, BlocksMessages, UI)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Dropspot = Class.create(blocks.elements.Surface, {

        //-----STATICS-----

        //-----CONSTANTS-----

        //-----VARIABLES-----
        //the parent surface of this dropspot
        anchor: undefined,
        //the side on the anchor
        side: undefined,

        //-----CONSTRUCTORS-----
        /**
         * A dropspot is a place between two surfaces where we can drop another surface.
         * This constructor immediately draws the new dropspot on the page.
         *
         * @param anchor The parent surface of this dropspot
         * @param side The side (top, right, bottom left) of the dropspot, relative to the anchor
         */
        constructor: function (anchor, side)
        {
            blocks.elements.Dropspot.Super.call(this);

            this.anchor = anchor;
            this.side = side;

            //instead of constructing the element and hiding it until it's needed,
            //we decided to create/destroy the element during show()/hide() instead
            this.overlay = undefined;
        },

        //-----PUBLIC METHODS-----
        createOverlay: function()
        {
            this.overlay = this._createOverlay(UI.dropspotWrapper);
            this.overlay.addClass(this.side.cssClass);

            switch (this.side.id) {
                case blocks.elements.Surface.SIDE.TOP.id:
                    this.overlay.css('top', this.anchor.top);
                    this.overlay.css('left', this.anchor.left);
                    this.overlay.css('width', this.anchor.right - this.anchor.left);
                    break;
                case blocks.elements.Surface.SIDE.RIGHT.id:
                    this.overlay.css('top', this.anchor.top);
                    this.overlay.css('left', this.anchor.right - BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH);
                    this.overlay.css('height', this.anchor.bottom - this.anchor.top);
                    break;
                case blocks.elements.Surface.SIDE.BOTTOM.id:
                    this.overlay.css('top', this.anchor.bottom - BlocksConstants.BLOCKS_DROPSPOT_BORDER_WIDTH);
                    this.overlay.css('left', this.anchor.left);
                    this.overlay.css('width', this.anchor.right - this.anchor.left);
                    break;
                case blocks.elements.Surface.SIDE.LEFT.id:
                    this.overlay.css('top', this.anchor.top);
                    this.overlay.css('left', this.anchor.left);
                    this.overlay.css('height', this.anchor.bottom - this.anchor.top);
                    break;
            }
        },

        //-----PRIVATE METHODS-----
        _getType: function()
        {
            return 'dropspot';
        },
        _getName: function()
        {
            return BlocksMessages.surfaceDropspotName;
        },
    });

}]);
