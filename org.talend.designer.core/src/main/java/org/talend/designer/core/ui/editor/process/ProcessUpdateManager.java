// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.editor.process;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.gef.commands.CommandStack;
import org.eclipse.ui.IEditorReference;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.model.components.ComponentUtilities;
import org.talend.core.model.components.IComponent;
import org.talend.core.model.context.ContextUtils;
import org.talend.core.model.context.JobContextManager;
import org.talend.core.model.metadata.IEbcdicConstant;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MetadataTool;
import org.talend.core.model.metadata.QueryUtil;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.Query;
import org.talend.core.model.metadata.builder.connection.SAPConnection;
import org.talend.core.model.metadata.builder.connection.SAPFunctionUnit;
import org.talend.core.model.metadata.builder.connection.impl.XmlFileConnectionImpl;
import org.talend.core.model.metadata.designerproperties.RepositoryToComponentProperty;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IContextManager;
import org.talend.core.model.process.IContextParameter;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.ContextItem;
import org.talend.core.model.properties.DatabaseConnectionItem;
import org.talend.core.model.properties.EbcdicConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.model.update.AbstractUpdateManager;
import org.talend.core.model.update.EUpdateItemType;
import org.talend.core.model.update.EUpdateResult;
import org.talend.core.model.update.RepositoryUpdateManager;
import org.talend.core.model.update.UpdateResult;
import org.talend.core.model.update.UpdatesConstants;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.core.ui.IEBCDICProviderService;
import org.talend.core.utils.SAPConnectionUtils;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.model.process.AbstractProcessProvider;
import org.talend.designer.core.model.utils.emf.talendfile.ContextParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.ContextType;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.update.UpdateCheckResult;
import org.talend.designer.core.ui.editor.update.UpdateManagerUtils;
import org.talend.designer.core.utils.SAPParametersUtils;
import org.talend.repository.UpdateRepositoryUtils;
import org.talend.repository.model.ComponentsFactoryProvider;
import org.talend.repository.ui.utils.ConnectionContextHelper;

/**
 * ggu class global comment. Detailled comment
 */
public class ProcessUpdateManager extends AbstractUpdateManager {

    /**
     * for joblet
     */
    private AbstractProcessProvider jobletProcessProvider = AbstractProcessProvider
            .findProcessProviderFromPID(IComponent.JOBLET_PID);

    private Map<String, Date> jobletReferenceMap = new HashMap<String, Date>();

    private Process process = null;

    public ProcessUpdateManager(org.talend.designer.core.ui.editor.process.Process process) {
        super();
        if (process == null) {
            throw new RuntimeException("The argument is null."); //$NON-NLS-1$
        }
        this.process = process;

    }

    public void retrieveRefInformation() {
        jobletReferenceMap.clear();

        if (jobletProcessProvider == null) {
            return;
        }
        for (INode node : this.getProcess().getGraphicalNodes()) {
            Item jobletItem = jobletProcessProvider.getJobletItem(node);
            if (jobletItem != null) {
                jobletReferenceMap.put(jobletItem.getProperty().getId(), jobletItem.getProperty().getModificationDate());
            }
        }
    }

    public CommandStack getCommandStack() {
        return process.getCommandStack();
    }

    public Process getProcess() {
        return this.process;
    }

    /*
     * check context.
     */
    private List<UpdateResult> checkContext() {
        List<UpdateResult> contextResults = new ArrayList<UpdateResult>();
        final IContextManager contextManager = getProcess().getContextManager();
        // record the unsame
        ContextItemParamMap unsameMap = new ContextItemParamMap();
        // rename
        ContextItemParamMap renamedMap = new ContextItemParamMap();
        // built in
        ContextItemParamMap builtInMap = new ContextItemParamMap();
        Set<String> builtInSet = new HashSet<String>();

        Map<ContextItem, Map<String, String>> repositoryRenamedMap = ((JobContextManager) contextManager)
                .getRepositoryRenamedMap();

        ContextItemParamMap deleteParams = new ContextItemParamMap();

        final List<ContextItem> allContextItem = ContextUtils.getAllContextItem();

        Set<String> refContextNames = new HashSet<String>();

        for (IContext context : contextManager.getListContext()) {
            for (IContextParameter param : context.getContextParameterList()) {
                if (!param.isBuiltIn()) {
                    String source = param.getSource();
                    String paramName = param.getName();
                    refContextNames.add(source);
                    // rename
                    boolean renamed = false;
                    for (ContextItem item : repositoryRenamedMap.keySet()) {
                        if (source.equals(item.getProperty().getLabel())) {
                            String newName = getRenamedVarName(paramName, repositoryRenamedMap.get(item));
                            if (newName != null && !newName.equals(paramName)) {
                                renamedMap.add(item, paramName);
                                renamed = true;
                            }
                        }
                    }
                    if (!renamed) {
                        // update
                        final ContextItem contextItem = ContextUtils.getContextItemByName(allContextItem, source);
                        boolean builtin = true;
                        if (contextItem != null) {
                            final ContextType contextType = ContextUtils.getContextTypeByName(contextItem, context.getName(),
                                    true);
                            if (contextType != null) {
                                final ContextParameterType contextParameterType = ContextUtils.getContextParameterTypeByName(
                                        contextType, paramName);
                                if (contextParameterType != null) {
                                    if (!ContextUtils.samePropertiesForContextParameter(param, contextParameterType)) {
                                        unsameMap.add(contextItem, paramName);
                                    }
                                    builtin = false;
                                } else {
                                    // delete context variable
                                    if (ContextUtils.isPropagateContextVariable()) {
                                        deleteParams.add(contextItem, paramName);
                                        builtin = false;
                                    }
                                }
                            }
                        }
                        if (builtin) {
                            // built in
                            if (contextItem != null) {
                                builtInMap.add(contextItem, paramName);
                            } else {
                                builtInSet.add(paramName);
                            }
                        }
                    }
                }
            }
        }
        // built-in
        if (contextManager instanceof JobContextManager) { // add the lost source for init process
            Set<String> lostParameters = ((JobContextManager) contextManager).getLostParameters();
            if (lostParameters != null && !lostParameters.isEmpty()) {
                builtInSet.addAll(lostParameters);
                lostParameters.clear();
            }
        }
        if (!builtInSet.isEmpty()) {
            UpdateCheckResult result = new UpdateCheckResult(builtInSet);
            result.setResult(EUpdateItemType.CONTEXT, EUpdateResult.BUIL_IN);
            result.setJob(getProcess());
            setConfigrationForReadOnlyJob(result);
            contextResults.add(result);
        }
        if (!builtInMap.isEmpty()) {
            for (ContextItem item : builtInMap.getContexts()) {
                Set<String> names = builtInMap.get(item);
                if (names != null && !names.isEmpty()) {
                    UpdateCheckResult result = new UpdateCheckResult(names);
                    result.setResult(EUpdateItemType.CONTEXT, EUpdateResult.BUIL_IN, null, UpdateRepositoryUtils
                            .getRepositorySourceName(item));
                    result.setJob(getProcess());
                    setConfigrationForReadOnlyJob(result);
                    contextResults.add(result);
                }
            }
        }
        // see 0004661: Add an option to propagate when add or remove a variable in a repository context to
        // jobs/joblets.
        checkPropagateContextVariable(contextResults, contextManager, deleteParams, allContextItem, refContextNames);

        // update
        if (!unsameMap.isEmpty()) {
            for (ContextItem item : unsameMap.getContexts()) {
                Set<String> names = unsameMap.get(item);
                if (names != null && !names.isEmpty()) {
                    collectUpdateResult(contextResults, EUpdateItemType.CONTEXT, EUpdateResult.UPDATE, item, names);
                }
            }
        }
        // rename
        if (!renamedMap.isEmpty()) {
            for (ContextItem item : renamedMap.getContexts()) {
                Map<String, String> nameMap = repositoryRenamedMap.get(item);
                if (nameMap != null && !nameMap.isEmpty()) {
                    for (String newName : nameMap.keySet()) {
                        String oldName = nameMap.get(newName);
                        if (newName.equals(oldName)) {
                            continue;
                        }
                        Set<String> nameSet = new HashSet<String>();
                        nameSet.add(oldName);

                        List<Object> parameterList = new ArrayList<Object>();
                        parameterList.add(item);
                        parameterList.add(oldName);
                        parameterList.add(newName);

                        UpdateCheckResult result = new UpdateCheckResult(nameSet);
                        result.setResult(EUpdateItemType.CONTEXT, EUpdateResult.RENAME, parameterList, UpdateRepositoryUtils
                                .getRepositorySourceName(item));
                        result.setJob(getProcess());
                        if (!isOpenedProcess(getProcess())) {
                            result.setItemProcess(getProcess());
                        }

                        setConfigrationForReadOnlyJob(result);
                        contextResults.add(result);
                    }
                }
            }
        }
        repositoryRenamedMap.clear();
        return contextResults;
    }

    /**
     * propagate when add or remove a variable in a repository context to jobs/joblets.
     * 
     * @param contextResults
     * @param contextManager
     * @param deleteParams
     * @param allContextItem
     * @param refContextNames
     */
    private void checkPropagateContextVariable(List<UpdateResult> contextResults, final IContextManager contextManager,
            ContextItemParamMap deleteParams, final List<ContextItem> allContextItem, Set<String> refContextNames) {
        if (ContextUtils.isPropagateContextVariable()) {
            // check newly added parameter
            Map<ContextItem, Set<String>> newParametersMap = ((JobContextManager) contextManager).getNewParametersMap();
            if (newParametersMap != null) {
                // improve lookup speed
                Map<String, ContextItem> contextItemsMap = new HashMap<String, ContextItem>();
                for (ContextItem contextItem : allContextItem) {
                    contextItemsMap.put(contextItem.getProperty().getLabel(), contextItem);
                }

                for (String name : refContextNames) {
                    ContextItem contextItem = contextItemsMap.get(name);
                    Set<String> names = newParametersMap.get(contextItem);
                    if (names == null || names.isEmpty()) {
                        continue;
                    }
                    collectUpdateResult(contextResults, EUpdateItemType.CONTEXT, EUpdateResult.ADD, contextItem, names);
                }
            }

            // delete
            if (!deleteParams.isEmpty()) {
                for (ContextItem item : deleteParams.getContexts()) {
                    Set<String> names = deleteParams.get(item);
                    if (!names.isEmpty()) {
                        collectUpdateResult(contextResults, EUpdateItemType.CONTEXT, EUpdateResult.DELETE, item, names);
                    }
                }
            }
        }
    }

    private UpdateCheckResult collectUpdateResult(List<UpdateResult> contextResults, EUpdateItemType itemType,
            EUpdateResult resulstType, ContextItem contextItem, Object names) {
        UpdateCheckResult result = new UpdateCheckResult(names);
        result.setResult(itemType, resulstType, contextItem, UpdateRepositoryUtils.getRepositorySourceName(contextItem));
        result.setJob(getProcess());
        setConfigrationForReadOnlyJob(result);
        contextResults.add(result);
        return result;
    }

    private static boolean isOpenedProcess(Process curProcess) {
        IEditorReference[] reference = RepositoryUpdateManager.getEditors();
        List<IProcess> openedProcessList = CorePlugin.getDefault().getDesignerCoreService().getOpenedProcess(reference);
        for (IProcess process : openedProcessList) {
            Property property = curProcess.getProperty();
            if (process.getId().equals(property.getId()) && process.getLabel().equals(property.getLabel())
                    && process.getVersion().equals(property.getVersion())) {
                return true;
            }
        }
        return false;
    }

    private static String getRenamedVarName(final String varName, Map<String, String> renamedMap) {
        if (varName == null || renamedMap == null || renamedMap.isEmpty()) {
            return null;
        }

        Set<String> keySet = renamedMap.keySet();
        for (String newName : keySet) {
            String oldName = renamedMap.get(newName);
            if (varName.equals(oldName)) {
                return newName;
            }
        }
        return null;
    }

    /*
     * check job settings parameters.
     */
    private List<UpdateResult> checkMainParameters(EUpdateItemType type) {
        List<UpdateResult> mainResults = new ArrayList<UpdateResult>();
        switch (type) {
        case JOB_PROPERTY_EXTRA:
            mainResults.addAll(checkJobSettingsParameters(EComponentCategory.EXTRA, type));
            break;
        case JOB_PROPERTY_STATS_LOGS:
            mainResults.addAll(checkJobSettingsParameters(EComponentCategory.STATSANDLOGS, type));
            break;
        default:
            return Collections.emptyList();
        }

        return mainResults;
    }

    private List<UpdateResult> checkJobSettingsParameters(EComponentCategory category, EUpdateItemType type) {
        List<UpdateResult> jobSettingsResults = new ArrayList<UpdateResult>();
        final IElementParameter propertyTypeParam = getProcess().getElementParameterFromField(EParameterFieldType.PROPERTY_TYPE,
                category);

        if (propertyTypeParam != null && propertyTypeParam.isShow(getProcess().getElementParameters())) {
            final Map<String, IElementParameter> childParameters = propertyTypeParam.getChildParameters();
            if (childParameters == null) {
                return Collections.emptyList();
            }
            IElementParameter elementParameter = childParameters.get(EParameterName.PROPERTY_TYPE.getName());
            // is repository
            if (elementParameter != null && EmfComponent.REPOSITORY.equals(elementParameter.getValue())) {
                IElementParameter repositoryParam = childParameters.get(EParameterName.REPOSITORY_PROPERTY_TYPE.getName());
                if (repositoryParam != null) {

                    // get the connection
                    Connection repositoryConnection = null;
                    String source = null;
                    IRepositoryObject lastVersion = UpdateRepositoryUtils.getRepositoryObjectById((String) repositoryParam
                            .getValue());
                    if (lastVersion != null) {
                        final Item item = lastVersion.getProperty().getItem();
                        if (item != null && item instanceof ConnectionItem) {
                            source = UpdateRepositoryUtils.getRepositorySourceName(item);
                            repositoryConnection = ((DatabaseConnectionItem) item).getConnection();
                        }
                    }
                    UpdateCheckResult result = null;

                    if (repositoryConnection != null) {
                        boolean sameValues = true;
                        for (IElementParameter param : getProcess().getElementParameters()) {
                            if (param.getCategory() == category) {
                                String repositoryValue = param.getRepositoryValue();
                                if (param.isShow(getProcess().getElementParameters()) && (repositoryValue != null)
                                        && !param.getName().equals(EParameterName.PROPERTY_TYPE.getName())) {
                                    Object repValue = RepositoryToComponentProperty.getValue(repositoryConnection,
                                            repositoryValue);
                                    if (repValue == null) {
                                        continue;
                                    }
                                    if (repositoryValue.equals(UpdatesConstants.TYPE)) { // datebase type
                                        boolean found = false;
                                        String[] list = param.getListRepositoryItems();
                                        for (int i = 0; (i < list.length) && (!found); i++) {
                                            if (repValue.equals(list[i])) {
                                                found = true;
                                            }
                                        }
                                        if (!found) {
                                            sameValues = false;
                                        }

                                    } else {
                                        // check the value
                                        if (!param.getValue().equals(repValue)) {
                                            sameValues = false;
                                        }
                                    }

                                }

                            }
                        }
                        if (!sameValues) {
                            result = new UpdateCheckResult(getProcess());
                            result.setResult(type, EUpdateResult.UPDATE, repositoryConnection, source);

                        }
                        for (IElementParameter param : getProcess().getElementParameters()) {
                            String repositoryValue = param.getRepositoryValue();
                            if (param.isShow(getProcess().getElementParameters()) && (repositoryValue != null)
                                    && (!param.getName().equals(EParameterName.PROPERTY_TYPE.getName()))
                                    && param.getCategory() == category) {
                                param.setRepositoryValueUsed(true);
                                param.setReadOnly(true);
                            }
                        }
                        // for context mode(bug 5198)
                        List<UpdateResult> contextResults = checkParameterContextMode(getProcess().getElementParameters(),
                                (ConnectionItem) lastVersion.getProperty().getItem(), category);
                        if (contextResults != null) {
                            jobSettingsResults.addAll(contextResults);
                        }
                    } else {
                        result = new UpdateCheckResult(getProcess());
                        result.setResult(type, EUpdateResult.BUIL_IN);
                    }

                    if (result != null) {
                        result.setJob(getProcess());
                        setConfigrationForReadOnlyJob(result);
                        jobSettingsResults.add(result);
                    }
                }
            }
        }
        return jobSettingsResults;
    }

    /*
     * check node parameters.
     */
    @SuppressWarnings("unchecked")
    private List<UpdateResult> checkNodesParameters(EUpdateItemType type) {
        List<UpdateResult> nodesResults = new ArrayList<UpdateResult>();
        for (Node node : (List<Node>) getProcess().getGraphicalNodes()) {
            switch (type) {
            case NODE_SCHEMA:
                nodesResults.addAll(checkNodeSchemaFromRepository(node));
                break;
            case NODE_PROPERTY:
                nodesResults.addAll(checkNodePropertiesFromRepository(node));
                break;
            case NODE_QUERY:
                nodesResults.addAll(checkNodeQueryFromRepository(node));
            case NODE_SAP_FUNCTION:
                nodesResults.addAll(checkNodeSAPFunctionFromRepository(node));
                break;
            default:
                return Collections.emptyList();
            }
        }
        getSchemaRenamedMap().clear();
        return nodesResults;
    }

    /**
     * 
     * DOC YeXiaowei Comment method "checkNodeSAPFunctionFromRepository".
     * 
     * @param node
     * @return
     */
    private List<UpdateResult> checkNodeSAPFunctionFromRepository(final Node node) {
        if (node == null) {
            return Collections.emptyList();
        }
        List<UpdateResult> queryResults = new ArrayList<UpdateResult>();
        String propertyType = (String) node.getPropertyValue(EParameterName.PROPERTY_TYPE.getName());
        if (propertyType != null) {
            if (propertyType.equals(EmfComponent.REPOSITORY)) {
                String propertyValue = (String) node.getPropertyValue(EParameterName.REPOSITORY_PROPERTY_TYPE.getName());

                ConnectionItem connectionItem = null;
                connectionItem = UpdateRepositoryUtils.getConnectionItemByItemId(propertyValue);
                if (connectionItem != null) {
                    boolean same = true;
                    IElementParameter sapNodeParam = node.getElementParameter("SAP_FUNCTION"); //$NON-NLS-1$

                    if (sapNodeParam == null) {
                        return queryResults;
                    }

                    String functioName = TalendTextUtils.removeQuotes((String) sapNodeParam.getValue());
                    SAPConnection connection = (SAPConnection) connectionItem.getConnection();
                    SAPFunctionUnit function = SAPConnectionUtils.findExistFunctionUnit(connection, functioName);
                    if (function == null) {
                        for (IElementParameter param : node.getElementParameters()) {
                            SAPParametersUtils.setNoRepositoryParams(param);
                        }
                        return queryResults;
                    }
                    // check SAP_ITERATE_OUT_TYPE
                    sapNodeParam = node.getElementParameter("SAP_ITERATE_OUT_TYPE"); //$NON-NLS-1$
                    if (sapNodeParam != null && !function.getOutputType().replace(".", "_").equals(sapNodeParam.getValue())) { //$NON-NLS-1$ //$NON-NLS-2$
                        same = false;
                    } else {
                        // check SAP_ITERATE_OUT_TABLENAME
                        sapNodeParam = node.getElementParameter("SAP_ITERATE_OUT_TABLENAME"); //$NON-NLS-1$
                        if (sapNodeParam != null) {
                            String source = (String) sapNodeParam.getValue();
                            String dest = function.getOutputTableName();
                            if (dest == null) {
                                dest = ""; //$NON-NLS-1$
                            }
                            if (!TalendTextUtils.addQuotes(dest).equals(source) && !source.equals(dest)) {
                                same = false;
                            }
                        }
                    }
                    boolean inputSame = true;
                    sapNodeParam = node.getElementParameter("MAPPING_INPUT"); //$NON-NLS-1$
                    if (sapNodeParam != null && sapNodeParam.getValue() != null) {
                        inputSame = SAPConnectionUtils.sameParamterTableWith(function, (List<Map<String, Object>>) sapNodeParam
                                .getValue(), true);
                    }
                    boolean outputSame = true;
                    sapNodeParam = node.getElementParameter("MAPPING_OUTPUT"); //$NON-NLS-1$
                    if (sapNodeParam != null && sapNodeParam.getValue() != null) {
                        outputSame = SAPConnectionUtils.sameParamterTableWith(function, (List<Map<String, Object>>) sapNodeParam
                                .getValue(), false);
                    }

                    if (!same || !inputSame || !outputSame) {
                        String source = UpdateRepositoryUtils.getRepositorySourceName(connectionItem);
                        UpdateCheckResult result = new UpdateCheckResult(node);
                        result.setResult(EUpdateItemType.NODE_SAP_FUNCTION, EUpdateResult.UPDATE, function, source);
                        setConfigrationForReadOnlyJob(result);
                        queryResults.add(result);
                    }
                }

            }
        }
        return queryResults;
    }

    /**
     * 
     * nrousseau Comment method "checkNodeSchemaFromRepository".
     * 
     * @param nc
     * @param metadataTable
     * @return true if the data have been modified
     */
    private List<UpdateResult> checkNodeSchemaFromRepository(final Node node) {
        if (node == null) {
            return Collections.emptyList();
        }
        List<UpdateResult> schemaResults = new ArrayList<UpdateResult>();

        if (PluginChecker.isEBCDICPluginLoaded()) {
            List<UpdateResult> resultForEBCDIC = checkNodeSchemaFromRepositoryForEBCDIC(node);
            if (resultForEBCDIC != null) {
                schemaResults.addAll(resultForEBCDIC);
            }
        }
        // check the metadata from the repository to see if it's up to date.
        IElementParameter schemaTypeParam = node.getElementParameterFromField(EParameterFieldType.SCHEMA_TYPE);
        if (schemaTypeParam != null) {
            IElementParameter schemaParam = schemaTypeParam.getChildParameters().get(EParameterName.SCHEMA_TYPE.getName());
            if (schemaParam != null && ((ElementParameter) schemaTypeParam).isDisplayedByDefault()) {
                if (schemaParam.getValue().equals(EmfComponent.REPOSITORY)) {
                    String propertyValue = (String) schemaTypeParam.getChildParameters().get(
                            EParameterName.REPOSITORY_SCHEMA_TYPE.getName()).getValue();
                    ConnectionItem connectionItem = null;
                    String schemaName = null;
                    String[] names = UpdateManagerUtils.getSourceIdAndChildName(propertyValue);
                    if (names != null && names.length > 1) {
                        connectionItem = UpdateRepositoryUtils.getConnectionItemByItemId(names[0]);
                        schemaName = names[1];
                    }

                    //
                    boolean builtIn = true;
                    UpdateCheckResult result = null;

                    if (connectionItem != null) {
                        final String uniqueName = node.getUniqueName();
                        String newSourceId = getSchemaRenamedMap().get(propertyValue);
                        // renamed
                        if (newSourceId != null && !newSourceId.equals(propertyValue)) {
                            String[] newSourceIdAndName = UpdateManagerUtils.getSourceIdAndChildName(newSourceId);
                            if (newSourceIdAndName != null) {
                                IMetadataTable table = UpdateRepositoryUtils
                                        .getTableByName(connectionItem, newSourceIdAndName[1]);
                                if (table != null) {
                                    String source = UpdateRepositoryUtils.getRepositorySourceName(connectionItem);

                                    final IMetadataTable copyOfrepositoryMetadata = table.clone();
                                    copyOfrepositoryMetadata.setTableName(uniqueName);
                                    copyOfrepositoryMetadata.setAttachedConnector(schemaTypeParam.getContext());

                                    List<Object> parameter = new ArrayList<Object>();
                                    parameter.add(copyOfrepositoryMetadata);
                                    parameter.add(propertyValue);
                                    parameter.add(newSourceId);

                                    result = new UpdateCheckResult(node);
                                    result.setResult(EUpdateItemType.NODE_SCHEMA, EUpdateResult.RENAME, parameter, source);
                                    builtIn = false;
                                }
                            }
                        } else {
                            IMetadataTable table = UpdateRepositoryUtils.getTableByName(connectionItem, schemaName);
                            if (table != null) {
                                String source = UpdateRepositoryUtils.getRepositorySourceName(connectionItem)
                                        + UpdatesConstants.SEGMENT_LINE + table.getLabel();

                                final IMetadataTable copyOfrepositoryMetadata = table.clone();
                                copyOfrepositoryMetadata.setTableName(uniqueName);
                                copyOfrepositoryMetadata.setAttachedConnector(schemaTypeParam.getContext());

                                // // fixed the such as tContextDump component.
                                // MetadataTool.initilializeSchemaFromElementParameters(copyOfrepositoryMetadata,
                                // (List<IElementParameter>) node.getElementParameters());

                                IMetadataTable metadataTable = node.getMetadataFromConnector(schemaTypeParam.getContext());
                                if (!metadataTable.sameMetadataAs(copyOfrepositoryMetadata, IMetadataColumn.OPTIONS_NONE)) {
                                    result = new UpdateCheckResult(node);
                                    result.setResult(EUpdateItemType.NODE_SCHEMA, EUpdateResult.UPDATE, copyOfrepositoryMetadata,
                                            source);
                                }
                                builtIn = false;
                            }
                        }
                    }

                    if (builtIn) {
                        // if the repository connection doesn't exists then set to built-in
                        result = new UpdateCheckResult(node);
                        result.setResult(EUpdateItemType.NODE_SCHEMA, EUpdateResult.BUIL_IN);
                    }

                    // add the check result to resultList, hold the value.
                    if (result != null) {
                        result.setJob(getProcess());
                        setConfigrationForReadOnlyJob(result);
                        schemaResults.add(result);
                    }
                }
            }
        }
        return schemaResults;
    }

    /**
     * 
     * nrousseau Comment method "checkNodeSchemaFromRepositoryForEBCDIC".
     * 
     * @param node
     * @return
     */
    private List<UpdateResult> checkNodeSchemaFromRepositoryForEBCDIC(final Node node) {
        if (node == null) {
            return Collections.emptyList();
        }
        List<UpdateResult> schemaResults = new ArrayList<UpdateResult>();

        if (PluginChecker.isEBCDICPluginLoaded()) {
            IEBCDICProviderService service = (IEBCDICProviderService) GlobalServiceRegister.getDefault().getService(
                    IEBCDICProviderService.class);
            if (service != null) {
                EbcdicConnectionItem repositoryItem = service.getRepositoryItem(node);
                if (repositoryItem != null) {
                    IElementParameter schemasTableParam = node.getElementParameter(IEbcdicConstant.TABLE_SCHEMAS);
                    if (schemasTableParam != null) {

                        List<Map<String, Object>> paramValues = (List<Map<String, Object>>) schemasTableParam.getValue();
                        for (Map<String, Object> line : paramValues) {
                            if (service.isRepositorySchemaLine(node, line)) {
                                final String schemaName = (String) line.get(IEbcdicConstant.FIELD_SCHEMA);
                                final String propertyValue = repositoryItem.getProperty().getId() + UpdatesConstants.SEGMENT_LINE
                                        + schemaName;
                                //
                                boolean builtIn = true;
                                UpdateCheckResult result = null;

                                if (repositoryItem != null) {
                                    String newSourceId = getSchemaRenamedMap().get(propertyValue);
                                    // renamed
                                    if (newSourceId != null && !newSourceId.equals(propertyValue)) {
                                        String[] newSourceIdAndName = UpdateManagerUtils.getSourceIdAndChildName(newSourceId);
                                        if (newSourceIdAndName != null) {
                                            IMetadataTable table = UpdateRepositoryUtils.getTableByName(repositoryItem,
                                                    newSourceIdAndName[1]);
                                            if (table != null) {
                                                String source = UpdateRepositoryUtils.getRepositorySourceName(repositoryItem);

                                                final IMetadataTable copyOfrepositoryMetadata = table.clone();
                                                // String uniqueName =
                                                // this.getProcess().generateUniqueConnectionName(table + "_");
                                                // copyOfrepositoryMetadata.setTableName(uniqueName);
                                                // copyOfrepositoryMetadata.setAttachedConnector(schemaTypeParam.
                                                // getContext());

                                                List<Object> parameter = new ArrayList<Object>();
                                                parameter.add(copyOfrepositoryMetadata);
                                                parameter.add(propertyValue);
                                                parameter.add(newSourceId);
                                                parameter.add(line);

                                                result = new UpdateCheckResult(node);
                                                result.setResult(EUpdateItemType.NODE_SCHEMA, EUpdateResult.RENAME, parameter,
                                                        source);
                                                builtIn = false;
                                            }
                                        }
                                    } else {
                                        IMetadataTable table = UpdateRepositoryUtils.getTableByName(repositoryItem, schemaName);
                                        if (table != null) {
                                            String source = UpdateRepositoryUtils.getRepositorySourceName(repositoryItem)
                                                    + UpdatesConstants.SEGMENT_LINE + table.getLabel();

                                            final IMetadataTable copyOfrepositoryMetadata = table.clone();
                                            // copyOfrepositoryMetadata.setTableName(uniqueName);
                                            //copyOfrepositoryMetadata.setAttachedConnector(schemaTypeParam.getContext()
                                            // );

                                            IMetadataTable metadataTable = null;
                                            // metadataTable =
                                            // node.getMetadataFromConnector(schemaTypeParam.getContext());
                                            metadataTable = MetadataTool.getMetadataTableFromNode(node, schemaName);

                                            if (metadataTable != null
                                                    && !metadataTable.sameMetadataAs(copyOfrepositoryMetadata,
                                                            IMetadataColumn.OPTIONS_NONE)) {

                                                List<Object> parameter = new ArrayList<Object>();
                                                parameter.add(copyOfrepositoryMetadata);
                                                parameter.add(schemaName);

                                                result = new UpdateCheckResult(node);
                                                result.setResult(EUpdateItemType.NODE_SCHEMA, EUpdateResult.UPDATE, parameter,
                                                        source);
                                            }
                                            builtIn = false;
                                        }
                                    }
                                }

                                if (builtIn) {
                                    // if the repository connection doesn't exists then set to built-in
                                    result = new UpdateCheckResult(node);
                                    result.setResult(EUpdateItemType.NODE_SCHEMA, EUpdateResult.BUIL_IN, line);
                                }

                                // add the check result to resultList, hold the value.
                                if (result != null) {
                                    result.setJob(getProcess());
                                    setConfigrationForReadOnlyJob(result);
                                    schemaResults.add(result);
                                }
                            }
                        }
                    }
                }
            }

        }
        return schemaResults;
    }

    /**
     * 
     * nrousseau Comment method "checkNodePropertiesFromRepository".
     * 
     * @param node
     * @return true if the data have been modified
     */
    @SuppressWarnings("unchecked")
    private List<UpdateResult> checkNodePropertiesFromRepository(final Node node) {
        if (node == null) {
            return Collections.emptyList();
        }
        List<UpdateResult> propertiesResults = new ArrayList<UpdateResult>();

        String propertyType = (String) node.getPropertyValue(EParameterName.PROPERTY_TYPE.getName());
        if (propertyType != null) {
            if (propertyType.equals(EmfComponent.REPOSITORY)) {
                String propertyValue = (String) node.getPropertyValue(EParameterName.REPOSITORY_PROPERTY_TYPE.getName());

                IRepositoryObject lastVersion = UpdateRepositoryUtils.getRepositoryObjectById(propertyValue);
                UpdateCheckResult result = null;

                Connection repositoryConnection = null;
                String source = null;
                if (lastVersion != null) {
                    final Item item = lastVersion.getProperty().getItem();
                    if (item != null && item instanceof ConnectionItem) {
                        source = UpdateRepositoryUtils.getRepositorySourceName(item);
                        repositoryConnection = ((ConnectionItem) item).getConnection();
                    }
                }

                if (repositoryConnection != null) {
                    boolean sameValues = true;
                    // if the repository connection exists then test the values
                    for (IElementParameter param : node.getElementParameters()) {
                        String repositoryValue = param.getRepositoryValue();
                        if (param.isShow(node.getElementParameters()) && (repositoryValue != null)) {
                            Object objectValue = RepositoryToComponentProperty.getValue(repositoryConnection, repositoryValue);
                            if (objectValue != null) {
                                if ((param.getField().equals(EParameterFieldType.CLOSED_LIST) && UpdatesConstants.TYPE
                                        .equals(param.getRepositoryValue()))) {
                                    boolean found = false;
                                    String[] list = param.getListRepositoryItems();
                                    for (int i = 0; (i < list.length) && (!found); i++) {
                                        if (objectValue.equals(list[i])) {
                                            found = true;
                                        }
                                    }
                                    if (!found) {
                                        sameValues = false;
                                    }

                                } else {
                                    // check the value
                                    if (!param.getValue().equals(objectValue)) {
                                        if (repositoryConnection instanceof XmlFileConnectionImpl) {
                                            if ((((XmlFileConnectionImpl) repositoryConnection).getXmlFilePath().endsWith("xsd") || ((XmlFileConnectionImpl) repositoryConnection)
                                                    .getXmlFilePath().endsWith("xsd\""))
                                                    && repositoryValue.equals("FILE_PATH")) {
                                            } else {
                                                sameValues = false;
                                            }
                                        } else {
                                            sameValues = false;
                                        }
                                    }
                                }
                            } else if (param.getField().equals(EParameterFieldType.TABLE)
                                    && UpdatesConstants.XML_MAPPING.equals(repositoryValue)) {
                                List<Map<String, Object>> newMaps = RepositoryToComponentProperty.getXMLMappingValue(
                                        repositoryConnection, node.getMetadataList().get(0));
                                if ((param.getValue() instanceof List) && newMaps != null) {
                                    List<Map<String, Object>> oldMaps = (List<Map<String, Object>>) param.getValue();
                                    // sameValues = oldMaps.size() == newMaps.size();
                                    for (int i = 0; i < newMaps.size() && sameValues; i++) {
                                        Map<String, Object> newmap = newMaps.get(i);
                                        Map<String, Object> oldmap = null; // oldMaps.get(i);
                                        if (i < oldMaps.size()) {
                                            oldmap = oldMaps.get(i);
                                        }
                                        if (oldmap != null && sameValues) {
                                            Object o = newmap.get(UpdatesConstants.QUERY);
                                            if (o != null) {
                                                sameValues = newmap.get(UpdatesConstants.QUERY).equals(
                                                        oldmap.get(UpdatesConstants.QUERY));
                                            } else {
                                                sameValues = oldmap.get(UpdatesConstants.QUERY) == null;
                                            }
                                        }
                                    }
                                    if (oldMaps.size() > newMaps.size()) {
                                        int size = newMaps.size();
                                        for (int i = size; i < oldMaps.size(); i++) {
                                            Map<String, Object> map = new HashMap<String, Object>();
                                            map.put(UpdatesConstants.QUERY, UpdatesConstants.EMPTY);
                                            newMaps.add(map);
                                        }
                                        sameValues = false;
                                    }
                                }
                            }
                        }
                    }
                    if (!sameValues) {
                        result = new UpdateCheckResult(node);
                        // for DBConnection
                        // boolean builtIn = true;
                        // if (repositoryConnection instanceof DatabaseConnection) {
                        // IElementParameter typeParam = node.getElementParameter(UpdatesConstants.TYPE);
                        // if (typeParam != null) {
                        // String dbType = ((DatabaseConnection) repositoryConnection).getDatabaseType();
                        // Object type = typeParam.getValue();
                        // if (dbType != null && type != null) {
                        // if (dbType.equalsIgnoreCase((String) type)) {
                        // result.setResult(EUpdateItemType.NODE_PROPERTY, EUpdateResult.UPDATE,
                        // repositoryConnection, source);
                        // builtIn = false;
                        // }
                        // }
                        // }
                        // } else {
                        result.setResult(EUpdateItemType.NODE_PROPERTY, EUpdateResult.UPDATE, repositoryConnection, source);
                        // builtIn = false;
                        // }
                        // if (builtIn) { // only for DB
                        // result.setResult(EUpdateItemType.NODE_PROPERTY, EUpdateResult.BUIL_IN, null, source);
                        // }

                    }
                    for (IElementParameter param : node.getElementParameters()) {
                        String repositoryValue = param.getRepositoryValue();
                        if (param.isShow(node.getElementParameters()) && (repositoryValue != null)
                                && (!param.getName().equals(EParameterName.PROPERTY_TYPE.getName()))
                                && param.getField() != EParameterFieldType.MEMO_SQL) {
                            param.setRepositoryValueUsed(true);
                            param.setReadOnly(true);
                        }
                    }
                    // for context mode(bug 5198)
                    List<UpdateResult> contextResults = checkParameterContextMode(node.getElementParameters(),
                            (ConnectionItem) lastVersion.getProperty().getItem(), null);
                    if (contextResults != null) {
                        propertiesResults.addAll(contextResults);
                    }
                } else {
                    result = new UpdateCheckResult(node);
                    result.setResult(EUpdateItemType.NODE_PROPERTY, EUpdateResult.BUIL_IN);
                }

                // add the check result to resultList, hold the value.
                if (result != null) {
                    result.setJob(getProcess());
                    setConfigrationForReadOnlyJob(result);
                    propertiesResults.add(result);
                }
            }
        }
        return propertiesResults;
    }

    /**
     * ggu Comment method "checkParameterContextMode".
     * 
     * for bug 5198
     */
    private List<UpdateResult> checkParameterContextMode(final List<? extends IElementParameter> parameters,
            ConnectionItem connItem, EComponentCategory category) {
        List<UpdateResult> contextResults = new ArrayList<UpdateResult>();

        if (connItem != null && parameters != null) {
            ConnectionContextHelper.checkContextMode(connItem);
            Connection connection = connItem.getConnection();
            if (connection.isContextMode()) {
                Set<String> neededVars = ConnectionContextHelper.retrieveContextVar(parameters, connection, category);
                if (neededVars != null && !neededVars.isEmpty()) {
                    ContextItem contextItem = ContextUtils.getContextItemById(connection.getContextId());
                    if (contextItem != null) {
                        // find added variables
                        Set<String> addedVars = ConnectionContextHelper.checkAndAddContextVariables(contextItem, neededVars,
                                process.getContextManager(), false);
                        if (addedVars != null && !addedVars.isEmpty()) {
                            UpdateCheckResult result = new UpdateCheckResult(addedVars);
                            String remark = UpdateRepositoryUtils.getRepositorySourceName(connItem);
                            result.setResult(EUpdateItemType.CONTEXT, EUpdateResult.ADD, contextItem, remark
                                    + UpdatesConstants.CONTEXT_MODE);
                            result.setJob(getProcess());
                            result.setContextModeConnectionItem(connItem);
                            setConfigrationForReadOnlyJob(result);
                            contextResults.add(result);

                        }
                    }
                }
            }
        }
        return contextResults;
    }

    /*
     * check node query.
     */
    private List<UpdateResult> checkNodeQueryFromRepository(final Node node) {
        if (node == null) {
            return Collections.emptyList();
        }
        List<UpdateResult> queryResults = new ArrayList<UpdateResult>();

        String propertyType = (String) node.getPropertyValue(EParameterName.QUERYSTORE_TYPE.getName());
        if (propertyType != null) {
            if (propertyType.equals(EmfComponent.REPOSITORY)) {
                String propertyValue = (String) node.getPropertyValue(EParameterName.REPOSITORY_QUERYSTORE_TYPE.getName());

                ConnectionItem connectionItem = null;
                String[] names = UpdateManagerUtils.getSourceIdAndChildName(propertyValue);
                if (names != null) {
                    connectionItem = UpdateRepositoryUtils.getConnectionItemByItemId(names[0]);

                }
                Query query = null;
                String source = null;
                if (connectionItem != null) {
                    query = UpdateRepositoryUtils.getQueryByName(connectionItem, names[1]);
                }
                String connectQuery = null;
                if (query != null) {
                    source = UpdateRepositoryUtils.getRepositorySourceName(connectionItem) + UpdatesConstants.SEGMENT_LINE
                            + query.getLabel();
                    connectQuery = query.getValue();
                }
                UpdateCheckResult result = null;

                if (connectQuery != null) {
                    IElementParameter sqlParam = node.getElementParameterFromField(EParameterFieldType.MEMO_SQL);
                    if (sqlParam != null && UpdatesConstants.QUERY.equals(sqlParam.getName())) {
                        String paramValue = (String) sqlParam.getValue();
                        // modefied by hywang , to see if there is contextmode
                        if (!query.isContextMode()) {
                            connectQuery = QueryUtil.checkAndAddQuotes(connectQuery);
                        }

                        if (!connectQuery.equals(paramValue)) {
                            result = new UpdateCheckResult(node);
                            result.setResult(EUpdateItemType.NODE_QUERY, EUpdateResult.UPDATE, query, source);
                        }
                        sqlParam.setReadOnly(true);
                        sqlParam.setRepositoryValueUsed(true);
                    }
                } else {
                    result = new UpdateCheckResult(node);
                    result.setResult(EUpdateItemType.NODE_QUERY, EUpdateResult.BUIL_IN);
                }

                if (result != null) {
                    result.setJob(getProcess());
                    setConfigrationForReadOnlyJob(result);
                    queryResults.add(result);
                }

            }
        }

        return queryResults;
    }

    /**
     * 
     * ggu Comment method "checkJobletNodesContext".
     * 
     * check and update, the result only record the operation.
     */
    private List<UpdateResult> checkJobletNodesContext() {
        if (getProcess().isReadOnly()) { // not readonly
            return Collections.emptyList();
        }
        List<AbstractProcessProvider> findAllProcessProviders = AbstractProcessProvider.findAllProcessProviders();
        List<String> labelList = new ArrayList<String>();
        for (AbstractProcessProvider abstractProcessProvider : findAllProcessProviders) {
            if (abstractProcessProvider != null) {
                List<String> tmpList = abstractProcessProvider.updateProcessContextsWithoutUI(getProcess());
                if (tmpList != null && !tmpList.isEmpty()) {
                    labelList.addAll(tmpList);
                }
            }
        }
        // source to variables list map
        Map<String, Set<String>> reformMap = new HashMap<String, Set<String>>();
        for (String label : labelList) {
            String[] str = label.split(UpdatesConstants.SPACE);
            if (str.length == 2) {
                String var = str[0].trim();
                String source = removeBrackets(str[1]);
                if (IContextParameter.BUILT_IN.equals(source)) {
                    source = str[1];
                }
                Set<String> set = reformMap.get(source);
                if (set == null) {
                    set = new HashSet<String>();
                    reformMap.put(source, set);
                }
                if (!set.contains(var)) {
                    set.add(var);
                }
            }
        }
        List<UpdateResult> contextResults = new ArrayList<UpdateResult>();
        for (String source : reformMap.keySet()) {
            Set<String> set = reformMap.get(source);
            if (set != null && !set.isEmpty()) {
                UpdateCheckResult result = new UpdateCheckResult(set);
                result.setResult(EUpdateItemType.JOBLET_CONTEXT, EUpdateResult.JOBLET_UPDATE, null, UpdatesConstants.CONTEXT
                        + UpdatesConstants.COLON + source);
                result.setJob(getProcess());
                setConfigrationForReadOnlyJob(result);
                contextResults.add(result);
            }
        }

        return contextResults;

    }

    private String removeBrackets(String str) {
        if (str == null) {
            return UpdatesConstants.EMPTY;
        }
        final String prefix = "\\"; //$NON-NLS-1$
        str = str.trim();

        str = str.replaceAll(prefix + UpdatesConstants.LEFT_BRACKETS, UpdatesConstants.EMPTY);
        str = str.replaceAll(prefix + UpdatesConstants.RIGHT_BRACKETS, UpdatesConstants.EMPTY);
        return str.trim();
    }

    private List<UpdateResult> checkJobletNodeReload() {
        if (getProcess() == null || jobletProcessProvider == null) {
            return Collections.emptyList();
        }

        List<UpdateResult> nodeResults = new ArrayList<UpdateResult>();
        for (INode node : this.getProcess().getGraphicalNodes()) {
            final Item jobletItem = jobletProcessProvider.getJobletItem(node);
            if (jobletItem != null) {
                final Property property = jobletItem.getProperty();
                final String id = property.getId();
                final Date modificationDate = property.getModificationDate();

                final Date oldDate = this.jobletReferenceMap.get(id);

                if (!modificationDate.equals(oldDate) && !getProcess().getId().equals(id)) {
                    List<INode> jobletNodes = findRelatedJobletNode(getProcess(), property.getLabel(), null);
                    if (jobletNodes != null && !jobletNodes.isEmpty()) {
                        String source = UpdatesConstants.JOBLET + UpdatesConstants.COLON + property.getLabel();
                        UpdateCheckResult result = new UpdateCheckResult(jobletNodes);
                        result.setResult(EUpdateItemType.RELOAD, EUpdateResult.RELOAD, jobletItem, source);

                        result.setJob(getProcess());
                        setConfigrationForReadOnlyJob(result);

                        nodeResults.add(result);
                    }
                }
            }
        }
        return nodeResults;
    }

    /**
     * 
     * ggu Comment method "checkNodesPropertyChanger".
     * 
     * If this is not relational joblet node to update. filter it.
     * 
     * @deprecated seems have unused it.
     */
    private List<UpdateResult> checkJobletNodesPropertyChanger() {
        if (getProcess() == null || getNodePropertyChanger() == null) {
            return Collections.emptyList();
        }
        List<UpdateResult> nodeResults = new ArrayList<UpdateResult>();
        for (PropertyChangeEvent event : getNodePropertyChanger()) {
            UpdateCheckResult result = null;

            String propertyName = event.getPropertyName();
            if (propertyName.equals(ComponentUtilities.NORMAL)) {
                if (jobletProcessProvider != null && !jobletProcessProvider.hasJobletComponent(getProcess())) {
                    break;

                }
                Object object = event.getSource();
                if (object instanceof IProcess) {
                    IProcess process2 = (IProcess) object;
                    // avoid reload self
                    if (!getProcess().getId().equals(process2.getId())) {
                        List<INode> jobletNodes = findRelatedJobletNode(getProcess(), process2.getLabel(), null);
                        if (jobletNodes != null && !jobletNodes.isEmpty()) {
                            String source = UpdatesConstants.JOBLET + UpdatesConstants.COLON + process2.getLabel();
                            result = new UpdateCheckResult(jobletNodes);
                            result.setResult(EUpdateItemType.RELOAD, EUpdateResult.RELOAD, event, source);
                        }
                    }
                } else { // reload all components
                    result = new UpdateCheckResult(UpdatesConstants.COMPONENT);
                    result.setResult(EUpdateItemType.RELOAD, EUpdateResult.RELOAD, event);
                }
                // moved (bug 4231)
                // } else if (propertyName.equals(ComponentUtilities.JOBLET_NAME_CHANGED)) {
                // String oldName = (String) event.getOldValue();
                // String newName = (String) event.getNewValue();
                // List<INode> jobletNodes = findRelatedJobletNode(getProcess(), oldName, newName);
                // if (jobletNodes != null && !jobletNodes.isEmpty()) {
                // String source = UpdatesConstants.JOBLET + UpdatesConstants.COLON + newName;
                //
                // result = new UpdateCheckResult(jobletNodes);
                // result.setResult(EUpdateItemType.JOBLET_RENAMED, EUpdateResult.JOBLET_UPDATE, event, source);
                // }
                // } else if (propertyName.equals(ComponentUtilities.JOBLET_SCHEMA_CHANGED)) {
                // Object object = event.getSource();
                // if (object instanceof IProcess) {
                // String oldName = ((IProcess) object).getName();
                // Set<String> nodesName = findRelatedJobletNode(getProcess(), oldName, null);
                // if (nodesName != null && !nodesName.isEmpty()) {
                // String source = UpdatesConstants.JOBLET + UpdatesConstants.COLON + ((IProcess) object).getLabel();
                //
                // result = new UpdateCheckResult(nodesName);
                // result.setResult(EUpdateItemType.JOBLET_SCHEMA, EUpdateResult.JOBLET_UPDATE, event, source);
                // }
                // }
            }
            if (result != null) {
                result.setJob(getProcess());
                setConfigrationForReadOnlyJob(result);
                nodeResults.add(result);
            }
        }
        // clear
        getNodePropertyChanger().clear();
        return nodeResults;
    }

    @SuppressWarnings("unchecked")
    private List<INode> findRelatedJobletNode(Process process, String oldjobletName, String newJobletName) {
        if (oldjobletName == null || process == null) {
            return null;
        }
        if (newJobletName == null) {
            newJobletName = oldjobletName;
        }
        IComponent newComponent = ComponentsFactoryProvider.getInstance().get(newJobletName);
        if (newComponent == null) {
            return Collections.EMPTY_LIST;
        }

        List<INode> jobletNodes = new ArrayList<INode>();

        for (Node node : (List<Node>) process.getGraphicalNodes()) {
            if (node.getComponent().getName().equals(newJobletName)) {
                jobletNodes.add(node);
            }
        }
        return jobletNodes;
    }

    private List<UpdateResult> checkJobletNodeSchema() {
        if (jobletProcessProvider != null) {
            return jobletProcessProvider.checkJobletNodeSchema(getProcess());
        }
        return null;
    }

    public List<UpdateResult> getUpdatesNeeded(EUpdateItemType type) {

        if (type == null) {
            return null;
        }
        List<UpdateResult> tmpResults = new ArrayList<UpdateResult>();
        switch (type) {
        case NODE_PROPERTY:
        case NODE_SCHEMA:
        case NODE_QUERY:
        case NODE_SAP_FUNCTION:
            tmpResults = checkNodesParameters(type);
            break;
        case JOB_PROPERTY_EXTRA:
        case JOB_PROPERTY_STATS_LOGS:
            tmpResults = checkMainParameters(type);
            break;
        case CONTEXT:
            tmpResults = checkContext();
            break;
        case JOBLET_SCHEMA:
            tmpResults = checkJobletNodeSchema();
            break;
        case JOBLET_RENAMED: // unused
            // case RELOAD:
            tmpResults = checkJobletNodesPropertyChanger();
            break;
        case RELOAD:
            tmpResults = checkJobletNodeReload();
            break;
        case JOBLET_CONTEXT:
            tmpResults = checkJobletNodesContext();
            break;
        default:
        }
        return tmpResults;
    }

    @SuppressWarnings("unchecked")
    public boolean executeUpdates(List<UpdateResult> results) {
        return UpdateManagerUtils.executeUpdates(results);
    }

    /**
     * 
     * DOC hcw ProcessUpdateManager class global comment. Detailled comment
     */
    static class ContextItemParamMap {

        private Map<ContextItem, Set<String>> map = new HashMap<ContextItem, Set<String>>();

        public void add(ContextItem item, String param) {
            Set<String> params = map.get(item);
            if (params == null) {
                params = new HashSet<String>();
                map.put(item, params);
            }
            params.add(param);
        }

        @SuppressWarnings("unchecked")
        public Set<String> get(ContextItem item) {
            Set<String> params = map.get(item);
            return (params == null) ? Collections.EMPTY_SET : params;

        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public Set<ContextItem> getContexts() {
            return map.keySet();
        }
    }

    private void setConfigrationForReadOnlyJob(UpdateCheckResult result) {
        if (this.process != null && this.process.isReadOnly()) {
            result.setChecked(false);
            result.setRemark(Messages.getString("ProcessUpdateManager.ReadOnlyProcessUpdateWarningMessages")); //$NON-NLS-1$
            result.setReadOnlyProcess(true);
        }

    }

}
