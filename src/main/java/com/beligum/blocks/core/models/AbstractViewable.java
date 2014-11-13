package com.beligum.blocks.core.models;

import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.storables.Row;

import java.util.*;

/**
 * Created by bas on 05.11.14.
 */
public class AbstractViewable extends IdentifiableObject
{
    /**string representing the html-template of this element, once the template has been set, it cannot be changed*/
    protected final String template;
    /**set with all the element ids of all the child-elements (and grandchild-elements) of this element, no double ids can be added*/
    protected Set<String> childHtmlIds = new HashSet<>();
    /**set containing all children (and grand-children) of this element*/
    private Set<Row> allChildren = new HashSet<>();
    /**map containing the final elements of this page, keys = html-id's of the elements, values = element-objects*/
    private Map<String, Row> cachedFinalChildren = new HashMap<>();
    /**set containing the non-final elements of this page, it is a hashset explicitly because it is used for hashing elements when checking for equality*/
    private HashSet<Row> cachedNonFinalChildren = new HashSet<>();

    /**
     * Constructor taking a unique id.
     * @param id id for this viewable
     * @param template the template-string which represents the content of this viewable
     * @param allChildren a set of all the children (and grand-children) of this abstractViewable
     */
    public AbstractViewable(ID id, String template, Set<Row> allChildren)
    {
        super(id);
        this.template = template;
        this.allChildren.addAll(allChildren);
    }

    /**
     *
     * @return the template of this viewable
     */
    public String getTemplate()
    {
        return template;
    }

    /**
     *
     * @return all the children (and grand-children) of this element in the element-tree
     */
    public Set<Row> getAllChildren(){
        return this.allChildren;
    }

    /**
     * Add an element to this tree-element if it's element-id (everything after the '#' in the url) is not already present in this abstract-page
     * @param child the element to be added to the viewable
     * @return true if the child was correctly added, false otherwise
     */
    public boolean addChild(Row child)
    {
        boolean added = false;
        boolean hasUniqueId = !this.childHtmlIds.contains(child.getHtmlId());
        if(hasUniqueId) {
            this.childHtmlIds.add(child.getHtmlId());
            added = this.allChildren.add(child);
        }
        this.clearCache();
        return added;
    }

    /**
     * Add children to this viewable
     * @param children children to be added
     * @return true if the collection of children in this viewable has changed, false otherwise
     */
    public boolean addChildren(Collection<Row> children){
        boolean changed = false;
        for(Row child : children){
            if(!changed){
                changed = this.addChild(child);
            }
            else{
                this.addChild(child);
            }
        }
        return changed;
    }

    /**
     *
     * @return a map with all elements in this element that cannot be altered by the client, keys = html-id's of the elements, values = element-objects
     */
    public Map<String, Row> getAllFinalChildren(){
        if(this.cachedFinalChildren.isEmpty()) {
            Set<Row> children = this.getAllChildren();
            for (Row child : children) {
                if(child.isFinal()) {
                    this.cachedFinalChildren.put(child.getHtmlId(), child);
                }
            }
        }
        return this.cachedFinalChildren;
    }

    /**
     *
     * @return a hashset with elements in this page that can be altered by the client, it is a hashset explicitly because it is typically used with hashing to check for equality
     */
    //this MUST return a HASH-set, not simply a set, since hashing will be used to check for equality
    public HashSet<Row> getAllNonFinalChildren(){
        if(this.cachedNonFinalChildren.isEmpty()) {
            Set<Row> children = this.getAllChildren();
            for (Row child : children) {
                if(!child.isFinal()) {
                    this.cachedNonFinalChildren.add(child);
                }
            }
        }
        return this.cachedNonFinalChildren;
    }

    /**
     * clear the cache of this abstract-page
     */
    private void clearCache(){
        if(!this.cachedNonFinalChildren.isEmpty()) {
            this.cachedNonFinalChildren.clear();
        }
        if(!this.cachedFinalChildren.isEmpty()) {
            this.cachedFinalChildren.clear();
        }
    }

}
