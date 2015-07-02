package com.beligum.blocks.search;

import com.beligum.base.server.R;
import com.beligum.base.server.RequestContext;
import com.beligum.base.templating.ifaces.Template;
import com.beligum.blocks.config.BlocksConfig;
import com.beligum.blocks.endpoints.ApplicationEndpoint;
import com.beligum.blocks.resources.interfaces.Resource;
import gen.com.beligum.blocks.core.fs.html.views.search;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by wouter on 27/06/15.
 */
public class ResourceSearch
{
    public static final String PAGE_PARAMETER = "p";
    public static final String QUERY_PARAMETER = "q";
    public static final String LETTER_PARAMETER = "l";

    public static Response getSearchPage(boolean showExample, String exampleStartLetter, String searchTerm, long page, SearchCommand resourceSearch, String itemTemplateName, String  mainTemplateName) throws Exception
    {
        URI uri = RequestContext.getJaxRsRequest().getUriInfo().getRequestUri();
        Response retVal = null;

        // Find the getLanguage of this url. If not found redirect internally to the default method to show pages
        Locale locale = null;
        java.nio.file.Path path = Paths.get(uri.getPath());
        if (path.getNameCount() > 0) {
            locale = BlocksConfig.instance().getLocaleForLanguage(path.getName(0).toString());
        }

        // if getLanguage does not exist -> redirect
        if (locale == null) {
            ApplicationEndpoint endpoint = new ApplicationEndpoint();
            retVal = endpoint.getPageWithId(RequestContext.getJaxRsRequest().getUriInfo().getRequestUri().getPath().toString());
        }

        // no problems encountered so proceed with search
        if (retVal == null) {
            StringBuilder results = new StringBuilder();
            Template searchTemplate = search.get().getNewTemplate();
            if (searchTerm != null || !showExample) {
                // do search for this name
                long total = resourceSearch.totalHits(searchTerm, locale);
                Long pagers = (total / (long) SearchCommand.RESOURCES_ON_PAGE);
                if (total % SearchCommand.RESOURCES_ON_PAGE > 0)
                    pagers++;
                if (page > pagers || page < 1)
                    page = 1;

                // Distribute the pager, try to put current page in teh middle
                Long min = 1L;
                Long max = pagers > min ? pagers : min;
                if (total > SearchCommand.RESOURCES_ON_PAGE) {
                    if (page - 5 < 1) {
                        Long newMax = min + 10L;
                        max = newMax < max ? newMax : max;
                    }
                    else if (page + 5L > pagers) {
                        Long newMin = pagers - 10L;
                        min = newMin > min ? newMin : min;
                    }
                    else {
                        min = page - 5L;
                        max = page + 5L;
                    }
                }
                if (page > max)
                    page = 1;

                // Set parameters for the pagination in the template
                List<Map<String, Object>> pages = new ArrayList<>();
                UriBuilder uriBuilder = UriBuilder.fromUri(uri);
                for (long i = min; i <= max; i++) {
                    uriBuilder.replaceQueryParam(PAGE_PARAMETER, i);
                    Map<String, Object> p = new HashMap<>();
                    p.put("index", i);
                    p.put("url", uriBuilder.build().toString());
                    pages.add(p);
                }

                List<Resource> resources = resourceSearch.search(searchTerm, page, locale);

                // create string of results
                //            results.append("<div property=\"result\">").append(System.lineSeparator());

                StringBuilder item = new StringBuilder().append("<").append(itemTemplateName).append(">").append("</").append(itemTemplateName).append(">");
                Template itemTemplate = R.templateEngine().getNewStringTemplate(item.toString());
                for (Resource resource : resources) {
                    itemTemplate.set("resource", resource);
                    results.append(itemTemplate.toString()).append(System.lineSeparator());
                }

                //            results.append("</div>");

                searchTemplate.set("search_active_page", page);
                searchTemplate.set("search_pages", pages);
                searchTemplate.set("search_last_page", pages.size() - 1);

            }
            else {
                // do show list of possible names to search for starting with the letter
                if (exampleStartLetter == null || exampleStartLetter.length() > 1)
                    exampleStartLetter = "a";


                UriBuilder uriBuilder = UriBuilder.fromUri(uri);

                List<HashMap<String, Object>> letters = new ArrayList<>(26);
                for (char c = 'a'; c <= 'z'; c++) {
                    HashMap<String, Object> pagerItem = new HashMap<>();
                    pagerItem.put("index", String.valueOf(c));
                    pagerItem.put("url", uriBuilder.replaceQuery("").replaceQueryParam(ResourceSearch.LETTER_PARAMETER, String.valueOf(c)).build().toString());
                    letters.add(pagerItem);
                }

                LinkedHashSet<String> names = new LinkedHashSet<>();
                names.addAll(resourceSearch.getTerms(exampleStartLetter, locale));


                for (String resultName : names) {
                    String url = uriBuilder.replaceQuery("").replaceQueryParam(QUERY_PARAMETER, resultName.trim()).build().toString();
                    results.append("<div>").append("<a href=\"").append(url).append("\"").append(" >").append(resultName).append("</a>").append("</div>");
                }

                searchTemplate.set("search_active_page", page);
                searchTemplate.set("search_pages", letters);
                searchTemplate.set("search_last_page", 25);

            }

            searchTemplate.set("results", results.toString());

            searchTemplate.set("search_query", searchTerm);

            StringBuilder main = new StringBuilder().append("<").append(mainTemplateName).append(">").append(searchTemplate.render()).append("</").append(mainTemplateName).append(">");
            Template mainTemplate = R.templateEngine().getNewStringTemplate(main.toString());

            retVal = Response.ok(mainTemplate.render()).build();

        }
        return retVal;
    }
}
