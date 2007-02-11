// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.designer.rowgenerator.ui.tabs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.talend.core.CorePlugin;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.prefs.ITalendCorePrefConstants;

/**
 * qzhang class global comment. Detailled comment <br/>
 * 
 * $Id: ShadowProcessPreview.java,v 1.10 2007/01/31 05:20:52 pub Exp $
 * 
 */
public class ShadowProcessPreview {

    protected static final int MAXIMUM_ROWS_TO_PREVIEW = CorePlugin.getDefault().getPreferenceStore().getInt(
            ITalendCorePrefConstants.PREVIEW_LIMIT);

    /**
     * Constante and main var.
     */

    private Table table;

    private Composite composite;

    /**
     * Create Object to manage Preview and MetaData.
     * 
     * @param compositeFileViewer
     * @param filepath (null or path)
     */
    public ShadowProcessPreview(Composite composite, int width, int height) {
        this(composite);
    }

    /**
     * qzhang ShadowProcessPreview constructor comment.
     */
    public ShadowProcessPreview(Composite composite) {
        this.composite = composite;
    }

    /**
     * Create Table to show the content of run returns.
     * 
     * @return
     * 
     */
    public void newTablePreview() {
        table = new Table(composite, SWT.BORDER);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        table.setLayoutData(gridData);
        // TableViewer viewer = new TableViewer(table);
        // viewer.refresh();
    }

    /**
     * refresh Header of table Preview.
     * 
     * @param columns
     */
    private void refreshPreviewHeader(final String[] columns) {
        int existingColumnsLength = table.getColumnCount();
        // update the existing columns
        for (int i = 0; i < existingColumnsLength; i++) {
            if (i < columns.length) {
                // the column exist and must be updated
                table.getColumn(i).setText(columns[i]);
            } else {
                // the following column exist and must be dispose
                i = existingColumnsLength;
            }
        }

        // if necessary, add the another columns must be created
        for (int i = existingColumnsLength; i < columns.length; i++) {
            if (i == 0) {
                new TableColumn(table, SWT.LEFT);

            } else {
                TableColumn column = new TableColumn(table, SWT.LEFT);
                column.setText(columns[i]);
            }
        }

        // if necessary, dispose the unusable columns
        while (table.getColumnCount() > columns.length) {
            table.getColumn(table.getColumnCount() - 1).dispose();
        }
    }

    /**
     * ocarbone Comment method "clearTablePreview".
     */
    private void clearTablePreview() {
        table.clearAll();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumn(i).setText(""); //$NON-NLS-1$
            table.getColumn(i).setWidth(0);
        }
    }

    private void refreshTablePreview(final String[] columns, List<String[]> items) {
        refreshPreviewHeader(columns);
        // refreshPreviewItem(items);
        refreshPreviewItem(columns, items);
        // resize all the columns but not the table
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumn(i).pack();
        }
        // scroll to show the first col and first row
        if (table.getItems().length > 0) {
            table.showItem(table.getItem(0));
            if (table.getColumns() != null && table.getColumns().length > 0) {
                table.showColumn(table.getColumn(0));
            }
        }

    }

    private void refreshPreviewItem(String[] columns, List<String[]> olditems) {
        if (isChanged) {
            List<String[]> newitems = new ArrayList<String[]>();
            List<List<String>> newListItem = new ArrayList<List<String>>();
            for (String[] strings : olditems) {
                List<String> line = new ArrayList<String>();
                line.add(strings[0]);
                newListItem.add(line);
            }

            int countCol = columns.length;
            for (int i = 1; i < countCol; i++) {
                List<String> c = columnData.get(columns[i]);
                for (int j = 0; j < c.size(); j++) {
                    newListItem.get(j).add(c.get(j));
                }
            }
            for (List<String> strs : newListItem) {
                String[] its = new String[strs.size()];
                for (int i = 0; i < its.length; i++) {
                    its[i] = strs.get(i);
                }
                newitems.add(its);
            }
            olditems = newitems;
        }
        refreshPreviewItem(olditems);
    }

    private boolean isChanged = false;

    /**
     * qzhang Comment method "refreshPreviewItem".
     * 
     * @param xmlRows
     */
    private void refreshPreviewItem(List<String[]> xmlRows) {

        int existingItemCount = table.getItemCount();
        int end = xmlRows.size();
        for (int f = 0; f < end; f++) {
            if (f >= existingItemCount) {
                // create a new Item
                TableItem row = new TableItem(table, SWT.NONE);
                row.setText(xmlRows.get(f));
            } else {
                // update an existing Item
                table.getItem(f).setText(xmlRows.get(f));
            }
        }

    }

    private static List<String> columnNames = new ArrayList<String>();

    private static Map<String, List<String>> columnData = new HashMap<String, List<String>>();

    /**
     * qzhang Comment method "refreshTablePreview".
     * 
     * @param columns
     * @param items
     * @param isRefreshItems
     */
    public void refreshTablePreview(List<IMetadataColumn> columns, List<List<String>> items, boolean isRefreshItems) {
        if (isRefreshItems) {
            clearTablePreview();
        }
        if (items == null) {
            initColumnNames(columns);
            return;
        }
        String[] cols = new String[columns.size() + 1];
        for (int i = 0; i < cols.length; i++) {

            if (i == 0) {
                cols[i] = ""; //$NON-NLS-1$
            } else {
                columnData.get(columns.get(i - 1).getLabel()).clear();
                cols[i] = columns.get(i - 1).getLabel();
            }
        }
        List<String[]> newItems = new ArrayList<String[]>();
        if (items != null && isRefreshItems) {
            for (List<String> strs : items) {
                String[] its = new String[strs.size()];

                for (int i = 0; i < its.length; i++) {
                    its[i] = strs.get(i);
                    if (i < its.length - 1) {
                        columnData.get(columnNames.get(i)).add(strs.get(i + 1));
                    }
                }
                newItems.add(its);
            }
        }

        refreshTablePreview(cols, newItems);
    }

    /**
     * qzhang Comment method "initColumnNames".
     * 
     * @param columns
     */
    private void initColumnNames(List<IMetadataColumn> columns) {
        String removeCol = "";
        boolean isEnd = false;
        for (IMetadataColumn col : columns) {
            if (!columnNames.contains(col.getLabel())) {
                columnNames.add(col.getLabel());
                isEnd = true;
            }
        }
        for (int i = 0; i < columnNames.size() && !isEnd; i++) {
            boolean is = false;
            for (IMetadataColumn column : columns) {
                if (column.getLabel().equals(columnNames.get(i))) {
                    is = true;
                    break;
                }
            }
            if (!is) {
                removeCol = columnNames.get(i);
            }
        }
        if (!"".equals(removeCol)) {
            columnNames.remove(removeCol);
        }
        columnData.clear();
        for (String name : columnNames) {
            columnData.put(name, new ArrayList<String>());
        }
    }

    public boolean isChanged() {
        return this.isChanged;
    }

    public void setChanged(boolean isChanged) {
        this.isChanged = isChanged;
    }

    public Table getTable() {
        return this.table;
    }

    public void renameColumn(IMetadataColumn bean, String value) {
        columnNames.set(columnNames.indexOf(bean.getLabel()), value);
        columnData.clear();
        for (String name : columnNames) {
            columnData.put(name, new ArrayList<String>());
        }
        return;
    }

}
