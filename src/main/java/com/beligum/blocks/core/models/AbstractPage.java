package com.beligum.blocks.core.models;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by bas on 08.10.14.
 * Super-class for PageClass (a page-class) and Page (a page-instance), which have quite a lot in common
 */
public abstract class AbstractPage extends IdentifiableObject
{
    //set with all blocks of this abstract-page
    protected Set<Block> blocks;
    //set with all rows of this abstract-page
    protected Set<Row> rows;

    protected AbstractPage(String uid){
        super(uid);
        this.blocks = new HashSet<>();
        this.rows = new HashSet<>();
    }

    public Set<Block> getBlocks()
    {
        return blocks;
    }
    public void setBlocks(Set<Block> blocks)
    {
        this.blocks = blocks;
    }
    public Set<Row> getRows()
    {
        return rows;
    }
    public void setRows(Set<Row> rows)
    {
        this.rows = rows;
    }

    public void addBlock(Block block){
        this.blocks.add(block);
    }
    public void addRow(Row row){
        this.rows.add(row);
    }
    /**
     * Add an element (row or block) to this abstract-page
     * @param element element to be added
     */
    public void addElement(AbstractIdentifiableElement element)
    {
        if(element instanceof Row){
            this.rows.add((Row) element);
        }
        else if(element instanceof Block){
            this.blocks.add((Block) element);
        }
        else{
            throw new RuntimeException("Could not add element to this abstract-page, the element has an unknown AbstractIdentifiableElement-type: " + element.getClass().getName());
        }
    }

    /**
     *
     * @return als rows and blocks in this abstract-page
     */
    public Set<AbstractIdentifiableElement> getElements(){
        Set<AbstractIdentifiableElement> elements = new HashSet<>();
        elements.addAll(this.blocks);
        elements.addAll(this.rows);
        return elements;
    }
}
