---
version: 6.4.1
module: https://talend.poolparty.biz/coretaxonomy/42
product:
- https://talend.poolparty.biz/coretaxonomy/23
---

# TPS-3090

| Info             | Value |
| ---------------- | ---------------- |
| Patch Name       | Patch_20190513_TPS-3090_v1-7.0.1 |
| Release Date     | 2019-05-13 |
| Target Version   | Talend-Studio-20170623_1246-V6.4.1 |
| Product affected | Talend Studio |

## Introduction

This is a self-contained patch.

**NOTE**: For information on how to obtain this patch, reach out to your Support contact at Talend.

## Fixed issues

This patch contains the following fixes:

- TPS-3090 [6.4.1] tFileInputJSON component has wrong jar in build resulting in class not found exception (TDI-39432)

## Prerequisites

Consider the following requirements for your system:

- Talend Studio 6.4.1 must be installed.

## Installation

### Installing the patch using Software update

**NOTE**: If the customer has not yet installed any patch before with Nexus3, a TUP patch named Patch_20180510_TPS-2482_v1-7.0.1.zip must be deployed first in the appoarch "Installing the patch using Talend Studio".

1) Logon TAC and switch to Configuration->Software Update, then enter the correct values and save referring to the documentation: https://help.talend.com/reader/f7Em9WV_cPm2RRywucSN0Q/j9x5iXV~vyxMlUafnDejaQ

2) Switch to Software update page, where the new patch will be listed. The patch can be downloaded from here into the nexus repository.

3) On Studio Side: Logon Studio with remote mode, on the logon page the Update button is displayed: click this button to install the patch.

### Installing the patch using Talend Studio

1) Create a folder named "patches" under your studio installer directory and copy the patch .zip file to this folder.

2) Restart your studio: a window pops up, then click OK to install the patch, or restart the commandline and the patch will be installed automatically.

### Installing the patch using Commandline

Execute the following commands:

1. Talend-Studio-win-x86_64.exe -nosplash -application org.talend.commandline.CommandLine -consoleLog -data commandline-workspace startServer -p 8002 --talendDebug
2. initRemote {tac_url} -ul {TAC login username} -up {TAC login password}
3. checkAndUpdate -tu {TAC login username} -tup {TAC login password}

## Uninstallation <!-- if applicable -->

<!--
Detailed instructions to uninstall the patch

In case this patch cannot be uninstalled, it is your responsability to define the backup procedures for your organization before installing.

-->
Backup the Affected files list below. Uninstall the patch by restore the backup files.

## Affected files for this patch <!-- if applicable -->

The following files are installed by this patch:

- {Talend_Studio_path}/plugins/org.talend.designer.components.localprovider_6.4.1.20170623_1246/components/tFileInputJSON/tFileInputJSON_java.xml
