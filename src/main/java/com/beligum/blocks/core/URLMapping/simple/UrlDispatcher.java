package com.beligum.blocks.core.URLMapping.simple;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.dbs.BlocksUrlDispatcher;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.models.nosql.BlocksStorable;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URL;
import java.util.*;

/**
 * Created by wouter on 17/03/15.
 */
public abstract class UrlDispatcher extends UrlBranch implements BlocksUrlDispatcher, BlocksStorable
{
    private BlockId id;
    private HashSet<String> possibleLanguages = null;
    private HashMap<String, String>  idToUrl = new HashMap<String, String>();



    public UrlDispatcher() {
        this.resetIdToUrl();
    }

    public BlockId getId()
    {
        return id;
    }
    public void setId(BlockId id)
    {
        this.id = id;
    }

    protected UrlBranch findBranch(ArrayList<String> url, String language, int index, boolean create, SEARCH_OPTION option) {
        UrlBranch retVal = null;
        if (url.size() == 0) {
            retVal = this;
        } else {
            for (UrlBranch branch: subBranches) {
                retVal = branch.findBranch(url, language, index, create, option);
                if (retVal != null) break;
            }
            if (retVal == null && create) {
                UrlBranch branch = new UrlBranch(url.get(index), language);
                this.subBranches.add(branch);
                branch.setParent(this);
                retVal = branch.findBranch(url, language, index, create, option);
            }
        }
        return retVal;
    }

    protected void resetIdToUrl() {
        Stack<UrlBranch> stack = new Stack<>();
        this.idToUrl = collectIdsForUrl(stack);
        if (this.id != null) {
            this.idToUrl.put(this.storedTemplateId, "/");
        }
    }



    public String findId(URL url) {
        String retVal = null;
        ArrayList<String> paths = splitUrl(url);
        String language = getLanguage(paths);
        paths = getUrlWithoutLanguage(paths);
        UrlBranch branch = findBranch(paths, language, 0, false, SEARCH_OPTION.NORMAL);

        if (branch != null) {
            retVal =  branch.getStoredTemplateId();
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
        this.idToUrl.put(stringUrl, id.toString());
        try {
            BlocksConfig.getInstance().getDatabase().saveSiteMap(this);
        } catch (Exception e) {

        }
    }

    public void removeId(URL url, String language, boolean completely)
    {
        String retVal = null;
        ArrayList<String> paths = splitUrl(url);
        paths = getUrlWithoutLanguage(paths);
        UrlBranch branch = findBranch(paths, language, 0, false, SEARCH_OPTION.NORMAL);
        if (branch != null) {
            if (!completely && branch.translations.size() == 1 && branch.translations.containsKey(language)) {
                completely = true;
            }
            if (completely) {
                this.idToUrl.remove(branch.getStoredTemplateId());
                branch.remove();
                String stringUrl = branch.getUrl();
            }
            else {
               if (branch.translations.containsKey(language)) {
                   branch.translations.remove(language);
               }
            }
        }

        try {
            BlocksConfig.getInstance().getDatabase().saveSiteMap(this);
        } catch (Exception e) {

        }
    }


    private ArrayList<String> splitUrl(URL url) {
        String[] paths = url.getPath().split("/");
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(paths));
        if (list.size() > 0 && list.get(0).isEmpty()) {
            list.remove(0);
        }
        return list;
    }

    private boolean isPossibleLanguage(String language) {
        if (possibleLanguages == null) {
            this.possibleLanguages = new HashSet<>();
            this.possibleLanguages.addAll(new ArrayList<String>(Arrays.asList(BlocksConfig.getLanguages())));
        }
        return this.possibleLanguages.contains(language);
    }

    public String getLanguage(URL url) {
        return getLanguage(splitUrl(url));
    }

    private String getLanguage(ArrayList<String> paths) {
        if (paths.size() > 0 && isPossibleLanguage(paths.get(0))) return paths.get(0); else return BlocksConfig.getDefaultLanguage();
    }

    private ArrayList<String> getUrlWithoutLanguage(ArrayList<String> paths) {
        ArrayList<String> retVal = (ArrayList<String>)paths.clone();
        if (retVal.size() > 0 && isPossibleLanguage(retVal.get(0))) retVal.remove(0);
        return retVal;
    }


}
