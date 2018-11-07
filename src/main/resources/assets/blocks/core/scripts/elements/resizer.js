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

/*
 * defines a resizehandle. The surface of the resizeHandle is the are that triggers when you hover over it
 * the draw-surface is the surface that will be drawn in the dom (can be bigger or smaller).
 * left and rightcolumn are the columns that this handle will resize when dragged
 */
base.plugin("blocks.core.elements.ResizeHandle", ["base.core.Class", "constants.blocks.core", "blocks.core.Resizer", "blocks.core.UI", function (Class, BlocksConstants, Resizer, UI)
{
    //----PACKAGES-----
    blocks = window['blocks'] || {};
    blocks.elements = blocks.elements || {};

    //----CLASSES-----
    blocks.elements.ResizeHandle = Class.create(blocks.elements.Surface, {

        //-----STATICS-----
        STATIC: {
            DRAW_WIDTH: 30,
            TRIGGER_WIDTH: 6
        },

        //-----CONSTANTS-----

        //-----VARIABLES-----

        //-----CONSTRUCTORS-----
        constructor: function (leftColumn, rightColumn)
        {
            blocks.elements.ResizeHandle.Super.call(this);

            this.leftColumn = leftColumn;
            this.rightColumn = rightColumn;

            this.overlay = this._createOverlay();
            this.overlay.addClass(BlocksConstants.COLUMN_RESIZER_CLASS);
            UI.handleWrapper.append(this.overlay);

            this._redraw();

            // var _this = this;
            // this.overlay.on("mousedown.resizehandle", function (event)
            // {
            //     // only start drag on left click
            //     if (event.which == 1) {
            //         Resizer.startDrag(_this);
            //         $(document).on("mouseup.resizehandle", function (event)
            //         {
            //             $(document).off("mouseup.resizehandle");
            //             Resizer.endDrag(null);
            //         });
            //     }
            // });
        },

        //-----PUBLIC METHODS-----

        //-----TODO UNCHECKED-----
        update: function ()
        {
            var left = Math.floor((this._calculateLeft(this.rightColumn.element) + this._calculateRight(this.leftColumn.element)) / 2) - Math.floor(blocks.elements.ResizeHandle.TRIGGER_WIDTH / 2)
            this.overlay.css("left", left);
            var siblings = this.leftColumn.parent.resizeHandles;
            var height = this._calculateBottom(this.leftColumn.parent.element) - this._calculateTop(this.leftColumn.parent.element);
            for (var i = 0; i < siblings.length; i++) {
                siblings[i].overlay.css("height", height);
            }
        },
        updateHeight: function ()
        {
            var height = this.leftColumn.parent.element.height();
            this.overlay.css("height", height);
        },

        //-----PRIVATE METHODS-----
        _getType: function()
        {
            return 'resizer';
        },
        _redraw: function()
        {
            if (this.overlay) {

                var left = this.leftColumn.right - Math.floor(blocks.elements.ResizeHandle.TRIGGER_WIDTH / 2);
                var top = this.leftColumn.top;
                var width = blocks.elements.ResizeHandle.TRIGGER_WIDTH;
                var height = this.leftColumn.bottom - this.leftColumn.top;

                this.overlay.css("top", top);
                this.overlay.css("height", height);
                this.overlay.css("left", left);
                this.overlay.css("width", width);
            }
        },
    });
}]);