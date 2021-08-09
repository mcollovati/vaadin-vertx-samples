package com.github.mcollovati.vertxvaadin.fusion.data;

import java.util.List;
import java.util.Optional;

import com.vaadin.flow.server.connect.EndpointExposed;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.data.domain.Page;
import org.vaadin.artur.helpers.CrudService;
import org.vaadin.artur.helpers.GridSorter;
import org.vaadin.artur.helpers.PagingUtil;

@AnonymousAllowed
@EndpointExposed
public abstract class CrudEndpoint<T, ID> {

    protected abstract CrudService<T, ID> getService();

    public List<T> list(int offset, int limit, List<GridSorter> sortOrder) {
        Page<T> page = getService()
            .list(PagingUtil.offsetLimitTypeScriptSortOrdersToPageable(offset, limit, sortOrder));
        return page.getContent();
    }

    public Optional<T> get(ID id) {
        return getService().get(id);
    }

    public T update(T entity) {
        return getService().update(entity);
    }

    public void delete(ID id) {
        getService().delete(id);
    }

    public int count() {
        return getService().count();
    }

}