package com.beligum.blocks.core.models.sql;

import com.beligum.core.framework.models.BasicModel;

import javax.persistence.*;

/**
 * Created by bas on 15.01.15.
 */
@Entity
public class Person extends BasicModel
{
    @OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.ALL })
    private Subject subject;

}
