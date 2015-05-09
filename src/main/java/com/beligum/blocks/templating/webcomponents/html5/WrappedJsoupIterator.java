package com.beligum.blocks.templating.webcomponents.html5;

import java.util.Iterator;

/**
 * Created by bram on 5/8/15.
 */
public abstract class WrappedJsoupIterator<N, T> implements Iterable<T>
{
    //-----CONSTANTS-----

    //-----VARIABLES-----
    private Iterator<N> iter;

    //-----CONSTRUCTORS-----
    public WrappedJsoupIterator(Iterator<N> iter)
    {
        this.iter = iter;
    }

    //-----PUBLIC METHODS-----
    public abstract T wrapNext(N node);

    @Override
    public Iterator iterator()
    {
        return new Iterator<T>()
        {
            @Override
            public boolean hasNext()
            {
                return iter.hasNext();
            }
            @Override
            public T next()
            {
                return wrapNext(iter.next());
            }
            @Override
            public void remove()
            {
                iter.remove();
            }
        };
    }

    //-----PROTECTED METHODS-----

    //-----PRIVATE METHODS-----

}
