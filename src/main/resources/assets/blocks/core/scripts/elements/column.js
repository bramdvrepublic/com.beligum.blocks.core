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
 * A column (inside a row) -> Can contain rows or templates
 *
 * Created by wouter on 9/03/15.
 */
base.plugin("blocks.core.elements.Column", ["base.core.Class", "constants.base.core.internal", "messages.blocks.core", "blocks.core.DOM", function (Class, Constants, BlocksMessages, DOM)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    var classPrefix = 'col';
    blocks.elements.Column = Class.create(blocks.elements.Surface, {

        //-----STATICS-----
        STATIC: {

            //the prefix of the bootstrap column class
            CLASS_PREFIX: classPrefix,

            // \b = beginning of a word
            // \d = digit
            // g: global
            // i: ignore case
            DIMS_REGEX: new RegExp('\\b' + classPrefix + '-([^-]*)-(\\d+)', 'gi'),

        },

        //-----CONSTANTS-----

        //-----VARIABLES-----
        //this will be filled with the full class
        columnClass: undefined,
        //this will be filled with xs, sm, md or lg
        columnSize: undefined,
        //this will be filled with the bootstrap grid width number: [1-12]
        columnWidth: undefined,

        //-----CONSTRUCTORS-----
        constructor: function (parentSurface, element)
        {
            blocks.elements.Column.Super.call(this, parentSurface, element);

            this._updateColumnDimensions();
        },

        //-----PUBLIC METHODS-----
        /**
         * Updates the Bootstrap-width of this column in the [1-12] range
         * according to the current size
         *
         * @param colWidth
         */
        setColumnWidth: function (colWidth)
        {
            //make sure it's in the right range
            colWidth = Math.max(Math.min(colWidth, 12), 1);

            var newClass = blocks.elements.Column.CLASS_PREFIX + '-' + this.columnSize + '-' + colWidth;

            this.element.removeClass(this.columnClass);
            this.element.addClass(newClass);

            //don't forget to sync the variables
            this.columnClass = newClass;
            this.columnWidth = colWidth;
        },

        //-----TODO UNCHECKED-----
        // Override
        getElementAtSide: function (side)
        {
            if (side == Constants.SIDE.LEFT) {
                return this.getPrevious();
            }
            else if (side == Constants.SIDE.RIGHT) {
                return this.getNext();
            }
            else {
                return null;
            }
        },

        // find all dropspots for an element
        // is called for a block and returns all dropspots for this block and his parents.
        calculateDropspots: function (side, dropspots)
        {
            if ((side == Constants.SIDE.LEFT || side == Constants.SIDE.RIGHT)) {
                dropspots.push(new blocks.elements.Dropspot(side, this, dropspots.length));
            }

            if (this.isOuter(side) && this.parent != null) {
                dropspots = this.parent.calculateDropspots(side, dropspots);
            }

            return dropspots;
        },

        //-----PRIVATE METHODS-----
        _getType: function ()
        {
            return 'column';
        },
        _getName: function ()
        {
            return BlocksMessages.surfaceColumnName;
        },
        _newChildInstance: function (element)
        {
            return new blocks.elements.Block(this, element);
        },
        _isAcceptableChild: function (element)
        {
            return DOM.isBlock(element);
        },
        _getChildOrientation: function ()
        {
            return blocks.elements.Surface.ORIENTATION.VERTICAL;
        },
        // _isOuterLeft: function ()
        // {
        //     return this.element.prev().length == 0
        // },
        // _isOuterRight: function ()
        // {
        //     return this.element.next().length == 0
        // },
        /**
         * Extracts the class width number from the element,
         * searching for a supplied clazz prefix eg. 'col-md-', 'col-xs-', etc
         * and extracting the size (xs, sm, md, lg) and the width ([1-12])
         * @private
         */
        _updateColumnDimensions: function ()
        {
            this.columnClass = undefined;
            this.columnSize = undefined;
            this.columnWidth = undefined;

            //Note that we'll investigate the full class attribute
            var classes = this.element.attr("class");

            var match = blocks.elements.Column.DIMS_REGEX.exec(classes);
            //Note: no while loop means we'll only take the first match!
            if (match != null) {
                // matched text: match[0]
                // match start: match.index
                // capturing group n: match[n]

                this.columnClass = match[0];
                this.columnSize = match[1];
                this.columnWidth = parseInt(match[2]);

                //test if we had more and log a warning
                match = blocks.elements.Column.DIMS_REGEX.exec(classes);
                if (match != null) {
                    Logger.info('Found an element with multiple bootstrap classes, this shouldn\'t happen', this.element);
                }
            }
        },
    });
}]);
