/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.pgjson;

import java.io.Serializable;
import java.sql.Array;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;

/**
 * The database-level column types.
 *
 * @since 11.1
 */
public enum PGType {

    TYPE_STRING("varchar", Types.VARCHAR),

    TYPE_STRING_ARRAY("varchar[]", Types.ARRAY, TYPE_STRING),

    TYPE_LONG("int8", Types.BIGINT),

    TYPE_LONG_ARRAY("int8[]", Types.ARRAY, TYPE_LONG),

    TYPE_DOUBLE("float8", Types.DOUBLE),

    TYPE_TIMESTAMP("int8", Types.BIGINT), // JSON compat

    TYPE_BOOLEAN("bool", Types.BIT),

    TYPE_JSON("jsonb", Types.OTHER);

    /** Database type name. */
    protected final String name;

    /** Type from {@link java.sql.Types} */
    protected final int type;

    /** {@link PGType} of array elements. */
    protected final PGType baseType;

    /** Creates a simple type. */
    private PGType(String string, int type) {
        this(string, type, null);
    }

    /** Creates an array type. */
    private PGType(String string, int type, PGType baseType) {
        this.name = string;
        this.type = type;
        this.baseType = baseType;
    }

    public boolean isArray() {
        return baseType != null;
    }

    @Override
    public String toString() {
        if (baseType == null) {
            return getClass().getSimpleName() + '(' + name + ',' + JDBCType.valueOf(type) + ')';
        } else {
            return getClass().getSimpleName() + '(' + name + ',' + JDBCType.valueOf(type) + ',' + baseType + ')';
        }
    }

    /**
     * Gets the value for this type from a {@link ResultSet}.
     */
    public Serializable getValue(ResultSet rs, int i, PGJSONConverter converter) throws SQLException {
        switch (this) {
        case TYPE_STRING:
            return rs.getString(i);
        case TYPE_LONG:
            long l = rs.getLong(i);
            if (rs.wasNull()) {
                return null;
            }
            return Long.valueOf(l);
        case TYPE_DOUBLE:
            double d = rs.getDouble(i);
            if (rs.wasNull()) {
                return null;
            }
            return Double.valueOf(d);
        case TYPE_TIMESTAMP:
            long millis = rs.getLong(i);
            if (rs.wasNull()) {
                return null;
            }
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millis);
            return cal;
        case TYPE_BOOLEAN:
            boolean b = rs.getBoolean(i);
            if (rs.wasNull()) {
                return null;
            }
            return Boolean.valueOf(b);
        case TYPE_STRING_ARRAY:
            Array array1 = rs.getArray(i);
            if (rs.wasNull()) {
                return null;
            }
            Object[] objectArray1 = (Object[]) array1.getArray();
            if (objectArray1 instanceof String[]) {
                return objectArray1;
            } else {
                // convert to String[]
                String[] stringArray = new String[objectArray1.length];
                System.arraycopy(objectArray1, 0, stringArray, 0, objectArray1.length);
                return stringArray;
            }
        case TYPE_LONG_ARRAY:
            Array array2 = rs.getArray(i);
            if (rs.wasNull()) {
                return null;
            }
            Object[] objectArray2 = (Object[]) array2.getArray();
            if (objectArray2 instanceof Long[]) {
                return objectArray2;
            } else {
                // convert to Long[]
                Long[] longArray = new Long[objectArray2.length];
                System.arraycopy(objectArray2, 0, longArray, 0, objectArray2.length);
                return longArray;
            }
        case TYPE_JSON:
            String json = rs.getString(i);
            if (rs.wasNull()) {
                return null;
            }
            return converter.jsonToValue(json);
        default:
            throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    public void setValue(PreparedStatement ps, int i, Object value, PGJSONConverter converter) throws SQLException {
        switch (this) {
        case TYPE_STRING:
            ps.setString(i, (String) value);
            break;
        case TYPE_LONG:
            ps.setLong(i, ((Long) value).longValue());
            break;
        case TYPE_DOUBLE:
            ps.setDouble(i, ((Double) value).doubleValue());
            break;
        case TYPE_TIMESTAMP:
            long millis = ((Calendar) value).getTimeInMillis();
            ps.setLong(i, millis);
            break;
        case TYPE_BOOLEAN:
            ps.setBoolean(i, ((Boolean) value).booleanValue());
            break;
        case TYPE_STRING_ARRAY:
        case TYPE_LONG_ARRAY:
            Array array = ps.getConnection().createArrayOf(baseType.name, (Object[]) value);
            ps.setArray(i, array);
            break;
        case TYPE_JSON:
            String json = converter.valueToJson(value);
            ps.setString(i, json);
            break;
        default:
            throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    /**
     * The bundling of a value and its type.
     */
    public static class PGTypedValue {

        public final Object value;

        public final PGType type;

        public PGTypedValue(Object value, PGType type) {
            this.value = value;
            this.type = type;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + type + ',' + value + ')';
        }
    }

}
