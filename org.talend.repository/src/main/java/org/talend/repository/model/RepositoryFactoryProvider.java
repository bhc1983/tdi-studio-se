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
package org.talend.repository.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.IllegalPluginConfigurationException;
import org.talend.commons.utils.workbench.extensions.ExtensionImplementationProviders;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.repository.ERepositoryType;
import org.talend.repository.model.extensions.ExtensionPointFactory;
import org.talend.repository.model.factoriesImpl.RepositoryFactoryImpl;

/**
 * Provides, using extension points, implementation of many factories.
 * 
 * <ul>
 * <li>IProcessFactory</li>
 * </ul>
 * 
 * $Id$
 */
public class RepositoryFactoryProvider {

    private static Map<RepositoryContext, IRepositoryFactory> listSingleton = new HashMap<RepositoryContext, IRepositoryFactory>();

    private static IRepositoryFactory localSingleton = null;

    private static IRepositoryFactory remoteSingleton = null;

    private static boolean init = false;

    /**
     * Returns a IRepositoryFactory based on the default context.
     */
    public static IRepositoryFactory getInstance() {
        init();
        RepositoryContext repositoryContext = (RepositoryContext) CorePlugin.getContext().getProperty(
                Context.REPOSITORY_CONTEXT_KEY);
        if (repositoryContext.getProject().isLocal()) {
            return localSingleton;
        } else {
            return remoteSingleton;
        }
    }

    public static IRepositoryFactory getInstance(RepositoryContext repositoryContext) {
        init();
        IRepositoryFactory toReturn = null;
        if (repositoryContext.getType() == ERepositoryType.LOCAL) {
            toReturn = localSingleton;
        } else {
            toReturn = remoteSingleton;
        }
        toReturn.setRepositoryContext(repositoryContext);
        return toReturn;
    }

    private static void init() {
        if (!init) {
            List<IRepositoryFactory> factoriesFromProvider = ExtensionImplementationProviders
                    .getInstance(ExtensionPointFactory.REPOSITORY_PROVIDER);

            for (IRepositoryFactory repositoryFactory : factoriesFromProvider) {
                ERepositoryType type = repositoryFactory.getType();
                switch (type) {
                case LOCAL:
                    localSingleton = repositoryFactory;
                    init = true;
                    break;
                case REMOTE:
                    remoteSingleton = repositoryFactory;
                    break;
                }
            }
        }
    }

    /**
     * Returns a IRepositoryFactory based on the specified context.
     */
    public static IRepositoryFactory oldgetInstance(RepositoryContext repositoryContext) {
        IRepositoryFactory toReturn = listSingleton.get(repositoryContext);

        if (toReturn == null) {
            try {
                List<IRepositoryFactory> factoriesFromProvider = ExtensionImplementationProviders
                        .getInstance(ExtensionPointFactory.REPOSITORY_PROVIDER);

                // if (repositoryContext.getType() != null) {
                for (IRepositoryFactory repositoryFactory : factoriesFromProvider) {
                    ERepositoryType type = repositoryFactory.getType();
                    ERepositoryType type2 = repositoryContext.getType();
                    if (type == type2) {
                        toReturn = new RepositoryFactoryImpl(repositoryFactory);
                        toReturn.setRepositoryContext(repositoryContext);
                        toReturn.initialize();
                        listSingleton.put(repositoryContext, toReturn);
                        break;
                    }
                }
                // } else {
                // toReturn = new RepositoryFactoryImpl(factoriesFromProvider.get(0));
                // }
            } catch (IllegalPluginConfigurationException e) {
                ExceptionHandler.process(e);
            }
        }
        return toReturn;
    }

    public static String[] getAvailableRepositories() {
        SortedSet<String> availableRepositories = new TreeSet<String>();

        List<IRepositoryFactory> factoriesFromProvider = ExtensionImplementationProviders
                .getInstance(ExtensionPointFactory.REPOSITORY_PROVIDER);

        for (IRepositoryFactory factory : factoriesFromProvider) {
            availableRepositories.add(factory.getType().getLabel());
        }
        return availableRepositories.toArray(new String[] {});
    }
}
