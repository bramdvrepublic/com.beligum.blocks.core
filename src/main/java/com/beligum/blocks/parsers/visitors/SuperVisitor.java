package com.beligum.blocks.parsers.visitors;

import com.beligum.blocks.exceptions.ParseException;
import org.jsoup.nodes.Node;

import java.net.URL;
import java.util.Stack;

/**
 * Created by wouter on 23/11/14.
 */
public class SuperVisitor
{
    protected Stack<Node> blueprintTypeStack = new Stack<>();
    protected URL parentUrl = null;
    protected URL entityUrl = null;


    public Node head(Node node, int depth) throws ParseException
    {
//        if (hasBlueprintType(node)) {
//            blueprintTypeStack.push(node);
//        }
        return node;
    }

    public Node tail(Node node, int depth) throws ParseException
    {

        return node;
    }


//    /**
//     *
//     * @return the last typed parent-node visited
//     */
//    protected Node getParent(){
//        if(!this.blueprintTypeStack.empty()){
//            return this.blueprintTypeStack.peek();
//        }
//        else{
//            return null;
//        }
//    }



}
