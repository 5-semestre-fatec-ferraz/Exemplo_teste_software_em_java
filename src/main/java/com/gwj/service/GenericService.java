package com.gwj.service;

import com.gwj.model.domain.IEntity;
import com.gwj.model.repository.IRepository;
import com.gwj.model.repository.GenericRepository;
import com.gwj.service.transaction.UnitOfWork;
import java.util.List;

public class GenericService<T extends IEntity> implements IService<T> {
    protected final IRepository<T> repository;

    @SuppressWarnings("unchecked")
    public GenericService(Class<? extends IEntity> entityClass) {
        this.repository = new GenericRepository<>((Class<T>) entityClass);
    }

    public GenericService(IRepository<T> repository) {
        this.repository = repository;
    }

    @Override
    public T create(T entity) {
        try (UnitOfWork uow = new UnitOfWork()) {
            UnitOfWork.beginTransaction();
            T created = repository.save(entity);
            UnitOfWork.commit();
            return created;
        } catch (Exception e) {
            UnitOfWork.rollback();
            throw new RuntimeException("Erro ao criar registro no serviço: " + e.getMessage(), e);
        }
    }

    @Override
    public List<T> read(T entity) {
        try (UnitOfWork uow = new UnitOfWork()) {
            return repository.query(entity);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar registros no serviço: " + e.getMessage(), e);
        }
    }

    @Override
    public Long update(T entity) {
        try (UnitOfWork uow = new UnitOfWork()) {
            UnitOfWork.beginTransaction();
            Long id = repository.update(entity);
            UnitOfWork.commit();
            return id;
        } catch (Exception e) {
            UnitOfWork.rollback();
            throw new RuntimeException("Erro ao atualizar registro no serviço: " + e.getMessage(), e);
        }
    }

    @Override
    public Long delete(T entity) {
        try (UnitOfWork uow = new UnitOfWork()) {
            UnitOfWork.beginTransaction();
            Long id = repository.delete(entity);
            UnitOfWork.commit();
            return id;
        } catch (Exception e) {
            UnitOfWork.rollback();
            throw new RuntimeException("Erro ao deletar registro no serviço: " + e.getMessage(), e);
        }
    }
}
