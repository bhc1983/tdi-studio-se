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
package org.talend.sqlbuilder.util;

import java.util.StringTokenizer;

/**
 * QueryTokenizer based on original SquirreL SQL tokenizer, but adds the
 * possibility to use multiple characters as the sql delimeter.
 * 
 * This is used for splitting the input text in the editor into
 * multiple executable sql statements.
 * 
 * @author qzhang
 */
public class QueryTokenizer {

    private static String palternateQuerySeparator;

    private static String pquerySeparator;

    private String psQuerys;

    private String psNextQuery;

    /**
     * These characters at the beginning of an SQL statement indicate that it is
     * a comment.
     */
    private String psolComment;

    /**
     * QueryTokenizer constructor comment.
     */
    public QueryTokenizer(String sql, String querySeparator, String alternateSeparator, String solComment) {

        if (querySeparator != null && querySeparator.trim().length() > 0) {
            pquerySeparator = querySeparator.substring(0, 1);
        } else {
            // failsave..
            pquerySeparator = ";";
        }
        
        if (alternateSeparator != null && alternateSeparator.trim().length() > 0) {
            palternateQuerySeparator = alternateSeparator;    
        } else {
            palternateQuerySeparator = null;
        }        

        if (solComment != null && solComment.trim().length() > 0) {
            psolComment = solComment;
        } else {
            psolComment = null;
        }

        if (sql != null) {
            psQuerys = prepareSQL(sql);
            psNextQuery = parse();
        } else {
            psQuerys = "";
        }
    }

    public boolean hasQuery() {
        return psNextQuery != null;
    }

    public String nextQuery() {
        String sReturnQuery = psNextQuery;
        psNextQuery = parse();
        return sReturnQuery;
    }

    private int findFirstSeparator() {

        String separator = pquerySeparator;
        int separatorLength = pquerySeparator.length();
        int iQuoteCount = 1;
        int iIndex1 = 0 - separatorLength;
        
        while (iQuoteCount % 2 != 0) {
            
            iQuoteCount = 0;
            iIndex1 = psQuerys.indexOf(separator, iIndex1 + separatorLength);

            if (iIndex1 > -1) {
                int iIndex2 = psQuerys.lastIndexOf('\'', iIndex1 + separatorLength - 1);
                while (iIndex2 != -1) {
                    if (psQuerys.charAt(iIndex2 - 1) != '\\') {
                        iQuoteCount++;
                    }
                    iIndex2 = psQuerys.lastIndexOf('\'', iIndex2 - 1);
                }
            } else {
                return -1;
            }
        }
        
        return iIndex1;
    }

    private int findFirstAlternateSeparator() {

        if (palternateQuerySeparator == null) {
            return -1;
        }

        String separator = palternateQuerySeparator;
        int separatorLength = palternateQuerySeparator.length();
        int iQuoteCount = 1;
        int iIndex1 = 0 - separatorLength;
        
        while (iQuoteCount % 2 != 0) {
            
            iQuoteCount = 0;
            iIndex1 = psQuerys.indexOf(separator, iIndex1 + separatorLength);

            if (iIndex1 > -1) {
                int iIndex2 = psQuerys.lastIndexOf('\'', iIndex1 + separatorLength - 1);
                while (iIndex2 != -1) {
                    if (psQuerys.charAt(iIndex2 - 1) != '\\') {
                        iQuoteCount++;
                    }
                    iIndex2 = psQuerys.lastIndexOf('\'', iIndex2 - 1);
                }
            } else {
                return -1;
            }
        }
        
        return iIndex1;
    }

    public String parse() {
        
        if (psQuerys.length() == 0) {
            return null;
        }
        
        String separator = pquerySeparator;
                
        int indexSep = findFirstSeparator();
        int indexAltSep = findFirstAlternateSeparator();
        
        if (indexAltSep > -1) {
            if (indexSep < 0 || indexAltSep < indexSep) {
                // use alternate separator
                separator = palternateQuerySeparator;
            }
        }
        
        int separatorLength = separator.length();
        int iQuoteCount = 1;
        int iIndex1 = 0 - separatorLength;
        
        while (iQuoteCount % 2 != 0) {
            
            iQuoteCount = 0;
            iIndex1 = psQuerys.indexOf(separator, iIndex1 + separatorLength);

            if (iIndex1 > -1) {
                int iIndex2 = psQuerys.lastIndexOf('\'', iIndex1 + separatorLength - 1);
                while (iIndex2 != -1) {
                    if (psQuerys.charAt(iIndex2 - 1) != '\\') {
                        iQuoteCount++;
                    }
                    iIndex2 = psQuerys.lastIndexOf('\'', iIndex2 - 1);
                }
            } else {
                String sNextQuery = psQuerys;
                psQuerys = "";
                if (psolComment != null && sNextQuery.startsWith(psolComment)) {
                    return parse();
                }
                return replaceLineFeeds(sNextQuery);
            }
        }
        String sNextQuery = psQuerys.substring(0, iIndex1);
        psQuerys = psQuerys.substring(iIndex1 + separatorLength).trim();
        if (psolComment != null && sNextQuery.startsWith(psolComment)) {
            return parse();
        }
        return replaceLineFeeds(sNextQuery);
    }

    private String prepareSQL(String sql) {
        StringBuffer results = new StringBuffer(1024);

        for (StringTokenizer tok = new StringTokenizer(sql.trim(), "\n", false); tok.hasMoreTokens();) {
            String line = tok.nextToken();
            if (!line.startsWith(psolComment)) {
                results.append(line).append('\n');
            }
        }

        return results.toString();
    }

    private String replaceLineFeeds(String sql) {
        StringBuffer sbReturn = new StringBuffer();
        int iPrev = 0;
        int linefeed = sql.indexOf('\n');
        int iQuote = -1;
        while (linefeed != -1) {
            iQuote = sql.indexOf('\'', iQuote + 1);
            if (iQuote != -1 && iQuote < linefeed) {
                int iNextQute = sql.indexOf('\'', iQuote + 1);
                if (iNextQute > linefeed) {
                    sbReturn.append(sql.substring(iPrev, linefeed));
                    sbReturn.append('\n');
                    iPrev = linefeed + 1;
                    linefeed = sql.indexOf('\n', iPrev);
                }
            } else {
                linefeed = sql.indexOf('\n', linefeed + 1);
            }
        }
        sbReturn.append(sql.substring(iPrev));
        return sbReturn.toString();
    }

}
