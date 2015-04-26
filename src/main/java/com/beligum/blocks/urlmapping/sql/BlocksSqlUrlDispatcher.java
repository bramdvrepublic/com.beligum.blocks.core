package com.beligum.blocks.urlmapping.sql;

import com.beligum.base.server.RequestContext;
import com.beligum.blocks.base.Blocks;
import com.beligum.blocks.identifiers.BlockId;
import com.beligum.blocks.urlmapping.BlocksUrlDispatcher;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.EntityManager;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by wouter on 19/04/15.
 */
public class BlocksSqlUrlDispatcher implements BlocksUrlDispatcher
{

    private HashSet<String> possibleLanguages = null;

    @Override
    public BlockId findId(URL url)
    {
        if (url == null)
            return null;
        String cleanUrl = "/" + StringUtils.join(getUrlWithoutLanguage(splitUrl(url)), "/");
        EntityManager em = RequestContext.getEntityManager();
        //        em.find()
        return null;
    }
    @Override
    public BlockId findPreviousId(URL url)
    {
        return null;
    }
    @Override
    public void addId(URL url, BlockId id, String language)
    {

    }
    @Override
    public void removeId(URL url) throws Exception
    {

    }

    @Override
    public String getUrlForId(String id)
    {
        return null;
    }

    @Override
    public String getLanguage(URL url)
    {
        return getLanguage(splitUrl(url));
    }

    @Override
    public String getLanguageOrNull(URL url)
    {
        return getLanguage(splitUrl(url));
    }

    private boolean isPossibleLanguage(String language)
    {
        if (possibleLanguages == null) {
            this.possibleLanguages = new HashSet<>();
            this.possibleLanguages.addAll(Blocks.config().getLanguages());
        }
        return this.possibleLanguages.contains(language);
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
