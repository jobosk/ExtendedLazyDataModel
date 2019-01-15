package es.valencia.ssociales.side.jsf.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.Repository;

import es.valencia.ssociales.side.constant.Edad;

public class ExtendedLazyDataModel<T> extends LazyDataModel<T> {

	private static Logger logger = Logger.getLogger(ExtendedLazyDataModel.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String ID = "id";
	private static final String SEPARATOR = ".";

	private Repository<T, Long> repository;

	public ExtendedLazyDataModel(Repository<T, Long> repository) {
		this.repository = repository;
	}

	@Override
	public Object getRowKey(T t) {
		Object rowKey;
		try {
			Method m = t.getClass().getMethod("getId");
			rowKey = m.invoke(t);
		} catch (Exception e) {
			logger.error("", e);
			rowKey = null;
		}
		return rowKey;
	}

	@Override
	public T getRowData(String rowKey) {
		return ((JpaRepository<T, Long>) repository).findOne(Long.parseLong(rowKey));
	}

	@Override
	public void setRowIndex(int rowIndex) {
		super.setRowIndex(getPageSize() != 0 && rowIndex != -1 ? rowIndex % getPageSize() : -1);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder, final Map<String, Object> filters) {
		this.setPageSize(pageSize);
		Specification<T> specification = new Specification<T>() {
			@Override
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
				List<Predicate> predicates = new ArrayList<Predicate>();
				Iterator<String> filter = filters.keySet().iterator();
				while (filter.hasNext()) {
					try {
						String filterProperty = filter.next();
						Predicate predicate = buildPredicate(root, query, builder, filterProperty, filters.get(filterProperty));
						if (predicate != null) {
							predicates.add(predicate);
						}
					} catch (Exception e) {
						logger.error("", e);
					}
				}
				return builder.and(predicates.toArray(new Predicate[predicates.size()]));
			}
		};
		Pageable pageable;
		if (sortOrder == SortOrder.UNSORTED || StringUtils.isBlank(sortField)) {
			pageable = new PageRequest(first / pageSize, pageSize);
		} else {
			Sort sort = new Sort(sortOrder == SortOrder.ASCENDING ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
			pageable = new PageRequest(first / pageSize, pageSize, sort);
		}
		Page<T> page = ((JpaSpecificationExecutor<T>) repository).findAll(specification, pageable);
		this.setRowCount((int) page.getTotalElements());
		return page.getContent();
	}

	private Predicate buildPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder, String filterProperty, Object filterValue) {
		if (filterValue == null) {
			return null;
		}
		Path<?> path = root;
		Type<?> pathType = root.getModel();
		SingularAttribute<?, ?> attribute = null;
		String property = filterProperty;
		int index;
		while (pathType instanceof ManagedType && property != null) {
			index = property.indexOf(SEPARATOR);
			attribute = ((ManagedType<?>) pathType).getSingularAttribute(index != -1 ? property.substring(0, index++) : property);
			property = index != -1 ? property.substring(index) : null;
			path = getPath(path, attribute);
			pathType = attribute.getType();
		}
		if (pathType instanceof ManagedType && property == null) {
			attribute = ((ManagedType<?>) pathType).getDeclaredSingularAttribute(ID);
			path = getPath(path, attribute);
		}
		boolean isId = attribute != null ? attribute.isId() : false;
		if ("_NULL_".equals(filterValue)) {
			return builder.isNull(path);
		} else if (filterValue instanceof Enum) {
			return buildEnumPredicate(path, builder, filterValue);
		} else if (isId) {
			return builder.equal(path, filterValue);
		} else {
			return builder.like(getValue(path, builder), "%" + String.valueOf(filterValue).toUpperCase() + "%");
		}
	}

	private Expression<String> getValue(Path<?> path, CriteriaBuilder builder) {
		return Date.class.equals(path.getJavaType()) ? formatDate(path, builder) : builder.upper(path.as(String.class));
	}

	private Expression<String> formatDate(Path<?> path, CriteriaBuilder builder) {
		return builder.function("TO_CHAR", String.class, path, builder.literal("DD/MM/YYYY"));
	}

	private Predicate buildEnumPredicate(Path<?> path, CriteriaBuilder builder, Object filterValue) {
		if (filterValue instanceof Edad) {
			Edad edad = (Edad) filterValue;
			if (edad.getEdadMin() == null) {
				return null;
			}
			List<Predicate> predicates = new ArrayList<Predicate>();
			predicates.add(builder.ge(path.as(Integer.class), edad.getEdadMin()));
			if (edad.getEdadMax() != null) {
				predicates.add(builder.lt(path.as(Integer.class), edad.getEdadMax()));
			}
			return builder.and(predicates.toArray(new Predicate[predicates.size()]));
		} else {
			return builder.like(path.as(String.class), "%" + ((Enum<?>) filterValue).name() + "%");
		}
	}

	public Path<?> getPath(Path<?> path, SingularAttribute<?, ?> attribute) {
		return getPathHelper(path, attribute);
	}

	@SuppressWarnings("unchecked")
	private <X, Y> Path<Y> getPathHelper(Path<X> path, SingularAttribute<?, ?> attribute) {
		return path.get((SingularAttribute<? super X, Y>) attribute);
	}
}