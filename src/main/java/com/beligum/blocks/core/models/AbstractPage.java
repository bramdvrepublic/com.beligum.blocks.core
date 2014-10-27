package com.beligum.blocks.core.models;

import com.beligum.blocks.core.exceptions.ElementException;
import com.beligum.blocks.core.identifiers.ID;
import com.beligum.blocks.core.models.ifaces.StorableElement;
import com.beligum.blocks.core.models.storables.Block;
import com.beligum.blocks.core.models.storables.Row;

import java.util.HashSet;
import java.util.Set;

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
    /**map containing the final elements of this page, it is a hashset explicitly because it is used for hashing elements when checking for equality*/
    private HashSet<StorableElement> cachedFinalElements = new HashSet<>();
    /**map containing the non-final elements of this page, it is a hashset explicitly because it is used for hashing elements when checking for equality*/
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
    }

    public Set<Block> getBlocks()
    {
        return blocks;
    }
    public void addBlocks(Set<Block> blocks) throws ElementException
    {
        for(Block block : blocks){
            if(!this.canContain(block)){
                throw new ElementException("Only blocks with a compatible id can be added to an abstract-page. An block's id should start with the abstract-page's id. Received block-id: '" + block.getId() + "', but abstract-page-id is: '" + this.getId() + "'");
            }
            else{
                this.addBlock(block);
            }
        }
    }
    public Set<Row> getRows()
    {
        return rows;
    }
    public void addRows(Set<Row> rows) throws ElementException
    {
        for(Row row : rows){
            if(!canContain(row)){
                throw new ElementException("Only rows with a compatible id can be added to an abstract-page. An row's id should start with the abstract-page's id. Received row-id: '" + row.getId() + "', but abstract-page-id is: '" + this.getId() + "'");
            }
            else{
                this.addRow(row);
            }
        }
    }

    public void addBlock(Block block) throws ElementException
    {
        if(this.canContain(block)) {
            this.blocks.add(block);
            this.clearCache();
        }
        else{
            throw new ElementException("Only blocks with a compatible id can be added to an abstract-page. An block's id should start with the abstract-page's id. Received block-id: '" + block.getId() + "', but abstract-page-id is: '" + this.getId() + "'");
        }
    }
    public void addRow(Row row) throws ElementException
    {
        if(this.canContain(row)) {
            this.rows.add(row);
            this.clearCache();
        }
        else{
            throw new ElementException("Only rows with a compatible id can be added to an abstract-page. An row's id should start with the abstract-page's id. Received row-id: '" + row.getId() + "', but abstract-page-id is: '" + this.getId() + "'");
        }
    }
    /**
     * Add an element (row or block) to this abstract-page
     * @param element element to be added
     */
    public void addElement(StorableElement element) throws ElementException
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
     * @return a hashset with all elements in this page that cannot be altered by the client, it is a hashset explicitly because it is typically used with hashing to check for equality
     */
    public HashSet<StorableElement> getFinalElements(){
        if(this.cachedFinalElements.isEmpty()) {
            Set<StorableElement> elements = this.getElements();
            for (StorableElement element : elements) {
                if(element.isFinal()) {
                    this.cachedFinalElements.add(element);
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

    /**
     * Checks whether an element is compatible with this abstract-page, whether or not it can ben contained by this abstract-page
     * @param element
     * @return true if the id of the element starts with the id of this abstract-page (and can thus be seen as a part of the element-tree represented by this abstract-page), false otherwise
     */
    private boolean canContain(StorableElement element){
        return element.getId().toString().startsWith(this.getId().toString());
    }
}
