/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.classfile;

/**
 * Marker for values that may be written to a class file as a {@code CONSTANT_Dynamic} constant pool
 * entry.
 *
 * <p>The marker carries no behaviour of its own: a {@link DynamicConstantDescriber} registered with
 * the {@link ClassFileWriter} is what actually turns a value into a constant. Implementing this
 * interface only declares that such a describer may exist, which is what lets {@link
 * ClassFileWriter#addLoadDynamicConstant} accept the value at compile time.
 *
 * <p>This interface deliberately declares no methods and references nothing outside {@code
 * java.lang}, so that runtime classes which are loaded on platforms without bytecode generation
 * support can implement it safely.
 */
public interface DynamicConstant {}
