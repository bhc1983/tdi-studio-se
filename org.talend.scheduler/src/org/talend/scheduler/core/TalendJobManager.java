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
package org.talend.scheduler.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.data.container.Content;
import org.talend.commons.utils.data.container.ContentList;
import org.talend.commons.utils.data.container.RootContainer;
import org.talend.core.CorePlugin;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.general.Project;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IContextManager;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.prefs.ITalendCorePrefConstants;
import org.talend.designer.core.model.context.ContextManager;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.designer.runprocess.perl.PerlUtils;
import org.talend.repository.model.IRepositoryFactory;
import org.talend.repository.model.RepositoryFactoryProvider;
import org.talend.scheduler.SchedulerPlugin;

/**
 * This class is responsible for keeping synchronous with project,job and context in comboxes, according to these 3
 * arameters, generate corresponding command.
 * 
 * <br/> $Id$
 * 
 */
public class TalendJobManager {

    private RepositoryContext repositoryContext;

    private IRepositoryFactory factory;

    // private RootContainer<Integer, IRepositoryObject> processContainer;
    private RootContainer<String, IRepositoryObject> processContainer;

    // private ContentList<Integer, IRepositoryObject> processAbsoluteMembers;
    private ContentList<String, IRepositoryObject> processAbsoluteMembers;

    /**
     * 
     * Default constructor.
     */
    public TalendJobManager() {

        try {
            // repositoryContext = (RepositoryContext)
            // CorePlugin.getContext().getProperty(Context.REPOSITORY_CONTEXT_KEY);

            factory = RepositoryFactoryProvider.getInstance();

            processContainer = factory.getProcess();

            processAbsoluteMembers = processContainer.getAbsoluteMembers();

        } catch (Exception exception) {
            SchedulerPlugin.log(exception);

        }
    }

    /**
     * 
     * Gets current project.
     * 
     * @return an instance of Project.
     */
    public Project getCurrentProject() {

        Project project = repositoryContext.getProject();

        return project;
    }

    /**
     * 
     * Gets all prjects' names.
     * 
     * @return an array of project names.
     */
    // public String[] getAllProjectNames() {
    // List<String> names = new ArrayList<String>();
    //
    // for (Project project : this.getAllProjects()) {
    // names.add(project.getLabel());
    // }
    // return names.toArray(new String[names.size()]);
    // }
    /**
     * 
     * Gets all projects.
     * 
     * @return an ArrayList of Projects.
     */
    // public List<Project> getAllProjects() {
    // List<Project> list = new ArrayList<Project>();
    //
    // Project[] projects = null;
    // try {
    // String server = checkString(repositoryContext.getServer());
    // String port = checkString(repositoryContext.getPort() + "");
    // // String user = checkString(repositoryContext.getUser().getName());
    // User user = repositoryContext.getUser();
    // // String password = checkString(repositoryContext.getPassword());
    //
    // projects = factory.readProject(server, port, user);
    // } catch (PersistenceException e) {
    // SchedulerPlugin.log(e);
    // }
    //
    // if (projects == null) {
    // return new ArrayList<Project>();
    // }
    //
    // for (Project project : projects) {
    // list.add(project);
    // }
    //
    // return list;
    // }
    /**
     * 
     * Gets the set of current project's job.
     * 
     * @return a List of jobs
     * @exception
     */
    public List<String> getCurrentProjectJobs() {
        List<String> processNameList = new ArrayList<String>();

        try {
            for (Content<String, IRepositoryObject> object : processAbsoluteMembers.values()) {
                IRepositoryObject process = (IRepositoryObject) object.getContent();
                if (!factory.isDeleted(process)) {
                    String path = object.getParent().getPath().toString();
                    String name;
                    if (path.equals("")) {
                        name = IPath.SEPARATOR + process.getLabel();
                    } else {
                        name = IPath.SEPARATOR + path + IPath.SEPARATOR + process.getLabel();
                    }

                    processNameList.add(name);
                }
            }
        } catch (PersistenceException e) {
            SchedulerPlugin.log(e);
        }
        return processNameList;
    }

    /**
     * 
     * Gets the set of current job's context.
     * 
     * @return a List of context names.
     * 
     */
    public List<String> getCurrentJobContexts() {
        List<String> contextNameList = new ArrayList<String>();

        try {
            for (Content<String, IRepositoryObject> object : processAbsoluteMembers.values()) {
                IRepositoryObject process = (IRepositoryObject) object.getContent();
                if (process.getProperty().getItem() instanceof ProcessItem) {
                    ProcessItem processItem = (ProcessItem) process.getProperty().getItem();
                    // Process p = new Process();
                    // p.loadXmlFile((ProcessType) (processItem).getProcess());

                    IContextManager c = ContextManager.getContextManagerFromXmlProcess((ProcessType) (processItem).getProcess());
                    // p.getContextManager();
                    for (IContext context : c.getListContext()) {
                        if (contextNameList.contains(context.getName())) {
                            continue;
                        }
                        contextNameList.add(context.getName());
                    }
                }
            }
        } catch (Exception e) {
            SchedulerPlugin.log(e);
        }
        return contextNameList;
    }

    /**
     * 
     * Updates context list according to selected job name.
     * 
     * @param selectedJobName
     * 
     * @return a List of context name.
     */
    public List<String> updateContextList(String selectedJobName) {

        List<String> contextNameList = new ArrayList<String>();

        String selectedProcess = selectedJobName;

        try {
            for (Content<String, IRepositoryObject> object : processAbsoluteMembers.values()) {
                IRepositoryObject process = (IRepositoryObject) object.getContent();
                String id = process.getLabel();

                if (selectedProcess.equals(id)) {
                    if (process.getProperty().getItem() instanceof ProcessItem) {

                        ProcessItem processItem = (ProcessItem) process.getProperty().getItem();
                        // processItem.getProperty().getItem().getProperty().g
                        // Process p = new Process();
                        // p.loadXmlFile((ProcessType) processItem.getProcess());
                        // IContextManager c = p.getContextManager();
                        IContextManager c = ContextManager.getContextManagerFromXmlProcess((ProcessType) (processItem)
                                .getProcess());
                        for (IContext context : c.getListContext()) {
                            contextNameList.add(context.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {

            SchedulerPlugin.log(e);
        }

        return contextNameList;
    }

    /**
     * 
     * DOC dev Comment method "getCommandByTalendJob".
     * 
     * @param project
     * @param jobName
     * @param context
     * @return
     * @throws ProcessorException
     */
    public String getCommandByTalendJob(String project, String jobName, String context) throws ProcessorException {
        project = project.replace("/", "");
        jobName = jobName.replace("/", "");
        context = context.replace("/", "");
        String contextArg = "--context=";

        String projectSeparator = ".process";

        String wordSeparator = "_";

        String perlExt = ".pl";
        IPreferenceStore prefStore = CorePlugin.getDefault().getPreferenceStore();
        String perlInterpreter = prefStore.getString(ITalendCorePrefConstants.PERL_INTERPRETER);
        if (perlInterpreter == null || perlInterpreter.length() == 0) {
            // throw new ProcessorException(Messages.getString("Processor.configurePerl")); //$NON-NLS-1$
        }

        String perlLib = null;
        String exePath = null;
        try {
            perlLib = PerlUtils.getPerlModulePath().toOSString();
            IProject pro = PerlUtils.getProject();
            IPath path = pro.getLocation();

            exePath = path.toOSString();
        } catch (Exception ee1) {
            SchedulerPlugin.log(ee1);
            // throw new ProcessorException(Messages.getString("Processor.perlModuleNotFound")); //$NON-NLS-1$
        }

        String perlLibOption = perlLib != null && perlLib.length() > 0 ? "-I" + perlLib : ""; //$NON-NLS-1$ //$NON-NLS-2$

        String perlCode = project + projectSeparator + wordSeparator + jobName + perlExt;

        String contextCode = project + projectSeparator + wordSeparator + jobName + wordSeparator + context + perlExt;

        String[] cmd = new String[] { perlInterpreter, perlLibOption, exePath + "/" + perlCode,
                contextArg + exePath + "/" + contextCode };

        StringBuffer sb = new StringBuffer();
        sb.append("");
        for (String s : cmd) {
            sb.append(' ').append(s);
        }
        return sb.toString();
    }

    /**
     * 
     * Checks if a string is null or empty, translate it to "".
     * 
     * @param str input string
     * @return a string
     */
    private String checkString(String str) {
        if (str == null || str.length() == 0) {
            return "";
        }
        return str;
    }
}
