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
 * An element with 4 corners.
 */
base.plugin("blocks.core.Elements.Surface", ["base.core.Class", function (Class)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.Surface = Class.create({

        //-----STATICS-----

        //-----CONSTANTS-----

        //-----VARIABLES-----
        top: 0,
        bottom: 0,
        left: 0,
        right: 0,

        // The values above don't necessarily represent
        // the real bounds of this surface because we sometimes
        // sync them with the bounds of their parents for a more natural
        // look. The values below hold the original bounds
        // that were used during construction.
        realTop: 0,
        realBottom: 0,
        realLeft: 0,
        realRight: 0,

        //-----CONSTRUCTORS-----
        /**
         * @param top the upper coordinate of this surface, relative to the document
         * @param bottom the lower coordinate of this surface, relative to the document
         * @param left the leftmost coordinate of this surface, relative to the document
         * @param right the rightmost coordinate of this surface, relative to the document
         */
        constructor: function (top, bottom, left, right)
        {
            if (top <= bottom) {
                this.top = top;
                this.bottom = bottom;
            }
            else {
                this.top = bottom;
                this.bottom = top;
            }

            if (left <= right) {
                this.left = left;
                this.right = right;
            }
            else {
                this.left = right;
                this.right = left;
            }

            this.realTop = this.top;
            this.realBottom = this.bottom;
            this.realLeft = this.left;
            this.realRight = this.right;
        },

        //-----PUBLIC METHODS-----

        //-----PRIVATE METHODS-----
        /**
         * Calculates the top value for the constructor of the supplied element
         */
        calculateTop: function (element)
        {
            return element.offset().top
        },
        /**
         * Calculates the bottom value for the constructor of the supplied element
         */
        calculateBottom: function (element)
        {
            return element.offset().top + element.outerHeight();
        },
        /**
         * Calculates the left value for the constructor of the supplied element
         */
        calculateLeft: function (element)
        {
            return element.offset().left
        },
        /**
         * Calculates the right value for the constructor of the supplied element
         */
        calculateRight: function (element)
        {
            return element.offset().left + element.outerWidth()
        },
        /**
         * Returns true of the supplied coordinate is inside this surface, it's bounds included
         */
        isTriggered: function (x, y)
        {
            return this.top <= y && y <= this.bottom && this.left <= x && x <= this.right;
        },
    });

}]);
