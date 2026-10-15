package com.gwj.model.repository;

import com.gwj.model.domain.IEntity;
import java.util.List;

public interface IRepository<T extends IEntity> {
    T save(T entity);
    List<T> query(T entity);
    Long update(T entity);
    Long delete(T entity);
}
