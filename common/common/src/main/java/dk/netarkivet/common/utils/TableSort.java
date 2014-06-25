/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.common.utils;

import dk.netarkivet.common.exceptions.ArgumentNotValid;


/** contains the data about how a table is sorted.*/
public class TableSort {
    /**list of the sort order.*/
    public enum SortOrder { NONE, INCR, DESC };
    /** id of the sorted column.*/
    private int columnIdent = -1;
    /** order of the sort.*/
    private SortOrder order = TableSort.SortOrder.NONE;

    /**constructor.
     * @param columnId the id of the sorted column
     * @param sortOrder the order of the sort
     * */
    public TableSort(final int columnId, final SortOrder sortOrder) {
        ArgumentNotValid.checkTrue(
                sortOrder == TableSort.SortOrder.DESC
                || sortOrder == TableSort.SortOrder.INCR
                || sortOrder == TableSort.SortOrder.NONE
                , "set order invalid");

        columnIdent = columnId;
        order = sortOrder;
    }

    /** return the id of the sorted column.
     * @return the id of the sorted column
     * */
    public final int getColumnIdent() {
        return columnIdent;
    }

    /** set the id of the sorted column.
     * @param columnident the id of the sorted column
     * */
    public final void setColumnIdent(final int columnident) {
        columnIdent = columnident;
    }
    /** return the order of the sort.
     * @return the order of the sort
     * */
    public final SortOrder getOrder() {
        return order;
    }

    /** set the order of the sort.
     * @param sortorder the order of the sort
     * */
    public final void setOrder(final SortOrder sortorder) {
        ArgumentNotValid.checkTrue(
                sortorder == TableSort.SortOrder.DESC
                || sortorder == TableSort.SortOrder.INCR
                || sortorder == TableSort.SortOrder.NONE
                , "set order invalid");
        order = sortorder;
    }
}
