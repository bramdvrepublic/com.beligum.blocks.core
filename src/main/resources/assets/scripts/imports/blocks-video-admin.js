/*
 * Allows editing of an video when you click on it
 * */
base.plugin("blocks.imports.VideoAdmin", ["base.core.Class", "blocks.imports.Property", "base.core.Commons", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.Notification", "base.core.Mime", function (Class, Property, Commons, BlocksConstants, BlocksMessages, Sidebar, Notification, Mime)
{
    var VideoAdmin = this;
    this.TAGS = ["video"];

    (this.Class = Class.create(Property.Class, {

        //-----VARIABLES-----
        videoSourceTimeout: null,

        //-----CONSTRUCTORS-----
        constructor: function ()
        {
            VideoAdmin.Class.Super.call(this);
        },

        //-----IMPLEMENTED METHODS-----
        init: function ()
        {
        },
        focus: function (block, element, hotspot, event)
        {
            VideoAdmin.Class.Super.prototype.focus.call(this, block, element, hotspot, event);
        },
        blur: function (block, element)
        {
            VideoAdmin.Class.Super.prototype.blur.call(this, block, element);
        },
        getConfigs: function (block, element)
        {
            var _this = this;

            var retVal = VideoAdmin.Class.Super.prototype.getConfigs.call(this, block, element);

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

            var posterWidget = this.createTextInput(Sidebar,
                function getterFunction()
                {
                    return videoEl.attr('poster');
                },
                function setterFunction(val)
                {
                    if (val && val !== '') {
                        videoEl.attr('poster', val);
                    }
                    else {
                        videoEl.removeAttr('poster');
                    }
                },
                BlocksMessages.widgetVideoPosterTitle,
                BlocksMessages.widgetVideoPosterPlaceholder,
                false, this.buildInputActions(Sidebar, true, false)
            );

            var sourceWidget = this.createTextInput(Sidebar,
                function getterFunction()
                {
                    return firstSrcEl ? firstSrcEl.attr('src') : null;
                },
                function setterFunction(val)
                {
                    videoEl.find('source').remove();
                    //videoEl.removeAttr('poster');

                    if (val && val !== '') {

                        //check if the media classes are loaded
                        var MediaCommonsConstants = base.getPlugin("constants.blocks.media.commons");
                        if (MediaCommonsConstants) {

                            //only use the last timeout
                            if (_this.videoSourceTimeout) {
                                clearTimeout(_this.videoSourceTimeout);
                                _this.videoSourceTimeout = null;
                            }

                            //request the mime type from the server and set it if we have it
                            _this.videoSourceTimeout = setTimeout(function ()
                            {
                                var mimeType = Mime.lookup(val);

                                if (mimeType.indexOf('video/') == 0) {
                                    //this gives us access to the plain JS api
                                    var video = videoEl.get(0);

                                    //changing the source while playing doesn't work
                                    if (!video.paused) {
                                        video.pause();
                                    }

                                    //Note: we can't use the mime time of the original,
                                    // but make sure the server transcode settings support all in this list
                                    var formats = ["mp4"];
                                    var baseVal = val.substring(val.lastIndexOf(MediaCommonsConstants.HDFS_URL_ALIAS_SEMI_AUTO));
                                    for (var i = 0; i < formats.length; i++) {
                                        var format = formats[i];
                                        var formatMime = "video/" + format;
                                        var alias = MediaCommonsConstants.HDFS_URL_ALIAS_SEMI_AUTO.replace(new RegExp('\\*', 'g'), format);
                                        var aliasedUrl = baseVal + MediaCommonsConstants.HDFS_URL_ALIAS_DIVIDER + alias;
                                        var newSrc = $('<source>').attr('src', aliasedUrl).attr('type', formatMime).prependTo(videoEl);

                                        //because the first source file will be loaded on focus (see above),
                                        // we need to sync the behavior with that input here
                                        if (i == 0) {
                                            var sourceInput = sourceWidget.find('input:text');
                                            sourceInput.val(aliasedUrl);
                                            //don't trigger the change or we'll have infinite recursion
                                            //sourceInput.trigger("change");
                                        }
                                    }

                                    _this._loadPoster(baseVal, MediaCommonsConstants, function callback(posterUrl, error)
                                    {
                                        if (posterUrl) {
                                            //Note: jquery val() doesn't trigger any events, do it manually
                                            var posterInput = posterWidget.find('input:text');
                                            posterInput.val(posterUrl);
                                            posterInput.trigger("change");
                                        }
                                        else {
                                            Logger.error("Error caught while loading video thumbnail", error);
                                        }

                                        //make sure the video is reloaded
                                        video.load();
                                    });
                                }
                                else {
                                    //Annoying?
                                    //Notification.error(BlocksMessages.widgetVideoErrorNoVideoFile);
                                }
                            }, 500);
                        }
                        else {
                            Logger.error("Can't seem to find the media libraries; this shouldn't happen");
                        }
                    }
                },
                BlocksMessages.widgetVideoSourceTitle,
                BlocksMessages.widgetVideoSourcePlaceholder,
                false, this.buildInputActions(Sidebar, true, false)
            );

            retVal.push(sourceWidget);
            retVal.push(posterWidget);

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
        _loadPoster: function (videoUrl, MediaCommonsConstants, callback)
        {
            var retVal = null;

            if (callback) {
                try {
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
                catch (e) {
                    callback(null, e);
                }
            }

            return retVal;
        }

    })).register(this.TAGS);

}]);