/*
 * Allows editing of an video when you click on it
 * */
base.plugin("blocks.imports.Video", ["base.core.Class", "blocks.imports.Property", "base.core.Commons", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.Notification", "base.core.Mime", function (Class, Property, Commons, BlocksConstants, BlocksMessages, Sidebar, Notification, Mime)
{
    var Video = this;
    this.TAGS = ["video"];

    (this.Class = Class.create(Property.Class, {

        //-----VARIABLES-----
        videoSourceTimeout: null,
        lastTriedVideoUrl: null,

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            Video.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            Video.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            Video.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element)
        {
            var _this = this;

            var retVal = Video.Class.Super.prototype.getConfigs.call(this, block, element);

            var videoEl = block.element.find('video');

            var firstSrcEl = null;
            videoEl.find('source').each(function ()
            {
                var retVal = true;

                var sourceEl = $(this);
                if (!firstSrcEl && sourceEl.hasAttribute('src') && sourceEl.attr('src') != '') {
                    firstSrcEl = sourceEl;
                    retVal = true;
                }

                return retVal;
            });

            var sourceWidget = this.createTextInput(Sidebar,
                function getterFunction()
                {
                    return firstSrcEl ? firstSrcEl.attr('src') : null;
                },
                function setterFunction(val)
                {
                    videoEl.find('source').remove();
                    videoEl.removeAttr('poster');

                    if (val && val !== '' && val !== _this.lastTriedVideoUrl) {

                        //only use the last timeout
                        if (_this.videoSourceTimeout) {
                            clearTimeout(_this.videoSourceTimeout);
                            _this.videoSourceTimeout = null;
                        }

                        //request the mime type from the server and set it if we have it
                        _this.videoSourceTimeout = setTimeout(function ()
                        {
                            //this will make sure the same (if erroneous) server call isn't repeated over and over again
                            _this.lastTriedVideoUrl = val;

                            var mimeType = Mime.lookup(val);

                            if (mimeType.indexOf('video/') == 0) {
                                var newSrc = $('<source>').attr('src', val).attr('type', mimeType).prependTo(videoEl);
                                _this._getPoster(val, function callback(posterUrl, error)
                                {
                                    if (posterUrl) {
                                        videoEl.attr('poster', posterUrl);
                                    }
                                    else {
                                        Logger.error("Error caught while loading video thumbnail", error);
                                    }
                                    //videoEl[0].load();
                                });
                            }
                            else {
                                //Annoying?
                                //Notification.error(BlocksMessages.widgetVideoErrorNoVideoFile);
                            }
                        }, 1000);
                    }
                },
                BlocksMessages.widgetVideoSourceTitle,
                BlocksMessages.widgetVideoSourcePlaceholder,
                false, this.buildInputActions(Sidebar, true, false)
            );
            retVal.push(sourceWidget);

            retVal.push(this.addUniqueClass(Sidebar, block.element, BlocksMessages.widgetVideoAspectTitle, [
                {name: BlocksMessages.widgetVideoAspect16by9, value: BlocksConstants.VIDEO_ASPECT_16BY9_CLASS},
                {name: BlocksMessages.widgetVideoAspect4by3, value: BlocksConstants.VIDEO_ASPECT_4BY3_CLASS},
                {name: BlocksMessages.widgetVideoAspect1dot85by1, value: BlocksConstants.VIDEO_ASPECT_1DOT85BY1_CLASS},
                {name: BlocksMessages.widgetVideoAspect2dot39by1, value: BlocksConstants.VIDEO_ASPECT_2DOT39BY1_CLASS},
                {name: BlocksMessages.widgetVideoAspect1by1, value: BlocksConstants.VIDEO_ASPECT_1BY1_CLASS},
            ]));

            //Note: the 'controls' attr is the html5 one
            retVal.push(this.addOptionalClass(Sidebar, videoEl, BlocksMessages.widgetVideoControlsTitle, null, null, 'controls'));
            retVal.push(this.addOptionalClass(Sidebar, videoEl, BlocksMessages.widgetVideoAutoplayTitle, null, null, 'autoplay'));
            retVal.push(this.addOptionalClass(Sidebar, videoEl, BlocksMessages.widgetVideoLoopTitle, null, null, 'loop'));
            retVal.push(this.addOptionalClass(Sidebar, videoEl, BlocksMessages.widgetVideoMutedTitle, null, null, 'muted'));

            var unsupportedMessage = BlocksMessages.widgetVideoUnsupported1;
            if (firstSrcEl) {
                unsupportedMessage += Commons.format(BlocksMessages.widgetVideoUnsupported2, firstSrcEl.attr('src'));
            }
            unsupportedMessage += ".";

            //replaces the default text content, not any of the nodes
            videoEl.contents().filter(function ()
            {
                //Note: text nodes use the code 3
                return this.nodeType == 3;
            }).filter(':first').text(unsupportedMessage);

            return retVal;
        },
        getWindowName: function ()
        {
            return BlocksMessages.widgetVideoTitle;
        },

        //-----PRIVATE FUNCTIONS-----
        _getPoster: function (videoUrl, callback)
        {
            var retVal = null;

            if (callback) {
                try {
                    var MediaCommonsConstants = base.getPlugin("constants.blocks.media.commons");

                    //check if the media classes are loaded
                    if (MediaCommonsConstants) {
                        var thumbUrl = videoUrl + MediaCommonsConstants.HDFS_URL_ALIAS_DIVIDER + MediaCommonsConstants.HDFS_URL_ALIAS_FINDER_THUMB;

                        var thumb = $('<img/>')
                        thumb.on('load', function ()
                        {
                            callback(thumb.attr('src'));
                        });
                        thumb.on('error', function (e)
                        {
                            //make sure it's only called once
                            $(this).unbind("error");

                            callback(null, e);
                        });

                        //now try to fire up the loading
                        thumb.attr("src", thumbUrl);
                    }
                }
                catch (e) {
                    callback(null, e);
                }
            }

            return retVal;
        }

    })).register(this.TAGS);

}]);