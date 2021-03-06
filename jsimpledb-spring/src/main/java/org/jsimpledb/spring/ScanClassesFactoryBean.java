
/*
 * Copyright (C) 2015 Archie L. Cobbs. All rights reserved.
 */

package org.jsimpledb.spring;

class ScanClassesFactoryBean extends ScanClassPathFactoryBean {

    @Override
    AnnotatedClassScanner createScanner() {
        return new JSimpleDBClassScanner(this.useDefaultFilters, this.environment);
    }
}

