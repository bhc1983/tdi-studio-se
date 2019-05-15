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

import org.talend.designer.dbmap.language.AbstractDbLanguage;
import org.talend.designer.dbmap.language.operator.GenericDbOperatorsManager;

/**
 * created by hcyi on May 13, 2019
 * Detailled comment
 *
 */
public class MSSqlLanguage extends AbstractDbLanguage {

    public MSSqlLanguage() {
        super(new GenericDbOperatorsManager());
    }
}
