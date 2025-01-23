/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.read.service;

import static io.camunda.db.rdbms.read.domain.DbQueryPage.KeySetPaginationFieldEntry.determineOperator;

import io.camunda.db.rdbms.read.domain.DbQueryPage;
import io.camunda.db.rdbms.read.domain.DbQueryPage.KeySetPagination;
import io.camunda.db.rdbms.read.domain.DbQueryPage.KeySetPaginationFieldEntry;
import io.camunda.db.rdbms.read.domain.DbQueryPage.Operator;
import io.camunda.db.rdbms.read.domain.DbQuerySorting;
import io.camunda.db.rdbms.read.domain.DbQuerySorting.SortingEntry;
import io.camunda.db.rdbms.sql.columns.SearchColumn;
import io.camunda.search.page.SearchQueryPage;
import io.camunda.search.query.SearchQueryResult;
import io.camunda.search.sort.SortOption;
import io.camunda.search.sort.SortOption.FieldSorting;
import io.camunda.search.sort.SortOrder;
import io.camunda.zeebe.util.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

abstract class AbstractEntityReader<T> {

  private final SearchColumnFinder<T> searchColumnFinder;

  public AbstractEntityReader(final SearchColumnFinder<T> searchColumnFinder) {
    this.searchColumnFinder = searchColumnFinder;
  }

  @SafeVarargs
  public final DbQuerySorting<T> convertSort(
      final SortOption sortOption, final SearchColumn<T>... discriminatorColumns) {
    final var builder = new DbQuerySorting.Builder<T>();
    final var discriminatorColumnList = new ArrayList<>(Arrays.asList(discriminatorColumns));

    for (final FieldSorting fieldSorting : sortOption.getFieldSortings()) {
      final var column = searchColumnFinder.findByProperty(fieldSorting.field());
      if (column == null) {
        throw new IllegalArgumentException("Unknown sortField: " + fieldSorting.field());
      }

      // remove the column from the discriminator list to not sort double
      discriminatorColumnList.remove(column);
      builder.addEntry(column, fieldSorting.order());
    }

    for (final SearchColumn<T> discriminatorColumn : discriminatorColumnList) {
      builder.addEntry(discriminatorColumn, SortOrder.ASC);
    }

    return builder.build();
  }

  public static DbQueryPage convertPaging(
      final DbQuerySorting<?> sort, final SearchQueryPage page) {
    List<KeySetPagination> keySetPagination = new ArrayList<>();
    if (page.searchAfter() != null || page.searchBefore() != null) {
      keySetPagination = createKeySetPagination(sort, page);
    }

    return new DbQueryPage(page.size(), page.from(), keySetPagination);
  }

  /**
   * To create key set pagination for rdbms, supporting mixed order (e.g. <code>ORDER BY name ASC,
   * startDate DESC, key ASC</code>), a simple solution like
   *
   * <pre>WHERE (name, startDate, key) < ("Process A", "2024-10-10T08:00:00Z", 1234)</pre>
   *
   * <br>
   * does not work anymore. <br>
   * <br>
   * Instead, we need to split up the query into multiple comparisons:
   *
   * <pre>
   * WHERE (name > "Process A")
   *    OR (name = "Process A" AND startDate < "2024-10-10T08:00:00Z")
   *    OR (name = "Process A" AND startDate = "2024-10-10T08:00:00Z" AND key > 1234)
   * </pre>
   *
   * This method takes the sortOrder and the sortValues (before or after) and creates a list grouped
   * expressions. We do this in Java and not in MyBatis because it is easier to program and to test
   */
  private static List<KeySetPagination> createKeySetPagination(
      final DbQuerySorting<?> sort, final SearchQueryPage page) {
    final boolean isSearchAfter = page.searchAfter() != null;
    final Object[] sortValues =
        page.searchAfter() != null ? page.searchAfter() : page.searchBefore();
    final List<KeySetPagination> keySetPagination = new ArrayList<>();

    for (int i = 0; i < sort.orderings().size(); i++) {
      final List<KeySetPaginationFieldEntry> fieldEntries = new ArrayList<>();
      // add entries for equals sorting
      for (int j = 0; j < i; j++) {
        final SearchColumn<?> sortColumn = sort.orderings().get(j).column();
        fieldEntries.add(
            new KeySetPaginationFieldEntry(
                sortColumn.name(), Operator.EQUALS, sortColumn.convertSortOption(sortValues[j])));
      }

      // add comparator entry
      final var sorting = sort.orderings().get(i);
      fieldEntries.add(
          new KeySetPaginationFieldEntry(
              sorting.column().name(),
              determineOperator(sorting.order(), isSearchAfter),
              sorting.column().convertSortOption(sortValues[i])));
      keySetPagination.add(new KeySetPagination(fieldEntries));
    }

    return keySetPagination;
  }

  protected final SearchQueryResult<T> buildSearchQueryResult(
      final long totalHits, final List<T> hits, final DbQuerySorting<T> dbSort) {
    return new SearchQueryResult.Builder<T>()
        .total(totalHits)
        .items(hits)
        .firstSortValues(extractFirstSortValues(hits, dbSort))
        .lastSortValues(extractLastSortValues(hits, dbSort))
        .build();
  }

  @VisibleForTesting
  Object[] extractFirstSortValues(final List<T> hits, final DbQuerySorting<T> sort) {
    return extractSortValues(hits, sort, List::getFirst);
  }

  @VisibleForTesting
  Object[] extractLastSortValues(final List<T> hits, final DbQuerySorting<T> sort) {
    return extractSortValues(hits, sort, List::getLast);
  }

  private Object[] extractSortValues(
      final List<T> hits,
      final DbQuerySorting<T> sort,
      final Function<List<T>, T> firstOrLastGetter) {
    if (hits.isEmpty() || sort.orderings().isEmpty()) {
      return new Object[0];
    }
    final T firstOrLast = firstOrLastGetter.apply(hits);
    final List<Object> sortOptions = new ArrayList<>();
    for (final SortingEntry<T> fieldSorting : sort.orderings()) {
      sortOptions.add(fieldSorting.column().getPropertyValue(firstOrLast));
    }

    return sortOptions.toArray();
  }
}
