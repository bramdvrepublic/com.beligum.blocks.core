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
    /**the direct children of this tree-element*/
    protected Set<Row> children;
    /**set with all the element ids of all the child-elements (and grandchild-elements) of this element, no double ids can be added*/
    protected Set<String> elementHtmlIds = new HashSet<>();
    /**set containing all children (and grand-children) of this element*/
    private Set<Row> cachedElements = new HashSet<>();
    /**map containing the final elements of this page, keys = html-id's of the elements, values = element-objects*/
    private Map<String, Row> cachedFinalElements = new HashMap<>();
    /**set containing the non-final elements of this page, it is a hashset explicitly because it is used for hashing elements when checking for equality*/
    private HashSet<Row> cachedNonFinalElements = new HashSet<>();

    /**
     * Constructor taking a unique id.
     * @param id id for this viewable
     */
    public AbstractViewable(ID id, String template)
    {
        super(id);
        this.template = template;
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
     * @return the direct children (also elements) of this element
     */
    public Set<Row> getDirectChildren(){
        return this.children;
    }

    /**
     *
     * @return all the children (and grand-children) of this element in the element-tree
     */
    public Set<Row> getAllElements(){
        if(this.cachedElements.isEmpty()) {
            Set<Row> elements = new HashSet<>();
            if (!this.children.isEmpty()) {
                Set<Row> directChildren = this.children;
                for (Row child : directChildren) {
                    elements.addAll(child.getAllElements());
                }
            }
            this.cachedElements = elements;
        }
        return this.cachedElements;
    }

    /**
     * Add an element to this tree-element if it's element-id (everything after the '#' in the url) is not already present in this abstract-page, nor is any of it's children's element-ids.
     * @param element the element to be added to the page
     * @return true if the child was correctly added, false otherwise
     */
    public boolean addChild(Row element)
    {
        boolean added = false;
        boolean hasUniqueId = this.elementHtmlIds.add(element.getHtmlId());
        if(hasUniqueId) {
            added = this.children.add(element);
        }
        this.clearCache();
        return added;
    }

    /**
     * Add direct children to this element if and only if all of them and their grandchildren have a unique (html-)id in the tree with this element as a root and the child-set of this element has actually changed.
     * @param children children to be added
     * @return true if the children have been added, false otherwise
     */
    public boolean addDirectChildren(Collection<Row> children){
        boolean allHaveUniqueIds = true;
        Iterator<Row> childIt = children.iterator();
        while(allHaveUniqueIds && childIt.hasNext()){
            Row child = childIt.next();
            allHaveUniqueIds = !this.elementHtmlIds.contains(child.getHtmlId());
            if(allHaveUniqueIds) {
                //we're not making use of recursion, because we do not , so the tree is not
                Collection<Row> grandchildren = child.getAllElements();
                Iterator<Row> grandChildIt = grandchildren.iterator();
                while(allHaveUniqueIds && grandChildIt.hasNext()){
                    Row grandChild = grandChildIt.next();
                    allHaveUniqueIds = !this.elementHtmlIds.contains(grandChild.getHtmlId());
                }
            }
        }
        if(allHaveUniqueIds){
            boolean changed = false;
            changed = this.children.addAll(children);
            if(changed){
                this.clearCache();
            }
            return changed;
        }
        else{
            return false;
        }
    }

    /**
     *
     * @return a map with all elements in this element that cannot be altered by the client, keys = html-id's of the elements, values = element-objects
     */
    public Map<String, Row> getAllFinalElements(){
        if(this.cachedFinalElements.isEmpty()) {
            Set<Row> elements = this.getAllElements();
            for (Row element : elements) {
                if(element.isFinal()) {
                    this.cachedFinalElements.put(element.getHtmlId(), element);
                }
            }
        }
        return this.cachedFinalElements;
    }

    /**
     *
     * @return a hashset with elements in this page that can be altered by the client, it is a hashset explicitly because it is typically used with hashing to check for equality
     */
    //this MUST return a HASH-set, not simply a set, since hashing will be used to check for equality
    public HashSet<Row> getAllNonFinalElements(){
        if(this.cachedNonFinalElements.isEmpty()) {
            Set<Row> elements = this.getAllElements();
            for (Row element : elements) {
                if(!element.isFinal()) {
                    this.cachedNonFinalElements.add(element);
                }
            }
        }
        return this.cachedNonFinalElements;
    }

    /**
     * clear the cache of this abstract-page
     */
    private void clearCache(){
        if(!this.cachedNonFinalElements.isEmpty()) {
            this.cachedNonFinalElements.clear();
        }
        if(!this.cachedFinalElements.isEmpty()) {
            this.cachedFinalElements.clear();
        }
        if(!this.cachedElements.isEmpty()) {
            this.cachedElements.clear();
        }
    }

}
