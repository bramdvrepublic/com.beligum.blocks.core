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
 * A bootstrap column element
 */
base.plugin("blocks.core.elements.Column", ["base.core.Class", "messages.blocks.core", function (Class, BlocksMessages)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    var classPrefix = 'col-';
    blocks.elements.Column = Class.create(blocks.elements.Surface, {

        //-----STATICS-----
        STATIC: {

            //the prefix of the bootstrap column class
            CLASS_PREFIX: classPrefix,

            // \b = beginning of a word
            // \d = digit
            // g: global
            // i: ignore case
            DIMS_REGEX: new RegExp('\\b' + classPrefix + '([^-]*)-(\\d+)', 'gi'),

            /**
             * Create the correct class name for a column with the specified size and width
             *
             * @param columnSize
             * @param columnWidth
             * @returns {string}
             * @private
             */
            _createClass: function (columnSize, columnWidth)
            {
                return blocks.elements.Column.CLASS_PREFIX + columnSize + '-' + columnWidth;
            },
            createElement: function (columnSize, columnWidth, tagName)
            {
                return $('<' + (tagName ? tagName : blocks.elements.Surface.DEFAULT_TAG) + ' class="' + blocks.elements.Column._createClass(columnSize, columnWidth) + '" />');
            },
        },

        //-----CONSTANTS-----

        //-----VARIABLES-----
        //this will be filled with the full class, eg. col-md-4
        columnClass: undefined,
        //this will be filled with xs, sm, md or lg
        columnSize: undefined,
        //this will be filled with the bootstrap grid width number: [1-12]
        columnWidth: undefined,

        //-----CONSTRUCTORS-----
        constructor: function (parentSurface, element)
        {
            blocks.elements.Column.Super.call(this, parentSurface, element);

            this._extractColumnDimensions();
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
            colWidth = Math.max(Math.min(colWidth, blocks.elements.Row.MAX_COLS), blocks.elements.Row.MIN_COLS);

            var newClass = blocks.elements.Column.CLASS_PREFIX + this.columnSize + '-' + colWidth;

            this.element.removeClass(this.columnClass);
            this.element.addClass(newClass);

            //don't forget to sync the variables
            this.columnClass = newClass;
            this.columnWidth = colWidth;
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
            return blocks.elements.Surface.isRow(element) ? new blocks.elements.Row(this, element) : new blocks.elements.Block(this, element);
        },
        _isAcceptableChild: function (element)
        {
            return blocks.elements.Surface.isRow(element) || blocks.elements.Surface.isBlock(element);
        },
        _getChildOrientation: function ()
        {
            return blocks.elements.Surface.ORIENTATION.VERTICAL;
        },
        /**
         * Overloads the parent surface function to simplify a column where all children are rows with one full-width child column.
         * This happens when we need to create a nested row/col because we dropped a block left or right of another block and moved
         * it away again, resulting in a couple of nested rows with full-width columns and blocks in them. Those blocks can easily be
         * moved to this column without much loss of layout (except possible extra - and unnecessary - padding because of the nesting)
         *
         * @param deep
         * @private
         */
        _simplify: function (deep)
        {
            var retVal = false;

            //this is a lot of iteration, I know, but we need to detect the situation before we can alter it
            var allFullRows = true;
            for (var i = 0; i < this.children.length && allFullRows; i++) {
                var child = this.children[i];
                allFullRows &= child.isRow() && child.children.length === 1 && child.children[0].isColumn() && child.children[0].columnWidth === blocks.elements.Row.MAX_COLS;
            }

            if (allFullRows) {
                var blocksToAdd = [];
                for (var i = 0; i < this.children.length; i++) {
                    var childRow = this.children[i];
                    //above check already detected the child row only has one child: a full width col
                    var childCol = childRow.children[0];
                    for (var j = 0; j < childCol.children.length; j++) {
                        var childBlock = childCol.children[j];
                        childCol._removeChild(childBlock);
                        j--;

                        // Instead of adding the child to the parent column directly,
                        // we move it to a temp array and iterate it again when cleanup is done.
                        // Stupid, but this seems to solve a lot of issues with the iteration
                        // of the children in the parent loop
                        blocksToAdd.push(childBlock);

                        retVal = true;
                    }

                    this._removeChild(childRow);
                    i--;

                    retVal = true;
                }

                for (var i = 0; i < blocksToAdd.length; i++) {
                    this._addChild(blocksToAdd[i]);

                    retVal = true;
                }
            }

            //now call the superclass function to iterate the children
            retVal = blocks.elements.Row.Super.prototype._simplify.call(this, deep) || retVal;

            return retVal;
        },
        /**
         * Extracts the class width number from the element,
         * searching for a supplied clazz prefix eg. 'col-md-', 'col-xs-', etc
         * and extracting the size (xs, sm, md, lg) and the width ([1-12])
         * @private
         */
        _extractColumnDimensions: function ()
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
                    Logger.error('Found an element with multiple bootstrap classes, this shouldn\'t happen', this.element);
                }
            }
        },
    });
}]);
