package com.github.mcollovati.vertxvaadin.fusion.data;

import java.util.List;
import java.util.Optional;

import com.vaadin.fusion.EndpointExposed;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.fusion.Nonnull;
import org.springframework.data.domain.Page;
import org.vaadin.artur.helpers.CrudService;
import org.vaadin.artur.helpers.GridSorter;
import org.vaadin.artur.helpers.PagingUtil;

@AnonymousAllowed
@EndpointExposed
public abstract class CrudEndpoint<T, ID> {

    protected abstract CrudService<T, ID> getService();

    @Nonnull
    public List<@Nonnull T> list(int offset, int limit, List<GridSorter> sortOrder) {
        Page<T> page = getService()
            .list(PagingUtil.offsetLimitTypeScriptSortOrdersToPageable(offset, limit, sortOrder));
        return page.getContent();
    }

    public Optional<T> get(@Nonnull ID id) {
        return getService().get(id);
    }

    @Nonnull
    public T update(@Nonnull T entity) {
        return getService().update(entity);
    }

    public void delete(@Nonnull ID id) {
        getService().delete(id);
    }

    public int count() {
        return getService().count();
    }

}