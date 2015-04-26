package com.beligum.blocks.models;

import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by wouter on 18/04/15.
 */

@javax.persistence.Entity
@Table(name = "url")
public class SiteUrl
{
    @Id
    Long id;
    String name;
    String verb;
    String BlockId;
    String language;

}
