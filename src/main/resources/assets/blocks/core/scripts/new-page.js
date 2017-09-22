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
 * Created by bram on 12/8/16.
 */
base.plugin("blocks.core.NewPage", ["base.core.Class", "constants.blocks.core", "messages.blocks.core", "base.core.Commons", "blocks.core.Notification", function (Class, BlocksConstants, BlocksMessages, Commons, Notification)
{
    var NewPage = this;

    var translations = $('.' + BlocksConstants.NEW_PAGE_CLASS + ' .actions .translations a');
    translations.click(function (e)
    {
        var link = $(this);
        NewPage.handleSelect(null, link.attr(BlocksConstants.NEW_PAGE_TRANSLATION_ATTR), null, true);
    });

    var input = $('.' + BlocksConstants.NEW_PAGE_CLASS + ' .actions .copy input');

    var MAX_RESULTS = 10;
    var acEndpointURL = BlocksConstants.RDF_RESOURCES_ENDPOINT + '?'
        //we want to search for all types, so don't specify a type curie
        //+ BlocksConstants.RDF_RES_TYPE_CURIE_PARAM + '=' + '' + '&'
        + BlocksConstants.RDF_MAX_RESULTS_PARAM + '=' + MAX_RESULTS + '&'
        + BlocksConstants.RDF_PREFIX_SEARCH_PARAM + '=true' + '&'
        + BlocksConstants.RDF_QUERY_PARAM + '=';

    //init the typeahead plugin
    var engine = new Bloodhound(
        {
            queryTokenizer: Bloodhound.tokenizers.whitespace,
            datumTokenizer: Bloodhound.tokenizers.whitespace,
            remote: {
                //note: the prepare function below will add the correct query at the end
                url: acEndpointURL,
                prepare: function (query, settings)
                {
                    settings.url = settings.url + encodeURIComponent(query);
                    return settings;
                },
            },
        });

    var options = {
        highlight: false,
        minLength: 1,
        hint: true,
    };

    var dataSet = {
        name: 'allPages',
        source: engine,
        //workaround for bug https://github.com/twitter/typeahead.js/issues/1201#issuecomment-185854471
        limit: MAX_RESULTS - 1,
        //sync this with the title field of com.beligum.blocks.fs.index.entries.PageIndexEntry
        display: 'title',
        templates: {
            empty: '<div class="tt-suggestion "' + BlocksConstants.INPUT_TYPE_RES_SUG_EMPTY_CLASS + '><p class="' + BlocksConstants.INPUT_TYPE_RES_SUG_TITLE_CLASS + '">' + BlocksMessages.emptySearchResultsTitle + '</p></div>',
            //we add title (hover) tags as well because the css will probably chop it off (ellipsis overflow)
            suggestion: Handlebars.compile('<div title="{{title}} - {{subTitle}}"><p class="' + BlocksConstants.INPUT_TYPE_RES_SUG_TITLE_CLASS + '">{{title}}</p><p class="' + BlocksConstants.INPUT_TYPE_RES_SUG_SUBTITLE_CLASS + '">{{subTitle}}</p></div>')
        }
    };
    input.typeahead(options, dataSet);

    //gets called when a real selection is done
    input.bind('typeahead:select', function (ev, suggestion)
    {
        NewPage.handleSelect(suggestion.title, suggestion.publicPage, suggestion.value, false);
    });

    this.handleSelect = function (pageTitle, pagePublicUrl, pageResourceUrl, linkResources)
    {
        var message = BlocksMessages.newPageCopyDialogMessage +
            '<blockquote><p><a href="' + pagePublicUrl + '" target="_blank" class="text-info">' + pagePublicUrl + '</a></p></blockquote>';

        BootstrapDialog.show({
            title: BlocksMessages.newPageCopyDialogTitle,
            message: message,
            type: BootstrapDialog.TYPE_INFO,
            buttons: [
                {
                    id: 'btn-close',
                    label: BlocksMessages.cancel,
                    action: function (dialogRef)
                    {
                        dialogRef.close();
                    }
                },
                {
                    id: 'btn-ok',
                    label: BlocksMessages.ok,
                    cssClass: 'btn-info',
                    action: function (dialogRef)
                    {
                        var createUrl = BlocksConstants.NEW_PAGE_TEMPLATE_ENDPOINT + '?'
                            + BlocksConstants.NEW_PAGE_URL_PARAM + '=' + encodeURIComponent(window.location) + '&'
                            + BlocksConstants.NEW_PAGE_COPY_URL_PARAM + '=' + encodeURIComponent(pagePublicUrl) + '&'
                            + BlocksConstants.NEW_PAGE_COPY_LINK_PARAM + '=' + (linkResources ? 'true' : 'false');

                        window.location = createUrl;

                        dialogRef.close();
                    }
                }
            ]
        })
    };

}]);
