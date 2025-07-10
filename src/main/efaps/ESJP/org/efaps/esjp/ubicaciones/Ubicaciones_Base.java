/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.ubicaciones;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.datamodel.ui.IUIValue;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.field.Field;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIUbicaciones;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.util.InterfaceUtils;
import org.efaps.esjp.common.util.InterfaceUtils_Base.DojoLibs;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("66b65261-371f-4cfc-8260-af1904543c02")
@EFapsApplication("eFapsLocalizations-Ubicaciones")
public abstract class Ubicaciones_Base
    extends AbstractCommon
{
    /** The Constant CACHEKEY. */
    public static final String CACHEKEY = Ubicaciones.class.getName() +  "CACHEKEY";

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
        final Field field = (Field) _parameter.get(ParameterValues.UIOBJECT);
        final Long id = Long.parseLong(_parameter.getParameterValue(field.getName()));
        final QueryBuilder queryBldr = new QueryBuilder(CIUbicaciones.UbicacionStandard);
        queryBldr.addWhereAttrEqValue(CIUbicaciones.UbicacionStandard.ParentLink, id);
        queryBldr.addOrderByAttributeAsc(CIUbicaciones.UbicacionStandard.Name);
        final MultiPrintQuery multi = queryBldr.getCachedPrint(Ubicaciones_Base.CACHEKEY);
        multi.setEnforceSorted(true);
        multi.addAttribute(CIUbicaciones.UbicacionStandard.Name);
        multi.execute();
        final StringBuilder js = new StringBuilder();
        final String target = getProperty(_parameter, "TargetField");
        if (target != null && !target.isEmpty()) {
            js.append("var box; var option;");
            for (final String fieldName : analyseProperty(_parameter, "CleanField").values()) {
                js.append("box = document.getElementsByName(\'").append(fieldName).append("\')[0];")
                    .append("while ( box.options.length ) box.options[0] = null;");
            }
            js.append("box = document.getElementsByName(\'").append(target).append("\')[0];")
                .append("while ( box.options.length ) box.options[0] = null;")
                .append("option = new Option(\"").append(StringEscapeUtils.escapeEcmaScript("-")).append("\",")
                .append(0).append(");").append("box.options[box.length] = option;");
            while (multi.next()) {
                final String name = multi.<String>getAttribute(CIUbicaciones.UbicacionStandard.Name);
                js.append("option = new Option(\"").append(StringEscapeUtils.escapeEcmaScript(name)).append("\",")
                    .append(multi.getCurrentInstance().getId()).append(");")
                    .append("box.options[box.length] = option;");
            }
        }
        final Collection<String> combineTargets = analyseProperty(_parameter, "CombineTargetField").values();
        final Collection<String> combineSources = analyseProperty(_parameter, "CombineSourceField").values();
        if (!combineTargets.isEmpty()) {
            final StringBuilder js2 = new StringBuilder();
            for (final String combineTarget : combineTargets) {
                js2.append("query(\"[name=").append(combineTarget).append("]\").forEach(function(_node1) {\n")
                    .append("domAttr.set(_node1, 'value', '');\n")
                    .append("var vA = [");
                boolean first = true;
                for (final String combineSource : combineSources) {
                    if (first) {
                        first = false;
                    } else {
                        js2.append(",");
                    }
                    js2.append("'").append(combineSource).append("'");
                }
                js2.append("];\n")
                    .append(" array.forEach(vA, function(item) {\n")
                    .append("query(\"[name=\" + item + \"]\").forEach(function(_node2) {\n")
                    .append(" var cv = domAttr.get(_node1, 'value'); \n")
                    .append("var nv = _node2.selectedOptions[0].label;\n")
                    .append("domAttr.set(_node1, 'value', cv.length > 0 ? cv  + \" - \" + nv : nv); \n")
                    .append("});\n")
                    .append("}); \n")
                    .append("});\n");
                }
            js.append(InterfaceUtils.wrapInDojoRequire(_parameter, js2, DojoLibs.QUERY, DojoLibs.DOMATTR,
                            DojoLibs.ARRAY));
        }
        final List<Map<String, String>> list = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();
        map.put("eFapsFieldUpdateJS", js .toString());
        list.add(map);
        ret.put(ReturnValues.VALUES, list);
        return ret;
    }

    /**
     * Gets the dropdown.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the dropdown
     * @throws EFapsException on error
     */
    public Return getDropdown(final Parameter _parameter)
        throws EFapsException
    {
        final org.efaps.esjp.common.uiform.Field field = new org.efaps.esjp.common.uiform.Field() {

            @Override
            protected void add2QueryBuilder4List(final Parameter _parameter,
                                                 final QueryBuilder _queryBldr)
                throws EFapsException
            {
                final String parentAttr = getProperty(_parameter, "ParentAttribute");

                final Instance instance = _parameter.getCallInstance();
                if (parentAttr != null && !"".equals(parentAttr)) {
                    final PrintQuery print = new CachedPrintQuery(instance, Ubicaciones_Base.CACHEKEY);
                    final SelectBuilder selID = new SelectBuilder().linkto(parentAttr).attribute("ID");
                    print.addSelect(selID);
                    if (print.execute()) {
                        final Long id = print.<Long>getSelect(selID);
                        _queryBldr.addWhereAttrEqValue(CIUbicaciones.UbicacionAbstract.ParentLink, id);
                    }
                }
            }

            @Override
            public DropDownPosition getDropDownPosition(final Parameter _parameter,
                                                           final Object _value,
                                                           final Object _option)
                throws EFapsException
            {
                final DropDownPosition ret = super.getDropDownPosition(_parameter, _value, _option);

                final IUIValue uiValue = (IUIValue) _parameter.get(ParameterValues.UIOBJECT);
                final Instance instance = _parameter.getCallInstance();
                if (uiValue != null) {
                    final PrintQuery print = new CachedPrintQuery(instance, Ubicaciones_Base.CACHEKEY);
                    final SelectBuilder selID = new SelectBuilder()
                                                        .linkto(uiValue.getAttribute().getName()).attribute("ID");
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
        return field.getOptionListFieldValue(_parameter);
    }

    /**
     * Gets the address label.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _code the code
     * @return the address label
     * @throws EFapsException on error
     */
    public CharSequence getAddressLabel(final Parameter _parameter,
                                        final String _code)
        throws EFapsException
    {
        CharSequence ret = new StringBuilder();
        final QueryBuilder queryBuilder = new QueryBuilder(CIUbicaciones.UbicacionAbstract);
        queryBuilder.addWhereAttrEqValue(CIUbicaciones.UbicacionAbstract.Code, _code);
        final InstanceQuery query = queryBuilder.getQuery();
        query.execute();
        if (query.next()) {
            ret = getAddressLabel(_parameter, query.getCurrentValue());
        }
        return ret;
    }

    /**
     * Gets the address label.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instance the instance
     * @return the address label
     * @throws EFapsException on error
     */
    public CharSequence getAddressLabel(final Parameter _parameter,
                                        final Instance _instance)
        throws EFapsException
    {
        final String[] names = getNames(_parameter, _instance);
        ArrayUtils.reverse(names);
        return StringUtils.join(names, " - ");
    }

    /**
     * Gets the names.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _instance the instance
     * @return the names
     * @throws EFapsException on error
     */
    protected String[] getNames(final Parameter _parameter,
                                final Instance _instance)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_instance);
        final SelectBuilder selParentInst = SelectBuilder.get().linkto(CIUbicaciones.UbicacionAbstract.ParentLink)
                        .instance();
        print.addSelect(selParentInst);
        print.addAttribute(CIUbicaciones.UbicacionAbstract.Name);
        print.execute();
        final Instance parentInst = print.getSelect(selParentInst);
        String[] ret = new String[] { print.getAttribute(CIUbicaciones.UbicacionAbstract.Name) };
        if (InstanceUtils.isValid(parentInst)) {
            ret = ArrayUtils.addAll(ret, getNames(_parameter, parentInst));
        }
        return ret;
    }
}
