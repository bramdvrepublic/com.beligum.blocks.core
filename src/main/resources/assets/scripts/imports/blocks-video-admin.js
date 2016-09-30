/*
 * Allows editing of an video when you click on it
 * */
base.plugin("blocks.imports.VideoAdmin", ["base.core.Class", "blocks.imports.Property", "base.core.Commons", "constants.blocks.core", "messages.blocks.core", "blocks.core.Sidebar", "blocks.core.Notification", "base.core.Mime", function (Class, Property, Commons, BlocksConstants, BlocksMessages, Sidebar, Notification, Mime)
{
    var VideoAdmin = this;
    this.TAGS = ["video"];

    //check if the media classes are loaded
    var MediaCommonsConstants = base.getPlugin("constants.blocks.media.commons");
    if (!MediaCommonsConstants) {
        throw Logger.error("Can't seem to find the media libraries; this shouldn't happen");
    }

    (this.Class = Class.create(Property.Class, {

        //-----VARIABLES-----
        videoSourceTimeout: null,
        videoEl: null,

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

            this.videoEl = block.element.find('video');
            this._videoPause(true);
        },
        blur: function (block, element)
        {
            VideoAdmin.Class.Super.prototype.blur.call(this, block, element);

            this.videoEl = null;
        },
        getConfigs: function (block, element)
        {
            var _this = this;

            var retVal = VideoAdmin.Class.Super.prototype.getConfigs.call(this, block, element);

            var posterWidget = this.createTextInput(Sidebar,
                function getterFunction()
                {
                    var retVal = null;

                    retVal = _this.videoEl.attr('poster');
                    if (retVal) {
                        //cut off any possible alias (we don't show it to the user)
                        retVal = _this._trimAlias(retVal);
                    }

                    return retVal;
                },
                function setterFunction(val)
                {
                    _this.videoEl.removeAttr('poster');

                    if (val && val !== '') {

                        //make sure we start out with an unaliased version to build from
                        var baseVal = _this._trimAlias(val);

                        var mimeType = Mime.lookup(baseVal);

                        //we support video (proxy) and image files
                        var posterUrl = null;
                        if (mimeType.indexOf('image/') == 0) {
                            posterUrl = baseVal;
                        }
                        else if (mimeType.indexOf('video/') == 0) {
                            if (baseVal.indexOf('/') == 0) {
                                posterUrl = baseVal + MediaCommonsConstants.HDFS_URL_ALIAS_DIVIDER + MediaCommonsConstants.HDFS_URL_ALIAS_VIDEO_POSTER;
                            }
                        }

                        if (posterUrl) {
                            _this._loadPoster(posterUrl, MediaCommonsConstants, function callback(loadedUrl, error)
                            {
                                if (loadedUrl) {
                                    _this.videoEl.attr('poster', loadedUrl);

                                    //note that we only save the aliased url in the poster attr, not show it to the user in the input
                                    var basePosterUrl = _this._trimAlias(loadedUrl);

                                    //Note: jquery val() doesn't trigger any events, do it manually
                                    var posterInput = posterWidget.find('input:text');
                                    if (posterInput.val() != basePosterUrl) {
                                        posterInput.val(basePosterUrl);
                                        //don't trigger the change or we'll have infinite recursion
                                        //posterInput.trigger("change");
                                    }
                                }
                                else {
                                    Logger.error("Error caught while loading video thumbnail", error);
                                }

                                _this._videoReload();
                            });
                        }
                        else {
                            _this._videoReload();
                        }
                    }
                    else {
                        _this._videoReload();
                    }
                },
                BlocksMessages.widgetVideoPosterTitle,
                BlocksMessages.widgetVideoPosterPlaceholder,
                false, this.buildInputActions(Sidebar, true, false)
            );

            var sourceWidget = this.createTextInput(Sidebar,
                function getterFunction()
                {
                    var retVal = null;

                    retVal = _this.videoEl.find('source').first().attr('src');
                    if (retVal) {
                        //cut off any possible alias (we don't show it to the user)
                        retVal = _this._trimAlias(retVal);
                    }

                    return retVal;
                },
                function setterFunction(val)
                {
                    _this.videoEl.find('source').remove();

                    if (val && val !== '') {

                        //only use the last timeout
                        if (_this.videoSourceTimeout) {
                            clearTimeout(_this.videoSourceTimeout);
                            _this.videoSourceTimeout = null;
                        }

                        //request the mime type from the server and set it if we have it
                        _this.videoSourceTimeout = setTimeout(function ()
                        {
                            //make sure we start out with an unaliased version to build from
                            var baseVal = _this._trimAlias(val);

                            var mimeType = Mime.lookup(baseVal);

                            //we only support video files
                            if (mimeType.indexOf('video/') == 0) {

                                //changing the source while playing doesn't work
                                _this._videoPause(true);

                                //Note: we can't use the mime time of the original,
                                // but make sure the server transcode settings support all in this list
                                // and the constants are modeled after the syntax in the loop
                                var formats = ["mp4"];
                                for (var i = 0; i < formats.length; i++) {

                                    //this is both the mime type and also (no coincidence) the constants of the alias proxy
                                    var formatMime = "video/" + formats[i];

                                    //if the url is a local url, append it with a proxy alias
                                    var videoUrl = baseVal;
                                    if (videoUrl.indexOf('/') == 0) {
                                        videoUrl = baseVal + MediaCommonsConstants.HDFS_URL_ALIAS_DIVIDER + formatMime;
                                    }

                                    var newSrc = $('<source>').attr('src', videoUrl).attr('type', formatMime).prependTo(_this.videoEl);

                                    //because the first source file will be loaded on focus (see above),
                                    // we need to sync the behavior with that input here
                                    if (i == 0) {
                                        var sourceInput = sourceWidget.find('input:text');
                                        //note that we only save the aliased url in the source element, not show it to the user in the input
                                        var baseVideoUrl = _this._trimAlias(videoUrl);
                                        if (sourceInput.val() != baseVideoUrl) {
                                            sourceInput.val(baseVideoUrl);
                                            //don't trigger the change or we'll have infinite recursion
                                            //sourceInput.trigger("change");
                                        }

                                        //we propagate the video change to the poster input by passing the video file and letting the code above do it's work
                                        var posterInput = posterWidget.find('input:text');
                                        if (posterInput.val() != baseVideoUrl) {
                                            posterInput.val(baseVideoUrl);
                                            //we do need to trigger this one to activate it
                                            posterInput.trigger("change");
                                        }
                                    }
                                }
                            }
                            else {
                                //Annoying?
                                //Notification.error(BlocksMessages.widgetVideoErrorNoVideoFile);
                            }

                            //make sure the video is always reloaded
                            _this._videoReload();
                        }, 500);
                    }
                    else {
                        //also reload the video if the value is cleared
                        _this._videoReload();
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
            retVal.push(this.addOptionalClass(Sidebar, _this.videoEl, BlocksMessages.widgetVideoControlsTitle, null, null, 'controls'));
            retVal.push(this.addOptionalClass(Sidebar, _this.videoEl, BlocksMessages.widgetVideoAutoplayTitle, null, null, 'autoplay'));
            retVal.push(this.addOptionalClass(Sidebar, _this.videoEl, BlocksMessages.widgetVideoLoopTitle, null, null, 'loop'));
            retVal.push(this.addOptionalClass(Sidebar, _this.videoEl, BlocksMessages.widgetVideoMutedTitle, null, null, 'muted'));

            var unsupportedMessage = BlocksMessages.widgetVideoUnsupported1;
            var firstSrc = _this.videoEl.find('source').first().attr('src');
            if (firstSrc) {
                unsupportedMessage += Commons.format(BlocksMessages.widgetVideoUnsupported2, firstSrc);
            }
            unsupportedMessage += ".";

            //replaces the default text content, not any of the nodes
            _this.videoEl.contents().filter(function ()
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
        _trimAlias: function (val)
        {
            var retVal = val;

            var idx = retVal.lastIndexOf(MediaCommonsConstants.HDFS_URL_ALIAS_DIVIDER);
            if (idx >= 0) {
                retVal = retVal.substring(0, idx);
            }

            return retVal;
        },
        _loadPoster: function (posterUrl, MediaCommonsConstants, callback)
        {
            var retVal = null;

            if (callback) {
                try {
                    var thumb = $('<img/>')
                    thumb.on('load', function ()
                    {
                        callback(posterUrl);
                    });
                    thumb.on('error', function (e)
                    {
                        //make sure it's only called once
                        $(this).unbind("error");

                        callback(null, e);
                    });

                    //now try to fire up the loading
                    thumb.attr("src", posterUrl);
                }
                catch (e) {
                    callback(null, e);
                }
            }

            return retVal;
        },
        _videoReload: function()
        {
            if (this.videoEl && this.videoEl.length) {
                this.videoEl.get(0).load();
            }
        },
        _videoPlaying: function()
        {
            var retVal = false;

            if (this.videoEl && this.videoEl.length) {
                retVal = !this.videoEl.get(0).paused;
            }

            return retVal;
        },
        _videoPause: function(pause)
        {
            if (this.videoEl && this.videoEl.length) {
                if (pause) {
                    this.videoEl.get(0).pause();
                }
                else {
                    this.videoEl.get(0).play();
                }
            }
        }

    })).register(this.TAGS);

}]);