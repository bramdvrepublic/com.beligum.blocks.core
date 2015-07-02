package com.beligum.blocks.resources.sql;

import com.beligum.base.models.AbstractSubject;
import com.beligum.base.models.ifaces.BasicModel;
import com.beligum.base.models.ifaces.Subject;
import com.beligum.base.security.Authentication;
import com.beligum.base.security.Principal;
import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Created by wouter on 1/07/15.
 */
@MappedSuperclass
@EntityListeners(DBDocumentInfo.DBDocumentInfoListener.class)
public class DBDocumentInfo implements BasicModel
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @Column(name = "created_at")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime")
    protected LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Type(type = "org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime")
    protected LocalDateTime updatedAt;
    @Column(name = "deleted", nullable = false)
    protected boolean deleted = false;
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = AbstractSubject.class, cascade = { })
    @JoinColumn(name = "created_by", updatable = false, insertable = false)
    private Subject createdBy;
    @Column(name = "created_by")
    private Long createdByRawId;
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = AbstractSubject.class, cascade = { })
    @JoinColumn(name = "updated_by", updatable = false, insertable = false)
    private Subject updatedBy;
    @Column(name = "updated_by")
    private Long updatedByRawId;

    // -----CONSTRUCTOR-----
    protected DBDocumentInfo()
    {
        this.setCreatedAt(LocalDateTime.now());
    }

    // -----PUBLIC GETTERS/SETTERS-----
    @Override
    public Long getId()
    {
        return id;
    }
    public void setId(Long id)
    {
        this.id = id;
    }

    @Override
    @XmlTransient
    public LocalDateTime getCreatedAt()
    {
        return createdAt;
    }
    @Override
    public void setCreatedAt(LocalDateTime created_at)
    {
        this.createdAt = created_at;
    }

    @Override
    @XmlTransient
    public LocalDateTime getUpdatedAt()
    {
        return updatedAt;
    }
    @Override
    public void setUpdatedAt(LocalDateTime updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    @Override
    @XmlTransient
    public Subject getCreatedBy()
    {
        return createdBy;
    }
    @Override
    public void setCreatedBy(Subject createdBy)
    {
        this.createdBy = createdBy;
        this.createdByRawId = createdBy == null ? null : createdBy.getId();
    }

    @Override
    @XmlTransient
    public Subject getUpdatedBy()
    {
        return updatedBy;
    }
    @Override
    public void setUpdatedBy(Subject updatedBy)
    {
        this.updatedBy = updatedBy;
        this.updatedByRawId = updatedBy == null ? null : updatedBy.getId();
    }

    @Override
    @XmlTransient
    public boolean isDeleted()
    {
        return deleted;
    }
    @Override
    public void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }

    @Override
    @Transient
    public boolean getIsNew()
    {
        return this.getId() == null || this.getId() <= 0;
    }



    //-----PRIVATE CLASSES-----
    public static class DBDocumentInfoListener
    {
        public DBDocumentInfoListener()
        {
        }

        /*
    * WATCH OUT HERE: this may trigger a query inside the domain object, leading up to
    * numerous unexpected behaviors.
    * On top of it, it's called during a management function, so situations like endless loops
    * are to be expected if this isn't implemented with care. See the comments in
    * Authentication2.getCurrentUser() for more details.
    *
    * (from http://stackoverflow.com/questions/5267408/how-to-use-entitymanager-from-prepersist)
    *
    * The JPA 2 specification (JSR 317) states the following:
    * "In general, the lifecycle method of a portable application should not invoke EntityManager or Query operations,
    * access other entity instances, or modify relationships within the same persistence context."
    *
    * As far as implementations go, Hibernate forbids it explicitly:
    * "A callback method must not invoke EntityManager or Query methods!"
    *
    */
        @PrePersist
        public void doPrePersist(DBDocumentInfo model)
        {
            if (model.getCreatedAt() == null) {
                model.setCreatedAt(LocalDateTime.now());
            }
            model.setUpdatedAt(LocalDateTime.now());

            Principal currentPrincipal = Authentication.getCurrentPrincipal();
            if (currentPrincipal != null) {
                model.createdByRawId = currentPrincipal.getId();
                model.updatedByRawId = currentPrincipal.getId();
            }
        }
        @PreUpdate
        public void doPreUpdate(DBDocumentInfo model)
        {
            model.setUpdatedAt(LocalDateTime.now());
            Principal currentPrincipal = Authentication.getCurrentPrincipal();
            if (currentPrincipal != null) {
                model.updatedByRawId = currentPrincipal.getId();
            }
        }
    }

}
