package com.beligum.blocks.core.urlmapping;

import com.beligum.blocks.core.base.Blocks;
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

    @JsonIgnore
    private UrlBranch parent = null;

    protected UrlBranch() {
        this("");
    }

    protected UrlBranch(String name) {
        this(name, Blocks.config().getDefaultLanguage());
    }

    protected UrlBranch(String name, String language) {
        this(name, language, null);
    }

    protected UrlBranch(String name, String language, String storedTemplateId) {
        //
        this.translations.put(Blocks.config().getDefaultLanguage(), name);
        this.storedTemplateId = storedTemplateId;
        this.translations.put(language, name);
    }

    private void fixThisPath() {
        boolean found = false;
        for (String l: Blocks.config().getLanguages()) {
            if (translations.containsKey(l)) {
                translations.put(Blocks.config().getDefaultLanguage(), translations.get(l));
                found = true;
                break;
            }
            // if not yet found fill with random existing value for this branch
            if (!found) {
                if (translations.values().size() > 0) {
                    translations.put(Blocks.config().getDefaultLanguage(), translations.values().iterator().next());
                } else {
                    translations.put(Blocks.config().getDefaultLanguage(), "unknown");
                }
            }
        }
    }

    protected boolean pathFoundForLanguage(String path, String language, SEARCH_OPTION option) {
        // this should never be null so fill it with the closest default value
        if (translations.get(Blocks.config().getDefaultLanguage()) == null) {
            fixThisPath();
        }

        if (option == SEARCH_OPTION.NORMAL) {
          if (translations.containsKey(language) && translations.get(language).equals(path)) {
            return true;
          } else if (translations.containsKey(Blocks.config().getDefaultLanguage()) && translations.get(Blocks.config().getDefaultLanguage()).equals(path)) {
            return true;
          } else {
              return false;
          }
        } else if (option == SEARCH_OPTION.STRICT) {
            if (translations.containsKey(language)) {
                return translations.get(language).equals(path);
            } else if (translations.containsKey(Blocks.config().getDefaultLanguage())) {
                return translations.get(Blocks.config().getDefaultLanguage()).equals(path);
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
        if (language == null) language = Blocks.config().getDefaultLanguage();
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
        return this.translations.get(Blocks.config().getDefaultLanguage());
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
