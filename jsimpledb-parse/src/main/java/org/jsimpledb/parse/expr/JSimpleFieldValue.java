
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb.parse.expr;

import org.jsimpledb.JObject;
import org.jsimpledb.JSimpleField;
import org.jsimpledb.parse.ParseSession;

/**
 * {@link Value} that reflects a {@link JSimpleField} in some {@link JObject}.
 */
public class JSimpleFieldValue extends JFieldValue implements LValue {

    /**
     * Constructor.
     *
     * @param jobj database object
     * @param jfield database field
     * @throws IllegalArgumentException if {@code jobj} is null
     * @throws IllegalArgumentException if {@code jfield} is null
     */
    public JSimpleFieldValue(JObject jobj, JSimpleField jfield) {
        super(jobj, jfield);
    }

    @Override
    public void set(ParseSession session, Value value) {
        final Object obj = value.get(session);
        try {
            ((JSimpleField)this.jfield).setValue(this.jobj, obj);
        } catch (IllegalArgumentException e) {
            throw new EvalException("invalid " + AbstractValue.describeType(obj) + " for field `" + this.jfield.getName() + "'"
              + (e.getMessage() != null ? ": " + e.getMessage() : ""), e);
        }
    }
}

