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

base.plugin("blocks.core.UI", ["constants.base.core.internal", "constants.blocks.core", function (BaseConstantsInternal, BlocksConstants)
{
    var UI = this;

    //-----CONSTANTS-----

    //-----VARIABLES-----
    this.html = $("html");
    this.body = $("body");
    this.startButton = undefined;
    this.sidebar = undefined;
    this.pageContent = undefined;
    this.overlayWrapper = undefined;
    this.surfaceWrapper = undefined;
    this.resizerWrapper = undefined;
    this.dropspotWrapper = undefined;

    //-----PUBLIC METHODS-----
    //this is called just before BLOCKS_START is fired
    this.init = function ()
    {
        //this is created in page.js
        UI.newBlockBtn = $('.' + BlocksConstants.CREATE_BLOCK_CLASS);

        //before we start building the surfaces, make sure the wrappers are empty
        UI.surfaceWrapper.empty();
        UI.resizerWrapper.empty();
        UI.dropspotWrapper.empty();
    };

    this.showOverlays = function(show)
    {
        if (this.overlayWrapper) {
            if (show) {
                this.overlayWrapper.show();
            }
            else {
                this.overlayWrapper.hide();
            }
        }
    };

    //-----PRIVATE METHODS-----

}]);