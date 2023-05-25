package javaT.beans.XMLEncoder;
/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 6245149
 * @summary Tests URI encoding
 * @author Sergey Malenkov
 */

import java.net.URI;
import java.net.URISyntaxException;

public final class java_net_URI extends AbstractTest<URI> {
    public static void main(String[] args) {
        new java_net_URI().test(true);
    }

    protected URI getObject() {
        try {
            return new URI("http://www.sun.com/");
        } catch (URISyntaxException exception) {
            throw new Error("unexpected exception", exception);
        }
    }

    protected URI getAnotherObject() {
        try {
            return new URI("ftp://www.sun.com/");
        } catch (URISyntaxException exception) {
            throw new Error("unexpected exception", exception);
        }
    }
}