package com.gwj.service;

import com.gwj.model.domain.IEntity;
import java.util.List;

public interface IService<T extends IEntity> {
    T create(T entity);
    List<T> read(T entity);
    Long update(T entity);
    Long delete(T entity);
}
