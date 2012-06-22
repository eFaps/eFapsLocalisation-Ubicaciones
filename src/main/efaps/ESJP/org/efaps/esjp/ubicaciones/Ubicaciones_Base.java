/*
 * Copyright 2003 - 2011 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.ubicaciones;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.field.Field;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIUbicaciones;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("66b65261-371f-4cfc-8260-af1904543c02")
@EFapsRevision("$Rev$")
public abstract class Ubicaciones_Base
{

    /**
     * Method is called with update event only in case the selected item.
     *
     * @param _parameter Parameters as passed from eFaps API.
     * @return Return
     * @throws EFapsException on error
     */
    public Return updateDropDown4Ubication(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        final Long id = Long.parseLong(_parameter.getParameterValue(field.getName()));
        final QueryBuilder queryBldr = new QueryBuilder(CIUbicaciones.UbicacionStandard);
        queryBldr.addWhereAttrEqValue(CIUbicaciones.UbicacionStandard.ParentLink, id);
        queryBldr.addOrderByAttributeAsc(CIUbicaciones.UbicacionStandard.Name);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.setEnforceSorted(true);
        multi.addAttribute(CIUbicaciones.UbicacionStandard.Name);
        multi.execute();
        final StringBuilder js = new StringBuilder();
        final String target = (String) props.get("targetField");
        if (target != null && !target.isEmpty()) {
            js.append("var box; var option;");
            final String clean = (String) props.get("cleanFields");
            if (clean != null && !clean.isEmpty()) {
                final String[] cleanFields = clean.split(";");
                for (final String fieldName : cleanFields) {
                    js.append("box = document.getElementsByName(\'").append(fieldName).append("\')[0];")
                        .append("while ( box.options.length ) box.options[0] = null;");
                }
            }
            js.append("box = document.getElementsByName(\'").append(target).append("\')[0];")
                .append("while ( box.options.length ) box.options[0] = null;")
                .append("option = new Option(\"").append(StringEscapeUtils.unescapeJavaScript("-")).append("\",")
                .append(0).append(");").append("box.options[box.length] = option;");
            while (multi.next()) {
                final String name = multi.<String>getAttribute(CIUbicaciones.UbicacionStandard.Name);
                js.append("option = new Option(\"").append(StringEscapeUtils.unescapeJavaScript(name)).append("\",")
                    .append(multi.getCurrentInstance().getId()).append(");").append("box.options[box.length] = option;");
            }
        }

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Map<String, String> map = new HashMap<String, String>();
        map.put(EFapsKey.FIELDUPDATE_JAVASCRIPT.getKey(), js.toString());
        list.add(map);
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    public Return getDropdown(final Parameter _parameter)
        throws EFapsException
    {
        final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field() {

            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
                final String parentAttr = (String) props.get("ParentAttribute");

                final Instance instance = _parameter.getCallInstance();
                if (parentAttr != null && !"".equals(parentAttr)) {
                    final PrintQuery print = new PrintQuery(instance);
                    final SelectBuilder selID = new SelectBuilder().linkto(parentAttr).attribute("ID");
                    print.addSelect(selID);
                    if (print.execute()) {
                        final Long id = print.<Long>getSelect(selID);
                        _queryBldr.addWhereAttrEqValue(CIUbicaciones.UbicacionAbstract.ParentLink, id);
                    }
                }
            }

            @Override
            protected DropDownPosition getDropDownPosition(final Parameter _parameter,
                                                           final Object _value,
                                                           final Object _option)
                throws EFapsException
            {
                final DropDownPosition ret = super.getDropDownPosition(_parameter, _value, _option);

                final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
                final Instance instance = _parameter.getCallInstance();
                if (fieldValue != null) {
                    final PrintQuery print = new PrintQuery(instance);
                    final SelectBuilder selID = new SelectBuilder()
                                                        .linkto(fieldValue.getAttribute().getName()).attribute("ID");
                    print.addSelect(selID);
                    if (print.execute()) {
                        final Long id = print.<Long>getSelect(selID);
                        if (id.equals(_value)) {
                            ret.setSelected(true);
                        }
                    }
                }

                return ret;
            }
        };

        return field.dropDownFieldValue(_parameter);
    }

}
