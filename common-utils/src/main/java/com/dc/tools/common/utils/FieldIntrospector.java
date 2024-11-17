package com.dc.tools.common.utils;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fields 工具类
 *
 * @author zhangyang
 * @date 2020-08-13
 */
@UtilityClass
public class FieldIntrospector {

    /**
     * @param targetType     the target type to search fields on
     * @param metadataLookup A callback interface for metadata lookup on a given field
     * @return the selected fields, or an empty map in case of no match
     */
    public <T> Map<Field, T> selectFields(Class<?> targetType, final MetadataLookup<T> metadataLookup) {

        final Map<Field, T> fieldMap = new LinkedHashMap<>();
        Class<?> targetClass = ClassUtils.getUserClass(targetType);
        ReflectionUtils.doWithFields(targetClass, field -> {

            T result = metadataLookup.inspect(field);
            if (result != null) {
                fieldMap.put(field, result);
            }
        }, ReflectionUtils.COPYABLE_FIELDS);

        return fieldMap;
    }

    /**
     * Select fields on the given target type based on a filter.
     * <p>Callers define fields of interest through the {@code FieldFilter} parameter.
     *
     * @param targetType  the target type to search fields on
     * @param fieldFilter a {@code FiledFilter} to help
     *                    recognize handler fields of interest
     * @return the selected fields, or an empty set in case of no match
     */
    public static Set<Field> selectFields(Class<?> targetType, final ReflectionUtils.FieldFilter fieldFilter) {
        return selectFields(targetType, (MetadataLookup<?>) field -> (fieldFilter.matches(field) ? Boolean.TRUE : null)).keySet();
    }


    /**
     * A callback interface for metadata lookup on a given field.
     *
     * @param <T> the type of metadata returned
     */
    public interface MetadataLookup<T> {

        /**
         * Perform a lookup on the given field and return associated metadata, if any.
         *
         * @param field the field to inspect
         * @return non-null metadata to be associated with a field if there is a match,
         * or {@code null} for no match
         */
        T inspect(Field field);
    }


}
