// ============================================================================
//
// Copyright (C) 2006-2019 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.dbmap.language.mssql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.process.IConnection;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.utils.TalendQuoteUtils;
import org.talend.designer.dbmap.DbMapComponent;
import org.talend.designer.dbmap.external.data.ExternalDbMapData;
import org.talend.designer.dbmap.external.data.ExternalDbMapEntry;
import org.talend.designer.dbmap.external.data.ExternalDbMapTable;
import org.talend.designer.dbmap.language.AbstractDbLanguage;
import org.talend.designer.dbmap.language.IJoinType;
import org.talend.designer.dbmap.language.generation.DbGenerationManager;
import org.talend.designer.dbmap.language.generation.DbMapSqlConstants;

/**
 * created by hcyi on May 13, 2019
 * Detailled comment
 *
 */
public class MSSqlGenerationManager extends DbGenerationManager {

    private INode source;

    public MSSqlGenerationManager() {
        super(new MSSqlLanguage());
    }

    @Override
    public String buildSqlSelect(DbMapComponent component, String outputTableName) {
        return super.buildSqlSelect(component, outputTableName);
    }

    private boolean isCheckUpdateUsingStatement(DbMapComponent dbMapComponent, String outputTableName) {
        List<IConnection> outputConnections = (List<IConnection>) dbMapComponent.getOutgoingConnections();
        if (outputConnections != null) {
            IConnection iconn = this.getConnectonByMetadataName(outputConnections, outputTableName);
            if (iconn != null) {
                source = iconn.getTarget();
                if (source != null) {
                    IElementParameter updateUsingParam = source.getElementParameter("USE_UPDATE_USING_STATEMENT"); //$NON-NLS-1$
                    if (updateUsingParam != null && updateUsingParam.isShow(source.getElementParameters())
                            && updateUsingParam.getValue() != null) {
                        return Boolean.parseBoolean(updateUsingParam.getValue().toString());
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String buildSqlSelect(DbMapComponent dbMapComponent, String outputTableName, String tabString) {
        boolean isCheckUpdateUsingStatement = isCheckUpdateUsingStatement(dbMapComponent, outputTableName);
        if (!isCheckUpdateUsingStatement) {
            return super.buildSqlSelect(dbMapComponent, outputTableName, tabString);
        } else {
            queryColumnsName = "\""; //$NON-NLS-1$
            aliasAlreadyDeclared.clear();
            queryColumnsSegments.clear();
            querySegments.clear();
            subQueryTable.clear();

            this.tabSpaceString = tabString;
            DbMapComponent component = getDbMapComponent(dbMapComponent);

            List<IConnection> outputConnections = (List<IConnection>) component.getOutgoingConnections();

            Map<String, IConnection> nameToOutputConnection = new HashMap<String, IConnection>();
            for (IConnection connection : outputConnections) {
                nameToOutputConnection.put(connection.getUniqueName(), connection);
            }

            ExternalDbMapData data = component.getExternalData();
            StringBuilder sb = new StringBuilder();

            List<ExternalDbMapTable> outputTables = data.getOutputTables();
            int lstOutputTablesSize = outputTables.size();
            ExternalDbMapTable outputTable = null;
            for (int i = 0; i < lstOutputTablesSize; i++) {
                ExternalDbMapTable temp = outputTables.get(i);
                if (outputTableName.equals(temp.getName())) {
                    outputTable = temp;
                    break;
                }
            }

            if (outputTable != null) {
                IConnection connection = nameToOutputConnection.get(outputTable.getName());
                List<IMetadataColumn> columns = new ArrayList<IMetadataColumn>();
                if (connection != null) {
                    IMetadataTable metadataTable = connection.getMetadataTable();
                    if (metadataTable != null) {
                        columns.addAll(metadataTable.getListColumns());
                    }
                }
                IElementParameter eltSchemaNameParam = source.getElementParameter("ELT_SCHEMA_NAME"); //$NON-NLS-1$
                IElementParameter eltTableNameParam = source.getElementParameter("ELT_TABLE_NAME"); //$NON-NLS-1$

                appendSqlQuery(sb, "\"", false); //$NON-NLS-1$
                appendSqlQuery(sb, DbMapSqlConstants.UPDATE);
                appendSqlQuery(sb, DbMapSqlConstants.SPACE);
                appendSqlQuery(sb, TalendQuoteUtils.removeQuotesIfExist(eltSchemaNameParam.getValue().toString()) + "." //$NON-NLS-1$
                        + TalendQuoteUtils.removeQuotesIfExist(eltTableNameParam.getValue().toString()));
                appendSqlQuery(sb, tabSpaceString);

                appendSqlQuery(sb, DbMapSqlConstants.NEW_LINE);
                appendSqlQuery(sb, tabSpaceString);
                appendSqlQuery(sb, "USING");//$NON-NLS-1$
                List<ExternalDbMapTable> inputTables = data.getInputTables();

                // load input table in hash
                boolean explicitJoin = false;
                int lstSizeInputTables = inputTables.size();
                Map<String, ExternalDbMapTable> nameToInputTable = new HashMap<String, ExternalDbMapTable>();
                for (int i = 0; i < lstSizeInputTables; i++) {
                    ExternalDbMapTable inputTable = inputTables.get(i);
                    nameToInputTable.put(inputTable.getName(), inputTable);
                    IJoinType joinType = language.getJoin(inputTable.getJoinType());
                    if (!language.unuseWithExplicitJoin().contains(joinType) && i > 0) {
                        explicitJoin = true;
                    }
                }
                appendSqlQuery(sb, tabSpaceString);

                for (int i = 0; i < lstSizeInputTables; i++) {
                    ExternalDbMapTable inputTable = inputTables.get(i);
                    IJoinType joinType = null;
                    if (i == 0) {
                        joinType = AbstractDbLanguage.JOIN.NO_JOIN;
                    } else {
                        joinType = language.getJoin(inputTable.getJoinType());
                    }
                    if (language.unuseWithExplicitJoin().contains(joinType) && !explicitJoin) {
                        appendSqlQuery(sb, DbMapSqlConstants.SPACE);
                        appendSqlQuery(sb, inputTable.getTableName());
                        appendSqlQuery(sb, DbMapSqlConstants.SPACE);
                        appendSqlQuery(sb, inputTable.getAlias());
                    }
                }
                appendSqlQuery(sb, DbMapSqlConstants.NEW_LINE);

                List<ExternalDbMapEntry> metadataTableEntries = outputTable.getMetadataTableEntries();
                if (metadataTableEntries != null) {
                    appendSqlQuery(sb, "SET"); //$NON-NLS-1$
                    appendSqlQuery(sb, DbMapSqlConstants.SPACE);
                    int lstSizeOutTableEntries = metadataTableEntries.size();
                    for (int i = 0; i < lstSizeOutTableEntries; i++) {
                        ExternalDbMapEntry dbMapEntry = metadataTableEntries.get(i);
                        String expression = dbMapEntry.getExpression();
                        expression = initExpression(component, dbMapEntry);
                        expression = addQuoteForSpecialChar(expression, component);
                        //
                        if (!DEFAULT_TAB_SPACE_STRING.equals(this.tabSpaceString)) {
                            expression += DbMapSqlConstants.SPACE + DbMapSqlConstants.AS + DbMapSqlConstants.SPACE
                                    + getAliasOf(dbMapEntry.getName());
                        }
                        String exp = replaceVariablesForExpression(component, expression);
                        String columnSegment = exp;
                        if (i > 0) {
                            queryColumnsName += DbMapSqlConstants.COMMA + DbMapSqlConstants.SPACE;
                            columnSegment = DbMapSqlConstants.COMMA + DbMapSqlConstants.SPACE + columnSegment;
                        }
                        if (expression != null && expression.trim().length() > 0) {
                            queryColumnsName += exp;
                            queryColumnsSegments.add(columnSegment);
                        }
                        //
                        boolean isKey = false;
                        for (IMetadataColumn column : columns) {
                            String columnName = column.getLabel();
                            if (columnName.equals(dbMapEntry.getName()) && column.isKey()) {
                                isKey = column.isKey();
                                break;
                            }
                        }
                        if (isKey) {
                            continue;
                        }
                        if (expression != null && expression.trim().length() > 0) {
                            appendSqlQuery(sb, dbMapEntry.getName() + " = " + expression); //$NON-NLS-1$
                            if (i < lstSizeOutTableEntries - 1) {
                                appendSqlQuery(sb, DbMapSqlConstants.COMMA);
                                appendSqlQuery(sb, DbMapSqlConstants.NEW_LINE);
                            }

                        }
                    }
                }

                StringBuilder sbWhere = new StringBuilder();
                this.tabSpaceString = DEFAULT_TAB_SPACE_STRING;
                boolean isFirstClause = true;
                for (int i = 0; i < lstSizeInputTables; i++) {
                    ExternalDbMapTable inputTable = inputTables.get(i);
                    if (buildConditions(component, sbWhere, inputTable, false, isFirstClause, false)) {
                        isFirstClause = false;
                    }
                }
                /*
                 * for addition conditions
                 */
                // like as input.newcolumn1>100
                List<String> whereAddition = new ArrayList<String>();
                // olny pure start with group or order, like as order/group by input.newcolumn1
                // List<String> byAddition = new ArrayList<String>();
                // like as input.newcolumn1>100 group/oder by input.newcolumn1
                // List<String> containWhereAddition = new ArrayList<String>();
                // like as "OR/AND input.newcolumn1", will keep original
                List<String> originalWhereAddition = new ArrayList<String>();
                List<String> otherAddition = new ArrayList<String>();

                if (outputTable != null) {
                    List<ExternalDbMapEntry> customWhereConditionsEntries = outputTable.getCustomWhereConditionsEntries();
                    if (customWhereConditionsEntries != null) {
                        for (ExternalDbMapEntry entry : customWhereConditionsEntries) {
                            String exp = initExpression(component, entry);
                            if (exp != null && !DbMapSqlConstants.EMPTY.equals(exp.trim())) {
                                if (containWith(exp, DbMapSqlConstants.OR, true)
                                        || containWith(exp, DbMapSqlConstants.AND, true)) {
                                    exp = replaceVariablesForExpression(component, exp);
                                    originalWhereAddition.add(exp);
                                } else {
                                    exp = replaceVariablesForExpression(component, exp);
                                    whereAddition.add(exp);
                                }
                            }
                        }
                    }

                    List<ExternalDbMapEntry> customOtherConditionsEntries = outputTable.getCustomOtherConditionsEntries();
                    if (customOtherConditionsEntries != null) {
                        for (ExternalDbMapEntry entry : customOtherConditionsEntries) {
                            String exp = initExpression(component, entry);
                            if (exp != null && !DbMapSqlConstants.EMPTY.equals(exp.trim())) {
                                exp = replaceVariablesForExpression(component, exp);
                                otherAddition.add(exp);
                            }
                        }
                    }
                }
                this.tabSpaceString = tabString;

                String whereClauses = sbWhere.toString();
                boolean whereFlag = whereClauses.trim().length() > 0;
                boolean whereAddFlag = !whereAddition.isEmpty();
                boolean whereOriginalFlag = !originalWhereAddition.isEmpty();
                if (whereFlag || whereAddFlag || whereOriginalFlag) {
                    appendSqlQuery(sb, DbMapSqlConstants.NEW_LINE);
                    appendSqlQuery(sb, tabSpaceString);
                    appendSqlQuery(sb, DbMapSqlConstants.WHERE);
                }
                if (whereFlag) {
                    appendSqlQuery(sb, whereClauses);
                }
                if (whereAddFlag) {
                    for (int i = 0; i < whereAddition.size(); i++) {
                        if (i == 0 && whereFlag || i > 0) {
                            appendSqlQuery(sb, DbMapSqlConstants.NEW_LINE);
                            appendSqlQuery(sb, tabSpaceString);
                            appendSqlQuery(sb, DbMapSqlConstants.SPACE);
                            appendSqlQuery(sb, DbMapSqlConstants.AND);
                        }
                        appendSqlQuery(sb, DbMapSqlConstants.SPACE);
                        appendSqlQuery(sb, whereAddition.get(i));
                    }
                }
                if (whereOriginalFlag) {
                    for (String s : originalWhereAddition) {
                        appendSqlQuery(sb, DbMapSqlConstants.NEW_LINE);
                        appendSqlQuery(sb, DbMapSqlConstants.SPACE);
                        appendSqlQuery(sb, s);
                    }
                }
                if (!otherAddition.isEmpty()) {
                    appendSqlQuery(sb, DbMapSqlConstants.NEW_LINE);
                    appendSqlQuery(sb, tabSpaceString);
                    for (String s : otherAddition) {
                        appendSqlQuery(sb, s);
                        appendSqlQuery(sb, DbMapSqlConstants.NEW_LINE);
                        appendSqlQuery(sb, tabSpaceString);
                    }
                }
            }

            String sqlQuery = sb.toString();
            sqlQuery = handleQuery(sqlQuery);
            queryColumnsName = handleQuery(queryColumnsName);
            return sqlQuery;
        }
    }
}
