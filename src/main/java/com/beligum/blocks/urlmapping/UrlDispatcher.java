package com.beligum.blocks.urlmapping;

import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.models.interfaces.BlocksStorable;

import java.net.URL;
import java.util.*;

/**
 * Created by wouter on 17/03/15.
 */
public abstract class UrlDispatcher extends UrlBranch implements BlocksUrlDispatcher, BlocksStorable
{

    private HashSet<String> possibleLanguages = null;
    private HashMap<String, String> idToUrl = new HashMap<String, String>();

    public UrlDispatcher()
    {
        this.resetIdToUrl();
    }

    protected void resetIdToUrl()
    {
        Stack<UrlBranch> stack = new Stack<>();
        this.idToUrl = collectIdsForUrl(stack);
        if (this.storedTemplateId != null) {
            this.idToUrl.put(this.storedTemplateId, "/");
        }
    }

    public BlockId findId(URL url)
    {
        BlockId retVal = null;
        ArrayList<String> paths = splitUrl(url);
        String language = getLanguage(paths);
        paths = getUrlWithoutLanguage(paths);
        UrlBranch branch = findBranch(paths, language, 0, false, SEARCH_OPTION.NORMAL);

        if (branch != null) {
            retVal = Blocks.factory().getIdForString(branch.getStoredTemplateId());
        }
        return retVal;
    }

    @Override
    public BlockId findPreviousId(URL url)
    {
        BlockId retVal = null;
        ArrayList<String> paths = splitUrl(url);
        String language = getLanguage(paths);
        paths = getUrlWithoutLanguage(paths);
        UrlBranch branch = findBranch(paths, language, 0, false, SEARCH_OPTION.NORMAL);

        if (branch != null) {
            ArrayList<UrlBranchHistory> history = branch.getDeleted();
            if (history.size() > 0) {
                UrlBranchHistory last = history.get(history.size() - 1);
                retVal = Blocks.factory().getIdForString(last.getStoredTemplateId());
            }
        }
        return retVal;
    }

    public void addId(URL url, BlockId id, String language)
    {
        String retVal = null;
        ArrayList<String> paths = splitUrl(url);
        paths = getUrlWithoutLanguage(paths);
        UrlBranch branch = findBranch(paths, language, 0, true, SEARCH_OPTION.NORMAL);
        branch.add(id.toString());
        String stringUrl = branch.getUrl();
        this.idToUrl.put(id.toString(), stringUrl);

        try {
            Blocks.database().save(this);
        }
        catch (Exception e) {

        }

    }

    public void removeId(URL urlWithLanguage) throws Exception
    {
        String retVal = null;
        ArrayList<String> paths = splitUrl(urlWithLanguage);
        String language = getLanguage(urlWithLanguage);
        paths = getUrlWithoutLanguage(paths);
        UrlBranch branch = findBranch(paths, language, 0, false, SEARCH_OPTION.NORMAL);
        if (branch != null) {
            this.idToUrl.remove(branch.getStoredTemplateId());
            branch.remove();
        }
        else {
            throw new Exception("Url does not exits. could not delete.");
        }

        // Save the sitemap to the  database
        try {
            Blocks.database().save(this);
        }
        catch (Exception e) {

        }
    }

    public String getUrlForId(String id)
    {
        return this.idToUrl.get(id);
    }

    private ArrayList<String> splitUrl(URL url)
    {
        String[] paths = url.getPath().split("/");
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(paths));
        if (list.size() > 0 && list.get(0).isEmpty()) {
            list.remove(0);
        }
        return list;
    }

    private boolean isPossibleLanguage(String language)
    {
        if (possibleLanguages == null) {
            this.possibleLanguages = new HashSet<>();
            this.possibleLanguages.addAll(Blocks.config().getLanguages());
        }
        return this.possibleLanguages.contains(language);
    }

    public String getLanguage(URL url)
    {
        String retVal = getLanguage(splitUrl(url));
        if (retVal == null) {
            retVal = Blocks.config().getDefaultLanguage();
        }
        return retVal;
    }

    public String getLanguageOrNull(URL url)
    {
        return getLanguage(splitUrl(url));
    }

    private String getLanguage(ArrayList<String> paths)
    {
        if (paths.size() > 0 && isPossibleLanguage(paths.get(0)))
            return paths.get(0);
        else
            return null;
    }

    private ArrayList<String> getUrlWithoutLanguage(ArrayList<String> paths)
    {
        ArrayList<String> retVal = (ArrayList<String>) paths.clone();
        if (retVal.size() > 0 && isPossibleLanguage(retVal.get(0)))
            retVal.remove(0);
        return retVal;
    }

}
