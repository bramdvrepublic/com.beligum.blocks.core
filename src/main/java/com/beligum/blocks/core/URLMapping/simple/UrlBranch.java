package com.beligum.blocks.core.URLMapping.simple;

import com.beligum.blocks.core.config.BlocksConfig;
import com.beligum.blocks.core.identifiers.BlockId;
import com.beligum.blocks.core.models.nosql.META;
import com.beligum.core.framework.annotations.Meta;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Created by wouter on 17/03/15.
 */
public class UrlBranch
{

    public static enum SEARCH_OPTION {
        PERMISSIVE, NORMAL, STRICT
    }


    protected ArrayList<UrlBranch> subBranches = new ArrayList<UrlBranch>();
    protected String storedTemplateId;
    protected HashSet<UrlBranchHistory> deleted = new HashSet<>();
    protected HashMap<String, String> translations  = new HashMap<>();
    protected META meta;

    @JsonIgnore
    private UrlBranch parent = null;

    protected UrlBranch() {
        this("");
    }

    protected UrlBranch(String name) {
        this(name, BlocksConfig.getDefaultLanguage());
    }

    protected UrlBranch(String name, String language) {
        this(name, language, null);
    }

    protected UrlBranch(String name, String language, String storedTemplateId) {
        //
        this.translations.put(BlocksConfig.getDefaultLanguage(), name);
        this.meta = new META();
        this.meta.touch();
        this.storedTemplateId = storedTemplateId;
        this.translations.put(language, name);
    }

    public META getMeta() {
        return this.meta;
    }

    public void setMeta(META meta) {
        this.meta = meta;
    }

    private void fixThisPath() {
        boolean found = false;
        for (String l: BlocksConfig.getLanguages()) {
            if (translations.containsKey(l)) {
                translations.put(BlocksConfig.getDefaultLanguage(), translations.get(l));
                found = true;
                break;
            }
            // if not yet found fill with random existing value for this branch
            if (!found) {
                if (translations.values().size() > 0) {
                    translations.put(BlocksConfig.getDefaultLanguage(), translations.values().iterator().next());
                } else {
                    translations.put(BlocksConfig.getDefaultLanguage(), "unknown");
                }
            }
        }
    }

    protected boolean pathFoundForLanguage(String path, String language, SEARCH_OPTION option) {
        // this should never be null so fill it with the closest default value
        if (translations.get(BlocksConfig.getDefaultLanguage()) == null) {
            fixThisPath();
        }

        if (option == SEARCH_OPTION.NORMAL) {
          if (translations.containsKey(language) && translations.get(language).equals(path)) {
            return true;
          } else if (translations.containsKey(BlocksConfig.getDefaultLanguage()) && translations.get(BlocksConfig.getDefaultLanguage()).equals(path)) {
            return true;
          } else {
              return false;
          }
        } else if (option == SEARCH_OPTION.STRICT) {
            if (translations.containsKey(language)) {
                return translations.get(language).equals(path);
            } else if (translations.containsKey(BlocksConfig.getDefaultLanguage())) {
                return translations.get(BlocksConfig.getDefaultLanguage()).equals(path);
            } else {
                return false;
            }
        } else if (option == SEARCH_OPTION.PERMISSIVE) {
            return translations.containsValue(path);
        } else {
            return false;
        }
    }


    protected void remove() {
        if (this.storedTemplateId == null) {
            this.keepDeletedInfo(this.storedTemplateId);
            this.storedTemplateId = null;
        }
    }

    protected void add(String storedTemplateId) {
        if (this.storedTemplateId != null) {
            this.remove();
        }
        this.storedTemplateId = storedTemplateId;
    }



    protected UrlBranch findBranch(ArrayList<String> url, String language, int index, boolean create, SEARCH_OPTION option) {
        UrlBranch retVal = null;
        String path = url.get(index);

        boolean found = pathFoundForLanguage(path, language, option);
        if (url.size() - 1 == index) {
            if (found) {
                retVal = this;
            }
        } else if (found) {
            for (UrlBranch branch: subBranches) {
                retVal = branch.findBranch(url, language, index + 1, create, option);
                if (retVal != null) break;
            }
            if (retVal == null && create) {
                UrlBranch branch = new UrlBranch(url.get(index), language);
                this.subBranches.add(branch);
                branch.setParent(this);
                retVal = branch.findBranch(url, language, index + 1, create, option);
            }
        } else if (create) {
                UrlBranch branch = new UrlBranch(url.get(index), language);
                this.subBranches.add(branch);
                branch.setParent(this);
            if (url.size()-1 > index) {
                retVal = branch.findBranch(url, language, index + 1, create, option);
            } else if (url.size()-1 == index) {
                retVal = branch;
            }
        }
        return retVal;
    }

    protected HashMap<String, String> collectIdsForUrl(Stack<UrlBranch> stack) {
        HashMap<String, String> retVal = new HashMap<>();
        stack.push(this);
        if (this.getStoredTemplateId() != null) {
            StringBuilder url = new StringBuilder();
            for (int i=0; i < stack.size(); i++) {
                url.append("/").append(stack.get(i).getPathName());
            }
            retVal.put(this.getStoredTemplateId(), url.toString());
        }

        for (UrlBranch branch: this.subBranches) {
            branch.setParent(this);
            retVal.putAll(branch.collectIdsForUrl(stack));
        }

        stack.pop();
        return retVal;
    }

    protected String getUrl(String language) {
        String retVal = "/";
        if (language == null) language = BlocksConfig.getDefaultLanguage();
        UrlBranch parent = this;
        List<String> paths = new ArrayList<>();
        while (parent != null) {
            paths.add(this.getPathName(language));
            parent = parent.getParent();
        }
        Collections.reverse(paths);
        retVal += StringUtils.join(paths.toArray(), "/");
        return retVal;
    }

    protected String getUrl() {
        return getUrl(null);
    }


    protected String getStoredTemplateId() {
        return this.storedTemplateId;
    }

    protected void setParent(UrlBranch urlBranch) {
        this.parent = urlBranch;
    }

    protected UrlBranch getParent() {
        return this.parent;
    }

    protected String getPathName() {
        return this.translations.get(BlocksConfig.getDefaultLanguage());
    }

    protected String getPathName(String language) {
        String retVal = this.translations.get(language);
        if (retVal == null) retVal = getPathName();
        return retVal;
    }

    private void keepDeletedInfo(String storedTemplateId) {
        this.deleted.add(new UrlBranchHistory(storedTemplateId));
    }

    protected HashSet<UrlBranchHistory> getDeleted() {
        return this.deleted;
    }
}
