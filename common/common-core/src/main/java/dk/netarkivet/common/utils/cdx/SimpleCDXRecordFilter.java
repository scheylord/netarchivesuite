/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2017 The Royal Danish Library, 
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

package dk.netarkivet.common.utils.cdx;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A Simple CDXRecordFilter to be extended. It only implements the filtername method.
 */
@SuppressWarnings({"serial"})
public abstract class SimpleCDXRecordFilter implements CDXRecordFilter {

    /** Variable holding the filtername. */
    private String filtername;

    /**
     * @param filtername - the name of the filter
     * @throws ArgumentNotValid If 'filtername' equals null or the empty string
     */
    public SimpleCDXRecordFilter(String filtername) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(filtername, "filtername");
        this.filtername = filtername;
    }

    /**
     * @return the filter name
     */
    public String getFilterName() {
        return this.filtername;
    }

    /*
     * (non-Javadoc)
     * 
     * @see dk.netarkivet.common.utils.cdx.CDXRecordFilter#process( dk.netarkivet.common.utils.cdx.CDXRecord)
     */
    public abstract boolean process(CDXRecord cdxrec);

}
