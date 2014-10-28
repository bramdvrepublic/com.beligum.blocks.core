package com.beligum.blocks.core.models;

import com.beligum.blocks.core.identifiers.ElementID;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.ifaces.Storable;
import com.beligum.blocks.core.models.ifaces.StorableElement;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Row;

import java.util.*;

/**
 * Created by bas on 08.10.14.
 * Super-class for PageClass (a page-class) and Page (a page-instance), which have quite a lot in common
 */
public abstract class AbstractPage extends IdentifiableObject
{
    /**set with all blocks of this abstract-page*/
    protected Set<Block> blocks;
    /**set with all rows of this abstract-page*/
    protected Set<Row> rows;
    /**set with all the element ids of this abstract-page, no double ids can be added*/
    private Set<String> elementIds;
    /**map containing the final elements of this page, keys = html-id's of the elements, values = element-objects*/
    private Map<String, StorableElement> cachedFinalElements = new HashMap<>();
    /**set containing the non-final elements of this page, it is a hashset explicitly because it is used for hashing elements when checking for equality*/
    private HashSet<StorableElement> cachedNonFinalElements = new HashSet<>();

    /**
     *
     * @param id the id of the page
     */
    protected AbstractPage(ID id)
    {
        super(id);
        this.blocks = new HashSet<>();
        this.rows = new HashSet<>();
        this.elementIds = new HashSet<>();
    }

    public Set<Block> getBlocks()
    {
        return blocks;
    }
    public Set<Row> getRows()
    {
        return rows;
    }

    /**
     * Add blocks to this abstract-page if it's element-id (everything after the '#' in the url) is not already present in this abstract-page
     * @param blocks blocks to be added
     */
    public void addBlocks(Set<Block> blocks)
    {
        for(Block block : blocks) {
            this.addBlock(block);
        }
    }
    /**
     * Add rows to this abstract-page if it's element-id (everything after the '#' in the url) is not already present in this abstract-page
     * @param rows rows to be added
     */
    public void addRows(Set<Row> rows)
    {
        for(Row row : rows) {
            this.addRow(row);
        }
    }

    /**
     * Add a block to this abstract-page if it's element-id (everything after the '#' in the url) is not already present in this abstract-page
     * @param block
     */
    public void addBlock(Block block)
    {
        boolean hasUniqueId = this.elementIds.add(block.getHtmlId());
        if(hasUniqueId) {
            this.blocks.add(block);
        }
        this.clearCache();
    }
    /**
     * Add a row to this abstract-page if it's element-id (everything after the '#' in the url) is not already present in this abstract-page
     * @param row
     */
    public void addRow(Row row)
    {
        boolean hasUniqueId = this.elementIds.add(row.getHtmlId());
        if(hasUniqueId){
            this.rows.add(row);
        }
        this.clearCache();
    }
    /**
     * Add an element (row or block) to this abstract-page if it's element-id (everything after the '#' in the url) is not already present in this abstract-page
     * @param element element to be added
     */
    public void addElement(StorableElement element)
    {
        if (element instanceof Row) {
            this.addRow((Row) element);
        }
        else if (element instanceof Block) {
            this.addBlock((Block) element);
        }
        else {
            throw new RuntimeException("Could not add element to this abstract-page, the element has an unknown StorableElement-type: " + element.getClass().getName());
        }
    }
    /**
     * Add a elements (row or block) to this abstract-page if their element-id (everything after the '#' in the url) is not already present in this abstract-page
     * @param elements elements to be added
     */
    public void addElements(Collection<StorableElement> elements){
        for(StorableElement element : elements){
            this.addElement(element);
        }
    }

    /**
     *
     * @return als rows and blocks in this abstract-page
     */
    public Set<StorableElement> getElements(){
        Set<StorableElement> elements = new HashSet<>();
        elements.addAll(this.blocks);
        elements.addAll(this.rows);
        return elements;
    }

    /**
     *
     * @return a map with all elements in this page that cannot be altered by the client, keys = html-id's of the elements, values = element-objects
     */
    public Map<String, StorableElement> getFinalElements(){
        if(this.cachedFinalElements.isEmpty()) {
            Set<StorableElement> elements = this.getElements();
            for (StorableElement element : elements) {
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
    public HashSet<StorableElement> getNonFinalElements(){
        if(this.cachedNonFinalElements.isEmpty()) {
            Set<StorableElement> elements = this.getElements();
            for (StorableElement element : elements) {
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
    }
}
